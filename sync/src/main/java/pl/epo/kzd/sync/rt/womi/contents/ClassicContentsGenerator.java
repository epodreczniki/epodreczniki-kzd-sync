package pl.epo.kzd.sync.rt.womi.contents;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import pl.epo.kzd.sync.resource.ResourceFile;

public class ClassicContentsGenerator extends ContentsGenerator {

	private final ResourceFile resourceFile;

	public ClassicContentsGenerator(File womiDir, ResourceFile resFile) {
		super(womiDir);
		this.resourceFile = resFile;
	}

	@Override
	public void close() throws IOException {
		try {
			String filename = resourceFile.getSafeFilename();
			File contentsDir = super.createContentsDirAndMainFile(recognizeWomiFormatType(), filename);
			File objectFile = new File(contentsDir, filename);
			try (FileOutputStream fos = new FileOutputStream(objectFile)) {
				resourceFile.copyTo(fos);
			}
		} catch (ParserConfigurationException | TransformerException e) {
			throw new IOException(e);
		}
	}

	private String recognizeWomiFormatType() {
		switch (resourceFile.getType()) {
			case GRAPHICS:
			case THUMBNAIL:
			case GALLERY_GRAPHICS:
				return "GRAPHICS_IMAGE_CLASSIC";
			case MOVIE:
				return "MOVIE_VIDEO_CLASSIC";
			case SOUND:
				return "SOUND_AUDIO_CLASSIC";
			case INTERACTIVE:
				return "INTERACTIVE_PACKAGE_CLASSIC";
			default:
				throw new IllegalStateException("ClassicContentsGenerator does not support files of format '"
						+ resourceFile.getType() + "'");
		}
	}

}
