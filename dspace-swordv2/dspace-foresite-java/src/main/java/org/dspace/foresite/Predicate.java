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
public class Predicate
{
    private URI uri;
    
    private String namespace;

    private String prefix;

    private String name;

    /*
    public String toString()
    {
        return prefix + ":" + name;
    }*/

    public URI getURI()
    {
        return uri;
    }

    public void setURI(URI uri)
    {
        this.uri = uri;
    }

    public String getNamespace()
    {
        return namespace;
    }

    public void setNamespace(String namespace)
    {
        this.namespace = namespace;
    }

    public String getPrefix()
    {
        return prefix;
    }

    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}
