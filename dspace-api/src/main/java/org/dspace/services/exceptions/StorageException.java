/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.services.exceptions;

import java.io.IOException;

/**
 * @author João Melo <jmelo@lyncode.com>
 */
public class StorageException extends IOException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public StorageException() {
		
	}

	/**
	 * @param arg0
	 */
	public StorageException(String arg0) {
		super(arg0);
		
	}

	/**
	 * @param arg0
	 */
	public StorageException(Throwable arg0) {
		super(arg0);
		
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public StorageException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		
	}

}
