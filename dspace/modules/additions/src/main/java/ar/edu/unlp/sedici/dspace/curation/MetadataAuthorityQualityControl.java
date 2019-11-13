package ar.edu.unlp.sedici.dspace.curation;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;
import com.hp.hpl.jena.sparql.expr.ExprException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Curation task that checks each authority controlled metadata of an item,
 * reports anomalies and optionally fix them
 *
 */
public class MetadataAuthorityQualityControl extends AbstractCurationTask {

	private MetadataAuthorityManager authManager = MetadataAuthorityManager.getManager();

	private ChoiceAuthorityManager choiceAuthorityManager = ChoiceAuthorityManager.getManager();

	/**
	 * Configuration property. If true, curation task fixes the metadata errors by
	 * itself. If false, only reports.
	 */
	private boolean fixmode = false;

	/**
	 * Configuration property, only works with fixmode in true. If true, curation
	 * task also fixes metadata value variants.
	 */
	private boolean fixvariants = false;

	/**
	 * Configuration property, only works with fixmode in true. If true, curation
	 * task also unsets every authority key which is not connected with any
	 * authority.
	 */
	private boolean fixDangling = false;

	/**
	 * Configuration property, only works with fixmode in true. If true, curation
	 * task also tries to find an authority key for those metadata with a missing
	 * authority key.
	 */
	private boolean fixMissing = false;

	/**
	 * Configuration property. List of metadata_fields which are authority
	 * controlled but we don't want the curation task to process
	 */
	private String[] metadataToSkip;

	/**
	 * Configuration property. List of metadata_fields that we want the curation
	 * task process. If metadataToCheck = "*" then all authority controlled metadata
	 * can be processed
	 */
	private String[] metadataToCheck;

	private String mtFieldName;

	@Override
	public void init(Curator curator, String taskId) throws IOException {
		super.init(curator, taskId);
		fixmode = taskBooleanProperty("fixmode", false);
		if (fixmode) {
			fixvariants = taskBooleanProperty("fixVariants", false);
			fixMissing = taskBooleanProperty("fixMissing", false);
			fixDangling = taskBooleanProperty("fixDangling", false);
		}
		String metadataToSkipProperty = this.taskProperty("skipMetadata");
		if (metadataToSkipProperty != null) {
			metadataToSkip = metadataToSkipProperty.trim().split("[\\ ]*,[\\ ]*");
		}
		String metadataToCheckProperty = this.taskProperty("checkMetadata");
		if (metadataToCheckProperty != null) {
			metadataToCheck = metadataToCheckProperty.trim().split("[\\ ]*,[\\ ]*");
		}
	}

	@Override
	public int perform(DSpaceObject dso) throws IOException {
		int status = Curator.CURATE_UNSET;
		StringBuilder reporter = new StringBuilder();
		if (dso instanceof Item) {
			Item item = (Item) dso;
			reporter.append("########## ");
			reporter.append("Checking item with handle ").append(item.getHandle()).append(" and item id ")
					.append(item.getID());
			if (item.isWithdrawn()) {
				reporter.append(" (WITHDRAWN)");
			}
			reporter.append(" ##########\n");

			Metadatum[] mValues = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
			for (Metadatum mt : mValues) {
				// Only check metadata if it is authority controlled
				mtFieldName = mt.getField().replace(".", "_");
				if (authManager.isAuthorityControlled(mtFieldName) && !skipMetadata(mtFieldName)
						&& isMetadataToCheck(mtFieldName)) {
					try {
						checkMetadataAuthority(reporter, mt, item);
					} catch (ExprException | QueryExceptionHTTP e) {
						report(reporter, mt, "ERROR", "An error ocurred processing metadata: ", e.getMessage());
						e.printStackTrace();
					}
				}
			}

			report(reporter.toString());
			status = Curator.CURATE_SUCCESS;
		} else {
			status = Curator.CURATE_SKIP;
		}
		setResult(reporter.toString());
		return status;
	}

	private void checkMetadataAuthority(StringBuilder reporter, Metadatum mt, Item item) throws IOException {
		Metadatum oldMt = mt.copy();
		if ((mt.authority == null || mt.authority.isEmpty()) && (mt.value == null || mt.value.isEmpty())) {
			report(reporter, mt, "ERROR", "Null both authority key and text_value");
		} else if (mt.authority == null || mt.authority.isEmpty()) {
			checkMetadataWithoutAuthorityKey(reporter, mt, item);
		} else {
			checkMetadataWithAuthorityKey(reporter, mt, item);
		}
		if (fixmode) {
			item.replaceMetadataValue(oldMt, mt);
			try {
				item.update();
			} catch (Exception e) {
				throw new IOException(e);
			}
		}
	}

	/**
	 * Reconciles metadata value with authority
	 *
	 * @throws IOException
	 */
	private void checkMetadataWithoutAuthorityKey(StringBuilder reporter, Metadatum mt, Item item) throws IOException {
		// Null or empty authority, try to find one from the text_value
		Choices choices;
		try {
			choices = choiceAuthorityManager.getBestMatch(mtFieldName, mt.value, item.getOwningCollection().getID(),
					mt.language);
		} catch (SQLException e) {
			throw new IOException(e);
		}
		if (authManager.isAuthorityRequired(mtFieldName)) {
			report(reporter, mt, "ERROR", "Missing authority key for <<", mt.value, ">>");
		} else {
			report(reporter, mt, "INFO", "Missing optional authority key for <<", mt.value, ">>");
		}
		if (choices.values.length > 0) {
			// Authority found from the text_value
			String value = choices.values[0].value;
			if (compare(value, mt.value)) {
				// Exact match, new authority available
				saveAuthorityKey(reporter, mt, choices);
			} else {
				// do not fix because can be either a false positive or variant
				report(reporter, mt, "INFO", "Recommended value <<", choices.values[0].authority, ">> with confidence ",
						String.valueOf(choices.confidence));
				// As we didn't set the authority, confidence shouldn't be greater than 300 (not found)
				assertConfidenceNotFound(reporter, mt);
			}
		} else {
			// Authority not found from the text_value, just check confidence
			assertConfidenceNotFound(reporter, mt);
		}

	}

	private void checkMetadataWithAuthorityKey(StringBuilder reporter, Metadatum mt, Item item) throws IOException {
		String value = choiceAuthorityManager.getLabel(mtFieldName, mt.authority, mt.language);
		if (value == null || value.isEmpty()) {
			// Authority not found
			saveDanglingAuthority(reporter, mt, item);
		} else if (compare(value, mt.value)) {
			// Authority found, value==text_value
			assertConfidenceUncertain(reporter, mt);
		} else {
			// Authority found, but value!=text_value
			saveVariant(reporter, mt, value);
		}
	}

	private void assertConfidenceUncertain(StringBuilder reporter, Metadatum mt) {
		if (mt.confidence < Choices.CF_UNCERTAIN) {
			report(reporter, mt, "ERROR", ": Invalid confidence ", String.valueOf(mt.confidence), ", expected ",
					String.valueOf(Choices.CF_UNCERTAIN));
			if (fixmode) {
				mt.confidence = Choices.CF_UNCERTAIN;
				report(reporter, mt, "FIXED", "[CONFIDENCE] confidence replaced with value ",
						String.valueOf(Choices.CF_UNCERTAIN));
			}
		}
	}

	private void assertConfidenceNotFound(StringBuilder reporter, Metadatum mt) {
		if (mt.confidence > Choices.CF_NOTFOUND) {
			report(reporter, mt, "ERROR", "Invalid confidence ", String.valueOf(mt.confidence), ", expected ",
					String.valueOf(Choices.CF_NOTFOUND));
			if (fixmode) {
				mt.confidence = Choices.CF_NOTFOUND;
				report(reporter, mt, "FIXED", "[CONFIDENCE] confidence replaced with value ",
						String.valueOf(Choices.CF_NOTFOUND));
			}
		}
	}

	private void saveVariant(StringBuilder reporter, Metadatum mt, String value) {
		report(reporter, mt, "WARN", "Variant found. Authority  <<", mt.authority, ">>; Metadata text_value <<",
				mt.value, ">>; Authority value <<", value, ">>");
		if (fixvariants) {
			mt.value = value;
			mt.confidence = Choices.CF_UNCERTAIN;
			report(reporter, mt, "FIXED", "[VARIANT] variant replaced with authority's value.");
		}

	}

	private void saveAuthorityKey(StringBuilder reporter, Metadatum mt, Choices choices) {
		String newAuthority = choices.values[0].authority;
		report(reporter, mt, "INFO", "Found Authority <<", newAuthority, ">> for value <<", mt.value, ">>.");
		if (fixMissing && choices.confidence >= Choices.CF_UNCERTAIN) {
			mt.authority = newAuthority;
			mt.confidence = choices.confidence;
			report(reporter, mt, "FIXED", "[MISSING AUTHORITY] Authority set with value <<", newAuthority, ">>");
		} else if (choices.confidence < Choices.CF_UNCERTAIN) {
			report(reporter, mt, "NOT FIXED", "[AMBIGUOUS AUTHORITY] Authority key <<", newAuthority,
					">> is ambiguous for text_value");
		}

	}

	private void saveDanglingAuthority(StringBuilder reporter, Metadatum mt, Item item) throws IOException {
		report(reporter, mt, "ERROR", "Authority key <<", mt.authority, ">> not found");
		assertConfidenceNotFound(reporter, mt);
		if (fixDangling) {
			mt.authority = null;
			report(reporter, mt, "FIXED", "[DANGLING AUTHORITY] Authority set with NULL");
			checkMetadataWithoutAuthorityKey(reporter, mt, item);
		}
	}

	private void report(StringBuilder reporter, Metadatum mt, String level, String... messages) {
		reporter.append("- ").append(level).append(" (").append(mtFieldName).append(") : ");
		for (String message : messages) {
			reporter.append(message);
		}
		reporter.append("\n");
	}

	/**
	 * Method used to compare 2 strings, considering several criteria
	 *
	 * @param value1
	 * @param value2
	 * @return true if value1.equals(value2) after some functions applied to both
	 *         strings
	 */
	private boolean compare(String value1, String value2) {
		String customValue1 = value1.trim();
		String customValue2 = value2.trim();
		return customValue1.equalsIgnoreCase(customValue2);
	}

	/**
	 * Check if current metadata must not be processed.
	 *
	 * @param mf metadata field of the current metadata being processed
	 * @return true if metadataToSkip array contains mf
	 */
	private boolean skipMetadata(String mf) {
		if (metadataToSkip == null) {
			return false;
		}

		for (String dataToSkip : metadataToSkip) {
			if (mf.equals(dataToSkip)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if current metadata must be processed. If metadataToCheck is equals to
	 * "*", all authority controlled metadata can be processed
	 *
	 * @param mf metadata field of the current metadata being processed
	 * @return true if metadataToCheck array contains mf or if metadataTocheck is
	 *         empty
	 */
	private boolean isMetadataToCheck(String mf) {
		if (metadataToCheck == null) {
			return false;
		}

		if (metadataToCheck.length > 0 && metadataToCheck[0].equals("*")) {
			return true;
		}
		for (String dataToCheck : metadataToCheck) {
			if (mf.equals(dataToCheck)) {
				return true;
			}
		}
		return false;
	}

}