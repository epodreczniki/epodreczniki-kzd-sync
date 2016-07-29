package pl.epo.kzd.sync.rt;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.aeonbits.owner.ConfigCache;
import org.apache.commons.lang3.math.NumberUtils;

import pl.epo.kzd.sync.SyncConfig;
import pl.epo.kzd.sync.rt.womi.WOMI;

@Slf4j
public class RTClient {

	private static final String UPLOADER_SCRIPT = "uploader.sh";

	private static final String DESTROYER_SCRIPT = "destroyer.sh";

	private static final String EPRT_TOOLS_DIR = "eprt-tools";

	private static final int MAX_SCRIPT_RUN_RETRYING = 3;

	private SyncConfig config = ConfigCache.getOrCreate(SyncConfig.class);

	private final File eprtDir;

	public RTClient() {
		this.eprtDir = new File(EPRT_TOOLS_DIR);
		if (!eprtDir.exists()) {
			throw new IllegalStateException("Cannot find " + EPRT_TOOLS_DIR + " directory\n" + "Save it into "
					+ EPRT_TOOLS_DIR + " directory");
		}
	}

	public void removeWOMI(long womiId) throws IOException {
		log.debug("calling " + DESTROYER_SCRIPT + " for " + womiId);
		ProcessBuilder pb = new ProcessBuilder(createEprtToolsCall(DESTROYER_SCRIPT, "-w", womiId + ""));
		pb.directory(eprtDir);

		List<String> stdout = runAndRetryIfNecessary(pb, 0);

		String lastLine = getLastNotEmptyLine(stdout);
		if (lastLine == null || !lastLine.contains("INFO")) {
			throw new IOException("removing WOMI " + womiId + " failed, script returned: " + lastLine);
		}
	}

	/**
	 * @return id of the created WOMI
	 */
	public long uploadWOMI(WOMI womi) throws IOException {
		log.debug("calling " + UPLOADER_SCRIPT + " for " + womi.getDirectory());
		ProcessBuilder pb = new ProcessBuilder(createEprtToolsCall(UPLOADER_SCRIPT, womi.getDirectory().getPath()));
		pb.directory(eprtDir);

		List<String> stdout = runAndRetryIfNecessary(pb, 0);

		return recognizeWomiId(stdout);
	}

	private List<String> runAndRetryIfNecessary(ProcessBuilder pb, int depth) throws IOException {
		try {
			return runProcessAndTrackOutput(pb);
		} catch (IOException e) {
			if (depth + 1 < MAX_SCRIPT_RUN_RETRYING) {
				String errorMessage = "Retrying after an error in eprt-tools for " + pb.command();
				log.warn(errorMessage);
				log.debug(errorMessage, e);
				try {
					Thread.sleep(30 * 1000);
				} catch (InterruptedException e1) {
				}
				return runAndRetryIfNecessary(pb, depth + 1);
			} else {
				throw e;
			}
		}
	}

	private List<String> runProcessAndTrackOutput(ProcessBuilder pb) throws IOException {
		Process p = pb.start();

		List<String> stdout = new LinkedList<>();

		try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
			String line = null;
			while ((line = br.readLine()) != null) {
				log.debug(line);
				stdout.add(line);
			}
		}

		StringBuilder errors = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
			String line = null;
			while ((line = br.readLine()) != null) {
				errors.append(line).append("\n");
			}
		}
		try {
			p.waitFor();
		} catch (InterruptedException e) {
			log.warn("on waiting for subprocess: " + e);
		}

		if (errors.length() > 0) {
			throw new IOException("An error in eprt-tools occurred for " + pb.command() + ":\n" + errors);
		}
		return stdout;
	}

	private List<String> createEprtToolsCall(String scriptName, String... scriptArguments) {
		List<String> call = new LinkedList<>();
		call.add(new File(eprtDir, scriptName).getAbsolutePath());
		call.add(config.rtUsername());
		call.add(config.rtPassword());
		for (String arg : scriptArguments) {
			call.add(arg);
		}
		return call;
	}

	private long recognizeWomiId(final List<String> stdout) throws IOException {
		long publicationId = recognizeWomiInternalId(stdout);
		log.debug("publication id of the created WOMI: " + publicationId);
		long womiId = RTUtils.convertPublicationIdToWomiId(publicationId);
		return womiId;
	}

	private long recognizeWomiInternalId(final List<String> stdout) {
		String lastLine = getLastNotEmptyLine(stdout);
		if (lastLine == null) {
			throw new IllegalStateException("Cannot find line to parse for WOMI id");
		}

		if (NumberUtils.isNumber(lastLine)) {
			return Long.parseLong(lastLine);
		} else {
			throw new IllegalStateException("Cannot parse WOMI id from '" + lastLine + "'");
		}
	}

	private String getLastNotEmptyLine(final List<String> lines) {
		List<String> linesReversed = new LinkedList<>(lines);
		Collections.reverse(linesReversed);
		for (String line : linesReversed) {
			if (line.trim().length() > 0) {
				return line.trim();
			}
		}
		return null;
	}

}
