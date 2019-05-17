/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.builder;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.EPerson;

public class ResourcePolicyBuilder extends AbstractBuilder<ResourcePolicy, ResourcePolicyService> {

    /* Log4j logger*/
    private static final Logger log = Logger.getLogger(ResourcePolicyBuilder.class);

    private ResourcePolicy resourcePolicy;

    protected ResourcePolicyBuilder(Context context) {
        super(context);
    }

    @Override
    protected ResourcePolicyService getService() {
        return resourcePolicyService;
    }

    @Override
    public void cleanup() throws Exception {
        delete(resourcePolicy);
    }

    @Override
    public ResourcePolicy build() {
        try {

            resourcePolicyService.update(context, resourcePolicy);
            context.dispatchEvents();

            indexingService.commit();
        } catch (SearchServiceException e) {
            log.error(e);
        } catch (SQLException e) {
            log.error(e);
        } catch (AuthorizeException e) {
            log.error(e);
            ;
        }
        return resourcePolicy;
    }

    public void delete(ResourcePolicy rp) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            ResourcePolicy attachedDso = c.reloadEntity(rp);
            if (attachedDso != null) {
                getService().delete(c, attachedDso);
            }
            c.complete();
        }

        indexingService.commit();
    }


    public static ResourcePolicyBuilder createResourcePolicy(Context context)
            throws SQLException, AuthorizeException {
        ResourcePolicyBuilder resourcePolicyBuilder = new ResourcePolicyBuilder(context);
        return resourcePolicyBuilder.create(context);
    }

    private ResourcePolicyBuilder create(Context context)
            throws SQLException, AuthorizeException {
        this.context = context;

        resourcePolicy = resourcePolicyService.create(context);

        return this;
    }

    public ResourcePolicyBuilder withUser(EPerson ePerson) throws SQLException {
        resourcePolicy.setEPerson(ePerson);
        return this;
    }
    public ResourcePolicyBuilder withAction(int action) throws SQLException {
        resourcePolicy.setAction(action);
        return this;
    }
    public ResourcePolicyBuilder withDspaceObject(DSpaceObject dspaceObject) throws SQLException {
        resourcePolicy.setdSpaceObject(dspaceObject);
        return this;
    }

}
