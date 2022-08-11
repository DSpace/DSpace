/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deduplication.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import com.ibm.icu.text.Normalizer;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.workflow.factory.WorkflowServiceFactory;

/**
 * This signature will return search signatures that are transformed for use in solr filter queries with ~ max
 * edit distance operators, and normalised for fuzzy search.
 * "This Test Title" will be returned as thistesttitle~n
 * (where 'n' is the max edit distance configured in spring)
 *
 * @author Kim Shepherd
 */
public class FuzzyMetadataValueSignature implements Signature {

    /** log4j logger */
    protected static Logger log = LogManager.getLogger(FuzzyMetadataValueSignature.class);

    private String metadata;

    private int resourceTypeID;

    private String signatureType;

    private int maxDistance;

    protected List<String> ignorePrefix = new ArrayList<String>();

    protected String prefix;

    private String normalizationRegexp;

    private boolean useCollection;

    private boolean useEntityType = true;

    private ItemService itemService;

    // Is this signature case sensitive?
    private boolean caseSensitive;

    protected WorkflowItemService<?> workflowItemService = WorkflowServiceFactory.getInstance()
                                                                                 .getWorkflowItemService();

    /**
     * This list of signatures is specifically intended for search filter values, NOT for inclusion
     * in the _signature field when indexing the document
     * @param context
     * @param item
     * @return
     */
    public List<String> getSearchSignature(Context context, DSpaceObject item) {
        List<String> result = new ArrayList<>();
        for (String signature: getSignature(context, item)) {
            if (StringUtils.isNotEmpty(signature)) {
                String searchFilterValue = signature + "~" + maxDistance;
                if (!result.contains(searchFilterValue)) {
                    result.add(searchFilterValue);
                }
            }
        }
        return result;
    }

    /**
     * List of signatures intended for display, report, building and indexing Solr documents, etc.
     * @param context
     * @param item
     * @return
     */
    public List<String> getSignature(Context context, DSpaceObject item) {
        List<String> result = new ArrayList<>();
        List<String> values = getMultiValue(item, metadata);
        if (values != null) {
            for (String value : values) {
                if (StringUtils.isNotEmpty(value)) {
                    // Normalisation - this should match the index plugin normalisation and handling
                    // but only for the puporses of *indexed* or other uses of this signature.
                    // For search query value handling, see getSearchSignature(...)
                    String normValue = normalise(value);
                    result.add(normValue);
                }
            }
        }
        return result;

    }

    /**
     * Get metadata for a given item and field name
     *
     * @param item      DSpace item
     * @param metadata  Metadata field name
     * @return          List of metadata string values
     */
    protected List<String> getMultiValue(DSpaceObject item, String metadata) {
        List<MetadataValue> values = ContentServiceFactory.getInstance().getDSpaceObjectService(item)
                .getMetadataByMetadataString(item, metadata);
        ArrayList<String> retValue = new ArrayList<String>();

        for (MetadataValue v : values) {
            retValue.add(v.getValue());
        }
        return retValue;
    }

    // Normalise the string using the provided regular expression
    private String normalise(String value) {
        String norm = value;
        // Normalise using regular expression
        if (StringUtils.isNotBlank(this.normalizationRegexp)) {
            norm = Normalizer.normalize(value, Normalizer.NFD);
            norm = norm.replaceAll(this.normalizationRegexp, "");
        }

        // Case insensitive?
        if (!this.caseSensitive) {
            CharsetDetector cd = new CharsetDetector();
            cd.setText(norm.getBytes());
            CharsetMatch detect = cd.detect();
            Locale locale = Locale.ROOT;
            if (detect != null && detect.getLanguage() != null) {
                locale = new Locale(detect.getLanguage());
            }
            norm = norm.toLowerCase(locale);
        }
        return norm;
    }

    //
    // Simple Spring getters and setters. No detailed javadoc required.
    //

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public int getResourceTypeID() {
        return resourceTypeID;
    }

    public void setResourceTypeID(int resourceTypeID) {
        this.resourceTypeID = resourceTypeID;
    }

    public String getSignatureType() {
        return signatureType;
    }

    public void setSignatureType(String signatureType) {
        this.signatureType = signatureType;
    }

    synchronized public ItemService getItemService() {
        if (itemService == null) {
            itemService = ContentServiceFactory.getInstance().getItemService();
        }
        return itemService;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setNormalizationRegexp(String normalizationRegexp) {
        this.normalizationRegexp = normalizationRegexp;
    }

    public void setMaxDistance(int maxDistance) {
        this.maxDistance = maxDistance;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

}