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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
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

    private boolean caseSensitive;

    private boolean useCollection;

    private boolean useEntityType = true;

    private ItemService itemService;

    protected WorkflowItemService<?> workflowItemService = WorkflowServiceFactory.getInstance()
                                                                                 .getWorkflowItemService();

    protected WorkspaceItemService  wsItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();

    /**
     * This list of signatures is specifically intended for search filter values, NOT for inclusion
     * in the _signature field when indexing the document
     * @param item
     * @param context
     * @return
     */
    public List<String> getSearchSignature(DSpaceObject item, Context context) {
        List<String> result = new ArrayList<>();
        for (String signature: getSignature(item, context)) {
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
     * @param item
     * @param context
     * @return
     */
    public List<String> getSignature(DSpaceObject item, Context context) {
        List<String> result = new ArrayList<>();
        List<String> values = getMultiValue(item, metadata);
        if (values != null) {
            for (String value : values) {
                if (StringUtils.isNotEmpty(value)) {
                    // Normalisation - this should match the index plugin normalisation and handling
                    // but only for the puporses of *indexed* or other uses of this signature.
                    // For search query value handling, see getSearchSignature(...)
                    String normValue = value.toLowerCase(Locale.ROOT)
                            .replaceAll("\\s", "");
                    result.add(normValue);
                }
            }
        }
        return result;

    }

    protected String getSingleValue(DSpaceObject item, String metadata) {
        return ContentServiceFactory.getInstance().getDSpaceObjectService(item).getMetadata(item, metadata);
    }

    protected List<String> getMultiValue(DSpaceObject item, String metadata) {
        List<MetadataValue> values = ContentServiceFactory.getInstance().getDSpaceObjectService(item)
                .getMetadataByMetadataString(item, metadata);
        ArrayList<String> retValue = new ArrayList<String>();

        for (MetadataValue v : values) {
            retValue.add(v.getValue());
        }
        return retValue;
    }

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

    public List<String> getIgnorePrefix() {
        return ignorePrefix;
    }

    public void setIgnorePrefix(List<String> ignorePrefix) {
        this.ignorePrefix = ignorePrefix;
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

    public String getNormalizationRegexp() {
        return normalizationRegexp;
    }

    public void setNormalizationRegexp(String normalizationRegexp) {
        this.normalizationRegexp = normalizationRegexp;
    }

    public boolean isUseCollection() {
        return useCollection;
    }

    public void setUseCollection(boolean useCollection) {
        this.useCollection = useCollection;
    }

    public boolean isUseEntityType() {
        return useEntityType;
    }

    public void setUseEntityType(boolean useEntityType) {
        this.useEntityType = useEntityType;
    }

    public int getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(int maxDistance) {
        this.maxDistance = maxDistance;
    }
}