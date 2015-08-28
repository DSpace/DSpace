/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;

import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.KernelStartupCallbackService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Callback method to ensure that the default groups are created each time a kernel is created.
 *
 * @author kevinvandevelde at atmire.com
 */
public class GroupServiceInitializer implements KernelStartupCallbackService {

    private final Logger log = Logger.getLogger(GroupServiceInitializer.class);

    @Autowired(required = true)
    protected GroupService groupService;

    @Override
    public void executeCallback() {
        // After every migrate, ensure default Groups are setup correctly.
        Context context = null;
        try
        {
            context = new Context();
            context.turnOffAuthorisationSystem();
            // While it's not really a formal "registry", we need to ensure the
            // default, required Groups exist in the DSpace database
            groupService.initDefaultGroupNames(context);
            context.restoreAuthSystemState();
            // Commit changes and close context
            context.complete();
        }
        catch(Exception e)
        {
            log.error("Error attempting to add/update default DSpace Groups", e);
            throw new RuntimeException(e);
        }
        finally
        {
            // Clean up our context, if it still exists & it was never completed
            if(context!=null && context.isValid())
                context.abort();
        }

    }
}
