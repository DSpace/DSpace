/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.fileaccess;

import java.util.*;
import org.apache.commons.lang.*;
import org.dspace.content.*;
import org.dspace.content.service.*;
import org.dspace.core.*;
import org.dspace.fileaccess.service.*;
import org.dspace.services.*;
import org.dspace.services.factory.*;
import org.springframework.beans.factory.annotation.*;

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
        String piiMdField = ConfigurationManager.getProperty("external-sources.elsevier.metadata.field.pii");

        List<MetadataValue> piiMetadata = itemService.getMetadataByMetadataString(item, piiMdField);

        String pii = null;
        if (piiMetadata.size() > 0) {
            pii = piiMetadata.get(0).getValue();
        }

        return pii;
    }

    @Override
    public String getDOI(Item item) {
        ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

        String[] doiMdField = configurationService.getPropertyAsType("external-sources.elsevier.metadata.field.doi", String[].class);
        String doiValue = null;
        for (String field : doiMdField) {
            List<MetadataValue> doiMetadata = itemService.getMetadataByMetadataString(item, field);
            if (doiMetadata.size() > 0) {
                doiValue = doiMetadata.get(0).getValue();
                break;
            }
        }

        if (StringUtils.isNotBlank(doiValue)) {
            String[] possiblePrefixes = {"doi:", "http://dx.doi.org/"};
            for (String prefix : possiblePrefixes) {
                if (StringUtils.startsWithIgnoreCase(doiValue, prefix)) {
                    return doiValue.substring(prefix.length());
                }
            }
        }


        return doiValue;
    }

    @Override
    public String getEID(Item item) {
        String doiMdField = ConfigurationManager.getProperty("external-sources.elsevier.metadata.field.eid");

        List<MetadataValue> eidMetadata = itemService.getMetadataByMetadataString(item, doiMdField);
        String eid = null;
        if (eidMetadata.size() > 0) {
            eid = eidMetadata.get(0).getValue();
        }

        return eid;
    }

    @Override
    public String getScopusID(Item item) {
        String scopusMDField = ConfigurationManager.getProperty("external-sources.elsevier.metadata.field.scopus_id");

        List<MetadataValue> scopusMetadata = itemService.getMetadataByMetadataString(item, scopusMDField);

        String scopus_id = null;
        if (scopusMetadata.size() > 0) {
            scopus_id = scopusMetadata.get(0).getValue();
            if (StringUtils.startsWith(scopus_id, "SCOPUS_ID:")) {
                scopus_id = scopus_id.substring("SCOPUS_ID:".length());
            }
        }

        return scopus_id;
    }

    @Override
    public String getPubmedID(Item item) {
        String pubmedMDField = ConfigurationManager.getProperty("external-sources.elsevier.metadata.field.pubmed_id");

        List<MetadataValue> pubmedMetadata = itemService.getMetadataByMetadataString(item, pubmedMDField);

        String pubmed_id = null;
        if (pubmedMetadata.size() > 0) {
            pubmed_id = pubmedMetadata.get(0).getValue();
        }

        return pubmed_id;
    }
}
