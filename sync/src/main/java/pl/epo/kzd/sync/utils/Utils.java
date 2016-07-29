package pl.epo.kzd.sync.utils;

public class Utils {

	public static String sanitizeString(String str) {
		return str.replaceAll("[^a-zA-Z0-9.-]", "_");
	}

	private Utils() {
	}
}
