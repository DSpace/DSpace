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

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.dspace.app.rest.RestResourceController;
import org.dspace.app.rest.model.BrowseEntryRest;
import org.dspace.app.rest.model.BrowseIndexRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Browse Entry Rest HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links and embedded resources
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@RelNameDSpaceResource(BrowseEntryRest.NAME)
public class BrowseEntryResource extends ResourceSupport {

	@JsonUnwrapped
	private final BrowseEntryRest data;

	public BrowseEntryRest getData() {
		return data;
	}

	public BrowseEntryResource(BrowseEntryRest entry) {
		this.data = entry;
		BrowseIndexRest bix = entry.getBrowseIndex();
		RestResourceController methodOn = methodOn(RestResourceController.class, bix.getCategory(), bix.getType());
		UriComponentsBuilder uriComponentsBuilder = linkTo(methodOn
				.findRel(null, bix.getCategory(), bix.getTypePlural(), bix.getId(), BrowseIndexRest.ITEMS, null, null, null))
				.toUriComponentsBuilder();
		Link link = new Link(addFilterParams(uriComponentsBuilder).build().toString(), BrowseIndexRest.ITEMS);
		add(link);
	}

	// TODO use the reflection to discover the link repository and additional information on the link annotation to build the parameters? 
	private UriComponentsBuilder addFilterParams(UriComponentsBuilder uriComponentsBuilder) {
		UriComponentsBuilder result;
		if (data.getAuthority() != null) {
			result = uriComponentsBuilder.queryParam("filterValue", data.getAuthority());
		}
		else {
			result = uriComponentsBuilder.queryParam("filterValue", data.getValue());
		}
		return result;
	}

}
