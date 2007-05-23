/*
 * NullIngestionCrosswalk.java
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

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import java.sql.SQLException;

import org.apache.log4j.Logger;

import org.dspace.core.Context;
import org.dspace.core.Constants;
import org.dspace.content.DSpaceObject;
import org.dspace.authorize.AuthorizeException;

import org.jdom.*;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

/**
 * "Null" ingestion crosswalk
 * <p>
 * Use this crosswalk to ignore a metadata record on ingest.  It was
 * intended to be used with a package importer such as the METS
 * packager, which may receive metadata records of types for which it
 * hasn't got a crosswalk.  The safest thing to do with these is ignore
 * them.  To do that, use the plugin configuration to map the name
 * of the metadata type to this plugin  (or within the METS ingester,
 * use its metadata-name remapping configuration).
 * <pre>
 * # ignore LOM metadata when it comes up:
 * plugin.named.org.dspace.content.crosswalk.SubmissionCrosswalk = \
 *   org.dspace.content.crosswalk.NullIngestionCrosswalk = NULL, LOM
 * </pre>
 * @author Larry Stone
 * @version $Revision$
 */
public class NullIngestionCrosswalk
    implements IngestionCrosswalk
{
    /** log4j category */
    private static Logger log = Logger.getLogger(NullIngestionCrosswalk.class);

    private static XMLOutputter outputPretty = new XMLOutputter(Format.getPrettyFormat());

    public void ingest(Context context, DSpaceObject dso, Element root)
        throws CrosswalkException, IOException, SQLException, AuthorizeException
    {
        // drop xml on the floor but mention what we're missing for debugging:
        log.debug("Null crosswalk is ignoring this metadata Element: \n"+
                outputPretty.outputString(root));
    }

    public void ingest(Context context, DSpaceObject dso, List ml)
        throws CrosswalkException, IOException, SQLException, AuthorizeException
    {
        // drop xml on the floor but mention what we're missing for debugging:
        log.debug("Null crosswalk is ignoring this List of metadata: \n"+
                outputPretty.outputString(ml));
    }

    public boolean preferList()
    {
        return false;
    }
}
