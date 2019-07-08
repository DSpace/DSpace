/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.resourcesync;

/**
 * @author Richard Jones
 *
 */
public class MetadataFormat
{
    private String prefix;
    private String namespace;
    private String mimetype;

    public MetadataFormat(String prefix, String namespace, String mimetype)
    {
        this.prefix = prefix;
        this.namespace = namespace;
        this.mimetype = mimetype;
    }

    public String getPrefix()
    {
        return prefix;
    }

    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }

    public String getNamespace()
    {
        return namespace;
    }

    public void setNamespace(String namespace)
    {
        this.namespace = namespace;
    }

    public String getMimetype()
    {
        return mimetype;
    }

    public void setMimetype(String mimetype)
    {
        this.mimetype = mimetype;
    }
}
