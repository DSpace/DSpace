/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.listener;

import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;

public class CrisIDListener implements PreInsertEventListener, PreUpdateEventListener 
{
    
    @Transient
    private static Logger log = Logger.getLogger(CrisIDListener.class);

    @Override
    public boolean onPreInsert(PreInsertEvent event)
    {
        Object object = event.getEntity();
        if (object instanceof ACrisObject)
        {
            int idx = 0;
            boolean found = false;
            for (String propName : event.getPersister().getPropertyNames())
            {
                if ("crisID".equals(propName))
                {
                    found = true;
                    break;
                }
                idx++;
            }
            if (found)
            {
				String crisID = ResearcherPageUtils
						.getPersistentIdentifier((ACrisObject) object);
				event.getState()[idx] = crisID;
				ACrisObject crisObj = (ACrisObject) object;
				crisObj.setCrisID(crisID);
            }
        }
        return false;
    }

	@Override
	public boolean onPreUpdate(PreUpdateEvent event) {
        Object object = event.getEntity();
        if (object instanceof ACrisObject)
        {
            int idx = 0;
            boolean found = false;
            for (String propName : event.getPersister().getPropertyNames())
            {
                if ("crisID".equals(propName))
                {
                    found = true;
                    break;
                }
                idx++;
            }
            if (found)
            {
				ACrisObject crisObj = (ACrisObject) object;
				String crisID = crisObj.getCrisID();
				if(StringUtils.isBlank(crisID)) {
					crisID = ResearcherPageUtils
							.getPersistentIdentifier((ACrisObject) object);
					event.getState()[idx] = crisID;					
				}
				crisObj.setCrisID(crisID);
            }
        }
        return false;
	}

}
