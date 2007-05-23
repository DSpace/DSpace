/*
 * SimpleDCDisseminationCrosswalk.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
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

import java.io.OutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.core.Constants;
import org.dspace.content.Item;
import org.dspace.content.DSpaceObject;
import org.dspace.content.DCValue;
import org.dspace.core.SelfNamedPlugin;
import org.dspace.authorize.AuthorizeException;

import org.jdom.*;
//import org.jdom.output.XMLOutputter;
//import org.jdom.output.Format;

/**
 * Disseminator for Simple Dublin Core metadata in XML format.
 * Logic stolen from OAIDCCrosswalk.  This is mainly intended
 * as a proof-of-concept, to use crosswalk plugins in the OAI-PMH
 * server.
 *
 * @author Larry Stone
 * @version $Revision$
 */
public class SimpleDCDisseminationCrosswalk extends SelfNamedPlugin
    implements DisseminationCrosswalk
{
    // namespaces of interest.

    // XXX FIXME: may also want http://www.openarchives.org/OAI/2.0/oai_dc/  for OAI

    private static final Namespace DC_NS =
        Namespace.getNamespace("dc", "http://purl.org/dc/elements/1.1/");

    // simple DC schema for OAI
    private static final String DC_XSD =
                "http://dublincore.org/schemas/xmls/simpledc20021212.xsd";
                //"http://www.openarchives.org/OAI/2.0/oai_dc.xsd";

    private static final String schemaLocation =
        DC_NS.getURI()+" "+DC_XSD;

    private static final Namespace namespaces[] =
        { DC_NS, XSI_NS };

    private final static String aliases[] = { "SimpleDC", "DC" };

    public static String[] getPluginNames()
    {
        return aliases;
    }

    public Element disseminateElement(DSpaceObject dso)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        Element root = new Element("simpledc", DC_NS);
        root.setAttribute("schemaLocation", schemaLocation, XSI_NS);
        root.addContent(disseminateListInternal(dso, false));
        return root;
    }

    /**
     * Returns object's metadata as XML elements.
     * Simple-minded copying of elements: convert contributor.author to
     * "creator" but otherwise just grab element name without qualifier.
     */
    public List disseminateList(DSpaceObject dso)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        return disseminateListInternal(dso, true);
    }

    public List disseminateListInternal(DSpaceObject dso, boolean addSchema)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        if (dso.getType() != Constants.ITEM)
            throw new CrosswalkObjectNotSupported("SimpleDCDisseminationCrosswalk can only crosswalk an Item.");

        Item item = (Item)dso;
        DCValue[] allDC = item.getDC(Item.ANY, Item.ANY, Item.ANY);

        List dcl = new ArrayList(allDC.length);

        for (int i = 0; i < allDC.length; i++)
        {
            // Do not include description.provenance
            if (!(allDC[i].element.equals("description") &&
                  (allDC[i].qualifier != null && allDC[i].qualifier.equals("provenance"))))
            {
                String element;

                // contributor.author exposed as 'creator'
                if (allDC[i].element.equals("contributor")
                        && (allDC[i].qualifier != null)
                        && allDC[i].qualifier.equals("author"))
                    element = "creator";
                else
                    element = allDC[i].element;
                Element field = new Element(element, DC_NS);
                field.addContent(allDC[i].value);
                if (addSchema)
                    field.setAttribute("schemaLocation", schemaLocation, XSI_NS);
                dcl.add(field);
            }
        }
        return dcl;
    }

    public Namespace[] getNamespaces()
    {
        return namespaces;
    }

    public String getSchemaLocation()
    {
        return schemaLocation;
    }

    public boolean canDisseminate(DSpaceObject dso)
    {
        return dso.getType() == Constants.ITEM;
    }

    public boolean preferList()
    {
        return true;
    }
}
