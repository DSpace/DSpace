package cz.cuni.mff.ufal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLEncoder;
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
    private static final String PIWIK_API_URL = ConfigurationManager.getProperty(
		"lr", "lr.statistics.api.url");
    private static final String PIWIK_AUTH_TOKEN = ConfigurationManager.getProperty(
		"lr", "lr.statistics.api.auth.token");
    private static final String PIWIK_SITE_ID = ConfigurationManager.getProperty(
		"lr", "lr.statistics.api.site_id");
	private static final String PIWIK_DOWNLOAD_SITE_ID = ConfigurationManager.getProperty(
		"lr", "lr.tracker.bitstream.site_id");
	
	private static final int PIWIK_SHOW_LAST_N_DAYS = ConfigurationManager.getIntProperty(
		"lr", "lr.statistics.show_last_n", 7);

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
            
            if(eperson == null) {
            	throw new AuthorizeException();
            }
            
            /*if(!(AuthorizeManager.isAdmin(context) || item.getSubmitter().getID()==eperson.getID())) {
            	throw new AuthorizeException();
            }*/
            
        } catch (AuthorizeException | SQLException | IllegalStateException e) {
            throw new ProcessingException("Unable to read piwik statistics", e);
        }

    }


	@Override
	public void generate() throws IOException, SAXException, ProcessingException {
		try {
			// should contain the period
			String queryString = request.getQueryString();

			String format = request.getParameter("format");
			if(format==null || format.isEmpty()) {
				format = "xml";
			}
			int last_n = PIWIK_SHOW_LAST_N_DAYS;
			String last_n_param = request.getParameter("last_n");
			if (null != last_n_param) {
				last_n = Integer.parseInt(last_n_param);
			}

			Calendar cal = Calendar.getInstance();
			Date startDate = cal.getTime();
			cal.add(Calendar.DATE, -last_n);
			Date endDate = cal.getTime();
			
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");			
			
			if(request.getParameter("date")!=null) {
				String dt[] = request.getParameter("date").split(",");
				startDate = df.parse(dt[1]);
				endDate = df.parse(dt[0]);
			}
			
			String period = request.getParameter("period");

			String urlParams =
				  "&date=" + df.format(endDate) + "," + df.format(startDate)
				+ "&period=" + period
				+ "&idSite=" + PIWIK_SITE_ID
				+ "&token_auth=" + PIWIK_AUTH_TOKEN
				+ "&segment=pageUrl=@" + item.getHandle()
				+ "&columns=nb_pageviews,nb_uniq_pageviews,nb_uniq_visitors,nb_visits";
			String downloadUrlParams =
				  "&date=" + df.format(endDate) + "," + df.format(startDate)
				+ "&period=" + period
				+ "&idSite=" + PIWIK_DOWNLOAD_SITE_ID
				+ "&token_auth=" + PIWIK_AUTH_TOKEN
				+ "&segment=pageUrl=@" + item.getHandle()
				+ "&columns=nb_pageviews,nb_uniq_pageviews";
			String bitstreamWiseDownloadCount = "method=Actions.getPageUrls"
				+ "&date=" + df.format(endDate) + "," + df.format(startDate)
				+ "&period=range"
				+ "&idSite=" + PIWIK_DOWNLOAD_SITE_ID
				+ "&include_aggregate_rows=0"
				+ "&flat=1"
				+ "&token_auth=" + PIWIK_AUTH_TOKEN
				+ "&showColumns=url,nb_hits"
				+ "&segment=pageUrl=@" + item.getHandle();


			final boolean multi_requests = false;
			String mergedResult = "";
			queryString += "&token_auth=" + PIWIK_AUTH_TOKEN + "&module=API";
			String piwikApiGetQuery = "method=API.get";

			if ( multi_requests ) {

				String report = PiwikHelper.readFromURL(
					PIWIK_API_URL + rest + "?" + queryString + "&" + piwikApiGetQuery + urlParams
				);
				String downloadReport = PiwikHelper.readFromURL(
					PIWIK_API_URL + rest + "?" + queryString + "&" + piwikApiGetQuery + downloadUrlParams
				);

				if (format.equalsIgnoreCase("xml")) {
					mergedResult = PiwikHelper.mergeXML(report, downloadReport);
				} else if (format.equalsIgnoreCase("json")) {
					mergedResult = PiwikHelper.mergeJSON(report, downloadReport);
				}
			}else {
				String piwikBulkApiGetQuery = "module=API&method=API.getBulkRequest&format=JSON"
						+ "&token_auth=" + PIWIK_AUTH_TOKEN;

				String url0 = URLEncoder.encode(piwikApiGetQuery + urlParams, "UTF-8");
				String url1 = URLEncoder.encode(piwikApiGetQuery + downloadUrlParams, "UTF-8");
				String url2 = URLEncoder.encode(bitstreamWiseDownloadCount, "UTF-8");
				String report = PiwikHelper.readFromURL(
					PIWIK_API_URL + rest + "?"
						+ piwikBulkApiGetQuery
						+ "&urls[0]=" + url0
						+ "&urls[1]=" + url1
						+ "&urls[2]=" + url2
				);
				//mergedResult = PiwikHelper.mergeJSONResults(report);
				mergedResult = report;

			}

			out.write(mergedResult.getBytes());
			out.flush();
			
		} catch (Exception e) {
			throw new ProcessingException("Unable to read piwik statisitcs", e);
		}
	}	
	
}



