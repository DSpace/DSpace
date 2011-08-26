/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.swordapp.server;

import java.util.Map;

public interface ContainerManager
{
    DepositReceipt getEntry(String editIRI, Map<String, String> accept, AuthCredentials auth, SwordConfiguration config)
            throws SwordServerException, SwordError, SwordAuthException;

    DepositReceipt replaceMetadata(String editIRI, Deposit deposit, AuthCredentials auth, SwordConfiguration config)
        throws SwordError, SwordServerException, SwordAuthException;

    DepositReceipt replaceMetadataAndMediaResource(String editIRI, Deposit deposit, AuthCredentials auth, SwordConfiguration config)
        throws SwordError, SwordServerException, SwordAuthException;

    DepositReceipt addMetadataAndResources(String editIRI, Deposit deposit, AuthCredentials auth, SwordConfiguration config)
        throws SwordError, SwordServerException, SwordAuthException;

    DepositReceipt addMetadata(String editIRI, Deposit deposit, AuthCredentials auth, SwordConfiguration config)
        throws SwordError, SwordServerException, SwordAuthException;

    DepositReceipt addResources(String editIRI, Deposit deposit, AuthCredentials auth, SwordConfiguration config)
        throws SwordError, SwordServerException, SwordAuthException;

    void deleteContainer(String editIRI, AuthCredentials auth, SwordConfiguration config)
        throws SwordError, SwordServerException, SwordAuthException;

    DepositReceipt useHeaders(String editIRI, Deposit deposit, AuthCredentials auth, SwordConfiguration config)
        throws SwordError, SwordServerException, SwordAuthException;

	boolean isStatementRequest(String editIRI, Map<String, String> accept, AuthCredentials auth, SwordConfiguration config)
		throws SwordError, SwordServerException, SwordAuthException;
}
