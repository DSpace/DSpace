/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.rdf.storage;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.GraphStoreFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.atlas.web.auth.SimpleAuthenticator;
import org.apache.jena.web.DatasetGraphAccessor;
import org.apache.jena.web.DatasetGraphAccessorHTTP;
import org.apache.log4j.Logger;
import org.dspace.rdf.RDFUtil;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 */
public class RDFStorageImpl
implements RDFStorage
{
    private static final Logger log = Logger.getLogger(RDFStorageImpl.class);

    @Autowired(required=true)
    protected ConfigurationService configurationService;
    
    @Override
    public void store(String uri, Model model)
    {
        Node graphNode = NodeFactory.createURI(uri);
        DatasetGraphAccessor accessor = this.getAccessor();
        Dataset ds = DatasetFactory.create(model);
        DatasetGraph dsg = ds.asDatasetGraph();
        Graph g = dsg.getDefaultGraph();
        accessor.httpPut(graphNode, g);
    }
    
    @Override
    public Model load(String uri)
    {
        Node graphNode = NodeFactory.createURI(uri);
        DatasetGraphAccessor accessor = this.getAccessor();
        Graph g = accessor.httpGet(graphNode);
        if (g == null || g.isEmpty())
        {
            return null;
        }
        GraphStore gs = GraphStoreFactory.create(g);
        Dataset ds = gs.toDataset();
        Model m = ds.getDefaultModel();
        return m;
    }
    
    protected DatasetGraphAccessor getAccessor()
    {
        DatasetGraphAccessor accessor;
        if (configurationService.hasProperty(RDFUtil.STORAGE_GRAPHSTORE_LOGIN_KEY)
                && configurationService.hasProperty(RDFUtil.STORAGE_GRAPHSTORE_PASSWORD_KEY))
        {
            HttpAuthenticator httpAuthenticator = new SimpleAuthenticator(
                    configurationService.getProperty(RDFUtil.STORAGE_GRAPHSTORE_LOGIN_KEY),
                    configurationService.getProperty(RDFUtil.STORAGE_GRAPHSTORE_PASSWORD_KEY).toCharArray());
            accessor = new DatasetGraphAccessorHTTP(getGraphStoreEndpoint(),
                    httpAuthenticator);
        } else {
            log.debug("Did not found credential to use for our connection to the "
                    + "Graph Store HTTP endpoint, trying to connect unauthenticated.");
            accessor = new DatasetGraphAccessorHTTP(getGraphStoreEndpoint());
        }
        return accessor;
    }
    
    @Override
    public void delete(String uri) {
        this.getAccessor().httpDelete(NodeFactory.createURI(uri));
    }

    @Override
    public void deleteAll() {
        for (String graph : this.getAllStoredGraphs())
        {
            this.delete(graph);
        }
        // clean default graph:
        this.getAccessor().httpDelete();
    }
    
    @Override
    public List<String> getAllStoredGraphs() {
        String queryString = "SELECT DISTINCT ?g WHERE { GRAPH ?g { ?s ?p ?o } }";
        QueryExecution qexec;
        if (configurationService.hasProperty(RDFUtil.STORAGE_SPARQL_LOGIN_KEY)
                && configurationService.hasProperty(RDFUtil.STORAGE_SPARQL_PASSWORD_KEY))
        {
            HttpAuthenticator httpAuthenticator = new SimpleAuthenticator(
                    configurationService.getProperty(RDFUtil.STORAGE_SPARQL_LOGIN_KEY),
                    configurationService.getProperty(RDFUtil.STORAGE_GRAPHSTORE_PASSWORD_KEY).toCharArray());
            qexec = QueryExecutionFactory.sparqlService(getSparqlEndpoint(), 
                    queryString, httpAuthenticator);
        } else {
            qexec = QueryExecutionFactory.sparqlService(getSparqlEndpoint(),
                    queryString);
        }
        
        ResultSet rs = qexec.execSelect();
        List<String> graphs = Collections.synchronizedList(new ArrayList<String>());
        while (rs.hasNext())
        {
            QuerySolution solution = rs.next();
            if (solution.contains("g"))
            {
                graphs.add(solution.get("g").asResource().getURI());
            }
        }
        qexec.close();
        return graphs;
    }
    
    protected String getGraphStoreEndpoint()
    {
        String endpoint = configurationService.getProperty(RDFUtil.STORAGE_GRAPHSTORE_ENDPOINT_KEY);
        if (StringUtils.isEmpty(endpoint))
        {
            log.warn("Cannot load Graph Store HTTP Protocol endpoint! Property "
                    + RDFUtil.STORAGE_GRAPHSTORE_ENDPOINT_KEY + " does not "
                    + "exist or is empty.");
            throw new RuntimeException("Cannot load Graph Store HTTP Protocol "
                    + "endpoint! Property " 
                    + RDFUtil.STORAGE_GRAPHSTORE_ENDPOINT_KEY + " does not "
                    + "exist or is empty.");
        }
        return endpoint;
    }
    
    protected String getSparqlEndpoint()
    {
        // Lets see if a SPARQL endpoint is defined to be used by RDFStorageImpl
        String endpoint = configurationService.getProperty(RDFUtil.STORAGE_SPARQL_ENDPOINT_KEY);
        if (StringUtils.isEmpty(endpoint))
        {
            // try to load the public sparql endpoint
            endpoint = configurationService.getProperty(RDFUtil.SPARQL_ENDPOINT_KEY);
        }
        // check if we found an endpoint
        if (StringUtils.isEmpty(endpoint))
        {
            log.warn("Cannot load internal or public SPARQL endpoint!");
            throw new RuntimeException("Cannot load internal or public SPARQL "
                    + "endpoint!");
        }
        return endpoint;
    }
}
