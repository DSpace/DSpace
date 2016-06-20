/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.cocoon.servlet.multipart;

import org.apache.cocoon.servlet.multipart.MultipartException;
import org.apache.cocoon.servlet.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

/**
 * This is the interface of Request Wrapper in Cocoon.
 *
 * @version $Id: RequestFactory.java 587750 2007-10-24 02:35:22Z vgritsenko $
 */
public class DSpaceRequestFactory {

    private boolean saveUploadedFilesToDisk;

    private File uploadDirectory;

    private boolean allowOverwrite;

    private boolean silentlyRename;

    private String defaultCharEncoding;

    private int maxUploadSize;

    public DSpaceRequestFactory(boolean saveUploadedFilesToDisk,
                                File uploadDirectory,
                                boolean allowOverwrite,
                                boolean silentlyRename,
                                int maxUploadSize,
                                String defaultCharEncoding) {
        this.saveUploadedFilesToDisk = saveUploadedFilesToDisk;
        this.uploadDirectory = uploadDirectory;
        this.allowOverwrite = allowOverwrite;
        this.silentlyRename = silentlyRename;
        this.maxUploadSize = maxUploadSize;
        this.defaultCharEncoding = defaultCharEncoding;

        if (saveUploadedFilesToDisk) {
           // Empty the contents of the upload directory
           File[] files = uploadDirectory.listFiles();
           for (int i = 0; i < files.length; i++) {
               files[i].delete();
           }
        }
    }

    /**
     * If the request includes a "multipart/form-data", then wrap it with
     * methods that allow easier connection to those objects since the servlet
     * API doesn't provide those methods directly.
     * @param request user's request.
     * @return wrapped request.
     * @throws java.io.IOException passed through.
     * @throws org.apache.cocoon.servlet.multipart.MultipartException passed through.
     */
    public HttpServletRequest getServletRequest(HttpServletRequest request) throws IOException, MultipartException {
        HttpServletRequest req = request;
        String contentType = request.getContentType();
        
        if ((contentType != null) && (contentType.toLowerCase().indexOf("multipart/form-data") > -1)) {
 
            String charEncoding = request.getCharacterEncoding();
            if (charEncoding == null || charEncoding.equals("")) {
                charEncoding = this.defaultCharEncoding;
            }
            
            DSpaceMultipartParser parser = new DSpaceMultipartParser(
                    this.saveUploadedFilesToDisk, 
                    this.uploadDirectory, 
                    this.allowOverwrite, 
                    this.silentlyRename, 
                    this.maxUploadSize,
                    charEncoding);
                    
            Hashtable parts = parser.getParts(request);
            
            req = new MultipartHttpServletRequest(request,parts);
        }

        return req;
    }
    
}
