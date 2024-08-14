/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.dspace.app.rest.RestResourceController;
import org.dspace.app.rest.model.BrowseIndexRest;
import org.dspace.app.rest.model.VocabularyRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.springframework.hateoas.Link;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Browse Index Rest HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links and embedded resources
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@RelNameDSpaceResource(BrowseIndexRest.NAME)
public class BrowseIndexResource extends DSpaceResource<BrowseIndexRest> {


    public BrowseIndexResource(BrowseIndexRest bix, Utils utils) {
        super(bix, utils);
        // TODO: the following code will force the embedding of items and
        // entries in the browseIndex we need to find a way to populate the rels
        // array from the request/projection right now it is always null
        // super(bix, utils, "items", "entries");
        if (bix.getBrowseType().equals(BrowseIndexRest.BROWSE_TYPE_VALUE_LIST)) {
            add(utils.linkToSubResource(bix, BrowseIndexRest.LINK_ENTRIES));
            add(utils.linkToSubResource(bix, BrowseIndexRest.LINK_ITEMS));
        }
        if (bix.getBrowseType().equals(BrowseIndexRest.BROWSE_TYPE_FLAT)) {
            add(utils.linkToSubResource(bix, BrowseIndexRest.LINK_ITEMS));
        }
        if (bix.getBrowseType().equals(BrowseIndexRest.BROWSE_TYPE_HIERARCHICAL)) {
            ChoiceAuthorityService choiceAuthorityService =
                ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();
            ChoiceAuthority source = choiceAuthorityService.getChoiceAuthorityByAuthorityName(bix.getVocabulary());
            UriComponentsBuilder baseLink = linkTo(
                methodOn(RestResourceController.class, VocabularyRest.AUTHENTICATION).findRel(
                    null, null, VocabularyRest.CATEGORY, VocabularyRest.PLURAL_NAME,
                    source.getPluginInstanceName(), "", null, null)
            ).toUriComponentsBuilder();

            add(Link.of(baseLink.build().encode().toUriString(), BrowseIndexRest.LINK_VOCABULARY));
        }
    }
}
