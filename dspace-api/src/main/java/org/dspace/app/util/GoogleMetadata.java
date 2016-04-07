/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.sql.SQLException;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.jdom.Element;

/**
 * Configuration and mapping for Google Scholar output metadata
 * @author Sands Fish
 * 
 */

@SuppressWarnings("deprecation")
public class GoogleMetadata extends MappingMetadata
{

    private final static Logger log = Logger.getLogger(GoogleMetadata.class);

    private static final String GOOGLE_PREFIX = "google.";

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


    /**
     * Wrap the item, parse all configured fields and generate metadata field
     * values.
     * 
     * @param item
     *            - The item being viewed to extract metadata from
     */
    public GoogleMetadata(Context context, Item item) throws SQLException
    {
    	init("google-metadata.config");
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



	@Override
	protected String getPrefix() {
		return GOOGLE_PREFIX;
	}

}
