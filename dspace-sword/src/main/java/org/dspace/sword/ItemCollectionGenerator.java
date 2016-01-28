/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword;

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
	public ItemCollectionGenerator(SWORDService service)
	{
		super(service);
	}

	/**
	 * Build the collection around the give DSpaceObject.  If the object
	 * is not an instance of a DSpace Item this method will throw an
	 * exception.
	 *
	 * @param dso
	 * @throws DSpaceSWORDException
	 */
	public Collection buildCollection(DSpaceObject dso) throws DSpaceSWORDException
	{
		if (!(dso instanceof Item))
		{
			throw new DSpaceSWORDException("Incorrect ATOMCollectionGenerator instantiated");
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
		Metadatum[] dcv = item.getMetadataByMetadataString("dc.title");
		if (dcv.length > 0)
		{
			title = dcv[0].value;
		}
		scol.setTitle(title);

		// FIXME: there is no collection policy for items that is obvious to provide.
		// the collection policy is the licence to which the collection adheres
		// String collectionPolicy = col.getLicense();

		// abstract is the short description of the item, if it exists
		String dcAbstract = "";
		Metadatum[] dcva = item.getMetadataByMetadataString("dc.description.abstract");
		if (dcva.length > 0)
		{
			dcAbstract = dcva[0].value;
		}
		if (dcAbstract != null && !"".equals(dcAbstract))
		{
			scol.setAbstract(dcAbstract);
		}

		// do we suppot mediated deposit
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
