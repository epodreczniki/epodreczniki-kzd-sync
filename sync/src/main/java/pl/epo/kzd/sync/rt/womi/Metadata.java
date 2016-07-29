package pl.epo.kzd.sync.rt.womi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import pl.epo.kzd.sync.resource.Resource;
import pl.epo.kzd.sync.rt.womi.utils.XMLGenerator;
import pl.epo.kzd.xml.uspp.USPP;

class Metadata {

	private static final String ROOT_ELEMENT = "metadata";

	private final File file;

	public Metadata(File file) {
		this.file = file;
	}

	public void saveFrom(Resource resource) throws ParserConfigurationException, TransformerException, IOException {
		Document doc = createXml(resource);
		saveToFile(doc);
	}

	public void saveEmpty() throws ParserConfigurationException, TransformerException, IOException {
		Document doc = XMLGenerator.createDocument();
		Element root = doc.createElement(ROOT_ELEMENT);
		doc.appendChild(root);
		createChildElement(doc, root, "Przeznaczenie", "kzd_embedded");
		saveToFile(doc);
	}

	private Document createXml(Resource resource) throws ParserConfigurationException {
		Document doc = XMLGenerator.createDocument();
		Element root = doc.createElement(ROOT_ELEMENT);
		doc.appendChild(root);

		createChildElement(doc, root, "IdentyfikatorWlasny", resource.getId());
		createChildElement(doc, root, "Tytul", resource.getTitle());
		createChildElement(doc, root, "Autor", resource.getAuthor());
		createChildElement(doc, root, "TekstAlternatywny", resource.getAltText());
		createChildElement(doc, root, "Licencja", resource.getLicense());
		createChildElement(doc, root, "Przeznaczenie", "kzd");
		createChildElement(doc, root, "Pochodzenie", resource.getOrigin());
		createChildElement(doc, root, "Odbiorca", formatRecipientValue(resource));
		createChildElement(doc, root, "Kategoria", resource.getCategory());
		createChildElement(doc, root, "Opis", resource.getDescription());

		for (String keyword : resource.getKeywords()) {
			createChildElement(doc, root, "SlowaKluczowe", keyword);
		}
		if (resource.getLearningObjectives().size() > 0) {
			String learningObjectivesValue = createLearningObjectivesValue(resource);
			if (learningObjectivesValue.length() > 0) {
				createChildElement(doc, root, "PodstawaProgramowa", learningObjectivesValue);
			}
		}
		if (resource.getEnvironments().size() > 0) {
			createChildElement(doc, root, "SystemOperacyjny", StringUtils.join(resource.getEnvironments(), "|"));
		}

		return doc;
	}

	private String createLearningObjectivesValue(Resource resource) {
		List<String> list = new LinkedList<>(resource.getLearningObjectives());
		list.remove(USPP.getSpecialAllIdentifier());
		return StringUtils.join(list, "|");
	}

	protected String formatRecipientValue(Resource resource) {
		switch (resource.getRecipient()) {
			case STUDENT:
				return "Uczeń";
			case TEACHER:
				return "Nauczyciel";
		}
		throw new IllegalArgumentException("unknown recipient value: " + resource.getRecipient());
	}

	private void createChildElement(Document doc, Element parent, String tagName, String value) {
		Element element = doc.createElement(tagName);
		element.setTextContent(value);
		parent.appendChild(element);
	}

	private void saveToFile(Document doc) throws TransformerException, IOException {
		try (FileOutputStream fos = new FileOutputStream(file)) {
			XMLGenerator.generateXML(doc, fos);
		}
	}

}
