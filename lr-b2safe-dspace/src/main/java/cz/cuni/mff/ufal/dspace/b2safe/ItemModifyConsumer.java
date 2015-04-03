/**
 * Institute of Formal and Applied Linguistics
 * Charles University in Prague, Czech Republic
 * 
 * http://ufal.mff.cuni.cz
 * 
 */

package cz.cuni.mff.ufal.dspace.b2safe;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;

/**
 * Replicate if needed.
 * 
 * These lines were added to dspace.cfg for the consumer to become active:
 * 
 * # consumer to maintain the browse index event.consumer.eudatreplication.class
 * = cz.cuni.mff.ufal.dspace.b2safe.ItemModifyConsumer
 * event.consumer.eudatreplication.filters =
 * Community|Collection|Item+Create|Modify
 * 
 * and the eudatreplication must be added to event.dispatcher.default.consumers
 * 
 */
public class ItemModifyConsumer implements Consumer {

	Logger log = Logger.getLogger(ItemModifyConsumer.class);
	
	final private List<String> updatedHandles = new ArrayList<String>();

	public void initialize() throws Exception {
		if(ReplicationManager.isReplicationOn()) {
			try {
				ReplicationManager.initialize();
			} catch(Exception e) {
				log.error(e);
			}
		}
	}

	public void consume(Context context, Event event) throws Exception {
		
		// if not on don't do anything
		if(!ReplicationManager.isReplicationOn()) {
			return;
		}
		
		// if not initialized try to initialize 
		if(!ReplicationManager.isInitialized()) {
			try {
				boolean test = ReplicationManager.initialize();
				if(!test) {
					throw new Exception("Unable to initialize Replication Service.");
				}
			} catch(Exception e) {
				log.error(e);
			}
		}
		
		int subjectType = event.getSubjectType();
		int eventType = event.getEventType();

		switch (subjectType) {
		// If an Item is created or its metadata is modified..
		case Constants.ITEM:
			if (eventType == Event.MODIFY) {
				Item item = (Item) event.getSubject(context);
				if (item != null) {
					// replicate if necessary
					try {
						String handle = item.getHandle();
						if (handle == null) {
							// e.g., during submission
							return;
						}

						// we have already done it in this round
						// - can happen because one event can get here multiple
						// times
						// e.g., after editor approval + finishItem
						if (updatedHandles.contains(handle)) {
							return;
						}

						updatedHandles.add(handle);

						// testing again just to be sure
						if (ReplicationManager.isInitialized() && ReplicationManager.isReplicationOn()) {
							// force overwrite
							ReplicationManager.replicate(context, handle, item, true);
						}

					} catch (Exception e) {
						// non public - in logs
					}
				}
			}
			break;
		default:
			break;
		}

	}

	public void end(Context ctx) {
		updatedHandles.clear();
	}

	public void finish(Context ctx) {
	}

}
