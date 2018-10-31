/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.statistics;

import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.statistics.StatisticsMetadataGenerator;


public class BundleNameAdditionalStatisticsData implements
        StatisticsMetadataGenerator
{

    private static Logger log = Logger
            .getLogger(BundleNameAdditionalStatisticsData.class);

    @Override
    public void addMetadata(SolrInputDocument document, HttpServletRequest request,
            DSpaceObject dso)
    {
        if (dso instanceof Bitstream) {
            Bitstream bit = (Bitstream) dso;
            Bundle[] bundles;
            try
            {
                bundles = bit.getBundles();
                for (Bundle bundle : bundles) {
                    document.addField("bundleName", bundle.getName());
                }
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

}
