/*
 * Copyright (c) 2004-2005, Hewlett-Packard Company and Massachusetts
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

import java.io.BufferedInputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.dspace.app.didl.UUIDFactory;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.search.HarvestedItemInfo;
import org.dspace.storage.bitstore.BitstreamStorageManager;

import ORG.oclc.oai.server.crosswalk.Crosswalk;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;
import ORG.oclc.oai.server.verb.ServerVerb;

/**
 * DSpace Item DIDL crosswalk.
 * 
 * Development of this code was part of the aDORe repository project 
 * by the Research Library of the Los Alamos National Laboratory.
 * 
 * @author Henry Jerez
 * @author Los Alamos National Laboratory
 */

public class DIDLCrosswalk extends Crosswalk
{
    public DIDLCrosswalk(Properties properties)
    {
    	super("urn:mpeg:mpeg21:2002:02-DIDL-NS http://standards.iso.org/ittf/PubliclyAvailableStandards/MPEG-21_schema_files/did/didl.xsd ");
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
       
        Date d = ((HarvestedItemInfo) nativeItem).datestamp;
        String ITEMDATE = new DCDate(d).toString();
        
        // Get all the DC
        DCValue[] allDC = item.getDC(Item.ANY, Item.ANY, Item.ANY);
        
        StringBuffer metadata = new StringBuffer();
        StringBuffer metadata1 = new StringBuffer();
        String itemhandle=item.getHandle();
        int maxsize=  Integer.parseInt(ConfigurationManager.getProperty("oai.didl.maxresponse")); 
        String currdate=ServerVerb.createResponseDate(new Date());
        
        metadata.append("<didl:DIDL ")
            .append(" xmlns:didl=\"urn:mpeg:mpeg21:2002:02-DIDL-NS\"  ")
            .append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
            .append("xsi:schemaLocation=\"urn:mpeg:mpeg21:2002:02-DIDL-NS http://standards.iso.org/ittf/PubliclyAvailableStandards/MPEG-21_schema_files/did/didl.xsd \">")
            .append ("<didl:DIDLInfo>")
            .append ("<dcterms:created xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://purl.org/dc/terms/ \">")
            .append  (currdate)
            .append ("</dcterms:created> </didl:DIDLInfo>" )
            .append("<didl:Item id=\"")
            .append("uuid-" + UUIDFactory.generateUUID().toString()+"\">");
        metadata.append("<didl:Descriptor>")
            .append("<didl:Statement mimeType=\"application/xml; charset=utf-8\">")
            .append("<dii:Identifier xmlns:dii=\"urn:mpeg:mpeg21:2002:01-DII-NS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"urn:mpeg:mpeg21:2002:01-DII-NS http://standards.iso.org/ittf/PubliclyAvailableStandards/MPEG-21_schema_files/dii/dii.xsd\">")
            .append("urn:hdl:" + itemhandle)
            .append("</dii:Identifier>")
            .append("</didl:Statement>")
            .append("</didl:Descriptor>");
        metadata.append("<didl:Descriptor>")
            .append("<didl:Statement mimeType=\"application/xml; charset=utf-8\">");
					
        for (int i = 0; i < allDC.length; i++)
        {
            // Do not include description.provenance
            boolean description = allDC[i].element.equals("description");
            boolean provenance = allDC[i].qualifier != null &&
                                 allDC[i].qualifier.equals("provenance");

            if (!(description && provenance))
            {
                // Escape XML chars <, > and &
                String value = allDC[i].value;

                // First do &'s - need to be careful not to replace the
                // & in "&amp;" again!
                int c = -1;
                while ((c = value.indexOf("&", c + 1)) > -1)
                {
                    value = value.substring(0, c) +
                        "&amp;" +
                        value.substring(c + 1);
                }

                while ((c = value.indexOf("<")) > -1)
                {
                    value = value.substring(0, c) +
                        "&lt;" +
                        value.substring(c + 1);
                }
                
                while ((c = value.indexOf(">")) > -1)
                {
                    value = value.substring(0, c) +
                        "&gt;" +
                        value.substring(c + 1);
                }

                metadata1.append("<dc:")
                    .append(allDC[i].element)
                    .append(">")
                    .append(value)
                    .append("</dc:")
                    .append(allDC[i].element)
                    .append(">");
            }
        }
        
        metadata.append("<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">");
        				
        metadata.append(metadata1);
        
        metadata.append("</oai_dc:dc>")
            .append("</didl:Statement>")
            .append("</didl:Descriptor>");				
        
        /**putfirst item here**/
        
        
        //**CYCLE HERE!!!!**//
       
        try
        {
            Bundle[] bundles= item.getBundles("ORIGINAL");    
            
            if (bundles.length == 0)
            {
                metadata.append("<P>There are no files associated with this item.</P>");
            }
            else
            {  
            	/**cycle bundles**/
                for (int i = 0; i < bundles.length; i++)
                { 
                    int flag=0;			
                    Bitstream[] bitstreams = bundles[i].getBitstreams();
                    
                    /**cycle bitstreams**/
                    for (int k = 0; k < bitstreams.length ; k++)
                    {
                        // Skip internal types
                        if (!bitstreams[k].getFormat().isInternal())
                        {
                            if (flag==0)	
                            {
                                flag=1;
                            }
                        	
                            metadata.append("<didl:Component id=" + "\"uuid-"+ UUIDFactory.generateUUID().toString() + "\">");
                           
                           if (bitstreams[k].getSize()> maxsize) 
                           {
                               metadata.append("<didl:Resource ref=\""+ConfigurationManager.getProperty("dspace.url")+"/bitstream/"+itemhandle+"/"+bitstreams[k].getSequenceID()+"/"+bitstreams[k].getName() );
                               metadata.append("\" mimeType=\"");
                               metadata.append(bitstreams[k].getFormat().getMIMEType());
                               metadata.append("\">");
                               metadata.append("</didl:Resource>");
                           }
                           else
                           {    
                            
                                try
                                {
                                    metadata.append("<didl:Resource mimeType=\"");
                                    metadata.append(bitstreams[k].getFormat().getMIMEType());
                                    metadata.append("\" encoding=\"base64\">");
                                                                       
                                    /*
                                     * Assume that size of in-line bitstreams will always be
                                     * smaller than MAXINT bytes
                                     */
                                    int intSize = (int) bitstreams[k].getSize();
                                    
                                    byte[] buffer = new byte[intSize];
                                    
                                    Context contextl= new Context();
                                    BufferedInputStream bis=new BufferedInputStream(BitstreamStorageManager.retrieve(contextl,bitstreams[k].getID()));
                                    int size=bis.read(buffer);
                                    contextl.complete();
                                    
                                    String encoding = new String(Base64.encodeBase64(buffer), "ASCII");
                                    metadata.append(encoding);
                                }
                                catch (Exception ex)
                                {
                                    ex.printStackTrace();                       
                                    
                                    metadata.append("<didl:Resource ref=\""+ConfigurationManager.getProperty("dspace.url")+"/bitstream/"+itemhandle+"/"+bitstreams[k].getSequenceID()+"/"+bitstreams[k].getName() );
                                    metadata.append("\" mimeType=\"");
                                    metadata.append(bitstreams[k].getFormat().getMIMEType());
                                    metadata.append("\">");
                                }

                                metadata.append("</didl:Resource>");
                            }
                            metadata.append("</didl:Component>");	
                        }
                        /*end bitstream cycle*/     
                    }
                    /*end bundle cycle*/
                }
            }
        }
        catch (SQLException sqle)
        {
            System.err.println("Caught exception:"+sqle.getCause());
            sqle.printStackTrace();
        }
    		
        //**END CYCLE HERE **//		
        
        metadata.append("</didl:Item>")
                .append("</didl:DIDL>");
    
        return metadata.toString();
    }
}