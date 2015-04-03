/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.statistics;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;

//
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.analytics.Analytics;
import com.google.api.services.analytics.Analytics.Data.Ga.Get;
import com.google.api.services.analytics.AnalyticsScopes;
import com.google.api.services.analytics.model.GaData;
import com.google.api.services.analytics.model.GaData.ColumnHeaders;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;   
import com.ibm.icu.text.SimpleDateFormat;


/**
 * modified for LINDAT/CLARIN
*/
public class GAStatisticsTransformer extends AbstractDSpaceTransformer {

	private static Logger log = Logger.getLogger(GAStatisticsTransformer.class);

    private static final Message T_dspace_home = message("xmlui.general.dspace_home");
    private static final Message T_head_title = message("xmlui.statistics.ga.title");
    private static final Message T_statistics_trail = message("xmlui.statistics.ga.trail");

    // google stuff
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();
    
    // ga analytics
	private static String key_file = null;
	private static String profile_id = null;
	private static String account_email = null;

	static {
		try {
			key_file = ConfigurationManager.getProperty("lr", "lr.ga.analytics.key.file");
			profile_id = ConfigurationManager.getProperty("lr", "lr.ga.analytics.profile.id");
			account_email = ConfigurationManager.getProperty("lr", "lr.ga.analytics.account.email");
		}catch( Exception e ) {
		}
	}
	
	
    
    /**
     * Add a page title and trail links
     */
    public void addPageMeta(PageMeta pageMeta) 
    		throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        pageMeta.addTrail().addContent(T_statistics_trail);
        // Add the page title
        pageMeta.addMetadata("title").addContent(T_head_title);
        pageMeta.addMetadata("include-library", "statistic-map");
        pageMeta.addMetadata("include-library", "datepicker");
    }

    /**
	 * What to add at the end of the body
	 */
	public void addBody(Body body) throws SAXException, WingException,
			UIException, SQLException, IOException, AuthorizeException {

		try {
			//
			renderGA(body);
        } catch (RuntimeException e) {
            throw e;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public void renderGA(Body body) throws WingException {
		
        //Try to find our dspace object
        DSpaceObject dso = null;
		try {
			dso = HandleUtil.obtainHandle(objectModel);
		} catch (SQLException e1) {
		}
		
		String name = "Main page";
		String url = null;
		if(dso != null) {
			name = dso.getName();
			url = contextPath + "/" + sitemapURI.replaceAll( "\\/[^/]*$", "" );
		}
		
		
		Division home = body.addDivision("home", "primary repository");
		Division division = home.addDivision("stats", "secondary stats");
		division.setHead(T_head_title);
		try {
			
			String start_date = this.parameters.getParameter("start");
			String end_date = this.parameters.getParameter("end");
			String filters = this.parameters.getParameter("filters");
			String dimensions = this.parameters.getParameter("dim");
			
			if(!DateHelper.checkDate(start_date)){
				start_date = DateHelper.today_one_month_ago();
			}
			if(!DateHelper.checkDate(end_date)){
				end_date = DateHelper.today_string();
			}
			Analytics analytics = initializeAnalytics();
			
			if(url != null && !url.isEmpty()) {
				if(filters == null) { 
					filters = "ga:pagePath=@" + url;
				} else {
					filters += "ga:pagePath=@" + url;
				}
			}
			
			
			if(dimensions==null || dimensions.isEmpty()) {
				dimensions = "ga:country,ga:visitorType,ga:networkDomain,ga:hostname";				
			}
			
			if(filters != null && filters.contains("country") && !filters.contains("city")) {
				dimensions.replace("ga:country", "ga:city");
			}

			
			
			GaData q = getCountries(analytics, start_date, end_date, dimensions, filters);
			add_header_table( division, q, start_date, end_date, name );
			
			// place holder for map
			division.addDivision( "chart_id", "ga_chart" );
			
			division.addPara( " " );
			add_data( division, q, "ga-countries" );
			
		} catch (Exception e) {
		    division.addPara("errors", "alert alert-danger").addContent(
		                    String.format("Could not get statistics: %s", e.toString()) );
			log.error("Error occurred while creating statistics for home page", e);
		}

	} // renderGA
	
	
	//
	//
	public static java.util.List<String[]> get_info( GaData q ) {
		java.util.List<String[]> info = new ArrayList<String[]>();
		info.add( new String[] { 
				"Profile name", 
				q.getProfileInfo().getProfileName()
				} );
		info.add( new String[] { 
				"Account id", 
				q.getProfileInfo().getAccountId()
				} );
		try {
			info.add( new String[] { 
					"Query", 
					q.getQuery().toPrettyString()
					} );
		} catch (Exception e) {
		}
		return info;
	}
	
	private String extractTotalVisits(GaData q) {
		final String visitsKey = "ga:visits";
		String totalVisits = "N/A";
		Map<String, String> totals = q.getTotalsForAllResults();
		
		if(totals != null) {			
			if(totals.get(visitsKey) != null) {
				totalVisits = totals.get(visitsKey);
			}
		}
		
		return totalVisits;
	}
	
	
	private void add_header_table( Division division, GaData q, String start_date, String end_date, String name ) throws WingException {
		java.util.List<String[]> info = get_info(q);
		Table table = division.addTable("ga-info", 2, info.size(), "ga-header");
		for ( String[] ss : info ) {
			// show all but query info
			Row r = ss[0] != "Query" ? table.addRow() : table.addRow(null, null, "hidden");
			for ( String s : ss ) {
				r.addCell().addContent(s);
			}
		}
		// time range
		Row r = table.addRow();
		r.addCell().addContent( "Statistics for" );
		r.addCell().addContent( name );
		//
		r = table.addRow();
		r.addCell().addContent("Time range");
		
		Cell c = r.addCell();				
		Text sDate = c.addHighlight("col-md-4").addText("start_date", "form-control");
		sDate.setValue(start_date);
		sDate.setLabel("from");
		Text eDate = c.addHighlight("col-md-4").addText("end_date", "form-control");
		eDate.setValue(end_date);
		eDate.setLabel("to");
		c.addHighlight("col-md-2").addButton("update_range", "btn btn-default").setValue("Update");
		
		r = table.addRow( null, null, "totalvisits" );
		r.addCell().addContent( "Total # of visits" );						
		r.addCell().addContent( extractTotalVisits(q) );		
	}

	private void add_data( Division division, GaData q, String name) throws WingException {
		java.util.List<ColumnHeaders> ch = q.getColumnHeaders();
		java.util.List<java.util.List<String>> data = q.getRows();
			if ( data == null )
				return;
		Table table = division.addTable(name, data.get(0).size(), data.size(), "ga-data" );

		Row header = table.addRow();
		for ( ColumnHeaders head : ch ) {
			header.addCell(null, null, "ga-data-header").addContent(head.getName().replace("ga:", ""));
		}

		for ( java.util.List<String> cells : data ) {
			Row r = table.addRow();
			for ( String s : cells ) {
				r.addCell().addContent(s);
			}
		}
	}
	
	
	// ga
	//
	private static Analytics initializeAnalytics() throws Exception  {
		// Set up and return Google Analytics API client.
		Credential credential = authorize();
		return new Analytics.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
	    	.setApplicationName("Dspace GA by UFAL")
	        	.build();
	  }	
	
	
	  /** Authorizes the installed application to access user's protected data. */
	  private static Credential authorize() throws Exception {
		  java.io.File key = new java.io.File(key_file);
			return new GoogleCredential.Builder().setTransport(HTTP_TRANSPORT)
		            .setJsonFactory(JSON_FACTORY)
		            .setServiceAccountId(account_email)
		            .setServiceAccountScopes(Collections.singleton(AnalyticsScopes.ANALYTICS_READONLY))
		            .setServiceAccountPrivateKeyFromP12File(key)
		            .build();
	  }	
	  
	  private static GaData getCountries( Analytics analytics, String start_date, String end_date, String dimensions, String filters) throws IOException {
		    // Query accounts collection.
		  Get q = analytics.data().ga().get("ga:" + profile_id, start_date, end_date, "ga:visits");
		  q.setDimensions(dimensions);
		  q.setSort("-ga:visits");
		  if ( filters != null && !filters.isEmpty() ) {
			  q = q.setFilters(filters);
		  }
		  return q.execute();
	 }
	  

	  //
	  //
	  private static String toString(GaData results) {
		  String tmp = "";
		  if (results != null && null != results.getRows() && !results.getRows().isEmpty()) {
		    tmp = "Profile Name: " + results.getProfileInfo().getProfileName() +  "\n";
		    tmp += "Account id:" + results.getProfileInfo().getAccountId() + "\n";
		    tmp += "Headers:" + results.getColumnHeaders() + "\n";
		    tmp += "Query:" + results.getQuery() + "\n";
		    for ( java.util.List<String> arrs: results.getRows() ) {
		    	tmp += "\n" + arrs;
		    }
		  } else {
		    tmp = "No results found";
		  }
		  return tmp;
		}	  
	  
	  
	  //
	  //
	  //
	  public static void main( String[] args ) {
		  try {
              String start_date = DateHelper.today_one_month_ago();
			  GAStatisticsTransformer.key_file = "g:\\dspace\\ga.api.key.p12";
			  GAStatisticsTransformer.profile_id = "52779327";
			  GAStatisticsTransformer.account_email = "144418224547@developer.gserviceaccount.com";
		      Analytics analytics = initializeAnalytics();
		      String end_date = DateHelper.today_string();
		      //GaData q = getCountries( analytics, start_date, end_date );
		      
		      String filters = "ga:pagePath=@/xmlui/handle/11858/00-097C-0000-0001-4880-3"; 
		      
		      GaData q = getCountries(analytics, start_date, end_date, "ga:country,ga:visitorType,ga:networkDomain,ga:pagePath", filters);
		      for ( String[] ss : get_info( q ) ) {
		    	  String tmp = "";
			      for ( String s : ss ) {
			    	  tmp += " " + s;
			      }
		    	  System.out.println( tmp );
		      }
		      
		      System.out.println( toString( q ) );

		    } catch (GoogleJsonResponseException e) {
		      System.err.println("There was a service error: " + e.getDetails().getCode() + " : "
		          + e.getDetails().getMessage());
		    } catch (Throwable t) {
		      t.printStackTrace();
		    }
	  }
	  
} // class



class DateHelper {
	
	static public String today_string() {
		return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
	}

	static public String today_one_month_ago() {
	    Calendar calendar = Calendar.getInstance();
        calendar.add( Calendar.MONTH ,  -1 );
        Date one_month_ago = calendar.getTime();
		return new SimpleDateFormat("yyyy-MM-dd").format(one_month_ago).toString();
	}

	static public String today_year_string() {
		return new SimpleDateFormat("yyyy").format(new Date()).toString()+"-01-01";
	}
	
	static public boolean checkDate(String date){
		if(date == null || date.length() == 0){
			return false;
		}

		try{
			Date d = new SimpleDateFormat("yyyy-MM-dd").parse(date);
			return d != null;
		}catch(ParseException e){
			//do nothing
		}
		return false;
	}
	
} // class


