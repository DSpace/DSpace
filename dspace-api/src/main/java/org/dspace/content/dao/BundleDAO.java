package org.dspace.content.dao;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.dao.CRUD;
import org.dspace.dao.Link;
import org.dspace.uri.dao.*;

import java.util.List;
import java.util.UUID;

public abstract class BundleDAO extends ContentDAO<BundleDAO>
        implements CRUD<Bundle>, Link<Bundle, Bitstream>
{
    protected Context context;

    protected BundleDAO childDAO;
    protected ObjectIdentifierDAO oidDAO;
    protected ExternalIdentifierDAO identifierDAO;

    public BundleDAO(Context context)
    {
        try
        {
            this.context = context;

            oidDAO = ObjectIdentifierDAOFactory.getInstance(context);
            identifierDAO = ExternalIdentifierDAOFactory.getInstance(context);
        }
        catch (ExternalIdentifierStorageException e)
        {
            throw new RuntimeException(e);
        }
        catch (ObjectIdentifierStorageException e)
        {
            throw new RuntimeException(e);
        }
    }

    public BundleDAO getChild()
    {
        return childDAO;
    }

    public void setChild(BundleDAO childDAO)
    {
        this.childDAO = childDAO;
    }

    public Bundle create() throws AuthorizeException
    {
        return childDAO.create();
    }

    public Bundle retrieve(int id)
    {
        return childDAO.retrieve(id);
    }

    public Bundle retrieve(UUID uuid)
    {
        return childDAO.retrieve(uuid);
    }

    public void update(Bundle bundle) throws AuthorizeException
    {
        childDAO.update(bundle);
    }

    public void delete(int id) throws AuthorizeException
    {
        childDAO.delete(id);
    }

    public List<Bundle> getBundles(Item item)
    {
        return childDAO.getBundles(item);
    }

    public List<Bundle> getBundles(Bitstream bitstream)
    {
        return childDAO.getBundles(bitstream);
    }

    public void link(Bundle bundle, Bitstream bitstream)
            throws AuthorizeException
    {
        childDAO.link(bundle, bitstream);
    }

    public void unlink(Bundle bundle, Bitstream bitstream)
            throws AuthorizeException
    {
        childDAO.unlink(bundle, bitstream);
    }

    public boolean linked(Bundle bundle, Bitstream bitstream)
    {
        return childDAO.linked(bundle, bitstream);
    }
}
