/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.hateoas.Link;
import org.springframework.stereotype.Service;

/**
 * A Service able to list all the discoverable endpoints in our REST
 * application. Endpoints need to register their managed endpoints. The service
 * is responsible to check conflict and priorities
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Tim Donohue
 */
@Service
public class DiscoverableEndpointsService {
	/**
	 * Contains all the registeredEndpoints as received by the single controller
	 */
	private Map<Object, List<Link>> registeredEndpoints = new HashMap<Object, List<Link>>();

	/**
	 * Is the computed list of discoverableEndpoints valid?
	 */
	private boolean initialized = false;

	private List<Link> discoverableEndpoints = new ArrayList<Link>();

	public void register(Object controller, List<Link> links) {
		synchronized (this) {
			initialized = false;
			registeredEndpoints.put(controller, links);
		}
	}

	public void unregister(Object controller) {
		synchronized (this) {
			initialized = false;
			registeredEndpoints.remove(controller);
		}
	}

	public List<Link> getDiscoverableEndpoints() {
		synchronized (this) {
			if (initialized)
				return discoverableEndpoints;

			discoverableEndpoints.clear();
			Set<String> rels = new HashSet<String>();
			for (Entry<Object, List<Link>> controller : registeredEndpoints.entrySet()) {
				for (Link link : controller.getValue()) {
					if (isLinkValid(controller.getKey(), link.getHref())) {
						discoverableEndpoints.add(link);
						// sanity check
						// FIXME improve logging for debugging
						if (rels.contains(link.getRel())) {
							throw new IllegalStateException("The rel " + link.getRel() + " is defined multiple times!");
						}
						rels.add(link.getRel());
					}
				}
			}
			initialized = true;

			return discoverableEndpoints;
		}
	}

	private boolean isLinkValid(Object controller, String href) {
		// FIXME we need to implement a check to be sure that there are no other
		// controller with an highter precedence mapped on the same URL (this
		// could be used to override default implementation)
		return true;
	}
}