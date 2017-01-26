package org.dspace.app.rest.model.hateoas;

import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class DSpaceResource<T> extends ResourceSupport {
	@JsonUnwrapped
	private final T data;

	public DSpaceResource(T data) {
		// String username = bookmark.getAccount().getUsername();
		this.data = data;
		// this.add(new Link(bookmark.getUri(), "bookmark-uri"));
		// this.add(linkTo(BookmarkRestController.class,
		// username).withRel("bookmarks"));
		// this.add(linkTo(methodOn(BookmarkRestController.class, username)
		// .readBookmark(username, bookmark.getId())).withSelfRel());
	}

	public T getData() {
		return data;
	}
}
