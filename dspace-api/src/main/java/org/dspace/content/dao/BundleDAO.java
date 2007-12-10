package org.dspace.content.dao;

import java.util.UUID;
import java.util.List;

import org.dspace.storage.dao.CRUD;
import org.dspace.storage.dao.Link;
import org.dspace.content.Bundle;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;

public abstract class BundleDAO extends ContentDAO<BundleDAO>
        implements CRUD<Bundle>, Link<Bundle, Bitstream>
{
    protected Context context;

    protected BundleDAO childDAO;

    public BundleDAO(Context context)
    {
        this.context = context;
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
