/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal.curation;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.content.Collection;
import org.dspace.content.Metadatum;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
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
 * based on class by richardrodgers
 * modified for LINDAT/CLARIN
 */
//@Suspendable
public class RequiredMetadata extends AbstractCurationTask
{
	
	private static Logger log = Logger.getLogger(Curator.class);
	
    // map of DCInputSets
    private DCInputsReader reader = null;
    // map of required fields
    private Map<String, List<DCInput>> reqMap = new HashMap<String, List<DCInput>>();

    // meta data equivalence map
    private Map<String, String> mdEquivalenceMap;
    
    // ignore certain metadata from required checks
    private boolean ignoreMdPattern = false;    
    private String[] mdPatterns;
    
    //
    private Map<String, List<RepeatableComponent>> rcMap = new HashMap<String, List<RepeatableComponent>>();
    
    @Override 
    public void init(Curator curator, String taskId) throws IOException
    {
        super.init(curator, taskId);
        try
        {
            reader = new DCInputsReader();

            mdEquivalenceMap = new HashMap<String, String>();
            
            // "dc.contributor.author" <=> "dc.contributor.other"
            mdEquivalenceMap.put("dc.contributor.author", "dc.contributor.other");
            mdEquivalenceMap.put("dc.contributor.other", "dc.contributor.author");           
            
            String checkReqMdPat = ConfigurationManager.getProperty("lr", "lr.curation.metadata.checkrequired.ignore");
            if (checkReqMdPat != null) {
            	ignoreMdPattern = true;
            	
            	// remove spaces at the beginning and end
            	Pattern p = Pattern.compile("(^\\s+|\\s+$)");
            	Matcher m = p.matcher(checkReqMdPat);
            	String cleanedMdPatLine = m.replaceAll("");
            	
            	// split the space separated pattern line 
            	p = Pattern.compile("\\s+");
            	mdPatterns = p.split(cleanedMdPatLine);
            	if (mdPatterns.length == 1) {
            		if (mdPatterns[0].equals("")) {
            			ignoreMdPattern = false;
            		}
            	}
            }
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
            
            String checkExtraInCollections = ConfigurationManager.getProperty("lr", "lr.extrametadata.required.check.col.ids");
            Set<String> checkExtraIds = new HashSet<String>();
            if(checkExtraInCollections != null && checkExtraInCollections.trim().length()>0){
            	String[] colIds = checkExtraInCollections.split(",");
            	checkExtraIds = new HashSet<String>(Arrays.asList(colIds));
            }
            
            boolean checkExtra = false;
            
            int count = 0;
            try
            {
                StringBuilder sb = new StringBuilder();
                String handle = item.getHandle();
                if (handle == null)
                {
                    // we are still in workflow - no handle assigned
                    handle = "in workflow";
                }
                sb.append("Item: ").append(handle);
                String colhandle = null;
                if ( null != item.getOwningCollection() ){
                	colhandle = item.getOwningCollection().getHandle();
                	checkExtra = checkExtraIds.contains(Integer.toString(item.getOwningCollection().getID()));
                }
                Metadatum[] itemTypeDCV = item.getMetadata("dc", "type", null, Item.ANY);
                String itemType = null;
                if(itemTypeDCV != null && itemTypeDCV.length >0){
                	itemType = itemTypeDCV[0].value;
                }
                for (DCInput input : getReqList(colhandle,checkExtra))
                {
                	StringBuilder reqsb = new StringBuilder();
                    reqsb.append(input.getSchema()).append(".");
                    reqsb.append(input.getElement()).append(".");
                    String qual = input.getQualifier();
                    if (qual == null)
                    {
                        qual = "";
                    }
                    reqsb.append(qual);
                    String req = reqsb.toString();
                    
                    boolean mdPatFound = false;
                    if (ignoreMdPattern) {
                    	for (String p: mdPatterns) {
                    		mdPatFound = req.matches("^" + p + "$")?true:false;
                    		if (mdPatFound) {
                    			break;
                    		}
                    	}
                    }
                    
                    if (!mdPatFound) {
                        Metadatum[] vals = item.getMetadataByMetadataString(req);
                        if ((itemType == null || input.isAllowedFor(itemType)) && vals.length == 0)
                        {
                        	if (mdEquivalenceMap.containsKey(req)) {
                        		Metadatum[] valsAlt = item.getMetadataByMetadataString(mdEquivalenceMap.get(req));
                        		if (valsAlt == null || valsAlt.length == 0) {
                                	sb.append(" missing required field: ").append(req);
                                    count++;                    			
                        		}
                        	}
                        	else {
                            	sb.append(" missing required field: ").append(req);
                                count++;                    		
                        	}
                        }                    	
                    }
                }
                
                if(checkExtra){
                        //If we have one field in repeatable component we need all the others
                        for (RepeatableComponent rc : getRepeatableComponents(colhandle)){
                                if(rc.getFieldCount()>1){
                                        Iterator<String> i = rc.iterateFields();
                                        int valuesCount = getCount(item.getMetadataByMetadataString(i.next()));
                                        while(i.hasNext()){
                                    int nextValuesCount = getCount(item.getMetadataByMetadataString(i.next()));
                                                if(valuesCount != nextValuesCount){
                                                        sb.append(" missing field in repeatable component ").append(rc.getName());
                                                        count++;
                                                        break;
                                                }
                                                valuesCount = nextValuesCount;
                                        }
                                }
                        }
                }
                
                if (count == 0)
                {
                    sb = new StringBuilder();
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
           setResult("Object skipped. This task runs only on ITEMs.");
           report("Object skipped");
           return Curator.CURATE_SKIP;
        }
    }
    
    private List<RepeatableComponent> getRepeatableComponents(String handle) throws DCInputsReaderException {
        List<RepeatableComponent> rcList = rcMap.get(handle);
        if (rcList == null)
        {
        	HashMap<String, RepeatableComponent> tmpMap = new HashMap<String, RepeatableComponent>();
        	DCInputSet inputs = reader.getInputsExtra(handle);
            for (int i = 0; i < inputs.getNumberPages(); i++)
            {
                for (DCInput input : inputs.getPageRows(i, true, true))
                {
                    if (input.getExtraRepeatableComponent() != null)
                    {
                    	RepeatableComponent rc;
                    	rc = tmpMap.get(input.getExtraRepeatableComponent());
                    	if(rc == null){
                    		rc = new RepeatableComponent(input.getSchema(),input.getExtraRepeatableComponent());
                    		tmpMap.put(input.getExtraRepeatableComponent(),rc);
                    	}
                    	rc.addField(input.getQualifier());
                    }
                }
            }
            rcList = new ArrayList<RepeatableComponent>();
            rcList.addAll(tmpMap.values()); 
            rcMap.put(handle, rcList);
        }
        return rcList;
	}

	private List<DCInput> getReqList(String colhandle, boolean checkExtra) throws DCInputsReaderException
    {
        List<DCInput> reqList = reqMap.get(colhandle);
        if (reqList == null)
        {
            reqList = new ArrayList<DCInput>();
            DCInputSet[] inputSets = checkExtra ? new DCInputSet[]{reader.getInputs(colhandle), reader.getInputsExtra(colhandle)}
            									: new DCInputSet[]{reader.getInputs(colhandle)};
            for(DCInputSet inputs: inputSets){
                    for (int i = 0; i < inputs.getNumberPages(); i++)
                    {
                        for (DCInput input : inputs.getPageRows(i, true, true))
                        {
                            if (input.isRequired())
                            {
                                reqList.add(input);
                            }
                        }
                    }
            }
            reqMap.put(colhandle, reqList);
        }
        return reqList;
    }
    
    private int getCount(Object[] arr){
    	return arr == null ? 0 : arr.length;
    }
    
    static class RepeatableComponent{
    	
    	private String name;
    	private Set<String> repeatableFields;
    	
    	RepeatableComponent(String schema, String component){
    		this.name = schema + "." + component;
    		repeatableFields = new HashSet<String>();
    	}
    	
    	String getName(){
    		return name;
    	}
    	
    	boolean addField(String fieldName){
    		return repeatableFields.add(name +"."+fieldName);
    	}
    	
    	int getFieldCount(){
    		return repeatableFields.size();
    	}
    	
    	Iterator<String> iterateFields(){
    		return repeatableFields.iterator();
    	}
    	
    }
}


