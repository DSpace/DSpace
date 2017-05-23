package uk.ac.edina.datashare.events;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemDataset;
import org.dspace.content.Metadatum;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;

import uk.ac.edina.datashare.utils.DSpaceUtils;
import uk.ac.edina.datashare.utils.MetaDataUtil;

/**
 * This listens for events raised by DSpace for DataShare.
 */
public class DataShareConsumer implements Consumer{
    private static final Logger LOG = Logger.getLogger(DataShareConsumer.class);
    private ConsumerEvent event = null;
    
    /*
     * (non-Javadoc)
     * @see org.dspace.event.Consumer#consume(org.dspace.core.Context, org.dspace.event.Event)
     */
    public void consume(Context context, Event event) throws Exception{
        if(this.event == null && event.getSubjectType() == Constants.COLLECTION){
            switch(event.getEventType()){
                case Event.ADD:{
                    Item item = this.getItem(context, event);
                    if(item.isArchived()){
                        // if a new item has been created and archived,
                        // mark item for cleaning up
                        this.event = new ConsumerEvent(item, event.getEventType());
                    }
                    break;
                }
                case Event.REMOVE:{
                    this.event = new ConsumerEvent(
                            this.getItem(context, event), event.getEventType());
                    break;
                }
                default:{
                    LOG.info("Unkown subject type: " + event.getSubjectType());
                }
            }
        }
    }
        
    private Item getItem(Context context, Event event) throws Exception{
        Item item = null;
        //event.getObjectID();
        DSpaceObject dso = event.getObject(context);
       /* LOG.info("****");
        LOG.info(dso);
        LOG.info(event.getObjectID());
        LOG.info(Item.find(context, event.getObjectID()));
        LOG.info(dso.getClass());
        LOG.info(dso.getID());*/
        if(dso instanceof Item)
        {
            item = (Item)dso;
        }
        
        return item;
    }

    /*
     * (non-Javadoc)
     * @see org.dspace.event.Consumer#end(org.dspace.core.Context)
     */
	public void end(Context context) throws Exception
    {
        if(this.event != null)
        {
            switch(this.event.getType()){
                case Event.ADD:{
                    LOG.info("ADD ***********************");
                    this.addItem(context, this.event.getItem());
                    break;
                }
                case Event.REMOVE:{
                    LOG.info("DELETE ***********************");
                    LOG.info(this.event.getItem());
                    LOG.info(this.event.getItem().getID());
                    LOG.info(this.event.getItem().getHandle());
                    new ItemDataset(this.event.getItem()).delete();
                    break;
                }
                default:{
                    LOG.info("Unkown event type: " + this.event.getType());
                }
            }
            this.event = null;
        }
    }
	
	private void addItem(Context context, Item item){
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
        new ItemDataset(item).createDataset();
        
        context.turnOffAuthorisationSystem();
        
        try{
            // commit changes
            item.update();
        }
        catch(SQLException ex){
            throw new RuntimeException(ex);
        }
        catch(AuthorizeException ex){
            throw new RuntimeException(ex);
        }
        finally{
            context.restoreAuthSystemState();
        }   
	}
    
    // not used
    public void finish(Context ctx) throws Exception{}
    public void initialize() throws Exception{}
    
    private class ConsumerEvent{
        private Item item;
        private int type;
        public ConsumerEvent(Item item, int type){
            this.item = item;
            this.type = type;
        }
        public Item getItem() {
            return item;
        }
        public int getType() {
            return type;
        }
    }
}
