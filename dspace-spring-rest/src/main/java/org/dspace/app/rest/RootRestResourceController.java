/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

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
		for (Link l : discoverableEndpointsService.getDiscoverableEndpoints()) {
			root.add(new Link(getRestURL(request) + l.getHref(), l.getRel()));
		}
		return root;
	}

	private StringBuffer getRestURL(HttpServletRequest request) {
		StringBuffer url = new StringBuffer();
		String scheme = request.getScheme();
		int port = request.getServerPort();
		if (port < 0) {
			// Work around java.net.URL bug
			port = 80;
		}

		url.append(scheme);
		url.append("://");
		url.append(request.getServerName());
		if ((scheme.equals("http") && (port != 80))
				|| (scheme.equals("https") && (port != 443))) {
			url.append(':');
			url.append(port);
		}
		url.append(request.getContextPath());

		return url;
	}
}