/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.paymentsystem;

import org.dspace.content.DSpaceObject;

import java.util.List;

/**
 * PaymentService provides an interface for the DSpace application to
 * interact with the Payment Service implementation.
 *
 * @author Mark Diggory, mdiggory at atmire.com
 * @author Fabio Bolognesi, fabio at atmire.com
 * @author Lantian Gai, lantian at atmire.com
 */
public class PaymentSystemException extends Exception{
    private List<String> myTableList; //set of tables in which EPerson exists

    /**
     * Create an empty EPersonDeletionException
     */
    public PaymentSystemException()
    {
        super();
        myTableList = null;
    }
    public PaymentSystemException(List<String> tableList)
    {
        super();
        myTableList = tableList;
    }

    /**
     * Return the list of offending tables.
     *
     * @return The tables in which the eperson ID exists.
     */
    public List<String> getTables()
    {
        return myTableList;
    }

    /**
     * Create an authorize exception with a message
     *
     * @param message
     *            the message
     */
    public PaymentSystemException(String message)
    {
        super(message);
    }
}
