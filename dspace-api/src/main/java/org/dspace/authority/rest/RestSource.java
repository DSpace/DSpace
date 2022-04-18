/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.rest;

import org.dspace.authority.SolrAuthorityInterface;
<<<<<<< HEAD
=======
import org.dspace.external.OrcidRestConnector;
>>>>>>> dspace-7.2.1

/**
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public abstract class RestSource implements SolrAuthorityInterface {

    protected OrcidRestConnector restConnector;

    public RestSource(String url) {
        this.restConnector = new OrcidRestConnector(url);
    }
<<<<<<< HEAD

=======
>>>>>>> dspace-7.2.1
}
