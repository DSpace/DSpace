/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.network;

import it.cilea.osd.jdyna.value.TextValue;

import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.VisibilityConstants;
import org.dspace.app.cris.model.jdyna.RPAdditionalFieldStorage;
import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPProperty;
import org.dspace.app.cris.network.dto.JsGraph;
import org.dspace.app.cris.network.dto.JsGraphAdjacence;
import org.dspace.app.cris.network.dto.JsGraphData;
import org.dspace.app.cris.network.dto.JsGraphNodeData;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.core.ConfigurationManager;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;

public abstract class AVisualizationGraph implements NetworkPlugin
{

    public static String splitterAuthority = "###";
    
    protected Pattern p = Pattern.compile("rp(.*?)");

    private static final String FACET_METRICS = "focus_auth";

    private static final String FACET_SEARCH = "focus";

    private static final String COLOR_PROPERTY = "network.connection.color";

    private static final String COLOR_SUFFIX_NODE = "node";

    private static final String COLOR_SUFFIX_EDGETOO = "edgetooverride";

    private static final String COLOR_SUFFIX_EDGE = "edge";

    /** log4j logger */
    protected static Logger log = Logger.getLogger(AVisualizationGraph.class);

    private DSpace dspace = new DSpace();

    private VisualizationGraphSolrService service = dspace.getServiceManager()
            .getServiceByName("visualNetworkSolrService",
                    VisualizationGraphSolrService.class);

    private VisualizationGraphIndexer indexer = dspace.getServiceManager()
            .getServiceByName("visualNetworkIndexer",
                    VisualizationGraphIndexer.class);

    private ApplicationService applicationService = dspace.getServiceManager()
            .getServiceByName("applicationService", ApplicationService.class);

    private DecimalFormat df = new DecimalFormat("#.###");

    abstract public String getConnectionName();

    protected Integer getLimitLevel(Integer level)
    {
        String obj = ConfigurationManager.getProperty(NetworkPlugin.CFG_MODULE, "network.connection."
                + getConnectionName() + ".nodestoshoweachlevel." + level);
        if (obj == null)
        {
            return ConfigurationManager
                    .getIntProperty(NetworkPlugin.CFG_MODULE, "network.connection.nodestoshoweachlevel.default") + 1;
        }
        else
        {
            return Integer.parseInt(obj) + 1;
        }
    }
    
    public Integer getCustomMaxDepth()
    {
        String obj = ConfigurationManager.getProperty(NetworkPlugin.CFG_MODULE, "network.connection."
                + getConnectionName() + ".maxdepth");
        if (obj == null)
        {
            return ConfigurationManager
                    .getIntProperty(NetworkPlugin.CFG_MODULE, "network.connection.maxdepth");
        }
        else
        {
            return Integer.parseInt(obj);
        }
    }

    @Override
    public JsGraph search(String authority, String name, Integer level,
            boolean showExternal, boolean showSameDept, String dept,
            Integer modeEntity) throws Exception
    {

        SolrQuery solrQuery = new SolrQuery();

        String query = buildQuery(authority, name, showSameDept, dept,
                modeEntity, level);
        String[] fqs = {"type:" + getConnectionName(), "entity:" + modeEntity};
        solrQuery.setQuery(query);
        solrQuery.addFilterQuery(fqs);
	if (!showExternal && authority != null && !authority.isEmpty()) {
		solrQuery.addFilterQuery(new String[] {"a_auth:rp*", "b_auth:rp*" });
	}
        solrQuery.setFacet(true);
        solrQuery.addFacetField(FACET_SEARCH);
        if (modeEntity == ConstantNetwork.ENTITY_RP)
        {
            solrQuery.setFacetLimit(getLimitLevel(level));
        }
        else if (modeEntity == ConstantNetwork.ENTITY_DEPT)
        {
            solrQuery.setFacetLimit(Integer.MAX_VALUE);
        }
        solrQuery.setFacetMinCount(1);
        solrQuery.setRows(0);

        QueryResponse rsp = service.search(solrQuery);

        FacetField facets = rsp.getFacetField(FACET_SEARCH);

        JsGraph rsGraph = null;
        String src = null;
        if (authority != null && !authority.isEmpty())
        {
            src = authority;
            rsGraph = new JsGraph();
            rsGraph.setId(authority);
            rsGraph.setName(name);
            JsGraphNodeData dataNode = new JsGraphNodeData();
            dataNode.setColor(getNodeCustomColor());
            dataNode.setType(getType());
            dataNode.setModeStyle("fill");
            rsGraph.setData(dataNode);

        }
        else
        {
            src = name;
            rsGraph = new JsGraph();
            rsGraph.setId(name);
            rsGraph.setName(name);
            JsGraphNodeData dataNodeLeaf = new JsGraphNodeData();
            dataNodeLeaf.setColor(getNodeLeafCustomColor());            
            dataNodeLeaf.setType(getType());
            dataNodeLeaf.setModeStyle("stroke");
            rsGraph.setData(dataNodeLeaf);

        }
        if (rsGraph != null)
        {
            if (facets != null && facets.getValueCount() > 0)
            {
                for (Count facet : facets.getValues())
                {
                    if (facet.getCount() > 0)
                    {

                        String node2 = (String) facet.getName();
                        String split[] = node2.split("\\|\\|\\|");

                        String srcnode2 = null;
                        String displayValue = "";
                        String authorityValue = null;
                        boolean isAuthority = false;
                        
                        if (split.length > 1)
                        {
                            String[] splitAuthority = split[1].split(splitterAuthority);
                            
                            displayValue = splitAuthority[0];
                            srcnode2 = displayValue;
                            if (splitAuthority.length > 1)
                            {
                                isAuthority = true;
                                authorityValue = splitAuthority[1];
                                srcnode2 = authorityValue;
                            }                                                                               
                            
                        }               
                        else if (split.length == 1)
                        {
                            displayValue = split[0];
                            srcnode2 = displayValue;
                        }

                        if (!(src.equals(srcnode2)))
                        {
                            JsGraphAdjacence adjacence = new JsGraphAdjacence();
                            JsGraphData data = new JsGraphData();

                            adjacence.setSrc(node2);

                            if (isAuthority)
                            {
                                adjacence.setNodeTo(authorityValue);
                            }
                            else
                            {
                                adjacence.setNodeTo(displayValue);
                            }

                            if (authorityValue != null || showExternal)
                            {
                                data.setColor(getEdgeCustomColor());
                                data.setLineWidth(getCustomLineWidth((int) facet
                                        .getCount()));
                                data.setCount((int) facet.getCount());
                                data.setType(getType());
                                adjacence.setData(data);

                                rsGraph.getAdjacencies().add(adjacence);

                            }
                        }
                    }
                }
            }

        }
        return rsGraph;
    }

    private String buildQuery(String authority, String name,
            boolean showSameDept, String dept, Integer modeEntity, Integer level)
    {
        String query = "";
        if (authority != null && !authority.isEmpty())
        {
            query += "focus_auth:"
                    + authority;
        }
        else
        {
            query += "focus_val:\""
                    + ClientUtils.escapeQueryChars(name) + "\"";
        }

        

        if (showSameDept)
        {
            if (modeEntity == ConstantNetwork.ENTITY_RP)
            {
                query += " AND focus_dept:\""
                        + ClientUtils.escapeQueryChars(dept) + "|||"
                        + ClientUtils.escapeQueryChars(dept) + "\"";
            }
        }
        return query;
    }

    public DSpace getDspace()
    {
        return dspace;
    }

    public void setDspace(DSpace dspace)
    {
        this.dspace = dspace;
    }

    public VisualizationGraphSolrService getService()
    {
        return service;
    }

    public void setService(VisualizationGraphSolrService service)
    {
        this.service = service;
    }

    public VisualizationGraphIndexer getIndexer()
    {
        return indexer;
    }

    public void setIndexer(VisualizationGraphIndexer indexer)
    {
        this.indexer = indexer;
    }

    protected String buildExtra(String extra)
    {
        String result = buildExtraCustom(extra);
        if (result != null)
        {
            return result;
        }
        return extra;
    }

    protected abstract String buildExtraCustom(String extra);

    protected void buildRow(List<VisualizationGraphNode> result, String a,
            String a_authority, String a_value, String b, String b_authority,
            String b_value, List<String> values, String extra, String a_dept,
            String b_dept, Integer entity) throws NoSuchAlgorithmException
    {

        VisualizationGraphNode node = new VisualizationGraphNode();
        node.setA(a);
        node.setA_auth(a_authority);
        node.setFavalue(a_value);
        node.setType(getConnectionName());
        node.setB(b);
        node.setB_auth(b_authority);
        node.setFbvalue(b_value);
        node.getValue().addAll(values);
        node.getExtra().add(extra);
        node.setA_dept(a_dept);
        node.setB_dept(b_dept);        
        node.setEntity(entity);
        result.add(node);
    }

    public String getCustomLineWidth(int val)
    {
        if (val > 50)
        {
            return "5";
        }
        else if (val > 30)
        {
            return "3";
        }
        else if (val > 10)
        {
            return "2";
        }
        else if (val > 5)
        {
            return "1.5";
        }
        return "1";
    }

    public String getNodeLeafCustomColor()
    {
        return getNodeCustomColor();
    }

    public String getNodeCustomColor()
    {
        String color = ConfigurationManager.getProperty(NetworkPlugin.CFG_MODULE, COLOR_PROPERTY + "."
                + COLOR_SUFFIX_NODE + "." + getConnectionName());
        if (color == null)
        {
            color = ConfigurationManager.getProperty(NetworkPlugin.CFG_MODULE, COLOR_PROPERTY + "."
                    + COLOR_SUFFIX_NODE + ".default");
        }
        return color;
    }

    public String getEdgeColorToOverride()
    {
        String color = ConfigurationManager.getProperty(NetworkPlugin.CFG_MODULE, COLOR_PROPERTY + "."
                + COLOR_SUFFIX_EDGETOO + "." + getConnectionName());
        if (color == null)
        {
            color = ConfigurationManager.getProperty(NetworkPlugin.CFG_MODULE, COLOR_PROPERTY + "."
                    + COLOR_SUFFIX_EDGETOO + ".default");
            if (color == null)
            {
                color = ConfigurationManager.getProperty(NetworkPlugin.CFG_MODULE, COLOR_PROPERTY + "."
                        + COLOR_SUFFIX_EDGE + ".default");
            }
        }
        return color;
    }

    public String getEdgeCustomColor()
    {
        String color = ConfigurationManager.getProperty(NetworkPlugin.CFG_MODULE, COLOR_PROPERTY + "."
                + COLOR_SUFFIX_EDGE + "." + getConnectionName());
        if (color == null)
        {
            color = ConfigurationManager.getProperty(NetworkPlugin.CFG_MODULE, COLOR_PROPERTY + "."
                    + COLOR_SUFFIX_EDGE + ".default");
        }
        return color;
    }

    public List<ResearcherPage> loadMetrics(List<String[]> discardedNode,
            Integer importedNodes, Boolean otherError)
            throws SearchServiceException
    {
        // load all publications
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("type:" + getType() + " AND entity:"
                + ConstantNetwork.ENTITY_RP);
        solrQuery.addFacetField(FACET_METRICS);
        solrQuery.setFacetLimit(Integer.MAX_VALUE);
        solrQuery.setFacetMinCount(1);
        solrQuery.setRows(0);
        QueryResponse rsp = getService().search(solrQuery);
        FacetField facets = rsp.getFacetField(FACET_METRICS);

        // for each interests get authority's authors
        List<ResearcherPage> result = new LinkedList<ResearcherPage>();
        int counter = 0;
        external: for (Count facetElement : facets.getValues())
        {
            counter++;
            log.debug("" + counter + " of " + facets.getValueCount());
            ResearcherPage researcher = null;
            try
            {
                String facetValue = facetElement.getName();

                Integer realPersistentIdentifier = ResearcherPageUtils
                        .getRealPersistentIdentifier(facetValue, ResearcherPage.class);
                researcher = applicationService
                        .get(ResearcherPage.class, realPersistentIdentifier);
                //researcher.getDynamicField().setAnagraficaLazy(applicationService.getAnagraficaByRP(realPersistentIdentifier));
            
                solrQuery = new SolrQuery();
                solrQuery.setQuery("type:" + getType() + " AND entity:" + ConstantNetwork.ENTITY_RP + " AND " + FACET_METRICS + ":\"" + facetValue + "\"");
                solrQuery.addFacetField(FACET_SEARCH);
                solrQuery.setFacetMinCount(1);
                solrQuery.setFacetLimit(Integer.MAX_VALUE);
                solrQuery.setRows(0);

                rsp = getService().search(solrQuery);
                FacetField relations = rsp.getFacetField(FACET_SEARCH);
                int i = 0;
                int nConnections = 0;
                int maxStrength = 0;
                int sumStrength = 0;
                List<Long> quadraticVarianceArrays = new ArrayList<Long>();
                nConnections = Integer.valueOf(relations
                        .getValueCount() - 1);
                internal: for (Count relation : relations.getValues())
                {

                    log.debug("" + counter + " works on " + i + " of "
                            + relations.getValueCount());

                    if (i == 0)
                    {                     
                        i++;
                        continue internal;
                    }
                    else
                    {
                        if (i == 1)
                        {
                            // max
                            maxStrength = Integer.valueOf((int) relation
                                    .getCount());
                        }

                        sumStrength += Integer.valueOf((int) relation
                                .getCount());

                        quadraticVarianceArrays.add(relation
                                .getCount());
                    }

                    i++;

                }
                                               
                                
                
                RPAdditionalFieldStorage anagraficaObject = researcher.getDynamicField();
                
                
                setMetadata(String.valueOf(nConnections), anagraficaObject, ConstantNetwork.PREFIX_METADATA_BIBLIOMETRIC_1
                        + getType());

                setMetadata(String.valueOf(maxStrength), anagraficaObject, ConstantNetwork.PREFIX_METADATA_BIBLIOMETRIC_2
                        + getType());
                
                double averageStrength = ((double)sumStrength/((double)nConnections));                
                setMetadata(String.valueOf(df.format(averageStrength)), anagraficaObject, ConstantNetwork.PREFIX_METADATA_BIBLIOMETRIC_3
                        + getType());
                
                double quadraticVariance = 0;
                double n = quadraticVarianceArrays.size();
                for(Long variance : quadraticVarianceArrays) {
                    quadraticVariance += ((variance - averageStrength) * (variance - averageStrength));
                }
                quadraticVariance = Math.sqrt(quadraticVariance/n);
                setMetadata(String.valueOf(df.format(quadraticVariance)), anagraficaObject, ConstantNetwork.PREFIX_METADATA_BIBLIOMETRIC_4
                        + getType());
                                                
                
                result.add(researcher);
            }
            catch (Exception e)
            {
                log.error("Error try to build object to index with ID:" + researcher.getId());
                log.error(e.getMessage(), e);
                otherError = true;
                continue;
            }
            
        }
        
        committnode: for (ResearcherPage node : result)
        {
//            try
//            {
                applicationService.saveOrUpdate(ResearcherPage.class, node);
                boolean rr = ((CrisSearchService) getIndexer().getIndexer()).indexCrisObject(node,
                        true); // index node
                if(rr) {
                	importedNodes++;
                }
                else {
                	 discardedNode.add(new String[] { getConnectionName() + " - " + node.getId()});
                }
//            }
//            catch (SolrServerException e)
//            {
//                log.error(e.getMessage(), e);
//                discardedNode.add(new String[] { getConnectionName() + " - " + node.getId()});
//                continue committnode;
//            }
//            catch (IOException e)
//            {
//                log.error(e.getMessage(), e);
//                discardedNode.add(new String[] { getConnectionName() + " - " + node.getId()});
//                continue committnode;
//            }
        }

        log.debug("commit " + getType());
        getIndexer().getIndexer().commit();
        return result;

    }

    private void setMetadata(String value,
            RPAdditionalFieldStorage anagraficaObject, String shortName)
    {
        List<RPProperty> nListConnectionRPP = anagraficaObject
                .getAnagrafica4view().get(
                        shortName);

        RPProperty nConnectionRPP = null;
        if (nListConnectionRPP != null && nListConnectionRPP.size() > 0)
        {
            List<RPProperty> toRemove = new LinkedList<RPProperty>(); 
            for (RPProperty p : nListConnectionRPP)
            {
                toRemove.add(p);
            }   
            
            for(RPProperty to : toRemove) {
                anagraficaObject.removeProprieta(to);
            }
            //            nConnectionRPP = nListConnectionRPP.get(0);
        }
//        else
//        {
            nConnectionRPP = anagraficaObject
                    .createProprieta(applicationService
                            .findPropertiesDefinitionByShortName(
                                    RPPropertiesDefinition.class,
                                    shortName));
//        }

        TextValue nConnectionValue = new TextValue();
        nConnectionValue.setOggetto(value);
        nConnectionRPP.setValue(nConnectionValue);
        nConnectionRPP.setVisibility(VisibilityConstants.PUBLIC);
    }
    
    protected String getDepartmentFromSOLR(String a_authority) throws SearchServiceException
    {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("crisrp.this_authority:" + a_authority);
        solrQuery.setFields("dept_authority");        
        solrQuery.setRows(1);
        QueryResponse rsp = getService().getSearcher().search(solrQuery);
        SolrDocumentList publications = rsp.getResults();
        
        Iterator<SolrDocument> iter = publications.iterator();

        String rp_dept = "";
        while (iter.hasNext())
        {
            SolrDocument publication = iter.next();

            rp_dept = (String) publication
                    .getFirstValue("dept_authority");
            break;
        }
        
        return rp_dept;
    }

    protected String getStatusFromSOLR(String a_authority) throws SearchServiceException
    {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("crisrp.this_authority:" + a_authority);
        solrQuery.setFields("disabled");        
        solrQuery.setRows(1);
        QueryResponse rsp = getService().getSearcher().search(solrQuery);
        SolrDocumentList publications = rsp.getResults();
        
        Iterator<SolrDocument> iter = publications.iterator();

        String rp_status = "";
        while (iter.hasNext())
        {
            SolrDocument publication = iter.next();

            rp_status = (String) publication
                    .getFirstValue("disabled");
            break;
        }
        
        return rp_status;
    }

}
