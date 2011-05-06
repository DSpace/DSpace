/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
	 *  @param itarch
	 *  @param isTest
	 *  @param suppressUndo
	 *  @throws IllegalArgumentException
	 *  @throws ParseException
	 *  @throws IOException
	 *  @throws AuthorizeException
	 *  @throws SQLException
	 */
	public void execute(Context context, ItemArchive itarch, boolean isTest,
            boolean suppressUndo) throws AuthorizeException,
            BitstreamFilterException, IOException, ParseException, SQLException 
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
	    				if (!bundleName.equals("THUMBNAIL") && !bundleName.equals("TEXT"))
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
