package org.dspace.app.mediafilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class LitPdfThumbnailFilter extends LitImageMagickThumbnailFilter {
   public InputStream getDestinationStream(InputStream source)
        throws Exception
    {
		File f = inputStreamToTempFile(source, "litpdfthumb", ".pdf");
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
