/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.app.cris.model.Project;
import org.dspace.app.util.MappingMetadata;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

/**
 * 
 * Based on GoogleMetadata crosswalk
 * 
 * @author l.pascarelli
 *
 */
@SuppressWarnings("deprecation")
public class OrcidFundingMetadata extends MappingMetadata {

	private final static Logger log = Logger.getLogger(OrcidFundingMetadata.class);

	private static final String ORCID_PREFIX = "orcid.";

	public static final String TITLE = "funding-title";

	public static final String CONTRIBUTORSLEAD = "funding-contributor.lead";
	public static final String CONTRIBUTORSCOLEAD = "funding-contributor.colead";

	public static final String ABSTRACT = "short-description";

	public static final String TYPE = "type";

	public static final String AMOUNT = "amount";
	public static final String CURRENCY_CODE = "currency_code";
	
	public static final String STARTDATE = "start-date";
	
	private static final String STARTDATE_YEAR = "start-date.year";
	private static final String STARTDATE_MONTH = "start-date.month";
	private static final String STARTDATE_DAY = "start-date.day";

	public static final String ENDDATE = "end-date";
	
	private static final String ENDDATE_YEAR = "end-date.year";
	private static final String ENDDATE_MONTH = "end-date.month";
	private static final String ENDDATE_DAY = "end-date.day";
	
	public static final String URL = "url";
	
	public static final String EXTERNAL_IDENTIFIER = "funding-external-identifier";
	
	public static final String ORGANIZATION = "organization";
	
	public static final String ORGANIZATION_CITY = "organization.city";
	public static final String ORGANIZATION_COUNTRY = "organization.country";
	

	/**
	 * Wrap the item, parse all configured fields and generate metadata field
	 * values.
	 * 
	 * @param item
	 *            - The item being viewed to extract metadata from
	 */
	public OrcidFundingMetadata(Context context, Project item) throws SQLException {
		init("orcid-project-metadata.config");
		// Hold onto the item in case we need to refresh a stale parse
		this.item = item;		
		itemURL = ConfigurationManager.getProperty("dspace.url") + "/cris/uuid/" + item.getHandle();
		parseFunding();
	}

	/**
	 * Using metadata field mappings contained in the loaded configuration,
	 * parse through configured metadata fields, building valid Google metadata
	 * value strings. Field names & values contained in metadataMappings.
	 * 
	 */
	private void parseFunding() {

		// TITLE
		addSingleField(TITLE);

		// STARTDATE
		addDateField(STARTDATE);
		// ENDDATE
		addDateField(ENDDATE);

		// ABSTRACT
		addSingleField(ABSTRACT);

		// TYPE
		addSingleField(TYPE);

		// AUTHORS (multi)
		addMultipleWithAuthorityValues(CONTRIBUTORSLEAD);
		addMultipleWithAuthorityValues(CONTRIBUTORSCOLEAD);

		//URL
		addSingleField(URL);

		//AMOUNT
		addCurrencyField(AMOUNT);
		
		// EXTERNAL IDs
		addMultiInvertedValues(EXTERNAL_IDENTIFIER);
		
		// ORGANIZATION
		addSingleField(ORGANIZATION);
		addSingleField(ORGANIZATION_CITY);
		addSingleField(ORGANIZATION_COUNTRY);
		
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
	 * @return the citation_abstract_html_url
	 */
	public String getAbstract() {
		if (!metadataMappings.get(ABSTRACT).isEmpty()) {
			return metadataMappings.get(ABSTRACT).get(0);
		}
		return null;
	}
	
	@Override
	protected String getPrefix() {
		return ORCID_PREFIX;
	}

	public String getCurrencyCode() {
		if (!metadataMappings.get(CURRENCY_CODE).isEmpty()) {
			return metadataMappings.get(CURRENCY_CODE).get(0);
		}
		return null;
	}
	
	public String getAmount() {
		if (!metadataMappings.get(AMOUNT).isEmpty()) {
			return metadataMappings.get(AMOUNT).get(0);
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
		return null;
	}
	
	public String getExternalIdentifierType(String identifier) {
		if (!metadataMappings.get(identifier).isEmpty()) {
			return metadataMappings.get(identifier).get(0);
		}
		return null;
	}
	
	public String getStartYear() {
		if (!metadataMappings.get(STARTDATE_YEAR).isEmpty()) {
			return metadataMappings.get(STARTDATE_YEAR).get(0);
		}
		return null;
	}

	public String getStartMonth() {
		if (!metadataMappings.get(STARTDATE_MONTH).isEmpty()) {
			return metadataMappings.get(STARTDATE_MONTH).get(0);
		}
		return null;
	}

	public String getStartDay() {
		if (!metadataMappings.get(STARTDATE_DAY).isEmpty()) {
			return metadataMappings.get(STARTDATE_DAY).get(0);
		}
		return null;
	}
	
	public String getEndYear() {
		if (!metadataMappings.get(ENDDATE_YEAR).isEmpty()) {
			return metadataMappings.get(ENDDATE_YEAR).get(0);
		}
		return null;
	}

	public String getEndMonth() {
		if (!metadataMappings.get(ENDDATE_MONTH).isEmpty()) {
			return metadataMappings.get(ENDDATE_MONTH).get(0);
		}
		return null;
	}

	public String getEndDay() {
		if (!metadataMappings.get(ENDDATE_DAY).isEmpty()) {
			return metadataMappings.get(ENDDATE_DAY).get(0);
		}
		return null;
	}

	public List<String> getContributorsLead() {
		if (!metadataMappings.get(CONTRIBUTORSLEAD).isEmpty()) {
			return metadataMappings.get(CONTRIBUTORSLEAD);
		}
		return null;
	}
	public List<String> getContributorsCoLead() {
		if (!metadataMappings.get(CONTRIBUTORSCOLEAD).isEmpty()) {
			return metadataMappings.get(CONTRIBUTORSCOLEAD);
		}
		return null;
	}
	
	public String getType() {
		if (!metadataMappings.get(TYPE).isEmpty()) {
			return metadataMappings.get(TYPE).get(0);
		}
		return "grant";
	}
	
	public String getOrganization() {
		if (!metadataMappings.get(ORGANIZATION).isEmpty()) {
			return metadataMappings.get(ORGANIZATION).get(0);
		}
		return null;
	}
	
	public String getOrganizationCity() {
		if (!metadataMappings.get(ORGANIZATION_CITY).isEmpty()) {
			return metadataMappings.get(ORGANIZATION_CITY).get(0);
		}
		return null;
	}
	
	public String getOrganizationCountry() {
		if (!metadataMappings.get(ORGANIZATION_COUNTRY).isEmpty()) {
			return metadataMappings.get(ORGANIZATION_COUNTRY).get(0);
		}
		return null;
	}
}