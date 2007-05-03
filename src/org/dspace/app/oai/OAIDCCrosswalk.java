/*
 * OAIDCCrosswalk.java
 *
 * Version: $Revision: 1.6 $
 *
 * Date: $Date: 2004/12/22 17:48:37 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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
 *
 * <pre>
 * Revision History:
 * 
 *   2006/06/29: Ben
 *     - make escapeXml more efficient
 *
 *   2004/07/13: Ben
 *     - make the xml escaping into a static method
 * </pre>
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
 * @version $Revision: 1.6 $
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

		value = escapeXml(value);

                metadata.append("<dc:").append(element).append(">").append(
                        value).append("</dc:").append(element).append(">");
            }
        }

        metadata.append("</oai_dc:dc>");

        return metadata.toString();
    }
  

  /************************************************************ escapeXml */
  /**
   * XML escape the input text.
   *
   * @param strMsg input text
   * @return XML escaped version of input
   */

  public static String escapeXml(String strMsg) {
    if (strMsg == null)
      return null;

    StringBuffer sb = new StringBuffer();
    for (int i=0; i < strMsg.length(); i++) {
      char ch = strMsg.charAt(i);
      
      switch (ch) {
      case '&':   sb.append("&amp;");    break;
      case '<':   sb.append("&lt;");     break;
      case '>':   sb.append("&gt;");     break;
      default:    sb.append(ch);         break;
      }
    }

    return sb.toString();
  }

}
