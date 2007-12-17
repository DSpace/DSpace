package org.dspace.administer.update1516;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.dao.BitstreamDAO;
import org.dspace.content.dao.BitstreamDAOFactory;
import org.dspace.content.dao.BundleDAO;
import org.dspace.content.dao.BundleDAOFactory;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.content.dao.CollectionDAOFactory;
import org.dspace.content.dao.CommunityDAO;
import org.dspace.content.dao.CommunityDAOFactory;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.core.Context;
import org.dspace.uri.ObjectIdentifier;
import org.dspace.uri.dao.ObjectIdentifierDAO;
import org.dspace.uri.dao.ObjectIdentifierDAOFactory;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: richard
 * Date: Dec 13, 2007
 * Time: 4:05:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class MigrateUUID
{
    public void migrate()
            throws SQLException, AuthorizeException
    {
        Context context = new Context();
        context.setIgnoreAuthorization(true);

        CommunityDAO communityDAO = CommunityDAOFactory.getInstance(context);
        CollectionDAO collectionDAO = CollectionDAOFactory.getInstance(context);
        ItemDAO itemDAO = ItemDAOFactory.getInstance(context);
        BundleDAO bundleDAO = BundleDAOFactory.getInstance(context);
        BitstreamDAO bitstreamDAO = BitstreamDAOFactory.getInstance(context);
        ObjectIdentifierDAO oidDAO = ObjectIdentifierDAOFactory.getInstance(context);

        // register all the items
        List<Item> items = itemDAO.getItems();
        for (Item item : items)
        {
            ObjectIdentifier oid = new ObjectIdentifier(true);
            item.setIdentifier(oid);
            oidDAO.update(item.getIdentifier());

            // do the bundles while we're at it
            Bundle[] bundles = item.getBundles();
            for (int i = 0; i < bundles.length; i++)
            {
                ObjectIdentifier oidb = new ObjectIdentifier(true);
                bundles[i].setIdentifier(oidb);
                oidDAO.update(bundles[i].getIdentifier());

                // do the bitstreams while we're at it
                Bitstream[] bss = bundles[i].getBitstreams();
                for (int j = 0; j < bss.length; j++)
                {
                    ObjectIdentifier oidc = new ObjectIdentifier(true);
                    bss[j].setIdentifier(oidc);
                    oidDAO.update(bss[j].getIdentifier());
                    bitstreamDAO.update(bss[j]);
                }

                bundleDAO.update(bundles[i]);
            }

            itemDAO.update(item);
        }


        List<Collection> cols = collectionDAO.getCollections();
        for (Collection col : cols)
        {
            ObjectIdentifier oid = new ObjectIdentifier(true);
            col.setIdentifier(oid);
            oidDAO.update(oid);
            collectionDAO.update(col);
        }

        List<Community> coms = communityDAO.getCommunities();
        for (Community com : coms)
        {
            ObjectIdentifier oid = new ObjectIdentifier(true);
            com.setIdentifier(oid);
            oidDAO.update(oid);
            communityDAO.update(com);
        }

        context.complete();
    }
}
