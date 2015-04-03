/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.event;

import org.apache.log4j.Logger;
import org.dspace.content.Community;
import org.dspace.content.Collection;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.Bitstream;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.utils.DSpace;
import org.dspace.xoai.app.BasicConfiguration;
import org.dspace.xoai.app.XOAI;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Class for updating oai index from content events.
 *
 */
public class OAIIndexEventConsumer implements Consumer {
    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(OAIIndexEventConsumer.class);

    // collect Items, Collections, Communities that need indexing
    private Set<Item> itemsToUpdate = null;

    DSpace dspace = new DSpace();

    public void initialize() throws Exception {

    }

    /**
     * Consume a content event -- just build the sets of objects to add (new) to
     * the index, update, and delete.
     *
     * @param ctx   DSpace context
     * @param event Content event
     */
    public void consume(Context ctx, Event event) throws Exception {

        if (itemsToUpdate == null) {
            itemsToUpdate = new HashSet<Item>();
        }

        int st = event.getSubjectType();
        if (!(st == Constants.ITEM || st == Constants.BUNDLE
                || st == Constants.COLLECTION || st == Constants.COMMUNITY || st == Constants.BITSTREAM)) {
            log
                    .warn("IndexConsumer should not have been given this kind of Subject in an event, skipping: "
                            + event.toString());
            return;
        }

        DSpaceObject subject = event.getSubject(ctx);
        DSpaceObject object = event.getObject(ctx);


        int et = event.getEventType();
        
        if(object != null && event.getObjectType() == Constants.ITEM){
        	//update just object
        	itemsToUpdate.add((Item)object);
        	return;
        }
        
        if(subject != null){
                if(event.getSubjectType() == Constants.COLLECTION || event.getSubjectType() == Constants.COMMUNITY){
                        if(et == Event.MODIFY || et == Event.MODIFY_METADATA || et == Event.REMOVE || et == Event.DELETE){
                                //must update all the items
                        	if(subject.getType() == Constants.COMMUNITY){
                        		for(Collection col : ((Community)subject).getCollections()){
                        			addAll(col);
                        		}
                        	}else{
                        		addAll((Collection)subject);
                        	}
                        }
                }else if(event.getSubjectType() == Constants.BITSTREAM || event.getSubjectType() == Constants.BUNDLE){
                        //must update owning items regardless the event
                	if(subject.getType() == Constants.BITSTREAM){
                		for(Bundle bun : ((Bitstream)subject).getBundles()){
                			itemsToUpdate.addAll(Arrays.asList(bun.getItems()));
                		}
                	} else {
                		itemsToUpdate.addAll(Arrays.asList(((Bundle)subject).getItems()));
                	}
                }else if(event.getSubjectType() == Constants.ITEM){
                        //any event reindex this item        	
                	itemsToUpdate.add((Item)subject);
                }
        }
    }
    
    private void addAll(Collection col) throws SQLException{
    	ItemIterator i = col.getAllItems();
    	while(i.hasNext()){
    		itemsToUpdate.add(i.next());
    	}
    }

    /**
     * Process sets of objects to add, update, and delete in index. Correct for
     * interactions between the sets -- e.g. objects which were deleted do not
     * need to be added or updated, new objects don't also need an update, etc.
     */
	public void end(Context ctx) throws Exception {

		try {
			if (itemsToUpdate != null) {

				Set<Item> filtered = new HashSet<Item>(itemsToUpdate.size());
				for (Item item : itemsToUpdate) {
					if (item.getHandle() == null) {
						// probably submission item, skip
						continue;
					}
					filtered.add(item);
				}

				// "free" the resources
				itemsToUpdate = null;

				XOAI indexer = new XOAI(ctx, false, false, false);
				AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
						new Class[] { BasicConfiguration.class });
				applicationContext.getAutowireCapableBeanFactory()
						.autowireBean(indexer);
				indexer.indexItems(filtered);
				applicationContext.close();
			}
		} catch (Exception e) {
			itemsToUpdate = null;
			throw e;
		}
	}

    public void finish(Context ctx) throws Exception {
        // No-op

    }

}
