/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

import static java.nio.charset.Charset.defaultCharset;

import org.springframework.http.MediaType;

/**
 *
 */
public class RdfMediaType {

    public final static MediaType APPLICATION_JSON_LD = new MediaType("application", "ld+json", defaultCharset());
    public final static MediaType APPLICATION_N_TRIPLES = new MediaType("application", "n-triples", defaultCharset());
    public final static MediaType APPLICATION_RDF_XML = new MediaType("application", "rdf+xml", defaultCharset());
    public final static MediaType APPLICATION_RDF_JSON = new MediaType("application", "rdf+json", defaultCharset());
    public final static MediaType APPLICATION_X_TURTLE = new MediaType("application", "x-turtle", defaultCharset());

    public final static MediaType TEXT_TURTLE = new MediaType("text", "turtle", defaultCharset());
    public final static MediaType TEXT_N3 = new MediaType("text", "n3", defaultCharset());
    public final static MediaType TEXT_RDF_N3 = new MediaType("text", "rdf+n3", defaultCharset());

    /**
     * 
     */
    private RdfMediaType() {

    }

}