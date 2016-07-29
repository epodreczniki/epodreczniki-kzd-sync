package pl.epo.kzd.xml.category;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;

import pl.epo.kzd.xml.Config;

public class CategoryValidator {

	private static final String JSON_ENCODING = "utf-8";

	private static List<String> categories;

	public boolean isValid(String category) throws IOException {
		return getCategories().contains(category);
	}

	protected List<String> getCategories() throws IOException {
		if (categories == null) {
			synchronized (CategoryValidator.class) {
				if (categories == null) {
					categories = loadCategoryList();
				}
			}
		}
		return categories;
	}

	private List<String> loadCategoryList() throws IOException {
		List<String> categories = new LinkedList<>();
		try {
			JSONArray array = new JSONArray(readCategoryListApi());
			for (int i = 0; i < array.length(); i++) {
				categories.add(array.getString(i));
			}
			return categories;
		} catch (JSONException e) {
			throw new IllegalStateException(e);
		}
	}

	private String readCategoryListApi() throws IOException {
		URL url = getCategoryListApiUrl();
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		return IOUtils.toString(conn.getInputStream(), JSON_ENCODING);
	}

	private URL getCategoryListApiUrl() throws IOException {
		return new URL(Config.getCategoryListUrl());
	}

}
