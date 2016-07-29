package pl.epo.kzd.sync.resource;

import java.io.InputStream;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class ResourceDescriptorTest {

	@Test
	public void testGetResources() throws Exception {
		ResourceDescriptor rd;
		try (InputStream is = this.getClass().getResourceAsStream("/epo-kzd-example2.xml")) {
			ResourceFactory resourceFactory = new ResourceFactory(null);
			rd = new ResourceDescriptor(is, resourceFactory);
		}
		List<Resource> resources = rd.getResources();
		Assert.assertEquals(3, resources.size());
		Assert.assertEquals("KZD0000001", resources.get(0).getId());
		Assert.assertEquals(Recipient.STUDENT, resources.get(0).getRecipient());
		Assert.assertEquals("CC BY 3.0", resources.get(0).getLicense());
		Assert.assertEquals("nieznana", resources.get(1).getLicense());
		Assert.assertEquals("KZD0000003", resources.get(1).getId());
		Assert.assertEquals("przykladowy_film", resources.get(2).getId());
	}

}
