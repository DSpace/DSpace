/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;

/**
 * Implementation of {@link DSpaceRunnable} to update metrics field in Solr document
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class UpdateCrisMetricsInSolrDoc extends
             DSpaceRunnable<UpdateCrisMetricsInSolrDocScriptConfiguration<UpdateCrisMetricsInSolrDoc>> {

    private static final Logger log = LogManager.getLogger(UpdateCrisMetricsInSolrDoc.class);

    private UpdateCrisMetricsInSolrDocService updateCrisMetricsInSolrDocService;

    protected Context context;

    @Override
    public void setup() throws ParseException {
        updateCrisMetricsInSolrDocService = new DSpace().getServiceManager().getServiceByName(
                UpdateCrisMetricsInSolrDocService.class.getName(), UpdateCrisMetricsInSolrDocService.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public UpdateCrisMetricsInSolrDocScriptConfiguration<UpdateCrisMetricsInSolrDoc> getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("update-metrics-in-solr",
                               UpdateCrisMetricsInSolrDocScriptConfiguration.class);
    }

    @Override
    public void internalRun() throws Exception {
        assignCurrentUserInContext();
        assignSpecialGroupsInContext();
        try {
            updateCrisMetricsInSolrDocService.performUpdate(context, handler, commandLine.hasOption("o"));
            context.complete();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            handler.handleException(e);
            context.abort();
        }
    }

    protected void assignCurrentUserInContext() throws SQLException {
        context = new Context();
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            context.setCurrentUser(ePerson);
        }
    }

    private void assignSpecialGroupsInContext() throws SQLException {
        for (UUID uuid : handler.getSpecialGroups()) {
            context.setSpecialGroup(uuid);
        }
    }

}