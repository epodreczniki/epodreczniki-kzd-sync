package pl.epo.kzd.xml.ftp;

public class FtpSettings {

	private final String host;

	private final Integer port;

	private final String username;

	private final String password;

	public FtpSettings(String host, Integer port, String username, String password) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
	}

	public FtpSettings(String host, String username, String password) {
		this(host, null, username, password);
	}

	public String getHost() {
		return host;
	}

	public Integer getPort() {
		return port;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

}
