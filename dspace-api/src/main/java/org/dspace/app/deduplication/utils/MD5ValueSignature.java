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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;

public class MD5ValueSignature implements Signature {
    public static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
        'f' };

    /** log4j logger */
    protected static Logger log = Logger.getLogger(MD5ValueSignature.class);

    private String metadata;

    private int resourceTypeID;

    private String signatureType;

    protected List<String> ignorePrefix = new ArrayList<String>();

    protected String prefix;

    private ItemService itemService;

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
        return normalize(item, value);
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

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
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
}
