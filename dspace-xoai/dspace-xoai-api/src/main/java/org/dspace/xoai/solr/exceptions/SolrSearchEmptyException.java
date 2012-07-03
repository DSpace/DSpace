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
public class SolrSearchEmptyException extends Exception
{

    /**
     * Creates a new instance of <code>SolrSearchEmptyException</code> without
     * detail message.
     */
    public SolrSearchEmptyException()
    {
    }

    /**
     * Constructs an instance of <code>SolrSearchEmptyException</code> with the
     * specified detail message.
     * 
     * @param msg
     *            the detail message.
     */
    public SolrSearchEmptyException(String msg)
    {
        super(msg);
    }

    public SolrSearchEmptyException(String msg, Throwable t)
    {
        super(msg, t);
    }
}
