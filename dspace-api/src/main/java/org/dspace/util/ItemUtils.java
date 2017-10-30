/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.workflow.WorkflowItem;

public class ItemUtils
{
 
    private static Logger log = Logger.getLogger(ItemUtils.class);
    
    public final static int UNKNOWN = -1;
    public final static int WORKSPACE = 0;
    public final static int WORKFLOW = 1;
    public final static int ARCHIVE = 2;
    public final static int WITHDRAWN = 3;
    
    
    public static int getItemStatus(Context context, Item item)
            throws SQLException
    {
        if (item.isArchived())
            return ARCHIVE;
        if (item.isWithdrawn())
            return WITHDRAWN;
        TableRow row = DatabaseManager.querySingle(context,
                "select * from workflowitem where item_id = ?", item.getID());
        if (row != null)
        {
            return WORKFLOW;
        }
        row = DatabaseManager.querySingle(context,
                "select * from workspaceitem where item_id = ?", item.getID());
        if (row != null)
        {
            return WORKSPACE;
        }
        return UNKNOWN;
    }
    

    public static DCInputSet getDCInputSet(Item item)
    {
        try
        {
            String formFileName = I18nUtil
                    .getInputFormsFileName(I18nUtil.getDefaultLocale());
            String col_handle = "";

            Collection collection = item.getOwningCollection();

            if (collection == null)
            {
                // set an empty handle so to get the default input set
                col_handle = "";
            }
            else
            {
                col_handle = collection.getHandle();
            }

            // Read the input form file for the specific collection
            DCInputsReader inputsReader = new DCInputsReader(
                    formFileName);

            return inputsReader.getInputs(col_handle);
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
        return null;
    }
    
    public static DCInput getDCInput(String schema, String element,
            String qualifier, DCInputSet dcinputset)
    {
        for (int idx = 0; idx < dcinputset.getNumberPages(); idx++)
        {
            for (DCInput dcinput : dcinputset.getPageRows(idx, true, true))
            {
                if (dcinput.getSchema().equals(schema)
                        && dcinput.getElement().equals(element)
                        && ((dcinput.getQualifier() != null && dcinput.getQualifier().equals(qualifier)) || (dcinput
                                .getQualifier() == null && qualifier == null) || "qualdrop_value".equals(dcinput.getInputType())))
                {
                    return dcinput;
                }
            }
        }
        return null;
    }
    
    public static void removeOrWithdrawn(Context context, Item item)
            throws SQLException, AuthorizeException, IOException
    {
        // Find item in workspace or workflow...
        InProgressSubmission inprogress = WorkspaceItem.findByItem(context,
                item);
        if (inprogress == null)
        {
            inprogress = WorkflowItem.findByItem(context, item);
        }
        // if we have an item that has been public at some time, better to keep
        // it for history
        if (item.getHandle() != null)
        {

            // Reopened
            if (inprogress != null)
            {
                item.setOwningCollection(inprogress.getCollection());
            }
            item.withdraw();
            item.update();

            // Delete wrapper
            if (inprogress != null)
            {
                inprogress.deleteWrapper();
            }

        }
        else
        {
            inprogress.deleteWrapper();
            item.delete();

        }
    }


    /**
     * Utility method for pattern-matching metadata elements.  This
     * method will return <code>true</code> if the given schema,
     * element, qualifier and language match the schema, element,
     * qualifier and language of the <code>Metadatum</code> object passed
     * in.  Any or all of the element, qualifier and language passed
     * in can be the <code>Item.ANY</code> wildcard.
     *
     * @param schema
     *            the schema for the metadata field. <em>Must</em> match
     *            the <code>name</code> of an existing metadata schema.
     * @param element
     *            the element to match, or <code>Item.ANY</code>
     * @param qualifier
     *            the qualifier to match, or <code>Item.ANY</code>
     * @param language
     *            the language to match, or <code>Item.ANY</code>
     * @param dcv
     *            the Dublin Core value
     * @return <code>true</code> if there is a match
     */
    public static boolean match(String schema, String element, String qualifier,
                          String language, Metadatum dcv)
    {
    
    	if (StringUtils.isBlank(element) || StringUtils.isBlank(schema)) {
    		return false;
    	}
    
    	// We will attempt to disprove a match - if we can't we have a match
    	if (!element.equals(Item.ANY) && !element.equals(dcv.element)) {
    		// Elements do not match, no wildcard
    		return false;
    	}
    	
        if (qualifier == null)
        {
            // Value must be unqualified
            if (dcv.qualifier != null)
            {
                // Value is qualified, so no match
                return false;
            }
        }
        else if (!qualifier.equals(Item.ANY))
        {
            // Not a wildcard, so qualifier must match exactly
            if (!qualifier.equals(dcv.qualifier))
            {
                return false;
            }
        }
    
        if (language == null)
        {
            // Value must be null language to match
            if (dcv.language != null)
            {
                // Value is qualified, so no match
                return false;
            }
        }
        else if (!language.equals(Item.ANY))
        {
            // Not a wildcard, so language must match exactly
            if (!language.equals(dcv.language))
            {
                return false;
            }
        }
    
        if (!schema.equals(Item.ANY))
        {
            if (dcv.schema != null && !dcv.schema.equals(schema))
            {
                // The namespace doesn't match
                return false;
            }
        }
    
        // If we get this far, we have a match
        return true;
    }
}
