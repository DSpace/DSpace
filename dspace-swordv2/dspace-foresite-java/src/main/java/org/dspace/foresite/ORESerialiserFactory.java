/*
 * ORESerialiserFactory.java
 *
 * Copyright (c) 2008, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
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
