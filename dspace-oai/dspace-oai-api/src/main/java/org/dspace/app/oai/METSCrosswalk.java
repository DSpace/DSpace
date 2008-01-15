/*
 * METSCrosswalk.java
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

import java.io.ByteArrayOutputStream;
import java.util.Properties;

import org.dspace.app.mets.METSExport;
import org.dspace.search.HarvestedItemInfo;

import ORG.oclc.oai.server.crosswalk.Crosswalk;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;

/**
 * OAICat crosswalk to allow METS to be harvested.
 * 
 * No security or privacy measures in place.
 * 
 * @author Li XiaoYu (Rita)
 * @author Robert Tansley
 */
public class METSCrosswalk extends Crosswalk
{
    public METSCrosswalk(Properties properties)
    {
        super(
                "http://www.loc.gov/METS/ http://www.loc.gov/standards/mets/mets.xsd");
    }

    public boolean isAvailableFor(Object nativeItem)
    {
        // We have METS for everything
        return true;
    }

    public String createMetadata(Object nativeItem)
            throws CannotDisseminateFormatException
    {
        HarvestedItemInfo hii = (HarvestedItemInfo) nativeItem;

        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            METSExport.writeMETS(hii.context, hii.item, baos, true);

            // FIXME: Nasty hack to remove <?xml...?> header that METS toolkit
            // puts there.  Hopefully the METS toolkit itself can be updated
            // to fix this
            String fullXML = baos.toString("UTF-8");
            String head = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>\n";
            int pos = fullXML.indexOf(head);
            if (pos != -1)
            {
                fullXML = fullXML.substring(pos + head.length());
            }
            
            return fullXML;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }

    }
}
