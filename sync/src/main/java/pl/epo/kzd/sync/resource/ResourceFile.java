package pl.epo.kzd.sync.resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

public interface ResourceFile {

	ResourceFileType getType();

	String getLabel();

	String getSafeFilename();

	void copyTo(OutputStream out) throws IOException;

	String getFtpFilePath();

	Date getModifiedDate() throws FileNotFoundException, IOException;

}