package org.dspace.app.mediafilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;


/**
 * Filter image bitstreams, scaling the image to be within the bounds of
 * thumbnail.maxwidth, thumbnail.maxheight, the size we want our thumbnail to be
 * no bigger than. Creates only JPEGs.
 */
public class LitImageThumbnailFilter extends LitImageMagickThumbnailFilter 
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
		File f = inputStreamToTempFile(source, "litthumb", ".tmp");
    	File f2 = getThumbnailFile(f);
		return new FileInputStream(f2);
    }


}
