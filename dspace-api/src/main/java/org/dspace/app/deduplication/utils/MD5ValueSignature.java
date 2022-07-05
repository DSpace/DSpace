/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deduplication.utils;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import com.ibm.icu.text.Normalizer;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.workflow.factory.WorkflowServiceFactory;

public class MD5ValueSignature implements Signature {
    public static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
        'f' };

    /** log4j logger */
    protected static Logger log = LogManager.getLogger(MD5ValueSignature.class);

    private String metadata;

    private int resourceTypeID;

    private String signatureType;

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

    public List<String> getSearchSignature(DSpaceObject item, Context context) {
        List<String> result = new ArrayList<>();
        for (String signature: getSignature(item, context)) {
            if (StringUtils.isNotEmpty(signature)) {
                // Boost MD5 signature matches above others
                String searchFilterValue = signature + "^5";
                if (!result.contains(searchFilterValue)) {
                    result.add(searchFilterValue);
                }
            }
        }
        return result;
    }

    public List<String> getSignature(DSpaceObject item, Context context) {
        List<String> result = new ArrayList<String>();
        try {
            MessageDigest digester = MessageDigest.getInstance("MD5");
            List<String> values = getMultiValue(item, metadata);
            if (values != null) {
                for (String value : values) {
                    if (StringUtils.isNotEmpty(value)) {
                        String valueNorm = normalize(item, context, value);
                        digester.update(valueNorm.getBytes("UTF-8"));
                        byte[] signature = digester.digest();
                        char[] arr = new char[signature.length << 1];
                        for (int i = 0; i < signature.length; i++) {
                            int b = signature[i];
                            int idx = i << 1;
                            arr[idx] = HEX_DIGITS[(b >> 4) & 0xf];
                            arr[idx + 1] = HEX_DIGITS[b & 0xf];
                        }
                        String sigString = new String(arr);
                        result.add(sigString);
                    }
                }
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected String normalize(DSpaceObject item, Context context, String value) {
        if (value != null) {
            String temp = StringUtils.EMPTY;
            String entityType = StringUtils.EMPTY;
            String result = value;
            if (StringUtils.isEmpty(value)) {
                if (StringUtils.isNotEmpty(prefix)) {
                    result = prefix + item.getID();
                } else {
                    result = "entity:" + item.getID();
                }
            } else {
                for (String prefix : ignorePrefix) {
                    if (value.startsWith(prefix)) {
                        result = value.substring(prefix.length());
                        break;
                    }
                }
                if (StringUtils.isNotEmpty(prefix)) {
                    result = prefix + result;
                }
            }

            if (Objects.nonNull(item) && this.useCollection) {
                DSpaceObject parent = getParent(context, item);
                if (Objects.nonNull(parent)) {
                    temp = parent.getID().toString();
                }
            }
            if (Objects.nonNull(item) && this.useEntityType) {
                entityType = getItemService().getMetadataFirstValue((Item) item, "dspace", "entity",
                        "type", null);
            }
            String norm = result;
            if (StringUtils.isNotBlank(this.normalizationRegexp)) {
                norm = Normalizer.normalize(result, Normalizer.NFD);
                CharsetDetector cd = new CharsetDetector();
                cd.setText(result.getBytes());
                CharsetMatch detect = cd.detect();
                if (detect != null && detect.getLanguage() != null) {
                    norm = norm.replaceAll(this.normalizationRegexp, "");
                    if (!this.caseSensitive) {
                        norm = norm.toLowerCase(new Locale(detect.getLanguage()));
                    }
                } else {
                    norm = norm.replaceAll(this.normalizationRegexp, "");
                    if (!this.caseSensitive) {
                        norm = norm.toLowerCase();
                    }
                }
            }
            return (temp + entityType + norm).trim();
        } else {
            return "item:" + item.getID();
        }
    }

    protected String normalize(DSpaceObject item, String value) {
        String result = value;
        if (StringUtils.isEmpty(value)) {
            if (StringUtils.isNotEmpty(prefix)) {
                result = prefix + item.getID();
            } else {
                result = "entity:" + item.getID();
            }
        } else {
            for (String prefix : ignorePrefix) {
                if (value.startsWith(prefix)) {
                    result = value.substring(prefix.length());
                    break;
                }
            }
            if (StringUtils.isNotEmpty(prefix)) {
                result = prefix + result;
            }
        }

        return result;
    }

    private DSpaceObject getParent(Context context, DSpaceObject obj) {
        Item item = (Item) obj;
        try {
            if (item.isArchived()) {
                return getItemService().getParentObject(context, item);
            }
            WorkflowItem workflowItem = workflowItemService.findByItem(context, item);
            if (Objects.nonNull(workflowItem)) {
                return workflowItem.getCollection();
            }
            WorkspaceItem wsItem = wsItemService.findByItem(context, item);
            if (Objects.nonNull(wsItem)) {
                return wsItem.getCollection();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
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

}