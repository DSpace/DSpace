package org.dspace.app.rest.utils;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import java.util.List;

import org.dspace.app.rest.exception.PaginationException;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;

public class Utils {

	public static <T> Page<T> getPage(List<T> fullContents, Pageable pageable) {
		int total = fullContents.size();
		List<T> pageContent = null;
		if (pageable.getOffset() > total) {
			throw new PaginationException(total);
		}
		else {
			if (pageable.getOffset()+pageable.getPageSize() > total) {
				pageContent = fullContents.subList(pageable.getOffset(), total);
			}
			else {
				pageContent = fullContents.subList(pageable.getOffset(), pageable.getOffset() + pageable.getPageSize());
			}
			return new PageImpl<T>(pageContent, pageable, total);
		}
	}

	public static Link linkToSingleResource(DSpaceResource r, String rel) {
		RestModel data = r.getData();
		return linkToSingleResource(data, rel);
	}
	
	public static Link linkToSingleResource(RestModel data, String rel) {
		return linkTo(data.getController(), data.getType()).slash(data).withRel(rel);
	}

	public static Link linkToSubResource(RestModel data, String rel) {
		return linkTo(data.getController(), data.getType()).slash(data).slash(rel).withRel(rel);
	}
}
