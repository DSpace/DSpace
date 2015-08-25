/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.listener;


import java.util.UUID;

import org.apache.log4j.Logger;
import org.dspace.app.cris.model.UUIDSupport;
import org.hibernate.HibernateException;
import org.hibernate.event.internal.DefaultSaveOrUpdateEventListener;
import org.hibernate.event.spi.SaveOrUpdateEvent;

public class UUIDListener extends DefaultSaveOrUpdateEventListener
{

	private static Logger log = Logger
            .getLogger(UUIDListener.class);
	
    private void generateUUID(Object object)
    {
        UUIDSupport uuidOwner = (UUIDSupport) object;
        if (uuidOwner.getUuid() == null || uuidOwner.getUuid().isEmpty())
        {
            uuidOwner.setUuid(UUID.randomUUID().toString().trim());
        }
    }

    @Override
    public void onSaveOrUpdate(SaveOrUpdateEvent event)
            throws HibernateException
    {
        Object object = event.getObject();
        if (object instanceof UUIDSupport)
        {
        	log.debug("UUIDSupport Call onSaveOrUpdate " + UUIDListener.class);
            generateUUID(object);
        }
        log.debug("onSaveOrUpdate continue " + UUIDListener.class);
        super.onSaveOrUpdate(event);
        log.debug("onSaveOrUpdate end " + UUIDListener.class);
    }

}
