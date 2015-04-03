/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.curation;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.dspace.content.Community;
import org.dspace.content.Metadatum;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

public class AddBrandingMetadata extends AbstractCurationTask {

    private int status = Curator.CURATE_UNSET;

    // The log4j logger for this class
    private static Logger log = Logger.getLogger(Curator.class);

	@Override
	public int perform(DSpaceObject dso) throws IOException {

		// The results that we'll return
        StringBuilder results = new StringBuilder();

        // Unless this is  an item, we'll skip this item
        status = Curator.CURATE_SKIP;
		
        if (dso instanceof Item)
        {
        	try {
	            Item item = (Item)dso;	            
	            Community c[] = item.getCommunities();
	            if ( c!=null && c.length>0) {
	            	String cName = c[0].getName();

					Metadatum[] oldLocalBrandings = item.getMetadata("local", "branding", null, Item.ANY);
					String oldCName = ( 1 == oldLocalBrandings.length ) ? oldLocalBrandings[0].value : "";
					if ( !cName.equals(oldCName) ) {
						String oldBranding = oldCName;

						if ( 1 < oldLocalBrandings.length ) {
							oldBranding = "<< multiple local.branding >>";
						}else if ( 0 == oldLocalBrandings.length ) {
							oldBranding = "<< no local.branding >>";
						}

						results.append(
							String.format("Item [%s] had different branding [%s], changed to [%s]",
								item.getHandle(), oldBranding, cName)
						);
						item.clearMetadata("local", "branding", null, Item.ANY);
						item.addMetadata("local", "branding", null, null, cName);
						item.update();
					}
	            }
				status = Curator.CURATE_SUCCESS;
        	} catch (Exception ex) {
        		status = Curator.CURATE_FAIL;
        		results.append(ex.getLocalizedMessage()).append("\n");
        	}
        }
        
        report(results.toString());
        setResult(results.toString());
		return status;
	}


}
