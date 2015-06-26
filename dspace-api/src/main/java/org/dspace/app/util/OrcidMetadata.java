/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.jdom.Element;

/**
 * 
 * Based on GoogleMetadata crosswalk
 * 
 * @author l.pascarelli
 *
 */
@SuppressWarnings("deprecation")
public class OrcidMetadata {

	private final static Logger log = Logger.getLogger(OrcidMetadata.class);

	private static final String ORCID_PREFIX = "orcid.";

	private Item item;

	private String itemURL;

	// Configuration keys and fields
	private static Map<String, String> configuredFields = new HashMap<String, String>();

	// Orcid field names (e.g. fieldname) and formatted metadata values
	private Map<String, String> metadataMappings = new HashMap<String, String>();

	public static final String TITLE = "title";

	public static final String SUBTITLE = "sub_title";

	public static final String JOURNAL_TITLE = "journal_title";

	public static final String ABSTRACT = "short_description";

	public static final String TYPE = "work_type";

	public static final String CITATION = "citation";

	public static final String ISSUE = "publication_date";

	private static final int SINGLE = 0;

	private static final int MULTI = 1;

	private static final int ALL_FIELDS_IN_OPTION = 2;

	// Load configured fields from orcid-metadata.properties
	static {

		File loadedFile = null;
		URL url = null;
		InputStream is = null;

		String googleConfigFile = ConfigurationManager.getProperty("orcid-metadata.config");
		log.info("Using [" + googleConfigFile + "] for ORCID Metadata configuration");

		loadedFile = new File(googleConfigFile);
		try {
			url = loadedFile.toURL();

		} catch (MalformedURLException mux) {
			log.error("Can't find ORCID Metadata configuration file: " + googleConfigFile, mux);
		}

		Properties properties = new Properties();
		try {
			is = url.openStream();
			properties.load(is);

		} catch (IOException iox) {
			log.error("Could not read Google Metadata configuration file: " + googleConfigFile, iox);
		}

		Enumeration propertyNames = properties.propertyNames();

		while (propertyNames.hasMoreElements()) {
			String key = ((String) propertyNames.nextElement()).trim();

			if (key.startsWith(ORCID_PREFIX)) {

				String name = key.substring(ORCID_PREFIX.length());
				String field = properties.getProperty(key);

				if (null != name && !name.equals("") && null != field && !field.equals("")) {
					configuredFields.put(name.trim(), field.trim());
				}
			}
		}

		if (log.isDebugEnabled()) {
			logConfiguration();
		}
	}

	/**
	 * Dump Metadata field mapping to log
	 * 
	 */
	public static void logConfiguration() {
		log.debug("Google Metadata Configuration Mapping:");

		for (String name : configuredFields.keySet()) {
			log.debug("  " + name + " => " + configuredFields.get(name));
		}
	}

	/**
	 * Wrap the item, parse all configured fields and generate metadata field
	 * values.
	 * 
	 * @param item
	 *            - The item being viewed to extract metadata from
	 */
	public OrcidMetadata(Context context, Item item) throws SQLException {

		// Hold onto the item in case we need to refresh a stale parse
		this.item = item;
		itemURL = HandleManager.resolveToURL(context, item.getHandle());
		parseItem();
	}

	/**
	 * Add a single metadata value to the Google field, defaulting to the
	 * first-encountered instance of the field for this Item.
	 * 
	 * @param fieldName
	 * @param schema
	 * @param element
	 * @param qualifier
	 * @return
	 */
	private boolean addSingleField(String fieldName) {

		String config = configuredFields.get(fieldName);

		if (null == config || config.equals("")) {
			return false;
		}

		if (log.isDebugEnabled()) {
			log.debug("Processing " + fieldName);
		}

		if (config.equals("$handle")) {
			if (null != itemURL && !itemURL.equals("")) {
				metadataMappings.put(fieldName, itemURL);
				return true;
			} else {
				return false;
			}
		}

		if (config.equals("$simple-pdf")) {
			String pdf_url = getPDFSimpleUrl(item);
			if (pdf_url.length() > 0) {
				metadataMappings.put(fieldName, pdf_url);
				return true;
			} else {
				return false;
			}
		}

		DCValue v = resolveMetadataField(config);

		if (null != v && (null != v.value) && !v.value.trim().equals("")) {
			metadataMappings.put(fieldName, v.value);
			return true;
		} else {
			// No values found
			return false;
		}
	}

	/**
	 * A singular version of resolveMetadata to return only one field value
	 * instead of an aggregate.
	 * 
	 * @param configFilter
	 * @return The first configured match of metadata field for the item.
	 */
	private DCValue resolveMetadataField(String configFilter) {

		ArrayList<DCValue> fields = resolveMetadata(configFilter, SINGLE);
		if (null != fields && fields.size() > 0) {
			return fields.get(0);
		}

		return null;
	}

	/**
	 * A plural version of resolveMetadata for aggregate fields.
	 * 
	 * @param configFilter
	 * @return Aggregate of all matching metadata fields configured in the first
	 *         option field-set to return any number of filter matches.
	 */
	private ArrayList<DCValue> resolveMetadataFields(String configFilter) {

		ArrayList<DCValue> fields = resolveMetadata(configFilter, MULTI);
		if (null != fields && fields.size() > 0) {
			return fields;
		}
		return null;
	}

	/**
	 * Aggregate an array of DCValues present on the current item that pass the
	 * configuration filter.
	 * 
	 * @param configValue
	 * @return Array of configuration -> item-field matches
	 */
	private ArrayList<DCValue> resolveMetadata(String configFilter, int returnType) {

		if (null == configFilter || configFilter.trim().equals("") || !configFilter.contains(".")) {
			log.error("The configuration string [" + configFilter + "] is invalid.");
			return null;
		} else {
			configFilter = configFilter.trim();
		}
		ArrayList<ArrayList<String>> parsedOptions = new ArrayList<ArrayList<String>>();
		parsedOptions = parseOptions(configFilter);

		if (log.isDebugEnabled()) {
			log.debug("Resolved Fields For This Item Per Configuration Filter:");
			for (int i = 0; i < parsedOptions.size(); i++) {
				ArrayList<String> optionFields = parsedOptions.get(i);

				log.debug("Option " + (i + 1) + ":");
				for (String f : optionFields) {
					log.debug("{" + f + "}");
				}
			}
		}

		// Iterate through each configured option's field-set until
		// we have a match.
		for (ArrayList<String> optionFields : parsedOptions) {

			int optionMatches = 0;
			String[] components;
			DCValue[] values;
			ArrayList<DCValue> resolvedFields = new ArrayList<DCValue>();

			for (String field : optionFields) {

				components = parseComponents(field);
				values = item.getMetadata(components[0], components[1], components[2], Item.ANY);

				if (values.length > 0) {
					for (DCValue v : values) {

						resolvedFields.add(v);

						if (returnType == SINGLE) {
							if (!resolvedFields.isEmpty()) {
								if (log.isDebugEnabled()) {
									log.debug("Resolved Field Value For This Item:");
									for (DCValue r : resolvedFields) {
										log.debug("{" + r.value + "}");
									}
								}
								return resolvedFields;
							}
						}
					}
				}
			}

			// If the item had any of the fields contained in this option,
			// return them, otherwise move on to the next option's field-set.
			if (!resolvedFields.isEmpty()) {
				if (log.isDebugEnabled()) {
					log.debug("Resolved Field Values For This Item:");
					for (DCValue v : resolvedFields) {
						log.debug("{" + v.value + "}");
					}
				}

				// Check to see if this is a full option match
				if (ALL_FIELDS_IN_OPTION == returnType) {
					if (resolvedFields.size() == optionMatches) {
						return resolvedFields;
					}
					// Otherwise, if there are any matches for the option,
					// return them.
				} else if (MULTI == returnType) {
					return resolvedFields;
				}
			}
		}
		return null;
	}

	/**
	 * Parse first-match path of metadata field-group options for the given
	 * configuration.
	 * 
	 * @param configFilter
	 * @return
	 */
	private ArrayList<ArrayList<String>> parseOptions(String configFilter) {

		ArrayList<String> options = new ArrayList<String>();
		ArrayList<ArrayList<String>> parsedOptions = new ArrayList<ArrayList<String>>();

		if (null == configFilter || configFilter.equals("")) {
			return null;
		}

		if (configFilter.contains("|")) {

			String[] configOptions = configFilter.split("\\|");

			for (String option : configOptions) {
				options.add(option.trim());
			}
		} else {
			options = new ArrayList<String>();
			options.add(configFilter);
		}

		// Parse first-match path options. The first option (field-set)
		// to match fields present in the item is used.
		ArrayList<String> parsedFields;

		// Parse the fields for each field-set in order.
		for (String option : options) {

			ArrayList<String> fields;
			parsedFields = new ArrayList<String>();

			if (option.contains(",")) {
				fields = parseFields(option);
			} else {
				fields = new ArrayList<String>();
				fields.add(option);
			}

			// Parse field list for this field-set, expanding any wildcards.
			for (String field : fields) {

				if (field.contains("*")) {

					ArrayList<String> wc = parseWildcard(field);
					for (String wcField : wc) {
						if (!parsedFields.contains(wcField)) {
							parsedFields.add(wcField);
						}
					}

				} else {
					if (!parsedFields.contains(field)) {
						parsedFields.add(field);
					}
				}
			}

			parsedOptions.add(parsedFields);
		}

		return parsedOptions;
	}

	/**
	 * Build a Vector of fields that can be added to when expanding wildcards.
	 * 
	 * @param configString
	 *            - Value of one metadata field configuration
	 * @return A vector of raw field configurations.
	 */
	private ArrayList<String> parseFields(String configString) {

		ArrayList<String> fields = new ArrayList<String>();

		for (String field : configString.split("\\,")) {
			fields.add(field.trim());
		}

		return fields;
	}

	/**
	 * Pull apart an individual field structure.
	 * 
	 * @param The
	 *            configured field for one metadata field map
	 * @return Schema, Element, Qualifier of metadata field
	 */
	private String[] parseComponents(String field) {

		int index = 0;
		String[] components = new String[3];

		for (String c : field.split("\\.")) {
			components[index] = c.trim();
			index++;
		}

		return components;
	}

	/**
	 * Expand any wildcard characters to an array of all matching fields for
	 * this item. No order consistency is implied.
	 * 
	 * @param field
	 *            The field identifier containing a wildcard character.
	 * @return Expanded field list.
	 */
	private ArrayList<String> parseWildcard(String field) {

		if (!field.contains("*")) {
			return null;
		} else {
			String[] components = parseComponents(field);

			for (int i = 0; i < components.length; i++) {
				if (components[i].trim().equals("*")) {
					components[i] = Item.ANY;
				}
			}

			DCValue[] allMD = item.getMetadata(components[0], components[1], components[2], Item.ANY);

			ArrayList<String> expandedDC = new ArrayList<String>();
			for (DCValue v : allMD) {

				// De-dup multiple occurrences of field names in item
				if (!expandedDC.contains(buildFieldName(v))) {
					expandedDC.add(buildFieldName(v));
				}
			}

			if (log.isDebugEnabled()) {
				log.debug("Field Names From Expanded Wildcard \"" + field + "\"");
				for (String v : expandedDC) {
					log.debug("    " + v);
				}
			}

			return expandedDC;
		}
	}

	/**
	 * Construct metadata field name out of DCValue components
	 * 
	 * @param v
	 *            The DCValue to construct a name for.
	 * @return The complete metadata field name.
	 */
	private String buildFieldName(DCValue v) {

		StringBuilder name = new StringBuilder();

		name.append(v.schema + "." + v.element);
		if (null != v.qualifier) {
			name.append("." + v.qualifier);
		}

		return name.toString();
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
		addSingleField(ISSUE);

		// ABSTRACT_HTML_URL ('$handle' variable substitution if present)
		addSingleField(ABSTRACT);

	}

	/**
	 * Fetch all metadata mappings
	 * 
	 * Usage: GoogleMetadata gmd = new GoogleMetadata(item); for(Entry<String,
	 * String> mapping : googlemd.getMappings()) { ... }
	 * 
	 * @return Iterable of metadata fields mapped to Google-formatted values
	 */
	public Set<Entry<String, String>> getMappings() {

		return new HashSet<Entry<String, String>>(metadataMappings.entrySet());
	}

	/**
	 * Produce meta elements that can easily be put into the head.
	 */
	public List<Element> disseminateList() {
		List<Element> metas = new ArrayList<Element>();

		for (Entry<String, String> m : getMappings()) {
			Element e = new Element("meta");
			e.setNamespace(null);
			e.setAttribute("name", m.getKey());
			e.setAttribute("content", m.getValue());
			metas.add(e);
		}
		return metas;
	}

	// Getters for individual metadata fields...

	/**
	 * @return the citation_title
	 */
	public String getTitle() {
		return metadataMappings.get(TITLE);
	}

	/**
	 * @return the citation_journal_title
	 */
	public String getJournalTitle() {
		return metadataMappings.get(JOURNAL_TITLE);
	}

	/**
	 * @return the citation_issue
	 */
	public String getIssue() {
		return metadataMappings.get(ISSUE);
	}

	/**
	 * @return the citation_abstract_html_url
	 */
	public String getAbstractHTMLURL() {
		return metadataMappings.get(ABSTRACT);
	}

	/**
	 * Gets the URL to a PDF using a very basic strategy by assuming that the
	 * PDF is in the default content bundle, and that the item only has one
	 * public bitstream and it is a PDF.
	 *
	 * @param item
	 * @return URL that the PDF can be directly downloaded from
	 */
	private String getPDFSimpleUrl(Item item) {
		try {
			Bitstream bitstream = findLinkableFulltext(item);
			if (bitstream != null) {
				StringBuilder path = new StringBuilder();
				path.append(ConfigurationManager.getProperty("dspace.url"));

				if (item.getHandle() != null) {
					path.append("/bitstream/");
					path.append(item.getHandle());
					path.append("/");
					path.append(bitstream.getSequenceID());
				} else {
					path.append("/retrieve/");
					path.append(bitstream.getID());
				}

				path.append("/");
				path.append(Util.encodeBitstreamName(bitstream.getName(), Constants.DEFAULT_ENCODING));
				return path.toString();
			}
		} catch (UnsupportedEncodingException ex) {
			log.debug(ex.getMessage());
		} catch (SQLException ex) {
			log.debug(ex.getMessage());
		}

		return "";
	}

	/**
	 * A bitstream is considered linkable fulltext when it is either
	 * <ul>
	 * <li>the item's only bitstream (in the ORIGINAL bundle); or</li>
	 * <li>the primary bitstream</li>
	 * </ul>
	 * Additionally, this bitstream must be publicly viewable.
	 * 
	 * @param item
	 * @return
	 * @throws SQLException
	 */
	private Bitstream findLinkableFulltext(Item item) throws SQLException {
		Bitstream bestSoFar = null;
		int bitstreamCount = 0;
		Bundle[] contentBundles = item.getBundles("ORIGINAL");
		for (Bundle bundle : contentBundles) {
			int primaryBitstreamId = bundle.getPrimaryBitstreamID();
			Bitstream[] bitstreams = bundle.getBitstreams();
			for (Bitstream candidate : bitstreams) {
				if (candidate.getID() == primaryBitstreamId) { // is primary ->
																// use this one
					if (isPublic(candidate)) {
						return candidate;
					}
				} else if (bestSoFar == null) {
					bestSoFar = candidate;
				}
				bitstreamCount++;
			}
		}
		if (bitstreamCount > 1 || !isPublic(bestSoFar)) {
			bestSoFar = null;
		}

		return bestSoFar;
	}

	private boolean isPublic(Bitstream bitstream) {
		if (bitstream == null) {
			return false;
		}
		boolean result = false;
		Context context = null;
		try {
			context = new Context();
			result = AuthorizeManager.authorizeActionBoolean(context, bitstream, Constants.READ, true);
		} catch (SQLException e) {
			log.error("Cannot determine whether bitstream is public, assuming it isn't. bitstream_id="
					+ bitstream.getID(), e);
		} finally {
			if (context != null) {
				context.abort();
			}
		}
		return result;
	}

	/**
	 * 
	 * 
	 * @param Field
	 *            to aggregate all values of in a matching option
	 * @param delimiter
	 *            to delimit field values with
	 */
	private void addAggregateValues(String FIELD, String delim) {

		String authorConfig = configuredFields.get(FIELD);
		ArrayList<DCValue> fields = resolveMetadataFields(authorConfig);

		if (null != fields && !fields.isEmpty()) {

			StringBuilder fieldMetadata = new StringBuilder();
			int count = 0;

			for (DCValue field : fields) {
				fieldMetadata.append(field.value);
				if (count < fields.size() - 1) {
					fieldMetadata.append(delim + " ");
					count++;
				}
			}
			metadataMappings.put(FIELD, fieldMetadata.toString());
		}
	}

}