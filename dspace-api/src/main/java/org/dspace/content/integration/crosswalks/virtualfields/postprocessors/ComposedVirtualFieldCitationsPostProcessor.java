/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields.postprocessors;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.dspace.content.Item;
import org.dspace.content.integration.crosswalks.csl.CSLResult;
import org.dspace.core.Context;

/**
 * Implementation of {@link VirtualFieldCitationsPostProcessor} that modify the
 * citation generated via CSL by invoking multiple instances of
 * {@link VirtualFieldCitationsPostProcessor} in cascade. The result of the
 * first post processor is passed to the second processor and so on.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ComposedVirtualFieldCitationsPostProcessor implements VirtualFieldCitationsPostProcessor {

    private String name;

    private List<VirtualFieldCitationsPostProcessor> postProcessors;

    @Override
    public CSLResult process(Context context, Item item, CSLResult cslResult) {

        if (CollectionUtils.isEmpty(postProcessors)) {
            return cslResult;
        }

        CSLResult composedCSLResult = cslResult;
        for (VirtualFieldCitationsPostProcessor postProcessor : postProcessors) {
            composedCSLResult = postProcessor.process(context, item, composedCSLResult);
        }

        return composedCSLResult;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<VirtualFieldCitationsPostProcessor> getPostProcessors() {
        return postProcessors;
    }

    public void setPostProcessors(List<VirtualFieldCitationsPostProcessor> postProcessors) {
        this.postProcessors = postProcessors;
    }

}
