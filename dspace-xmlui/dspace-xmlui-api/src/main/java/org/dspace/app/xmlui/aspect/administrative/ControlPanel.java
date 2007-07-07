/*
 * ControlPanel.java
 *
 * Version: $Revision: 1.3 $
 *
 * Date: $Date: 2006/07/13 23:20:54 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.xmlui.aspect.administrative;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.cocoon.components.flow.ContinuationsManager;
import org.apache.cocoon.components.flow.WebContinuation;
import org.apache.cocoon.components.flow.WebContinuationDataBean;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.TextArea;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;

/**
 * This page displays important (and some not-so important) systems 
 * type information about your running dspace.
 *
 * @author Jay Paz
 * @author Scott Phillips
 */
public class ControlPanel extends AbstractDSpaceTransformer implements Serviceable{
	
	/** Language Strings */
    private static final Message T_DSPACE_HOME =
        message("xmlui.general.dspace_home");
    private static final Message T_title 				= message("xmlui.administrative.ControlPanel.title");
	private static final Message T_trail	 			= message("xmlui.administrative.ControlPanel.trail");
	private static final Message T_head	 				= message("xmlui.administrative.ControlPanel.head");
	private static final Message T_option_java  		= message("xmlui.administrative.ControlPanel.option_java");
	private static final Message T_option_dspace  		= message("xmlui.administrative.ControlPanel.option_dspace");
	private static final Message T_option_knots  		= message("xmlui.administrative.ControlPanel.option_knots");
	private static final Message T_option_alerts  		= message("xmlui.administrative.ControlPanel.option_alerts");
	private static final Message T_JAVA_HEAD 			= message("xmlui.administrative.ControlPanel.java_head");
	private static final Message T_JAVA_VERSION 		= message("xmlui.administrative.ControlPanel.java_version");
	private static final Message T_JAVA_VENDOR 			= message("xmlui.administrative.ControlPanel.java_vendor");
	private static final Message T_OS_NAME 				= message("xmlui.administrative.ControlPanel.os_name");
    private static final Message T_OS_ARCH 				= message("xmlui.administrative.ControlPanel.os_arch");
    private static final Message T_OS_VERSION 			= message("xmlui.administrative.ControlPanel.os_version"); 
    private static final Message T_RUNTIME_HEAD 		= message("xmlui.administrative.ControlPanel.runtime_head");   
    private static final Message T_RUNTIME_PROCESSORS 	= message("xmlui.administrative.ControlPanel.runtime_processors"); 
    private static final Message T_RUNTIME_MAX 			= message("xmlui.administrative.ControlPanel.runtime_max"); 
    private static final Message T_RUNTIME_TOTAL 		= message("xmlui.administrative.ControlPanel.runtime_total"); 
    private static final Message T_RUNTIME_USED 		= message("xmlui.administrative.ControlPanel.runtime_used"); 
    private static final Message T_RUNTIME_FREE 		= message("xmlui.administrative.ControlPanel.runtime_free"); 
    private static final Message T_DSPACE_HEAD 			= message("xmlui.administrative.ControlPanel.dspace_head");
    private static final Message T_DSPACE_DIR 			= message("xmlui.administrative.ControlPanel.dspace_dir");
    private static final Message T_DSPACE_URL 			= message("xmlui.administrative.ControlPanel.dspace_url");
    private static final Message T_DSPACE_HOST_NAME 	= message("xmlui.administrative.ControlPanel.dspace_hostname");
    private static final Message T_DSPACE_NAME 			= message("xmlui.administrative.ControlPanel.dspace_name");
    private static final Message T_DB_NAME 				= message("xmlui.administrative.ControlPanel.db_name");
    private static final Message T_DB_URL 				= message("xmlui.administrative.ControlPanel.db_url");
    private static final Message T_DB_DRIVER 			= message("xmlui.administrative.ControlPanel.db_driver");
    private static final Message T_DB_MAX_CONN 			= message("xmlui.administrative.ControlPanel.db_maxconnections");
    private static final Message T_DB_MAX_WAIT 			= message("xmlui.administrative.ControlPanel.db_maxwait");
    private static final Message T_DB_MAX_IDLE 			= message("xmlui.administrative.ControlPanel.db_maxidle");
    private static final Message T_MAIL_SERVER 			= message("xmlui.administrative.ControlPanel.mail_server");
    private static final Message T_MAIL_FROM_ADDRESS 	= message("xmlui.administrative.ControlPanel.mail_from_address");
    private static final Message T_FEEDBACK_RECIPIENT 	= message("xmlui.administrative.ControlPanel.mail_feedback_recipient");
    private static final Message T_MAIL_ADMIN 			= message("xmlui.administrative.ControlPanel.mail_admin");
	private static final Message T_knots_head 			= message("xmlui.administrative.ControlPanel.knots_head");
	private static final Message T_knots_column1 		= message("xmlui.administrative.ControlPanel.knots_column1");
	private static final Message T_knots_column2 		= message("xmlui.administrative.ControlPanel.knots_column2");
	private static final Message T_knots_column3 		= message("xmlui.administrative.ControlPanel.knots_column3");
	private static final Message T_knots_hours 			= message("xmlui.administrative.ControlPanel.knots_hours");
	private static final Message T_knots_minutes 		= message("xmlui.administrative.ControlPanel.knots_minutes");
	private static final Message T_knots_expired 		= message("xmlui.administrative.ControlPanel.knots_expired");
	private static final Message T_knots_active 		= message("xmlui.administrative.ControlPanel.knots_active");
	private static final Message T_knots_none			= message("xmlui.administrative.ControlPanel.knots_none");
	private static final Message T_alerts_head				= message("xmlui.administrative.ControlPanel.alerts_head");
	private static final Message T_alerts_warning			= message("xmlui.administrative.ControlPanel.alerts_warning");
	private static final Message T_alerts_message_label		= message("xmlui.administrative.ControlPanel.alerts_message_label");
	private static final Message T_alerts_message_default	= message("xmlui.administrative.ControlPanel.alerts_message_default");
	private static final Message T_alerts_countdown_label	= message("xmlui.administrative.ControlPanel.alerts_countdown_label");
	private static final Message T_alerts_countdown_none	= message("xmlui.administrative.ControlPanel.alerts_countdown_none");
	private static final Message T_alerts_countdown_5		= message("xmlui.administrative.ControlPanel.alerts_countdown_5");
	private static final Message T_alerts_countdown_15		= message("xmlui.administrative.ControlPanel.alerts_countdown_15");
	private static final Message T_alerts_countdown_30		= message("xmlui.administrative.ControlPanel.alerts_countdown_30");
	private static final Message T_alerts_countdown_60		= message("xmlui.administrative.ControlPanel.alerts_countdown_60");
	private static final Message T_alerts_countdown_keep	= message("xmlui.administrative.ControlPanel.alerts_countdown_keep");
	private static final Message T_alerts_submit_activate	= message("xmlui.administrative.ControlPanel.alerts_submit_activate");
	private static final Message T_alerts_submit_deactivate	= message("xmlui.administrative.ControlPanel.alerts_submit_deactivate");
	private static final Message T_select_panel             = message("xmlui.administrative.ControlPanel.select_panel");
    /** 
     * The service manager allows us to access the continuation's 
     * manager, it is obtained from the servicable API
     */
    private ServiceManager serviceManager;
    
    /**
     * The four states that this page can be in.
     */
    private enum OPTIONS {java, dspace,knots,alerts};
    
    /**
     * From the servicable api, give us a service manager.
     */
    public void service(ServiceManager serviceManager) throws ServiceException 
    {
		this.serviceManager = serviceManager;
	}
	
	public void addPageMeta(PageMeta pageMeta) throws SAXException,
			WingException, UIException, SQLException, IOException,
			AuthorizeException {
		pageMeta.addMetadata("title").addContent(T_title);

		pageMeta.addTrailLink(contextPath + "/", T_DSPACE_HOME);
		pageMeta.addTrailLink(contextPath + "/admin/panel", T_trail);
	}

	public void addBody(Body body) throws SAXException, WingException,
			UIException, SQLException, IOException, AuthorizeException {
		
		if (!AuthorizeManager.isAdmin(context))
			throw new AuthorizeException("You are not authorized to view this page.");
		
		Request request = ObjectModelHelper.getRequest(objectModel);
		OPTIONS option = null;
		if (request.getParameter("java") != null)
			option = OPTIONS.java;
		if (request.getParameter("dspace") != null)
			option = OPTIONS.dspace;
		if (request.getParameter("knots") != null)
			option = OPTIONS.knots;
		if (request.getParameter("alerts") != null)
			option = OPTIONS.alerts;
		
		Division div = body.addInteractiveDivision("control-panel", contextPath+"/admin/panel", Division.METHOD_POST, "primary administrative");
		div.setHead(T_head);
		
		// LIST: options
		List options = div.addList("options",List.TYPE_SIMPLE,"horizontal");
		
		// our options, selected or not....
		if (option == OPTIONS.java)
			options.addItem().addHighlight("bold").addXref("?java",T_option_java);
		else
			options.addItemXref("?java",T_option_java);
		
		if (option == OPTIONS.dspace)
			options.addItem().addHighlight("bold").addXref("?dspace",T_option_dspace);
		else
			options.addItemXref("?dspace",T_option_dspace);
		
		if (option == OPTIONS.knots)
			options.addItem().addHighlight("bold").addXref("?knots",T_option_knots);
		else
			options.addItemXref("?knots",T_option_knots);
		
		if (option == OPTIONS.alerts)
			options.addItem().addHighlight("bold").addXref("?alerts",T_option_alerts);
		else
			options.addItemXref("?alerts",T_option_alerts);
		
		
		// The main content:
		if (option == OPTIONS.java)
			addJavaInformation(div);
		else if (option == OPTIONS.dspace)
			addDSpaceConfiguration(div);
		else if (option == OPTIONS.knots)
			addWebContinuations(div);
		else if (option == OPTIONS.alerts)
			addAlerts(div);
		else{
			div.addPara(T_select_panel);
		}
		
	}
	
	/**
	 * Add specific java information including JRE, OS, and runtime memory statistics.
	 */
	private void addJavaInformation(Division div) throws WingException
	{
		// Get memory statistics
		int processors = Runtime.getRuntime().availableProcessors();
		long maxMemory = Runtime.getRuntime().maxMemory();
		long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
		long usedMemory = totalMemory-freeMemory;
        
		// Convert bytes into MiB
		maxMemory   = maxMemory   / 1024 / 1024;
		totalMemory = totalMemory / 1024 / 1024;
		usedMemory  = usedMemory  / 1024 / 1024;
		freeMemory  = freeMemory  / 1024 / 1024;
		
		// LIST: Java
		List list = div.addList("javaOs");
		list.setHead(T_JAVA_HEAD);
		list.addLabel(T_JAVA_VERSION);
        list.addItem(System.getProperty("java.version"));
        list.addLabel(T_JAVA_VENDOR);
        list.addItem(System.getProperty("java.vendor"));
        list.addLabel(T_OS_NAME);
        list.addItem(System.getProperty("os.name"));
        list.addLabel(T_OS_ARCH);
        list.addItem(System.getProperty("os.arch"));
        list.addLabel(T_OS_VERSION);
        list.addItem(System.getProperty("os.version"));
		
		// LIST: memory
		List runtime = div.addList("runtime");
		runtime.setHead(T_RUNTIME_HEAD);
		runtime.addLabel(T_RUNTIME_PROCESSORS);
		runtime.addItem(String.valueOf(processors));
		runtime.addLabel(T_RUNTIME_MAX);
		runtime.addItem(String.valueOf(maxMemory) + " MiB");
		runtime.addLabel(T_RUNTIME_TOTAL);
		runtime.addItem(String.valueOf(totalMemory) + " MiB");
		runtime.addLabel(T_RUNTIME_USED);
		runtime.addItem(String.valueOf(usedMemory) + " MiB");
		runtime.addLabel(T_RUNTIME_FREE);
		runtime.addItem(String.valueOf(freeMemory) + " MiB");	
	}
	
	/**
	 * List important DSpace configuration parameters.
	 */
	private void addDSpaceConfiguration(Division div) throws WingException 
	{
		// LIST: DSpace
        List dspace = div.addList("dspace");
        dspace.setHead(T_DSPACE_HEAD);
        
        dspace.addLabel(T_DSPACE_DIR);
    	dspace.addItem(ConfigurationManager.getProperty("dspace.dir"));
    
        dspace.addLabel(T_DSPACE_URL);
    	dspace.addItem(ConfigurationManager.getProperty("dspace.url"));

        dspace.addLabel(T_DSPACE_HOST_NAME);
    	dspace.addItem(ConfigurationManager.getProperty("dspace.hostname"));

        dspace.addLabel(T_DSPACE_NAME);
    	dspace.addItem(ConfigurationManager.getProperty("dspace.name"));

        dspace.addLabel(T_DB_NAME);
    	dspace.addItem(ConfigurationManager.getProperty("db.name"));

    	dspace.addLabel(T_DB_URL);
    	dspace.addItem(ConfigurationManager.getProperty("db.url"));

        dspace.addLabel(T_DB_DRIVER);
    	dspace.addItem(ConfigurationManager.getProperty("db.driver"));

    	dspace.addLabel(T_DB_MAX_CONN);
    	dspace.addItem(ConfigurationManager.getProperty("db.maxconnections"));


        dspace.addLabel(T_DB_MAX_WAIT);
    	dspace.addItem(ConfigurationManager.getProperty("db.maxwait"));
    	
        dspace.addLabel(T_DB_MAX_IDLE);
    	dspace.addItem(ConfigurationManager.getProperty("db.maxidle"));

       	dspace.addLabel(T_MAIL_SERVER);
    	dspace.addItem(ConfigurationManager.getProperty("mail.server"));
 
        dspace.addLabel(T_MAIL_FROM_ADDRESS);
    	dspace.addItem(ConfigurationManager.getProperty("mail.from.address"));

        dspace.addLabel(T_FEEDBACK_RECIPIENT);
    	dspace.addItem(ConfigurationManager.getProperty("feedback.recipient"));

        dspace.addLabel(T_MAIL_ADMIN);
    	dspace.addItem(ConfigurationManager.getProperty("mail.admin"));        
	}
	
	/**
	 * Add a list of all active web continuations.
	 */
	private void addWebContinuations(Division div) throws WingException 
	{
		// Get all the web continuations and sort them.
		ArrayList<WebContinuation> knots = new ArrayList<WebContinuation>();
		try {
			// Get a list of all continuations
	        ContinuationsManager continuationManager = (ContinuationsManager) serviceManager.lookup(ContinuationsManager.ROLE);
	        @SuppressWarnings("unchecked") // the cast is correct
	        java.util.List<WebContinuationDataBean> knotBeans = continuationManager.getWebContinuationsDataBeanList();
	        for (WebContinuationDataBean flow : knotBeans)
	        {
	        	WebContinuation knot = continuationManager.lookupWebContinuation(flow.getId(), flow.getInterpreterId());
	     
	        	if (knot != null)
	        		knots.add(knot);
	        }
	        
	        // Sort them based upon access time
	        Collections.sort(knots, new WebContinuationByAccessTimeComparator<WebContinuation>());
	        
	        // Reverse it so that the shortest times are at the top.
	        Collections.reverse(knots);
        } 
        catch (ServiceException se)
        {
        	throw new UIException("Unable to query continuation states.",se);
        }
		
		
		
		
        // LIST: Flows
        Table activeFlows = div.addTable("knots",1,1);
        activeFlows.setHead(T_knots_head);
        
        Row row = activeFlows.addRow(Row.ROLE_HEADER);
        row.addCellContent(T_knots_column1);
        row.addCellContent(T_knots_column2);
        row.addCellContent(T_knots_column3);
        
        for (WebContinuation knot : knots)
        {
        	String interpreter = knot.getInterpreterId();
        	if (interpreter != null && interpreter.length() > 45)
        		interpreter = "..." + interpreter.substring(interpreter.length()-43);
        	
        	Message lastAccessMessage = null;
        	long lastAccess = System.currentTimeMillis() - knot.getLastAccessTime();
        	if (lastAccess > 2*60*60*1000)
        		lastAccessMessage = T_knots_hours.parameterize((lastAccess / (60*60*1000))); 
        	else
        		lastAccessMessage = T_knots_minutes.parameterize((lastAccess / (60*1000)));
        	
        	row = activeFlows.addRow();
        	row.addCellContent(interpreter);
        	row.addCellContent(lastAccessMessage);
        	row.addCellContent(knot.hasExpired() ? T_knots_expired : T_knots_active);	
        }
        
        if (knots.size() == 0)
        {
        	activeFlows.addRow().addCell(1, 3).addContent(T_knots_none);
        }
	}
	
	/**
	 * Comparator to sort webcontinuations by their access times.
	 */
	public static class WebContinuationByAccessTimeComparator<WC extends WebContinuation> implements Comparator<WC>
	{
		public int compare(WC a, WC b) {
			if (a.getLastAccessTime() > b.getLastAccessTime())
				return 1;  // A > B
			else if (a.getLastAccessTime() > b.getLastAccessTime())
				return -1; // B < A
			return 0; // A == B
		}
		
	}
	
	/**
	 * Add a section that allows administrators to activate or deactivate system-wide alerts.
	 */
	private void addAlerts(Division div) throws WingException 
	{
		// Remember we're in teh alerts section
		div.addHidden("alerts").setValue("true");
		
		List form = div.addList("system-wide-alerts",List.TYPE_FORM);
		form.setHead(T_alerts_head);
		
		form.addItem(T_alerts_warning);
		
		TextArea message = form.addItem().addTextArea("message");
		message.setLabel(T_alerts_message_label);
		message.setSize(5, 45);
		if (SystemwideAlerts.getMessage() == null)
			message.setValue(T_alerts_message_default);
		else
			message.setValue(SystemwideAlerts.getMessage());
		
		Select countdown = form.addItem().addSelect("countdown");
		countdown.setLabel(T_alerts_countdown_label);
		
		countdown.addOption(0,T_alerts_countdown_none);
		countdown.addOption(5,T_alerts_countdown_5);
		countdown.addOption(15,T_alerts_countdown_15);
		countdown.addOption(30,T_alerts_countdown_30);
		countdown.addOption(60,T_alerts_countdown_60);
		
		// Is there a current count down active?
		if (SystemwideAlerts.isAlertActive() && SystemwideAlerts.getCountDownToo() - System.currentTimeMillis() > 0)
			countdown.addOption(true,-1,T_alerts_countdown_keep);
		else
			countdown.setOptionSelected(0);
		
		Item actions = form.addItem();
		actions.addButton("submit_activate").setValue(T_alerts_submit_activate);
		actions.addButton("submit_deactivate").setValue(T_alerts_submit_deactivate);
		
	}
	
	
}
