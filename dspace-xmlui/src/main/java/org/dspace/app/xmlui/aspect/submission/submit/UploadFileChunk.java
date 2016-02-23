/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.submission.submit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.curate.Curator;

/**
 * Action that responds to requests related to resumable upload.
 */
public class UploadFileChunk extends AbstractAction
{
    private static final Logger log = Logger.getLogger(UploadFileChunk.class);
    
    private static String tempDir;    
    private String submissionDir;
    private String chunkDir;
    private String handle;
    
    static
    {
        if (ConfigurationManager.getProperty("upload.temp.dir") != null)
        {
            tempDir = ConfigurationManager.getProperty("upload.temp.dir");
        }
        else
        {
            tempDir = System.getProperty("java.io.tmpdir");
        }
    }
    
    /**
     * Set up directories for resumable upload.
     * @param request
     */
    private void init(Request request)
    {
        // parent directory containing all bitstreams related to a submission
        this.submissionDir = tempDir + File.separator + request.getParameter("submissionId");
        
        // directory containing chunks of an individual bitstream
        this.chunkDir = this.submissionDir + File.separator + request.getParameter("resumableIdentifier");
        
        // create upload directories if required
        File uploadDir = new File(this.submissionDir);
        if(!uploadDir.exists())
        {
            uploadDir.mkdir();
        }
        
        if(this.chunkDir != null && this.chunkDir.length() > 0)
        { 
            File finalDir = new File(this.chunkDir);
            if (!finalDir.exists())
            {
                finalDir.mkdir();
            }
        }        
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.cocoon.acting.Action#act(org.apache.cocoon.environment.Redirector, org.apache.cocoon.environment.SourceResolver, java.util.Map, java.lang.String, org.apache.avalon.framework.parameters.Parameters)
     */
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
        
        this.handle = parameters.getParameter("handle", null);
        HashMap<String, String> returnValues = null;
        
        if(request.getMethod().equals("GET"))
        {
            if(doGetResumable(request, response))
            {
                log.info("ok");
            }
            
            returnValues = new HashMap<String, String>();
        }
        else if(request.getMethod().equals("POST"))
        {
            returnValues = new HashMap<String, String>();
            File completedFile = doPostResumable(request);
            
            if(completedFile != null)
            {
                SubmissionInfo si = FlowUtils.obtainSubmissionInfo(
                        objectModel, 'S' + request.getParameter("submissionId"));
                Item item = si.getSubmissionItem().getItem();
                Context context = ContextUtil.obtainContext(objectModel);
                Bitstream b = this.createBitstream(context, completedFile, item);

                // delete upload 
                if(!deleteDirectory(new File(this.submissionDir)))
                {
                    log.warn("Coudln't delete submission upload path " + this.submissionDir + ", ignoring it.");
                }

                if(b == null)
                {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                }
                else
                {
                    log.info("bitstream created " + b.getID() + " for " +
                            this.handle);

                    context.commit();
                    returnValues.put("bitstream", String.valueOf(b.getID()));
                }
            }
            else
            {
                returnValues.put("status", "200");
            }
        }
        else if(request.getMethod().equals("DELETE"))
        {
            if(this.doDeleteResumable(request, objectModel))
            {
                returnValues = new HashMap<String, String>();
                returnValues.put("status", "200");
            }
        }

        return returnValues;
    }
    
    /**
     * Resumable.js uses HTTP GET to recognize whether a specific part/chunk of
     * a file was uploaded already. This method handles those requests.
     * @param request
     * @param response
     * @return
     * @throws IOException
     */
    private boolean doGetResumable(
            HttpServletRequest request,
            HttpServletResponse response) 
        throws IOException
    {
        boolean exists = false;
        
        int resumableTotalChunks =
                Integer.parseInt(request.getParameter("resumableTotalChunks").toString());
        long resumableCurrentChunkSize = 
                Long.valueOf(request.getParameter("resumableCurrentChunkSize"));

        String chunkPath;
        if(resumableTotalChunks == 1)
        {
            // there is only one chunk give it the name of the file
            chunkPath = this.submissionDir + File.separator +
                    request.getParameter("resumableFilename").toString(); 
        }
        else
        {
            // use the String "part" and the chunkNumber as filename of a chunk
            chunkPath = this.chunkDir + File.separator + "part" +
                    request.getParameter("resumableChunkNumber").toString();
        }
        
        File chunkFile = new File(chunkPath);
        // if the chunk was uploaded already, we send a status code of 200
        if(chunkFile.exists())
        {
            if(chunkFile.length() == resumableCurrentChunkSize)
            {
                response.setStatus(HttpServletResponse.SC_OK);
                exists = true;
            }
            
            // The chunk file does not have the expected size, delete it and 
            // pretend that it wasn't uploaded already.
            chunkFile.delete();
        }
        else{
            // if we don't have the chunk send a http status code 204 No content
            response.sendError(HttpServletResponse.SC_NO_CONTENT);
        }
        
        return exists;
    }

    /**
     * Resumable.js sends chunks of files using HTTP POST. If a chunk was the
     * last missing one, we have to assemble the file and return it. If other
     * chunks are missing, we just return null.
     * @param request
     * @return
     * @throws FileSizeLimitExceededException
     * @throws IOException
     * @throws ServletException
     */
    private File doPostResumable(Request request)
            throws FileSizeLimitExceededException, IOException, ServletException 
    {
        File completedFile = null;
        
        int resumableTotalChunks =
                Integer.parseInt(request.get("resumableTotalChunks").toString());
        int resumableChunkNumber =
                Integer.parseInt(request.get("resumableChunkNumber").toString());
        
        // determine name of chunk
        String chunkPath;
        if(resumableTotalChunks == 1)
        {
            // there is only one chunk give it the name of the file
            chunkPath = this.submissionDir + File.separator +
                    request.get("resumableFilename").toString(); 
        }
        else
        {
            chunkPath = this.chunkDir + File.separator + "part" +
                    request.get("resumableChunkNumber").toString();
        }
        
        // cocoon will have uploaded the chunk automatically
        // now move it to temporary directory
        File chunkOrg = ((PartOnDisk)request.get("file")).getFile();
        File chunk = new File(chunkPath);
        chunkOrg.renameTo(chunk);
        
        log.debug("rename file " + ((PartOnDisk)request.get("file")).getFile() + " to " + chunkPath);
        
        boolean foundAll = false;
        
        if(resumableTotalChunks > 1)
        {
        	if(resumableChunkNumber == resumableTotalChunks)
        	{
        	    int noOfChunks = new File(this.chunkDir).listFiles().length; 
        		if(noOfChunks == resumableTotalChunks)
        		{
        		    foundAll = true;
        		}
        		else
        		{
        		    String msg = "something has gone wrong with " + this.handle +
        		            ", we have received last chunk but some are missing: expected " +
        		            resumableTotalChunks + " , have: " + noOfChunks;
        		    log.error(msg);
        		    throw new RuntimeException(msg);
        		}        	
            }
            
            if (foundAll) 
            {
                try
                {
                    // assemble the file from it chunks.
                    File file = makeFileFromChunks(request);
                    if (file != null)
                    {
                        completedFile = file;
                    }
                }
                catch (IOException ex)
                {
                    log.error("* error * " + ex);
                    // if the assembling of a file results in an IOException a
                    // retransmission has to be triggered. Throw the IOException
                    // here and handle it above.
                    throw ex;
                }
            }
        }
        else
        {
            log.debug(chunkPath + " Uploaded");
            
            completedFile = chunk;
        }
        
        return completedFile;
    }    
    
    /**
     * Delete previously uploaded bitstream
     * @param request
     * @param objectModel
     * @return True if bitstream successfully deleted.
     */
    private boolean doDeleteResumable(
            Request request,
            @SuppressWarnings("rawtypes") Map objectModel)
    {
        boolean success = false;
        
        try
        {
            Context context = ContextUtil.obtainContext(objectModel);
            String sBid = request.getParameter("bitstreamId");
            
            if(sBid != null && sBid.length() > 0)
            {
                // delete bitstream from dspace item
                int bid = Integer.parseInt(sBid);
                Bitstream bitstream = Bitstream.find(context, bid);

                if(bitstream != null)
                {
                    log.info("delete " + bitstream.getID());

                    // remove bitstream from bundle..
                    // delete bundle if it's now empty
                    Bundle[] bundles = bitstream.getBundles();
                    bundles[0].removeBitstream(bitstream);
                    Bitstream[] bitstreams = bundles[0].getBitstreams();

                    // remove bundle if it's now empty
                    if(bitstreams.length < 1)
                    {
                        SubmissionInfo si = FlowUtils.obtainSubmissionInfo(
                                objectModel, 'S' + request.getParameter("submissionId"));
                        Item item = si.getSubmissionItem().getItem();

                        item.removeBundle(bundles[0]);
                        item.update();
                    }

                    success = true;
                }
                else
                {
                    log.error("Bitstream " + bid + " not found. Can't delete");
                }
            }
            else
            {
                // delete incomplete upload from disk
                if(!deleteDirectory(new File(this.chunkDir)))
                {
                    log.warn("Could not delete incomplete upload: " + chunkDir);
                }
                else{
                    log.info(chunkDir + " deleted");
                    success = true;
                }
            }
        }
        catch(NumberFormatException nfe)
        {
            log.error("Invalid bitstream id " +
                    request.getParameter("bitstreamId") + " Can't delete");
        }
        catch(SQLException ex)
        {
            log.error(ex);
        }
        catch(AuthorizeException ex)
        {
            log.error(ex);
        }
        catch(IOException ex){
            log.error(ex);
        }

        return success;
    }
            
    /**
     * Assemble a file from it chunks
     * @param request
     * @return newly assembled file
     * @throws IOException
     */
    private File makeFileFromChunks(HttpServletRequest request) 
            throws IOException
    {
        int resumableTotalChunks =
                Integer.valueOf(request.getParameter("resumableTotalChunks"));
        String resumableFilename = request.getParameter("resumableFilename");
        String chunkPath = this.chunkDir + File.separator + "part";
        File destFile = null;

        String destFilePath = this.submissionDir + File.separator +
                resumableFilename;
        destFile = new File(destFilePath);
        OutputStream os = null;

        try {
            destFile.createNewFile();
            os = new FileOutputStream(destFile);

            for (int i = 1; i <= resumableTotalChunks; i++) 
            {
                File fi = new File(chunkPath.concat(Integer.toString(i)));
                try 
                {
                    InputStream is = new FileInputStream(fi);

                    byte[] buffer = new byte[1024];

                    int length;

                    while ((length = is.read(buffer)) > 0) 
                    {
                        os.write(buffer, 0, length);
                    }
                    
                    try 
                    {
                        is.close();
                    } 
                    catch (IOException ex){}
                }
                catch (IOException e) 
                {
                    log.warn(e);
                    // try to delete destination file,
                    // as we got an exception while writing it.
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
                if (os != null) 
                {
                    os.close();
                }
            } 
            catch (IOException ex){}
        }
        
        return destFile;
    }
    
    /**
     * Create DSpace bitstream from file.
     * @param context
     * @param file
     * @param item
     * @return new DSpace bitstream.
     */
    private Bitstream createBitstream(Context context, File file, Item item)
    {
        Bitstream b = null;
        int status = Curator.CURATE_SUCCESS;
        String scan = ConfigurationManager.getProperty(
                "submission-curation", "virus-scan");
        if(!scan.equalsIgnoreCase("false"))
        {
            status = this.virusCheck(file);
        }
        
        if(status == Curator.CURATE_ERROR)
        {
            log.error("Problem with virus checker");
        }
        else if (status == Curator.CURATE_FAIL)
        {
            log.warn(file + " failed virus check");
        }
        else
        {
            try
            {
                FileInputStream fis = new FileInputStream(file);

                // do we already have a bundle?
                Bundle[] bundles = item.getBundles("ORIGINAL");


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

                // Check for virus
                b.setName(file.getName()); 
                b.setSource(file.getAbsolutePath());

                // identify the format
                b.setFormat(FormatIdentifier.guessFormat(context, b));

                b.update();
                item.update();
            }
            catch(FileNotFoundException ex)
            {
                log.error("Can't find file " + file + ": " + ex);
            }
            catch(SQLException ex)
            {
                log.error(ex);
            }
            catch(AuthorizeException ex)
            {
                log.error(ex);
            }
            catch(IOException ex){
                log.error(ex);
            }

        }
        
        return b;
    }
        
    /**
     * Is file virus free? Check using clamdscan process.
     * @param file The file to check for virus.
     * @return Curator.CURATE_SUCCESS if file is virus free?
     */
    private int virusCheck(File file)
    { 
        int bstatus = Curator.CURATE_FAIL;
        
        final Pattern PATTERN = Pattern.compile(".*Infected files: 0.*");
        BufferedReader stdInput = null;
        
        try
        {
            // spawn clamscan process and wait for result
            Process p = Runtime.getRuntime().exec(
                    new String[] {"clamdscan", file.getPath()});
           
            p.waitFor();
            int retVal = p.exitValue();
            if(retVal == 0)
            {
                // good status returned check if pattern is in output 
                stdInput = new BufferedReader(
                        new InputStreamReader(p.getInputStream()));
                
                String s = null;
                while((s = stdInput.readLine()) != null)
                {
                    Matcher matchHandle = PATTERN.matcher(s);
                    if(matchHandle.find())
                    {
                        bstatus = Curator.CURATE_SUCCESS;
                        break;
                    }
                }
            }
            else
            {
                bstatus = Curator.CURATE_ERROR;
            }
        }
        catch(InterruptedException ex){log.warn(ex);}
        catch(IOException ex){log.warn(ex);}
        finally
        {
            if(stdInput != null)
            {
                try{stdInput.close();} catch (Exception e){log.warn(e);}
            }
        }

        if(bstatus != Curator.CURATE_SUCCESS)
        {
            log.warn("*** File " + file + " has failed virus check. status = " + bstatus);
        }

        return bstatus;
    }

    /**
     * Delete directory and children from file system.
     * @param path
     * @return True is successful.
     */
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
}
