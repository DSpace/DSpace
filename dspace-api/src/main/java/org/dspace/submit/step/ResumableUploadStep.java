package org.dspace.submit.step;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadBase.FileSizeLimitExceededException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

public class ResumableUploadStep extends UploadStep{
    
    private static Logger log = Logger.getLogger(ResumableUploadStep.class);
    
    private static String tempDir;
    
    static{
        if (ConfigurationManager.getProperty("upload.temp.dir") != null)
        {
            tempDir = ConfigurationManager.getProperty("upload.temp.dir");
        }
        else
        {
            tempDir = System.getProperty("java.io.tmpdir");
        } 
    }
    
    public int processUploadFile(
            Context context,
            HttpServletRequest request,
            HttpServletResponse response,
            SubmissionInfo subInfo) throws ServletException, IOException,
    SQLException, AuthorizeException
    {
        String resumableFilename = request.getParameter("resumable_filename");
        if (!StringUtils.isEmpty(resumableFilename))
        {
            if (request.getMethod().equals("GET"))
            {
                DoGetResumable(request, response);
            }
            else
            {
                log.debug("resumable Filename: '" + resumableFilename + "'.");
                File completedFile = null;
                try
                {
                    log.debug("Starting doPostResumable method.");
                    completedFile = doPostResumable(request);
                }
                catch(IOException e){
                    // we were unable to receive the complete chunk => initialize reupload
                    response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                }
                
                if (completedFile == null)
                {
                    // if a part/chunk was uploaded, but the file is not completly uploaded yet
                    log.debug("Got one file chunk, but the upload is not completed yet.");
                    return;
                }
                else
                {
                    // We got the complete file. Assemble it and store
                    // it in the repository.
                    log.debug("Going to assemble file chunks.");

                    if (completedFile.length() > 0)
                    {
                        String fileName = completedFile.getName();
                        String filePath = tempDir + File.separator + fileName;
                        // Read the temporary file
                        InputStream fileInputStream = 
                                new BufferedInputStream(new FileInputStream(completedFile));
                        
                        // to safely store the file in the repository
                        // we have to add it as a bitstream to the
                        // appropriate item (or to be specific its
                        // bundle). Instead of rewriting this code,
                        // we should use the same code, that's used for
                        // the "old" file upload (which is not using JS).
                        SubmissionInfo si = getSubmissionInfo(context, request);
                        UploadStep us = new UploadStep();
                        request.setAttribute(fileName + "-path", filePath);
                        request.setAttribute(fileName + "-inputstream", fileInputStream);
                        request.setAttribute(fileName + "-description", request.getParameter("description"));
                        int uploadResult = us.processUploadFile(context, request, response, si);

                        // cleanup our temporary file
                        if (!completedFile.delete())
                        {
                            log.error("Unable to delete temporary file " + filePath);
                        }

                        // We already assembled the complete file.
                        // In case of any error it won't help to
                        // reupload the last chunk. That makes the error
                        // handling realy easy:
                        if (uploadResult != UploadStep.STATUS_COMPLETE)
                        {
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            return;
                        }
                        context.commit();
                    }
                    return;
                }  
            }
        }
        else
        {
            log.warn("Recieved a remubale upload request with no resumable_filename parameter");
        }
        
        return 0;
    }
    
    /**
     * Resumable.js sends chunks of files using http post.
     * If a chunk was the last missing one, we have to assemble the file and
     * return it. If other chunks are missing, we just return null.
     * @param request
     * @param response
     * @throws IOException
     */
    protected void DoGetResumable(HttpServletRequest request, HttpServletResponse response) 
        throws IOException
    {
        String tempDir;
        
       

        String resumableIdentifier = request.getParameter("resumableIdentifier");
        String resumableChunkNumber = request.getParameter("resumableChunkNumber");
        long resumableCurrentChunkSize = 
                Long.valueOf(request.getParameter("resumableCurrentChunkSize"));

        tempDir = tempDir + File.separator + resumableIdentifier;

        File fileDir = new File(tempDir);

        // create a new directory for each resumableIdentifier
        if (!fileDir.exists()) {
            fileDir.mkdir();
        }
        
        // use the String "part" and the chunkNumber as filename of a chunk
        String chunkPath = tempDir + File.separator + "part" + resumableChunkNumber;

        File chunkFile = new File(chunkPath);
        // if the chunk was uploaded already, we send a status code of 200
        if (chunkFile.exists()) {
            if (chunkFile.length() == resumableCurrentChunkSize) {
                response.setStatus(HttpServletResponse.SC_OK);
                return;
            }
            // The chunk file does not have the expected size, delete it and 
            // pretend that it wasn't uploaded already.
            chunkFile.delete();
        }
        
        // if we don't have the chunk send a http status code 404
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }  
    
    /**
     * Resumable.js sends chunks of files using http post.
     * @param request
     * @return If a chunk was the last missing one, we have to assemble the file and
     *         return it. If other chunks are missing, we just return null.
     * @throws FileSizeLimitExceededException
     * @throws IOException
     * @throws ServletException
     */
    protected File doPostResumable(HttpServletRequest request)
            throws FileSizeLimitExceededException, IOException, ServletException 
    {
        File completedFile = null;
        //FileUploadRequest wrapper = null;
        
        if (ConfigurationManager.getProperty("upload.temp.dir") != null)
        {
            tempDir = ConfigurationManager.getProperty("upload.temp.dir");
        }
        else {
            tempDir = System.getProperty("java.io.tmpdir");
        }
        
        try
        {
            // if we already have a FileUploadRequest, use it
            if (Class.forName("org.dspace.app.webui.util.FileUploadRequest").isInstance(request))
            {
                wrapper = (FileUploadRequest) request;
            } 
            else // if not wrap the mulitpart request to get the submission info
            {
                wrapper = new FileUploadRequest(request);
            }
        }
        catch (ClassNotFoundException ex)
        {
            // Cannot find a class that is part of the JSPUI?
            log.fatal("Cannot find class org.dspace.app.webui.util.FileUploadRequest");
            throw new ServletException("Cannot find class org.dspace.app.webui.util.FileUploadRequest.", ex);
        }

        String resumableIdentifier = wrapper.getParameter("resumableIdentifier");
        long resumableTotalSize = Long.valueOf(wrapper.getParameter("resumableTotalSize"));
        int resumableTotalChunks = Integer.valueOf(wrapper.getParameter("resumableTotalChunks"));

        String chunkDirPath = tempDir + File.separator + resumableIdentifier;
        File chunkDirPathFile = new File(chunkDirPath);
        boolean foundAll = true;
        long currentSize = 0l;
        
        // check whether all chunks were received.
        if(chunkDirPathFile.exists())
        {
            for (int p = 1; p <= resumableTotalChunks; p++) 
            {
                File file = new File(chunkDirPath + File.separator + "part" + Integer.toString(p));

                if (!file.exists()) 
                {
                    foundAll = false;
                    break;
                }
                currentSize += file.length();
            }
        }
        
        if (foundAll && currentSize >= resumableTotalSize) 
        {
            try {
                // assemble the file from it chunks.
                File file = makeFileFromChunks(tempDir, chunkDirPathFile, wrapper);
            
                if (file != null) 
                {
                    completedFile = file;
                }
            } catch (IOException ex) {
                // if the assembling of a file results in an IOException a
                // retransmission has to be triggered. Throw the IOException
                // here and handle it above.
                throw ex;
            }
        }

        return completedFile;
    }
}
