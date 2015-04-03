package cz.cuni.mff.ufal.dspace.runnable;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FixRightsLabel {
	private static Logger log = LoggerFactory.getLogger(FixRightsLabel.class);

	// run through dsrun
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

				// only for items without files
				if (!item.hasUploadedFiles()) {
					Metadatum[] dcvs = item.getMetadata("dc", "rights", "label",
							Item.ANY);
					// if there is dc.rights.label delete
					if (dcvs != null && dcvs.length > 0) {
						StringBuilder labels = new StringBuilder();
						for (Metadatum label : dcvs) {
							labels.append(label.value + " ");
						}
						String message = String
								.format("REMOVING labels [%s] from item's [%s] metadata.",
										labels.toString(), item.getID());
						log.info(message);
						System.out.println(message);
						item.addMetadata(
								"dc",
								"description",
								"provenance",
								"en",
								String.format(
										"removed dc.rights.label with the following values [%s]",
										labels.toString()));
						item.clearMetadata("dc", "rights", "label", Item.ANY);
						try {
							item.update();
						} catch (AuthorizeException e) {
							e.printStackTrace();
							log.error(e.getMessage());
						}
					}
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

}
