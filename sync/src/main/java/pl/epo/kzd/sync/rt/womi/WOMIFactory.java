package pl.epo.kzd.sync.rt.womi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.aeonbits.owner.ConfigCache;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import pl.epo.kzd.sync.SyncConfig;
import pl.epo.kzd.sync.resource.Resource;
import pl.epo.kzd.sync.resource.ResourceFile;
import pl.epo.kzd.sync.resource.ResourceFileType;
import pl.epo.kzd.sync.rt.womi.contents.ClassicContentsGenerator;
import pl.epo.kzd.sync.rt.womi.contents.aggregate.AggregateContentsGenerator;
import pl.epo.kzd.sync.rt.womi.contents.attachment.AttachmentContentsGenerator;

@Slf4j
public class WOMIFactory {

	public WOMI createMainWOMI(Resource resource, List<WOMI> embeddedWOMIs) throws IOException,
			ParserConfigurationException, TransformerException {
		return createMainWOMI(resource, embeddedWOMIs, null);
	}

	/**
	 * @param womiId id for created WOMI
	 */
	public WOMI createMainWOMI(Resource resource, List<WOMI> embeddedWOMIs, Long womiId) throws IOException,
			ParserConfigurationException, TransformerException {
		File dir = Files.createTempDirectory("kzd-womi-" + resource.getId().replace(" ", "") + "-").toFile();
		log.debug("creating WOMI for " + resource + ", (womi=" + womiId + ") in " + dir);
		WOMISketch sketch = new WOMISketch(womiId, WOMISketchDataSource.build(resource), dir);
		sketch.setEmbeddedWOMIs(embeddedWOMIs);
		sketch.generate();
		return new WOMI(dir);
	}

	public WOMI createEmbeddedWOMI(Resource resource, ResourceFile resourceFile) throws IOException,
			ParserConfigurationException, TransformerException {
		File dir = Files.createTempDirectory("kzd-womi-emb-").toFile();
		log.debug("creating embedded WOMI for " + resourceFile + " in " + dir);
		WOMISketch sketch = new WOMISketch(null, WOMISketchDataSource.build(resource, resourceFile), dir);
		sketch.generate();
		return new WOMI(dir, resourceFile);
	}

	public WOMI createCustomWomi(final File file) throws IOException, ParserConfigurationException,
			TransformerException {
		File dir = Files.createTempDirectory("custom-womi-").toFile();
		log.debug("creating custom WOMI for " + file + " in " + dir);
		WOMISketch sketch = new WOMISketch(null, new WOMISketchDataSource() {

			@Override
			public Resource getResource() {
				return null;
			}

			@Override
			public int getNumberOfFiles() {
				return 1;
			}

			@Override
			public String getMachineName() {
				return file.getName();
			}

			@Override
			public List<ResourceFile> getFiles() {
				LinkedList<ResourceFile> list = new LinkedList<>();
				list.add(new LocalFile(file));
				return list;
			}
		}, dir);
		sketch.generate();
		return new WOMI(dir, null);
	}

}

class WOMISketch {

	private static final String PUBLICATION_PROPS_TEMPLATE_RESOURCE = "/womi-template/publication.properties";

	private static final String PUBLICATION_PROPS_FILENAME = "publication.properties";

	private static final String METADATA_FILENAME = "metadata.xml";

	private SyncConfig config = ConfigCache.getOrCreate(SyncConfig.class);

	private final File womiDir;

	private final WOMISketchDataSource dataSource;

	private final Long womiId;

	@Setter
	private List<WOMI> embeddedWOMIs;

	public WOMISketch(Long womiId, WOMISketchDataSource dataSource, File dir) throws IOException,
			ParserConfigurationException, TransformerException {
		this.dataSource = dataSource;
		this.womiId = womiId;

		File womiDir = new File(dir, "womi");
		womiDir.mkdir();
		this.womiDir = womiDir;
	}

	public void generate() throws IOException, ParserConfigurationException, TransformerException {
		generatePublicationProps();
		generateMetadata();
		generateMainFileAndContents();
	}

	private void generateMainFileAndContents() throws IOException, ParserConfigurationException {
		if (dataSource.getNumberOfFiles() == 1) {
			ResourceFile resFile = dataSource.getFiles().get(0);
			if (resFile.getType() != ResourceFileType.ATTACHMENT) {
				new ClassicContentsGenerator(womiDir, resFile).close();
			} else {
				new AttachmentContentsGenerator(womiDir, resFile).close();
			}
		} else {
			try (AggregateContentsGenerator gen = new AggregateContentsGenerator(womiDir)) {
				for (ResourceFile resFile : dataSource.getFiles()) {
					WOMI embedded = findWOMIFor(resFile);
					gen.addItem(resFile, embedded);
				}
			}
		}
	}

	private WOMI findWOMIFor(ResourceFile resFile) {
		if (embeddedWOMIs != null) {
			for (WOMI womi : embeddedWOMIs) {
				if (womi.getSource() == resFile) {
					return womi;
				}
			}
		}
		throw new IllegalStateException("Cannot find WOMI created for " + resFile);
	}

	private void generateMetadata() throws IOException, ParserConfigurationException, TransformerException {
		Resource resource = dataSource.getResource();
		File f = new File(womiDir, METADATA_FILENAME);
		Metadata metadata = new Metadata(f);
		if (resource != null) {
			metadata.saveFrom(resource);
		} else {
			metadata.saveEmpty();
		}
	}

	private void generatePublicationProps() throws IOException {
		String pub = IOUtils.toString(this.getClass().getResourceAsStream(PUBLICATION_PROPS_TEMPLATE_RESOURCE));
		pub = pub.replace("${name}", dataSource.getMachineName());
		pub = pub.replace("${directory.id}", config.rtDirectoryId() + "");

		if (womiId != null) {
			if (!pub.endsWith("\n")) {
				pub += "\n";
			}
			pub += "edition.id=" + womiId + "\n";
		}

		File file = new File(womiDir, PUBLICATION_PROPS_FILENAME);
		FileUtils.write(file, pub, "utf-8");
	}
}

abstract class WOMISketchDataSource {

	public abstract String getMachineName();

	public abstract Resource getResource();

	public abstract int getNumberOfFiles();

	public abstract List<ResourceFile> getFiles();

	public static WOMISketchDataSource build(final Resource resource) {
		return new WOMISketchDataSource() {

			@Override
			public Resource getResource() {
				return resource;
			}

			@Override
			public int getNumberOfFiles() {
				return resource.getNumberOfFiles();
			}

			@Override
			public String getMachineName() {
				return "[KZD] " + resource.getId();
			}

			@Override
			public List<ResourceFile> getFiles() {
				return resource.getFiles();
			}
		};
	}

	public static WOMISketchDataSource build(final Resource resource, final ResourceFile resFile) {
		return new WOMISketchDataSource() {

			private List<ResourceFile> files;

			@Override
			public Resource getResource() {
				return null;
			}

			@Override
			public int getNumberOfFiles() {
				return 1;
			}

			@Override
			public String getMachineName() {
				return "[KZD] " + resource.getId() + " [embedded] " + resFile.getSafeFilename();
			}

			@Override
			public List<ResourceFile> getFiles() {
				if (files == null) {
					files = new LinkedList<>();
					files.add(resFile);
				}
				return files;
			}
		};
	}
}
