/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.discovery;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;

public class ResourceTypeSolrIndexer implements SolrServiceIndexPlugin {

    private static final String REGEX = "\\S+(?:\\s*\\|\\|\\|(\\s*\\S+))+";

    @Override
    public void additionalIndex(Context context, BrowsableDSpaceObject dso, SolrInputDocument document) {

        final String typeText = StringUtils.deleteWhitespace(dso.getTypeText().toLowerCase());
        String acvalue = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty(
                "discovery.facet.type." + typeText, typeText + SolrServiceImpl.AUTHORITY_SEPARATOR + typeText);
        String fvalue = acvalue;
        addResourceTypeIndex(document, acvalue, fvalue);
    }

    private void addResourceTypeIndex(SolrInputDocument document, String acvalue, String fvalue) {

        document.addField("resourcetype_filter", acvalue);

        String[] avalues = acvalue.split(SolrServiceImpl.AUTHORITY_SEPARATOR);
        acvalue = avalues[0];

        String avalue = avalues[1];
        document.addField("resourcetype_authority", avalue);
        document.addField("resourcetype_group", avalue);
        document.addField("resourcetype_ac", acvalue);

        Pattern pattern = Pattern.compile(REGEX);
        Matcher matcher = pattern.matcher(acvalue);
        if (matcher.matches()) {
            fvalue = matcher.group(1);
        }

        document.addField("resourcetype_keyword", fvalue);
    }

}
