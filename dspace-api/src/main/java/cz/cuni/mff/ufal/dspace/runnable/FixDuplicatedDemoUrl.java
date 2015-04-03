package cz.cuni.mff.ufal.dspace.runnable;

import java.io.Console;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FixDuplicatedDemoUrl {
	private static Logger log = LoggerFactory
			.getLogger(FixDuplicatedDemoUrl.class);

	// run via dsrun
	public static void main(String[] args) {
		Context context = null;
		try {
			context = new Context();
			context.turnOffAuthorisationSystem();

			// get in_archive items
			ItemIterator it = Item.findAll(context);

			while (it.hasNext()) {
				Item item = it.next();

				// skip withdrawn items
				if (item.isWithdrawn()) {
					continue;
				}

				Metadatum[] sl_dcvs = item.getMetadata("metashare",
						"ResourceInfo#ResourceDocumentationInfo",
						"samplesLocation", Item.ANY);
				Metadatum[] demo_dcvs = item.getMetadata("local", "demo", "uri",
						Item.ANY);

				if ((demo_dcvs == null || demo_dcvs.length == 0)
						&& (sl_dcvs != null && sl_dcvs.length > 0)) {
					// no local.demo.uri; copy samplesLocation there
					for (Metadatum dcv : sl_dcvs) {
						moveValue(item, dcv);
					}
					item.clearMetadata("metashare",
							"ResourceInfo#ResourceDocumentationInfo",
							"samplesLocation", Item.ANY);
				} else if ((demo_dcvs != null && demo_dcvs.length > 0)
						&& (sl_dcvs != null && sl_dcvs.length > 0)) {
					if (demo_dcvs.length > 1 || sl_dcvs.length > 1) {
						System.out
								.println(String
										.format("Please check item %s by hand, it appears to have more than 2 uris",
												item.getID()));
					} else {
						if (demo_dcvs[0].value.equals(sl_dcvs[0].value)) {
							keepDemoUri(item, demo_dcvs[0].value);
						} else {
							System.out
									.println(String
											.format("The values demo.uri=%s and samplesLocation=%s differ.",
													demo_dcvs[0].value,
													sl_dcvs[0].value));
							System.out
									.println("samplesLocation will be removed, please select the value to keep in demo.uri");
							Console console = System.console();
							String keep;
							do {
								keep = console
										.readLine("Enter 1 to keep the demo.uri value or 2 to keep the samplesLocation value:");
							} while (!("1".equals(keep) || "2".equals(keep)));
							if ("1".equals(keep)) {
								keepDemoUri(item, demo_dcvs[0].value);
							} else if ("2".equals(keep)) {
								item.clearMetadata("local", "demo", "uri",
										Item.ANY);
								moveValue(item, sl_dcvs[0]);
								item.clearMetadata(
										"metashare",
										"ResourceInfo#ResourceDocumentationInfo",
										"samplesLocation", Item.ANY);
							}

						}
					}
				}
				try {
					item.update();
				} catch (AuthorizeException e) {
					e.printStackTrace();
					log.error(e.getMessage());
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			if (context != null) {
				context.abort();
			}
		}

		if (context != null) {
			try {
				context.complete();
			} catch (SQLException e) {
				e.printStackTrace();
				log.error(e.getMessage());
			}
		}

	}

	private static void moveValue(Item item, Metadatum dcv) {
		String message = String.format(
				"Moved value %s from samplesLocation to local.demo.uri ",
				dcv.value);
		String logMessage = String.format("%s, for item with id %s",
				message, item.getID());
		log.info(logMessage);
		System.out.println(logMessage);
		item.addMetadata("local", "demo", "uri", dcv.language, dcv.value);
		item.addMetadata("dc", "description", "provenance", "en", message);
	}

	private static void keepDemoUri(Item item, String demo_value) {
		String message = String.format(
				"Removing the duplicate value %s from samplesLocation",
				demo_value);
		String logMessage = String.format("%s, for item with id %s",
				message, item.getID());
		log.info(logMessage);
		System.out.println(logMessage);
		item.addMetadata("dc", "description", "provenance", "en", message);
		item.clearMetadata("metashare",
				"ResourceInfo#ResourceDocumentationInfo", "samplesLocation",
				Item.ANY);
	}

}
