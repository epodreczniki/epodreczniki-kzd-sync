package pl.epo.kzd.sync.resource;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ResourceFileTest {

	@Test
	public void testGetFilename() {
		assertEquals("wer", new ResourceFtpFile(ResourceFileType.ATTACHMENT, "/asdf/2342/wer", "test").getSafeFilename());
		assertEquals("wer", new ResourceFtpFile(ResourceFileType.ATTACHMENT, "/wer", "test").getSafeFilename());
		assertEquals("wer", new ResourceFtpFile(ResourceFileType.ATTACHMENT, "wer", "test").getSafeFilename());
		assertEquals("", new ResourceFtpFile(ResourceFileType.ATTACHMENT, "/wer/", "test").getSafeFilename());
	}

}
