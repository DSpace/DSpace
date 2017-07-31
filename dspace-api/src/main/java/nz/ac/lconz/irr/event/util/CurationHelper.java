package nz.ac.lconz.irr.event.util;

import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.curate.Curator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Helper class to deal with curation tasks.
 * @author Andrea Schweer schweer@waikato.ac.nz for the LCoNZ IRRs
 */
public class CurationHelper {
	private static Logger log = Logger.getLogger(CurationHelper.class);

	private List<String> taskNames = new ArrayList<String>();
	private String queueName = "continually";
	private ArrayList<Item> toQueue;

	public void initQueueName(String queueProperty) {
		String queueConfig = ConfigurationManager.getProperty("lconz-event", queueProperty);
		if (queueConfig != null && !"".equals(queueConfig)) {
			queueName = queueConfig;
			log.info("Using queue name " + queueName);
		} else {
			log.info("No queue name specified, using default: " + queueName);
		}
	}

	public void initTaskNames(String tasksProperty) {
		List<String> taskNamesList;
		String taskConfig = ConfigurationManager.getProperty("lconz-event", tasksProperty);

        // From VSIM-36 testing, we found that when used with DSpace 6x, this code is difficult to configure correctly...
        // ...so we will hard code a default, allowing us to continue using this code with VSim, even though
        // it's difficult to configure. Maybe someday we'll figure out something better.
		if (taskConfig == null || "".equals(taskConfig)) {
            taskConfig = "vsiminit";
		}

        // since we've guaranteed there is a value in taskConfig, we can assume it's safe to split it into the taskNamesList
		taskNamesList = Arrays.asList(taskConfig.split("\\s*,\\s*"));

		taskNames.addAll(taskNamesList);
		log.info("Setting up tasks as " + Arrays.deepToString(taskNames.toArray()));
	}

	public void addToQueue(Item item) {
		if (toQueue == null) {
			toQueue = new ArrayList<Item>();
		}
		toQueue.add(item);
		log.info("Adding item " + item.getHandle() + " to list of items to queue");
	}

	public void queueForCuration(Context ctx) throws IOException {
		if (toQueue != null && !toQueue.isEmpty()) {
			log.info("Actually queueing " + toQueue.size() + " items for curation");
			for (String taskName : taskNames) {
				Curator curator = new Curator().addTask(taskName);
				for (Item item : toQueue) {
					String identifier;
					if (item.getHandle() != null) {
						identifier = item.getHandle();
					} else {
						identifier = item.getID()  + "";
					}
					log.info("Queued item " + identifier + " for curation in queue " + queueName + ", task " + taskName);
					curator.queue(ctx, identifier, queueName);
				}
			}
		}
		toQueue = null;
	}

	public boolean hasTaskNames() {
		return !taskNames.isEmpty();
	}
}
