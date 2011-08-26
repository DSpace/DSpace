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
public class ReMSerialisation
{
    private URI uri;

    private String mimeType;

	private boolean authoritative;

	public ReMSerialisation() { }

    public ReMSerialisation(String mimeType, URI uri)
    {
        this.setURI(uri);
        this.setMimeType(mimeType);
    }

    public URI getURI()
    {
        return uri;
    }

    public void setURI(URI uri)
    {
        this.uri = uri;
    }

    public String getMimeType()
    {
        return mimeType;
    }

    public void setMimeType(String mimeType)
    {
        this.mimeType = mimeType;
    }

	public boolean isAuthoritative()
	{
		return authoritative;
	}

	public void setAuthoritative(boolean authoritative)
	{
		this.authoritative = authoritative;
	}
}
