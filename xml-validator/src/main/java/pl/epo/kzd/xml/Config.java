package pl.epo.kzd.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {

	private static final String CONF_RESOURCE_FILE = "conf.properties";

	private static Properties properties;

	public static String getSchemaUrl() throws IOException {
		return getProperties().getProperty("schemaUrl");
	}

	public static String getCategoryListUrl() throws IOException {
		return getProperties().getProperty("categoryListUrl");
	}

	private static Properties getProperties() throws IOException {
		if (properties == null) {
			synchronized (Config.class) {
				if (properties == null) {
					properties = new Properties();
					try (InputStream is = Config.class.getResourceAsStream(CONF_RESOURCE_FILE)) {
						properties.load(is);
					}
				}
			}
		}
		return properties;
	}

	private Config() {
	}

}
