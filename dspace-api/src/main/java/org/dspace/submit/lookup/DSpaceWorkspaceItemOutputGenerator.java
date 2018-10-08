/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.AdditionalMetadataUpdateProcessPlugin;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.Metadatum;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.submit.util.ItemSubmissionLookupDTO;
import org.dspace.submit.util.SubmissionLookupPublication;
import org.dspace.util.ItemUtils;
import org.dspace.utils.DSpace;

import gr.ekt.bte.core.DataOutputSpec;
import gr.ekt.bte.core.OutputGenerator;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.RecordSet;
import gr.ekt.bte.core.Value;

/**
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 */
public class DSpaceWorkspaceItemOutputGenerator implements OutputGenerator
{

    private static Logger log = Logger
            .getLogger(DSpaceWorkspaceItemOutputGenerator.class);

    private Context context;

    private DSpace dspace = new DSpace();
    
    private String formName;

    private List<WorkspaceItem> witems;

    private ItemSubmissionLookupDTO dto;

    private Collection collection;

    Map<String, String> outputMap;

    private List<String> extraMetadataToKeep;

    @Override
    public List<String> generateOutput(RecordSet recordSet)
    {

        log.info("BTE OutputGenerator started. Records to output: "
                + recordSet.getRecords().size());

        // Printing debug message
        String totalString = "";
        for (Record record : recordSet.getRecords())
        {
            totalString += SubmissionLookupUtils.getPrintableString(record)
                    + "\n";
        }
        log.debug("Records to output:\n" + totalString);

        witems = new ArrayList<WorkspaceItem>();

        for (Record rec : recordSet.getRecords())
        {
            try
            {
                boolean templateItem = ConfigurationManager.getBooleanProperty(null, "bte.applytemplateitem",false);
                if(rec instanceof SubmissionLookupPublication) {
                    SubmissionLookupPublication dto = (SubmissionLookupPublication)rec;
                    if(SubmissionLookupService.MANUAL_USER_INPUT.equals(dto.getProviderName())) {
                        templateItem = true;
                    }                    
                }
                WorkspaceItem wi = WorkspaceItem.create(context, collection,
                        templateItem);
                merge(formName, wi.getItem(), rec);

                witems.add(wi);

            }
            catch (AuthorizeException e)
            {
                log.error(e.getMessage(), e);
            }
            catch (SQLException e)
            {
                log.error(e.getMessage(), e);
            }
            catch (IOException e)
            {
                log.error(e.getMessage(), e);
            }

        }

        return new ArrayList<String>();
    }

    @Override
    public List<String> generateOutput(RecordSet records, DataOutputSpec spec)
    {
        return generateOutput(records);
    }

    public List<WorkspaceItem> getWitems()
    {
        return witems;
    }

    public void setContext(Context context)
    {
        this.context = context;
    }

    public void setFormName(String formName)
    {
        this.formName = formName;
    }

    public void setDto(ItemSubmissionLookupDTO dto)
    {
        this.dto = dto;
    }

    public void setOutputMap(Map<String, String> outputMap)
    {
        // Reverse the key-value pairs
        this.outputMap = new HashMap<String, String>();
        for (String key : outputMap.keySet())
        {
            this.outputMap.put(outputMap.get(key), key);
        }
    }

    public void setCollection(Collection collection)
    {
        this.collection = collection;
    }

    public void setExtraMetadataToKeep(List<String> extraMetadataToKeep)
    {
        this.extraMetadataToKeep = extraMetadataToKeep;
    }

    // Methods
    public void merge(String formName, Item item, Record record) throws SQLException, AuthorizeException
    {

        Record itemLookup = record;
        

        Set<String> addedMetadata = new HashSet<String>();
        for (String field : itemLookup.getFields())
        {
            String metadata = getMetadata(formName, itemLookup, field);
            if (StringUtils.isBlank(metadata))
            {
                continue;
            }
            if (item.getMetadataByMetadataString(metadata).length == 0
                    || addedMetadata.contains(metadata))
            {
                addedMetadata.add(metadata);
                String[] md = splitMetadata(metadata);
                if (isValidMetadata(collection.getHandle(), md) )
                { // if in extra metadata or in the spefic form
                    List<Value> values = itemLookup.getValues(field);
                    if (values != null && values.size() > 0)
                    {
                        if (isRepeatableMetadata(collection.getHandle(), md))
                        { // if metadata is repeatable in form
                            for (Value value : values)
                            {
                                String[] splitValue = splitValue(value
                                        .getAsString());
                                if (splitValue[3] != null)
                                {
                                    item.addMetadata(md[0], md[1], md[2],
                                            md[3], splitValue[0],
                                            splitValue[1],
                                            Integer.parseInt(splitValue[2]));
                                }
                                else
                                {
                                    item.addMetadata(md[0], md[1], md[2],
                                            md[3], value.getAsString());
                                }
                            }
                        }
                        else
                        {
                            String value = values.iterator().next()
                                    .getAsString();
                            String[] splitValue = splitValue(value);
                            if (splitValue[3] != null)
                            {
                                item.addMetadata(md[0], md[1], md[2], md[3],
                                        splitValue[0], splitValue[1],
                                        Integer.parseInt(splitValue[2]));
                            }
                            else
                            {
                                item.addMetadata(md[0], md[1], md[2], md[3],
                                        value);
                            }
                        }
                    }
                }
            }
        }

        
            String providerName = "";
            List<Value> providerNames = itemLookup.getValues("provider_name_field");
            if(providerNames!=null && providerNames.size()>0) {
                providerName = providerNames.get(0).getAsString();
            }
            List<AdditionalMetadataUpdateProcessPlugin> additionalMetadataUpdateProcessPlugins = (List<AdditionalMetadataUpdateProcessPlugin>)dspace
                    .getServiceManager().getServicesByType(AdditionalMetadataUpdateProcessPlugin.class);
            for(AdditionalMetadataUpdateProcessPlugin additionalMetadataUpdateProcessPlugin : additionalMetadataUpdateProcessPlugins) {
                additionalMetadataUpdateProcessPlugin.process(this.context, item, providerName);
            }
            
            item.update();
    }

    private String getMetadata(String formName, Record itemLookup, String name)
    {
        String type = SubmissionLookupService.getType(itemLookup);

        String md = outputMap.get(type + "." + name);
        if (StringUtils.isBlank(md))
        {
            md = outputMap.get(formName + "." + name);
            if (StringUtils.isBlank(md))
            {
                md = outputMap.get(name);
            }
        }

        // KSTA:ToDo: Make this a modifier
        if (md != null && md.contains("|"))
        {
            String[] cond = md.trim().split("\\|");
            for (int idx = 1; idx < cond.length; idx++)
            {
                boolean temp = itemLookup.getFields().contains(cond[idx]);
                if (temp)
                {
                    return null;
                }
            }
            return cond[0];
        }
        return md;
    }

    private String[] splitMetadata(String metadata)
    {
        String[] mdSplit = new String[3];
        if (StringUtils.isNotBlank(metadata))
        {
            String tmpSplit[] = metadata.split("\\.");
            if (tmpSplit.length == 4)
            {
                mdSplit = new String[4];
                mdSplit[0] = tmpSplit[0];
                mdSplit[1] = tmpSplit[1];
                mdSplit[2] = tmpSplit[2];
                mdSplit[3] = tmpSplit[3];
            }
            else if (tmpSplit.length == 3)
            {
                mdSplit = new String[4];
                mdSplit[0] = tmpSplit[0];
                mdSplit[1] = tmpSplit[1];
                mdSplit[2] = tmpSplit[2];
                mdSplit[3] = null;
            }
            else if (tmpSplit.length == 2)
            {
                mdSplit = new String[4];
                mdSplit[0] = tmpSplit[0];
                mdSplit[1] = tmpSplit[1];
                mdSplit[2] = null;
                mdSplit[3] = null;
            }
        }
        return mdSplit;
    }

    private boolean isValidMetadata(String collHandle, String[] md)
    {
        try
        {
        	MetadataSchema schema = MetadataSchema.find(context,md[0]);
        	if (schema != null)
        	{
	        	int schemaID = schema.getSchemaID();
	        	MetadataField foundField = MetadataField.findByElement(context, schemaID, md[1], md[2]);
	            if(foundField != null) {
	            
		            if (extraMetadataToKeep != null
		                    && extraMetadataToKeep.contains(StringUtils.join(
		                            Arrays.copyOfRange(md, 0, 3), ".")))
		            {
		                return true;
		            }
		            return getDCInput(collHandle, md[0], md[1], md[2]) != null;
	            }
        	}
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    private DCInput getDCInput(String collHandle, String schema, String element,
            String qualifier) throws DCInputsReaderException
    {
        DCInputSet dcinputset = new DCInputsReader().getInputs(collHandle);
        return ItemUtils.getDCInput(schema, element, qualifier, dcinputset);
    }

    private boolean isRepeatableMetadata(String collHandle, String[] md)
    {
        try
        {
            DCInput dcinput = getDCInput(collHandle, md[0], md[1], md[2]);
            if (dcinput != null)
            {
                return dcinput.isRepeatable();
            }
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    private String[] splitValue(String value)
    {
        String[] splitted = value
                .split(SubmissionLookupService.SEPARATOR_VALUE_REGEX);
        String[] result = new String[6];
        result[0] = splitted[0];
        result[2] = "-1";
        result[3] = "-1";
        result[4] = "-1";
        if (splitted.length > 1)
        {
            result[5] = "splitted";
            if (StringUtils.isNotBlank(splitted[1]))
            {
                result[1] = splitted[1];
            }
            if (splitted.length > 2)
            {
                result[2] = String.valueOf(Integer.parseInt(splitted[2]));
                if (splitted.length > 3)
                {
                    result[3] = String.valueOf(Integer.parseInt(splitted[3]));
                    if (splitted.length > 4)
                    {
                        result[4] = String.valueOf(Integer
                                .parseInt(splitted[4]));
                    }
                }
            }
        }
        return result;
    }

    private void makeSureMetadataExist(Context context, String schema,
            String element, String qualifier)
    {
        try
        {
            context.turnOffAuthorisationSystem();
            boolean create = false;
            MetadataSchema mdschema = MetadataSchema.find(context, schema);
            MetadataField mdfield = null;
            if (mdschema == null)
            {
                mdschema = new MetadataSchema(
                        SubmissionLookupService.SL_NAMESPACE_PREFIX + schema,
                        schema);
                mdschema.create(context);
                create = true;
            }
            else
            {
                mdfield = MetadataField.findByElement(context,
                        mdschema.getSchemaID(), element, qualifier);
            }

            if (mdfield == null)
            {
                mdfield = new MetadataField(mdschema, element, qualifier,
                        "Campo utilizzato per la cache del provider submission-lookup: "
                                + schema);
                mdfield.create(context);
                create = true;
            }
            if (create)
            {
                context.commit();
            }
            context.restoreAuthSystemState();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
