package pl.epo.kzd.sync.resource;

import lombok.Getter;

public enum ResourceFileType {
	ATTACHMENT("file"), THUMBNAIL, GRAPHICS, MOVIE, SOUND, INTERACTIVE, GALLERY_GRAPHICS("gallery-graphics");

	public static ResourceFileType valueOfXmlTagName(String tagName) {
		for (ResourceFileType type : values()) {
			if (tagName.equals(type.xmlTagName)) {
				return type;
			}
		}
		throw new IllegalArgumentException(tagName);
	}

	@Getter
	private final String xmlTagName;

	private ResourceFileType() {
		this.xmlTagName = this.name().toLowerCase();
	}

	private ResourceFileType(String xmlTagName) {
		this.xmlTagName = xmlTagName;
	}

}
