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

public class ImageMagickPdfThumbnailFilter extends ImageMagickThumbnailFilter {
   public InputStream getDestinationStream(InputStream source)
        throws Exception
    {
		File f = inputStreamToTempFile(source, "impdfthumb", ".pdf");
		File f2 = getImageFile(f, 0);
    	File f3 = getThumbnailFile(f2);
    	return new FileInputStream(f3);
    }

   public static final String[] PDF = {"Adobe PDF"};
   public String[] getInputMIMETypes()
  {
      return PDF;
  }

}
