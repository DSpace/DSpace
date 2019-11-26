/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import java.util.LinkedList;

import org.atteo.evo.inflector.English;
import org.dspace.app.rest.RestResourceController;
import org.dspace.app.rest.model.AuthorityEntryRest;
import org.dspace.app.rest.model.AuthorityRest;
import org.dspace.app.rest.model.hateoas.AuthorityEntryResource;
import org.dspace.app.rest.utils.AuthorityUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * This class' purpose is to provide a factory to add links to the AuthorityEntryResource. The addLinks factory will
 * be called
 * from the HalLinkService class addLinks method.
 */
@Component
public class AuthorityEntryHalLinkFactory extends HalLinkFactory<AuthorityEntryResource, RestResourceController> {

    protected void addLinks(final AuthorityEntryResource halResource, final Pageable pageable,
                            final LinkedList<Link> list) throws Exception {
        AuthorityEntryRest entry = halResource.getContent();

        if (entry.getOtherInformation() != null) {
            if (entry.getOtherInformation().containsKey(AuthorityUtils.RESERVED_KEYMAP_PARENT)) {
                UriComponentsBuilder uriComponentsBuilder = linkTo(
                    getMethodOn(AuthorityRest.CATEGORY, AuthorityRest.NAME)
                        .findRel(null, null, AuthorityRest.CATEGORY,
                                 English.plural(AuthorityRest.NAME),
                                 entry.getAuthorityName() + "/" + AuthorityRest.ENTRY,
                                 entry.getOtherInformation().get(AuthorityUtils.RESERVED_KEYMAP_PARENT), null, null))
                        .toUriComponentsBuilder();

                list.add(buildLink(AuthorityUtils.RESERVED_KEYMAP_PARENT, uriComponentsBuilder.build().toString()));
            }
        }
        String selfLinkString = linkTo(
            getMethodOn().findOne(entry.getCategory(), English.plural(entry.getType()), entry.getAuthorityName()))
            .toUriComponentsBuilder().build().toString() + "/entryValues/" + entry.getId();
        list.add(buildLink(Link.REL_SELF, selfLinkString));
    }

    protected Class<RestResourceController> getControllerClass() {
        return RestResourceController.class;
    }

    protected Class<AuthorityEntryResource> getResourceClass() {
        return AuthorityEntryResource.class;
    }

}

