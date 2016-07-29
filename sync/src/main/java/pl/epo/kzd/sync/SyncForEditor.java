package pl.epo.kzd.sync;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.aeonbits.owner.ConfigCache;

import pl.epo.kzd.sync.editor.EditorStorageFacade;
import pl.epo.kzd.sync.ftp.FtpConnection;
import pl.epo.kzd.sync.resource.Resource;
import pl.epo.kzd.sync.resource.ResourceDescriptor;
import pl.epo.kzd.sync.resource.ResourceFactory;
import pl.epo.kzd.sync.rt.RT;
import pl.epo.kzd.sync.rt.RT.WomiNotFoundException;
import pl.epo.kzd.xml.ftp.FtpSettings;

@Slf4j
public class SyncForEditor {

	private static final int SUPPORTED_WOMI_VERSION = 1;

	private SyncConfig config = ConfigCache.getOrCreate(SyncConfig.class);

	private final FtpSettings ftpSettings;

	private final RT rt;

	private EditorStorageFacade editorStorageFacade = new EditorStorageFacade();

	public SyncForEditor() {
		this.ftpSettings = new FtpSettings(config.ftpHost(), config.ftpPort(), config.ftpUsername(),
				config.ftpPassword());
		this.rt = new RT();
	}

	public void update(String resourceId, long womiId, long womiVersion) throws Exception {
		validateWomiVersion(womiVersion);

		try (FtpConnection ftpConnection = new FtpConnection(ftpSettings)) {
			ResourceDescriptor descriptor = retrieveDescriptor(ftpConnection, resourceId, womiId, womiVersion);
			List<Resource> resources = descriptor.getResources();
			if (resources.size() != 1) {
				throw new IllegalStateException(String.format("a single resource exptected in resource file, %d found",
						resources.size()));
			}
			Resource resource = resources.get(0);
			rt.updateExistingResource(resource, womiId);
			log.debug("finished");
		}
	}

	public void delete(String resourceId, long womiId, long womiVersion) throws IOException {
		validateWomiVersion(womiVersion);
		try {
			rt.deleteResource(womiId);
		} catch (WomiNotFoundException e) {
			log.info("WOMI {} not found", e.getWomiId());
		}
	}

	public void rereadResource(String resourceId) throws Exception {
		long womiId = rt.getExistingResourceWomiId(resourceId);
		update(resourceId, womiId, SUPPORTED_WOMI_VERSION);
	}

	private void validateWomiVersion(long womiVersion) throws IllegalArgumentException {
		if (womiVersion != SUPPORTED_WOMI_VERSION) {
			throw new IllegalArgumentException("WOMI version (" + womiVersion + ") other than 1 is not supported");
		}
	}

	private ResourceDescriptor retrieveDescriptor(FtpConnection ftpConnection, String resourceId, long womiId,
			long womiVersion) throws Exception {
		log.debug("retrieving resource descriptor...");
		ResourceFactory resourceFactory = new ResourceFactory(ftpConnection);

		File descriptorFile = retrieveDescriptorFile(resourceId, womiId, womiVersion);

		Utils.backupDescriptorFilesQuietly(descriptorFile);

		try (InputStream is = new FileInputStream(descriptorFile)) {
			return new ResourceDescriptor(is, resourceFactory);
		}
	}

	private File retrieveDescriptorFile(String resourceId, long womiId, long womiVersion) throws IOException {
		log.debug("saving XML for resource {} (womi={}/{})", resourceId, womiId, womiVersion);
		return editorStorageFacade.downloadXml(womiId, womiVersion);
	}

}
