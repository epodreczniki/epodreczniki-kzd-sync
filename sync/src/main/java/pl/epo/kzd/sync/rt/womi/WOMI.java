package pl.epo.kzd.sync.rt.womi;

import java.io.File;
import java.io.IOException;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;

import pl.epo.kzd.sync.resource.ResourceFile;

@Slf4j
public class WOMI {

	private static final String ILLEGAL_STATE_MESSAGE = "The directory of this WOMI does not exist "
			+ "(has been cleaned up or the WOMI doesn't have a local directory)";

	private final File directory;

	@Getter
	@Setter
	private long id;

	@Getter
	private ResourceFile source;

	public WOMI(File directory) {
		if (!directory.exists()) {
			throw new IllegalArgumentException("The given WOMI directory does not exist: " + directory);
		}
		this.directory = directory;
	}

	public WOMI(File directory, ResourceFile resourceFile) {
		this(directory);
		this.source = resourceFile;
	}

	public WOMI(long id, ResourceFile source) {
		this.directory = null;
		this.id = id;
		this.source = source;
	}

	public void cleanup() {
		if (directory != null) {
			try {
				FileUtils.deleteDirectory(this.directory);
			} catch (IOException e) {
				log.warn("error deleting " + this.directory, e);
			}
		}
	}

	public File getDirectory() {
		if (directory == null && !directory.exists()) {
			throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
		}
		return directory;
	}

}
