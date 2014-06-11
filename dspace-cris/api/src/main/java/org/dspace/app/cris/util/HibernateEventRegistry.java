/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.util;

import java.util.List;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.beans.factory.annotation.Autowired;

public class HibernateEventRegistry {

	private SessionFactory sessionFactory;

	private Map<String, List<Object>> eventListeners;

	public void init() {

		EventListenerRegistry eventListenerRegistry = ((SessionFactoryImpl) sessionFactory)
				.getServiceRegistry().getService(EventListenerRegistry.class);

		// If you wish to have custom determination and handling of "duplicate"
		// listeners, you would have to add an
		// implementation of the
		// org.hibernate.event.service.spi.DuplicationStrategy contract like
		// this
		// eventListenerRegistry.addDuplicationStrategy( myDuplicationStrategy
		// );
		// EventListenerRegistry defines 3 ways to register listeners:
		// 1) This form overrides any existing registrations with
		// eventListenerRegistry.setListeners( EventType.AUTO_FLUSH,
		// myCompleteSetOfListeners );
		// 2) This form adds the specified listener(s) to the beginning of the
		// listener chain
		// eventListenerRegistry.prependListeners( EventType.AUTO_FLUSH,
		// myListenersToBeCalledFirst );
		// 3) This form adds the specified listener(s) to the end of the
		// listener chain
		// eventListenerRegistry.appendListeners( EventType.AUTO_FLUSH,
		// myListenersToBeCalledLast );

		for (String eTypeStr : eventListeners.keySet()) {
			EventType eType = EventType.resolveEventTypeByName(eTypeStr);
			for (Object listener : eventListeners.get(eTypeStr)) {
				eventListenerRegistry.setListeners(eType, listener);
			}
		}

	}

	public void setEventListeners(Map<String, List<Object>> eventListeners) {
		this.eventListeners = eventListeners;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

}
