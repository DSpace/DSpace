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
import org.dspace.core.Context;

/**
 * ScriptedTask describes a rather generic ability to perform an operation
 * upon a DSpace object. It's semantics are identical to the CurationTask interface,
 * but is designed to be implemented in scripting languages, rather than
 * Java. For this reason, the 'perform' methods are renamed to accomodate
 * languages (like Ruby) that lack method overloading.
 *
 * @author richardrodgers
 */
public interface ScriptedTask
{
    /**
     * Initialize task - parameters inform the task of it's invoking curator.
     * Since the curator can provide services to the task, this represents
     * curation DI.
     * 
     * @param curator the Curator controlling this task
     * @param taskId identifier task should use in invoking services
     * @throws IOException if IO error
     */
    public void init(Curator curator, String taskId) throws IOException;

    /**
     * Perform the curation task upon passed DSO
     *
     * @param dso the DSpace object
     * @return status code
     * @throws IOException if IO error
     */
    public int performDso(DSpaceObject dso) throws IOException;

    /**
     * Perform the curation task for passed id
     * 
     * @param ctx DSpace context object
     * @param id persistent ID for DSpace object
     * @return status code
     * @throws IOException if IO error
     */
    public int performId(Context ctx, String id) throws IOException;
}
