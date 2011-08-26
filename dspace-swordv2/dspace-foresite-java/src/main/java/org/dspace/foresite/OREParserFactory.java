/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.foresite;

import org.dspace.foresite.atom.AtomOREParser;
import org.dspace.foresite.jena.JenaOREParser;

import java.util.Properties;

/**
 * @Author Richard Jones
 */
public class OREParserFactory
{
    public static OREParser getInstance(String desc)
    {
        if ("RDF/XML".equals(desc))
        {
            Properties properties = new Properties();
            properties.setProperty("type", "RDF/XML");
            OREParser s = new JenaOREParser();
            s.configure(properties);
            return s;
        }
        if ("N-TRIPLE".equals(desc))
        {
            Properties properties = new Properties();
            properties.setProperty("type", "N-TRIPLE");
            OREParser s = new JenaOREParser();
            s.configure(properties);
            return s;
        }
        if ("RDF/XML-ABBREV".equals(desc))
        {
            Properties properties = new Properties();
            properties.setProperty("type", "RDF/XML-ABBREV");
            OREParser s = new JenaOREParser();
            s.configure(properties);
            return s;
        }
        if ("N3".equals(desc))
        {
            Properties properties = new Properties();
            properties.setProperty("type", "N3");
            OREParser s = new JenaOREParser();
            s.configure(properties);
            return s;
        }
        if ("ATOM-1.0".equals(desc))
        {
            return new AtomOREParser();
        }
        return null;
    }
}
