/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.builder;

import org.apache.log4j.Logger;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.MutablePeriod;
import org.joda.time.format.PeriodFormat;

import java.util.Date;

/**
 * Abstract builder to construct DSpace Objects
 *
 * @author Atmire NV (info at atmire dot com)
 */
public abstract class AbstractDSpaceObjectBuilder<T extends DSpaceObject> extends AbstractBuilder<T, DSpaceObjectService> {

    /* Log4j logger*/
    private static final Logger log =  Logger.getLogger(AbstractDSpaceObjectBuilder.class);

    protected AbstractDSpaceObjectBuilder(Context context){
        super(context);
        this.context = context;
    }

    protected abstract void cleanup() throws Exception;


    protected abstract DSpaceObjectService<T> getService();


    protected <B> B handleException(final Exception e) {
        log.error(e.getMessage(), e);
        return null;
    }


    protected <B extends AbstractDSpaceObjectBuilder<T>> B addMetadataValue(final T dso, final String schema, final String element, final String qualifier, final String value) {
        try {
            getService().addMetadata(context, dso, schema, element, qualifier, Item.ANY, value);
        } catch (Exception e) {
            return handleException(e);
        }
        return (B) this;
    }

    protected <B extends AbstractDSpaceObjectBuilder<T>> B setMetadataSingleValue(final T dso, final String schema, final String element, final String qualifier, final String value) {
        try {
            getService().setMetadataSingleValue(context, dso, schema, element, qualifier, Item.ANY, value);
        } catch (Exception e) {
            return handleException(e);
        }

        return (B) this;
    }

    protected <B extends AbstractDSpaceObjectBuilder<T>> B setEmbargo(String embargoPeriod, DSpaceObject dso) {
        // add policy just for anonymous
        try {
            MutablePeriod period = PeriodFormat.getDefault().parseMutablePeriod(embargoPeriod);
            Date embargoDate = DateTime.now(DateTimeZone.UTC).plus(period).toDate();

            return setOnlyReadPermission(dso, groupService.findByName(context, Group.ANONYMOUS), embargoDate);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    protected <B extends AbstractDSpaceObjectBuilder<T>> B setOnlyReadPermission(DSpaceObject dso, Group group, Date startDate) {
        // add policy just for anonymous
        try {
            authorizeService.removeAllPolicies(context, dso);

            ResourcePolicy rp = authorizeService.createOrModifyPolicy(null, context, null, group,
                    null, startDate, Constants.READ, "Integration Test", dso);
            if (rp != null) {
                resourcePolicyService.update(context, rp);
            }
        } catch (Exception e) {
            return handleException(e);
        }
        return (B) this;
    }

    public abstract T build();

    public void delete(T dso) throws Exception {

        try(Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            T attachedDso = c.reloadEntity(dso);
            if (attachedDso != null) {
                getService().delete(c, attachedDso);
            }
            c.complete();
        }

        indexingService.commit();
    }
}
