/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * This is the main entry point of the new REST API. Its responsibility is to
 * provide a consistent behaviors for all the exposed resources in terms of
 * returned HTTP codes, endpoint URLs, HTTP verbs to methods translation, etc.
 * It delegates to the repository the business logic
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@RestController
@RequestMapping("/api")
public class RootRestResourceController {

	@Autowired
	DiscoverableEndpointsService discoverableEndpointsService;

	@RequestMapping(method = RequestMethod.GET)
	public ResourceSupport listDefinedEndpoint(HttpServletRequest request) {
		ResourceSupport root = new ResourceSupport();
		String restURL = getRestURL(request);

		for (Link l : discoverableEndpointsService.getDiscoverableEndpoints()) {
			root.add(new Link(restURL + l.getHref(), l.getRel()));
		}

		return root;
	}

	private String getRestURL(HttpServletRequest request) {
		String url = request.getRequestURL().toString();
		return url.substring(0, url.length() - request.getRequestURI().length()) + request.getContextPath();
	}
}