package pl.epo.kzd.sync.editor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import lombok.extern.slf4j.Slf4j;

import org.aeonbits.owner.ConfigCache;
import org.apache.commons.io.IOUtils;

import pl.epo.kzd.sync.SyncConfig;
import pl.epo.kzd.sync.resource.Resource;

/**
 * 
 * handles access to KZD Editor resource storage
 *
 */
@Slf4j
public class EditorStorageFacade {

	private static final int PORTAL_RETRY_WAIT = 60 * 1000;

	private static final String XML_EXT = "xml";

	private static final String URL = "%s://www.%s/edit/kzd/resource/%d/%d";

	private static final String PORTAL_SECRET_HEADER = "X-KZD-SYNC-SECRET";

	private static final String BACKUP_DIR = "created-resources";

	private SyncConfig config = ConfigCache.getOrCreate(SyncConfig.class);

	public File downloadXml(long womiId, long womiVersion) throws IOException {
		File file = File.createTempFile(String.format("kzd-input-%d-%d-", womiId, womiVersion), ".xml");
		try (InputStream in = openStream(womiId, womiVersion); OutputStream out = new FileOutputStream(file)) {
			IOUtils.copy(in, out);
		}
		return file;
	}

	public void publishXml(long womiId, long womiVersion, Resource resource) throws IOException {
		backupXml(resource);

		final int maxRetry = 5;
		HttpURLConnection conn = null;
		for (int i = 0; i < maxRetry; i++) {
			try {
				String url = buildUrl(womiId, womiVersion);
				conn = (HttpURLConnection) new URL(url).openConnection();
				conn.setRequestMethod("PUT");
				conn.setRequestProperty(PORTAL_SECRET_HEADER, config.portalSecret());
				conn.setDoOutput(true);
				try (OutputStream out = conn.getOutputStream()) {
					IOUtils.copy(resource.toXml(), out);
				}
				conn.getInputStream().close();
				log.info("resource {} (womi={}/{}) put to portal", resource.getId(), womiId, womiVersion);
				return;
			} catch (IOException e) {
				if ((conn == null || conn.getResponseCode() >= 500) && i + 1 < maxRetry) {
					log.debug("error publishing XML for " + womiId + "/" + womiVersion, e);
					log.warn("error publishing XML for {}/{}: {}, retrying...", womiId, womiVersion, e);
					try {
						Thread.sleep(PORTAL_RETRY_WAIT);
					} catch (InterruptedException e1) {
					}
				} else {
					throw e;
				}
			}
		}
		throw new IllegalStateException();
	}

	private InputStream openStream(long womiId, long womiVersion) throws IOException {
		final int maxRetry = 5;
		HttpURLConnection conn = null;
		for (int i = 0; i < maxRetry; i++) {
			String url = buildUrl(womiId, womiVersion);
			try {
				conn = (HttpURLConnection) new URL(url).openConnection();
				return conn.getInputStream();
			} catch (IOException e) {
				if ((conn == null || conn.getResponseCode() >= 500) && i + 1 < maxRetry) {
					log.debug("error downloading XML for " + womiId + "/" + womiVersion, e);
					log.warn("error downloading XML for {}/{}: {}, retrying...", womiId, womiVersion, e);
					try {
						Thread.sleep(PORTAL_RETRY_WAIT);
					} catch (InterruptedException e1) {
					}
				} else {
					throw e;
				}
			}
		}
		throw new IllegalStateException();
	}

	private String buildUrl(long womiId, long womiVersion) {
		return String.format(URL, config.portalProtocol(), config.portalDomain(), womiId, womiVersion);
	}

	private void backupXml(Resource resource) throws IOException {
		File backupDir = new File(BACKUP_DIR);
		if (!backupDir.exists()) {
			backupDir.mkdirs();
		}

		File file = new File(backupDir, resource.getId() + "." + XML_EXT);
		log.debug("saving resource to " + file.getAbsolutePath());
		try (FileOutputStream fos = new FileOutputStream(file)) {
			IOUtils.copy(resource.toXml(), fos);
		}
	}

}
