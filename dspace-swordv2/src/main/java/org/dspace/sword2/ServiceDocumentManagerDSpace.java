/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.ServiceDocument;
import org.swordapp.server.ServiceDocumentManager;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordCollection;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;
import org.swordapp.server.SwordWorkspace;

import java.util.List;

public class ServiceDocumentManagerDSpace implements ServiceDocumentManager
{
    /** logger */
    private static Logger log = Logger
            .getLogger(ServiceDocumentManagerDSpace.class);

    protected CommunityService communityService = ContentServiceFactory
            .getInstance().getCommunityService();

    public ServiceDocument getServiceDocument(String sdUri,
            AuthCredentials authCredentials, SwordConfiguration config)
            throws SwordError, SwordServerException, SwordAuthException
    {
        SwordContext sc = null;

        try
        {
            // first authenticate the request
            // note: this will build our various DSpace contexts for us
            SwordAuthenticator auth = new SwordAuthenticator();
            sc = auth.authenticate(authCredentials);
            Context context = sc.getContext();

            // ensure that this method is allowed
            WorkflowManagerFactory.getInstance().retrieveServiceDoc(context);

            if (log.isDebugEnabled())
            {
                log.debug(LogManager
                        .getHeader(context, "sword_do_service_document", ""));
            }

            // log the request
            String un = authCredentials.getUsername() != null ?
                    authCredentials.getUsername() :
                    "NONE";
            String obo = authCredentials.getOnBehalfOf() != null ?
                    authCredentials.getOnBehalfOf() :
                    "NONE";
            log.info(LogManager
                    .getHeader(context, "sword_service_document_request",
                            "username=" + un + ",on_behalf_of=" + obo));

            return this.getServiceDocument(sc, sdUri,
                    (SwordConfigurationDSpace) config);
        }
        catch (DSpaceSwordException e)
        {
            log.error("caught exception: ", e);
            throw new SwordServerException(
                    "The DSpace SWORD interface experienced an error", e);
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

    public ServiceDocument getServiceDocument(SwordContext context, String url,
            SwordConfigurationDSpace swordConfig)
            throws SwordError, SwordServerException, DSpaceSwordException
    {
        // first check that the sword context have
        // been set
        if (context == null)
        {
            throw new DSpaceSwordException(
                    "The Sword Context is null; please set it before calling getServiceDocument");
        }

        // ensure that this method is allowed
        WorkflowManagerFactory.getInstance()
                .retrieveServiceDoc(context.getContext());

        // get the URL manager
        SwordUrlManager urlManager = swordConfig
                .getUrlManager(context.getContext(), swordConfig);

        // we'll need the authenticator
        SwordAuthenticator swordAuth = new SwordAuthenticator();

        // construct the ATOM collection generators that we might use
        AtomCollectionGenerator comGen = new CommunityCollectionGenerator();
        AtomCollectionGenerator colGen = new CollectionCollectionGenerator();

        // construct a new service document
        ServiceDocument service = new ServiceDocument();

        // set the max upload size
        service.setMaxUploadSize(swordConfig.getMaxUploadSize());

        if (url == null || urlManager.isBaseServiceDocumentUrl(url))
        {
            // we are dealing with the default service document

            // set the title of the workspace as per the name of the DSpace installation
            String ws = ConfigurationManager.getProperty("dspace.name");
            SwordWorkspace workspace = new SwordWorkspace();
            workspace.setTitle(ws);

            // next thing to do is determine whether the default is communities or collections
            boolean swordCommunities = ConfigurationManager
                    .getBooleanProperty("swordv2-server", "expose-communities");

            if (swordCommunities)
            {
                List<Community> comms = swordAuth
                        .getAllowedCommunities(context);
                for (Community comm : comms)
                {
                    SwordCollection scol = comGen
                            .buildCollection(context.getContext(), comm,
                                    swordConfig);
                    workspace.addCollection(scol);
                }
            }
            else
            {
                List<Collection> cols = swordAuth
                        .getAllowedCollections(context);
                for (Collection col : cols)
                {
                    SwordCollection scol = colGen
                            .buildCollection(context.getContext(), col,
                                    swordConfig);
                    workspace.addCollection(scol);
                }
            }

            service.addWorkspace(workspace);
        }
        else
        {
            // we are dealing with a partial or sub-service document
            DSpaceObject dso = urlManager.extractDSpaceObject(url);
            if (dso == null)
            {
                throw new SwordError(404);
            }

            if (dso instanceof Community)
            {
                Community community = (Community) dso;
                SwordWorkspace workspace = new SwordWorkspace();
                workspace.setTitle(communityService.getName(community));

                List<Collection> collections = swordAuth
                        .getAllowedCollections(context, community);
                for (Collection collection : collections)
                {
                    SwordCollection scol = colGen
                            .buildCollection(context.getContext(), collection,
                                    swordConfig);
                    workspace.addCollection(scol);
                }

                List<Community> communities = swordAuth
                        .getCommunities(context, community);
                for (Community comm : communities)
                {
                    SwordCollection scol = comGen
                            .buildCollection(context.getContext(), comm,
                                    swordConfig);
                    workspace.addCollection(scol);
                }

                service.addWorkspace(workspace);
            }
        }

        return service;
    }

}
