/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.purl.sword.base.Collection;
import org.dspace.content.*;
import org.dspace.core.Context;

import java.util.List;

/**
 * @author Richard Jones
 *
 * Class to generate ATOM Collection elements for DSpace Items
 */
public class ItemCollectionGenerator extends ATOMCollectionGenerator
{
    protected ItemService itemService = ContentServiceFactory.getInstance()
            .getItemService();

    public ItemCollectionGenerator(SWORDService service)
    {
        super(service);
    }

    /**
     * Build the collection around the given DSpaceObject.  If the object
     * is not an instance of a DSpace Item this method will throw an
     * exception.
     *
     * @param dso the dso for which the collection should be built
     * @throws DSpaceSWORDException if the dso is not an instance of Item
     */
    public Collection buildCollection(DSpaceObject dso)
            throws DSpaceSWORDException
    {
        if (!(dso instanceof Item))
        {
            throw new DSpaceSWORDException(
                    "Incorrect ATOMCollectionGenerator instantiated");
        }

        // get the things we need out of the service
        SWORDConfiguration swordConfig = swordService.getSwordConfig();
        SWORDUrlManager urlManager = swordService.getUrlManager();
        Context context = swordService.getContext();

        Item item = (Item) dso;
        Collection scol = new Collection();

        // prepare the parameters to be put in the sword collection
        String location = urlManager.getDepositLocation(item);
        scol.setLocation(location);

        // the item title is the sword collection title, or "untitled" otherwise
        String title = "Untitled";
        List<MetadataValue> dcv = itemService
                .getMetadataByMetadataString(item, "dc.title");
        if (!dcv.isEmpty())
        {
            String firstValue = dcv.get(0).getValue();
            if (StringUtils.isNotBlank(firstValue))
            {
                title = firstValue;
            }
        }
        scol.setTitle(title);

        // FIXME: there is no collection policy for items that is obvious to provide.
        // the collection policy is the licence to which the collection adheres
        // String collectionPolicy = col.getLicense();

        // abstract is the short description of the item, if it exists
        String dcAbstract = "";
        List<MetadataValue> dcva = itemService
                .getMetadataByMetadataString(item, "dc.description.abstract");
        if (!dcva.isEmpty())
        {
            String firstValue = dcva.get(0).getValue();
            if (StringUtils.isNotBlank(firstValue))
            {
                dcAbstract = firstValue;
            }
        }
        if (StringUtils.isNotBlank(dcAbstract))
        {
            scol.setAbstract(dcAbstract);
        }

        // do we support mediated deposit
        scol.setMediation(swordConfig.isMediated());

        // the list of mime types that we accept, which we take from the
        // bitstream format registry
        List<String> acceptFormats = swordConfig.getAccepts(context, item);
        for (String acceptFormat : acceptFormats)
        {
            scol.addAccepts(acceptFormat);
        }

        return scol;
    }
}
