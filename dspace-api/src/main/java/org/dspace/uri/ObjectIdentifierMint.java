package org.dspace.uri;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.uri.dao.ObjectIdentifierDAO;
import org.dspace.uri.dao.ObjectIdentifierDAOFactory;

import java.util.UUID;

public class ObjectIdentifierMint
{
    public static ObjectIdentifier mint(Context context, DSpaceObject dso)
    {
        UUID uuid = UUID.randomUUID();
        ObjectIdentifier oid = new ObjectIdentifier(uuid, dso.getType(), dso.getID());
        dso.setIdentifier(oid);
        return oid;
    }

    public static SimpleIdentifier mintSimple()
    {
        UUID uuid = UUID.randomUUID();
        SimpleIdentifier sid = new SimpleIdentifier(uuid);
        return sid;
    }

    public static ObjectIdentifier get(Context context, int type, int id)
    {
        ObjectIdentifierDAO dao = ObjectIdentifierDAOFactory.getInstance(context);
        ObjectIdentifier oid = dao.retrieve(type, id);
        return oid;
    }

    public static ObjectIdentifier get(Context context, UUID uuid)
    {
        ObjectIdentifierDAO dao = ObjectIdentifierDAOFactory.getInstance(context);
        ObjectIdentifier oid = dao.retrieve(uuid);
        return oid;
    }
}
