package pl.epo.kzd.sync.rt;

import java.io.IOException;
import java.net.MalformedURLException;

import lombok.extern.slf4j.Slf4j;

import org.aeonbits.owner.ConfigCache;
import org.json.JSONArray;
import org.json.JSONObject;

import pl.epo.kzd.sync.SyncConfig;

@Slf4j
class RTCurrentState {

	private static final String SEARCH_WOMI_URL = "http://%s/repo/searchwomi?customId=%s";

	private static final int MAX_SEARCH_WOMI_READ_RETRYING = 3;

	private SyncConfig config = ConfigCache.getOrCreate(SyncConfig.class);

	public WomiInRt getExistingResourceWomi(String resourceId) throws IOException {
		JSONObject json = readSearchWomiEndpointAndRetryIfNecessary(resourceId);
		JSONArray items = json.getJSONArray("items");
		for (int i = 0; i < items.length(); i++) {
			JSONObject item = items.getJSONObject(i);
			String name = item.getString("name");
			boolean isWomiEmbeddedInResource = name.contains("[embedded]");
			boolean isRtCachingError = name.equals("!ERROR!");
			if (!isWomiEmbeddedInResource && !isRtCachingError) {
				long womiId = item.getLong("id");
				log.debug("analyzing WOMI " + womiId);
				WomiInRt womi = new WomiInRt(womiId);
				if (resourceId.equals(womi.getResourceId())) {
					return womi;
				}
			}
		}
		return null;
	}

	private JSONObject readSearchWomiEndpointAndRetryIfNecessary(String resourceId) throws IOException {
		return readSearchWomiEndpointAndRetryIfNecessary(resourceId, 0);
	}

	private JSONObject readSearchWomiEndpointAndRetryIfNecessary(String resourceId, int depth) throws IOException {
		try {
			return RTUtils.readJson(String.format(SEARCH_WOMI_URL, config.rtDomain(), resourceId));
		} catch (MalformedURLException e) {
			throw new IllegalStateException(e);
		} catch (IOException e) {
			if (depth + 1 < MAX_SEARCH_WOMI_READ_RETRYING) {
				String errorMessage = "Retrying after an I/O error on reading searchWOMI endpoint";
				log.warn(errorMessage);
				log.debug(errorMessage, e);
				try {
					Thread.sleep(30 * 1000);
				} catch (InterruptedException e1) {
				}
				return readSearchWomiEndpointAndRetryIfNecessary(resourceId, depth + 1);
			} else {
				throw e;
			}
		}
	}

}
