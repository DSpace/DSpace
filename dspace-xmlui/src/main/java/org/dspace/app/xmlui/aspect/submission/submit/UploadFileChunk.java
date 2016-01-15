package org.dspace.app.xmlui.aspect.submission.submit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.servlet.multipart.PartOnDisk;
import org.apache.commons.fileupload.FileUploadBase.FileSizeLimitExceededException;
import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.xmlui.aspect.submission.FlowUtils;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;

public class UploadFileChunk extends AbstractAction{

    private static final Logger log = Logger.getLogger(UploadFileChunk.class);
    
    private static String tempDir;
    private static Object mutex = new Object();
    
    private String uploadDir;
    private String finalDir;
    //private Map objectModel;
    
    static{
        if (ConfigurationManager.getProperty("upload.temp.dir") != null)
        {
            tempDir = ConfigurationManager.getProperty("upload.temp.dir");
        }
        else {
            tempDir = System.getProperty("java.io.tmpdir");
        }
    }
    
    private void init(Request request){
        String resumableIdentifier;
        if(request.getParameter("resumableIdentifier") == null){
            resumableIdentifier = request.get("resumableIdentifier").toString();
        }
        else{
            resumableIdentifier = request.getParameter("resumableIdentifier");
        }
        
        // directory to upload chunks
        this.uploadDir = tempDir + File.separator + resumableIdentifier;
        
        // directory where file will be reassembled
        this.finalDir = tempDir + File.separator + resumableIdentifier.split("-")[0];
        
        // create upload directories if required
        File uploadDir = new File(this.uploadDir);
        if (!uploadDir.exists()) {
            uploadDir.mkdir();
        }
        File finalDir = new File(this.finalDir);
        if (!finalDir.exists()) {
            finalDir.mkdir();
        }

        log.info(this.finalDir);
        log.info(this.uploadDir);
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public Map act(
            Redirector redirector,
            SourceResolver resolver,
            Map objectModel,
            String source,
            Parameters parameters) throws Exception
    {
        Response response = ObjectModelHelper.getResponse(objectModel);
        Request request = ObjectModelHelper.getRequest(objectModel);
        
        this.init(request);
        
        log.info("=> " + request.getMethod());
        String handle = parameters.getParameter("handle", null);
        log.info("==> " + handle);
        log.info("===> " + request.getParameter("submissionId"));
        HashMap<String, String> returnValues = null;
        
        //WebContinuation wb = FlowHelper.getWebContinuation(objectModel);
        //log.info(wb.getContinuation().getClass());
        //FlowUtils.obtainSubmissionInfo(getObjectModel(), workspaceID);
        
        if(request.getMethod().equals("GET"))
        {
            if(doGetResumable(request, response)){
                log.info("ok");
                
                // if no exception has been throw then assume success
                returnValues = new HashMap<String, String>();
            }
        }
        else{
            
            File completedFile = doPostResumable(request);
            
            if(completedFile != null){
                FileInputStream fis = new FileInputStream(completedFile);
                SubmissionInfo si = FlowUtils.obtainSubmissionInfo(objectModel, request.getParameter("submissionId"));
                Item item = si.getSubmissionItem().getItem();

                // do we already have a bundle?
                Bundle[] bundles = item.getBundles("ORIGINAL");
                Bitstream b = null;
                
                if (bundles.length < 1)
                {
                    // set bundle's name to ORIGINAL
                    b = item.createSingleBitstream(fis, "ORIGINAL");
                }
                else
                {
                    // we have a bundle already, just add bitstream
                    b = bundles[0].createBitstream(fis);
                }

                /*HttpSession session = request.getSession(false);
                synchronized(mutex)
                {
                    if (session.getAttribute("assembled_files") == null)
                    {
                        session.setAttribute("assembled_files", new ArrayList<File>());
                    }
                    ((List<File>) session.getAttribute("assembled_files")).add(completedFile);
                }*/
            }
            
            returnValues = new HashMap<String, String>();
        }

        return returnValues;
    }
    
    // Resumable.js uses HTTP Get to recognize whether a specific part/chunk of 
    // a file was uploaded already. This method handles those requests.
    protected boolean doGetResumable(HttpServletRequest request, HttpServletResponse response) 
        throws IOException
    {
        boolean exists = false;
        
        int resumableTotalChunks = Integer.parseInt(request.getParameter("resumableTotalChunks").toString());
        long resumableCurrentChunkSize = 
                Long.valueOf(request.getParameter("resumableCurrentChunkSize"));

        String chunkPath;
        if(resumableTotalChunks == 1){
            // there is only one chunk give it the name of the file
            chunkPath = this.finalDir + File.separator + request.getParameter("resumableFilename").toString(); 
        }
        else{
            // use the String "part" and the chunkNumber as filename of a chunk
            chunkPath = this.uploadDir + File.separator + "part" + request.getParameter("resumableChunkNumber").toString();
        }
        
        File chunkFile = new File(chunkPath);
        // if the chunk was uploaded already, we send a status code of 200
        if (chunkFile.exists()) {
            if (chunkFile.length() == resumableCurrentChunkSize) {
                response.setStatus(HttpServletResponse.SC_OK);
                exists = true;
                log.info("* 200 *");
            }
            
            // The chunk file does not have the expected size, delete it and 
            // pretend that it wasn't uploaded already.
            chunkFile.delete();
        }
        else{
            log.info("* 204 *");
            // if we don't have the chunk send a http status code 204 No content
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
        
        return exists;
    }

    // Resumable.js sends chunks of files using http post.
    // If a chunk was the last missing one, we have to assemble the file and
    // return it. If other chunks are missing, we just return null.
    protected File doPostResumable(Request request)
            throws FileSizeLimitExceededException, IOException, ServletException 
    {
        File completedFile = null;
                
        long resumableTotalSize = Long.valueOf(request.get("resumableTotalSize").toString());
        int resumableTotalChunks = Integer.parseInt(request.get("resumableTotalChunks").toString());
        
        /*for (Enumeration<Object> e = request.getParameterNames(); e.hasMoreElements();){
            String key = e.nextElement().toString();
            log.info("---");
            log.info(key);
            Object val = request.get(key.toString());
            log.info(val);
            log.info(val.getClass());
        }*/
        
        // determine name of chunk
        String chunkPath;
        if(resumableTotalChunks == 1){
            // there is only one chunk give it the name of the file
            chunkPath = this.finalDir + File.separator + request.get("resumableFilename").toString(); 
        }
        else{
            chunkPath = this.uploadDir + File.separator + "part" + request.get("resumableChunkNumber").toString();
        }
        
        // cocoon will have uploaded the chunk automatically
        // now move it to temporary directory
        File chunk = ((PartOnDisk)request.get("file")).getFile();
        chunk.renameTo(new File(chunkPath));
        
        boolean foundAll = true;
        long currentSize = 0l;
        
        log.info("--------------------------- " + resumableTotalChunks);
        if (resumableTotalChunks > 1){
            for (int p = 1; p <= resumableTotalChunks; p++){
                File file = new File(this.uploadDir + File.separator + "part" + Integer.toString(p));
                if (!file.exists()) 
                {
                    foundAll = false;
                    break;
                }
                currentSize += file.length();
            }
            
            if (foundAll && currentSize >= resumableTotalSize) 
            {
                log.info("*");
                try {
                    // assemble the file from it chunks.
                    File file = makeFileFromChunks(request);
                    log.info("** " + file);
                    if (file != null){
                        completedFile = file;
                    }
                } catch (IOException ex) {
                    log.error("* error * " + ex);
                    // if the assembling of a file results in an IOException a
                    // retransmission has to be triggered. Throw the IOException
                    // here and handle it above.
                    throw ex;
                }
            }
        }
        else{
            log.info(chunkPath + " Uploaded");
            this.deleteTempDirectory();
        }
        
        return completedFile;
    }    

    // assembles a file from it chunks
    protected File makeFileFromChunks(HttpServletRequest request) 
            throws IOException
    {
        int resumableTotalChunks = Integer.valueOf(request.getParameter("resumableTotalChunks"));
        String resumableFilename = request.getParameter("resumableFilename");
        String chunkPath = this.uploadDir + File.separator + "part";
        File destFile = null;

        String destFilePath = this.finalDir + File.separator + resumableFilename;
        destFile = new File(destFilePath);
        InputStream is = null;
        OutputStream os = null;
        
        log.info("chunkPath: " + chunkPath);
        log.info("destFilePath: " + destFilePath);

        try {
            destFile.createNewFile();
            os = new FileOutputStream(destFile);

            for (int i = 1; i <= resumableTotalChunks; i++) 
            {
                File fi = new File(chunkPath.concat(Integer.toString(i)));
                try 
                {
                    is = new FileInputStream(fi);

                    byte[] buffer = new byte[1024];

                    int length;

                    while ((length = is.read(buffer)) > 0) 
                    {
                        os.write(buffer, 0, length);
                    }
                } 
                catch (IOException e) 
                {
                    log.info("jings: ");
                    log.info(e);
                    // try to delete destination file, as we got an exception while writing it.
                    if(!destFile.delete())
                    {
                        log.warn("While writing an uploaded file an error occurred. "
                                + "We were unable to delete the damaged file: " 
                                + destFile.getAbsolutePath() + ".");
                    }
                    
                    // throw IOException to handle it in the calling method
                    throw e;
                }
            }
        } 
        finally 
        {
            try 
            {
                if (is != null) 
                {
                    is.close();
                }
            } 
            catch (IOException ex) 
            {
                // nothing to do here
            }
            try 
            {
                if (os != null) 
                {
                    os.close();
                }
            } 
            catch (IOException ex) 
            {
                // nothing to do here
            }
            
            this.deleteTempDirectory();
        }
        return destFile;
    }
    
    private boolean deleteDirectory(File path) 
    {
        if (path.exists()) 
        {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) 
            {
                if (files[i].isDirectory()) 
                {
                    deleteDirectory(files[i]);
                } 
                else 
                {
                    files[i].delete();
                }
            }
        }
        
        return (path.delete());
    }
    
    private void deleteTempDirectory(){
        if (!deleteDirectory(new File(this.uploadDir))){
            log.warn("Coudln't delete temporary upload path " + this.uploadDir + ", ignoring it.");
        }        
    }
}
