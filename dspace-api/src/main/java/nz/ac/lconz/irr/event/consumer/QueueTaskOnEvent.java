package nz.ac.lconz.irr.event.consumer;

import nz.ac.lconz.irr.event.util.CurationHelper;
import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;

import java.sql.SQLException;
import java.util.LinkedList;

/**
 *  @author Andrea Schweer schweer@waikato.ac.nz for the LCoNZ IRRs
 *
 * Abstract event consumer that queues curation tasks when specific events occur.
 */
public abstract class QueueTaskOnEvent implements Consumer {
	private static Logger log = Logger.getLogger(QueueTaskOnEvent.class);

	private CurationHelper helper;

	public void initialize() throws Exception {
		helper = new CurationHelper();
		helper.initTaskNames(getTasksProperty());
		if (!helper.hasTaskNames()) {
			log.error("QueueTaskOnEvent: no configuration value found for tasks to queue (" + getTasksProperty() + "), can't initialise.");
			return;
		}

		helper.initQueueName(getQueueProperty());
	}

	public void consume(Context ctx, Event event) throws Exception {
		Item item = null;

		if (isApplicableEvent(ctx, event)) {
			// check whether it is the last applicable event in the queue
			LinkedList<Event> events = ctx.getEvents();
			for (Event queuedEvent : events) {
				if (isApplicableEvent(ctx, queuedEvent)) {
					return;
				}
			}
			item = findItem(ctx, event);
		}

		if (item == null) {
			// not applicable -> skip
			return;
		}

		helper.addToQueue(item);
	}

	abstract Item findItem(Context ctx, Event event) throws SQLException;

	abstract boolean isApplicableEvent(Context ctx, Event event) throws SQLException;

	public void end(Context ctx) throws Exception {
		helper.queueForCuration(ctx);
	}

	public void finish(Context ctx) throws Exception {
		// enables memory cleanup routines to handle this context correctly
	}

	abstract String getTasksProperty();

	abstract String getQueueProperty();
}
