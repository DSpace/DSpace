/*
 * METSDisseminationCrosswalk.java
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.packager.PackageDisseminator;
import org.dspace.content.packager.PackageException;
import org.dspace.content.packager.PackageParameters;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * METS dissemination crosswalk
 * <p>
 * Produces a METS manifest for the DSpace item as a metadata
 * description -- intended to work within an application like the
 * OAI server.
 *
 * @author Larry Stone
 * @version $Revision$
 */
public class METSDisseminationCrosswalk
    implements DisseminationCrosswalk
{
    /** log4j category */
    private static Logger log = Logger.getLogger(METSDisseminationCrosswalk.class);

    // Plugin Name of METS packager to use for manifest;
    // maybe make  this configurable.
    private static final String METS_PACKAGER_PLUGIN = "METS";

    /**
     * MODS namespace.
     */
    public static final Namespace MODS_NS =
        Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");

    private static final Namespace XLINK_NS =
        Namespace.getNamespace("xlink", "http://www.w3.org/TR/xlink");


    /** METS namespace -- includes "mets" prefix for use in XPaths */
    private static Namespace METS_NS = Namespace
            .getNamespace("mets", "http://www.loc.gov/METS/");

    private static final Namespace namespaces[] = { METS_NS, MODS_NS, XLINK_NS };

    /**  URL of METS XML Schema */
    private static final String METS_XSD = "http://www.loc.gov/standards/mets/mets.xsd";

    private static final String schemaLocation =
        METS_NS.getURI()+" "+METS_XSD;

    private static XMLOutputter outputUgly = new XMLOutputter();
    private static XMLOutputter outputPretty = new XMLOutputter(Format.getPrettyFormat());
    private static SAXBuilder builder = new SAXBuilder();

    public Namespace[] getNamespaces()
    {
        return namespaces;
    }

    public String getSchemaLocation()
    {
        return schemaLocation;
    }

    public List disseminateList(DSpaceObject dso)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        List result = new ArrayList(1);
        result.add(disseminateElement(dso));
        return result;
    }

    public Element disseminateElement(DSpaceObject dso)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        if (dso.getType() != Constants.ITEM)
            throw new CrosswalkObjectNotSupported("METSDisseminationCrosswalk can only crosswalk an Item.");
        Item item = (Item)dso;

        PackageDisseminator dip = (PackageDisseminator)
          PluginManager.getNamedPlugin(PackageDisseminator.class, METS_PACKAGER_PLUGIN);
        if (dip == null)
            throw new CrosswalkInternalException("Cannot find a disseminate plugin for package="+METS_PACKAGER_PLUGIN);

        try
        {
            // Set the manifestOnly=true param so we just get METS document
            PackageParameters pparams = new PackageParameters();
            pparams.put("manifestOnly", "true");

            // "pipe" the output into a parser to create JDOM document.
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Context context = new Context();
            dip.disseminate(context, item, pparams, baos);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

            try
            {
                SAXBuilder builder = new SAXBuilder();
                Document metsDocument = builder.build(bais);
                return metsDocument.getRootElement();
            }
            catch (JDOMException je)
            {
                throw new MetadataValidationException("Error parsing METS: ",je);
            }
        }
        catch (PackageException pe)
        {
            throw new CrosswalkInternalException("Failed making METS manifest in packager: ",pe);
        }
    }

    public boolean canDisseminate(DSpaceObject dso)
    {
        return true;
    }

    public boolean preferList()
    {
        return false;
    }
}
