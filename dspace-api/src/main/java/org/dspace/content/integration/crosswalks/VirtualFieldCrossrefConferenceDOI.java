/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;

import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.integration.batch.ScriptCrossrefSender;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

/**
 * Implements virtual field processing to build suffix doi
 * 
 * @author pascarelli
 */
public class VirtualFieldCrossrefConferenceDOI implements VirtualFieldDisseminator,
        VirtualFieldIngester
{

    public String[] getMetadata(Item item, Map<String, String> fieldCache,
            String fieldName)
    {

        Context context = null;
        try
        {
            context = new Context();

           
           
            // Check to see if the virtual field is already in the cache
            // - processing is quite intensive, so we generate all the values on
            // first request
            if (fieldCache.containsKey(fieldName))
            {
                return new String[] { fieldCache.get(fieldName) };
            }
            
            String result = "";
            
            Metadatum md = item.getMetadata("dc", "contributor", "author", Item.ANY)[0];
            String mdValue = md.value.toLowerCase();
            mdValue = mdValue.replaceAll("[^a-z]+", "-");
            result += mdValue;
             
            
            Metadatum mddate = item.getMetadata("dc", "date", "issued", Item.ANY)[0];
            result += mddate.value;
           
            Metadatum mdtitle = item.getMetadata("dc", "title", null, Item.ANY)[0];
            String mdtitleValue = mdtitle.value.toLowerCase();
            mdtitleValue = mdtitleValue.replaceAll("[^a-z]+", "-");
            
            String[] mdtitleArrays = mdtitleValue.split("-");
            
            if(mdtitleArrays.length>1) {
                Arrays.sort(mdtitleArrays, 0, mdtitleArrays.length, new ComparatorLengthString());            
                result += "_" + mdtitleArrays[0] + "_" + mdtitleArrays[1];
            }
            else {
                result += "_" + mdtitleArrays[0];
            }
      
            
            TableRow row = DatabaseManager.querySingle(context,
                    "select count(*) as cc from "
                            + ScriptCrossrefSender.TABLE_NAME_DOI2ITEM
                            + " where identifier_doi = ?", result);
            if(row!=null) {
                if(row.getLongColumn("cc")>0) {
                    result += "_" + item.getID();
                }
            }
            fieldCache.put("virtual.conferencedoi", result);
            
            // Return the value of the virtual field (if any)
            if (fieldCache.containsKey(fieldName))
            {
                return new String[] { fieldCache.get(fieldName) };
            }

        }
        catch (SQLException e)
        {
            // nothing
        }
        finally
        {
            if (context!=null && context.isValid())
            {
                context.abort();
            }
        }
        return null;
    }

    public boolean addMetadata(Item item, Map<String, String> fieldCache,
            String fieldName, String value)
    {
        // NOOP - we won't add any metadata yet, we'll pick it up when we
        // finalise the item
        return true;
    }

    public boolean finalizeItem(Item item, Map<String, String> fieldCache)
    {
        return false;
    }
}
