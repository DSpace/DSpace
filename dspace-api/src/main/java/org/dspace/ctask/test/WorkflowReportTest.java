/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.ctask.test;

import java.io.IOException;

import org.dspace.content.DSpaceObject;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Curation task which simply reports its invocation without changing anything.
 * Meant for testing.
 *
 * @author mhwood
 */
public class WorkflowReportTest
        extends AbstractCurationTask {
    private static final Logger LOG = LoggerFactory.getLogger(WorkflowReportTest.class);

    @Override
    public int perform(DSpaceObject dso)
            throws IOException {
        LOG.info("Class {} as task {} received 'perform' for object {}",
                WorkflowReportTest.class.getSimpleName(), taskId, dso);
        curator.report(String.format(
                "Class %s as task %s received 'perform' for object %s%n",
                WorkflowReportTest.class.getSimpleName(), taskId, dso));
        return Curator.CURATE_SUCCESS;
    }
}
