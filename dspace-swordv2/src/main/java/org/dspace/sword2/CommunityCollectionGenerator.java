/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.apache.abdera.i18n.iri.IRI;
import org.apache.commons.lang.StringUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Community;
import org.apache.log4j.Logger;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.dspace.handle.HandleServiceImpl;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.swordapp.server.SwordCollection;

import java.util.List;

public class CommunityCollectionGenerator implements AtomCollectionGenerator
{
    private static Logger log = Logger
            .getLogger(CommunityCollectionGenerator.class);

    protected HandleService handleService = HandleServiceFactory.getInstance()
            .getHandleService();

    protected CommunityService communityService = ContentServiceFactory
            .getInstance().getCommunityService();

    public SwordCollection buildCollection(Context context, DSpaceObject dso,
            SwordConfigurationDSpace swordConfig)
            throws DSpaceSwordException
    {
        if (!(dso instanceof Community))
        {
            log.error(
                    "buildCollection passed something other than a Community object");
            throw new DSpaceSwordException(
                    "Incorrect ATOMCollectionGenerator instantiated");
        }

        // get the things we need out of the service
        SwordUrlManager urlManager = swordConfig
                .getUrlManager(context, swordConfig);

        Community com = (Community) dso;
        SwordCollection scol = new SwordCollection();

        // prepare the parameters to be put in the sword collection
        String location = urlManager.getDepositLocation(com);
        if (location == null)
        {
            location = handleService.getCanonicalForm(com.getHandle());
        }
        scol.setLocation(location);

        // collection title is just the community name
        String title = communityService.getName(com);
        if (StringUtils.isNotBlank(title))
        {
            scol.setTitle(title);
        }

        // FIXME: the community has no obvious licence
        // the collection policy is the licence to which the collection adheres
        // String collectionPolicy = col.getLicense();

        // abstract is the short description of the collection
        List<MetadataValue> abstracts = communityService
                .getMetadataByMetadataString(com, "short_description");
        if (abstracts != null && !abstracts.isEmpty())
        {
            String firstValue = abstracts.get(0).getValue();
            if (StringUtils.isNotBlank(firstValue))
            {
                scol.setAbstract(firstValue);
            }
        }

        // do we support mediated deposit
        scol.setMediation(swordConfig.isMediated());

        // NOTE: for communities, there are no MIME types that it accepts.
        // the list of mime types that we accept

        // offer up the collections from this item as deposit targets
        String subService = urlManager.constructSubServiceUrl(com);
        scol.addSubService(new IRI(subService));

        log.debug("Created ATOM Collection for DSpace Community");

        return scol;
    }
}
