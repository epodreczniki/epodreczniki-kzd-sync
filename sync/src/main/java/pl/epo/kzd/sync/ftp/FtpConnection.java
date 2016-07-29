package pl.epo.kzd.sync.ftp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTPFile;

import pl.epo.kzd.xml.ftp.FtpClient;
import pl.epo.kzd.xml.ftp.FtpSettings;

@Slf4j
public class FtpConnection extends FtpClient {

	public FtpConnection(FtpSettings ftpSettings) throws SocketException, IOException, AuthenticationException {
		super(ftpSettings);
	}

	public void retrieveFileStream(String path, OutputStream os) throws IOException {
		log.debug("downloading file: " + path);
		InputStream is = ftpClient.retrieveFileStream(path);
		if (is == null || ftpClient.getReplyCode() == 550) {
			throw new FileNotFoundException(path);
		}
		IOUtils.copy(is, os);
		if (!ftpClient.completePendingCommand()) {
			throw new IOException("could not complete pending FTP command");
		}
		is.close();
	}

	/**
	 * @return file paths
	 */
	public List<String> listFiles(String dirPath) throws IOException {
		if (!dirPath.endsWith("/")) {
			dirPath = dirPath + "/";
		}

		FTPFile[] ftpFiles = ftpClient.listFiles(dirPath);
		List<String> paths = new LinkedList<String>();
		for (FTPFile ftpFile : ftpFiles) {
			paths.add(dirPath + ftpFile.getName());
		}
		return paths;
	}

	public Date getModifiedDate(String path) throws FileNotFoundException, IOException {
		String response = ftpClient.getModificationTime(path);
		if (response == null) {
			throw new FileNotFoundException("FTP path: " + path);
		}
		String time = response.split(" ")[1];
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		try {
			return dateFormat.parse(time);
		} catch (ParseException e) {
			throw new IOException(e);
		}
	}

	public long getFileSize(String path) throws IOException {
		ftpClient.sendCommand("SIZE", path);
		String reply = ftpClient.getReplyString();
		return Long.parseLong(reply.split(" ")[1].trim());
	}

	public void deleteFile(String path) throws IOException {
		ftpClient.deleteFile(path);
	}

}
