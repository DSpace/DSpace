package org.dspace.util;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

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
}
