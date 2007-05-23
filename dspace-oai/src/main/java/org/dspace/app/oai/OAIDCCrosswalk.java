/*
 * OAIDCCrosswalk.java
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
package org.dspace.app.oai;

import java.util.Properties;

import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.search.HarvestedItemInfo;

import ORG.oclc.oai.server.crosswalk.Crosswalk;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;

/**
 * An OAICat Crosswalk implementation that extracts unqualified Dublin Core from
 * DSpace items into the oai_dc format.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class OAIDCCrosswalk extends Crosswalk
{
    public OAIDCCrosswalk(Properties properties)
    {
        super(
                "http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd");
    }

    public boolean isAvailableFor(Object nativeItem)
    {
        // We have DC for everything
        return true;
    }

    public String createMetadata(Object nativeItem)
            throws CannotDisseminateFormatException
    {
        Item item = ((HarvestedItemInfo) nativeItem).item;

        // Get all the DC
        DCValue[] allDC = item.getDC(Item.ANY, Item.ANY, Item.ANY);

        StringBuffer metadata = new StringBuffer();

        metadata
                .append(
                        "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" ")
                .append("xmlns:dc=\"http://purl.org/dc/elements/1.1/\" ")
                .append(
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
                .append(
                        "xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">");

        for (int i = 0; i < allDC.length; i++)
        {
            // Do not include description.provenance
            boolean description = allDC[i].element.equals("description");
            boolean provenance = (allDC[i].qualifier != null)
                    && allDC[i].qualifier.equals("provenance");

            if (!(description && provenance))
            {
                String element = allDC[i].element;

                // contributor.author exposed as 'creator'
                if (allDC[i].element.equals("contributor")
                        && (allDC[i].qualifier != null)
                        && allDC[i].qualifier.equals("author"))
                {
                    element = "creator";
                }

                // Escape XML chars <, > and &
                String value = allDC[i].value;
                
                if (value != null)
                {
                    // remove control unicode char
                    String temp = value.trim();
                    char[] dcvalue = temp.toCharArray();
                    for (int charPos = 0; charPos < dcvalue.length; charPos++)
                    {
                        if (Character.isISOControl(dcvalue[charPos]) &&
                            !String.valueOf(dcvalue[charPos]).equals("\u0009") &&
                            !String.valueOf(dcvalue[charPos]).equals("\n") &&
                            !String.valueOf(dcvalue[charPos]).equals("\r"))
                        {
                            dcvalue[charPos] = ' ';
                        }
                    }
                    value = String.valueOf(dcvalue);
                }

                // First do &'s - need to be careful not to replace the
                // & in "&amp;" again!
                int c = -1;

                while ((c = value.indexOf("&", c + 1)) > -1)
                {
                    value = value.substring(0, c) + "&amp;"
                            + value.substring(c + 1);
                }

                while ((c = value.indexOf("<")) > -1)
                {
                    value = value.substring(0, c) + "&lt;"
                            + value.substring(c + 1);
                }

                while ((c = value.indexOf(">")) > -1)
                {
                    value = value.substring(0, c) + "&gt;"
                            + value.substring(c + 1);
                }

                metadata.append("<dc:").append(element).append(">").append(
                        value).append("</dc:").append(element).append(">");
            }
        }

        metadata.append("</oai_dc:dc>");

        return metadata.toString();
    }
}
