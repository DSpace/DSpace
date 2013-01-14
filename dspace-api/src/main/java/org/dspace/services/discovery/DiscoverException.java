/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.discovery;

public class DiscoverException extends Exception {
	private static final long serialVersionUID = 695592737552892991L;

	public DiscoverException() {
		super();
	}

	public DiscoverException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public DiscoverException(String message, Throwable cause) {
		super(message, cause);
	}

	public DiscoverException(String message) {
		super(message);
	}

	public DiscoverException(Throwable cause) {
		super(cause);
	}
	
	
}
