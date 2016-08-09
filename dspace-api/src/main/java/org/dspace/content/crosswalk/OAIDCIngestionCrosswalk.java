/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.jdom.Element;
import org.jdom.Namespace;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * DIM ingestion crosswalk
 * <p>
 * Processes Dublic Core metadata encased in an oai_dc:dc wrapper
 *
 * @author Alexey Maslov
 * @version $Revision: 1 $
 */
public class OAIDCIngestionCrosswalk
    implements IngestionCrosswalk
{
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
	private CrosswalkMetadataValidator metadataValidator = new CrosswalkMetadataValidator();

	@Override
	public void ingest(Context context, DSpaceObject dso, List<Element> metadata, boolean createMissingMetadataFields) throws CrosswalkException, IOException, SQLException, AuthorizeException {
        Element wrapper = new Element("wrap", metadata.get(0).getNamespace());
		wrapper.addContent(metadata);
		
		ingest(context,dso,wrapper, createMissingMetadataFields);
	}

	@Override
	public void ingest(Context context, DSpaceObject dso, Element root, boolean createMissingMetadataFields) throws CrosswalkException, IOException, SQLException, AuthorizeException {
		
		if (dso.getType() != Constants.ITEM)
        {
            throw new CrosswalkObjectNotSupported("DIMIngestionCrosswalk can only crosswalk an Item.");
        }
        Item item = (Item)dso;
        
        if (root == null) {
        	System.err.println("The element received by ingest was null");
        	return;
        }
        
        List<Element> metadata = root.getChildren();
        for (Element element : metadata) {
			// get language - prefer xml:lang, accept lang.
			String lang = element.getAttributeValue("lang", Namespace.XML_NAMESPACE);
			if (lang == null) {
				lang = element.getAttributeValue("lang");
			}
			MetadataField metadataField = metadataValidator.checkMetadata(context, MetadataSchema.DC_SCHEMA, element.getName(), null, createMissingMetadataFields);
			itemService.addMetadata(context, item, metadataField, lang, element.getText());
        }
        
	}
	
}
