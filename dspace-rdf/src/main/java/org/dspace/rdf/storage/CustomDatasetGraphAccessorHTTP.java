/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.rdf.storage;

import com.hp.hpl.jena.graph.Graph;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.riot.web.HttpCaptureResponse;
import org.apache.jena.riot.web.HttpOp;
import org.apache.jena.riot.web.HttpResponseLib;
import org.apache.jena.web.DatasetGraphAccessorHTTP;
import org.apache.jena.web.HttpSC;

/*
 * Jena (at least until version 2.11.1) uses N-TRIPLES to load Graphs.
 * N-TRIPLES don't support prefixes. This class extend 
 * org.apache.jena.web.DataSetGraphAccessorHTTP to set the HTTP Accept Header 
 * to prefer TURTLE instead of N-TRIPLES. Using TURTLE to load graphs, prefix 
 * mappings will be loaded with the graphs.
 * 
 * This class is neccessary until JENA-663 is resolved
 * (https://issues.apache.org/jira/browse/JENA-663).
 * 
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 */
public class CustomDatasetGraphAccessorHTTP extends DatasetGraphAccessorHTTP {
    
    /*
     * As org.apache.jena.web.DatasetGraphAccessorHTTP is private, we have to
     * override it (it is used in HTTPGet).
     */
    private HttpAuthenticator authenticator ;

    public CustomDatasetGraphAccessorHTTP(String remote) {
        super(remote);
    }
    
    public CustomDatasetGraphAccessorHTTP(String remote, HttpAuthenticator authenticator)
    {
        super(remote, authenticator);
        this.authenticator = authenticator;
    }
    
    /**
     * Sets an authenticator to use for authentication to the remote URL
     * 
     * @param authenticator
     *            Authenticator
     */
    public void setAuthenticator(HttpAuthenticator authenticator) {
        super.setAuthenticator(authenticator);
        this.authenticator = authenticator ;
    }
    
    /**
     * Accept header for fetching graphs - prefer TURTLE
     * 
     * @See WebContent.defaultGraphAcceptHeader
     * 
     */
    private static String GetAcceptHeader =
            "text/turtle,application/n-triples;q=0.9,application/rdf+xml;q=0.8,application/xml;q=0.7";


    /*
     * This method actually does the HTTPGet. Override it to 
     */
    protected Graph doGet(String url) {
        HttpCaptureResponse<Graph> graph = HttpResponseLib.graphHandler() ;
        try {
            HttpOp.execHttpGet(url, GetAcceptHeader, graph, this.authenticator) ;
        } catch (HttpException ex) {
            if ( ex.getResponseCode() == HttpSC.NOT_FOUND_404 )
                return null ;
            throw ex ;
        }
        return graph.get() ;
    }
}