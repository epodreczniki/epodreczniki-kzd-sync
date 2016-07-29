package pl.epo.kzd.sync;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.LoadType;
import org.aeonbits.owner.Config.Sources;

@LoadPolicy(LoadType.MERGE)
@Sources({ "classpath:application-${env}.properties", "classpath:application.properties" })
public interface SyncConfig extends Config {

	@Key("ftp.host")
	String ftpHost();

	@Key("ftp.port")
	Integer ftpPort();

	@Key("ftp.username")
	String ftpUsername();

	@Key("ftp.password")
	String ftpPassword();

	@Key("ftp.xml_directory")
	String ftpXmlDirectory();

	@Key("amqp.connection_url")
	String amqpConnectionUrl();

	@Key("amqp.queue")
	String amqpQueue();

	@Key("portal.domain")
	String portalDomain();

	@Key("portal.protocol")
	String portalProtocol();

	@Key("portal.secret")
	@DisableFeature(value = DisableableFeature.PARAMETER_FORMATTING)
	String portalSecret();

	@Key("rt.domain")
	String rtDomain();

	@Key("rt.username")
	String rtUsername();

	@Key("rt.password")
	String rtPassword();

	@Key("rt.directory_id")
	long rtDirectoryId();

}