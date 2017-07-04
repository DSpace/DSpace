/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.batch.bte;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.submit.lookup.SubmissionLookupService;
import org.dspace.submit.lookup.SubmissionLookupUtils;

import gr.ekt.bte.core.DataOutputSpec;
import gr.ekt.bte.core.OutputGenerator;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.RecordSet;
import gr.ekt.bte.core.Value;

public class ImpRecordOutputGenerator implements OutputGenerator {
	Logger log = Logger.getLogger(ImpRecordOutputGenerator.class);

    private List<ImpRecordItem> recordIdItems;    
    
    private Collection collection;
    
    private String formName;
    
    private Map<String, String> outputMap;

	private String sourceIdMetadata;
	
    private String providerName;
	
	@Override
	public List<String> generateOutput(RecordSet recordSet) {
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

        recordIdItems = new ArrayList<ImpRecordItem>();

        for (Record rec : recordSet.getRecords())
        {
            	ImpRecordItem pfi = new ImpRecordItem();
                pfi = merge( pfi, rec);
                recordIdItems.add(pfi);
        }

        return new ArrayList<String>();
	}
	
	@Override
	public List<String> generateOutput(RecordSet recs, DataOutputSpec spec) {
		return generateOutput(recs);
	}

	public ImpRecordItem  merge(ImpRecordItem item, Record record)
    {
        Record itemLookup = record;

        Set<String> addedMetadata = new HashSet<String>();
        for (String field : itemLookup.getFields())
        {
			String metadata = getMetadata(formName,itemLookup, field);
            if (StringUtils.isBlank(metadata))
            {
                continue;
            }
            addedMetadata.add(metadata);

            List<Value> values = itemLookup.getValues(field);
            if(StringUtils.equals(metadata, sourceIdMetadata)){
            	if(values.isEmpty() || values.get(0) == null || !StringUtils.isNotBlank(values.get(0).getAsString())){
            		break;
            	}
            	String pmid = values.get(0).getAsString();
            	
            	String providerName = this.providerName;
            	if(StringUtils.isNotBlank(providerName)) {
            	    if(itemLookup.hasField("provider_name_field")) {
            	        providerName = itemLookup.getValues("provider_name_field").get(0).getAsString();    
            	    }            	        
            	}
            	
            	item.setSourceId(pmid);            	
                item.setSourceRef(providerName);
            }
            
            Set<ImpRecordMetadata> val = new HashSet<ImpRecordMetadata>();
            if (values != null && values.size() > 0)
            {
                for (Value value : values)
                {
                    String asString = value.getAsString();
                	if(!StringUtils.isNotBlank(asString)){
                		continue;
                	}
					ImpRecordMetadata strVal= splitValue(asString);
                	if(!StringUtils.isNotBlank(strVal.getValue())){
                		continue;
                	}
                	val.add(strVal);
                }
                if(!val.isEmpty()) {
                    item.addMetadata(metadata,val);
                }
            }
        }
        return item;
    }

    private ImpRecordMetadata splitValue(String value)
    {
        String[] splitted = value
                .split(SubmissionLookupService.SEPARATOR_VALUE_REGEX);
        ImpRecordMetadata result = new ImpRecordMetadata();
        result.setValue(splitted[0]);
        if (splitted.length > 1)
        {
            if (StringUtils.isNotBlank(splitted[1]))
            {
                result.setAuthority(splitted[1]);
            }
            if (splitted.length > 2)
            {
                result.setConfidence(Integer.parseInt(splitted[2]));
                if (splitted.length > 3)
                {
                    result.setMetadataOrder(Integer.parseInt(splitted[3]));
                    if (splitted.length > 4)
                    {
                        result.setShare(Integer
                                .parseInt(splitted[4]));
                    }
                }
            }
        }
        return result;
    }
    
    private String getMetadata(String formName,Record itemLookup, String name)
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
    
	public Collection getCollection() {
		return collection;
	}

	public void setCollection(Collection collection) {
		this.collection = collection;
	}

	public Map<String, String> getOutputMap() {
		return outputMap;
	}

	public void setOutputMap(Map<String, String> outputMap) {
        // Reverse the key-value pairs
        this.outputMap = new HashMap<String, String>();
        for (String key : outputMap.keySet())
        {
            this.outputMap.put(outputMap.get(key), key);
        }
	}

	public String getFormName() {
		return formName;
	}

	public void setFormName(String formName) {
		this.formName = formName;
	}

    public String getSourceIdMetadata()
    {
        return sourceIdMetadata;
    }

    public void setSourceIdMetadata(String sourceIdMetadata)
    {
        this.sourceIdMetadata = sourceIdMetadata;
    }


    public List<ImpRecordItem> getRecordIdItems()
    {
        return recordIdItems;
    }

    public void setRecordIdItems(List<ImpRecordItem> recordIdItems)
    {
        this.recordIdItems = recordIdItems;
    }


    public String getProviderName()
    {
        return providerName;
    }


    public void setProviderName(String providerName)
    {
        this.providerName = providerName;
    }

}
