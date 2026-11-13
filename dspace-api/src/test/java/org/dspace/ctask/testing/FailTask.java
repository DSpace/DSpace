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
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

/**
 * Curation task that always fails on Items, so that we can test what happens
 * when a curation task fails.
 */
public class FailTask
        extends AbstractCurationTask {
    @Override
    public int perform(Context context, DSpaceObject dso)
            throws IOException {
        if (dso instanceof Item) {
            setResult(String.format("Item %s failed.", dso.getID().toString()));
            return Curator.CURATE_FAIL;
        } else {
            return Curator.CURATE_SKIP;
        }
    }
}
