/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.submit.util.ItemSubmissionLookupDTO;

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
public class SubmissionLookupOutputGenerator implements OutputGenerator
{
	
    private static Logger log = Logger
            .getLogger(SubmissionLookupOutputGenerator.class);
    
    private List<ItemSubmissionLookupDTO> dtoList;

    private static final String NOT_FOUND_IDENTIFIER = "NOT-FOUND-IDENTIFIER";

    private Map<String, String> identifiersMap = new HashMap<String, String>();

    public void setIdentifiersMap(Map<String, String> identifiersMap) {
		this.identifiersMap = identifiersMap;
	}

	public SubmissionLookupOutputGenerator()
    {
    	
    }

    @Override
    public List<String> generateOutput(RecordSet records)
    {
        dtoList = new ArrayList<ItemSubmissionLookupDTO>();

        Map<String, List<Record>> record_sets = new HashMap<String, List<Record>>();

        List<Record> originalRecords = new ArrayList<Record>();
        List<Record> childrenRecords = new ArrayList<Record>();
        Set<Record> removedChildrenRecords = new HashSet<Record>();
        
        for (Record rec : records)
        {
            if(log.isDebugEnabled()) {
    		    log.debug("#########Records to output##########"); 
    		    log.debug(SubmissionLookupUtils.getPrintableString(rec));
            }
            
            if (rec.getValues("originalRecord") != null && !rec.getValues("originalRecord").isEmpty())
            {
                originalRecords.add(rec);
            }
            else
            {
                childrenRecords.add(rec);
            }
        }

        if(log.isDebugEnabled()) {
		    log.debug("n. originalRecord: " + originalRecords.size()); 
		    log.debug("n. childrenRecords: " + childrenRecords.size());
        }
        
        int counter = 0;
        
        //try to aggregate the original records found with the first provider by the others one (related provider) 
        for (Record originalRecord : originalRecords)
        {
            Map<String, String> identifiersMap = createIdentifiersMap(originalRecord);
            
            boolean found = false;
            String firstOriginalIdentifier = NOT_FOUND_IDENTIFIER + "_" + counter;
            for (Map.Entry<String, String> identifier : identifiersMap.entrySet())
            {
                firstOriginalIdentifier = identifier.getValue();
                if (record_sets.keySet().contains(firstOriginalIdentifier))
                {
                	record_sets.get(firstOriginalIdentifier).add(originalRecord);
                }
                else
                {
					ArrayList<Record> publication = new ArrayList<Record>();
					publication.add(originalRecord);
					record_sets.put(firstOriginalIdentifier, publication);
                }
                found = true;
            }
            if(!found) {
                ArrayList<Record> publication = new ArrayList<Record>();
                publication.add(originalRecord);
                record_sets.put(firstOriginalIdentifier, publication);
            }

            // search all related records
			if (!childrenRecords.isEmpty()) {
				for (Record rr : childrenRecords) {
					for (Map.Entry<String, String> identifier : identifiersMap.entrySet()) {
						String identifierName = identifier.getKey();
						String identifierValue = identifier.getValue();

						List<Value> values = rr.getValues(identifierName);
						if (values != null && !values.isEmpty()) {
							if (values.get(0).getAsString().equals(identifierValue)) {
								if(!removedChildrenRecords.contains(rr)) {
									record_sets.get(firstOriginalIdentifier).add(rr);
								}
								removedChildrenRecords.add(rr);
							}
						}
					}
				}
			}
            
            counter++;
        }

        for (Record originalRecord : originalRecords)
        {
            Map<String, String> identifiersMap = createIdentifiersMap(originalRecord);
            int sizePreviousIdentifierService = 0;
            String previousIdentifierService = null;
            
            String firstOriginalIdentifier = null;
            for (Map.Entry<String, String> identifier : identifiersMap.entrySet())
            {
                firstOriginalIdentifier = identifier.getValue();
                
				if (record_sets.keySet().contains(firstOriginalIdentifier)) {
					int size = record_sets.get(firstOriginalIdentifier).size();
					if (size <= sizePreviousIdentifierService) {
						record_sets.remove(firstOriginalIdentifier);
					}
					else {
						sizePreviousIdentifierService = size;
						if(StringUtils.isNotBlank(previousIdentifierService)) {
							record_sets.remove(previousIdentifierService);
						}
						previousIdentifierService = firstOriginalIdentifier;
					}
				}
            }
        }
        
        //results
        for (Map.Entry<String, List<Record>> entry : record_sets.entrySet())
        {
            ItemSubmissionLookupDTO dto = new ItemSubmissionLookupDTO(
                    entry.getValue());
            dtoList.add(dto);
        }
        
        return new ArrayList<String>();
    }

    @Override
    public List<String> generateOutput(RecordSet records, DataOutputSpec spec)
    {
        return generateOutput(records);
    }

    /**
     * @return the items
     */
    public List<ItemSubmissionLookupDTO> getDtoList()
    {
        return dtoList;
    }

    /**
     * @param items
     *            the items to set
     */
    public void setDtoList(List<ItemSubmissionLookupDTO> items)
    {
        this.dtoList = items;
    }

    private Map<String, String> createIdentifiersMap(Record rec)
    {
        Map<String, String> identifiersMap = new HashMap<String, String>();
        List<Value> values = rec.getValues("originalRecord");
        List<Value> valuesproviderName = rec.getValues(SubmissionLookupService.PROVIDER_NAME_FIELD);
        
		String asString = valuesproviderName.get(0).getAsString();
		
        for (Map.Entry<String, String> entry : this.identifiersMap.entrySet()) {
            String providerNameField = entry.getKey();
            String identifierNameField = entry.getValue();
            boolean added = false;
			if (values != null && !values.isEmpty())
            {
                if(valuesproviderName!=null && !valuesproviderName.isEmpty()) {
					if (!providerNameField.equals(asString))
	                {
	                    List<Value> identifierValueList = rec.getValues(identifierNameField);
	                    if (identifierValueList != null && !identifierValueList.isEmpty())
	                    {
	                    	added = true;
	                        identifiersMap.put(identifierNameField, identifierValueList.get(0).getAsString());
	                    }
	                }
                }
            }
			
            if(!added)
            {
            	List<Value> identifierValueList = rec.getValues(identifierNameField);
                if (identifierValueList != null && !identifierValueList.isEmpty())
                {
                    identifiersMap.put(identifierNameField, identifierValueList.get(0).getAsString());
                }
            }
        }

        return identifiersMap;
    }
}
