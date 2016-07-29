package pl.epo.kzd.sync.rt.womi.contents.attachment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import pl.epo.kzd.sync.resource.ResourceFile;

class AttachmentContents {

	private static final String MANIFEST_FILENAME = "manifest.json";

	private ResourceFile resourceFile;

	public AttachmentContents(ResourceFile resourceFile) {
		this.resourceFile = resourceFile;
	}

	public void save(File destinationZipFile) throws IOException {
		try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(destinationZipFile))) {
			String attachmentFilename = resourceFile.getSafeFilename();

			AttachmentManifest manifest = new AttachmentManifest(attachmentFilename);
			zip.putNextEntry(new ZipEntry(MANIFEST_FILENAME));
			manifest.save(zip);

			zip.putNextEntry(new ZipEntry(attachmentFilename));
			resourceFile.copyTo(zip);
		}
	}
}
