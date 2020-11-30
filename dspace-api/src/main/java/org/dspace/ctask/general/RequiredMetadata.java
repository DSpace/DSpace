/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.content.Metadatum;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.curate.Suspendable;

/**
 * RequiredMetadata task compares item metadata with fields 
 * marked as required in input-forms.xml. The task succeeds if all
 * required fields are present in the item metadata, otherwise it fails.
 * Primarily a curation task demonstrator.
 *
 * @author richardrodgers
 */
@Suspendable
public class RequiredMetadata extends AbstractCurationTask
{
    // map of DCInputSets
    private DCInputsReader reader = null;
    // map of required fields
    private Map<String, List<String>> reqMap = new HashMap<String, List<String>>();

    private Map<String, List<String>> typeBindsList = new HashMap<String, List<String>>();
    
    @Override 
    public void init(Curator curator, String taskId) throws IOException
    {
        super.init(curator, taskId);
        try
        {
            reader = new DCInputsReader();
        }
        catch (DCInputsReaderException dcrE)
        {
            throw new IOException(dcrE.getMessage(), dcrE);
        }
    }

    /**
     * Perform the curation task upon passed DSO
     *
     * @param dso the DSpace object
     * @throws IOException
     */
    @Override
    public int perform(DSpaceObject dso) throws IOException
    {
        if (dso.getType() == Constants.ITEM)
        {
            Item item = (Item)dso;
            int count = 0;
            try
            {
                StringBuilder sb = new StringBuilder();
                String handle = item.getHandle();
                //Builds type binds map
                getTypeBinds(handle);
                if (handle == null)
                {
                    // we are still in workflow - no handle assigned
                    handle = "in workflow";
                }
                sb.append("Item: ").append(handle);
                for (String req : getReqList(item.getOwningCollection().getHandle()))
                {
                    boolean isReqTypeBound = false;
                    Metadatum[] vals = item.getMetadataByMetadataString(req);
                    //If required field is type bound
                    //ONLY require if item is of bounded type
                    if(typeBindsList.get(req) != null)
                    {
                        isReqTypeBound = true;
                    }
                    if(isReqTypeBound)
                    {
                        Metadatum[] itemType = item.getMetadataByMetadataString("dc.type");
                        if(itemType.length > 0)
                        {
                            boolean typeMatch = false;
                            //Iterate over all possible dc.types
                            for(int i = 0; i < itemType.length; i++)
                            {
                                String itemTypeBind = itemType[i].value;
                                List<String> typeBinds = typeBindsList.get(req);
                                for (String typeBind : typeBinds)
                                {
                                    if (itemTypeBind.equalsIgnoreCase(typeBind))
                                    {
                                        typeMatch = true;
                                    }
                                }
                            }
                            //If true then failure
                            if (typeMatch)
                            {
                                sb.append(" missing type bound required field: ").append(req);
                                count++;
                            }
                        }
                    }
                    else if (vals.length == 0)
                    {
                        sb.append(" missing required field: ").append(req);
                        count++;
                    }
                }
                if (count == 0)
                {
                    sb.append(" has all required fields");
                }
                report(sb.toString());
                setResult(sb.toString());
            }
            catch (DCInputsReaderException dcrE)
            {
                throw new IOException(dcrE.getMessage(), dcrE);
            }
            catch (SQLException sqlE)
            {
                throw new IOException(sqlE.getMessage(), sqlE);
            }
            return (count == 0) ? Curator.CURATE_SUCCESS : Curator.CURATE_FAIL;
        }
        else
        {
           setResult("Object skipped");
           return Curator.CURATE_SKIP;
        }
    }
    
    private List<String> getReqList(String handle) throws DCInputsReaderException
    {
        List<String> reqList = reqMap.get(handle);
        if (reqList == null)
        {
            reqList = reqMap.get("default");
        }
        if (reqList == null)
        {
            reqList = new ArrayList<String>();
            DCInputSet inputs = reader.getInputs(handle);
            for (int i = 0; i < inputs.getNumberPages(); i++)
            {
                for (DCInput input : inputs.getPageRows(i, true, true))
                {
                    if (input.isRequired())
                    {
                        StringBuilder sb = new StringBuilder();
                        sb.append(input.getSchema()).append(".");
                        sb.append(input.getElement()).append(".");
                        String qual = input.getQualifier();
                        if (qual == null)
                        {
                            qual = "";
                        }
                        sb.append(qual);
                        reqList.add(sb.toString());
                    }
                }
            }
            reqMap.put(inputs.getFormName(), reqList);
        }
        return reqList;
    }

    /** Populate Type Bind Map
    *
    * @param handle the DSpace object item handle
    * @throws DCInputsReaderException
    */
    private void getTypeBinds(String handle) throws DCInputsReaderException
    {
        int pageNum = reader.getInputs(handle).getNumberPages();
        for(int i =0; i < pageNum; i++)
        {
            DCInput[] inputs = reader.getInputs(handle).getPageRows(i, false, false);
            for(DCInput input : inputs)
            {
                if(input.getTypeBind().size() > 0 )
                {
                    List<String>  typeBinds = input.getTypeBind();
                    String field = input.getSchema() + "." + input.getElement();
                    if(input.getQualifier() != null)
                    {
                        field = field + "." + input.getQualifier();
                    }
                    typeBindsList.put(field, typeBinds);
                }
            }
        }
    }
}
