/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import org.atteo.evo.inflector.English;
import org.dspace.app.rest.RestResourceController;
import org.dspace.app.rest.model.AuthorityEntryRest;
import org.dspace.app.rest.model.AuthorityRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.AuthorityUtils;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Authority Rest HAL Resource. The HAL Resource wraps the REST Resource adding
 * support for the links and embedded resources
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
@RelNameDSpaceResource(AuthorityEntryRest.NAME)
public class AuthorityEntryResource extends ResourceSupport {

	@JsonUnwrapped
	private final AuthorityEntryRest data;

	public AuthorityEntryResource(AuthorityEntryRest entry) {
		this.data = entry;
		if (entry.getOtherInformation() != null) {
			if (entry.getOtherInformation().containsKey(AuthorityUtils.RESERVED_KEYMAP_PARENT)) {
				RestResourceController methodOn = methodOn(RestResourceController.class, AuthorityRest.CATEGORY,
						AuthorityRest.NAME);
				UriComponentsBuilder uriComponentsBuilder = linkTo(methodOn.findRel(null, AuthorityRest.CATEGORY,
						English.plural(AuthorityRest.NAME), entry.getAuthorityName() + "/" + AuthorityRest.ENTRY,
						entry.getOtherInformation().get(AuthorityUtils.RESERVED_KEYMAP_PARENT), null, null, null)).toUriComponentsBuilder();
				Link link = new Link(uriComponentsBuilder.build().toString(), AuthorityUtils.RESERVED_KEYMAP_PARENT);
				add(link);
			}
		}
	}

	public AuthorityEntryRest getData() {
		return data;
	}

}
