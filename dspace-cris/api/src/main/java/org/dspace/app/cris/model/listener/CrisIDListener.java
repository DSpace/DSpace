/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.listener;

import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;

public class CrisIDListener implements PreInsertEventListener 
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
                event.getState()[idx] = ResearcherPageUtils.getPersistentIdentifier((ACrisObject)object);
            }
        }
        return false;
    }

}
