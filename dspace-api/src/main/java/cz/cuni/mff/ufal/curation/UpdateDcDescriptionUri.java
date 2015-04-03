/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.curation;

import java.io.IOException;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.dspace.content.Metadatum;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

public class UpdateDcDescriptionUri extends AbstractCurationTask {

	private int status = Curator.CURATE_UNSET;

	// The log4j logger for this class
	private static Logger log = Logger.getLogger(Curator.class);

	@Override
	public int perform(DSpaceObject dso) throws IOException {

		// The results that we'll return
		StringBuilder results = new StringBuilder();

		// Unless this is an item, we'll skip this item
		status = Curator.CURATE_SKIP;

		if (dso instanceof Item) {
			try {
				Item item = (Item) dso;
				Metadatum[] vals = item.getMetadataByMetadataString("dc.source.uri");

				HashSet<String> sourceUris = new HashSet<String>();

				if (vals != null && vals.length > 0) {
					for (Metadatum val : vals) {
						sourceUris.add(val.value);
					}
				}

				vals = item.getMetadataByMetadataString("dc.description.uri");
				if (null != vals && vals.length > 0) {
					if (sourceUris.size() > 0) {
						// if we already have some source uri
						for (Metadatum val : vals) {
							// and there's different description.uri
							if (!sourceUris.contains(val.value)) {
								throw new Exception(
										String.format(
												"Item %s has different description and source uris",
												item.getID()));
							} else {
								; // do nothing, uri already present
							}
						}
					} else {
						for (Metadatum val : vals) {
							item.addMetadata(MetadataSchema.DC_SCHEMA,
									"source", "uri", val.language, val.value);
						}
					}
					item.clearMetadata(MetadataSchema.DC_SCHEMA, "description",
							"uri", Item.ANY);
					item.update();
				}
			} catch (Exception ex) {
				status = Curator.CURATE_FAIL;
				results.append(ex.getLocalizedMessage()).append("\n");
			}
		}

		report(results.toString());
		setResult(results.toString());
		return status;
	}

}
