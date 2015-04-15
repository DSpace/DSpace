/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public class ImageMagickPdfThumbnailFilter extends ImageMagickThumbnailFilter {
   public InputStream getDestinationStream(InputStream source)
        throws Exception
    {
		File f = inputStreamToTempFile(source, "impdfthumb", ".pdf");
		File f2 = getImageFile(f, 0);
	    f.delete();
    	File f3 = getThumbnailFile(f2);
	    f2.delete();
	    byte[] bytes = Files.readAllBytes(f3.toPath());
	    f3.delete();
	    return new ByteArrayInputStream(bytes);
    }

   public static final String[] PDF = {"Adobe PDF"};
   public String[] getInputMIMETypes()
  {
      return PDF;
  }

}
