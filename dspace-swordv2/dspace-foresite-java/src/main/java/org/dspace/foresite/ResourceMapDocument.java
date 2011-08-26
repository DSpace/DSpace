/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.foresite;

import java.net.URI;

/**
 * @Author Richard Jones
 */
public class ResourceMapDocument
{
    private String serialisation;

    private String mimeType;

    private URI uri;

    public String toString()
    {
        return this.serialisation;    
    }

    public String getSerialisation() {
        return serialisation;
    }

    public void setSerialisation(String serialisation) {
        this.serialisation = serialisation;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
}
