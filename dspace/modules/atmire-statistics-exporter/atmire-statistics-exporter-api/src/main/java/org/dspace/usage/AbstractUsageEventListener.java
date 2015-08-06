/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.usage;

import org.dspace.services.EventService;
import org.dspace.services.model.EventListener;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * AbstractUsageEventListener is used as the base class for listening events running
 * in the EventService.
 *
 * @author Mark Diggory (mdiggory at atmire.com)
 * @version $Revision: $
 */
public abstract class AbstractUsageEventListener implements EventListener, BeanPostProcessor {

	public AbstractUsageEventListener() {
		super();
	}

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if(beanName.equals("org.dspace.services.EventService"))
        {
            setEventService((EventService) bean);
        }
        return bean;
	}

	/**
	 * Empty String[] flags to have Listener
	 * consume any event name prefixes.
	 */
	public String[] getEventNamePrefixes() {
		return new String[0];
	}

	/**
	 * Currently consumes events generated for
	 * all resources.
	 */
	public String getResourcePrefix() {
		return null;
	}

	public void setEventService(EventService service) {
		if(service != null)
        {
            service.registerEventListener(this);
        }
		else
        {
            throw new IllegalStateException("EventService handed to Listener cannot be null");
        }

	}

}
