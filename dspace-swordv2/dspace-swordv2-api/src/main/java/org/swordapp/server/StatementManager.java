package org.swordapp.server;

import java.util.Map;

public interface StatementManager
{
    Statement getStatement(String iri, Map<String, String> accept, AuthCredentials auth, SwordConfiguration config)
        throws SwordServerException, SwordError, SwordAuthException;
}
