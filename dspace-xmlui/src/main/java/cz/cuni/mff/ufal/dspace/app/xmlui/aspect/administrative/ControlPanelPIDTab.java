/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.content.Community;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.icu.text.SimpleDateFormat;

import cz.cuni.mff.ufal.dspace.PIDService;
import cz.cuni.mff.ufal.dspace.handle.PIDCommunityConfiguration;
import cz.cuni.mff.ufal.dspace.handle.PIDConfiguration;
import cz.cuni.mff.ufal.dspace.handle.PIDLogMiner;
import cz.cuni.mff.ufal.dspace.handle.PIDLogStatistics;
import cz.cuni.mff.ufal.dspace.handle.PIDLogStatisticsEntry;

/**
 * 
 */
public class ControlPanelPIDTab extends AbstractControlPanelTab {

	private Request request = null;
	private static Logger log = Logger.getLogger(ControlPanelPIDTab.class);
	
	private static final String UNDEFINED = "undefined";
	
	private static final int PID_STATISTICS_DAYS = 6;
	private static final int PID_STATISTICS_TOP_N = 10;

	private static String get_param(String param, String default_value) {
		String value = ConfigurationManager.getProperty(param);
		if (value == null || value.length() == 0)
			value = default_value;
		return value;
	}
	
	private static String get_param(String module, String param, String default_value) {
		String value = ConfigurationManager.getProperty(param);
		if (value == null || value.length() == 0)
			value = default_value;
		return value;
	}


	@Override
	public void addBody(Map objectModel, Division div) throws WingException {

		request = ObjectModelHelper.getRequest(objectModel);
		
		// info
		String pid_type = get_param("lr", "lr.pid.service.type", "not defined");
		String pid_url = get_param("lr", "lr.pid.service.url", "not defined");
		String pid_user = get_param("lr", "lr.pid.service.user", "not defined");
		String pid_testpid = get_param("lr", "lr.pid.service.testPid", "not defined");
		String all_pids = String.format("%sread/search?creator=%s", pid_url, pid_user);

		addResults(div, all_pids);
		
		List info = div.addList("info");
															
		addPIDConfigurationInfo(info);				

		info = div.addList("local-pid");
		info.setHead("LOCAL PID info");		
		info.addLabel("Handle dir (logs should be there too, {access,error}.log");
		info.addItem(get_param("handle.dir", "not defined"));
		
		info = div.addList("epic-pid");
		info.setHead("EPIC PID info");
		
		info.addLabel("PID type");
        info.addItem(pid_type);
		info.addLabel("PID url");
		info.addItemXref(pid_url, pid_url);
		info.addLabel("PID user");
		info.addItem(pid_user);
		info.addLabel("PID test pid");
		info.addItem(pid_testpid);

		info.addLabel("PID server web app (valid for EPIC!)");
		String webapp_pid = "http://handle.gwdg.de:8080/pidservice/";
		info.addItemXref(webapp_pid, webapp_pid);

		info.addLabel("PID see all created records (use wisely)");
		info.addItemXref(all_pids, all_pids);
		info.addLabel();
		info.addItem().addButton("submit_pid_dummy")
				.setValue("List dummy urls for this instance");

		// test
		List form = div.addList("pid", List.TYPE_FORM, "cp-pid");
		form.setHead("EPIC test helpers");
		Item item = form.addItem();
		item.addButton("submit_pid_whoami").setValue("PID who am I");
		item.addButton("submit_pid_test").setValue("PID test server");
		
		// add PID statistics
		addPIDStatistics(div);

	}
	
	private void addResults(Division div, String all_pids) throws WingException {
		String message = null;
		if (request.getParameter("submit_pid_whoami") != null) {
			message = "PID whoami\n";
			String whoami = null;
			try {
				whoami = PIDService.who_am_i("encoding=xml");
			} catch (Exception e) {
				whoami = e.toString();
			}
			message += whoami;
		} else if (request.getParameter("submit_pid_test") != null) {
			message = "PID test (whoami, search, resolve, modify, resolve - verify)\n";
			try {
				String test_pid = ConfigurationManager.getProperty("lr", "lr.pid.service.testPid");
				if (test_pid == null) {
					message += "Testing PID server not done! Test pid not in dspace.cfg!";
				} else {
					message += PIDService.test_pid(test_pid);
				}
			} catch (Exception e) {
				message += e.toString();
			}
		} else if (request.getParameter("submit_pid_dummy") != null) {
			final String instance_url = ConfigurationManager
					.getProperty("dspace.hostname");
			message = "List of dummy pids with url " + instance_url + ":\n";
			final StringBuilder dummyPids = new StringBuilder();
			try {
				SAXParser saxParser = SAXParserFactory.newInstance()
						.newSAXParser();
				saxParser.parse(new URL(all_pids + "&encoding=xml")
						.openConnection().getInputStream(),
						new DefaultHandler() {
							boolean isUrl = false;
							boolean isPid = false;
							String pid = null;

							public void startElement(String uri,
									String localName, String qName,
									Attributes attributes) throws SAXException {
								if (qName.equals("url")) {
									isUrl = true;
								} else if (qName.equals("pid")) {
									isPid = true;
								}
							}

							public void characters(char ch[], int start,
									int length) throws SAXException {
								String value = new String(ch, start, length);
								if (isPid) {
									pid = value;
									isPid = false;
								}
								if (isUrl) {
									if (value.contains(instance_url) && value.contains("?dummy")) {
										dummyPids.append("Url ").append(value)
												.append(" has pid ")
												.append(pid).append('\n');
									}
									isUrl = false;
								}

							}
						} // DefaultHandler
				);
			} catch (IOException e) {
				message += e.toString();
			} catch (SAXException e) {
				message += e.toString();
			} catch (javax.xml.parsers.ParserConfigurationException e) {
				message += e.toString();
			}

			if (dummyPids.length() > 0) {
				message += dummyPids.toString();
			} else {
				message += "No dummies found.";
			}
		}

		if (message != null) {
			div.addDivision("result", "alert alert-info")
					.addPara("pid-test-output", "programs-result")
					.addContent(message);
		}		
	}
	
	/**
	 * Adds PID Configuration to information list
	 * 
	 * @param info Information list
	 * @throws WingException 
	 */
	private void addPIDConfigurationInfo(List info) throws WingException 
	{
	    String communityConfigurationsParam = get_param("lr", "lr.pid.community.configurations", "not defined");	    
        Map<Integer, PIDCommunityConfiguration> pidCommunityConfigurations = PIDConfiguration.getPIDCommunityConfigurations();
                       
        info.setHead("Configuration");
                
	    info.addLabel("Raw community configurations");
	    info.addItem(communityConfigurationsParam);
	    	    
	    for(Map.Entry<Integer, PIDCommunityConfiguration> entry : pidCommunityConfigurations.entrySet())
	    {	    
	        Integer communityID = entry.getKey();
	        PIDCommunityConfiguration pidCommunityConfiguration = entry.getValue();
	        String communityName = "default";	        
	        if(communityID != null)
	        {
	            try 
	            {
	                Community community = Community.find(context, communityID);
	                communityName = community.getName();	                
	            }
	            catch (SQLException e) 
	            {	                
	                communityName = "non-existing community";
	            }
	            
	        }
	        
	        String communityIDString = "any";
	        if (pidCommunityConfiguration.getCommunityID() != null)
	        {
	            communityIDString = pidCommunityConfiguration.getCommunityID().toString();
	        }	        	        
	        	        
	        String alternativePrefixesString = StringUtils.join(pidCommunityConfiguration.getAlternativePrefixes(), ", ");      
	        
	        info.addLabel("Community");
	        info.addItem(nvl(communityName, UNDEFINED));
	        
	        info.addLabel("--- ID");
	        info.addItem(nvl(communityIDString, UNDEFINED));
	        
	        info.addLabel("--- type");
	        info.addItem(nvl(pidCommunityConfiguration.getType(), UNDEFINED));
	        
	        info.addLabel("--- prefix");
	        info.addItem(nvl(pidCommunityConfiguration.getPrefix(), UNDEFINED));
	        
            info.addLabel("--- alternative prefixes");
            info.addItem(nvl(alternativePrefixesString, UNDEFINED));
	        
	        info.addLabel("--- canonical prefix");
	        info.addItem(nvl(pidCommunityConfiguration.getCanonicalPrefix(), UNDEFINED));
	        	        
	    }
	}
	
	private void addPIDStatistics(Division mainDiv) throws WingException
	{
	    Division div = mainDiv.addDivision("pid_statistics");
	    div.setHead("PID resolution statistics");
	    
	    // set end date to yesterday	    
	    Calendar cal = GregorianCalendar.getInstance();	    
        cal.add(Calendar.DATE, -1);
        Date endDate = cal.getTime();
	    
	    // compute start date as PID_STATISTICS_DAYS days before end date	   
        cal.setTime(endDate);
        cal.add(Calendar.DATE, -PID_STATISTICS_DAYS);        
        Date startDate = cal.getTime();               
	    
	    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");               
                        
        PIDLogMiner logMiner = new PIDLogMiner();
        PIDLogStatistics statistics = logMiner.computeStatistics(startDate, endDate);
        int topN = PID_STATISTICS_TOP_N;
        Map<String, java.util.List<PIDLogStatisticsEntry>> topNEntries= statistics.getTopN(topN);        
        String eventsToDisplay[] = { PIDLogMiner.FAILURE_EVENT, PIDLogMiner.REQUEST_EVENT, PIDLogMiner.SUCCESS_EVENT, PIDLogMiner.UNKNOWN_EVENT };
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");        
        
        for(String event: eventsToDisplay)
        {
            int nRows;
            if(topNEntries.containsKey(event)) {
                nRows = topNEntries.get(event).size();
            }
            else {
                nRows = 0;
            }
            Table table = div.addTable(String.format("pid-statistics-table-%s", event.toLowerCase()), nRows+1, 4);
            
            table.setHead(String.format("Top %d events of type %s between %s and %s\n", topN, event, 
                    dateFormat.format(startDate), 
                    dateFormat.format(endDate)));
            Row r = table.addRow(Row.ROLE_HEADER);
            r.addCell().addContent("Count");
            r.addCell().addContent("PID");
            r.addCell().addContent("First occurence");
            r.addCell().addContent("Last occurence");
                            
            if(topNEntries.containsKey(event)) 
            {                
                for (PIDLogStatisticsEntry entry : topNEntries.get(event))
                {                    
                    r = table.addRow();                    
                    r.addCell().addContent(entry.getCount());
                    r.addCell().addContent(entry.getPID());
                    r.addCell().addContent(dateTimeFormat.format(entry.getFirstOccurence()));
                    r.addCell().addContent(dateTimeFormat.format(entry.getLastOccurence()));                                   
                }                
            }
        }        
	}
	
	private String nvl(String s, String alternative)
	{	    
	    if(s == null || s.isEmpty()) {
	        return alternative;
	    }
	    return s;
	}

} // ControlPanelPIDTab

