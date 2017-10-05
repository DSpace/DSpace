/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.Serializable;

import org.springframework.hateoas.ResourceSupport;

/**
 * This is the interface for Link Repositories.
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public interface LinkRestRepository<L extends Serializable> {
	public abstract ResourceSupport wrapResource(L model, String... rels);

	public default boolean isEmbeddableRelation(Object data, String name) {
		return true;
	}
}
