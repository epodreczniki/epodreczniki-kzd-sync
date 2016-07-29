package pl.epo.kzd.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import pl.epo.kzd.xml.category.CategoryValidator;
import pl.epo.kzd.xml.ftp.FtpClient;
import pl.epo.kzd.xml.ftp.FtpClient.AuthenticationException;
import pl.epo.kzd.xml.ftp.FtpSettings;
import pl.epo.kzd.xml.uspp.USPP;

public class KzdXmlValidator {

	private static final Pattern RESOURCE_ID_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");

	public static boolean isValidResourceId(String resourceId) {
		return RESOURCE_ID_PATTERN.matcher(resourceId).matches();
	}

	private Schema schema;

	private final FtpSettings ftpSettings;

	private final FtpClient ftpClient;

	private boolean skipResourceIdentifiersValidation = false;

	private CategoryValidator categoryValidator = new CategoryValidator();

	private KzdXmlValidator(FtpSettings ftpSettings, FtpClient ftpClient) throws IOException {
		this.ftpSettings = ftpSettings;
		this.ftpClient = ftpClient;

		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		URL schemaUrl;
		try {
			schemaUrl = getSchemaUrl();
		} catch (IOException e) {
			throw new IllegalStateException("Error reading schema URL", e);
		}

		try {
			this.schema = factory.newSchema(schemaUrl);
		} catch (SAXException e) {
			throw new IOException("Error reading schema from " + schemaUrl, e);
		}
	}

	public KzdXmlValidator(FtpSettings ftpSettings) throws IOException {
		this(ftpSettings, null);
	}

	public KzdXmlValidator(FtpClient ftpClient, boolean skipResourceIdentifiersValidation) throws IOException {
		this(null, ftpClient);
		this.skipResourceIdentifiersValidation = skipResourceIdentifiersValidation;
	}

	private URL getSchemaUrl() throws IOException {
		return new URL(Config.getSchemaUrl());
	}

	/**
	 * @return list of error messages
	 */
	public List<String> validate(File xmlFile) throws IOException {
		List<String> errors = new LinkedList<>();
		try (FileInputStream fis = new FileInputStream(xmlFile)) {
			errors.addAll(validateWithSchema(fis));
		}
		try (FileInputStream fis = new FileInputStream(xmlFile)) {
			errors.addAll(validateCategories(fis));
		}
		if (!this.skipResourceIdentifiersValidation) {
			try (FileInputStream fis = new FileInputStream(xmlFile)) {
				errors.addAll(validateResourceIdentifiers(fis));
			}
		}
		try (FileInputStream fis = new FileInputStream(xmlFile)) {
			errors.addAll(validateUsppIdentifiers(fis));
		}

		if (ftpSettings == null && ftpClient == null) {
			errors.add("Skipping FTP files checking because of no FTP settings");
		} else if (errors.size() > 0) {
			errors.add("Skipping FTP files checking because of earlier errors");
		} else {
			try (FileInputStream fis = new FileInputStream(xmlFile)) {
				errors.addAll(validateFtpFiles(fis));
			}
		}

		return errors;
	}

	private List<String> validateCategories(FileInputStream xmlInputStream) throws IOException {
		List<String> errors = new LinkedList<>();
		List<String> categories;
		try {
			categories = getCategories(xmlInputStream);
		} catch (ParserConfigurationException | SAXException e) {
			errors.add("Unexpected error in the given XML: " + e);
			return errors;
		}

		for (String category : categories) {
			if (!categoryValidator.isValid(category)) {
				errors.add(String.format("Category \"%s\" is unknown", category));
			}
		}

		return errors;
	}

	private List<String> validateResourceIdentifiers(FileInputStream xmlInputStream) throws IOException {
		List<String> errors = new LinkedList<>();
		List<String> identifiers;
		try {
			identifiers = getResourceIdentifiers(xmlInputStream);
		} catch (ParserConfigurationException | SAXException e) {
			errors.add("Unexpected error in the given XML: " + e);
			return errors;
		}

		for (String resourceId : identifiers) {
			if (!isValidResourceId(resourceId)) {
				errors.add(String.format(
						"Resource \"%s\" has invalid ID - use only a-z, A-Z, 0-9 or _ (no whitespaces!)", resourceId));
			}
		}

		return errors;
	}

	private List<String> validateUsppIdentifiers(FileInputStream xmlInputStream) throws IOException {
		List<String> errors = new LinkedList<>();
		List<ValidationSubject> identifiers;
		try {
			identifiers = getUsppIdentifiers(xmlInputStream);
		} catch (ParserConfigurationException | SAXException e) {
			errors.add("Unexpected error in the given XML: " + e);
			return errors;
		}

		int count = identifiers.size();
		System.out.println(count + " USPP identifiers found in XML");

		int i = 0;
		System.out.println("Identifiers validated: " + i + " of " + count);
		USPP uspp = new USPP();
		for (ValidationSubject err : identifiers) {
			try {
				String usppId = err.getValidationSubject();
				String resourceId = err.getResourceId();
				if (!uspp.isValidIdentifier(usppId)) {
					errors.add(String.format("Resource \"%s\" - unknown USPP identifier: \"%s\"", resourceId, usppId));
				}
				i++;
				if (i % 10 == 0) {
					System.out.println("Identifiers validated: " + i + " of " + count);
				}
			} catch (IOException e) {
				errors.add("Checking USPP identifiers stopped, because of connection problems (" + e.getMessage() + ")");
				return errors;
			}
		}
		System.out.println("Identifiers validated: " + i + " of " + count);

		return errors;
	}

	private List<String> validateFtpFiles(InputStream xmlInputStream) throws IOException {
		List<String> errors = new LinkedList<>();
		List<ValidationSubject> paths;
		try {
			paths = getFilePaths(xmlInputStream);
		} catch (ParserConfigurationException | SAXException e) {
			errors.add("Unexpected error in the given XML: " + e);
			return errors;
		}

		System.out.println(paths.size() + " file paths found in XML");

		if (ftpClient != null) {
			errors.addAll(checkFilesExist(ftpClient, paths));
		} else {
			System.out.println("Connecting to " + getHost());
			try (FtpClient ftp = new FtpClient(ftpSettings)) {
				errors.addAll(checkFilesExist(ftp, paths));
			} catch (AuthenticationException e) {
				System.err.println("Connecting to " + getHost() + " failed on username & password authentication");
				errors.add("Could not check FTP files");
			}
		}
		return errors;
	}

	private List<String> checkFilesExist(FtpClient ftp, List<ValidationSubject> paths) throws IOException {
		List<String> errors = new LinkedList<>();
		int count = paths.size();
		int i = 0;
		System.out.println("Files checked: " + i + " of " + count);
		for (ValidationSubject sub : paths) {
			String path = sub.getValidationSubject();
			String resourceId = sub.getResourceId();
			if (!ftp.fileExists(path)) {
				errors.add(String.format("Resource \"%s\" - file not found: %s", resourceId, path));
			}
			i++;
			if (i % 10 == 0) {
				System.out.println("Files checked: " + i + " of " + count);
			}
		}
		System.out.println("Files checked: " + i + " of " + count);
		return errors;
	}

	private String getHost() {
		if (ftpSettings != null) {
			return ftpSettings.getHost();
		} else {
			return "[already connected]";
		}
	}

	private List<String> getCategories(FileInputStream xmlInputStream) throws ParserConfigurationException,
			SAXException, IOException {
		List<String> categories = new LinkedList<>();
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(xmlInputStream);
		NodeList categoryNodes = doc.getElementsByTagName("category");
		for (int i = 0; i < categoryNodes.getLength(); i++) {
			Element categoryElement = (Element) categoryNodes.item(i);
			categories.add(categoryElement.getTextContent());
		}
		return categories;
	}

	private List<String> getResourceIdentifiers(FileInputStream xmlInputStream) throws ParserConfigurationException,
			SAXException, IOException {
		List<String> resourceIdentifiers = new LinkedList<>();
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(xmlInputStream);
		NodeList resourceNodes = doc.getElementsByTagName("resource");
		for (int i = 0; i < resourceNodes.getLength(); i++) {
			Element resourceElement = (Element) resourceNodes.item(i);
			String resourceId = resourceElement.getAttribute("id");
			resourceIdentifiers.add(resourceId);
		}
		return resourceIdentifiers;
	}

	private List<ValidationSubject> getUsppIdentifiers(FileInputStream xmlInputStream)
			throws ParserConfigurationException, SAXException, IOException {
		List<ValidationSubject> err = new LinkedList<>();
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(xmlInputStream);
		NodeList usppNodes = doc.getElementsByTagName("uspp");
		for (int i = 0; i < usppNodes.getLength(); i++) {
			Element usppElement = (Element) usppNodes.item(i);
			String usppId = usppElement.getTextContent();

			Element resourceElement = (Element) usppElement.getParentNode().getParentNode();
			String resourceId = resourceElement.getAttribute("id");
			err.add(new ValidationSubject(resourceId, usppId));
		}
		return err;
	}

	private List<ValidationSubject> getFilePaths(InputStream xmlInputStream) throws ParserConfigurationException,
			SAXException, IOException {
		List<ValidationSubject> paths = new LinkedList<>();
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(xmlInputStream);
		NodeList filesNodes = doc.getElementsByTagName("files");
		for (int i = 0; i < filesNodes.getLength(); i++) {
			Element filesElement = (Element) filesNodes.item(i);
			NodeList children = filesElement.getChildNodes();

			Element resourceElement = (Element) filesElement.getParentNode();
			String resourceId = resourceElement.getAttribute("id");
			for (int j = 0; j < children.getLength(); j++) {
				Node child = children.item(j);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					Element file = (Element) child;
					String path = file.getAttribute("path");
					paths.add(new ValidationSubject(resourceId, path));
				}
			}
		}
		return paths;
	}

	private List<String> validateWithSchema(InputStream is) throws IOException {
		List<String> errors = new LinkedList<>();
		try {
			Validator validator = schema.newValidator();
			validator.validate(new StreamSource(is));
		} catch (SAXException e) {
			errors.add(e.toString());
		}
		return errors;
	}
}

class ValidationSubject {

	private final String resourceId;

	private final String validationSubject;

	public ValidationSubject(String resourceId, String validationSubject) {
		super();
		this.resourceId = resourceId;
		this.validationSubject = validationSubject;
	}

	public String getResourceId() {
		return resourceId;
	}

	public String getValidationSubject() {
		return validationSubject;
	}

}