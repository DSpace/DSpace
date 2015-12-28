/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.curation;

import org.dspace.app.util.DCInput;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.OpenAIREAuthority;
import org.dspace.core.Constants;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;

public class FixOpenAIREMetadata extends AbstractCurationTask
{
	
	private int status = Curator.CURATE_UNSET;

    @Override
    public int perform(DSpaceObject dso) throws IOException
    {
        // Unless this is  an item, we'll skip this item
        status = Curator.CURATE_SKIP;
        
		// The results that we'll return
        StringBuilder results = new StringBuilder();
    	
        if (dso.getType() == Constants.ITEM)
        {
            Item item = (Item)dso;
            
            
            ArrayList<String> valid_dc_relations = new ArrayList<String>();

            OpenAIREAuthority authority = new OpenAIREAuthority();
            
            for(Metadatum m : item.getMetadata("local", "sponsor", null, Item.ANY)) {
            	if(!m.value.contains("euFunds")) continue;
            	String value[] = m.value.split(DCInput.ComplexDefinition.SEPARATOR);
            	String id_s[] = value[1].split("-");
            	String id = null;            	
            	try{
            		Long.parseLong(id_s[id_s.length-1]);
            		id = id_s[id_s.length-1];
            	} catch(NumberFormatException e) {
            		id = id_s[id_s.length-2];
            	}
            	
            	Choices choices = authority.getMatches("dc_relation", id, -1, 0, 0, null);
            	if(choices.total>0) {
            		String dc_relation_value = choices.values[0].value;
            		
            		valid_dc_relations.add(dc_relation_value);            		

            		
            		//test if sponsor contains OpenAIRE id at the end
            		if(!value[value.length-1].equals(dc_relation_value)){
            			Metadatum newValue = m.copy();
            			newValue.value += DCInput.ComplexDefinition.SEPARATOR + dc_relation_value;
            			item.replaceMetadataValue(m, newValue);
            		}
            		            		
            	} else {
            		status = Curator.CURATE_ERROR;
            		results.append("OpenAIRE return zero results for '" + value[1] + "'");
                    report(results.toString());
                    setResult(results.toString());
                    return status;
            	}
            	
            }

            item.clearMetadata("dc", "relation", null, Item.ANY);
            for(String dc_relation : valid_dc_relations) {
            	item.addMetadata("dc", "relation", null, Item.ANY, dc_relation);
            }
            
            try {
				item.update();
	            status = Curator.CURATE_SUCCESS;
				//Curation before approval - might not have handle
				String prefix = item.getHandle() == null ? "Item" : item.getHandle();
	            results.append(prefix + " synced for OpenAIRE").append("\n");
			} catch (SQLException | AuthorizeException e) {
				status = Curator.CURATE_FAIL;
				results.append(e.getLocalizedMessage()).append("\n");
			}
        }

        report(results.toString());
        setResult(results.toString());
        return status;
    }
}
