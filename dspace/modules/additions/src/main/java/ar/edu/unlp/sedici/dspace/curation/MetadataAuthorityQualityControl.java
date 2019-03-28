package ar.edu.unlp.sedici.dspace.curation;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import java.io.IOException;
import java.util.List;

/**
 * Curation task that checks each authority controlled metadata of an item, reports anomalies and optionally fix them
 *
 */
public class MetadataAuthorityQualityControl extends AbstractCurationTask {

	private MetadataAuthorityManager authService = MetadataAuthorityManager.getManager();

	private ChoiceAuthorityManager choiceAuthorityService = ChoiceAuthorityManager.getManager()();

	/**
	 * Configuration property. If true, curation task fixes the metadata errors by
	 * itself. If false, only reports.
	 */
	private boolean fixmode = false;

	/**
	 * Configuration property, only works with fixmode in true.
	 * If true, curation task also fixes metadata value variants.
	 */
	private boolean fixvariants = false;

	/**
	 * Configuration property, only works with fixmode in true.
	 * If true, curation task also unsets every authority key
	 * which is not connected with any authority.
	 */
	private boolean fixDangling = false;

	/**
	 * Configuration property, only works with fixmode in true.
	 * If true, curation task also tries to find an authority key
	 * for those metadata with a missing authority key.
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

	@Override
	public void init(Curator curator, String taskId) throws IOException {
		super.init(curator, taskId);
		fixmode = taskBooleanProperty("fixmode", false);
		if (fixmode) {
			fixvariants = taskBooleanProperty("fixVariants", false);
			fixMissing = taskBooleanProperty("fixMissing", false);
			fixDangling = taskBooleanProperty("fixDangling", false);
		}
		metadataToSkip = this.taskArrayProperty("skipMetadata");
		metadataToCheck = this.taskArrayProperty("checkMetadata");
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
			List<MetadataValue> mValues = itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
			for (MetadataValue mv : mValues) {
				//Only check metadata if it is authority controlled
				if (authService.isAuthorityControlled(mv.getMetadataField()) && !skipMetadata(mv.getMetadataField())
						&& isMetadataToCheck(mv.getMetadataField())) {
					checkMetadataAuthority(reporter, mv, item);
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

	private void checkMetadataAuthority(StringBuilder reporter, MetadataValue mv, Item item) {

		if ((mv.getAuthority() == null || mv.getAuthority().isEmpty())
				&& (mv.getValue() == null || mv.getValue().isEmpty())) {
			report(reporter, mv, "ERROR", "Null both authority key and text_value");
		} else if (mv.getAuthority() == null || mv.getAuthority().isEmpty()) {
			checkMetadataWithoutAuthorityKey(reporter, mv, item);
		} else {
			checkMetadataWithAuthorityKey(reporter, mv);
		}
	}

	/**
	 * Reconciles metadata value with authority
	 */
	private void checkMetadataWithoutAuthorityKey(StringBuilder reporter, MetadataValue mv, Item item) {
		//Null or empty authority, try to find one from the text_value
		Choices choices = choiceAuthorityService.getBestMatch(mv.getMetadataField().toString(), mv.getValue(),
				item.getOwningCollection(), mv.getLanguage());
		if (authService.isAuthorityRequired(mv.getMetadataField())) {
			report(reporter, mv, "ERROR", "Missing authority key for <<", mv.getValue(), ">>");
		} else {
			report(reporter, mv, "INFO", "Missing optional authority key for <<", mv.getValue(), ">>");
		}
		if (choices.values.length > 0) {
			//Authority found from the text_value
			String value = choices.values[0].value;
			if (compare(value, mv.getValue())) {
				//Exact match, new authority available
				saveAuthorityKey(reporter, mv, choices);
			} else {
				// do not fix because can be either a false positive or variant
				report(reporter, mv, "INFO", "Recommended value <<", choices.values[0].authority, ">> with confidence ",
						String.valueOf(choices.confidence));
			}
		} else {
			//Authority not found from the text_value, just check confidence
			assertConfidenceNotFound(reporter, mv);
		}

	}

	private void checkMetadataWithAuthorityKey(StringBuilder reporter, MetadataValue mv) {
		String value = choiceAuthorityService.getLabel(mv.getMetadataField().toString(), mv.getAuthority(),
				mv.getLanguage());
		if (value == null || value.isEmpty()) {
			// Authority not found
			saveDanglingAuthority(reporter, mv);
		} else if (compare(value, mv.getValue())) {
			// Authority found, value==text_value
			assertConfidenceUncertain(reporter, mv);
		} else {
			// Authority found, but value!=text_value
			saveVariant(reporter, mv, value);
		}
	}

	private void assertConfidenceUncertain(StringBuilder reporter, MetadataValue mv) {
		if (mv.getConfidence() < Choices.CF_UNCERTAIN) {
			report(reporter, mv, "ERROR", ": Invalid confidence ", String.valueOf(mv.getConfidence()), ", expected ",
					String.valueOf(Choices.CF_UNCERTAIN));
			if (fixmode) {
				mv.setConfidence(Choices.CF_UNCERTAIN);
				report(reporter, mv, "FIXED", "[CONFIDENCE] confidence replaced with value ",
						String.valueOf(Choices.CF_UNCERTAIN));
			}
		}
	}

	private void assertConfidenceNotFound(StringBuilder reporter, MetadataValue mv) {
		if (mv.getConfidence() > Choices.CF_NOTFOUND) {
			report(reporter, mv, "ERROR", "Invalid confidence ", String.valueOf(mv.getConfidence()), ", expected ",
					String.valueOf(Choices.CF_NOTFOUND));
			if (fixmode) {
				mv.setConfidence(Choices.CF_NOTFOUND);
				report(reporter, mv, "FIXED", "[CONFIDENCE] confidence replaced with value ",
						String.valueOf(Choices.CF_NOTFOUND));
			}
		}
	}

	private void saveVariant(StringBuilder reporter, MetadataValue mv, String value) {
		report(reporter, mv, "WARN", "Variant found. Authority  <<", mv.getAuthority(),
				">>; Metadata text_value <<", mv.getValue(), ">>; Authority value <<", value, ">>");
		if (fixvariants) {
			mv.setValue(value);
			mv.setConfidence(Choices.CF_UNCERTAIN);
			report(reporter, mv, "FIXED", "[VARIANT] variant replaced with authority's value.");
		}

	}

	private void saveAuthorityKey(StringBuilder reporter, MetadataValue mv, Choices choices) {
		String newAuthority = choices.values[0].authority;
		report(reporter, mv, "INFO", "Found Authority <<", newAuthority, ">> for value <<", mv.getValue(), ">>.");
		if (fixMissing && choices.confidence >= Choices.CF_UNCERTAIN) {
			mv.setAuthority(newAuthority);
			mv.setConfidence(choices.confidence);
			report(reporter, mv, "FIXED", "[MISSING AUTHORITY] Authority set with value <<", newAuthority, ">>");
		} else if (choices.confidence < Choices.CF_UNCERTAIN) {
			report(reporter, mv, "NOT FIXED", "[AMBIGUOUS AUTHORITY] Authority key <<", newAuthority,
					">> is ambiguous for text_value");
		}

	}

	private void saveDanglingAuthority(StringBuilder reporter, MetadataValue mv) {
		report(reporter, mv, "ERROR", "Authority key <<", mv.getAuthority(), ">> not found");
		assertConfidenceNotFound(reporter, mv);
		if (fixDangling) {
			mv.setAuthority(null);
			report(reporter, mv, "FIXED", "[DANGLING AUTHORITY] Authority set with NULL");
			checkMetadataWithoutAuthorityKey(reporter, mv, (Item) mv.getResourceId());
		}
	}

	private void report(StringBuilder reporter, MetadataValue value, String level, String... messages) {
		reporter.append("- ").append(level).append(" (").append(value.getFieldId()).append(",")
				.append(value.getMetadataField()).append(") : ");
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
	private boolean skipMetadata(MetadataField mf) {
		for (String dataToSkip : metadataToSkip) {
			if (mf.toString().equals(dataToSkip)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if current metadata must be processed. If metadataToCheck is equals to "*",
	 *  all authority controlled metadata can be processed
	 *
	 * @param mf metadata field of the current metadata being processed
	 * @return true if metadataToCheck array contains mf or if metadataTocheck is
	 *         empty
	 */
	private boolean isMetadataToCheck(MetadataField mf) {
		if (metadataToCheck.length > 0 && metadataToCheck[0].equals("*")) {
			return true;
		}
		for (String dataToCheck : metadataToCheck) {
			if (mf.toString().equals(dataToCheck)) {
				return true;
			}
		}
		return false;
	}

}