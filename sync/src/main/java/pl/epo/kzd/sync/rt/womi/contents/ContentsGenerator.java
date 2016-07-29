package pl.epo.kzd.sync.rt.womi.contents;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class ContentsGenerator implements Closeable {

	private static final String MAINFILE_FILENAME = "multiFormat.xml";

	protected final File womiDir;

	/**
	 * @return contents directory
	 */
	protected File createContentsDirAndMainFile(String formatType, String filename)
			throws ParserConfigurationException, IOException, TransformerException {
		MainFile mainFile = new MainFile();
		mainFile.addFormat(formatType, filename);
		mainFile.save(new File(womiDir, MAINFILE_FILENAME));

		File contentsDir = new File(womiDir, formatType);
		contentsDir.mkdir();
		return contentsDir;
	}
}
