/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.foresite;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @Author Richard Jones
 */
public enum OREVocabulary
{
    aggregation("Aggregation", "http://www.openarchives.org/ore/terms/Aggregation");

    private String label;

    private URI uri;

    OREVocabulary(String label, String uri)
    {
        try
        {
            this.label = label;
            this.uri = new URI(uri);
        }
        catch (URISyntaxException e)
        {
            // it's our job to make sure this doesn't happen in the enum
        }
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public URI getUri()
    {
        return uri;
    }

    public void setUri(URI uri)
    {
        this.uri = uri;
    }
}
