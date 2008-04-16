package org.dspace.uri.purl;

import org.dspace.uri.IdentifierAssigner;
import org.dspace.uri.IdentifierResolver;
import org.dspace.uri.Identifiable;
import org.dspace.uri.IdentifierException;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;

import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class PurlManager implements IdentifierAssigner<Purl>, IdentifierResolver<Purl>
{
    public Purl mint(Context context, Identifiable dso)
            throws IdentifierException
    {
        UUID uuid = UUID.randomUUID();
        String purlValue = uuid.toString();
        Purl purl = new Purl(purlValue, dso.getIdentifier());
        return purl;
    }

    public Purl extractURLIdentifier(String path)
            throws IdentifierException
    {
        String purlRX = ".*purl/([a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}).*";
        Pattern p = Pattern.compile(purlRX);
        Matcher m = p.matcher(path);
        if (!m.matches())
        {
            return null;
        }
        String value = m.group(1);
        Purl purl = new Purl(value);
        return purl;
    }
}
