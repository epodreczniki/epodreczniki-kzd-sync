package pl.epo.kzd.sync.rt.womi.contents.aggregate;

import java.io.IOException;
import java.io.OutputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import pl.epo.kzd.sync.rt.RTUtils;

class KzdInfo {

	private final JSONObject json = new JSONObject();

	private final JSONArray embeddedWomis;

	public KzdInfo() {
		embeddedWomis = new JSONArray();
		json.put("embeddedWomis", embeddedWomis);
	}

	public void addItem(AggregateItem item) throws IOException {
		JSONObject embedded = new JSONObject();
		embedded.put("id", item.getWomiId());
		embedded.put("sourceFtpPath", item.getResourceFileFtpPath());
		embedded.put("sourceModifiedDate", RTUtils.getISO8601DateFormat().format(item.getResourceFileModifiedDate()));
		embeddedWomis.put(embedded);
	}

	public void save(OutputStream os) throws JSONException, IOException {
		RTUtils.writeJsonToStream(json, os);
	}

}
