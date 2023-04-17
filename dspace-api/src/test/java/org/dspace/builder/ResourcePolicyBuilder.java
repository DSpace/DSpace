/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

public class ResourcePolicyBuilder extends AbstractBuilder<ResourcePolicy, ResourcePolicyService> {

    /* Log4j logger*/
    private static final Logger log = LogManager.getLogger();

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
        try (Context c = new Context()) {
            c.setDispatcher("noindex");
            c.turnOffAuthorisationSystem();
            // Ensure object and any related objects are reloaded before checking to see what needs cleanup
            resourcePolicy = c.reloadEntity(resourcePolicy);
            if (resourcePolicy != null) {
                delete(c, resourcePolicy);
            }
            c.complete();
            indexingService.commit();
        }
    }

    @Override
    public void delete(Context c, ResourcePolicy dso) throws Exception {
        if (dso != null) {
            getService().delete(c, dso);
        }
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
            c.setDispatcher("noindex");
            ResourcePolicy attachedDso = c.reloadEntity(rp);
            if (attachedDso != null) {
                getService().delete(c, attachedDso);
            }
            c.complete();
        }

        indexingService.commit();
    }

    public static void delete(Integer id)
            throws SQLException, IOException, SearchServiceException {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            ResourcePolicy rp = resourcePolicyService.find(c, id);
            if (rp != null) {
                try {
                    resourcePolicyService.delete(c, rp);
                } catch (AuthorizeException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
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

    public ResourcePolicyBuilder withGroup(Group epersonGroup) throws SQLException {
        resourcePolicy.setGroup(epersonGroup);
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

    public ResourcePolicyBuilder withPolicyType(String policyType) {
        resourcePolicy.setRpType(policyType);
        return this;
    }

    public ResourcePolicyBuilder withStartDate(Date data) throws SQLException {
        resourcePolicy.setStartDate(data);
        return this;
    }

    public ResourcePolicyBuilder withEndDate(Date data) throws SQLException {
        resourcePolicy.setEndDate(data);
        return this;
    }

    public ResourcePolicyBuilder withDescription(String description) throws SQLException {
        resourcePolicy.setRpDescription(description);
        return this;
    }

    public ResourcePolicyBuilder withName(String name) throws SQLException {
        resourcePolicy.setRpName(name);
        return this;
    }
}
