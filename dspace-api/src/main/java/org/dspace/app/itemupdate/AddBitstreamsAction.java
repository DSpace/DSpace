/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemupdate;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.InstallItemService;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;

/**
 * 	 Action to add bitstreams listed in item contents file to the item in DSpace	
 *   
 *
 */
public class AddBitstreamsAction extends UpdateBitstreamsAction {

    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();

	public AddBitstreamsAction()
	{
		//empty	
	}
	
	/**
	 * Adds bitstreams from the archive as listed in the contents file.
	 * 
	 * @param context DSpace Context
	 * @param itarch Item Archive
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
            boolean suppressUndo) throws IllegalArgumentException,
            ParseException, IOException, AuthorizeException, SQLException 
	{
		Item item = itarch.getItem();
		File dir = itarch.getDirectory();
		
		List<ContentsEntry> contents = MetadataUtilities.readContentsFile(new File(dir, ItemUpdate.CONTENTS_FILE));
		
		if (contents.isEmpty())
		{
			ItemUpdate.pr("Contents is empty - no bitstreams to add");
			return;
		}
		
		ItemUpdate.pr("Contents bitstream count: " + contents.size());
		
		String[] files = dir.list(ItemUpdate.fileFilter);
		List<String> fileList = new ArrayList<String>();
		for (String filename : files)
		{
			fileList.add(filename);
			ItemUpdate.pr("file: " + filename);
		}
		
		for (ContentsEntry ce : contents)
		{
			//validate match to existing file in archive
			if (!fileList.contains(ce.filename))
			{
				throw new IllegalArgumentException("File listed in contents is missing: " + ce.filename);
			}
		}
		int bitstream_bundles_updated = 0;
		
		//now okay to add
		for (ContentsEntry ce : contents)
		{        				
			String targetBundleName = addBitstream(context, itarch, item, dir, ce, suppressUndo, isTest);
			if (!targetBundleName.equals("")
			    && !targetBundleName.equals("THUMBNAIL")
			    && !targetBundleName.equals("TEXT"))
			{
				bitstream_bundles_updated++;
			}
		}

	        if (alterProvenance && bitstream_bundles_updated > 0)
	        {
	        	DtoMetadata dtom = DtoMetadata.create("dc.description.provenance", "en", "");
	        
	        	String append = ". Added " + Integer.toString(bitstream_bundles_updated)
	        	                + " bitstream(s) on " + DCDate.getCurrent() + " : "
	        	                + installItemService.getBitstreamProvenanceMessage(context, item);
	        	MetadataUtilities.appendMetadata(context, item, dtom, false, append);
	        }
	}
		
        /**
         * Add bitstream
         * @param context DSpace Context
         * @param itarch Item Archive
         * @param item DSpace Item
         * @param dir directory
         * @param ce contents entry for bitstream
         * @param suppressUndo undo flag
         * @param isTest test flag
         * @return bundle name
         * @throws IOException if IO error
         * @throws IllegalArgumentException if arg exception
         * @throws SQLException if database error
         * @throws AuthorizeException if authorization error
         * @throws ParseException if parse error
         */
	protected String addBitstream(Context context, ItemArchive itarch, Item item, File dir,
			                  ContentsEntry ce, boolean suppressUndo, boolean isTest)
	throws IOException, IllegalArgumentException, SQLException, AuthorizeException, ParseException
	{
		ItemUpdate.pr("contents entry for bitstream: " + ce.toString());
    	File f = new File(dir, ce.filename);
    	
        // get an input stream
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));

        Bitstream bs = null;
        String newBundleName = ce.bundlename;

        if (ce.bundlename == null)  // should be required but default convention established
        {
            if (ce.filename.equals("license.txt"))
            {
                newBundleName = "LICENSE";
            }
            else  
            {
                newBundleName = "ORIGINAL";
            }
        }
        ItemUpdate.pr("  Bitstream " + ce.filename + " to be added to bundle: " + newBundleName);
        
        if (!isTest)
        {
	        // find the bundle
	        List<Bundle> bundles = itemService.getBundles(item, newBundleName);
	        Bundle targetBundle = null;
	
	        if (bundles.size() < 1)
	        {
	            // not found, create a new one
	            targetBundle = bundleService.create(context, item, newBundleName);
	        }
	        else
	        {
	    		//verify bundle + name are not duplicates
	        	for (Bundle b : bundles)
	        	{
	        		List<Bitstream> bitstreams = b.getBitstreams();
	        		for (Bitstream bsm : bitstreams)
	        		{
                        if (bsm.getName().equals(ce.filename))
	        			{
	        				throw new IllegalArgumentException("Duplicate bundle + filename cannot be added: " 
	        						+ b.getName() + " + " + bsm.getName());
	        			}
	        		}
	        	}

	            // select first bundle
	            targetBundle = bundles.iterator().next();
	        }
	
	        bs = bitstreamService.create(context, targetBundle, bis);
	        bs.setName(context, ce.filename);
	
	        // Identify the format
	        // FIXME - guessing format guesses license.txt incorrectly as a text file format!
	        BitstreamFormat fmt = bitstreamFormatService.guessFormat(context, bs);
	    	bitstreamService.setFormat(context, bs, fmt);

	        if (ce.description != null)
	        {
	        	bs.setDescription(context, ce.description);
	        }
	        	        
	        if ((ce.permissionsActionId != -1) && (ce.permissionsGroupName != null))
	        {
				Group group = groupService.findByName(context, ce.permissionsGroupName);
				
				if (group != null)
				{
                    authorizeService.removeAllPolicies(context, bs);  // remove the default policy
                    authorizeService.createResourcePolicy(context, bs, group, null, ce.permissionsActionId, null);
				}
	        }
	        
	        //update after all changes are applied
            bitstreamService.update(context, bs);
        
	        if (!suppressUndo)
	        {
	        	itarch.addUndoDeleteContents(bs.getID());
	        }
		return targetBundle.getName();
        }
	return "";
    }

}
