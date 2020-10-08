/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;

/**
 * Based on GoogleMetadata crosswalk
 * 
 * @author Mykhaylo Boychuk  (mykhaylo.boychuk at 4science.it)
 */
public class OrcidWorkMetadata extends MappingMetadata {

    private static final String ORCID_PREFIX = "orcid.";

    public static final String TITLE = "title";

    public static final String AUTHORS = "authors";

    public static final String JOURNAL_TITLE = "journal_title";

    public static final String ABSTRACT = "short_description";

    public static final String TYPE = "work_type";

    public static final String ISSUE = "publication_date";

    public static final String CITATION = "citation_crosswalk";

    public static final String ISSUE_YEAR = "publication_date.year";
    public static final String ISSUE_MONTH = "publication_date.month";
    public static final String ISSUE_DAY = "publication_date.day";

    public static final String URL = "url";

    public static final String EXTERNAL_IDENTIFIER = "external_identifier";

    /**
     * Wrap the item, parse all configured fields and generate metadata field
     * values.
     * 
     * @param item    The item being viewed to extract metadata from
     */
    public OrcidWorkMetadata(Item item) throws SQLException {
        init("orcid-work-metadata.config");
        this.item = item;
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

        // AUTHORS (multi)
        addMultiple(AUTHORS);

        //URL
        addSingleField(URL);

        // EXTERNAL IDs
        addMultiInvertedValues(EXTERNAL_IDENTIFIER);

        //TYPE
        addSingleField(TYPE);

        // CITATION
        addCitation(CITATION);
    }

    @Override
    protected String getPrefix() {
        return ORCID_PREFIX;
    }

    public String getTitle() {
        if (!metadataMappings.get(TITLE).isEmpty()) {
            return metadataMappings.get(TITLE).get(0);
        }
        return null;
    }

    public String getJournalTitle() {
        if (!metadataMappings.get(JOURNAL_TITLE).isEmpty()) {
            return metadataMappings.get(JOURNAL_TITLE).get(0);
        }
        return null;
    }

    public String getIssue() {
        if (!metadataMappings.get(ISSUE).isEmpty()) {
            return metadataMappings.get(ISSUE).get(0);
        }
        return null;
    }

    public String getAbstract() {
        if (!metadataMappings.get(ABSTRACT).isEmpty()) {
            return metadataMappings.get(ABSTRACT).get(0);
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

    public List<MetadataValue> getAuthors() {
        if (!metadataValueMappings.get(AUTHORS).isEmpty()) {
            return metadataValueMappings.get(AUTHORS);
        }
        return new ArrayList<MetadataValue>();
    }

    public String getWorkType() {
        if (!metadataMappings.get(TYPE).isEmpty()) {
            return metadataMappings.get(TYPE).get(0);
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
}
