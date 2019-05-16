/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.builder;

import org.dspace.content.Site;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;

public class SiteBuilder extends AbstractDSpaceObjectBuilder<Site> {

    private Site site;

    protected SiteBuilder(Context context) {
        super(context);
    }

    @Override
    public void cleanup() throws Exception {
        //Do nothing
    }

    @Override
    protected DSpaceObjectService<Site> getService() {
        return siteService;
    }

    @Override
    public Site build() {
        try {
            siteService.update(context, site);

            context.dispatchEvents();

            indexingService.commit();
            return site;
        } catch (Exception e) {
            return handleException(e);
        }
    }

    public static SiteBuilder createSite(final Context context) {
        SiteBuilder builder = new SiteBuilder(context);
        return builder.create(context);
    }

    private SiteBuilder create(final Context context) {
        this.context = context;

        try {
            site = siteService.createSite(context);
        } catch (Exception e) {
            return handleException(e);
        }

        return this;
    }
}
