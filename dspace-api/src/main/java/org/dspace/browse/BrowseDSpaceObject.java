/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;

public class BrowseDSpaceObject extends BrowseItem
{
    private BrowsableDSpaceObject browseObject;

    public BrowseDSpaceObject(Context context, BrowsableDSpaceObject browseObject)
    {
        super(context, browseObject.getID(), browseObject.isArchived(), browseObject.isWithdrawn(), browseObject.isDiscoverable());
        this.browseObject = browseObject;
        this.extraInfo = browseObject.getExtraInfo();
    }
    
    @Override
    public Metadatum[] getMetadata(String schema, String element,
            String qualifier, String lang)
    {
        return browseObject.getMetadata(schema, element, qualifier, lang);
    }
    
    public Metadatum[] getMetadataWithoutPlaceholder(String schema, String element,
            String qualifier, String lang){
    	if(browseObject instanceof Item){
    		return ((Item) browseObject).getMetadataWithoutPlaceholder(schema, element, qualifier, lang);
    	}else{
    		return browseObject.getMetadata(schema, element, qualifier, lang);
    	}
    }

    public List<String> getMetadataValue(String mdString)
    {
        return browseObject.getMetadataValue(mdString);
    }
    
    public BrowsableDSpaceObject getBrowsableDSpaceObject()
    {
        return browseObject;
    }

}
