/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing;

import java.io.Serializable;

/**
 * This simple class just provides the Namespace datatype. It stores a URI that
 * is publicly accessible. The actually definitions of namespaces is found in
 * WingConstants.java.
 * 
 * @author Scott Phillips
 */

public class Namespace implements Serializable
{
    /** The URI for this namespace */
    public final String URI;

    /**
     * Construct a new Namespace for the given URI.
     * 
     * @param URI
     *            The namespaces URI
     */
    public Namespace(String URI)
    {
        this.URI = URI;
    }
}
