/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.service;


import java.util.List;

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisSubscription;
import org.dspace.eperson.EPerson;

public class CrisSubscribeService
{
    private ApplicationService applicationService;
       
        
    public CrisSubscribeService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }
    
    public void unsubscribe(EPerson e, String uuid)
    {        
        CrisSubscription rpsub = applicationService.getSubscription(e.getID(),
                uuid);
        if (rpsub != null)
        {
            applicationService.delete(CrisSubscription.class, rpsub.getId());
        }
    }
    
    public void subscribe(EPerson e, String uuid)
    {
        
        CrisSubscription rpsub = applicationService.getSubscription(e.getID(),
                uuid);
        if (rpsub == null)
        {
            rpsub = new CrisSubscription();
            rpsub.setEpersonID(e.getID());
            ACrisObject aco = applicationService.getEntityByUUID(uuid);
            rpsub.setTypeDef(aco.getType());
            rpsub.setUuid(uuid);
            applicationService.saveOrUpdate(CrisSubscription.class, rpsub);
        }
    }
    
    public boolean isSubscribed(EPerson e, ACrisObject rp)
    {
        if (e == null)
            return false;
        return applicationService.getSubscription(e.getID(), rp.getUuid()) != null;
    }
    
    public void clearAll(EPerson e)
    {
        applicationService.deleteSubscriptionByEPersonID(e.getID());
    }
    
    public List<String> getSubscriptions(EPerson e)
    {
        return applicationService.getCrisSubscriptionsByEPersonID(e.getID());
    }

}
