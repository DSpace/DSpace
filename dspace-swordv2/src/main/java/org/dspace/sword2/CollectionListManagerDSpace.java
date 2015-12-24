/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.apache.abdera.Abdera;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.swordapp.server.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CollectionListManagerDSpace extends DSpaceSwordAPI
        implements CollectionListManager
{
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory
            .getInstance().getWorkspaceItemService();

    protected WorkflowItemService workflowItemService = WorkflowServiceFactory
            .getInstance().getWorkflowItemService();

    public Feed listCollectionContents(IRI colIRI,
            AuthCredentials authCredentials, SwordConfiguration swordConfig)
            throws SwordServerException, SwordError, SwordAuthException
    {
        SwordContext sc = null;
        try
        {
            SwordConfigurationDSpace config = (SwordConfigurationDSpace) swordConfig;
            sc = this.doAuth(authCredentials);
            Context context = sc.getContext();
            SwordUrlManager urlManager = config.getUrlManager(context, config);

            Collection collection = urlManager
                    .getCollection(context, colIRI.toString());
            if (collection == null)
            {
                throw new SwordError(404);
            }

            List<Item> items = this.listItems(sc, collection, swordConfig);
            return this.itemListToFeed(sc, items, swordConfig);
        }
        catch (DSpaceSwordException e)
        {
            throw new SwordServerException(e);
        }
        finally
        {
            // this is a read operation only, so there's never any need to commit the context
            if (sc != null)
            {
                sc.abort();
            }
        }
    }

    private Feed itemListToFeed(SwordContext sc, List<Item> items,
            SwordConfiguration swordConfig)
            throws DSpaceSwordException
    {
        SwordConfigurationDSpace config = (SwordConfigurationDSpace) swordConfig;
        SwordUrlManager urlManager = config
                .getUrlManager(sc.getContext(), config);

        Abdera abdera = new Abdera();
        Feed feed = abdera.newFeed();

        for (Item item : items)
        {
            Entry entry = feed.addEntry();
            entry.setId(urlManager.getEditIRI(item).toString());
            String title = this.stringMetadata(item, ConfigurationManager
                    .getProperty("swordv2-server", "title.field"));
            title = title == null ? "Untitled" : title;
            entry.setTitle(title);
            entry.addLink(urlManager.getContentUrl(item).toString(),
                    "edit-media");
        }

        return feed;
    }

    private List<Item> listItems(SwordContext sc, Collection collection,
            SwordConfiguration swordConfig)
            throws DSpaceSwordException
    {
        try
        {
            EPerson person = sc.getOnBehalfOf() != null ?
                    sc.getOnBehalfOf() :
                    sc.getAuthenticated();
            List<Item> collectionItems = new ArrayList<>();

            // first get the ones out of the archive
            Iterator<Item> items = itemService
                    .findBySubmitter(sc.getContext(), person);
            while (items.hasNext())
            {
                Item item = items.next();
                List<Collection> cols = item.getCollections();
                for (Collection col : cols)
                {
                    if (col.getID().equals(collection.getID()))
                    {
                        collectionItems.add(item);
                        break;
                    }
                }
            }

            // now get the ones out of the workspace
            List<WorkspaceItem> wsis = workspaceItemService
                    .findByEPerson(sc.getContext(), person);
            for (WorkspaceItem wsi : wsis)
            {
                Item item = wsi.getItem();

                // check for the wsi collection
                Collection wsCol = wsi.getCollection();
                if (wsCol.getID().equals(collection.getID()))
                {
                    collectionItems.add(item);
                }

                // then see if there are any additional associated collections
                List<Collection> cols = item.getCollections();
                for (Collection col : cols)
                {
                    if (col.getID().equals(collection.getID()))
                    {
                        collectionItems.add(item);
                        break;
                    }
                }
            }

            // finally get the ones out of the workflow
            List wfis = workflowItemService.findBySubmitter(sc.getContext(),
                    person);
            for (Object found : wfis)
            {
                if (found instanceof WorkflowItem)
                {
                    WorkflowItem wfi = (WorkflowItem) found;
                    Item item = wfi.getItem();

                    // check for the wfi collection
                    Collection wfCol = wfi.getCollection();
                    if (wfCol.getID().equals(collection.getID()))
                    {
                        collectionItems.add(item);
                    }

                    // then see if there are any additional associated collections
                    List<Collection> cols = item.getCollections();
                    for (Collection col : cols)
                    {
                        if (col.getID().equals(collection.getID()))
                        {
                            collectionItems.add(item);
                            break;
                        }
                    }
                }
            }

            return collectionItems;
        }
        catch (SQLException e)
        {
            throw new DSpaceSwordException(e);
        }
    }

    private String stringMetadata(Item item, String field)
    {
        if (field == null)
        {
            return null;
        }

        List<MetadataValue> dcvs = itemService
                .getMetadataByMetadataString(item, field);
        if (dcvs == null)
        {
            return null;
        }

        StringBuilder md = new StringBuilder();
        for (MetadataValue dcv : dcvs)
        {
            if (md.length() > 0)
            {
                md.append(", ");
            }
            md.append(dcv.getValue());
        }
        return md.toString();
    }
}
