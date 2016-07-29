package pl.epo.kzd.sync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import lombok.extern.slf4j.Slf4j;

import org.aeonbits.owner.ConfigCache;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

import pl.epo.kzd.sync.editor.EditorStorageFacade;
import pl.epo.kzd.sync.ftp.FtpConnection;
import pl.epo.kzd.sync.resource.Resource;
import pl.epo.kzd.sync.resource.ResourceDescriptor;
import pl.epo.kzd.sync.resource.ResourceFactory;
import pl.epo.kzd.sync.rt.RT;
import pl.epo.kzd.xml.KzdXmlValidator;
import pl.epo.kzd.xml.ftp.FtpClient.AuthenticationException;
import pl.epo.kzd.xml.ftp.FtpSettings;

@Slf4j
public class SyncByFtp {

	private static final long SUPPORTED_WOMI_VERSION = 1;

	private SyncConfig config = ConfigCache.getOrCreate(SyncConfig.class);

	private final FtpSettings ftpSettings;

	private EditorStorageFacade editorStorage = new EditorStorageFacade();

	private List<String> ftpFiles;

	public SyncByFtp() throws SocketException, IOException, AuthenticationException {
		this.ftpSettings = new FtpSettings(config.ftpHost(), config.ftpPort(), config.ftpUsername(),
				config.ftpPassword());
	}

	public void run() throws Exception {
		try (FtpConnection ftpConnection = new FtpConnection(ftpSettings)) {
			List<ResourceDescriptor> descriptors = retrieveDescriptors(ftpConnection);
			List<Resource> resources = new LinkedList<>();
			for (ResourceDescriptor descriptor : descriptors) {
				resources.addAll(descriptor.getResources());
			}
			log.info(resources.size() + " resources found");
			skipResourcesWithWrongId(resources);
			validateIdUniqueness(resources);

			RT rt = new RT();
			int createdCount = 0;
			for (Resource resource : resources) {
				Long womiId = rt.saveNewResourceIfNeeded(resource);
				if (womiId != null) {
					createdCount++;
					editorStorage.publishXml(womiId, SUPPORTED_WOMI_VERSION, resource);
				}
			}

			log.info(createdCount + " resources created");
			removeDescriptorsFromFtp(ftpConnection);
			log.debug("finished");
		}
	}

	private void skipResourcesWithWrongId(List<Resource> resources) {
		Iterator<Resource> it = resources.iterator();
		while (it.hasNext()) {
			Resource resource = it.next();
			if (!KzdXmlValidator.isValidResourceId(resource.getId())) {
				log.warn("skipping resource \"%s\" because of invalid identifier", resource.getId());
				it.remove();
			}
		}
	}

	private void validateIdUniqueness(List<Resource> resources) {
		Set<String> ids = new HashSet<>();
		for (Resource resource : resources) {
			String id = resource.getId();
			if (ids.contains(id)) {
				throw new IllegalArgumentException("Id '" + id + "' is not unique");
			} else {
				ids.add(id);
			}
		}
	}

	private List<ResourceDescriptor> retrieveDescriptors(FtpConnection ftpConnection) throws IOException,
			ParserConfigurationException, SAXException {
		log.debug("retrieving resource descriptors...");
		KzdXmlValidator validator = new KzdXmlValidator(ftpConnection, true);
		ResourceFactory resourceFactory = new ResourceFactory(ftpConnection);
		List<ResourceDescriptor> descriptors = new LinkedList<>();

		List<File> descriptorFiles = retrieveDescriptorFiles(ftpConnection);
		log.debug(descriptorFiles.size() + " descriptor file(s) found");

		if (descriptorFiles.size() > 0) {
			Utils.backupDescriptorFilesQuietly(descriptorFiles);
		}

		for (File file : descriptorFiles) {
			log.debug("validating " + file);
			List<String> errors = validator.validate(file);
			if (errors.size() > 0) {
				throw new IllegalStateException("error validating " + file + "\n" + StringUtils.join(errors, '\n'));
			}

			try (InputStream is = new FileInputStream(file)) {
				descriptors.add(new ResourceDescriptor(is, resourceFactory));
			}
		}

		return descriptors;
	}

	private List<File> retrieveDescriptorFiles(FtpConnection ftpConnection) throws IOException {
		this.ftpFiles = ftpConnection.listFiles(config.ftpXmlDirectory());
		List<File> localFiles = new LinkedList<>();
		for (String ftpFile : ftpFiles) {
			if (ftpFile.endsWith(".xml")) {
				log.info("descriptor file found: " + ftpFile);
				File file = File.createTempFile("kzd-input-", ".xml");
				try (FileOutputStream fos = new FileOutputStream(file)) {
					ftpConnection.retrieveFileStream(ftpFile, fos);
				}
				localFiles.add(file);
			}
		}
		return localFiles;
	}

	private void removeDescriptorsFromFtp(FtpConnection ftpConnection) {
		for (String ftpFile : ftpFiles) {
			log.info("removing FTP file: " + ftpFile);
			try {
				ftpConnection.deleteFile(ftpFile);
			} catch (IOException e) {
				log.error("error deleting " + ftpFile, e);
			}
		}
	}

}
