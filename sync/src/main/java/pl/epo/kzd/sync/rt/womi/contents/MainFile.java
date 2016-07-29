package pl.epo.kzd.sync.rt.womi.contents;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import pl.epo.kzd.sync.rt.womi.utils.XMLGenerator;

class MainFile {

	private final Map<String, String> formats = new HashMap<>();

	public void addFormat(String type, String filename) {
		if (this.formats.containsKey(type)) {
			throw new IllegalArgumentException("the main file already contains format of type '" + type + "'");
		}
		this.formats.put(type, filename);
	}

	public void save(File file) throws ParserConfigurationException, IOException, TransformerException {
		Document doc = XMLGenerator.createDocument();
		Element root = doc.createElement("formats");
		doc.appendChild(root);

		for (Entry<String, String> format : formats.entrySet()) {
			Element el = doc.createElement("format");
			el.setAttribute("id", format.getKey());
			el.setAttribute("mainFile", format.getValue());
			root.appendChild(el);
		}

		try (FileOutputStream fos = new FileOutputStream(file)) {
			XMLGenerator.generateXML(doc, fos);
		}
	}

}
