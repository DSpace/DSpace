/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.util.List;
import java.util.Map;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;

/**
 * Implements virtual field processing for generate wos html tag map element
 *
 * @author l.pascarelli
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class VirtualFieldWosElement implements VirtualFieldDisseminator {

    private static final String fieldPubmedID = ConfigurationManager.getProperty("cris", "ametrics.identifier.pmid");

    private static final String fieldWosID = ConfigurationManager.getProperty("cris", "ametrics.identifier.ut");

    private static final String fieldDoiID = ConfigurationManager.getProperty("cris", "ametrics.identifier.doi");

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    @Override
    public String[] getMetadata(Item item, Map<String, String> fieldCache, String fieldName) {
        String result = "<map name=\"" + item.getID() + "\">";
        List<MetadataValue> metadaValuePMID = itemService.getMetadataByMetadataString(item, fieldPubmedID);
        if (metadaValuePMID != null && metadaValuePMID.size() > 0) {
            result += "<val name=\"pmid\">" + metadaValuePMID.get(0).getValue() + "</val>";
        }
        List<MetadataValue> metadaValueDoi = itemService.getMetadataByMetadataString(item, fieldDoiID);
        if (metadaValueDoi != null && metadaValueDoi.size() > 0) {
            result += "<val name=\"doi\">" + metadaValueDoi.get(0).getValue() + "</val>";
        }
        List<MetadataValue> metadaValueWos = itemService.getMetadataByMetadataString(item, fieldWosID);
        if (metadaValueWos != null && metadaValueWos.size() > 0) {
            result += "<val name=\"ut\">" + metadaValueWos.get(0).getValue() + "</val>";
        }
        result += "</map>";
        return new String[] { result };
    }
}