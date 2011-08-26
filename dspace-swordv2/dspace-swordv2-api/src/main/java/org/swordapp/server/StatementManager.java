/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.swordapp.server;

import java.util.Map;

public interface StatementManager
{
    Statement getStatement(String iri, Map<String, String> accept, AuthCredentials auth, SwordConfiguration config)
        throws SwordServerException, SwordError, SwordAuthException;
}
