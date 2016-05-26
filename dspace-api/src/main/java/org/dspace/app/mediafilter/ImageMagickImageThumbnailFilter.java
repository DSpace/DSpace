/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import org.dspace.content.Item;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;


/**
 * Filter image bitstreams, scaling the image to be within the bounds of
 * thumbnail.maxwidth, thumbnail.maxheight, the size we want our thumbnail to be
 * no bigger than. Creates only JPEGs.
 */
public class ImageMagickImageThumbnailFilter extends ImageMagickThumbnailFilter 
{

    /**
     * @param currentItem item
     * @param source  source input stream
     * @param verbose verbose mode
     * 
     * @return InputStream the resulting input stream
     * @throws Exception if error
     */
    @Override
    public InputStream getDestinationStream(Item currentItem, InputStream source, boolean verbose)
            throws Exception
    {
		File f = inputStreamToTempFile(source, "imthumb", ".tmp");
    	File f2 = null;
	    try
	    {
		    f2 = getThumbnailFile(f, verbose);
		    byte[] bytes = Files.readAllBytes(f2.toPath());
		    return new ByteArrayInputStream(bytes);
	    }
	    finally
	    {
		    //noinspection ResultOfMethodCallIgnored
		    f.delete();
		    if (f2 != null)
		    {
			    //noinspection ResultOfMethodCallIgnored
			    f2.delete();
		    }
	    }
	}


}
