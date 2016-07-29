package pl.epo.kzd.sync.rt;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aeonbits.owner.ConfigCache;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import pl.epo.kzd.sync.SyncConfig;

public class RTUtils {

	private static final String PUBLICATION_URL = "https://%s/repo/rt/publication/%d";

	private static final String DEFAULT_JSON_ENCODING = "UTF-8";

	private static final String WOMI_ID_PATTERN_SOURCE = "location.href = 'https://%s/repo/rt/docmetadata\\?id=(\\d+)&from=publication'";

	private static Pattern womiIdPattern;

	private static SyncConfig config = ConfigCache.getOrCreate(SyncConfig.class);

	public static long convertPublicationIdToWomiId(long publicationId) throws IOException {
		URL url = new URL(String.format(PUBLICATION_URL, config.rtDomain(), publicationId));
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		try (@SuppressWarnings("resource")
		BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
			String line;
			while ((line = r.readLine()) != null) {
				Matcher m = getWomiIdPattern().matcher(line);
				if (m.find()) {
					String idStr = m.group(1);
					return Long.parseLong(idStr);
				}
			}
		}
		throw new IllegalArgumentException("Publication #" + publicationId + " is not a WOMI");
	}

	public static JSONObject readJson(String url) throws MalformedURLException, FileNotFoundException, IOException {
		URL url_ = new URL(url);
		HttpURLConnection conn = (HttpURLConnection) url_.openConnection();

		if (conn.getResponseCode() == 404) {
			throw new FileNotFoundException(url);
		} else if (conn.getResponseCode() != 200) {
			throw new IOException(
					String.format("HTTP response status %d returned for: %s", conn.getResponseCode(), url));
		}

		String encoding = conn.getContentEncoding();
		if (StringUtils.isEmpty(encoding)) {
			encoding = DEFAULT_JSON_ENCODING;
		}
		try (InputStream in = conn.getInputStream()) {
			String body = IOUtils.toString(in, encoding);
			if (StringUtils.isEmpty(body)) {
				throw new IOException("URL returned status 200 with empty body: " + url);
			}
			return new JSONObject(body);
		}
	}

	public static DateFormat getISO8601DateFormat() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		return dateFormat;
	}

	public static void writeJsonToStream(JSONObject json, OutputStream out) throws JSONException, IOException {
		IOUtils.write(json.toString(4), out, "utf-8");
	}

	private static Pattern getWomiIdPattern() {
		if (womiIdPattern == null) {
			synchronized (RTUtils.class) {
				if (womiIdPattern == null) {
					String rtDomain = config.rtDomain().replaceAll("\\.", "\\.");
					womiIdPattern = Pattern.compile(String.format(WOMI_ID_PATTERN_SOURCE, rtDomain));
				}
			}
		}
		return womiIdPattern;
	}

	private RTUtils() {
	}

}
