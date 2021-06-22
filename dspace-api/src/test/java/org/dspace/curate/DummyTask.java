/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.io.IOException;

import org.dspace.content.DSpaceObject;

/**
 * Makes no model changes, but records certain property values for inspection.
 */
public class DummyTask
    extends AbstractCurationTask {
    public DummyTask() {
        // This constructor intentionally left blank.
    }

    @Override
    public int perform(DSpaceObject dso)
            throws IOException {
        CuratorTest.runParameter = taskProperty(CuratorTest.RUN_PARAMETER_NAME);
        CuratorTest.taskProperty = taskProperty(CuratorTest.TASK_PROPERTY_NAME);
        return Curator.CURATE_SUCCESS;
    }
}
