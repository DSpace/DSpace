package cz.cuni.mff.ufal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.reading.AbstractReader;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class PiwikStatisticsReader extends AbstractReader {
	
	private static Logger log = Logger.getLogger(PiwikStatisticsReader.class);
	
    /** The Cocoon response */
    protected Response response;

    /** The Cocoon request */
    protected Request request;
    
    /** Item requesting statistics */
    private Item item = null;

    /** True if user agent making this request was identified as spider. */
    private boolean isSpider = false;
    
    private String rest = "";
    
    
    /** Piwik configurations */
    private static final String PIWIK_API_URL = ConfigurationManager.getProperty("lr", "lr.statistics.api.url");
    private static final String PIWIK_AUTH_TOKEN = ConfigurationManager.getProperty("lr", "lr.statistics.api.auth.token");
    private static final String PIWIK_SITE_ID = ConfigurationManager.getProperty("lr", "lr.statistics.api.site_id");
    private static final String PIWIK_DOWNLOAD_SITE_ID = ConfigurationManager.getProperty("lr", "lr.tracker.bitstream.site_id");
    
    /**
     * Set up the PiwikStatisticsReader
     *
     * See the class description for information on configuration options.
     */
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters par) throws ProcessingException, SAXException, IOException {
        super.setup(resolver, objectModel, src, par);
        
        try
        {
        	
            Context context = ContextUtil.obtainContext(objectModel);
        	
            this.request = ObjectModelHelper.getRequest(objectModel);
            this.response = ObjectModelHelper.getResponse(objectModel);
            
            rest = par.getParameter("rest", null);
            
            String handle = par.getParameter("handle", null);
            
            this.isSpider = par.getParameter("userAgent", "").equals("spider");
            
         	// Reference by an item's handle.
            DSpaceObject dso = dso = HandleManager.resolveToObject(context, handle);

            if (dso instanceof Item) {
                item = (Item)dso;                
            } else {
            	throw new ResourceNotFoundException("Unable to locate item");
            }

            EPerson eperson = context.getCurrentUser();
            
            if(!(AuthorizeManager.isAdmin(context) || item.getSubmitter().getID()==eperson.getID())) {
            	throw new AuthorizeException();
            }
            
        } catch (AuthorizeException | SQLException | IllegalStateException e) {
            throw new ProcessingException("Unable to read piwik statistics", e);
        }

    }


	@Override
	public void generate() throws IOException, SAXException, ProcessingException {
		try {
			String queryString = request.getQueryString();
			
			String module = request.getParameter("module");
			String method = request.getParameter("method");
			String format = request.getParameter("format");
			
			if(format==null || format.isEmpty()) {
				format = "xml";
			}
			
			if(module!=null && !module.equals("API")) {
				throw new Exception("Only piwik module=API requests are processed.");
			}
			
			
			if(method!=null && method.equals("API.get")) {
				Calendar cal = Calendar.getInstance();
				Date today = cal.getTime();
				cal.add(Calendar.DATE, -7);
				Date weekBefore = cal.getTime();
				
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				
				String url = PIWIK_API_URL + rest + "?" + queryString + "&date=" + df.format(weekBefore) + "," + df.format(today) + "&idSite=" + PIWIK_SITE_ID + "&token_auth=" + PIWIK_AUTH_TOKEN + "&segment=pageUrl=@" + item.getHandle();																			
				String downloadURL = PIWIK_API_URL + rest + "?" + queryString + "&date=" + df.format(weekBefore) + "," + df.format(today) + "&idSite=" + PIWIK_DOWNLOAD_SITE_ID + "&token_auth=" + PIWIK_AUTH_TOKEN + "&segment=pageUrl=@" + item.getHandle();				
				String report = PiwikHelper.readFromURL(url);
				String downloadReport = PiwikHelper.readFromURL(downloadURL);
				
				String merge = "";

				if(format.equalsIgnoreCase("xml")) {
					merge = PiwikHelper.mergeXML(report, downloadReport);
				} else
				if(format.equalsIgnoreCase("json")) {
					merge = PiwikHelper.mergeJSON(report, downloadReport);
				}
				
				out.write(merge.getBytes());
			} else {
				String url = PIWIK_API_URL + rest + "?" + queryString  + "&idSite=" + PIWIK_SITE_ID + "&token_auth=" + PIWIK_AUTH_TOKEN + "&segment=pageUrl=@" + item.getHandle();
				String report = PiwikHelper.readFromURL(url);
				out.write(report.getBytes());
			}
			
			out.flush();
			
		} catch (Exception e) {
			throw new ProcessingException("Unable to read piwik statisitcs", e);
		}
	}	
	
}


