/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.projection;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.exception.MissingParameterException;
import org.dspace.app.rest.model.LinkRest;
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.hateoas.HALResource;
import org.dspace.services.RequestService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;

/**
 * This Projection will allow us to specify how many levels deep we're going to embed resources onto the requested
 * HalResource.
 * The projection is used by using the name combined with the embedLevelDepth parameter to specify how deep the embeds
 * have to go. There is an upper limit in place for this, which is specified on the bean through the maxEmbed property.
 */
public class SpecificLevelProjection extends AbstractProjection {

    @Autowired
    private RequestService requestService;

    public final static String NAME = "level";

    private int maxEmbed = DSpaceServicesFactory.getInstance().getConfigurationService()
            .getIntProperty("rest.projections.full.max", 2);

    public int getMaxEmbed() {
        return maxEmbed;
    }

    public void setMaxEmbed(int maxEmbed) {
        this.maxEmbed = maxEmbed;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean allowEmbedding(HALResource<? extends RestAddressableModel> halResource, LinkRest linkRest,
                                  Link... oldLinks) {
        String embedLevelDepthString = requestService.getCurrentRequest().getHttpServletRequest()
                                                .getParameter("embedLevelDepth");
        if (StringUtils.isBlank(embedLevelDepthString)) {
            throw new MissingParameterException("The embedLevelDepth parameter needs to be specified" +
                                                   " for this Projection");
        }
        int embedLevelDepth = Integer.parseInt(embedLevelDepthString);
        if (embedLevelDepth > maxEmbed) {
            throw new IllegalArgumentException("The embedLevelDepth may not exceed the configured max: " + maxEmbed);
        }
        return halResource.getContent().getEmbedLevel() < embedLevelDepth;
    }

    @Override
    public boolean allowLinking(HALResource halResource, LinkRest linkRest) {
        return true;
    }
}
