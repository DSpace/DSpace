/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.rdf;

import org.dspace.core.Constants;

import java.util.UUID;

/**
 * RDFConverter Exception
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 */
class RDFMissingIdentifierException extends Exception {
    public RDFMissingIdentifierException()
    {
        super("Coudln't generate a necessary RDF Identifier.");
    }

    RDFMissingIdentifierException(int type, UUID id) {
        super("Couldn't generate a necessary RDF Identifier for " 
                + Constants.typeText[type] + " " + id.toString() + ".");
    }
}
