package pl.epo.kzd.sync.rt.womi.contents.attachment;

import java.io.IOException;
import java.io.OutputStream;

import org.json.JSONException;
import org.json.JSONObject;

import pl.epo.kzd.sync.rt.RTUtils;

class AttachmentManifest {

	private final JSONObject json = new JSONObject();

	public AttachmentManifest(String attachmentFilename) {
		json.put("engine", "womi_attachment");
		json.put("version", "0.1");
		json.put("mainFile", attachmentFilename);
		json.put("parameters", new JSONObject());
	}

	public void save(OutputStream out) throws JSONException, IOException {
		RTUtils.writeJsonToStream(json, out);
	}

}
