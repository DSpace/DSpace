/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.license;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for using CC-related Metadata fields
 *
 * @author kevinvandevelde at atmire.com
 */
public class LicenseMetadataValue {

    protected final ItemService itemService;
    // Shibboleth for Creative Commons license data - i.e. characters that reliably indicate CC in a URI
    protected static final String ccShib = "creativecommons";

    private String[] params = new String[4];

        public LicenseMetadataValue(String fieldName)
        {
            if (fieldName != null && fieldName.length() > 0)
            {
                String[] fParams = fieldName.split("\\.");
                for (int i = 0; i < fParams.length; i++)
                {
                    params[i] = fParams[i];
                }
                params[3] = Item.ANY;
            }
            itemService = ContentServiceFactory.getInstance().getItemService();
        }

    /**
     * Returns first value that matches Creative Commons 'shibboleth',
     * or null if no matching values.
     * NB: this method will succeed only for metadata fields holding CC URIs
     *
     * @param item - the item to read
     * @return value - the first CC-matched value, or null if no such value
     */
    public String ccItemValue(Item item)
    {
        List<MetadataValue> dcvalues = itemService.getMetadata(item, params[0], params[1], params[2], params[3]);
        for (MetadataValue dcvalue : dcvalues)
        {
            if ((dcvalue.getValue()).indexOf(ccShib) != -1)
            {
                // return first value that matches the shib
                return dcvalue.getValue();
            }
        }
        return null;
    }

    /**
     * Returns the value that matches the value mapped to the passed key if any.
     * NB: this only delivers a license name (if present in field) given a license URI
     *
     * @param item - the item to read
     * @param key - the key for desired value
     * @return value - the value associated with key or null if no such value
     * @throws IOException
     *     A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException
     *     Exception indicating the current user of the context does not have permission
     *     to perform a particular action.
     */
    public String keyedItemValue(Item item, String key)
        throws AuthorizeException, IOException, SQLException
    {
         CCLookup ccLookup = new CCLookup();
         ccLookup.issue(key);
         String matchValue = ccLookup.getLicenseName();
         List<MetadataValue> dcvalues = itemService.getMetadata(item, params[0], params[1], params[2], params[3]);
         for (MetadataValue dcvalue : dcvalues)
         {
             if (dcvalue.getValue().equals(matchValue))
             {
                 return dcvalue.getValue();
             }
         }
        return null;
    }

    /**
     * Removes the passed value from the set of values for the field in passed item.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param item - the item to update
     * @param value - the value to remove
     * @throws IOException
     *     A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException
     *     Exception indicating the current user of the context does not have permission
     *     to perform a particular action.
     */
    public void removeItemValue(Context context, Item item, String value)
            throws AuthorizeException, IOException, SQLException
    {
        if (value != null)
        {
            List<MetadataValue> dcvalues = itemService.getMetadata(item, params[0], params[1], params[2], params[3]);
            ArrayList<String> arrayList = new ArrayList<String>();
            for (MetadataValue dcvalue : dcvalues)
            {
                if (!dcvalue.getValue().equals(value))
                {
                    arrayList.add(dcvalue.getValue());
                }
            }
            itemService.clearMetadata(context, item, params[0], params[1], params[2], params[3]);
            itemService.addMetadata(context, item, params[0], params[1], params[2], params[3], arrayList);
        }
    }

    /**
     * Adds passed value to the set of values for the field in passed item.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param item - the item to update
     * @param value - the value to add in this field
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public void addItemValue(Context context, Item item, String value) throws SQLException {
        itemService.addMetadata(context, item, params[0], params[1], params[2], params[3], value);
    }

}
