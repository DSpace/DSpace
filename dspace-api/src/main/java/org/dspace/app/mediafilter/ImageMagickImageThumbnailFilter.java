/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;


/**
 * Filter image bitstreams, scaling the image to be within the bounds of
 * thumbnail.maxwidth, thumbnail.maxheight, the size we want our thumbnail to be
 * no bigger than. Creates only JPEGs.
 */
public class ImageMagickImageThumbnailFilter extends ImageMagickThumbnailFilter 
{

    /**
     * @param source
     *            source input stream
     * 
     * @return InputStream the resulting input stream
     */
    public InputStream getDestinationStream(InputStream source)
            throws Exception
    {
		File f = inputStreamToTempFile(source, "imthumb", ".tmp");
    	File f2 = getThumbnailFile(f);
		return new FileInputStream(f2);
    }


}
