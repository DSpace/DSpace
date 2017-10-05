package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.AuthorityEntryRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

@RelNameDSpaceResource(AuthorityEntryRest.NAME)
public class AuthorityEntryResource extends ResourceSupport {
	
	@JsonUnwrapped
	private final AuthorityEntryRest data;
	
	public AuthorityEntryResource(AuthorityEntryRest entry) {
		this.data = entry;
	}
	
	public AuthorityEntryRest getData() {
		return data;
	}
}
