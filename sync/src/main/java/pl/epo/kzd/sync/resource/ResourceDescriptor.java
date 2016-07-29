package pl.epo.kzd.sync.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ResourceDescriptor {

	private final Document doc;

	private final ResourceFactory resourceFactory;

	public ResourceDescriptor(InputStream xmlInputStream, ResourceFactory resourceFactory)
			throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		doc = dBuilder.parse(xmlInputStream);
		this.resourceFactory = resourceFactory;
	}

	public List<Resource> getResources() {
		List<Resource> resources = new LinkedList<>();
		NodeList resourceNodes = doc.getElementsByTagName("resource");
		for (int i = 0; i < resourceNodes.getLength(); i++) {
			Element resourceElement = (Element) resourceNodes.item(i);
			resources.add(getResource(resourceElement));
		}
		return resources;
	}

	private Resource getResource(Element resourceElement) {
		return resourceFactory.createResource(resourceElement);
	}

}
