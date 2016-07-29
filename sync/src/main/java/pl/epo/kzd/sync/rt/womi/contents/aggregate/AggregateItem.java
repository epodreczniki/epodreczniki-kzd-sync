package pl.epo.kzd.sync.rt.womi.contents.aggregate;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import pl.epo.kzd.sync.resource.ResourceFile;
import pl.epo.kzd.sync.resource.ResourceFileType;
import pl.epo.kzd.sync.rt.womi.WOMI;

class AggregateItem {

	private final ResourceFile resourceFile;

	private final WOMI womi;

	public AggregateItem(ResourceFile resFile, WOMI womi) {
		this.resourceFile = resFile;
		this.womi = womi;
	}

	public JSONObject toJSONObject() {
		JSONObject json = new JSONObject();
		if (this.isAttachment()) {
			json.put("type", "womi-attachment");
			json.put("path", this.getSafeFilename());
		} else {
			json.put("type", "womi");
			if (this.isThumbnail()) {
				json.put("role", "thumbnail");
			}
		}
		json.put("womiId", this.getWomiId());
		json.put("womiVersion", 1);
		if (StringUtils.isNotEmpty(this.getLabel())) {
			json.put("label", this.getLabel());
		}
		return json;
	}

	private String getLabel() {
		return resourceFile.getLabel();
	}

	public String getSafeFilename() {
		return resourceFile.getSafeFilename();
	}

	private boolean isAttachment() {
		return resourceFile.getType() == ResourceFileType.ATTACHMENT;
	}

	private boolean isThumbnail() {
		return resourceFile.getType() == ResourceFileType.THUMBNAIL;
	}

	public boolean isGalleryGraphics() {
		return resourceFile.getType() == ResourceFileType.GALLERY_GRAPHICS;
	}

	public long getWomiId() {
		return womi.getId();
	}

	public String getResourceFileFtpPath() {
		return resourceFile.getFtpFilePath();
	}

	public Date getResourceFileModifiedDate() throws FileNotFoundException, IOException {
		return resourceFile.getModifiedDate();
	}

}
