/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword;

import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.SWORDErrorException;
import org.purl.sword.base.Service;
import org.purl.sword.base.Workspace;
import org.purl.sword.atom.Generator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.content.Community;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;

import java.util.List;

public class ServiceDocumentManager
{
    protected CollectionService collectionService = ContentServiceFactory
            .getInstance().getCollectionService();

    protected CommunityService communityService = ContentServiceFactory
            .getInstance().getCommunityService();

    private SWORDService swordService;

    private SWORDAuthenticator swordAuth;

    public ServiceDocumentManager(SWORDService service)
    {
        this.swordService = service;
        this.swordAuth = new SWORDAuthenticator();
    }

    /**
     * Obtain the service document for the repository based on the
     * DSpace context and the SWORD context which must be set for
     * this object prior to calling this method.
     *
     * @return The service document based on the context of the request
     * @throws DSpaceSWORDException
     */
    public ServiceDocument getServiceDocument()
            throws DSpaceSWORDException, SWORDErrorException
    {
        return this.getServiceDocument(null);
    }

    public ServiceDocument getServiceDocument(String url)
            throws DSpaceSWORDException, SWORDErrorException
    {
        // extract the things we need from the service
        Context context = swordService.getContext();
        SWORDContext swordContext = swordService.getSwordContext();
        SWORDConfiguration swordConfig = swordService.getSwordConfig();
        SWORDUrlManager urlManager = swordService.getUrlManager();

        // construct the ATOM collection generators that we might use
        ATOMCollectionGenerator comGen = new CommunityCollectionGenerator(
                swordService);
        ATOMCollectionGenerator colGen = new CollectionCollectionGenerator(
                swordService);
        ATOMCollectionGenerator itemGen = new ItemCollectionGenerator(
                swordService);

        // first check that the context and sword context have
        // been set
        if (context == null)
        {
            throw new DSpaceSWORDException(
                    "The Context is null; please set it before calling getServiceDocument");
        }

        if (swordContext == null)
        {
            throw new DSpaceSWORDException(
                    "The SWORD Context is null; please set it before calling getServiceDocument");
        }

        // construct a new service document
        Service service = new Service(SWORDProperties.VERSION,
                swordConfig.isNoOp(), swordConfig.isVerbose());

        // set the max upload size
        service.setMaxUploadSize(swordConfig.getMaxUploadSize());

        // Set the generator
        this.addGenerator(service);

        //
        if (url == null || urlManager.isBaseServiceDocumentUrl(url))
        {
            // we are dealing with the default service document

            // set the title of the workspace as per the name of the DSpace installation
            String ws = ConfigurationManager.getProperty("dspace.name");
            Workspace workspace = new Workspace();
            workspace.setTitle(ws);

            // next thing to do is determine whether the default is communities or collections
            boolean swordCommunities = ConfigurationManager
                    .getBooleanProperty("sword-server", "expose-communities");

            if (swordCommunities)
            {
                List<Community> comms = swordAuth
                        .getAllowedCommunities(swordContext);
                for (Community comm : comms)
                {
                    org.purl.sword.base.Collection scol = comGen
                            .buildCollection(comm);
                    workspace.addCollection(scol);
                }
            }
            else
            {
                List<Collection> cols = swordAuth
                        .getAllowedCollections(swordContext);
                for (Collection col : cols)
                {
                    org.purl.sword.base.Collection scol = colGen
                            .buildCollection(col);
                    workspace.addCollection(scol);
                }
            }

            service.addWorkspace(workspace);
        }
        else
        {
            // we are dealing with a partial or sub-service document
            DSpaceObject dso = urlManager.extractDSpaceObject(url);

            if (dso instanceof Collection)
            {
                Collection collection = (Collection) dso;
                Workspace workspace = new Workspace();
                workspace.setTitle(
                        collectionService.getMetadata(collection, "name"));

                List<Item> items = swordAuth
                        .getAllowedItems(swordContext, collection);
                for (Item item : items)
                {
                    org.purl.sword.base.Collection scol = itemGen
                            .buildCollection(item);
                    workspace.addCollection(scol);
                }

                service.addWorkspace(workspace);
            }
            else if (dso instanceof Community)
            {
                Community community = (Community) dso;
                Workspace workspace = new Workspace();
                workspace.setTitle(
                        communityService.getMetadata(community, "name"));

                List<Collection> collections = swordAuth
                        .getAllowedCollections(swordContext, community);
                for (Collection collection : collections)
                {
                    org.purl.sword.base.Collection scol = colGen
                            .buildCollection(collection);
                    workspace.addCollection(scol);
                }

                List<Community> communities = swordAuth
                        .getCommunities(swordContext, community);
                for (Community comm : communities)
                {
                    org.purl.sword.base.Collection scol = comGen
                            .buildCollection(comm);
                    workspace.addCollection(scol);
                }

                service.addWorkspace(workspace);
            }
        }

        return new ServiceDocument(service);
    }

    /**
     * Add the generator field content
     *
     * @param service The service document to add the generator to
     */
    private void addGenerator(Service service)
    {
        boolean identify = ConfigurationManager
                .getBooleanProperty("sword-server", "identify-version", false);
        SWORDUrlManager urlManager = swordService.getUrlManager();
        String softwareUri = urlManager.getGeneratorUrl();
        if (identify)
        {
            Generator generator = new Generator();
            generator.setUri(softwareUri);
            generator.setVersion(SWORDProperties.VERSION);
            service.setGenerator(generator);
        }
    }
}
