/*
 * DeleteBitstreamsByFilterAction.java
 *
 * Version: $Revision: 3984 $
 *
 * Date: $Date: 2009-06-29 22:33:25 -0400 (Mon, 29 Jun 2009) $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
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
 * - Neither the name of the DSpace Foundation nor the names of its
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
package org.dspace.app.itemupdate;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * 		Action to delete bitstreams using a specified filter implementing BitstreamFilter
 *      Derivatives for the target bitstreams are not deleted.
 * 
 *      The dc.description.provenance field is amended to reflect the deletions
 *      
 *      Note:  Multiple filters are impractical if trying to manage multiple properties files
 *      in a commandline environment
 * 
 *
 */
public class DeleteBitstreamsByFilterAction extends UpdateBitstreamsAction {

	private BitstreamFilter filter;
	
	/**
	 *   Set filter
	 *   
	 * @param filter
	 */
	public void setBitstreamFilter(BitstreamFilter filter)
	{
		this.filter = filter;
	}
	
	/**
	 *   Get filter
	 * @return filter 
	 */
	public BitstreamFilter getBitstreamFilter()
	{
		return filter;
	}
	
	/**
	 * 	 Delete bitstream
	 * 
	 *  @param context
	 *  @param ItemArchive
	 *  @param isTest
	 *  @param suppressUndo
	 *  @throws IllegalArgumentException
	 *  @throws ParseException
	 *  @throws IOException
	 *  @throws AuthorizeException
	 *  @throws SQLException
	 */
	public void execute(Context context, ItemArchive itarch, boolean isTest, boolean suppressUndo) 
	throws AuthorizeException, BitstreamFilterException, IOException, ParseException, SQLException 
	{
		
		List<String> deleted = new ArrayList<String>();
		
		Item item = itarch.getItem();
		Bundle[] bundles = item.getBundles();
		
		for (Bundle b : bundles)    
		{
			Bitstream[] bitstreams = b.getBitstreams();
			String bundleName = b.getName();
			
			for (Bitstream bs : bitstreams)
			{		
			    if (filter.accept(bs))
				{
	    			if (isTest)
	    			{
		    			ItemUpdate.pr("Delete from bundle " + bundleName + " bitstream " + bs.getName() 
		    					+ " with id = " + bs.getID());
	    			}
	    			else
	    			{	    					    				
	    				//provenance is not maintained for derivative bitstreams
	    				if (!bundleName.equals("THUMBMNAIL") && !bundleName.equals("TEXT"))
	    				{
	    					deleted.add(bs.getName());
	    				}
	    				b.removeBitstream(bs); 
		    			ItemUpdate.pr("Deleted " + bundleName + " bitstream " + bs.getName() 
		    					+ " with id = " + bs.getID());		    			
	    			}
				}
			}			
		}
		
        if (alterProvenance && !deleted.isEmpty())
        {     	
    		StringBuilder sb = new StringBuilder("  Bitstreams deleted on "); 	
    		sb.append(DCDate.getCurrent()).append(": ");
    		
    		for (String s : deleted)
    		{
            	sb.append(s).append(", ");
    		}
    		        	
        	DtoMetadata dtom = DtoMetadata.create("dc.description.provenance", "en", "");
        	
    		ItemUpdate.pr("Append provenance with: " + sb.toString());
    		
    		if (!isTest)
    		{
    			MetadataUtilities.appendMetadata(item, dtom, false, sb.toString());
    		}
        }
	}
	
}
