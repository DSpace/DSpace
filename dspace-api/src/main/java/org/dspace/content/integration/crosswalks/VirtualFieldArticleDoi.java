/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.sql.SQLException;
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
public class VirtualFieldArticleDoi implements VirtualFieldDisseminator,
        VirtualFieldIngester
{

    private static String DEFAULT_PREFIX = "article";
    
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
           
            Metadatum[] mdpartof = item.getMetadata("dc", "relation", "ispartofjournal", Item.ANY);
            if(mdpartof.length>0) {                         
                result += mdpartof[0].value.toLowerCase();
                result = result.replaceAll("[^a-z]+", "-");
            }
            else {
                result += DEFAULT_PREFIX;
            }            
            
            result += ".";
            
            Metadatum[] md = item.getMetadata("dc", "relation", "volume", Item.ANY);
            if(md.length>0){
                result += md[0].value + ".";    
            }                      
            
            Metadatum[] mddaterel = item.getMetadata("dc", "relation", "issue", Item.ANY);
            if(mddaterel.length>0){
                result += mddaterel[0].value + ".";    
            }                       

            Metadatum[] mdfirst = item.getMetadata("dc", "relation", "firstpage", Item.ANY);
            if(mdfirst.length>0){
                result += mdfirst[0].value + "-";    
            }                      
                       
            
            Metadatum[] mdlast = item.getMetadata("dc", "relation", "lastpage", Item.ANY);
            if(mdlast.length>0){
                result += mdlast[0].value + ".";    
            }                 

            Metadatum[] mddate = item.getMetadata("dc", "date", "issued", Item.ANY);
            
            if(mddate.length>0){
                result += mddate[0].value;    
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
            fieldCache.put("virtual.articledoi", result);
            
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
