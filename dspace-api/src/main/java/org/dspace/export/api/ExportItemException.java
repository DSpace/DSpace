/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.export.api;

/**
 * This class declares basic exceptions which should be used while 
 * handling the exportation of item's citation in common formats like RIS and BibTeX.
 * 
 * @author João Melo <jmelo@lyncode.com>
 * 
 */
public class ExportItemException extends Exception 
{

    public ExportItemException() 
    {
        super();
    }

    public ExportItemException(String message)
    {
        super(message);
    }

    public ExportItemException(Throwable cause) 
    {
        super(cause);
    }

    public ExportItemException(String message, Throwable cause) 
    {
        super(message, cause);
    }

}
