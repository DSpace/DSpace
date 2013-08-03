/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DSpaceObject;
import org.dspace.utils.DSpace;

public class DSpaceObjectStatsIndexPlugin implements SolrStatsIndexPlugin
{

    private static Logger log = Logger
            .getLogger(DSpaceObjectStatsIndexPlugin.class);

    private List<StatisticsMetadataGenerator> generators;
    

    @Override
    public void additionalIndex(HttpServletRequest request, DSpaceObject dso,
            SolrInputDocument document)
    {
        storeAdditionalMetadata(dso, request, document);
    }

    private void storeAdditionalMetadata(DSpaceObject dspaceObject,
            HttpServletRequest request, SolrInputDocument doc1)
    {
        if (getGenerators() != null)
        {
            for (StatisticsMetadataGenerator generator : generators)
            {
                generator.addMetadata(doc1, request, dspaceObject);
            }
        }
    }

    public List<StatisticsMetadataGenerator> getGenerators()
    {
        if(generators==null) {
            DSpace dspace = new DSpace();
            generators = dspace.getServiceManager().getServicesByType(StatisticsMetadataGenerator.class);
        }
        return generators;
    }
   

}
