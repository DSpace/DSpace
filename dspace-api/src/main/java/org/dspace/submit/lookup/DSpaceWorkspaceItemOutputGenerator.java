/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import gr.ekt.bte.core.DataOutputSpec;
import gr.ekt.bte.core.OutputGenerator;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.RecordSet;
import gr.ekt.bte.core.Value;

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
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.submit.util.ItemSubmissionLookupDTO;


/**
 * @author Kostas Stamatis
 */
public class DSpaceWorkspaceItemOutputGenerator implements OutputGenerator {
    
	private Context context;
	private String formName;
	private List<WorkspaceItem> witems;
	private ItemSubmissionLookupDTO dto;
	private Collection collection;
	Map<String, String> outputMap;
	
	private List<String> extraMetadataToKeep;
	
    public DSpaceWorkspaceItemOutputGenerator() {
        
    }

    @Override
    public List<String> generateOutput(RecordSet records) {
    	
    	witems = new ArrayList<WorkspaceItem>();
    	
        Map<String, List<Record>> record_sets = new HashMap<String, List<Record>>();
        for(Record rec : records) {
        	 try {
				WorkspaceItem wi = WorkspaceItem.create(context, collection, true);
				merge(formName, wi.getItem(), rec);
				
				witems.add(wi);
				
			} catch (AuthorizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	 
        }
        
        return new ArrayList<String>();
    }

    @Override
    public List<String> generateOutput(RecordSet records, DataOutputSpec spec) {
        return generateOutput(records);
    }

	public List<WorkspaceItem> getWitems() {
		return witems;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public void setFormName(String formName) {
		this.formName = formName;
	}
	
	public void setDto(ItemSubmissionLookupDTO dto) {
		this.dto = dto;
	}

	public void setOutputMap(Map<String, String> outputMap) {
		this.outputMap = outputMap;
	}

	public void setCollection(Collection collection) {
		this.collection = collection;
	}

	public void setExtraMetadataToKeep(List<String> extraMetadataToKeep) {
		this.extraMetadataToKeep = extraMetadataToKeep;
	}

	//Methods
	public void merge(String formName, Item item, Record record) {

        Record itemLookup = record;
        
        Set<String> addedMetadata = new HashSet<String>();
		for (String field : itemLookup.getFields()) {
			String metadata = getMetadata(formName, itemLookup, field);
			if (StringUtils.isBlank(metadata)) {
				continue;
			}
			if (item.getMetadata(metadata).length == 0
					|| addedMetadata.contains(metadata)) {
				addedMetadata.add(metadata);
				String[] md = splitMetadata(metadata);
				if (isValidMetadata(formName, md)) { //if in extra metadata or in the spefific form
					List<Value> values = itemLookup.getValues(field);
					if (values != null && values.size()>0){
						if (isRepeatableMetadata(formName, md)) { //if metadata is repeatable in form
							for (Value value : values) {
								String[] splitValue = splitValue(value.getAsString());
								if (splitValue[3] != null) {
									item.addMetadata(md[0], md[1], md[2], md[3],
											splitValue[0], splitValue[1],
											Integer.parseInt(splitValue[2]));
								} else {
									item.addMetadata(md[0], md[1], md[2], md[3],
											value.getAsString());
								}
							}
						} else {
							String value = values.iterator().next().getAsString();
							String[] splitValue = splitValue(value);
							if (splitValue[3] != null) {
								item.addMetadata(md[0], md[1], md[2], md[3],
										splitValue[0], splitValue[1],
										Integer.parseInt(splitValue[2]));
							} else {
								item.addMetadata(md[0], md[1], md[2], md[3], value);
							}
						}
					}
				}
			}
		}

		// creo un nuovo context per il check di esistenza dei metadata di cache
		Context context = null;
		try {
			context = new Context();
            for (Record pub : dto.getPublications()) {
                String providerName = SubmissionLookupService.getProviderName(pub);
				if (providerName != SubmissionLookupService.MANUAL_USER_INPUT) {
					for (String field : pub.getFields()) {
						String metadata = getMetadata(formName, pub, field);
						if (StringUtils.isBlank(metadata)) {
							continue;
						}

						String[] md = splitMetadata(metadata);
						if (isValidMetadata(formName, md)) {
							makeSureMetadataExist(context, providerName, md[1],
									md[2]);
							if (isRepeatableMetadata(formName, md)) {
								for (Value value : pub.getValues(field)) {
									String[] splitValue = splitValue(value.getAsString());
									item.addMetadata(providerName, md[1],
											md[2], md[3], splitValue[0],
											splitValue[1],
											Integer.parseInt(splitValue[2]));
								}
							} else {
								String[] splitValue = splitValue(SubmissionLookupService.getFirstValue(pub, field));
								item.addMetadata(providerName, md[1], md[2],
										md[3], splitValue[0], splitValue[1],
										Integer.parseInt(splitValue[2]));
							}
						}
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			if (context != null && context.isValid()) {
				context.abort();
			}
		}
	}
	
	private String getMetadata(String formName,
			Record itemLookup, String name) {
		String type = SubmissionLookupService.getType(itemLookup);
		
		String md = outputMap.get(type + "." + name);
		if (StringUtils.isBlank(md)){
			md = outputMap.get(formName + "." + name);
			if (StringUtils.isBlank(md)){
				md = outputMap.get(name);
			}
		}
		
		//KSTA:ToDo: Make this a modifier
		if (md != null && md.contains("|")) {
			String[] cond = md.trim().split("\\|");
			for (int idx = 1; idx < cond.length; idx++) {
				boolean temp = itemLookup.getFields().contains(cond[idx]);
				if (temp) {
					return null;
				}
			}
			return cond[0];
		}
		return md;
	}
	
	private String[] splitMetadata(String metadata) {
		String[] mdSplit = new String[3];
		if (StringUtils.isNotBlank(metadata)) {
			String tmpSplit[] = metadata.split("\\.");
			if (tmpSplit.length == 4) {
				mdSplit = new String[4];
				mdSplit[0] = tmpSplit[0];
				mdSplit[1] = tmpSplit[1];
				mdSplit[2] = tmpSplit[2];
				mdSplit[3] = tmpSplit[3];
			} else if (tmpSplit.length == 3) {
				mdSplit = new String[4];
				mdSplit[0] = tmpSplit[0];
				mdSplit[1] = tmpSplit[1];
				mdSplit[2] = tmpSplit[2];
				mdSplit[3] = null;
			} else if (tmpSplit.length == 2) {
				mdSplit = new String[4];
				mdSplit[0] = tmpSplit[0];
				mdSplit[1] = tmpSplit[1];
				mdSplit[2] = null;
				mdSplit[3] = null;
			}
		}
		return mdSplit;
	}
	
	private boolean isValidMetadata(String formName, String[] md) {
		try {
			if (extraMetadataToKeep != null
					&& extraMetadataToKeep.contains(StringUtils.join(
							Arrays.copyOfRange(md, 0, 3), "."))) {
				return true;
			}
            return getDCInput(formName, md[0], md[1], md[2])!=null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private DCInput getDCInput(String formName, String schema,
            String element, String qualifier) throws DCInputsReaderException
    {
        DCInputSet dcinputset = new DCInputsReader().getInputs(formName);
        for (int idx = 0; idx < dcinputset.getNumberPages(); idx++)
        {
            for (DCInput dcinput : dcinputset.getPageRows(idx, true, true))
            {
                if (dcinput.getSchema().equals(schema)
                        && dcinput.getElement().equals(element)
                        && (dcinput.getQualifier() != null && dcinput
                                .getQualifier().equals(qualifier))
                        || (dcinput.getQualifier() == null && qualifier == null))
                {
                    return dcinput;
                }
            }
        }
        return null;
    }
	
	private boolean isRepeatableMetadata(String formName, String[] md) {
		try {
			DCInput dcinput = getDCInput(formName, md[0], md[1], md[2]);
			if (dcinput != null) {
				return dcinput.isRepeatable();
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

    private String[] splitValue(String value) {
		String[] splitted = value.split(SubmissionLookupService.SEPARATOR_VALUE_REGEX);
		String[] result = new String[6];
		result[0] = splitted[0];
		result[2] = "-1";
		result[3] = "-1";
		result[4] = "-1";
		if (splitted.length > 1) {
			result[5] = "splitted";
			if (StringUtils.isNotBlank(splitted[1])) {
				result[1] = splitted[1];
			}
			if (splitted.length > 2) {
				result[2] = String.valueOf(Integer.parseInt(splitted[2]));
				if (splitted.length > 3) {
					result[3] = String.valueOf(Integer.parseInt(splitted[3]));
					if (splitted.length > 4) {
						result[4] = String.valueOf(Integer
								.parseInt(splitted[4]));
					}
				}
			}
		}
		return result;
	}
    
    private void makeSureMetadataExist(Context context, String schema,
			String element, String qualifier) {
		try {
			context.turnOffAuthorisationSystem();
			boolean create = false;
			MetadataSchema mdschema = MetadataSchema.find(context, schema);
			MetadataField mdfield = null;
			if (mdschema == null) {
				mdschema = new MetadataSchema(SubmissionLookupService.SL_NAMESPACE_PREFIX + schema,
						schema);
				mdschema.create(context);
				create = true;
			} else {
				mdfield = MetadataField.findByElement(context,
						mdschema.getSchemaID(), element, qualifier);
			}

			if (mdfield == null) {
				mdfield = new MetadataField(mdschema, element, qualifier,
						"Campo utilizzato per la cache del provider submission-lookup: "
								+ schema);
				mdfield.create(context);
				create = true;
			}
			if (create) {
				context.commit();
			}
			context.restoreAuthSystemState();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
