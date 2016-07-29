package pl.epo.kzd.sync.rt;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.aeonbits.owner.ConfigCache;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import pl.epo.kzd.sync.SyncConfig;
import pl.epo.kzd.sync.resource.Resource;
import pl.epo.kzd.sync.resource.ResourceFile;
import pl.epo.kzd.sync.resource.ResourceFileType;
import pl.epo.kzd.xml.uspp.USPP;

@Getter
@Slf4j
class WomiInRt {

	private static final String METADATA_URL = "http://%s/repo/womi-metadata/%d";

	private static final String MANIFEST_URL = "http://%s/repo/womi-manifest/%d";

	private static final String KZD_INFO_URL = "http://%s/repo/womi/%d/package/classic?path=__kzd/info.json";

	private final long id;

	private final String resourceId;

	private final List<Long> embeddedWomiIds;

	private final Map<String, EmbeddedWomiInfo> embeddedWomisByFtpPaths;

	private final JSONObject metadataJson;

	private final JSONObject manifestJson;

	private SyncConfig config = ConfigCache.getOrCreate(SyncConfig.class);

	WomiInRt(long id) throws FileNotFoundException, IOException {
		this.id = id;
		this.metadataJson = retrieveMetadataJson(id);
		if (!"kzd".equals(metadataJson.getString("purpose"))) {
			throw new IllegalArgumentException(String.format("WOMI %d is not a KZD resource", id));
		}
		this.manifestJson = retrieveManifestJson(id);
		this.resourceId = this.metadataJson.getString("customId");
		this.embeddedWomiIds = retrieveEmbeddedWomiIds(id);
		this.embeddedWomisByFtpPaths = retrieveEmbeddedWomis(id);
	}

	@Override
	public String toString() {
		return resourceId + " (womi=" + id + ")";
	}

	public List<Long> getEmbeddedWomiIds() {
		return Collections.unmodifiableList(embeddedWomiIds);
	}

	private JSONObject retrieveMetadataJson(long womiId) throws IOException {
		JSONObject metadataJson = RTUtils.readJson(String.format(METADATA_URL, config.rtDomain(), womiId));
		return metadataJson;
	}

	private JSONObject retrieveManifestJson(long womiId) throws IOException {
		JSONObject manifest = RTUtils.readJson(String.format(MANIFEST_URL, config.rtDomain(), womiId));
		return manifest;
	}

	private List<Long> retrieveEmbeddedWomiIds(long womiId) throws IOException {
		List<Long> embedded = new LinkedList<Long>();
		JSONArray jsonIds = manifestJson.getJSONArray("womiIds");
		for (int i = 0; i < jsonIds.length(); i++) {
			embedded.add(jsonIds.getLong(i));
		}
		return embedded;
	}

	private Map<String, EmbeddedWomiInfo> retrieveEmbeddedWomis(long womiId) throws IOException {
		Map<String, EmbeddedWomiInfo> map = new HashMap<>();
		try {
			JSONObject json = RTUtils.readJson(String.format(KZD_INFO_URL, config.rtDomain(), womiId));
			JSONArray embeddedWomis = json.getJSONArray("embeddedWomis");
			for (int i = 0; i < embeddedWomis.length(); i++) {
				JSONObject info = embeddedWomis.getJSONObject(i);
				long id = info.getLong("id");
				String path = info.getString("sourceFtpPath");
				Date modifiedDate = RTUtils.getISO8601DateFormat().parse(info.getString("sourceModifiedDate"));
				String label = getLabelFromManifestByWomiId(id);
				map.put(path, new EmbeddedWomiInfo(id, path, modifiedDate, label, getWomiType(id), isAttachment(id)));
			}
		} catch (ParseException e) {
			throw new IllegalStateException(e);
		}
		return map;
	}

	private String getWomiType(long womiId) {
		try {
			JSONObject metadataJson = retrieveMetadataJson(womiId);
			return metadataJson.getString("womiType");
		} catch (IOException e) {
			log.warn("error retrieving metadata.json for WOMI " + womiId, e);
			return null;
		}
	}

	private Boolean isAttachment(long womiId) {
		try {
			JSONObject manifestJson = retrieveManifestJson(womiId);
			return manifestJson.has("engine") && "womi_attachment".equals(manifestJson.getString("engine"));
		} catch (IOException e) {
			log.warn("error retrieving manifest.json for WOMI " + womiId, e);
			return null;
		}
	}

	private String getLabelFromManifestByWomiId(long id) {
		JSONArray items = manifestJson.getJSONArray("items");
		for (int i = 0; i < items.length(); i++) {
			JSONObject item = items.getJSONObject(i);
			if (item.has("womiId") && item.getLong("womiId") == id) {
				if (item.has("label")) {
					return item.getString("label");
				} else {
					return null;
				}
			}
		}
		return null;
	}

	public EmbeddedWomiInfo getEmbeddedWomiInfoByFtpFilePath(String ftpFilePath) {
		return embeddedWomisByFtpPaths.get(ftpFilePath);
	}

	public boolean metadataDiffer(Resource r) {
		JSONObject extended = metadataJson.getJSONObject("extended");

		if (differ(r.getId(), metadataJson.getString("customId"))) {
			return true;
		}
		if (differ(r.getTitle(), metadataJson.getString("title"))) {
			return true;
		}
		if (differ(r.getAuthor(), metadataJson.getString("author"))) {
			return true;
		}
		if (differ(r.getDescription(), extended.getString("description"))) {
			return true;
		}
		if (differ(r.getAltText(), metadataJson.getString("alternativeText"))) {
			return true;
		}
		if (differ(r.getLicense(), metadataJson.getString("license"))) {
			return true;
		}
		if (differ(r.getOrigin(), extended.getString("origin"))) {
			return true;
		}
		if (differ(r.getRecipient().name().toLowerCase(), extended.getString("recipient"))) {
			return true;
		}
		if (differ(r.getCategory(), extended.getString("category"))) {
			return true;
		}

		if (differ(r.getKeywords(), toList(metadataJson.getJSONArray("keywords")))) {
			return true;
		}

		List<String> resourceLearningObjectives = new LinkedList<>(r.getLearningObjectives());
		resourceLearningObjectives.remove(USPP.getSpecialAllIdentifier());
		List<String> womiLearningObjectives = toList(extended.getJSONArray("learningObjectives"));
		if (differ(resourceLearningObjectives, womiLearningObjectives)) {
			return true;
		}

		if (differ(r.getEnvironments(), toList(extended.getJSONArray("environments")))) {
			return true;
		}

		return false;
	}

	private boolean differ(Collection<String> col1, Collection<String> col2) {
		Set<String> set1 = new HashSet<>(normalize(col1));
		Set<String> set2 = new HashSet<>(normalize(col2));
		boolean differ = !set1.equals(set2);
		if (differ) {
			log.debug(String.format("the collections differ for WOMI %d: %s and %s", id, col1, col2));
		}
		return differ;
	}

	static boolean differ(String v1, String v2) {
		if (v1 == null && v2 == null) {
			return false;
		}
		if (v1 == null || v2 == null) {
			log.debug(String.format("the values differ: \"%s\" and \"%s\"", v1, v2));
			return true;
		}

		boolean differ = !normalize(v1).equals(normalize(v2));
		if (differ) {
			log.debug(String.format("the values differ: \"%s\" and \"%s\"", v1, v2));
		}
		return differ;
	}

	private static String normalize(String val) {
		return val.trim().replaceAll("\\s+", " ");
	}

	private Collection<String> normalize(Collection<String> col) {
		List<String> list = new LinkedList<>();
		for (String string : col) {
			list.add(normalize(string));
		}
		return list;
	}

	private List<String> toList(JSONArray jsonArrayOfStrings) {
		List<String> list = new LinkedList<>();
		for (int i = 0; i < jsonArrayOfStrings.length(); i++) {
			list.add(jsonArrayOfStrings.getString(i));
		}
		return list;
	}
}

@Data
class EmbeddedWomiInfo {

	private final long id;

	private final String path;

	private final Date modifiedDate;

	private final String label;

	/**
	 * null if unable to check
	 */
	private final String womiTypeInMetadata;

	/**
	 * null if unable to check
	 */
	private final Boolean isAttachment;

	public boolean labelDiffer(ResourceFile resFile) {
		String resFileLabel = resFile.getLabel();
		if (StringUtils.isEmpty(resFileLabel)) {
			resFileLabel = null;
		}
		if (WomiInRt.differ(this.label, resFileLabel)) {
			return true;
		}

		return false;
	}

	public boolean typesDiffer(ResourceFileType type) {
		switch (type) {
			case THUMBNAIL:
			case GRAPHICS:
			case GALLERY_GRAPHICS:
				return !"graphics".equals(womiTypeInMetadata);
			case MOVIE:
				return !"movie".equals(womiTypeInMetadata);
			case SOUND:
				return !"sound".equals(womiTypeInMetadata);
			case INTERACTIVE:
				return !"interactive".equals(womiTypeInMetadata) || isAttachment != false;
			case ATTACHMENT:
				return !"interactive".equals(womiTypeInMetadata) || isAttachment != true;
			default:
				throw new IllegalStateException("not supported resource file type: " + type);
		}
	}
}