/*
 * DIMDisseminationCrosswalk.java
 *
 * Version: $Revision: 1 $
 *
 * Date: $Date: 2007-07-30 12:26:50 -0500 (Mon, 30 Jul 2007) $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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

package org.dspace.content.crosswalk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.packager.PackageDisseminator;
import org.dspace.content.packager.PackageException;
import org.dspace.content.packager.PackageParameters;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Utils;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * DIM dissemination crosswalk
 * <p>
 * Produces the metadata encoded in DSpace Intermediate Format, without the overhead of XSLT processing.
 *
 * @author Alexey Maslov
 * @version $Revision: 1 $
 */
public class DIMDisseminationCrosswalk
    implements DisseminationCrosswalk
{
    /** log4j category */ 
    private static Logger log = Logger.getLogger(OREDisseminationCrosswalk.class);

    // Non-existant XSD schema
    public static final String DIM_XSD = "null";
    
    // Namespaces 
    public static final Namespace DIM_NS =
        Namespace.getNamespace("dim", "http://www.dspace.org/xmlns/dspace/dim");

    private static final Namespace namespaces[] = { DIM_NS };

    private static XMLOutputter outputUgly = new XMLOutputter();
    private static XMLOutputter outputPretty = new XMLOutputter(Format.getPrettyFormat());
    private static SAXBuilder builder = new SAXBuilder();

    public Namespace[] getNamespaces()
    {
        return namespaces;
    }

    /* No schema for DIM */ 
    public String getSchemaLocation()
    {
        return DIM_NS.getURI() + " " + DIM_XSD;
    }

    
    public Element disseminateElement(DSpaceObject dso)	throws CrosswalkException, IOException, SQLException, AuthorizeException 
	{
    	if (dso.getType() != Constants.ITEM)
            throw new CrosswalkObjectNotSupported("DIMDisseminationCrosswalk can only crosswalk an Item.");
        Item item = (Item)dso;
        
    	DCValue[] dc = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        Element dim = new Element("dim", DIM_NS);
        for (int i = 0; i < dc.length; i++)
        {
            Element field = new Element("field", DIM_NS);
            field.setAttribute("mdschema", dc[i].schema);
            field.setAttribute("element", dc[i].element);
            if (dc[i].qualifier != null)
                field.setAttribute("qualifier", dc[i].qualifier);
            if (dc[i].language != null)
                field.setAttribute("lang", dc[i].language);
            if (dc[i].value != null)
                field.setText(dc[i].value);
            dim.addContent(field);
        }
        return dim;
	}
   
    public List disseminateList(DSpaceObject dso) throws CrosswalkException, IOException, SQLException, AuthorizeException
	{
	    List result = new ArrayList(1);
	    result.add(disseminateElement(dso));
	    return result;
	}

    /* Only interested in disseminating items at this time */
    public boolean canDisseminate(DSpaceObject dso)
    {
    	if (dso.getType() == Constants.ITEM)
    		return true;
    	else
    		return false;
    }

    public boolean preferList()
    {
        return false;
    }
	
}
