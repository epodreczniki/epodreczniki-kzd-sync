package pl.epo.kzd.sync.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import lombok.Getter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pl.epo.kzd.sync.ftp.FtpConnection;

@Getter
public class Resource {

	private final String id;

	private final String title;

	private final String author;

	private final String description;

	private final List<String> keywords;

	private final String altText;

	private final String license;

	private final String origin;

	private final Recipient recipient;

	private final String category;

	private final List<String> learningObjectives;

	private final List<String> environments;

	private final List<ResourceFile> files;

	private Element sourceElement;

	Resource(FtpConnection ftpConnection, Element el) {
		this.sourceElement = el;
		this.id = el.getAttribute("id");
		this.title = getChildElement(el, "title").getTextContent();
		this.author = getChildElement(el, "author").getTextContent();
		this.description = getChildElement(el, "description").getTextContent();
		this.altText = getChildElement(el, "alt").getTextContent();
		this.license = extractLicense(el);
		this.origin = getChildElement(el, "origin").getTextContent();
		this.recipient = extractRecipient(el);
		this.category = getChildElement(el, "category").getTextContent();

		this.keywords = Collections.unmodifiableList(Arrays.asList(getChildElement(el, "keywords").getTextContent()
				.split("\\s*,\\s*")));
		this.learningObjectives = Collections.unmodifiableList(extractElementValues(getChildElement(el,
				"learning-objectives").getElementsByTagName("uspp")));
		Element envs = getChildElement(el, "environments");
		if (envs != null) {
			this.environments = Collections.unmodifiableList(extractElementValues(envs
					.getElementsByTagName("environment")));
		} else {
			this.environments = Collections.unmodifiableList(new LinkedList<String>());
		}

		List<ResourceFile> files = new LinkedList<>();
		NodeList fileNodes = getChildElement(el, "files").getChildNodes();
		for (int i = 0; i < fileNodes.getLength(); i++) {
			Node node = fileNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				files.add(new ResourceFtpFile((Element) node, ftpConnection));
			}
		}
		this.files = Collections.unmodifiableList(files);
	}

	private String extractLicense(Element el) {
		String originalLicense = getChildElement(el, "license").getTextContent();
		if ("read-only".equals(originalLicense)) {
			return "nieznana";
		} else {
			return originalLicense;
		}
	}

	private Recipient extractRecipient(Element el) {
		String text = getChildElement(el, "recipient").getTextContent();
		return Recipient.valueOf(text.toUpperCase());
	}

	private List<String> extractElementValues(NodeList learningObjectiveElements) {
		List<String> list = new LinkedList<>();
		for (int i = 0; i < learningObjectiveElements.getLength(); i++) {
			String uspp = learningObjectiveElements.item(i).getTextContent();
			list.add(uspp);
		}
		return list;
	}

	private Element getChildElement(Element parent, String tagName) {
		NodeList children = parent.getElementsByTagName(tagName);
		if (children.getLength() > 1) {
			throw new IllegalArgumentException("resource element should have no more than one child-element '"
					+ tagName + "'");
		} else if (children.getLength() == 1) {
			return (Element) children.item(0);
		} else {
			return null;
		}
	}

	@Override
	public String toString() {
		return "Resource \"" + id + "\"";
	}

	public int getNumberOfFiles() {
		return files.size();
	}

	public InputStream toXml() {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("kzd");
			doc.appendChild(rootElement);
			rootElement.appendChild(doc.importNode(sourceElement, true));

			DOMSource source = new DOMSource(doc);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			StreamResult result = new StreamResult(outputStream);
			TransformerFactory.newInstance().newTransformer().transform(source, result);
			return new ByteArrayInputStream(outputStream.toByteArray());
		} catch (ParserConfigurationException | TransformerException | TransformerFactoryConfigurationError e) {
			throw new IllegalStateException(e);
		}
	}

}
