/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.app.util.MappingMetadata;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

/**
 * 
 * Based on GoogleMetadata crosswalk
 * 
 * @author l.pascarelli
 *
 */
@SuppressWarnings("deprecation")
public class OrcidWorkMetadata extends MappingMetadata {

	private final static Logger log = Logger.getLogger(OrcidWorkMetadata.class);

	private static final String ORCID_PREFIX = "orcid.";

	public static final String TITLE = "title";

	public static final String AUTHORS = "authors";

	public static final String SUBTITLE = "sub_title";

	public static final String TRANSLATEDTITLE = "translated_title";
	
	public static final String TRANSLATEDTITLELANGUAGE = "translated_title.language";

	public static final String JOURNAL_TITLE = "journal_title";

	public static final String ABSTRACT = "short_description";

	public static final String TYPE = "work_type";

	public static final String CITATION = "citation_crosswalk";

	public static final String LANGUAGE = "language";

	public static final String ISSUE = "publication_date";
	
	public static final String ISSUE_YEAR = "publication_date.year";
	public static final String ISSUE_MONTH = "publication_date.month";
	public static final String ISSUE_DAY = "publication_date.day";

	public static final String KEYWORDS = "keywords";
	
	public static final String URL = "url";
	
	public static final String EXTERNAL_IDENTIFIER = "external_identifier";

	/**
	 * Wrap the item, parse all configured fields and generate metadata field
	 * values.
	 * 
	 * @param item
	 *            - The item being viewed to extract metadata from
	 */
	public OrcidWorkMetadata(Context context, Item item) throws SQLException {
		init("orcid-work-metadata.config");
		// Hold onto the item in case we need to refresh a stale parse
		this.item = item;
		itemURL = HandleManager.resolveToURL(context, item.getHandle());
		parseItem();
	}

	/**
	 * Using metadata field mappings contained in the loaded configuration,
	 * parse through configured metadata fields, building valid Google metadata
	 * value strings. Field names & values contained in metadataMappings.
	 * 
	 */
	private void parseItem() {

		// TITLE
		addSingleField(TITLE);

		// ISSUE
		addDateField(ISSUE);

		// ABSTRACT
		addSingleField(ABSTRACT);

		// TITLE
		addLanguageField(TRANSLATEDTITLE);

		// KEYWORDS (multi)
		addMultipleValues(KEYWORDS);

		// TITLE
		addSingleField(TITLE);

		// AUTHORS (multi)
		addMultipleWithAuthorityValues(AUTHORS);

		// JOURNAL_TITLE
		addSingleField(JOURNAL_TITLE);

		// LANGUAGE
		addSingleField(LANGUAGE);

		// CITATION
		addCitation(CITATION);
		
		//URL
		addSingleField(URL);
		
		// EXTERNAL IDs
		addMultiInvertedValues(EXTERNAL_IDENTIFIER);
		
		//TYPE
		addSingleField(TYPE);
		
	}


	/**
	 * @return the TITLE
	 */
	public String getTitle() {
		if (!metadataMappings.get(TITLE).isEmpty()) {
			return metadataMappings.get(TITLE).get(0);
		}
		return null;
	}

	/**
	 * @return the SUBTITLE
	 */
	public String getSubTitle() {
		if (!metadataMappings.get(SUBTITLE).isEmpty()) {
			return metadataMappings.get(SUBTITLE).get(0);
		}
		return null;
	}

	/**
	 * @return the journal_title
	 */
	public String getJournalTitle() {
		if (!metadataMappings.get(JOURNAL_TITLE).isEmpty()) {
			return metadataMappings.get(JOURNAL_TITLE).get(0);
		}
		return null;
	}

	/**
	 * @return the issue
	 */
	public String getIssue() {
		if (!metadataMappings.get(ISSUE).isEmpty()) {
			return metadataMappings.get(ISSUE).get(0);
		}
		return null;
	}

	/**
	 * @return the citation_abstract_html_url
	 */
	public String getAbstract() {
		if (!metadataMappings.get(ABSTRACT).isEmpty()) {
			return metadataMappings.get(ABSTRACT).get(0);
		}
		return null;
	}

	public String getCitation() {
		if (!metadataMappings.get(CITATION).isEmpty()) {
			return metadataMappings.get(CITATION).get(0);
		}
		return null;
	}

	public String getCitationType() {
		if (!configuredFields.get(CITATION).isEmpty()) {
			return configuredFields.get(CITATION);
		}
		return null;
	}
	
	@Override
	protected String getPrefix() {
		return ORCID_PREFIX;
	}

	public String getTranslatedTitle() {
		if (!metadataMappings.get(TRANSLATEDTITLE).isEmpty()) {
			return metadataMappings.get(TRANSLATEDTITLE).get(0);
		}
		return null;
	}
	
	public String getTranslatedTitleLanguage() {
		if (!metadataMappings.get(TRANSLATEDTITLELANGUAGE).isEmpty()) {
			return metadataMappings.get(TRANSLATEDTITLELANGUAGE).get(0);
		}
		return null;
	}

	public String getYear() {
		if (!metadataMappings.get(ISSUE_YEAR).isEmpty()) {
			return metadataMappings.get(ISSUE_YEAR).get(0);
		}
		return null;
	}

	public String getMonth() {
		if (!metadataMappings.get(ISSUE_MONTH).isEmpty()) {
			return metadataMappings.get(ISSUE_MONTH).get(0);
		}
		return null;
	}

	public String getDay() {
		if (!metadataMappings.get(ISSUE_DAY).isEmpty()) {
			return metadataMappings.get(ISSUE_DAY).get(0);
		}
		return null;
	}
	
	public String getURL() {
		if (!metadataMappings.get(URL).isEmpty()) {
			return metadataMappings.get(URL).get(0);
		}
		return null;
	}
	
	public List<String> getExternalIdentifier() {
		if (!metadataMappings.get(EXTERNAL_IDENTIFIER).isEmpty()) {
			return metadataMappings.get(EXTERNAL_IDENTIFIER);
		}
		return new ArrayList<String>();
	}
	
	public String getExternalIdentifierType(String identifier) {
		if (!metadataMappings.get(identifier).isEmpty()) {
			return metadataMappings.get(identifier).get(0);
		}
		return null;
	}
	
	public List<String> getAuthors() {
		if (!metadataMappings.get(AUTHORS).isEmpty()) {
			return metadataMappings.get(AUTHORS);
		}
		return new ArrayList<String>();
	}

	public String getLanguage() {
		if (!metadataMappings.get(LANGUAGE).isEmpty()) {
			return metadataMappings.get(LANGUAGE).get(0);
		}
		return null;
	}
	
	public String getWorkType() {
		if (!metadataMappings.get(TYPE).isEmpty()) {
			return metadataMappings.get(TYPE).get(0);
		}
		return null;
	}
}