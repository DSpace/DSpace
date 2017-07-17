/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.jdom.Element;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

/**
 * Configuration and mapping for Google Scholar output metadata
 * @author Sands Fish
 * 
 */

@SuppressWarnings("deprecation")
public class GoogleMetadata
{

    private final static Logger log = Logger.getLogger(GoogleMetadata.class);

    private static final String GOOGLE_PREFIX = "google.";

    private Item item;

    private String itemURL;

    // Configuration keys and fields
    private static Map<String, String> configuredFields = new HashMap<String, String>();

    // Google field names (e.g. citation_fieldname) and formatted metadata
    // values
    private ListMultimap<String, String> metadataMappings = ArrayListMultimap.create();

    public static final String TITLE = "citation_title";

    public static final String JOURNAL_TITLE = "citation_journal_title";

    public static final String PUBLISHER = "citation_publisher";

    public static final String AUTHORS = "citation_author";

    public static final String DATE = "citation_date";

    public static final String VOLUME = "citation_volume";

    public static final String ISSUE = "citation_issue";

    public static final String FIRSTPAGE = "citation_firstpage";

    public static final String LASTPAGE = "citation_lastpage";

    public static final String DOI = "citation_doi";

    public static final String PMID = "citation_pmid";

    public static final String ABSTRACT = "citation_abstract_html_url";

    public static final String FULLTEXT = "citation_fulltext_html_url";

    public static final String PDF = "citation_pdf_url";

    public static final String ISSN = "citation_issn";

    public static final String ISBN = "citation_isbn";

    public static final String LANGUAGE = "citation_language";

    public static final String KEYWORDS = "citation_keywords";

    public static final String CONFERENCE = "citation_conference";

    public static final String DISSERTATION_ID = "identifiers.dissertation";

    public static final String DISSERTATION_NAME = "citation_dissertation_name";

    public static final String DISSERTATION_INSTITUTION = "citation_dissertation_institution";

    public static final String PATENT_ID = "identifiers.patent";

    public static final String PATENT_NUMBER = "citation_patent_number";

    public static final String PATENT_COUNTRY = "citation_patent_country";

    public static final String TECH_REPORT_ID = "identifiers.technical_report";

    public static final String TECH_REPORT_NUMBER = "citation_technical_report_number";

    public static final String TECH_REPORT_INSTITUTION = "citation_technical_report_institution";

    private static final int SINGLE = 0;

    private static final int MULTI = 1;

    private static final int ALL_FIELDS_IN_OPTION = 2;

    private Context ourContext;
    // Load configured fields from google-metadata.properties
    static
    {

        File loadedFile = null;
        URL url = null;
        InputStream is = null;

        String googleConfigFile = ConfigurationManager
                .getProperty("google-metadata.config");
        log.info("Using [" + googleConfigFile
                + "] for Google Metadata configuration");

        loadedFile = new File(googleConfigFile);
        try
        {
            url = loadedFile.toURL();

        }
        catch (MalformedURLException mux)
        {
            log.error("Can't find Google Metadata configuration file: "
                    + googleConfigFile, mux);
        }

        Properties properties = new Properties();
        try
        {
            is = url.openStream();
            properties.load(is);

        }
        catch (IOException iox)
        {
            log.error("Could not read Google Metadata configuration file: "
                    + googleConfigFile, iox);
        }

        Enumeration propertyNames = properties.propertyNames();

        while (propertyNames.hasMoreElements())
        {
            String key = ((String) propertyNames.nextElement()).trim();

            if (key.startsWith(GOOGLE_PREFIX))
            {

                String name = key.substring(GOOGLE_PREFIX.length());
                String field = properties.getProperty(key);

                if (null != name && !name.equals("") && null != field
                        && !field.equals(""))
                {
                    configuredFields.put(name.trim(), field.trim());
                }
            }
        }

        if (log.isDebugEnabled())
        {
            logConfiguration();
        }
    }

    /**
     * Dump Metadata field mapping to log
     * 
     */
    public static void logConfiguration()
    {
        log.debug("Google Metadata Configuration Mapping:");

        for (String name : configuredFields.keySet())
        {
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
    public GoogleMetadata(Context context, Item item) throws SQLException
    {

        // Hold onto the item in case we need to refresh a stale parse
        this.item = item;
        itemURL = HandleManager.resolveToURL(context, item.getHandle());
        ourContext=context;
        EPerson currentUser = ourContext.getCurrentUser();
        ourContext.setCurrentUser(null);
        parseItem();
        ourContext.setCurrentUser(currentUser);
    }

    /**
     * Add a single metadata value to the Google field, defaulting to the
     * first-encountered instance of the field for this Item.
     * 
     * @param fieldName
     * @return
     */
    private boolean addSingleField(String fieldName)
    {

        String config = configuredFields.get(fieldName);

        if (null == config || config.equals(""))
        {
            return false;
        }

        if (log.isDebugEnabled())
        {
            log.debug("Processing " + fieldName);
        }

        if (config.equals("$handle"))
        {
            if (null != itemURL && !itemURL.equals(""))
            {
                metadataMappings.put(fieldName, itemURL);
                return true;
            }
            else
            {
                return false;
            }
        }

        if (config.equals("$simple-pdf"))
        {
            String pdf_url = getPDFSimpleUrl(item);
            if(pdf_url.length() > 0)
            {
                metadataMappings.put(fieldName, pdf_url);
                return true;
            } else
            {
                return false;
            }
        }

        Metadatum v = resolveMetadataField(config);

        if (null != v && (null != v.value) && !v.value.trim().equals(""))
        {
            metadataMappings.put(fieldName, v.value);
            return true;
        }
        else
        {
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
    private Metadatum resolveMetadataField(String configFilter)
    {

        ArrayList<Metadatum> fields = resolveMetadata(configFilter, SINGLE);
        if (null != fields && fields.size() > 0)
        {
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
    private ArrayList<Metadatum> resolveMetadataFields(String configFilter)
    {

        ArrayList<Metadatum> fields = resolveMetadata(configFilter, MULTI);
        if (null != fields && fields.size() > 0)
        {
            return fields;
        }
        return null;
    }

    /**
     * Aggregate an array of DCValues present on the current item that pass the
     * configuration filter.
     * 
     * @param configFilter
     * @param returnType
     * @return Array of configuration -> item-field matches
     */
    private ArrayList<Metadatum> resolveMetadata(String configFilter,
            int returnType)
    {

        if (null == configFilter || configFilter.trim().equals("")
                || !configFilter.contains("."))
        {
            log.error("The configuration string [" + configFilter
                    + "] is invalid.");
            return null;
        }
        else
        {
            configFilter = configFilter.trim();
        }
        ArrayList<ArrayList<String>> parsedOptions = new ArrayList<ArrayList<String>>();
        parsedOptions = parseOptions(configFilter);

        if (log.isDebugEnabled())
        {
            log
                    .debug("Resolved Fields For This Item Per Configuration Filter:");
            for (int i = 0; i < parsedOptions.size(); i++)
            {
                ArrayList<String> optionFields = parsedOptions.get(i);

                log.debug("Option " + (i + 1) + ":");
                for (String f : optionFields)
                {
                    log.debug("{" + f + "}");
                }
            }
        }

        // Iterate through each configured option's field-set until
        // we have a match.
        for (ArrayList<String> optionFields : parsedOptions)
        {

            int optionMatches = 0;
            String[] components;
            Metadatum[] values;
            ArrayList<Metadatum> resolvedFields = new ArrayList<Metadatum>();

            for (String field : optionFields)
            {

                components = parseComponents(field);
                values = item.getMetadata(components[0], components[1],
                        components[2], Item.ANY);

                if (values.length > 0)
                {
                    for (Metadatum v : values)
                    {

                        resolvedFields.add(v);

                        if (returnType == SINGLE)
                        {
                            if (!resolvedFields.isEmpty())
                            {
                                if (log.isDebugEnabled()) {
                                    log.debug("Resolved Field Value For This Item:");
                                    for (Metadatum r : resolvedFields)
                                    {
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
            if (!resolvedFields.isEmpty())
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Resolved Field Values For This Item:");
                    for (Metadatum v : resolvedFields)
                    {
                        log.debug("{" + v.value + "}");
                    }
                }

                // Check to see if this is a full option match
                if (ALL_FIELDS_IN_OPTION == returnType)
                {
                    if (resolvedFields.size() == optionMatches)
                    {
                        return resolvedFields;
                    }
                    // Otherwise, if there are any matches for the option,
                    // return them.
                }
                else if (MULTI == returnType)
                {
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
    private ArrayList<ArrayList<String>> parseOptions(String configFilter)
    {

        ArrayList<String> options = new ArrayList<String>();
        ArrayList<ArrayList<String>> parsedOptions = new ArrayList<ArrayList<String>>();

        if (null == configFilter || configFilter.equals(""))
        {
            return null;
        }

        if (configFilter.contains("|"))
        {

            String[] configOptions = configFilter.split("\\|");

            for (String option : configOptions)
            {
                options.add(option.trim());
            }
        }
        else
        {
            options = new ArrayList<String>();
            options.add(configFilter);
        }

        // Parse first-match path options. The first option (field-set)
        // to match fields present in the item is used.
        ArrayList<String> parsedFields;

        // Parse the fields for each field-set in order.
        for (String option : options)
        {

            ArrayList<String> fields;
            parsedFields = new ArrayList<String>();

            if (option.contains(","))
            {
                fields = parseFields(option);
            }
            else
            {
                fields = new ArrayList<String>();
                fields.add(option);
            }

            // Parse field list for this field-set, expanding any wildcards.
            for (String field : fields)
            {

                if (field.contains("*"))
                {

                    ArrayList<String> wc = parseWildcard(field);
                    for (String wcField : wc)
                    {
                        if (!parsedFields.contains(wcField))
                        {
                            parsedFields.add(wcField);
                        }
                    }

                }
                else
                {
                    if (!parsedFields.contains(field))
                    {
                        parsedFields.add(field);
                    }
                }
            }

            parsedOptions.add(parsedFields);
        }

        if (null != parsedOptions)
        {
            return parsedOptions;
        }
        else
        {
            return null;
        }
    }

    /**
     * Build a Vector of fields that can be added to when expanding wildcards.
     * 
     * @param configString
     *            - Value of one metadata field configuration
     * @return A vector of raw field configurations.
     */
    private ArrayList<String> parseFields(String configString)
    {

        ArrayList<String> fields = new ArrayList<String>();

        for (String field : configString.split("\\,"))
        {
            fields.add(field.trim());
        }

        return fields;
    }

    /**
     * Pull apart an individual field structure.
     * 
     * @param field
     *            The configured field for one metadata field map
     * @return Schema, Element, Qualifier of metadata field
     */
    private String[] parseComponents(String field)
    {

        int index = 0;
        String[] components = new String[3];

        for (String c : field.split("\\."))
        {
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
    private ArrayList<String> parseWildcard(String field)
    {

        if (!field.contains("*"))
        {
            return null;
        }
        else
        {
            String[] components = parseComponents(field);

            for (int i = 0; i < components.length; i++)
            {
                if (components[i].trim().equals("*"))
                {
                    components[i] = Item.ANY;
                }
            }

            Metadatum[] allMD = item.getMetadata(components[0], components[1],
                    components[2], Item.ANY);

            ArrayList<String> expandedDC = new ArrayList<String>();
            for (Metadatum v : allMD)
            {

                // De-dup multiple occurrences of field names in item
                if (!expandedDC.contains(buildFieldName(v)))
                {
                    expandedDC.add(buildFieldName(v));
                }
            }

            if (log.isDebugEnabled())
            {
                log.debug("Field Names From Expanded Wildcard \"" + field
                        + "\"");
                for (String v : expandedDC)
                {
                    log.debug("    " + v);
                }
            }

            return expandedDC;
        }
    }

    /**
     * Construct metadata field name out of Metadatum components
     * 
     * @param v
     *            The Metadatum to construct a name for.
     * @return The complete metadata field name.
     */
    private String buildFieldName(Metadatum v)
    {

        StringBuilder name = new StringBuilder();

        name.append(v.schema + "." + v.element);
        if (null != v.qualifier)
        {
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
    private void parseItem()
    {

        // TITLE
        addSingleField(TITLE);

        // AUTHORS (multi)
        addMultipleValues(AUTHORS);

        // DATE
        addSingleField(DATE);

        // ISSN
        addSingleField(ISSN);

        // ISBN
        addSingleField(ISBN);

        // JOURNAL_TITLE
        addSingleField(JOURNAL_TITLE);

        // VOLUME
        addSingleField(VOLUME);

        // ISSUE
        addSingleField(ISSUE);

        // FIRSTPAGE
        addSingleField(FIRSTPAGE);

        // LASTPAGE
        addSingleField(LASTPAGE);

        // DOI
        addSingleField(DOI);

        // PMID
        addSingleField(PMID);

        // ABSTRACT_HTML_URL ('$handle' variable substitution if present)
        addSingleField(ABSTRACT);

        // FULLTEXT_HTML_URL ('$handle' variable substitution if present)
        addSingleField(FULLTEXT);

        // PDF_URL ('$handle' variable substitution if present)
        addSingleField(PDF);

        // LANGUAGE
        addSingleField(LANGUAGE);

        // KEYWORDS (multi)
        addAggregateValues(KEYWORDS, ";");

        // CONFERENCE
        addSingleField(CONFERENCE);

        // Dissertations
        if (itemIsDissertation())
        {
            if(log.isDebugEnabled()) {
                log.debug("ITEM TYPE:  DISSERTATION");
            }

            addSingleField(DISSERTATION_NAME);
            addSingleField(DISSERTATION_INSTITUTION);
        }

        // Patents
        if (itemIsPatent())
        {
            if(log.isDebugEnabled()) {
                log.debug("ITEM TYPE:  PATENT");
            }

            addSingleField(PATENT_NUMBER);

            // Use config value for patent country. Should be a literal.
            String countryConfig = configuredFields.get(PATENT_COUNTRY);
            if (null != countryConfig && !countryConfig.trim().equals(""))
            {
                metadataMappings.put(PATENT_COUNTRY, countryConfig.trim());
            }

            addSingleField(PUBLISHER);
        }

        // Tech Reports
        if (itemIsTechReport())
        {
            if(log.isDebugEnabled()) {
                log.debug("ITEM TYPE:  TECH REPORT");
            }
            addSingleField(TECH_REPORT_NUMBER);
            addSingleField(TECH_REPORT_INSTITUTION);
        }


        if(!itemIsDissertation() && !itemIsTechReport()) {
            // PUBLISHER
            addSingleField(PUBLISHER);
        }
    }

    /**
     * Fetch retaining the order of the values for any given key in which they
     * where added (like authors).
     *
     * Usage: GoogleMetadata gmd = new GoogleMetadata(item); for(Entry<String,
     * String> mapping : googlemd.getMappings()) { ... }
     * 
     * @return Iterable of metadata fields mapped to Google-formatted values
     */
    public Collection<Entry<String, String>> getMappings()
    {
        return metadataMappings.entries();
    }

    /**
     * Produce meta elements that can easily be put into the head.
     */
    public List<Element> disseminateList()
    {
        List<Element> metas = new ArrayList<Element>();

        for (Entry<String, String> m : getMappings())
        {
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
    public List<String> getTitle()
    {
        return metadataMappings.get(TITLE);
    }

    /**
     * @return the citation_journal_title
     */
    public List<String> getJournalTitle()
    {
        return metadataMappings.get(JOURNAL_TITLE);
    }

    /**
     * @return the citation_publisher
     */
    public List<String> getPublisher()
    {
        return metadataMappings.get(PUBLISHER);
    }

    /**
     * @return the citation_authors
     */
    public List<String> getAuthors()
    {
        return metadataMappings.get(AUTHORS);
    }

    /**
     * @return the citation_date
     */
    public List<String> getDate()
    {
        return metadataMappings.get(DATE);
    }

    /**
     * @return the citation_volume
     */
    public List<String> getVolume()
    {
        return metadataMappings.get(VOLUME);
    }

    /**
     * @return the citation_issue
     */
    public List<String> getIssue()
    {
        return metadataMappings.get(ISSUE);
    }

    /**
     * @return the citation_firstpage
     */
    public List<String> getFirstpage()
    {
        return metadataMappings.get(FIRSTPAGE);
    }

    /**
     * @return the citation_lastpage
     */
    public List<String> getLastpage()
    {
        return metadataMappings.get(LASTPAGE);
    }

    /**
     * @return the citation_doi
     */
    public List<String> getDOI()
    {
        return metadataMappings.get(DOI);
    }

    /**
     * @return the citation_pmid
     */
    public List<String> getPmid()
    {
        return metadataMappings.get(PMID);
    }

    /**
     * @return the citation_abstract_html_url
     */
    public List<String> getAbstractHTMLURL()
    {
        return metadataMappings.get(ABSTRACT);
    }

    /**
     * @return the citation_fulltext_html_url
     */
    public List<String> getFulltextHTMLURL()
    {
        return metadataMappings.get(FULLTEXT);
    }

    /**
     * @return the citation_pdf_url
     */
    public List<String> getPDFURL()
    {
        return metadataMappings.get(PDF);
    }

    /**
     * @return the citation_issn
     */
    public List<String> getISSN()
    {
        return metadataMappings.get(ISSN);
    }

    /**
     * @return the citation_isbn
     */
    public List<String> getISBN()
    {
        return metadataMappings.get(ISBN);
    }

    /**
     * @return the citation_language
     */
    public List<String> getLanguage()
    {
        return metadataMappings.get(LANGUAGE);
    }

    /**
     * @return the citation_keywords
     */
    public List<String> getKeywords()
    {
        return metadataMappings.get(KEYWORDS);
    }

    /**
     * @return the citation_conference
     */
    public List<String> getConference()
    {
        return metadataMappings.get(CONFERENCE);
    }

    /**
     * @return the citation_dissertation_name
     */
    public List<String> getDissertationName()
    {
        return metadataMappings.get(DISSERTATION_NAME);
    }

    /**
     * @return the citation_dissertation_institution
     */
    public List<String> getDissertationInstitution()
    {
        return metadataMappings.get(DISSERTATION_INSTITUTION);
    }

    /**
     * @return the citation_patent_number
     */
    public List<String> getPatentNumber()
    {
        return metadataMappings.get(PATENT_NUMBER);
    }

    /**
     * @return the citation_patent_country
     */
    public List<String> getPatentCountry()
    {
        return metadataMappings.get(PATENT_COUNTRY);
    }

    /**
     * @return the citation_technical_report_number
     */
    public List<String> getTechnicalReportNumber()
    {
        return metadataMappings.get(TECH_REPORT_NUMBER);
    }

    /**
     * @return the citation_technical_report_institution
     */
    public List<String> getTechnicalReportInstitution()
    {
        return metadataMappings.get(TECH_REPORT_INSTITUTION);
    }

    /**
     * Gets the URL to a PDF using a very basic strategy by assuming that the PDF
     * is in the default content bundle, and that the item only has one public bitstream
     * and it is a PDF.
     *
     * @param item
     * @return URL that the PDF can be directly downloaded from
     */
    private String getPDFSimpleUrl(Item item)
    {
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
	 *     <li>the item's only bitstream (in the ORIGINAL bundle); or</li>
	 *     <li>the primary bitstream</li>
	 * </ul>
	 * Additionally, this bitstream must be publicly viewable.
	 * @param item
	 * @return
	 * @throws SQLException
	 */
	private Bitstream findLinkableFulltext(Item item) throws SQLException {
		Bitstream bestSoFar = null;
		Bundle[] contentBundles = item.getBundles("ORIGINAL");
		for (Bundle bundle : contentBundles) {
			int primaryBitstreamId = bundle.getPrimaryBitstreamID();
			Bitstream[] bitstreams = bundle.getBitstreams();
			for (Bitstream candidate : bitstreams) {
				if (candidate.getID() == primaryBitstreamId) { // is primary -> use this one
					if (isPublic(candidate)) {
						return candidate;
					}					
				} else 
					{
						
						if (bestSoFar == null && isPublic(candidate)) { //if bestSoFar is null but the candidate is not public you don't use it and try to find another
						bestSoFar = candidate;
						}					
					}
			}
		}

		return bestSoFar;
	}

	private boolean isPublic(Bitstream bitstream) {
		if (bitstream == null) {
			return false;
		}
		boolean result = false;
		try {
            result = AuthorizeManager.authorizeActionBoolean(ourContext, bitstream, Constants.READ, true);
		} catch (SQLException e) {
			log.error("Cannot determine whether bitstream is public, assuming it isn't. bitstream_id=" + bitstream.getID(), e);
		}
		return result;
	}

	/**
     * 
     * 
     * @param FIELD
     *            to aggregate all values of in a matching option
     * @param delim
     *            to delimit field values with
     */
    private void addAggregateValues(String FIELD, String delim)
    {

        String authorConfig = configuredFields.get(FIELD);
        ArrayList<Metadatum> fields = resolveMetadataFields(authorConfig);

        if (null != fields && !fields.isEmpty())
        {

            StringBuilder fieldMetadata = new StringBuilder();
            int count = 0;

            for (Metadatum field : fields)
            {
                fieldMetadata.append(field.value);
                if (count < fields.size() - 1)
                {
                    fieldMetadata.append(delim + " ");
                    count++;
                }
            }
            metadataMappings.put(FIELD, fieldMetadata.toString());
        }
    }

    /**
     * If metadata field contains multiple values, then add each value to the map separately
     * @param FIELD
     */
    private void addMultipleValues(String FIELD)
    {
        String fieldConfig = configuredFields.get(FIELD);
        ArrayList<Metadatum> fields = resolveMetadataFields(fieldConfig);

        if (null != fields && !fields.isEmpty())
        {
            for (Metadatum field : fields)
            {
                //TODO if this is author field, first-name first
                metadataMappings.put(FIELD, field.value);
            }
        }
    }

    /**
     * Determine, based on config values, if this item is a dissertation.
     * 
     * @return boolean
     */
    private boolean itemIsDissertation()
    {

        String dConfig = configuredFields.get(DISSERTATION_ID);
        if (null == dConfig || dConfig.trim().equals(""))
        {
            return false;
        }
        else
        {
            return identifyItemType(dConfig);
        }
    }

    /**
     * Determine, based on config values, if this item is a patent.
     * 
     * @return boolean
     */
    private boolean itemIsPatent()
    {

        String dConfig = configuredFields.get(PATENT_ID);
        if (null == dConfig || dConfig.trim().equals(""))
        {
            return false;
        }
        else
        {
            return identifyItemType(dConfig);
        }
    }

    /**
     * Determine, based on config values, if this item is a tech report.
     * 
     * @return boolean
     */
    private boolean itemIsTechReport()
    {

        String dConfig = configuredFields.get(TECH_REPORT_ID);
        if (null == dConfig || dConfig.trim().equals(""))
        {
            return false;
        }
        else
        {
            return identifyItemType(dConfig);
        }
    }

    /**
     * Identifies if this item matches a particular configuration of fields and
     * values for those fields to identify the type based on a type- cataloging
     * metadata practice.
     * 
     * @param dConfig
     * @return
     */
    private boolean identifyItemType(String dConfig)
    {
        // FIXME: Shouldn't have to parse identifiers for every identification.

        ArrayList<ArrayList<String>> options = parseOptions(dConfig);
        HashMap<String, ArrayList<String>> mdPairs = new HashMap<String, ArrayList<String>>();

        // Parse field/value pairs from field identifier string
        for (ArrayList<String> option : options)
        {

            String pair = option.get(0);
            String[] parsedPair = pair.split("\\:");
            if (2 == parsedPair.length)
            {
                // If we've encountered this field before, add the value to the
                // list
                if (mdPairs.containsKey(parsedPair[0].trim()))
                {
                    mdPairs.get(parsedPair[0].trim()).add(parsedPair[1]);
                    if(log.isDebugEnabled()) {
                        log.debug("Registering Type Identifier:  " + parsedPair[0] + " => " + parsedPair[1]);
                    }
                }
                else
                {
                    // Otherwise, add it as the first occurrence of this field
                    ArrayList<String> newField = new ArrayList<String>();
                    newField.add(parsedPair[1].trim());
                    mdPairs.put(parsedPair[0].trim(), newField);

                    if (log.isDebugEnabled()) {
                        log.debug("Registering Type Identifier:  " + parsedPair[0] + " => " + parsedPair[1]);
                    }
                }
            }
            else
            {
                log.error("Malformed field identifier name/value pair");
            }
        }

        // Build config string without values, only field names
        StringBuilder sb = new StringBuilder();
        for (String value : mdPairs.keySet())
        {
            sb.append(value + " | ");
        }

        // Check resolved/present metadata fields against configured values
        ArrayList<Metadatum> presentMD = resolveMetadataFields(sb.toString());
        if (null != presentMD && presentMD.size() != 0)
        {
            for (Metadatum v : presentMD)
            {
                String fieldName = buildFieldName(v);
                if (mdPairs.containsKey(fieldName))
                {
                    for (String configValue : mdPairs.get(fieldName))
                    {
                        if (configValue.equals(v.value))
                        {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
