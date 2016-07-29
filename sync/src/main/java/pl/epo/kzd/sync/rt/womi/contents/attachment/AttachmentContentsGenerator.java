package pl.epo.kzd.sync.rt.womi.contents.attachment;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import pl.epo.kzd.sync.resource.ResourceFile;
import pl.epo.kzd.sync.rt.womi.contents.ContentsGenerator;

public class AttachmentContentsGenerator extends ContentsGenerator {

	private static final String INTERACTIVE_PACKAGE_CLASSIC = "INTERACTIVE_PACKAGE_CLASSIC";

	private static final String ATTACHMENT_FILENAME = "attachment.zip";

	private final AttachmentContents contents;

	public AttachmentContentsGenerator(File womiDir, ResourceFile resFile) {
		super(womiDir);
		this.contents = new AttachmentContents(resFile);
	}

	@Override
	public void close() throws IOException {
		try {
			File contentsDir = super.createContentsDirAndMainFile(INTERACTIVE_PACKAGE_CLASSIC, ATTACHMENT_FILENAME);
			contents.save(new File(contentsDir, ATTACHMENT_FILENAME));
		} catch (ParserConfigurationException | TransformerException e) {
			throw new IOException(e);
		}
	}

}
