/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields.postprocessors;

import org.dspace.content.Item;
import org.dspace.content.integration.crosswalks.csl.CSLResult;
import org.dspace.content.integration.crosswalks.virtualfields.VirtualFieldCitations;
import org.dspace.core.Context;

/**
 * Interface for classes that can post process the citations generated via CSL
 * in {@link VirtualFieldCitations}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface VirtualFieldCitationsPostProcessor {

    /**
     * Returns the name of the post processor. To use a specific post processor, its
     * name must be set on the virtual field (see {@link VirtualFieldCitations} for
     * more details).
     *
     * @return the processor's name
     */
    public String getName();

    /**
     * Process the given CSL result returning a new instance of the same type.
     *
     * @param  context   the DSpace context
     * @param  item      the source item
     * @param  cslResult the citations generated with the CSL engine
     * @return           a modified CSLResult
     */
    public CSLResult process(Context context, Item item, CSLResult cslResult);
}
