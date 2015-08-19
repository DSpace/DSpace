/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.event;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;

/**
 * An instance of this class contains the configuration profile of a specific,
 * named Consumer, <em>in the context of a specific
 * Dispatcher</em>. This
 * includes the name, the class to instantiate and event filters. Note that all
 * characteristics are "global" and the same for all dispatchers.
 * 
 * @version $Revision$
 */
public class ConsumerProfile
{
    /** log4j category */
    private static Logger log = Logger.getLogger(ConsumerProfile.class);

    /** Name matching the key in DSpace Configuration */
    private String name;

    /** Instance of configured consumer class */
    private Consumer consumer;

    /** Filters - each is an array of 2 bitmasks, action mask and subject mask */
    private List<int[]> filters;

    // Prefix of keys in DSpace Configuration.
    private static final String CONSUMER_PREFIX = "event.consumer.";

    /**
     * Constructor.
     */
    private ConsumerProfile(String name)
    {
        this.name = name;
    }

    /**
     * Factory method, create new profile from configuration.
     * 
     * @param name
     *            configuration name of the consumer profile
     * @return a new ConsumerProfile; never null.
     */
    public static ConsumerProfile makeConsumerProfile(String name)
            throws IllegalArgumentException, ClassNotFoundException,
            InstantiationException, IllegalAccessException
    {
        ConsumerProfile result = new ConsumerProfile(name);
        result.readConfiguration();
        return result;
    }

    // Get class and filters from DSpace Configuration.
    private void readConfiguration() throws IllegalArgumentException,
            ClassNotFoundException, InstantiationException,
            IllegalAccessException
    {
        String className = ConfigurationManager.getProperty(CONSUMER_PREFIX
                + name + ".class");
        String filterString = ConfigurationManager.getProperty(CONSUMER_PREFIX
                + name + ".filters");

        if (className == null)
        {
            throw new IllegalArgumentException(
                    "No class configured for consumer named: " + name);
        }
        if (filterString == null)
        {
            throw new IllegalArgumentException(
                    "No filters configured for consumer named: " + name);
        }

        consumer = (Consumer) Class.forName(className.trim()).newInstance();

        // Each "filter" is <objectTypes> + <eventTypes> : ...
        filters = new ArrayList<int[]>();
        String part[] = filterString.trim().split(":");
        for (int j = 0; j < part.length; ++j)
        {
            String fpart[] = part[j].split("\\+");
            if (fpart.length != 2)
            {
                log
                        .error("Bad Filter clause in consumer stanza in Configuration entry for "
                                + CONSUMER_PREFIX
                                + name
                                + ".consumers: "
                                + part[j]);
            }
            else
            {
                int filter[] = new int[2];
                filter[0] = 0;
                filter[1] = 0;
                String objectNames[] = fpart[0].split("\\|");
                for (int k = 0; k < objectNames.length; ++k)
                {
                    int ot = Event.parseObjectType(objectNames[k]);
                    if (ot == 0)
                    {
                        log
                                .error("Bad ObjectType in Consumer Stanza in Configuration entry for "
                                        + CONSUMER_PREFIX
                                        + name
                                        + ".consumers: " + objectNames[k]);
                    }
                    else
                    {
                        filter[Event.SUBJECT_MASK] |= ot;
                    }
                }
                String eventNames[] = fpart[1].split("\\|");
                for (int k = 0; k < eventNames.length; ++k)
                {
                    int et = Event.parseEventType(eventNames[k]);
                    if (et == 0)
                    {
                        log
                                .error("Bad EventType in Consumer Stanza in Configuration entry for "
                                        + CONSUMER_PREFIX
                                        + name
                                        + ".consumers: " + eventNames[k]);
                    }
                    else
                    {
                        filter[Event.EVENT_MASK] |= et;
                    }
                }
                filters.add(filter);
            }
        }
    }

    public Consumer getConsumer()
    {
        return consumer;
    }

    public List<int[]> getFilters()
    {
        return filters;
    }

    public String getName()
    {
        return name;
    }
}
