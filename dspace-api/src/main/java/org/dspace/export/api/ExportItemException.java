/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.export.api;

/**
 * 
 * @author João Melo <jmelo@lyncode.com>
 * @version $Revision$
 */
public class ExportItemException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 952381479430728466L;

	public ExportItemException() {
	}

	public ExportItemException(String arg0) {
		super(arg0);
	}

	public ExportItemException(Throwable arg0) {
		super(arg0);
	}

	public ExportItemException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

}
