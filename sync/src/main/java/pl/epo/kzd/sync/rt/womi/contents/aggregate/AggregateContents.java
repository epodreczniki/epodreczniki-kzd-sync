package pl.epo.kzd.sync.rt.womi.contents.aggregate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class AggregateContents {

	private static final String MANIFEST_FILENAME = "manifest.json";

	private static final String KZD_INFO_FILENAME = "__kzd/info.json";

	private final List<AggregateItem> items = new LinkedList<>();

	public void addItem(AggregateItem aggregateItem) {
		this.items.add(aggregateItem);
	}

	public void save(File file) throws IOException {
		try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file))) {
			AggregateManifest manifest = new AggregateManifest();
			for (AggregateItem item : items) {
				manifest.addItem(item);
			}
			zip.putNextEntry(new ZipEntry(MANIFEST_FILENAME));
			manifest.save(zip);

			KzdInfo info = new KzdInfo();
			for (AggregateItem item : items) {
				info.addItem(item);
			}
			zip.putNextEntry(new ZipEntry(KZD_INFO_FILENAME));
			info.save(zip);
		}
	}

}
