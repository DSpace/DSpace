/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

/**
 * @author Andrea Schweer schweer@waikato.ac.nz for the LCoNZ Institutional Research Repositories
 */
public class SolrImportExportException extends Exception
{
	public SolrImportExportException(String message)
	{
		super(message);
	}

	public SolrImportExportException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
