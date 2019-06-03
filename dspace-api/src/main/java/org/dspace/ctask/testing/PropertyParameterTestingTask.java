/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.ctask.testing;

import java.io.IOException;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs what it was asked to do, samples run parameters and task
 * properties, making no changes to the repository.  For testing.
 *
 * <p>
 * To test parameters and properties, set a value for the run parameter
 * "runParameter" or the task property "taskProperty".
 *
 * <p>
 * This is a debugging tool, NOT a regression test.
 *
 * @author mhwood
 */
public class PropertyParameterTestingTask
        extends AbstractCurationTask {
    private static final Logger LOG
            = LoggerFactory.getLogger(PropertyParameterTestingTask.class);

    @Override
    public void init(Curator curator, String taskId)
            throws IOException {
        super.init(curator, taskId);
        LOG.info("Received 'init' on task {}", taskId);
        // Display some properties.
        LOG.info("taskProperty = '{}'; runParameter = '{}'",
                taskProperty("taskProperty"), taskProperty("runParameter"));
    }

    @Override
    public int perform(DSpaceObject dso)
            throws IOException {
        LOG.info("Received 'perform' on {}", dso);
        return Curator.CURATE_SUCCESS;
    }

    @Override
    public int perform(Context ctx, String id)
            throws IOException {
        LOG.info("Received 'perform' on object ID {}", id);
        return Curator.CURATE_SUCCESS;
    }
}
