package org.datadryad.app.xmlui.aspect.ame;

import java.io.ByteArrayInputStream;

import java.io.IOException;
import java.net.URLEncoder;

import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.reading.AbstractReader;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.xml.sax.SAXException;

/**
 * This Reader serves as a cross domain scripting proxy to allow 
 * a jQuery (or other AJAX-based component) to make a request to the 
 * HIVE server, which may be hosted on a separate domain.
 * 
 * This reader uses item and configuration information from DSpace
 * to construct a request to the HIVE auto-suggest servlet and 
 * proxy the raw JSON content back to the browser.
 * 
 * @author craig.willis@unc.edu
 */
@SuppressWarnings("deprecation")
public class AJAXSuggestReader extends AbstractReader
{
	private static final Logger log = Logger.getLogger(AJAXSuggestReader.class);

	protected Response response;
	
	@Override
	/**
	 * Generate the repsonse
	 */
	public void generate() throws IOException, SAXException, ProcessingException
	{
		this.response = ObjectModelHelper.getResponse(objectModel);
		
		int itemID = -1;
		String field = "";
		String cv = "";
		String mp = "";
		String format = "";
		String text = "";
		String ignore = "";
		
		// Get the HIVE host base URL
		String host = ConfigurationManager.getProperty("ame.host");
		
		try
		{
			Context context = ContextUtil.obtainContext(objectModel);
			itemID = parameters.getParameterAsInteger("itemID");
			
			// Get the text from the item to be used for automatic term suggestion
			Item item = Item.find(context, itemID);
			text += getItemTitle(item);
			text += getItemAbstract(item);	
			text += getItemKeywords(item);
			
			// Get the field to suggest terms for.
			field = parameters.getParameter("field");
					
			// Get the vocabulary, min phrase, and format configured for the selected field
			cv = ConfigurationManager.getProperty("ame." + field + ".cv");
			mp = ConfigurationManager.getProperty("ame." + field + ".mp");
			format = ConfigurationManager.getProperty("ame." + field + ".format");
		
			// Get all existing terms assigned to this item. These will be pre-selected
			// in the returned suggestion list
			ignore = getIgnoreList(field, item);
			
		} catch (Exception e) {
			log.error("Exception: error initializing");
		}

		text = URLEncoder.encode(text, "UTF-8");
		ignore = URLEncoder.encode(ignore, "UTF-8");

		// HIVE server prefix 
        String prefixUrl = URLEncoder.encode(host + "/ConceptBrowser.html?uri=", "UTF-8");    
		
		String url = host + "/suggest?cv=" + cv + "&tx=" + text + "&mp=" + mp + "&fmt=" + format + "&ex=" + ignore + "&pu=" + prefixUrl;
		GetMethod get = new GetMethod(url);
		HttpClient httpClient = new HttpClient();
		httpClient.executeMethod(get);
		
		// Return JSON data directly to browser
		String result = get.getResponseBodyAsString();
		if (result != null)
		{
			ByteArrayInputStream in = new ByteArrayInputStream(result.getBytes("UTF-8"));
			byte[] buffer = new byte[8192];
			
			response.setHeader("Content-Length", String.valueOf(result.length()));
			response.setContentType("text/json");

			int length;
			while ((length = in.read(buffer)) > -1)
				out.write(buffer, 0, length);
			out.flush();
		}
	}
	
	/**
	 * Get the item title
	 */
	public static String getItemTitle(Item item) {
		DCValue[] titles = item.getDC("title", Item.ANY, Item.ANY);

		String title;
		if (titles != null && titles.length > 0) title = titles[0].value;
		else title = null;
		return title;
	}
	
	/**
	 * Get the item keywords
	 */
	public static String getItemKeywords(Item item) {
		String text = " ";
		DCValue[] keywords = item.getMetadata("dc.subject");
		if (keywords != null)
		{
			for (DCValue kw: keywords)
				text += kw.value + ", ";
		}
		
		keywords = item.getMetadata("dwc.ScientificName");
		if (keywords != null) 
		{
			for (DCValue kw: keywords)
				text += kw.value;
		}
		return text;
	}
	
	/**
	 * Get the item description
	 */
	public static String getItemAbstract(Item item) {
		DCValue[] descriptions = item.getMetadata("dc.description.abstract");
		if (descriptions == null || descriptions.length == 0)
			descriptions = item.getMetadata("dc.description");
		

		String description;
		if (descriptions != null && descriptions.length > 0) description = descriptions[0].value;
		else description = null;
		return description;
	}
	
	/**
	 * Get the list of existing terms assigned to this item for the selected field
	 */
	public static String getIgnoreList(String field, Item item) {
		String ignore = "";
		
		DCValue[] keywords = item.getMetadata(field.replace("_", "."));
		if (keywords != null)
		{
			for (DCValue kw: keywords)
				ignore += kw.value + "|";
		}
		
		return ignore;
	}
}
