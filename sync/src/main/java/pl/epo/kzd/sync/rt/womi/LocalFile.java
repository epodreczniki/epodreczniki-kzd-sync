package pl.epo.kzd.sync.rt.womi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import org.apache.commons.io.IOUtils;

import pl.epo.kzd.sync.resource.ResourceFile;
import pl.epo.kzd.sync.resource.ResourceFileType;
import pl.epo.kzd.sync.utils.Utils;

class LocalFile implements ResourceFile {

	private final File file;

	LocalFile(File file) {
		this.file = file;
	}

	@Override
	public ResourceFileType getType() {
		return ResourceFileType.MOVIE;
	}

	@Override
	public String getSafeFilename() {
		return Utils.sanitizeString(getLabel());
	}

	@Override
	public String getLabel() {
		return file.getName();
	}

	@Override
	public void copyTo(OutputStream out) throws IOException {
		try (FileInputStream in = new FileInputStream(file)) {
			IOUtils.copy(in, out);
		}
	}

	@Override
	public String getFtpFilePath() {
		throw new IllegalStateException("not implemented :)");
	}

	@Override
	public Date getModifiedDate() throws IOException {
		throw new IllegalStateException("not implemented :)");
	}
}