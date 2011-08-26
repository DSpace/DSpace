/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.swordapp.server;

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Feed;

public interface CollectionListManager
{
    Feed listCollectionContents(IRI collectionIRI, AuthCredentials auth, SwordConfiguration config) throws SwordServerException, SwordAuthException, SwordError;
}
