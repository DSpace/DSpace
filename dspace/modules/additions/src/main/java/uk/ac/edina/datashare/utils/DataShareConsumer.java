package uk.ac.edina.datashare.utils;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemDataset;
import org.dspace.content.Metadatum;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;

/**
 * This listens for events raised by DSpace for DataShare.
 */
public class DataShareConsumer implements Consumer
{
    //private static final Logger LOG = Logger.getLogger(DataShareConsumer.class);
    private Item item = null;
    
    /*
     * (non-Javadoc)
     * @see org.dspace.event.Consumer#consume(org.dspace.core.Context, org.dspace.event.Event)
     */
    public void consume(Context context, Event event) throws Exception
    {
        // if a new item has been created and archived, mark item for cleaning up
        if(this.item == null &&
                event.getSubjectType() == Constants.COLLECTION &&
                event.getEventType() == Event.ADD)
        {
            DSpaceObject dso = event.getObject(context);
            
            if(dso instanceof Item)
            {
                Item item = (Item)dso;

                if(item.isArchived())
                {
                    this.item = item;
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.dspace.event.Consumer#end(org.dspace.core.Context)
     */
	public void end(Context context) throws Exception
    {
        if(this.item != null)
        {
            // clear field used to store license type
            DSpaceUtils.clearUserLicenseType(item);

            // copy hijacked spatial country to dc.coverage.spatial
            Metadatum[] vals = DSpaceUtils.getHijackedSpatial(item);
            for(int i = 0; i < vals.length; i++){
            	MetaDataUtil.setSpatial(item, vals[i].value, false);
            }            
            
            // clear hijacked spatial field
            DSpaceUtils.clearHijackedSpatial(item);
           
            // create zip file
            new ItemDataset(this.item).createDataset();
            
            context.turnOffAuthorisationSystem();
            
            try{
            	// commit changes
            	item.update();
            }
            finally{
            	context.restoreAuthSystemState();
            }
            
            this.item = null;
        }
    }
    
    // not used
    public void finish(Context ctx) throws Exception{}
    public void initialize() throws Exception{}
}
