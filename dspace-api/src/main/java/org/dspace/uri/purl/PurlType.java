package org.dspace.uri.purl;

import org.dspace.uri.ExternalIdentifierType;
import org.dspace.uri.ExternalIdentifier;
import org.dspace.uri.ObjectIdentifier;

public class PurlType extends ExternalIdentifierType
{
    public PurlType()
    {
        super("purl", "http", "purl.oclc.org", "://", "/");
    }

    public ExternalIdentifier getInstance(String value, ObjectIdentifier oid)
    {
        return new Purl(value, oid);
    }

    public boolean equals(ExternalIdentifierType type)
    {
        if (type instanceof PurlType)
        {
            return true;
        }
        return false;
    }
}
