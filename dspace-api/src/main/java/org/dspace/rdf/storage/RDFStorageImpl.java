/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.rdf.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.http.auth.AuthEnv;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;
import org.apache.logging.log4j.Logger;
import org.dspace.rdf.RDFUtil;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 */
public class RDFStorageImpl
    implements RDFStorage {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(RDFStorageImpl.class);

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    @Override
    public void store(String uri, Model model) {
        RDFConnection connection = this.getConnection();
        connection.put(uri, model);
    }

    @Override
    public Model load(String uri) {
        RDFConnection connection = this.getConnection();
        return connection.fetch(uri);
    }

    protected RDFConnection getConnection() {
        RDFConnection connection;
        if (configurationService.hasProperty(RDFUtil.STORAGE_GRAPHSTORE_LOGIN_KEY)
            && configurationService.hasProperty(RDFUtil.STORAGE_GRAPHSTORE_PASSWORD_KEY)) {
            AuthEnv.get()
                   .registerUsernamePassword(getGraphStoreEndpoint(),
                                             configurationService.getProperty(RDFUtil.STORAGE_GRAPHSTORE_LOGIN_KEY),
                                             configurationService.getProperty(RDFUtil.STORAGE_GRAPHSTORE_PASSWORD_KEY));
        } else {
            log.debug("Did not found credential to use for our connection to the "
                          + "Graph Store HTTP endpoint, trying to connect unauthenticated.");
        }
        connection = RDFConnectionRemote.service(getGraphStoreEndpoint()).build();
        return connection;
    }

    @Override
    public void delete(String uri) {
        this.getConnection().delete(uri);
    }

    @Override
    public void deleteAll() {
        for (String graph : this.getAllStoredGraphs()) {
            this.delete(graph);
        }
        // clean default graph:
        this.getConnection().delete();
    }

    @Override
    public List<String> getAllStoredGraphs() {
        String queryString = "SELECT DISTINCT ?g WHERE { GRAPH ?g { ?s ?p ?o } }";
        if (configurationService.hasProperty(RDFUtil.STORAGE_SPARQL_LOGIN_KEY)
            && configurationService.hasProperty(RDFUtil.STORAGE_SPARQL_PASSWORD_KEY)) {
            AuthEnv.get()
                   .registerUsernamePassword(getSparqlEndpoint(),
                                             configurationService.getProperty(RDFUtil.STORAGE_SPARQL_LOGIN_KEY),
                                             configurationService.getProperty(RDFUtil.STORAGE_GRAPHSTORE_PASSWORD_KEY));
        }

        List<String> graphs = Collections.synchronizedList(new ArrayList<>());
        try (QueryExecution qexec = QueryExecutionHTTP.service(getSparqlEndpoint()).queryString(queryString).build()) {
            ResultSet rs = qexec.execSelect();
            while (rs.hasNext()) {
                QuerySolution solution = rs.next();
                if (solution.contains("g")) {
                    graphs.add(solution.get("g").asResource().getURI());
                }
            }
        }
        return graphs;
    }

    protected String getGraphStoreEndpoint() {
        String endpoint = configurationService.getProperty(RDFUtil.STORAGE_GRAPHSTORE_ENDPOINT_KEY);
        if (StringUtils.isEmpty(endpoint)) {
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

    protected String getSparqlEndpoint() {
        // Lets see if a SPARQL endpoint is defined to be used by RDFStorageImpl
        String endpoint = configurationService.getProperty(RDFUtil.STORAGE_SPARQL_ENDPOINT_KEY);
        if (StringUtils.isEmpty(endpoint)) {
            // try to load the public sparql endpoint
            endpoint = configurationService.getProperty(RDFUtil.SPARQL_ENDPOINT_KEY);
        }
        // check if we found an endpoint
        if (StringUtils.isEmpty(endpoint)) {
            log.warn("Cannot load internal or public SPARQL endpoint!");
            throw new RuntimeException("Cannot load internal or public SPARQL "
                                           + "endpoint!");
        }
        return endpoint;
    }
}
