/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.purl.sword.base.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Community;
import org.apache.log4j.Logger;

import java.util.List;

public class CommunityCollectionGenerator extends ATOMCollectionGenerator
{
    private static Logger log = Logger
            .getLogger(CommunityCollectionGenerator.class);

    protected CommunityService communityService = ContentServiceFactory
            .getInstance().getCommunityService();

    public CommunityCollectionGenerator(SWORDService service)
    {
        super(service);
        log.debug("Created instance of CommunityCollectionGenerator");
    }

    public Collection buildCollection(DSpaceObject dso)
            throws DSpaceSWORDException
    {
        if (!(dso instanceof Community))
        {
            log.error(
                    "buildCollection passed something other than a Community object");
            throw new DSpaceSWORDException(
                    "Incorrect ATOMCollectionGenerator instantiated");
        }

        // get the things we need out of the service
        SWORDConfiguration swordConfig = swordService.getSwordConfig();
        SWORDUrlManager urlManager = swordService.getUrlManager();

        Community com = (Community) dso;
        Collection scol = new Collection();

        // prepare the parameters to be put in the sword collection
        String location = urlManager.getDepositLocation(com);
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
        scol.setService(subService);

        log.debug("Created ATOM Collection for DSpace Community");

        return scol;
    }
}
