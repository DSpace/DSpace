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

import it.cilea.osd.common.listener.NativePostUpdateEventListener;
import it.cilea.osd.common.listener.NativePreInsertEventListener;
import it.cilea.osd.common.model.Identifiable;

public class UUIDListener implements NativePreInsertEventListener
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
    public <T extends Identifiable> void onPreInsert(T entity) {
    	Object object = entity;
        if (object instanceof UUIDSupport)
        {
        	log.debug("UUIDSupport Call onPostUpdate " + UUIDListener.class);
            generateUUID(object);
        }
    }
}
