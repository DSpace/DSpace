/*
 * DIMIngestionCrosswalk.java
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

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.jdom.Element;
import org.jdom.Namespace;

/**
 * DIM ingestion crosswalk
 * <p>
 * Processes Dublic Core metadata encased in an oai_dc:dc wrapper
 *
 * @author Alexey Maslov
 * @version $Revision: 1 $
 */
public class OAIDCIngestionCrosswalk
    implements IngestionCrosswalk
{
    private static Namespace DC_NS = Namespace.getNamespace("http://www.dspace.org/xmlns/dspace/dim");
    private static Namespace OAI_DC_NS = Namespace.getNamespace("http://www.openarchives.org/OAI/2.0/oai_dc/");
    
	public void ingest(Context context, DSpaceObject dso, List<Element> metadata) throws CrosswalkException, IOException, SQLException, AuthorizeException {
        Element wrapper = new Element("wrap", metadata.get(0).getNamespace());
		wrapper.addContent(metadata);
		
		ingest(context,dso,wrapper);
	}

	public void ingest(Context context, DSpaceObject dso, Element root) throws CrosswalkException, IOException, SQLException, AuthorizeException {
		
		if (dso.getType() != Constants.ITEM)
        {
            throw new CrosswalkObjectNotSupported("DIMIngestionCrosswalk can only crosswalk an Item.");
        }
        Item item = (Item)dso;
        
        if (root == null) {
        	System.err.println("The element received by ingest was null");
        	return;
        }
        
        List<Element> metadata = root.getChildren();
        for (Element element : metadata) {
        	item.addMetadata("dc", element.getName(), null, null, element.getText());
        }
        
	}
	
}
