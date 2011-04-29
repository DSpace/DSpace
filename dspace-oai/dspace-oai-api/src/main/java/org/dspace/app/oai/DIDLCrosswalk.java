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
import java.io.InputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.dspace.app.didl.UUIDFactory;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
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
    /** default value if no oai.didl.maxresponse property is defined */
    public static int MAXRESPONSE_INLINE_BITSTREAM = 0;
    
    /** another crosswalk that will be used to generate the metadata section */
    private Crosswalk metadataCrosswalk;
    
    public DIDLCrosswalk(Properties properties)
    {
    	super("urn:mpeg:mpeg21:2002:02-DIDL-NS http://standards.iso.org/ittf/PubliclyAvailableStandards/MPEG-21_schema_files/did/didl.xsd ");

    	// FIXME this should be injected from the configuration... 
    	// but it is better than duplicate the OAIDCCrosswalk code! 
    	metadataCrosswalk = new OAIDCCrosswalk(properties);
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
       
        StringBuffer metadata = new StringBuffer();
        String itemhandle=item.getHandle();
        String strMaxSize = ConfigurationManager.getProperty("oai.didl.maxresponse");
        int maxsize = MAXRESPONSE_INLINE_BITSTREAM;
        if (strMaxSize != null)
        {
            maxsize = Integer.parseInt(strMaxSize);
        }
         
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
					
        // delegate the metadata section to another crosswalk
        metadata.append(metadataCrosswalk.createMetadata(nativeItem));
        
        metadata
            .append("</didl:Statement>")
            .append("</didl:Descriptor>");				
        
        /**putfirst item here**/
        
        
        //**CYCLE HERE!!!!**//
       
        try
        {
            Bundle[] bundles= item.getBundles("ORIGINAL");    
            
            if (bundles.length != 0)
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
                                    InputStream is = BitstreamStorageManager.retrieve(contextl,bitstreams[k].getID());
                                    BufferedInputStream bis = new BufferedInputStream(is);
                                    try
                                    {
                                        int size=bis.read(buffer);
                                    }
                                    finally
                                    {
                                        if (bis != null)
                                            try { bis.close(); } catch (IOException ioe) { }

                                        if (is != null)
                                            try { is.close(); } catch (IOException ioe) { }
                                    }

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