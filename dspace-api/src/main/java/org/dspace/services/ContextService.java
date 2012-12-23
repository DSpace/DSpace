package org.dspace.services;

import org.dspace.core.Context;

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
