package org.swordapp.server;

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Feed;

public interface CollectionListManager
{
    Feed listCollectionContents(IRI collectionIRI, AuthCredentials auth, SwordConfiguration config) throws SwordServerException, SwordAuthException, SwordError;
}
