/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.foresite;

import org.dspace.foresite.atom.AtomORESerialiser;
import org.dspace.foresite.jena.JenaORESerialiser;
import org.dspace.foresite.rdfa.RDFaORESerialiser;

import java.util.Properties;

/**
 * @Author Richard Jones
 */
public class ORESerialiserFactory
{
    public static ORESerialiser getInstance(String desc)
    {
        if ("RDF/XML".equals(desc))
        {
            Properties properties = new Properties();
            properties.setProperty("type", "RDF/XML");
            ORESerialiser s = new JenaORESerialiser();
            s.configure(properties);
            return s;
        }
        if ("N-TRIPLE".equals(desc))
        {
            Properties properties = new Properties();
            properties.setProperty("type", "N-TRIPLE");
            ORESerialiser s = new JenaORESerialiser();
            s.configure(properties);
            return s;
        }
        if ("RDF/XML-ABBREV".equals(desc))
        {
            Properties properties = new Properties();
            properties.setProperty("type", "RDF/XML-ABBREV");
            ORESerialiser s = new JenaORESerialiser();
            s.configure(properties);
            return s;
        }
        if ("N3".equals(desc))
        {
            Properties properties = new Properties();
            properties.setProperty("type", "N3");
            ORESerialiser s = new JenaORESerialiser();
            s.configure(properties);
            return s;
        }
		if ("TURTLE".equals(desc))
        {
            Properties properties = new Properties();
            properties.setProperty("type", "TURTLE");
            ORESerialiser s = new JenaORESerialiser();
            s.configure(properties);
            return s;
        }
		if ("RDFa".equals(desc))
        {
            return new RDFaORESerialiser();
        }
        if ("ATOM-1.0".equals(desc))
        {
            return new AtomORESerialiser();
        }
        return null;
    }
}
