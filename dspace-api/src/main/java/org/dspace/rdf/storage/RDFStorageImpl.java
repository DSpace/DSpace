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
import org.dspace.rdf.RDFConfiguration;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

/**
 *
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 */
public class RDFStorageImpl
implements RDFStorage
{
    private static final Logger log = Logger.getLogger(RDFStorageImpl.class);
    
    private final String GRAPHSTORE_ENDPOINT;
    private final String GRAPHSTORE_LOGIN;
    private final String GRAPHSTORE_PASSWORD;
    private final String SPARQL_ENDPOINT;
    private final String SPARQL_LOGIN;
    private final String SPARQL_PASSWORD;
    
    private ConfigurationService configurationService;
    
    public RDFStorageImpl()
    {
        this.configurationService = new DSpace().getConfigurationService();
        
        this.GRAPHSTORE_ENDPOINT = this.configurationService
                .getProperty(RDFConfiguration.STORAGE_GRAPHSTORE_ENDPOINT_KEY);
        if (StringUtils.isEmpty(this.GRAPHSTORE_ENDPOINT))
        {
            log.warn("Cannot load Graph Store HTTP Protocol endpoint! Property "
                    + RDFConfiguration.STORAGE_GRAPHSTORE_ENDPOINT_KEY + " does not "
                    + "exist or is empty.");
            throw new RuntimeException("Cannot load Graph Store HTTP Protocol "
                    + "endpoint! Property " 
                    + RDFConfiguration.STORAGE_GRAPHSTORE_ENDPOINT_KEY + " does not "
                    + "exist or is empty.");
        }
        
        boolean graphstore_use_auth = this.configurationService.getPropertyAsType(
                RDFConfiguration.STORAGE_GRAPHSTORE_AUTHENTICATION_KEY, false);
        String graphstore_login = this.configurationService.getProperty(
                RDFConfiguration.STORAGE_GRAPHSTORE_LOGIN_KEY);
        String graphstore_password = this.configurationService.getProperty(
                RDFConfiguration.STORAGE_GRAPHSTORE_PASSWORD_KEY);
        if (!graphstore_use_auth 
                || (graphstore_use_auth && StringUtils.isEmpty(graphstore_login))
                || (graphstore_use_auth && StringUtils.isEmpty(graphstore_password)))
        {
            this.GRAPHSTORE_LOGIN = null;
            this.GRAPHSTORE_PASSWORD = null;
            if (graphstore_use_auth)
            {
                log.warn("The rdf storage is configured to use authentication "
                        + "to connect to the Graph Store HTTP Protocol endpoint, "
                        + "but no credentials are configured.");
            }
        } else {
            this.GRAPHSTORE_LOGIN = graphstore_login;
            this.GRAPHSTORE_PASSWORD = graphstore_password;
        }
        
        this.SPARQL_ENDPOINT = RDFConfiguration.getInternalSparqlEndpointAddress();
        if (StringUtils.isEmpty(this.SPARQL_ENDPOINT))
        {
            log.warn("Cannot load internal or public SPARQL endpoint!");
            throw new RuntimeException("Cannot load internal or public SPARQL "
                    + "endpoint!");
        }

        boolean sparql_use_auth = this.configurationService.getPropertyAsType(
                RDFConfiguration.STORAGE_SPARQL_AUTHENTICATION_KEY, false);
        String sparql_login = this.configurationService.getProperty(
                RDFConfiguration.STORAGE_SPARQL_LOGIN_KEY);
        String sparql_password = this.configurationService.getProperty(
                RDFConfiguration.STORAGE_SPARQL_PASSWORD_KEY);
        if (!sparql_use_auth 
                || (sparql_use_auth && StringUtils.isEmpty(sparql_login))
                || (sparql_use_auth && StringUtils.isEmpty(sparql_password)))
        {
            this.SPARQL_LOGIN = null;
            this.SPARQL_PASSWORD = null;
            if (sparql_use_auth)
            {
                log.warn("The rdf storage is configured to use authentication "
                        + "for sparql quries, but no credentials are configured.");
            }
        } else {
            this.SPARQL_LOGIN = sparql_login;
            this.SPARQL_PASSWORD = sparql_password;
        }
    }
    
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
        if (this.GRAPHSTORE_LOGIN != null)
        {
            HttpAuthenticator httpAuthenticator = new SimpleAuthenticator(
                    GRAPHSTORE_LOGIN, GRAPHSTORE_PASSWORD.toCharArray());
            accessor = new DatasetGraphAccessorHTTP(GRAPHSTORE_ENDPOINT,
                    httpAuthenticator);
        } else {
            accessor = new DatasetGraphAccessorHTTP(GRAPHSTORE_ENDPOINT);
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
        if (this.SPARQL_LOGIN != null)
        {
            HttpAuthenticator httpAuthenticator = new SimpleAuthenticator(
                    SPARQL_LOGIN, SPARQL_PASSWORD.toCharArray());
            qexec = QueryExecutionFactory.sparqlService(SPARQL_ENDPOINT, 
                    queryString, httpAuthenticator);
        } else {
            qexec = QueryExecutionFactory.sparqlService(SPARQL_ENDPOINT,
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
        /*
        } catch (QueryExceptionHTTP ex)
        {
            System.err.println("== QUERYEXCEPTIONHTTP ==");
            System.err.println(ex.getMessage());
            System.err.println(ex.getResponseCode() + ": " + ex.getResponseMessage());
            Throwable cause = ex.getCause();
            int i = 1;
            while (cause != null)
            {
                System.err.println("Cause " + i + " '" + cause.getClass().getName() + "': " + cause.getMessage());
                cause = cause.getCause();
                i++;
            }
            ex.printStackTrace(System.err);
            throw new RuntimeException(ex);
        }*/
    }
}
