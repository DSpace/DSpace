package org.dspace.uri.purl;

import org.dspace.uri.ExternalIdentifier;
import org.dspace.uri.ObjectIdentifier;
import org.dspace.core.ConfigurationManager;

import java.net.URI;
import java.net.URISyntaxException;

public class Purl extends ExternalIdentifier
{
    /**
     * DO NOT USE
     *
     * Required for PluginManager to function, but should not be used
     * to construct new Handle instances in main code
     */
    public Purl()
    {
        super();
    }
    /**
     * Construct a new Handle object with the given handle value backed
     * by the given ObjectIdentifier
     *
     * @param value
     * @param oid
     */
    public Purl(String value, ObjectIdentifier oid)
    {
        super(new PurlType(), value, oid);
    }

    public Purl(String value)
    {
        super(new PurlType(), value);
    }

    public String getCanonicalForm()
    {
        // canonical form: http://purl.oclc.org/[partial redirect]/[local identifier]

        // this is what the getURI method will return
        return this.getURI().toString();
    }

    public ExternalIdentifier parseCanonicalForm(String canonical)
    {
        String prefix = ConfigurationManager.getProperty("purl.partial-redirect.prefix");
        if (prefix == null || "".equals(prefix))
        {
            throw new RuntimeException("no configuration, or configuration invalid for purl.partial-redirect.prefix");
        }

        // canonical form: http://purl.oclc.org/[partial redirect]/[local identifier]
        String starter = "http://purl.oclc.org/" + prefix;
        if (!canonical.startsWith(starter))
        {
            return null;
        }

        // strip the common part of the url from the start of the string
        String value = canonical.substring(starter.length());

        if ("".equals(value))
        {
            return null;
        }

        Purl purl = new Purl(value);
        return purl;
    }

    public URI getURI()
    {
        try
        {
            String prefix = ConfigurationManager.getProperty("purl.partial-redirect.prefix");
            if (prefix == null || "".equals(prefix))
            {
                throw new RuntimeException("no configuration, or configuration invalid for purl.partial-redirect.prefix");
            }

            // e.g. http + :// + purl.oclc.org + / + /NET/abc + 12345678-1234-1234-1234-123456789012
            return new URI(type.getProtocol() + type.getProtocolActivator() + type.getBaseURI() + type.getBaseSeparator() + prefix + value);
        }
        catch (URISyntaxException urise)
        {
            throw new RuntimeException(urise);
        }
    }
}
