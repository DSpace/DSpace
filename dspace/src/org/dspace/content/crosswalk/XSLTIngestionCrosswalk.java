/*
 * XSLTIngestionCrosswalk.java
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

import java.io.InputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Enumeration;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.File;
import java.io.FileInputStream;

import java.sql.SQLException;
import org.apache.log4j.Logger;

import org.dspace.core.Context;
import org.dspace.core.Constants;
import org.dspace.content.Item;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.SelfNamedPlugin;
import org.dspace.core.PluginManager;

import org.jdom.*;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.input.SAXBuilder;
import org.jdom.input.JDOMParseException;
import org.jdom.xpath.XPath;
import org.jdom.transform.XSLTransformer;
import org.jdom.transform.XSLTransformException;

/**
 * Configurable XSLT-driven ingestion Crosswalk
 * <p>
 * See the XSLTCrosswalk superclass for details on configuration.
 *
 * @author Larry Stone
 * @version $Revision$
 * @see XSLTCrosswalk
 */
public class XSLTIngestionCrosswalk
    extends XSLTCrosswalk
    implements IngestionCrosswalk
{
    /** log4j category */
    private static Logger log = Logger.getLogger(XSLTIngestionCrosswalk.class);

    private final static String DIRECTION = "submission";

    private static String aliases[] = makeAliases(DIRECTION);

    public static String[] getPluginNames()
    {
        return aliases;
    }

    // apply metadata values returned in DIM to the target item.
    private void applyDim(List dimList, Item item)
        throws MetadataValidationException
    {
        Iterator di = dimList.iterator();
        while (di.hasNext())
        {
            Element elt = (Element)di.next();
            if (elt.getName().equals("field") && elt.getNamespace().equals(DIM_NS))
                applyDimField(elt, item);

            // if it's a <dim> container, apply its guts
            else if (elt.getName().equals("dim") && elt.getNamespace().equals(DIM_NS))
                applyDim(elt.getChildren(), item);

            else
            {
                log.error("Got unexpected element in DIM list: "+elt.toString());
                throw new MetadataValidationException("Got unexpected element in DIM list: "+elt.toString());
            }
        }
    }

    // adds the metadata element from one <field>
    private void applyDimField(Element field, Item item)
    {
        String schema = field.getAttributeValue("mdschema");
        String element = field.getAttributeValue("element");
        String qualifier = field.getAttributeValue("qualifier");
        String lang = field.getAttributeValue("lang");

        item.addMetadata(schema, element, qualifier, lang, field.getText());
    }

    /**
     * Translate metadata with XSL stylesheet and ingest it.
     * Translation produces a list of DIM "field" elements;
     * these correspond directly to Item.addMetadata() calls so
     * they are simply executed.
     */
    public void ingest(Context context, DSpaceObject dso, List metadata)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        if (dso.getType() != Constants.ITEM)
            throw new CrosswalkObjectNotSupported("XsltSubmissionionCrosswalk can only crosswalk to an Item.");
        Item item = (Item)dso;

        XSLTransformer xform = getTransformer(DIRECTION);
        if (xform == null)
            throw new CrosswalkInternalException("Failed to initialize transformer, probably error loading stylesheet.");
        try
        {
            List dimList = xform.transform(metadata);
            applyDim(dimList, item);
        }
        catch (XSLTransformException e)
        {
            log.error("Got error: "+e.toString());
            throw new CrosswalkInternalException("XSL Transformation failed: "+e.toString());
        }
    }

    /**
     * Ingest a whole document.  Build Document object around root element,
     * and feed that to the transformation, since it may get handled
     * differently than a List of metadata elements.
     */
    public void ingest(Context context, DSpaceObject dso, Element root)
        throws CrosswalkException, IOException, SQLException, AuthorizeException
    {
        if (dso.getType() != Constants.ITEM)
            throw new CrosswalkObjectNotSupported("XsltSubmissionionCrosswalk can only crosswalk to an Item.");
        Item item = (Item)dso;

        XSLTransformer xform = getTransformer(DIRECTION);
        if (xform == null)
            throw new CrosswalkInternalException("Failed to initialize transformer, probably error loading stylesheet.");
        try
        {
            Document dimDoc = xform.transform(new Document((Element)root.clone()));
            applyDim(dimDoc.getRootElement().getChildren(), item);
        }
        catch (XSLTransformException e)
        {
            log.error("Got error: "+e.toString());
            throw new CrosswalkInternalException("XSL Transformation failed: "+e.toString());
        }

    }

    /**
     * Simple command-line rig for testing the DIM output of a stylesheet.
     * Usage:  java XSLTIngestionCrosswalk  <crosswalk-name> <input-file>
     */
    public static void main(String[] argv) throws Exception
    {
        if (argv.length < 2)
        {
            System.err.println("Usage:  java XSLTIngestionCrosswalk [-l] <crosswalk-name> <input-file>");
            System.exit(1);
        }

        int i = 0;
        boolean list = false;
        // skip first arg if it's the list option
        if (argv.length > 2 && argv[0].equals("-l"))
        {
            ++i;
            list = true;
        }
        IngestionCrosswalk xwalk = (IngestionCrosswalk)PluginManager.getNamedPlugin(
                IngestionCrosswalk.class, argv[i]);
        if (xwalk == null)
        {
            System.err.println("Error, cannot find an IngestionCrosswalk plugin for: \""+argv[i]+"\"");
            System.exit(1);
        }

        XSLTransformer xform = ((XSLTIngestionCrosswalk)xwalk).getTransformer(DIRECTION);
        if (xform == null)
            throw new CrosswalkInternalException("Failed to initialize transformer, probably error loading stylesheet.");

        SAXBuilder builder = new SAXBuilder();
        Document inDoc = builder.build(new FileInputStream(argv[i+1]));
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        Document dimDoc = null;
        List dimList = null;
        if (list)
        {
            dimList = xform.transform(inDoc.getRootElement().getChildren());
            outputter.output(dimList, System.out);
        }
        else
        {
            dimDoc = xform.transform(inDoc);
            outputter.output(dimDoc, System.out);
            dimList = dimDoc.getRootElement().getChildren();
        }

        // Sanity-check the generated DIM, make sure it would load.
        Context context = new Context();
        Iterator di = dimList.iterator();
        while (di.hasNext())
        {
            // skip over comment, text and other trash some XSLs generate..
            Object o = di.next();
            if (!(o instanceof Element))
                continue;

            Element elt = (Element)o;
            if (elt.getName().equals("field") && elt.getNamespace().equals(DIM_NS))
            {
                String schema = elt.getAttributeValue("mdschema");
                String element = elt.getAttributeValue("element");
                String qualifier = elt.getAttributeValue("qualifier");
                MetadataSchema ms = MetadataSchema.find(context, schema);
                if (ms == null )
                {
                    System.err.println("DIM Error, Cannot find metadata schema for: schema=\""+schema+
                        "\" (... element=\""+element+"\", qualifier=\""+qualifier+"\")");
    }
                else
                {
                    if (qualifier != null && qualifier.equals(""))
                    {
                        System.err.println("DIM Warning, qualifier is empty string: "+
                              " schema=\""+schema+"\", element=\""+element+"\", qualifier=\""+qualifier+"\"");
                        qualifier = null;
                    }
                    MetadataField mf = MetadataField.findByElement(context,
                                  ms.getSchemaID(), element, qualifier);
                    if (mf == null)
                        System.err.println("DIM Error, Cannot find metadata field for: schema=\""+schema+
                            "\", element=\""+element+"\", qualifier=\""+qualifier+"\"");
                }
            }
            else
            {
                // ("Got unexpected element in DIM list: "+elt.toString());
                throw new MetadataValidationException("Got unexpected element in DIM list: "+elt.toString());
            }
        }
    }

}
