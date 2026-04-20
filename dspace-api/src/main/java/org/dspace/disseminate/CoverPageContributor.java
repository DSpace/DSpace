/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.disseminate;

import java.util.Map;

import org.dspace.content.Item;

/**
 * This interface allows users to provide custom implementations for modifying or adding parameters
 * that will be used when generating cover pages.
 * Implementers of this interface can modify or add parameters based on the given {@link Item}.
 * These parameters will be processed and passed to the rendering engine, allowing users to customize
 * the cover page generation.
 */
public interface CoverPageContributor {

    /**
     * Processes and potentially modifies or adds parameters related to cover page generation.
     * Implementers can use the provided {@link Item} object and an existing map of parameters
     * to modify or add new key-value pairs. The resulting set of parameters will be passed on to
     * the rendering step.
     *
     * @param item The {@link Item} that the coverpage is rendered for.
     * @param parameters A map of existing parameters that are already set for the cover page.
     *
     * @return A map containing the final set of parameters after modification. This map
     *         will be passed to the rendering step for cover page generation.
     */
    Map<String, String> processCoverPageParams(Item item, Map<String, String> parameters);
}
