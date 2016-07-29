package pl.epo.kzd.sync.resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import lombok.Getter;
import lombok.ToString;

import org.w3c.dom.Element;

import pl.epo.kzd.sync.ftp.FtpConnection;
import pl.epo.kzd.sync.utils.Utils;

@ToString(exclude = { "ftpConnection", "label", "modifiedDate" })
class ResourceFtpFile implements ResourceFile {

	private FtpConnection ftpConnection;

	@Getter
	private final ResourceFileType type;

	private final String path;

	@Getter
	private final String label;

	private Date modifiedDate;

	public ResourceFtpFile(Element source, FtpConnection ftpConnection) {
		this.ftpConnection = ftpConnection;
		this.type = ResourceFileType.valueOfXmlTagName(source.getTagName());
		this.path = source.getAttribute("path");
		this.label = source.getAttribute("label");

		if (this.path.contains("\\")) {
			throw new IllegalArgumentException("FTP paths should use slashes rather than backslashes");
		}
	}

	/**
	 * for testing
	 */
	ResourceFtpFile(ResourceFileType type, String path, String label) {
		this.type = type;
		this.path = path;
		this.label = label;
	}

	@Override
	public String getSafeFilename() {
		return Utils.sanitizeString(getFilename());
	}

	protected String getFilename() {
		int i = path.lastIndexOf("/");
		if (i >= 0) {
			return path.substring(i + 1);
		} else {
			return path;
		}
	}

	@Override
	public void copyTo(OutputStream os) throws IOException {
		Date modifiedDate = getModifiedDate();
		long size = readFileSizeFromFTP();

		ftpConnection.retrieveFileStream(path, os);

		Date modifiedDateAfter = readModifiedDateFromFTP();
		long sizeAfter = readFileSizeFromFTP();
		if (!modifiedDate.equals(modifiedDateAfter) || size != sizeAfter) {
			throw new IOException(String.format(
					"file %s has changed during download (size %d vs. %d, modified date '%s' vs. '%s')", path, size,
					sizeAfter, modifiedDate, modifiedDateAfter));
		}
	}

	@Override
	public String getFtpFilePath() {
		return path;
	}

	@Override
	public Date getModifiedDate() throws FileNotFoundException, IOException {
		if (modifiedDate == null) {
			modifiedDate = readModifiedDateFromFTP();
		}
		return modifiedDate;
	}

	private Date readModifiedDateFromFTP() throws FileNotFoundException, IOException {
		return ftpConnection.getModifiedDate(path);
	}

	private long readFileSizeFromFTP() throws IOException {
		return ftpConnection.getFileSize(path);
	}
}
