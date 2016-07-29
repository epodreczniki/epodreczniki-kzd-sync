package pl.epo.kzd.xml.ftp;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

public class FtpClient implements Closeable {

	protected final FTPClient ftpClient;

	public FtpClient(FtpSettings settings) throws SocketException, IOException, AuthenticationException {
		if (settings == null) {
			throw new IllegalArgumentException("settings cannot be null");
		}

		ftpClient = new FTPClient();
		if (settings.getPort() != null) {
			ftpClient.connect(settings.getHost(), settings.getPort());
		} else {
			ftpClient.connect(settings.getHost());
		}
		boolean loginSuccess = ftpClient.login(settings.getUsername(), settings.getPassword());
		if (!loginSuccess) {
			throw new AuthenticationException();
		}

		ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
		ftpClient.enterLocalPassiveMode();
	}

	public boolean fileExists(String path) throws IOException {
		if (path == null || path.trim().length() == 0) {
			return false;
		}

		FTPFile[] files = ftpClient.listFiles(path);
		if (files.length != 1) {
			return false;
		}

		FTPFile ftpFile = files[0];
		return ftpFile.isFile() && path.endsWith(ftpFile.getName());
	}

	@Override
	public void close() throws IOException {
		try {
			if (ftpClient.isConnected()) {
				ftpClient.logout();
				ftpClient.disconnect();
			}
		} catch (IOException e) {
		}

	}

	public class AuthenticationException extends Exception {

	}
}
