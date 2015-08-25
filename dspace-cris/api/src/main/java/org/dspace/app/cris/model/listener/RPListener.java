/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.listener;

import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.RestrictedField;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;

import it.cilea.osd.common.core.SingleTimeStampInfo;

/**
 * This listner is used to keep track of changes in the RP name fields, all the
 * Event that are not related to the ResearcherPage Entity will be ignored. When
 * a change happen (adding a new form, removing an old one or change the
 * visibility flag) the <code>namesModifiedTimeStamp</code> field in the
 * Researcher Page is updated with the current date/timestamp.
 * 
 * @see ResearcherPage#getNamesModifiedTimeStamp()
 * @author cilea
 * 
 */
public class RPListener implements PreUpdateEventListener, PreInsertEventListener,
        PostLoadEventListener
{
	
	private static Logger log = Logger
            .getLogger(RPListener.class);
	
    /**
     * After the loading of the data the listner store "the initial values" of
     * the name fields in a transient field so to compare it with the current
     * values before saving to discover changes
     */
    public void onPostLoad(PostLoadEvent event)
    {
        Object object = event.getEntity();
        if (object instanceof ResearcherPage)
        {
        	log.debug("Call onPostLoad " + RPListener.class);
        	
            ResearcherPage rp = (ResearcherPage) object;

            String oldNames = "";

            Set<String> oldNamesSet = new TreeSet<String>();
            if(rp.getFullName()!=null) {
                oldNamesSet.add(rp.getFullName());
            }
            if (rp.getPreferredName().getValue() != null)
            {
                oldNamesSet.add(rp.getPreferredName().getValue()+rp.getPreferredName().getVisibility());
            }
            if (rp.getTranslatedName().getValue() != null)
            {
                oldNamesSet.add(rp.getTranslatedName().getValue()+rp.getTranslatedName().getVisibility());
            }
            for (RestrictedField variant : rp.getVariants())
            {
                if (variant.getValue() != null)
                {
                    oldNamesSet.add(variant.getValue()+variant.getVisibility());
                }
            }

            for (String oldName : oldNamesSet)
            {
                oldNames += oldName;
            }

            rp.setOldNames(oldNames);

            log.debug("End onPostLoad " + RPListener.class);
        }
    }

    /**
     * Compare the initial values of the name fields with the current values, if
     * a change is discovered the namesModifiedTimeStamp field of the
     * ResearcherPage is updated with the current date/timestamp
     */
    public boolean onPreUpdate(PreUpdateEvent event)
    {
        Object object = event.getEntity();
        if (object instanceof ResearcherPage)
        {
        	log.debug("Call onPreUpdate " + RPListener.class);
        	
            ResearcherPage rp = (ResearcherPage) object;

            String newNames = "";
            String oldNames = rp.getOldNames();

            Set<String> newNamesSet = new TreeSet<String>();
            if(rp.getFullName()!=null) {
                newNamesSet.add(rp.getFullName());
            }
            if (rp.getPreferredName().getValue() != null) {
                newNamesSet.add(rp.getPreferredName().getValue()+rp.getPreferredName().getVisibility());
            }
            if (rp.getTranslatedName().getValue() != null)
            {
                newNamesSet.add(rp.getTranslatedName().getValue()+rp.getTranslatedName().getVisibility());
            }
            for (RestrictedField variant : rp.getVariants())
            {
                newNamesSet.add(variant.getValue()+rp.getTranslatedName().getVisibility());
            }

            for (String newName : newNamesSet)
            {
                newNames += newName;
            }
            if (
                 (
                    ((oldNames == null || newNames == null) && 
                            !(oldNames == null && newNames == null)) 
                    || (oldNames != null && newNames != null)
                        && oldNames.hashCode() != newNames.hashCode()
                  )
                )
            {
                int idx = 0;
                boolean found = false;
                for (String propName : event.getPersister().getPropertyNames())
                {
                    if ("namesModifiedTimeStamp".equals(propName))
                    {
                        found = true;
                        break;
                    }
                    idx++;
                }
                if (found)
                {
                    event.getState()[idx] = new SingleTimeStampInfo(new Date());
                }
            }
            log.debug("End onPreUpdate " + RPListener.class);
        }
        return false;
    }

    /**
     * Store in the namesModifiedTimeStamp field of the ResearcherPage the
     * current date/timestamp for every new ResearcherPage.
     */
    public boolean onPreInsert(PreInsertEvent event)
    {
        Object object = event.getEntity();
        if (object instanceof ResearcherPage)
        {
            int idx = 0;
            boolean found = false;
            for (String propName : event.getPersister().getPropertyNames())
            {
                if ("namesModifiedTimeStamp".equals(propName))
                {
                    found = true;
                    break;
                }
                idx++;
            }
            if (found)
            {
                event.getState()[idx] = new SingleTimeStampInfo(new Date());
            }
        }
        return false;
    }
}
