/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services;

import org.dspace.core.Context;

/**
 * @author João Melo <jmelo@lyncode.com>
 */
public interface ContextService {
	/**
	 * Get's the current DSpace context (current thread or request)
	 * If no context is set to the current thread, creates a new one.
	 * 
	 * @return DSpace Context
	 */
	Context getContext();
	
	/**
	 * Creates a new DSpace Context.
	 * 
	 * @return DSpace Context
	 */
	Context newContext();
}
