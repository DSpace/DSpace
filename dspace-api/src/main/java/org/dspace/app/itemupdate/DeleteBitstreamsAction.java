/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemupdate;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
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
	 * @param context DSpace Context
	 * @param itarch item archive
	 * @param isTest test flag
	 * @param suppressUndo undo flag
	 * @throws IOException if IO error
         * @throws IllegalArgumentException if arg exception
         * @throws SQLException if database error
         * @throws AuthorizeException if authorization error
         * @throws ParseException if parse error
	 */
	@Override
    public void execute(Context context, ItemArchive itarch, boolean isTest,
            boolean suppressUndo) throws IllegalArgumentException, IOException,
            SQLException, AuthorizeException, ParseException 
	{
		File f = new File(itarch.getDirectory(), ItemUpdate.DELETE_CONTENTS_FILE);
		if (!f.exists())
		{
			ItemUpdate.pr("Warning: Delete_contents file for item " + itarch.getDirectoryName() + " not found.");
		}
		else
		{
			List<String> list = MetadataUtilities.readDeleteContentsFile(f);
			if (list.isEmpty())
			{
				ItemUpdate.pr("Warning: empty delete_contents file for item " + itarch.getDirectoryName() );
			}
			else
			{
				for (String id : list)
				{
					try
					{
			    		Bitstream bs = bitstreamService.findByIdOrLegacyId(context, id);
			    		if (bs == null)
			    		{
			    			ItemUpdate.pr("Bitstream not found by id: " + id);
			    		}
			    		else
			    		{
				    		List<Bundle> bundles = bs.getBundles();
				    		for (Bundle b : bundles)
				    		{
                                if (isTest)
				    			{
					    			ItemUpdate.pr("Delete bitstream with id = " + id);
				    			}
				    			else
				    			{
				    				bundleService.removeBitstream(context, b, bs);
					    			ItemUpdate.pr("Deleted bitstream with id = " + id);
					    			
				    			}
				    		}	
			    		
				            if (alterProvenance)
				            {
				            	DtoMetadata dtom = DtoMetadata.create("dc.description.provenance", "en", "");
				            	
				            	String append = "Bitstream " + bs.getName() + " deleted on " + DCDate.getCurrent() + "; ";
				            	Item item = bundles.iterator().next().getItems().iterator().next();
					    		ItemUpdate.pr("Append provenance with: " + append);
					    		
					    		if (!isTest)
					    		{
					    			MetadataUtilities.appendMetadata(context, item, dtom, false, append);
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
