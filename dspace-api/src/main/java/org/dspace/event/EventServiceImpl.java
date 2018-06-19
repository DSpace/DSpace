/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.event;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.ArrayUtils;

import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PoolUtils;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.event.service.EventService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Class for managing the content event environment. The EventManager mainly
 * acts as a factory for Dispatchers, which are used by the Context to send
 * events to consumers. It also contains generally useful utility methods.
 * 
 * Version: $Revision$
 */
public class EventServiceImpl implements EventService
{
    /** log4j category */
    private Logger log = Logger.getLogger(EventServiceImpl.class);


    protected DispatcherPoolFactory dispatcherFactory = null;

    protected GenericKeyedObjectPoolConfig poolConfig = null;

    // Keyed FIFO Pool of event dispatchers
    protected KeyedObjectPool dispatcherPool = null;

    protected Map<String, Integer> consumerIndicies = null;

    protected String CONSUMER_PFX = "event.consumer";
    
    private static final ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();


    protected EventServiceImpl()
    {
        initPool();
        log.info("EventService dispatcher pool initialized");
    }

    private void initPool()
    {

        if (dispatcherPool == null)
        {

            // TODO EVENT Some of these pool configuration
            // parameters can live in dspace.cfg or a
            // separate configuration file

            // TODO EVENT Eviction parameters should be set

            poolConfig = new GenericKeyedObjectPoolConfig();
            poolConfig.setMaxTotalPerKey(100);
            poolConfig.setMaxIdlePerKey(5);
            poolConfig.setMaxTotal(100);

            try
            {
                dispatcherFactory = new DispatcherPoolFactory();


                dispatcherPool = PoolUtils
                        .synchronizedPool(new GenericKeyedObjectPool(
                                dispatcherFactory, poolConfig));

                enumerateConsumers();

            }
            catch (Exception e)
            {
                log.error("Could not initialize EventService dispatcher pool", e);
            }

        }
    }

    @Override
    public Dispatcher getDispatcher(String name)
    {
        if (dispatcherPool == null)
        {
            initPool();
        }

        if (name == null)
        {
            name = DEFAULT_DISPATCHER;
        }

        try
        {
            return (Dispatcher) dispatcherPool.borrowObject(name);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Unable to aquire dispatcher named " + name, e);
        }

    }

    @Override
    public void returnDispatcher(String key, Dispatcher disp)
    {
        try
        {
            dispatcherPool.returnObject(key, disp);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Unable to return dispatcher named " + key, e);
        }
    }

    @Override
    public int getConsumerIndex(String consumerClass)
    {
        Integer index = (Integer) consumerIndicies.get(consumerClass);
        return index != null ? index.intValue() : -1;

    }

    protected void enumerateConsumers()
    {
        // Get all configs starting with CONSUMER_PFX
        List<String> propertyNames = configurationService.getPropertyKeys(CONSUMER_PFX);
        int bitSetIndex = 0;

        if (consumerIndicies == null)
        {
            consumerIndicies = new HashMap<String, Integer>();
        }

        for(String ckey : propertyNames)
        {
            if (ckey.endsWith(".class"))
            {
                String consumerName = ckey.substring(CONSUMER_PFX.length()+1,
                        ckey.length() - 6);

                consumerIndicies.put(consumerName, (Integer) bitSetIndex);
                bitSetIndex++;
            }
        }
    }

    protected class DispatcherPoolFactory implements KeyedPooledObjectFactory<String,Dispatcher>
    {

        // Prefix of keys in DSpace Configuration
        private static final String PROP_PFX = "event.dispatcher";

        // Cache of event dispatchers, keyed by name, for re-use.
        protected Map<String, String> dispatchers = new HashMap<String, String>();

        public DispatcherPoolFactory()
        {
            parseEventConfig();
        }

        public PooledObject<Dispatcher> wrap(Dispatcher d) {
           return new DefaultPooledObject<>(d);
        }

        @Override
        public PooledObject<Dispatcher> makeObject(String dispatcherName) throws Exception
        {
            Dispatcher dispatcher = null;
            String dispClass = dispatchers.get(dispatcherName);

            if (dispClass != null)
            {
                try
                {
                    // all this to call a constructor with an argument
                    final Class argTypes[] = { String.class };
                    Constructor dc = Class.forName(dispClass).getConstructor(
                            argTypes);
                    Object args[] = new Object[1];
                    args[0] = dispatcherName;
                    dispatcher = (Dispatcher) dc.newInstance(args);

                    // OK, now get its list of consumers/filters
                    String consumerKey = PROP_PFX + "." + dispatcherName
                            + ".consumers";
                    String[] consumers = configurationService
                            .getArrayProperty(consumerKey);
                    if (ArrayUtils.isEmpty(consumers))
                    {
                        throw new IllegalStateException(
                                "No Configuration entry found for consumer list of event Dispatcher: \""
                                        + consumerKey + "\"");
                    }

                    ConsumerProfile consumerProfile = null;

                    for (String consumer : consumers)
                    {
                        consumerProfile = ConsumerProfile
                                .makeConsumerProfile(consumer);
                        consumerProfile.getConsumer().initialize();

                        dispatcher.addConsumerProfile(consumerProfile);
                    }
                }
                catch (NoSuchMethodException e)
                {
                    throw new IllegalStateException(
                            "Constructor not found for event dispatcher="
                                    + dispatcherName, e);
                }
                catch (InvocationTargetException e)
                {
                    throw new IllegalStateException(
                            "Error creating event dispatcher=" + dispatcherName,
                            e);
                }
                catch (ClassNotFoundException e)
                {
                    throw new IllegalStateException(
                            "Dispatcher/Consumer class not found for event dispatcher="
                                    + dispatcherName, e);
                }
                catch (InstantiationException e)
                {
                    throw new IllegalStateException(
                            "Dispatcher/Consumer instantiation failure for event dispatcher="
                                    + dispatcherName, e);
                }
                catch (IllegalAccessException e)
                {
                    throw new IllegalStateException(
                            "Dispatcher/Consumer access failure for event dispatcher="
                                    + dispatcherName, e);
                }
            }
            else
            {
                throw new IllegalStateException(
                        "Requested Dispatcher Does Not Exist In DSpace Configuration!");
            }

            return wrap(dispatcher);

        }

        @Override
        public void activateObject(String arg0, PooledObject<Dispatcher> arg1) throws Exception
        {
            // No-op
            return;

        }

        @Override
        public void destroyObject(String key, PooledObject<Dispatcher> pooledDispatcher)
                throws Exception
        {
            Context ctx = new Context();

            try {
                Dispatcher dispatcher = pooledDispatcher.getObject();

                for (Iterator ci = dispatcher.getConsumers()
                        .iterator(); ci.hasNext();)
                {
                    ConsumerProfile cp = (ConsumerProfile) ci.next();
                    if (cp != null)
                    {
                        cp.getConsumer().finish(ctx);
                    }
                }
            } catch (Exception e) {
                ctx.abort();
                throw e;
            }
        }

        @Override
        public void passivateObject(String arg0, PooledObject<Dispatcher> arg1) throws Exception
        {
            // No-op
            return;

        }

        @Override
        public boolean validateObject(String arg0, PooledObject<Dispatcher> arg1)
        {
            // No-op
            return false;
        }

        /**
         * Looks through the configuration for dispatcher configurations and
         * loads one of each into a HashMap. This Map will be used to clone new
         * objects when the pool needs them.
         * 
         * Looks for configuration properties like:
         * 
         * <pre>
         *  # class of dispatcher &quot;default&quot;
         *  event.dispatcher.default.class = org.dspace.event.BasicDispatcher
         * </pre>
         * 
         */
        private void parseEventConfig()
        {
            // Get all configs starting with PROP_PFX
            List<String> propertyNames = configurationService.getPropertyKeys(PROP_PFX);
            
            for(String ckey : propertyNames)
            {
                // If it ends with ".class", append it to our list of dispatcher classes
                if (ckey.endsWith(".class"))
                {
                    String name = ckey.substring(PROP_PFX.length()+1, ckey
                            .length() - 6);
                    String dispatcherClass = configurationService
                            .getProperty(ckey);

                    dispatchers.put(name, dispatcherClass);

                }
            }
        }
    }
}