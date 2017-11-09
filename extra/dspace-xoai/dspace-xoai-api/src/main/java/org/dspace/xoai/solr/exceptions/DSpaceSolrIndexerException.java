/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.xoai.solr.exceptions;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
@SuppressWarnings("serial")
public class DSpaceSolrIndexerException extends Exception
{
    /**
     * Creates a new instance of <code>DSpaceSolrException</code> without detail
     * message.
     */
    public DSpaceSolrIndexerException()
    {
    }

    /**
     * Constructs an instance of <code>DSpaceSolrException</code> with the
     * specified detail message.
     * 
     * @param msg
     *            the detail message.
     */
    public DSpaceSolrIndexerException(String msg)
    {
        super(msg);
    }

    public DSpaceSolrIndexerException(String msg, Throwable t)
    {
        super(msg, t);
    }
}
