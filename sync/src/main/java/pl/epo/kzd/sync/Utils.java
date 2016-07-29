package pl.epo.kzd.sync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;

@Slf4j
class Utils {

	private static final String BACKUP_DIRECTORY = "inputs";

	public static void backupDescriptorFilesQuietly(File descriptorFile) {
		List<File> files = new ArrayList<>();
		files.add(descriptorFile);
		backupDescriptorFilesQuietly(files);
	}

	public static void backupDescriptorFilesQuietly(Collection<File> descriptorFiles) {
		try {
			backupDescriptorFiles(descriptorFiles);
		} catch (IOException e) {
			log.warn("error saving backup of descriptor files", e);
		}
	}

	private static void backupDescriptorFiles(Collection<File> descriptorFiles) throws IOException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		String backupFilename = sdf.format(new Date()) + ".zip";
		File backupDir = new File(BACKUP_DIRECTORY);
		if (!backupDir.exists()) {
			backupDir.mkdir();
		}
		File backupFile = new File(backupDir, backupFilename);
		try (ZipOutputStream backup = new ZipOutputStream(new FileOutputStream(backupFile))) {
			for (File descriptorFile : descriptorFiles) {
				backup.putNextEntry(new ZipEntry(descriptorFile.getName()));
				try (FileInputStream in = new FileInputStream(descriptorFile)) {
					IOUtils.copy(in, backup);
				}
			}
		}
		log.debug("a backup of descriptor files saved to: " + backupFile);
	}

}
