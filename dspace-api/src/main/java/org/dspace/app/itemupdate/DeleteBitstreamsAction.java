/*
 * DeleteBitstreamsAction.java
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

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * 	  Action to delete bitstreams	
 * 
 * 	  Undo not supported for this UpdateAction
 * 
 *    Derivatives of the bitstream to be deleted are not also deleted 
 *
 */
public class DeleteBitstreamsAction extends UpdateBitstreamsAction 
{
	/**
	 *   Delete bitstream from item
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
	throws IllegalArgumentException, IOException, SQLException, AuthorizeException, ParseException 
	{
		File f = new File(itarch.getDirectory(), ItemUpdate.DELETE_CONTENTS_FILE);
		if (!f.exists())
		{
			ItemUpdate.pr("Warning: Delete_contents file for item " + itarch.getDirectoryName() + " not found.");
		}
		else
		{
			List<Integer> list = MetadataUtilities.readDeleteContentsFile(f);
			if (list.isEmpty())
			{
				ItemUpdate.pr("Warning: empty delete_contents file for item " + itarch.getDirectoryName() );
			}
			else
			{
				for (int id : list)
				{
					try
					{
			    		Bitstream bs = Bitstream.find(context, id);
			    		if (bs == null)
			    		{
			    			ItemUpdate.pr("Bitstream not found by id: " + id);
			    		}
			    		else
			    		{
				    		Bundle[] bundles = bs.getBundles();
				    		for (Bundle b : bundles)
				    		{
				    			if (isTest)
				    			{
					    			ItemUpdate.pr("Delete bitstream with id = " + id);
				    			}
				    			else
				    			{
				    				b.removeBitstream(bs); 
					    			ItemUpdate.pr("Deleted bitstream with id = " + id);
					    			
				    			}
				    		}	
			    		
				            if (alterProvenance)
				            {
				            	DtoMetadata dtom = DtoMetadata.create("dc.description.provenance", "en", "");
				            	
				            	String append = "Bitstream " + bs.getName() + " deleted on " + DCDate.getCurrent() + "; ";
				            	Item item = bundles[0].getItems()[0];
					    		ItemUpdate.pr("Append provenance with: " + append);
					    		
					    		if (!isTest)
					    		{
					    			MetadataUtilities.appendMetadata(item, dtom, false, append);
					    		}
				            }
			    		}	
					}
					catch(SQLException e)
					{
						ItemUpdate.pr("Error finding bitstream from id: " + id + " : " + e.toString());
					}
				}
			}
		}
	}

}
