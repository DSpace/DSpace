/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.fileaccess;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.fileaccess.service.ItemMetadataService;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 26/10/15
 * Time: 13:18
 */
public class ElsevierItemMetadataServiceImpl implements ItemMetadataService {

    @Autowired
    private ItemService itemService;

    @Override
    public String getPII(Item item) {
        String piiMdField = ConfigurationManager.getProperty("elsevier-sciencedirect", "metadata.field.pii");

        List<MetadataValue> piiMetadata = itemService.getMetadataByMetadataString(item, piiMdField);

        String pii = null;
        if(piiMetadata.size()>0) {
            pii = piiMetadata.get(0).getValue();
        }

        return pii;
    }

    @Override
    public String getDOI(Item item) {
        String doiMdField = ConfigurationManager.getProperty("elsevier-sciencedirect", "metadata.field.doi");

        List<MetadataValue> doiMetadata = itemService.getMetadataByMetadataString(item, doiMdField);


        String doi = null;
        if(doiMetadata.size()>0) {
            doi = doiMetadata.get(0).getValue();

            if(doi.startsWith("DOI:")){
                doi = doi.substring(4);
            }
        }

        return doi;
    }
}
