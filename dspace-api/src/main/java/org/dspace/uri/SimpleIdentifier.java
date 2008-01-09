package org.dspace.uri;

import java.util.UUID;

public class SimpleIdentifier
{
    protected UUID uuid;

    public SimpleIdentifier(UUID uuid)
    {
        this.uuid = uuid;
    }

    public SimpleIdentifier(String uuid)
    {
        this.uuid = UUID.fromString(uuid);
    }

    public UUID getUUID()
    {
        return uuid;
    }

    public String getCanonicalForm()
    {
        if (uuid == null)
        {
            return null;
        }
        return "uuid:" + uuid.toString();
    }

    public static SimpleIdentifier parseCanonicalForm(String canonicalForm)
    {
        if (!canonicalForm.startsWith("uuid:"))
        {
            return null;
        }

        String value = canonicalForm.substring(5);

        return new SimpleIdentifier(value);
    }
}
