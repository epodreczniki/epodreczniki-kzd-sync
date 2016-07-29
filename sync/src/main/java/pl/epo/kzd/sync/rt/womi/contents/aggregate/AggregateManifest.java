package pl.epo.kzd.sync.rt.womi.contents.aggregate;

import java.io.IOException;
import java.io.OutputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import pl.epo.kzd.sync.rt.RTUtils;

class AggregateManifest {

	private final JSONObject json = new JSONObject();

	private final JSONArray items;

	private final JSONArray womiIds;

	private JSONArray imageGalleryWomis;

	public AggregateManifest() {
		json.put("engine", "womi_aggregate");
		json.put("version", "1.0");
		this.items = new JSONArray();
		json.put("items", items);
		this.womiIds = new JSONArray();
		json.put("womiIds", womiIds);
	}

	public void addItem(AggregateItem item) {
		womiIds.put(item.getWomiId());

		if (!item.isGalleryGraphics()) {
			items.put(item.toJSONObject());
		} else {
			if (imageGalleryWomis == null) {
				generateImageGallery();
			}
			imageGalleryWomis.put(item.toJSONObject());
		}
	}

	private void generateImageGallery() {
		imageGalleryWomis = new JSONArray();

		JSONObject galleryItem = new JSONObject();
		galleryItem.put("type", "image_gallery");
		galleryItem.put("womis", imageGalleryWomis);
		items.put(galleryItem);
	}

	public void save(OutputStream os) throws JSONException, IOException {
		RTUtils.writeJsonToStream(json, os);
	}

}
