/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import javax.servlet.http.HttpServletRequest;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DSpaceObject;

public interface StatisticsMetadataGenerator
{

    public void addMetadata(SolrInputDocument doc1, HttpServletRequest request,
            DSpaceObject dspaceObject);
}
