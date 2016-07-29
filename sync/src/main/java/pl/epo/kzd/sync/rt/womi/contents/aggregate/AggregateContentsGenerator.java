package pl.epo.kzd.sync.rt.womi.contents.aggregate;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import pl.epo.kzd.sync.resource.ResourceFile;
import pl.epo.kzd.sync.rt.womi.WOMI;
import pl.epo.kzd.sync.rt.womi.contents.ContentsGenerator;

public class AggregateContentsGenerator extends ContentsGenerator {

	private static final String AGGREGATE_FILENAME = "aggregate.zip";

	private static final String INTERACTIVE_PACKAGE_CLASSIC = "INTERACTIVE_PACKAGE_CLASSIC";

	private final AggregateContents contents = new AggregateContents();

	public AggregateContentsGenerator(File womiDir) {
		super(womiDir);
	}

	@Override
	public void close() throws IOException {
		try {
			File contentsDir = super.createContentsDirAndMainFile(INTERACTIVE_PACKAGE_CLASSIC, AGGREGATE_FILENAME);
			contents.save(new File(contentsDir, AGGREGATE_FILENAME));
		} catch (ParserConfigurationException | TransformerException e) {
			throw new IOException(e);
		}
	}

	public void addItem(ResourceFile resFile, WOMI womi) {
		contents.addItem(new AggregateItem(resFile, womi));
	}

}
