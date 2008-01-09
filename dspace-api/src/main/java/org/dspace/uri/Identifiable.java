package org.dspace.uri;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * Author: Richard Jones
 * Date: Jan 9, 2008
 * Time: 9:23:39 AM
 */
public interface Identifiable
{
    SimpleIdentifier getSimpleIdentifier();

    void setSimpleIdentifier(SimpleIdentifier sid) throws UnsupportedIdentifierException;

    ObjectIdentifier getIdentifier();

    void setIdentifier(ObjectIdentifier oid) throws UnsupportedIdentifierException;

    List<ExternalIdentifier> getExternalIdentifiers();

    void setExternalIdentifiers(List<ExternalIdentifier> eids) throws UnsupportedIdentifierException;

    void addExternalIdentifier(ExternalIdentifier eid) throws UnsupportedIdentifierException;
}
