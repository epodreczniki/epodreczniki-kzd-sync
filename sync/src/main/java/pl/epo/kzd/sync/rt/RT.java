package pl.epo.kzd.sync.rt;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import pl.epo.kzd.sync.resource.Resource;
import pl.epo.kzd.sync.resource.ResourceFile;
import pl.epo.kzd.sync.rt.womi.WOMI;
import pl.epo.kzd.sync.rt.womi.WOMIFactory;

@Slf4j
public class RT {

	private final WOMIFactory womiFactory = new WOMIFactory();

	private final RTClient rtClient = new RTClient();

	private final RTCurrentState rtCurrentState = new RTCurrentState();

	/**
	 * @return created WOMI id or null
	 */
	public Long saveNewResourceIfNeeded(Resource resource) throws Exception {
		log.debug("analyzing " + resource);
		WomiInRt existingResourceWomi = rtCurrentState.getExistingResourceWomi(resource.getId());
		if (existingResourceWomi != null) {
			log.debug(String.format("%s found in RT as WOMI %d - skipping", resource, existingResourceWomi.getId()));
			return null;
		} else {
			log.info("creating new WOMI for " + resource);
			return saveNewResource(resource);
		}
	}

	public long updateExistingResource(Resource resource, long knownWomiId) throws IOException,
			ParserConfigurationException, TransformerException {
		WomiInRt resourceWomi = new WomiInRt(knownWomiId);
		List<WOMI> embeddedWOMIs = new LinkedList<>();
		List<Long> oldEmbeddedWomiIdsToBeDeleted = new LinkedList<>(resourceWomi.getEmbeddedWomiIds());
		boolean anyUpdatesOfEmbedded = false;
		log.debug("analyzing " + resource.getNumberOfFiles() + " embedded files of " + resourceWomi);
		for (ResourceFile resFile : resource.getFiles()) {
			EmbeddedWomiInfo info = resourceWomi.getEmbeddedWomiInfoByFtpFilePath(resFile.getFtpFilePath());
			final boolean typesDiffer = info != null ? info.typesDiffer(resFile.getType()) : false;
			final Date resourceFileModifiedDate = tryToGetModifiedDate(resFile);
			if (info == null
					|| (resourceFileModifiedDate != null && (typesDiffer || resourceFileModifiedDate.after(info
							.getModifiedDate())))) {
				if (typesDiffer) {
					log.debug("type of {} differs from its previously saved type", resFile);
				}
				WOMI womi = womiFactory.createEmbeddedWOMI(resource, resFile);
				long embeddedWomiId = rtClient.uploadWOMI(womi);
				womi.setId(embeddedWomiId);
				embeddedWOMIs.add(womi);
				anyUpdatesOfEmbedded = true;
				log.info("embedded WOMI of " + resourceWomi + " saved as " + embeddedWomiId + " for " + resFile);
			} else {
				if (typesDiffer && resourceFileModifiedDate == null) {
					log.warn("cannot update type of embedded WOMI from non-existing FTP file: {}",
							resFile.getFtpFilePath());
				}
				log.debug("skipping updates for " + resFile);
				embeddedWOMIs.add(new WOMI(info.getId(), resFile));
				oldEmbeddedWomiIdsToBeDeleted.remove(info.getId());
				if (info.labelDiffer(resFile)) {
					anyUpdatesOfEmbedded = true;
				}
			}
		}

		if (anyUpdatesOfEmbedded || oldEmbeddedWomiIdsToBeDeleted.size() > 0 || resourceWomi.metadataDiffer(resource)) {
			WOMI womi = womiFactory.createMainWOMI(resource, embeddedWOMIs, resourceWomi.getId());
			long womiId = rtClient.uploadWOMI(womi);
			log.info("WOMI " + womiId + " saved for " + resourceWomi);

			if (womiId != resourceWomi.getId()) {
				log.error("WOMI IDs differ! " + womiId + " vs. old " + resourceWomi.getId());
			}
			cleanup(embeddedWOMIs, womi);
		} else {
			log.debug("skipping updates for " + resourceWomi);
		}

		if (oldEmbeddedWomiIdsToBeDeleted.size() > 0) {
			log.info("removing old embedded WOMIs of " + resourceWomi + ": " + oldEmbeddedWomiIdsToBeDeleted);
			for (long id : oldEmbeddedWomiIdsToBeDeleted) {
				try {
					rtClient.removeWOMI(id);
				} catch (IOException e) {
					log.warn("error removing WOMI " + id, e);
				}
			}
		}

		return resourceWomi.getId();
	}

	public void deleteResource(long womiId) throws IOException, WomiNotFoundException {
		List<Long> embeddedWomiIds;
		try {
			embeddedWomiIds = new WomiInRt(womiId).getEmbeddedWomiIds();
		} catch (FileNotFoundException e) {
			throw new WomiNotFoundException(womiId);
		}

		log.info("removing embedded WOMIs of {}: {}", womiId, embeddedWomiIds);
		for (long embeddedWomiId : embeddedWomiIds) {
			try {
				rtClient.removeWOMI(embeddedWomiId);
			} catch (IOException e) {
				log.warn("error removing WOMI " + embeddedWomiId, e);
			}
		}

		log.info("removing WOMI {}", womiId);
		rtClient.removeWOMI(womiId);
	}

	public long getExistingResourceWomiId(String resourceId) throws IOException {
		return rtCurrentState.getExistingResourceWomi(resourceId).getId();
	}

	private long saveNewResource(Resource resource) throws IOException, ParserConfigurationException,
			TransformerException {
		List<WOMI> embeddedWOMIs = new LinkedList<>();
		for (ResourceFile resFile : resource.getFiles()) {
			WOMI womi = womiFactory.createEmbeddedWOMI(resource, resFile);
			long womiId = rtClient.uploadWOMI(womi);
			womi.setId(womiId);
			embeddedWOMIs.add(womi);
			log.info("embedded WOMI saved as " + womiId + " for " + resFile);
		}

		WOMI womi = womiFactory.createMainWOMI(resource, embeddedWOMIs);
		long womiId = rtClient.uploadWOMI(womi);
		log.info("WOMI saved: " + womiId);

		cleanup(embeddedWOMIs, womi);

		return womiId;
	}

	private void cleanup(List<WOMI> embeddedWOMIs, WOMI womi) {
		for (WOMI embedded : embeddedWOMIs) {
			embedded.cleanup();
		}
		womi.cleanup();
	}

	private Date tryToGetModifiedDate(ResourceFile resFile) throws IOException {
		try {
			return resFile.getModifiedDate();
		} catch (FileNotFoundException e) {
			log.warn("resource file {} does not exists in FTP", resFile.getFtpFilePath());
			return null;
		}
	}

	public static class WomiNotFoundException extends Exception {

		@Getter
		private final long womiId;

		public WomiNotFoundException(long womiId) {
			super(womiId + "");
			this.womiId = womiId;
		}

	}

}
