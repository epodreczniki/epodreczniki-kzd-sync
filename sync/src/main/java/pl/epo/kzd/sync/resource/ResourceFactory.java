package pl.epo.kzd.sync.resource;

import lombok.RequiredArgsConstructor;

import org.w3c.dom.Element;

import pl.epo.kzd.sync.ftp.FtpConnection;

@RequiredArgsConstructor
public class ResourceFactory {

	private final FtpConnection ftpConnection;

	public Resource createResource(Element resourceElement) {
		return new Resource(ftpConnection, resourceElement);
	}

}
