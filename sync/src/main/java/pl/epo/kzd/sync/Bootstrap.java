package pl.epo.kzd.sync;

import lombok.extern.slf4j.Slf4j;
import pl.epo.kzd.sync.amqp.MessageListener;

@Slf4j
public class Bootstrap {

	private static final String CREATE_RESOURCES_FROM_FTP = "--create-new";

	private static final String LISTEN_TO_EDITOR_MESSAGES = "--listen";

	private static final String REREAD = "--reread";

	public static void main(String[] args) {
		String mode = getMode(args);
		try {
			if (CREATE_RESOURCES_FROM_FTP.equals(mode)) {
				new SyncByFtp().run();
			} else if (LISTEN_TO_EDITOR_MESSAGES.equals(mode)) {
				new MessageListener().processMessages();
			} else if (REREAD.equals(mode)) {
				new SyncForEditor().rereadResource(args[1]);
			} else {
				log.error("unrecognized mode: " + mode);
				printArgumentError();
			}
		} catch (Exception e) {
			log.error("", e);
		}
	}

	private static String getMode(String[] args) {
		if (args.length == 1 || (args.length == 2 && args[0].equals(REREAD))) {
			return args[0];
		} else {
			return printArgumentError();
		}
	}

	private static String printArgumentError() {
		System.err.println("call with parameters to choose Synchronizer's mode:");
		System.err.println("   " + CREATE_RESOURCES_FROM_FTP);
		System.err.println("   " + LISTEN_TO_EDITOR_MESSAGES);
		System.err.println("   " + REREAD + " <RESOURCE_ID>");
		System.exit(1);
		return null;
	}

}
