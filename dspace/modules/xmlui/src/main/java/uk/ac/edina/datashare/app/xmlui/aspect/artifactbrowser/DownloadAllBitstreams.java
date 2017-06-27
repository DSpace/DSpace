package uk.ac.edina.datashare.app.xmlui.aspect.artifactbrowser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.mail.internet.MimeUtility;

import org.apache.avalon.excalibur.pool.Recyclable;
import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.reading.AbstractReader;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.xml.sax.SAXException;

public class DownloadAllBitstreams extends AbstractReader implements Recyclable
{ 
	private Logger LOG = Logger.getLogger(DownloadAllBitstreams.class);
    
    private List<Bitstream> files = null;
    private String fileName = null;
    
    private long bitstreamSize = 0;
    private static final int EXPIRES = 60 * 60 * 60000;
    
    /*
     * (non-Javadoc)
     * @see org.apache.cocoon.reading.AbstractReader#setup(org.apache.cocoon.environment.SourceResolver, java.util.Map, java.lang.String, org.apache.avalon.framework.parameters.Parameters)
     */
    @SuppressWarnings({ "rawtypes" })
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters par) throws ProcessingException, SAXException,
            IOException
    {
        super.setup(resolver, objectModel, src, par);
        
        try
        {
            this.files = null;
            this.fileName = par.getParameter("file_name");
            
            Context context = ContextUtil.obtainContext(objectModel);
            
            // get the item from handle
            Object dso = HandleManager.resolveToObject(
                    context,
                    par.getParameter("handle"));
            
            if (dso instanceof Item)
            {
                Item item = (Item)dso;
                
                if(org.dspace.app.util.Util.allowDownloadAll(context, item))
                {
                    Bundle bundle[] = item.getBundles();
                    this.files = new ArrayList<Bitstream>();
              
                    // loop round bundles, there should be two - files and licences
                    for(int i = 0; i < bundle.length; i++)
                    {
                        // now get the actual bitstreams
                        Bitstream bitstreams[] = bundle[i].getBitstreams();
                        
                        for(int j = 0; j < bitstreams.length; j++)
                        {
                            // only add bitstream if valid 
                            if(validBitstream(context, bitstreams[j]))
                            {
                                this.files.add(bitstreams[j]);
                                this.bitstreamSize += bitstreams[j].getSize();
                            }
                        }
                    }
                    
                    // log download request 
                    // Request request = ObjectModelHelper.getRequest(objectModel);
                    LOG.info(this.fileName);
                    //new IRUSUKLog(context, request, item, this.fileName);
                }
                else
                {
                	LOG.warn("Download all attempted for " + item.getName());
                	Response response = ObjectModelHelper.getResponse(objectModel);
                	response.setStatus(404);
                }
            }
        }
        catch(ParameterException ex)
        {
            // empty
        }
        catch(SQLException ex)
        {
            throw new RuntimeException(ex);
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.cocoon.reading.Reader#generate()
     */
    public void generate() throws IOException, SAXException, ProcessingException
    {
        if(this.files != null && this.files.size() > 0)
        {
            Response response = ObjectModelHelper.getResponse(objectModel);
            response.setDateHeader("Expires", System.currentTimeMillis() + EXPIRES);
        
            // If this is a large bitstream then tell the browser it should treat it as a download.
            int threshold = ConfigurationManager.getIntProperty("xmlui.content_disposition_threshold");
            
            if (bitstreamSize > threshold && threshold != 0)
            {
                String name  = this.fileName;
            
                // Try and make the download file name formated for each browser.
                try
                {
                    Request request = ObjectModelHelper.getRequest(objectModel);
                    String agent = request.getHeader("USER-AGENT");
                    
                    if (agent != null && agent.contains("MSIE"))
                    {
                        name = URLEncoder.encode(name,"UTF8");
                    }
                    else if (agent != null && agent.contains("Mozilla"))
                    {
                        name = MimeUtility.encodeText(name, "UTF8", "B");
                    }
                }
                catch (UnsupportedEncodingException see)
                {
                    // do nothing
                }
                
                response.setHeader("Content-Disposition", "attachment;filename=\"" + name + "\""); 
            }
            
            ZipOutputStream zip = new ZipOutputStream(out);
            zip.setLevel(0);
            
            // put each file in zip
            for(Bitstream file : this.files)
            {
                ZipEntry entry = new ZipEntry(file.getName());
                zip.putNextEntry(entry);
                
                try
                {
                    read(file.retrieve(), zip);
                }
                catch(Exception ex)
                {
                    throw new RuntimeException(ex);
                }
            }
            
            zip.finish();
            zip.flush();
        }
    }
    
    /**
     * Read input stream into output stream.
     * @param in Input stream
     * @param out Output stream
     * @throws IOException
     */
    private void read(InputStream in, OutputStream out) throws IOException
    {
        final byte[] BUFFER = new byte[8192];
        int length = -1;
        while ((length = in.read(BUFFER)) > -1)
        {
            out.write(BUFFER, 0, length);
        }
        
        in.close();
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.cocoon.reading.AbstractReader#getMimeType()
     */
    public String getMimeType()
    {
        String mimeType = "application/zip";
        
        if(this.files == null)
        {
            // if there are no files to process change mime type to html 
            mimeType = "text/html";
        }
        
        return mimeType;
    }
    
    /**
     * A bitstream is valid if user has permission to read and the source is not
     * a format filter. The depositor agreement is also excluded from the
     * download.
     * @param context DSpace context.
     * @param bitstream The bitstream to check.
     * @return True is bitstream should be added to the zip file.
     */
    private boolean validBitstream(Context context, Bitstream bitstream)
    {
        boolean valid = false;
        
        final String WBFF = "Written by FormatFilter";
        
        if(!bitstream.getSource().startsWith(WBFF) &&
        		!bitstream.getName().equals(Constants.LICENSE_BITSTREAM_NAME))
        {
            try
            {
                valid = AuthorizeManager.authorizeActionBoolean(
                        context,
                        bitstream,
                        Constants.READ);
            }
            catch(SQLException ex)
            {
                throw new RuntimeException(ex);
            }
        }
        
        return valid;
    }
}
