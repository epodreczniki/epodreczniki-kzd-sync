package pl.epo.kzd.xml.uspp;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class USPP {

	private final static String SPECIAL_ALL_IDENTIFIER = "ALL";

	private final static String USPP_ABILITY_URL = "http://uspp.pl/api/descriptions/%s";

	public static String getSpecialAllIdentifier() {
		return SPECIAL_ALL_IDENTIFIER;
	}

	private final Set<String> cacheSuccess = new HashSet<>();

	private final Set<String> cacheFail = new HashSet<>();

	public synchronized boolean isValidIdentifier(String id) throws IOException {
		if (id == null || id.trim().length() == 0) {
			return false;
		}

		if (SPECIAL_ALL_IDENTIFIER.equals(id)) {
			return true;
		}

		if (cacheSuccess.contains(id)) {
			return true;
		} else if (cacheFail.contains(id)) {
			return false;
		}

		boolean r = checkApiFor(id);
		if (r) {
			cacheSuccess.add(id);
		} else {
			cacheFail.add(id);
		}
		return r;
	}

	private boolean checkApiFor(String id) throws MalformedURLException, IOException, ProtocolException {
		URL url = new URL(String.format(USPP_ABILITY_URL, id));
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("HEAD");
		int responseCode = conn.getResponseCode();
		return responseCode == 200;
	}

}
