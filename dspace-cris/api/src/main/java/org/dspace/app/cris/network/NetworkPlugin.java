/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.network;


import java.util.List;

import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.network.dto.JsGraph;
import org.dspace.discovery.SearchServiceException;

public interface NetworkPlugin
{
    public static String CFG_MODULE = "network";
    
    public JsGraph search(String authority, String name, Integer level,
            boolean showExternal, boolean showSameDept, String dept,
            Integer modeEntity) throws Exception;

    public List<VisualizationGraphNode> load(List<String[]> discardedNode,
            Integer importedNodes, Boolean otherError) throws Exception;

    public String getEdgeColorToOverride();

    public String getEdgeCustomColor();

    public String getCustomLineWidth(int i);

    public String getType();

    public String getLineWidthToOverride();

    public String getNodeCustomColor();

    public String getNodeLeafCustomColor();

    public List<ResearcherPage> loadMetrics(List<String[]> discardedNode,
            Integer importedNodes, Boolean otherError) throws SearchServiceException;

    public Integer getCustomMaxDepth();
}
