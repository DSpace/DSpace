/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.exception;

/**
 * This is an exception to manage information about pagination errors.
 * Out-of-order or other invalid requests
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class PaginationException extends RuntimeException {
	long total;

	public PaginationException(long total) {
		this.total = total;
	}

	public long getTotal() {
		return total;
	}
}
