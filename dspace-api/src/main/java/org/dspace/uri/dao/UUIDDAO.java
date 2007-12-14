package org.dspace.uri.dao;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.uri.ObjectIdentifier;

import java.util.UUID;

/**
 * Created by IntelliJ IDEA.
 * User: richard
 * Date: Dec 13, 2007
 * Time: 10:01:00 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class UUIDDAO
{
    protected Context context;
    
    public UUIDDAO(Context context)
    {
        this.context = context;
    }

    public abstract void create(UUID uuid, DSpaceObject dso);

    public abstract ObjectIdentifier retrieve(UUID uuid);

    public abstract ObjectIdentifier retrieve(int type, int id);

    public abstract void update(ObjectIdentifier oid);

    public abstract void delete(DSpaceObject dso);
}
