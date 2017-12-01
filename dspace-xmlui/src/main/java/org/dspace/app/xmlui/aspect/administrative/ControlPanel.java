/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.cocoon.configuration.Settings;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.commons.lang.StringUtils;
import org.apache.excalibur.store.Store;
import org.apache.excalibur.store.StoreJanitor;
import org.dspace.app.util.Util;
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
import org.dspace.harvest.HarvestedCollection;
import org.dspace.harvest.OAIHarvester.HarvestScheduler;
import org.dspace.core.ConfigurationManager;
import org.dspace.eperson.EPerson;
import org.xml.sax.SAXException;

/**
 * This page displays important (and some not-so important) systems 
 * type information about your running dspace.
 *
 * @author Jay Paz
 * @author Scott Phillips
 */
public class ControlPanel extends AbstractDSpaceTransformer implements Serviceable, Disposable {

    /** Language Strings */
    private static final Message T_DSPACE_HOME                  = message("xmlui.general.dspace_home");
    private static final Message T_title 			= message("xmlui.administrative.ControlPanel.title");
    private static final Message T_trail	 		= message("xmlui.administrative.ControlPanel.trail");
    private static final Message T_head	 			= message("xmlui.administrative.ControlPanel.head");
    private static final Message T_option_java  		= message("xmlui.administrative.ControlPanel.option_java");
    private static final Message T_option_dspace  		= message("xmlui.administrative.ControlPanel.option_dspace");
    private static final Message T_option_alerts  		= message("xmlui.administrative.ControlPanel.option_alerts");
    private static final Message T_seconds 			= message("xmlui.administrative.ControlPanel.seconds");
    private static final Message T_hours 			= message("xmlui.administrative.ControlPanel.hours");
    private static final Message T_minutes 			= message("xmlui.administrative.ControlPanel.minutes");
    private static final Message T_JAVA_HEAD 			= message("xmlui.administrative.ControlPanel.java_head");
    private static final Message T_JAVA_VERSION 		= message("xmlui.administrative.ControlPanel.java_version");
    private static final Message T_JAVA_VENDOR 			= message("xmlui.administrative.ControlPanel.java_vendor");
    private static final Message T_OS_NAME 			= message("xmlui.administrative.ControlPanel.os_name");
    private static final Message T_OS_ARCH 			= message("xmlui.administrative.ControlPanel.os_arch");
    private static final Message T_OS_VERSION 			= message("xmlui.administrative.ControlPanel.os_version"); 
    private static final Message T_RUNTIME_HEAD 		= message("xmlui.administrative.ControlPanel.runtime_head");   
    private static final Message T_RUNTIME_PROCESSORS           = message("xmlui.administrative.ControlPanel.runtime_processors"); 
    private static final Message T_RUNTIME_MAX 			= message("xmlui.administrative.ControlPanel.runtime_max"); 
    private static final Message T_RUNTIME_TOTAL 		= message("xmlui.administrative.ControlPanel.runtime_total"); 
    private static final Message T_RUNTIME_USED 		= message("xmlui.administrative.ControlPanel.runtime_used"); 
    private static final Message T_RUNTIME_FREE 		= message("xmlui.administrative.ControlPanel.runtime_free");
    private static final Message T_COCOON_HEAD                  = message("xmlui.administrative.ControlPanel.cocoon_head");  
    private static final Message T_COCOON_VERSION               = message("xmlui.administrative.ControlPanel.cocoon_version");
    private static final Message T_COCOON_CACHE_DIR             = message("xmlui.administrative.ControlPanel.cocoon_cache_dir");
    private static final Message T_COCOON_WORK_DIR              = message("xmlui.administrative.ControlPanel.cocoon_work_dir");
    private static final Message T_COCOON_MAIN_CACHE_SIZE       = message("xmlui.administrative.ControlPanel.cocoon_main_cache_size");
    private static final Message T_COCOON_PERSISTENT_CACHE_SIZE = message("xmlui.administrative.ControlPanel.cocoon_persistent_cache_size");
    private static final Message T_COCOON_TRANS_CACHE_SIZE      = message("xmlui.administrative.ControlPanel.cocoon_transient_cache_size");
    private static final Message T_COCOON_CACHE_CLEAR           = message("xmlui.administrative.ControlPanel.cocoon_cache_clear");
    private static final Message T_DSPACE_HEAD 			= message("xmlui.administrative.ControlPanel.dspace_head");
    private static final Message T_DSPACE_DIR 			= message("xmlui.administrative.ControlPanel.dspace_dir");
    private static final Message T_DSPACE_URL 			= message("xmlui.administrative.ControlPanel.dspace_url");
    private static final Message T_DSPACE_HOST_NAME             = message("xmlui.administrative.ControlPanel.dspace_hostname");
    private static final Message T_DSPACE_NAME 			= message("xmlui.administrative.ControlPanel.dspace_name");
    private static final Message T_DSPACE_VERSION               = message("xmlui.administrative.ControlPanel.dspace_version");
    private static final Message T_DB_NAME 			= message("xmlui.administrative.ControlPanel.db_name");
    private static final Message T_DB_URL 			= message("xmlui.administrative.ControlPanel.db_url");
    private static final Message T_DB_DRIVER 			= message("xmlui.administrative.ControlPanel.db_driver");
    private static final Message T_DB_MAX_CONN 			= message("xmlui.administrative.ControlPanel.db_maxconnections");
    private static final Message T_DB_MAX_WAIT 			= message("xmlui.administrative.ControlPanel.db_maxwait");
    private static final Message T_DB_MAX_IDLE 			= message("xmlui.administrative.ControlPanel.db_maxidle");
    private static final Message T_MAIL_SERVER 			= message("xmlui.administrative.ControlPanel.mail_server");
    private static final Message T_MAIL_FROM_ADDRESS            = message("xmlui.administrative.ControlPanel.mail_from_address");
    private static final Message T_FEEDBACK_RECIPIENT           = message("xmlui.administrative.ControlPanel.mail_feedback_recipient");
    private static final Message T_MAIL_ADMIN 			= message("xmlui.administrative.ControlPanel.mail_admin");
    private static final Message T_alerts_head                  = message("xmlui.administrative.ControlPanel.alerts_head");
    private static final Message T_alerts_warning               = message("xmlui.administrative.ControlPanel.alerts_warning");
    private static final Message T_alerts_message_label		= message("xmlui.administrative.ControlPanel.alerts_message_label");
    private static final Message T_alerts_message_default	= message("xmlui.administrative.ControlPanel.alerts_message_default");
    private static final Message T_alerts_countdown_label	= message("xmlui.administrative.ControlPanel.alerts_countdown_label");
    private static final Message T_alerts_countdown_none	= message("xmlui.administrative.ControlPanel.alerts_countdown_none");
    private static final Message T_alerts_countdown_5		= message("xmlui.administrative.ControlPanel.alerts_countdown_5");
    private static final Message T_alerts_countdown_15		= message("xmlui.administrative.ControlPanel.alerts_countdown_15");
    private static final Message T_alerts_countdown_30		= message("xmlui.administrative.ControlPanel.alerts_countdown_30");
    private static final Message T_alerts_countdown_60		= message("xmlui.administrative.ControlPanel.alerts_countdown_60");
    private static final Message T_alerts_countdown_keep	= message("xmlui.administrative.ControlPanel.alerts_countdown_keep");
    private static final Message T_alerts_session_label 	      = message("xmlui.administrative.ControlPanel.alerts_session_label");
    private static final Message T_alerts_session_all_sessions        = message("xmlui.administrative.ControlPanel.alerts_session_all_sessions");
    private static final Message T_alerts_session_current_sessions    = message("xmlui.administrative.ControlPanel.alerts_session_current_sessions");
    private static final Message T_alerts_session_only_administrative = message("xmlui.administrative.ControlPanel.alerts_session_only_administrative_sessions");
    private static final Message T_alerts_session_note                = message("xmlui.administrative.ControlPanel.alerts_session_note");	
    private static final Message T_alerts_submit_activate	= message("xmlui.administrative.ControlPanel.alerts_submit_activate");
    private static final Message T_alerts_submit_deactivate	= message("xmlui.administrative.ControlPanel.alerts_submit_deactivate");
    private static final Message T_activity_head 		= message("xmlui.administrative.ControlPanel.activity_head");
    private static final Message T_stop_anonymous               = message("xmlui.administrative.ControlPanel.stop_anonymous");
    private static final Message T_start_anonymous              = message("xmlui.administrative.ControlPanel.start_anonymous");
    private static final Message T_stop_bot                     = message("xmlui.administrative.ControlPanel.stop_bot");
    private static final Message T_start_bot                    = message("xmlui.administrative.ControlPanel.start_bot");
    private static final Message T_activity_sort_time 		= message("xmlui.administrative.ControlPanel.activity_sort_time");
    private static final Message T_activity_sort_user 		= message("xmlui.administrative.ControlPanel.activity_sort_user");
    private static final Message T_activity_sort_ip  		= message("xmlui.administrative.ControlPanel.activity_sort_ip");
    private static final Message T_activity_sort_url 		= message("xmlui.administrative.ControlPanel.activity_sort_url");
    private static final Message T_activity_sort_agent 		= message("xmlui.administrative.ControlPanel.activity_sort_Agent");
    private static final Message T_activity_anonymous 		= message("xmlui.administrative.ControlPanel.activity_anonymous");	
    private static final Message T_activity_none		= message("xmlui.administrative.ControlPanel.activity_none");
    private static final Message T_select_panel                 = message("xmlui.administrative.ControlPanel.select_panel");

    private static final Message T_option_harvest  			= message("xmlui.administrative.ControlPanel.option_harvest");
    private static final Message T_harvest_scheduler_head 		= message("xmlui.administrative.ControlPanel.harvest_scheduler_head");
    private static final Message T_harvest_label_status 		= message("xmlui.administrative.ControlPanel.harvest_label_status");
    private static final Message T_harvest_label_actions 		= message("xmlui.administrative.ControlPanel.harvest_label_actions");
    private static final Message T_harvest_submit_start 		= message("xmlui.administrative.ControlPanel.harvest_submit_start");
    private static final Message T_harvest_submit_reset 		= message("xmlui.administrative.ControlPanel.harvest_submit_reset");
    private static final Message T_harvest_submit_resume 		= message("xmlui.administrative.ControlPanel.harvest_submit_resume");
    private static final Message T_harvest_submit_pause 		= message("xmlui.administrative.ControlPanel.harvest_submit_pause");
    private static final Message T_harvest_submit_stop 			= message("xmlui.administrative.ControlPanel.harvest_submit_stop");
    private static final Message T_harvest_label_collections 		= message("xmlui.administrative.ControlPanel.harvest_label_collections");
    private static final Message T_harvest_label_active 		= message("xmlui.administrative.ControlPanel.harvest_label_active");
    private static final Message T_harvest_label_queued 		= message("xmlui.administrative.ControlPanel.harvest_label_queued");
    private static final Message T_harvest_label_oai_errors 		= message("xmlui.administrative.ControlPanel.harvest_label_oai_errors");
    private static final Message T_harvest_label_internal_errors 	= message("xmlui.administrative.ControlPanel.harvest_label_internal_errors");
    private static final Message T_harvest_head_generator_settings 	= message("xmlui.administrative.ControlPanel.harvest_head_generator_settings");
    private static final Message T_harvest_label_oai_url 		= message("xmlui.administrative.ControlPanel.harvest_label_oai_url");
    private static final Message T_harvest_label_oai_source 		= message("xmlui.administrative.ControlPanel.harvest_label_oai_source");
    private static final Message T_harvest_head_harvester_settings 	= message("xmlui.administrative.ControlPanel.harvest_head_harvester_settings");


    /** 
     * The service manager allows us to access the continuation's 
     * manager.  It is obtained from the Serviceable API
     */
    private ServiceManager serviceManager;

    /**
     * The Cocoon Settings (used to display Cocoon info)
     */
    private Settings settings;

    /**
     * The Cocoon StoreJanitor (used for cache statistics)
     */
    private StoreJanitor storeJanitor;

     /**
     * The Cocoon Default Store (used for cache statistics)
     */
    private Store storeDefault;

    /**
     * The Cocoon Persistent Store (used for cache statistics)
     */
    private Store storePersistent;

    /**
     * The five states that this page can be in.
     */
    private enum OPTIONS {java, dspace, alerts, activity, harvest};

    /**
     * From the <code>org.apache.avalon.framework.service.Serviceable</code> API, 
     * give us the current <code>ServiceManager</code> instance.
     * <P>
     * Much of this ServiceManager logic/code has been borrowed from the source
     * code of the Cocoon <code>StatusGenerator</code> class:
     * http://svn.apache.org/repos/asf/cocoon/tags/cocoon-2.2/cocoon-sitemap-components/cocoon-sitemap-components-1.0.0/src/main/java/org/apache/cocoon/generation/StatusGenerator.java
     */
    @Override
    public void service(ServiceManager serviceManager) throws ServiceException 
    {
        this.serviceManager = serviceManager;
        
        this.settings = (Settings) this.serviceManager.lookup(Settings.ROLE);
        
        if(this.serviceManager.hasService(StoreJanitor.ROLE))
            this.storeJanitor = (StoreJanitor) this.serviceManager.lookup(StoreJanitor.ROLE);
        
        if (this.serviceManager.hasService(Store.ROLE))
            this.storeDefault = (Store) this.serviceManager.lookup(Store.ROLE);
        
        if(this.serviceManager.hasService(Store.PERSISTENT_STORE))
            this.storePersistent = (Store) this.serviceManager.lookup(Store.PERSISTENT_STORE);
    }

    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
                    WingException, UIException, SQLException, IOException,
                    AuthorizeException 
    {
        pageMeta.addMetadata("title").addContent(T_title);

        pageMeta.addTrailLink(contextPath + "/", T_DSPACE_HOME);
        pageMeta.addTrailLink(null, T_trail);
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException,
                    UIException, SQLException, IOException, AuthorizeException 
    {

        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException("You are not authorized to view this page.");
        }

        Request request = ObjectModelHelper.getRequest(objectModel);
        OPTIONS option = null;
        if (request.getParameter("java") != null)
        {
            option = OPTIONS.java;
        }
        if (request.getParameter("dspace") != null)
        {
            option = OPTIONS.dspace;
        }
        if (request.getParameter("alerts") != null)
        {
            option = OPTIONS.alerts;
        }
        if (request.getParameter("activity") != null)
        {
            option = OPTIONS.activity;
        }
        if (request.getParameter("harvest") != null)
        {
            option = OPTIONS.harvest;
        }

        Division div = body.addInteractiveDivision("control-panel", contextPath+"/admin/panel", Division.METHOD_POST, "primary administrative");
        div.setHead(T_head);

        // LIST: options
        List options = div.addList("options",List.TYPE_SIMPLE,"horizontal");

        // our options, selected or not....
        if (option == OPTIONS.java)
        {
            options.addItem().addHighlight("bold").addXref("?java", T_option_java);
        }
        else
        {
            options.addItemXref("?java", T_option_java);
        }

        if (option == OPTIONS.dspace)
        {
            options.addItem().addHighlight("bold").addXref("?dspace", T_option_dspace);
        }
        else
        {
            options.addItemXref("?dspace", T_option_dspace);
        }

        if (option == OPTIONS.alerts)
        {
            options.addItem().addHighlight("bold").addXref("?alerts", T_option_alerts);
        }
        else
        {
            options.addItemXref("?alerts", T_option_alerts);
        }

        if (option == OPTIONS.harvest)
        {
            options.addItem().addHighlight("bold").addXref("?harvest", T_option_harvest);
        }
        else
        {
            options.addItemXref("?harvest", T_option_harvest);
        }

        String userSortTarget = "?activity";
        if (request.getParameter("sortBy") != null)
        {
            userSortTarget += "&sortBy=" + request.getParameter("sortBy");
        }
        if (option == OPTIONS.activity)
        {
            options.addItem().addHighlight("bold").addXref(userSortTarget, "Current Activity");
        }
        else
        {
            options.addItemXref(userSortTarget, "Current Activity");
        }


        // The main content:
        if (option == OPTIONS.java)
        {
            addJavaInformation(div);
        }
        else if (option == OPTIONS.dspace)
        {
            addDSpaceConfiguration(div);
        }
        else if (option == OPTIONS.alerts)
        {
            addAlerts(div);
        }
        else if (option == OPTIONS.activity)
        {
            addActivity(div);
        }
        else if (option == OPTIONS.harvest)
        {
            addHarvest(div);
        }
        else
        {
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
        list.addItem(System.getProperty("java.vm.name"));
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
        
        //List: Cocoon Info & Cache
        addCocoonInformation(div);
    }

    /**
     * Add specific Cocoon information, especially related to the Cocoon Cache.
     * <P>
     * For more information about Cocoon Caches/Stores, see:
     * http://wiki.apache.org/cocoon/StoreComponents  
     */
    private void addCocoonInformation(Division div) throws WingException
    {
        // List: Cocoon Info & Caches
        List cocoon = div.addList("cocoon");
        cocoon.setHead(T_COCOON_HEAD);
        
        cocoon.addLabel(T_COCOON_VERSION);
        cocoon.addItem(org.apache.cocoon.Constants.VERSION);

        // attempt to Display some basic info about Cocoon's Settings & Caches

        // Get access to basic Cocoon Settings
        if(this.settings!=null)
        {
            //Output Cocoon's Work Directory & Cache Directory
            cocoon.addLabel(T_COCOON_WORK_DIR);
            cocoon.addItem(this.settings.getWorkDirectory());
            cocoon.addLabel(T_COCOON_CACHE_DIR);
            cocoon.addItem(this.settings.getCacheDirectory());
        }    

        // Check if we have access to Cocoon's Default Cache
        // Cocoon's Main (Default) Store is used to store objects that are serializable
        if(this.storeDefault!=null)
        {
            // Store name is just the className (remove the package info though, just to save space)
            String storeName = this.storeDefault.getClass().getName();
            storeName = storeName.substring(storeName.lastIndexOf(".")+1); 

            // display main store's cache info
            cocoon.addLabel(T_COCOON_MAIN_CACHE_SIZE.parameterize(storeName + ", 0x" + Integer.toHexString(this.storeDefault.hashCode())));

            // display cache size & link to clear Cocoon's main cache
            Item defaultSize = cocoon.addItem();
            defaultSize.addContent(String.valueOf(this.storeDefault.size()) + "  ");
            defaultSize.addXref(contextPath + "/admin/panel?java=true&clearcache=true", T_COCOON_CACHE_CLEAR);
        }

        // Check if we have access to Cocoon's Persistent Cache
        // Cocoon's Persistent Store may be used by the Default Cache/Store to delegate persistent storage
        // (it's an optional store which may not exist)
        if(this.storePersistent!=null)
        {
            // Store name is just the className (remove the package info though, just to save space)
            String storeName = this.storeDefault.getClass().getName();
            storeName = storeName.substring(storeName.lastIndexOf(".")+1);

            // display persistent store's cache size info
            cocoon.addLabel(T_COCOON_PERSISTENT_CACHE_SIZE.parameterize(storeName + ", 0x" + Integer.toHexString(this.storePersistent.hashCode())));
            cocoon.addItem(String.valueOf(this.storePersistent.size()));
        }

        // Check if we have access to Cocoon's StoreJanitor
        // The Store Janitor manages all of Cocoon's "transient caches/stores"
        // These "transient" stores are used for non-serializable objects or objects whose
        // storage doesn't make sense across a server restart. 
        if(this.storeJanitor!=null)
        {
            // For each Cache Store in Cocoon's StoreJanitor
            Iterator i = this.storeJanitor.iterator();
            while(i.hasNext())
            {
                // get the Cache Store
                Store store = (Store) i.next();

                // Store name is just the className (remove the package info though, just to save space)
                String storeName = store.getClass().getName();
                storeName = storeName.substring(storeName.lastIndexOf(".")+1); 

                // display its size information
                cocoon.addLabel(T_COCOON_TRANS_CACHE_SIZE.parameterize(storeName + ", 0x" + Integer.toHexString(store.hashCode())));
                cocoon.addItem(String.valueOf(store.size()));
            }
        }
    }
	
    /**
     * List important DSpace configuration parameters.
     */
    private void addDSpaceConfiguration(Division div) throws WingException 
    {
        // LIST: DSpace
        List dspace = div.addList("dspace");
        dspace.setHead(T_DSPACE_HEAD);
        
        dspace.addLabel(T_DSPACE_VERSION);
        dspace.addItem(Util.getSourceVersion());
        
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
     * Add a section that allows administrators to activate or deactivate system-wide alerts.
     */
    private void addAlerts(Division div) throws WingException 
    {
        // Remember we're in the alerts section
        div.addHidden("alerts").setValue("true");

        List form = div.addList("system-wide-alerts",List.TYPE_FORM);
        form.setHead(T_alerts_head);

        form.addItem(T_alerts_warning);

        TextArea message = form.addItem().addTextArea("message");
        message.setAutofocus("autofocus");
        message.setLabel(T_alerts_message_label);
        message.setSize(5, 45);
        if (SystemwideAlerts.getMessage() == null)
        {
            message.setValue(T_alerts_message_default);
        }
        else
        {
            message.setValue(SystemwideAlerts.getMessage());
        }
		
        Select countdown = form.addItem().addSelect("countdown");
        countdown.setLabel(T_alerts_countdown_label);

        countdown.addOption(0,T_alerts_countdown_none);
        countdown.addOption(5,T_alerts_countdown_5);
        countdown.addOption(15,T_alerts_countdown_15);
        countdown.addOption(30,T_alerts_countdown_30);
        countdown.addOption(60,T_alerts_countdown_60);

        // Is there a current countdown active?
        if (SystemwideAlerts.isAlertActive() && SystemwideAlerts.getCountDownToo() - System.currentTimeMillis() > 0)
        {
            countdown.addOption(true, -1, T_alerts_countdown_keep);
        }
        else
        {
            countdown.setOptionSelected(0);
        }
		
        Select restrictsessions = form.addItem().addSelect("restrictsessions");
        restrictsessions.setLabel(T_alerts_session_label);
        restrictsessions.addOption(SystemwideAlerts.STATE_ALL_SESSIONS,T_alerts_session_all_sessions);
        restrictsessions.addOption(SystemwideAlerts.STATE_CURRENT_SESSIONS,T_alerts_session_current_sessions);
        restrictsessions.addOption(SystemwideAlerts.STATE_ONLY_ADMINISTRATIVE_SESSIONS,T_alerts_session_only_administrative);
        restrictsessions.setOptionSelected(SystemwideAlerts.getRestrictSessions());

        form.addItem(T_alerts_session_note);


        Item actions = form.addItem();
        actions.addButton("submit_activate").setValue(T_alerts_submit_activate);
        actions.addButton("submit_deactivate").setValue(T_alerts_submit_deactivate);
		
    }
	
    /** The possible sorting parameters */
    private static enum EventSort { TIME, URL, SESSION, AGENT, IP };
	
    /**
     * Create a list of all activity.
     */
    private void addActivity(Division div) throws WingException, SQLException 
    {
		
        // 0) Update recording settings
        Request request = ObjectModelHelper.getRequest(objectModel);

        // Toggle anonymous recording
        String recordAnonymousString = request.getParameter("recordanonymous");
        if (recordAnonymousString != null)
        {
            if ("ON".equals(recordAnonymousString))
            {
                CurrentActivityAction.setRecordAnonymousEvents(true);
            }
            if ("OFF".equals(recordAnonymousString))
            {
                CurrentActivityAction.setRecordAnonymousEvents(false);
            }
        }
		
        // Toggle bot recording
        String recordBotString = request.getParameter("recordbots");
        if (recordBotString != null)
        {
            if ("ON".equals(recordBotString))
            {
                CurrentActivityAction.setRecordBotEvents(true);
            }
            if ("OFF".equals(recordBotString))
            {
                CurrentActivityAction.setRecordBotEvents(false);
            }
        }		
		
        // 1) Determine how to sort
        EventSort sortBy = EventSort.TIME;
        String sortByString = request.getParameter("sortBy");
        if (EventSort.TIME.toString().equals(sortByString))
        {
            sortBy = EventSort.TIME;
        }
        if (EventSort.URL.toString().equals(sortByString))
        {
            sortBy = EventSort.URL;
        }
        if (EventSort.SESSION.toString().equals(sortByString))
        {
            sortBy = EventSort.SESSION;
        }
        if (EventSort.AGENT.toString().equals(sortByString))
        {
            sortBy = EventSort.AGENT;
        }
        if (EventSort.IP.toString().equals(sortByString))
        {
            sortBy = EventSort.IP;
        }
		
        // 2) Sort the events by the requested sorting parameter
        java.util.List<CurrentActivityAction.Event> events = CurrentActivityAction.getEvents();
        Collections.sort(events, new ActivitySort<CurrentActivityAction.Event>(sortBy));
        Collections.reverse(events);
        
        // 3) Toggle controls for anonymous and bot activity
        if (CurrentActivityAction.getRecordAnonymousEvents())
        {
            div.addPara().addXref("?activity&sortBy=" + sortBy + "&recordanonymous=OFF").addContent(T_stop_anonymous);
        }
        else
        {
            div.addPara().addXref("?activity&sortBy=" + sortBy + "&recordanonymous=ON").addContent(T_start_anonymous);
        }
        
        if (CurrentActivityAction.getRecordBotEvents())
        {
            div.addPara().addXref("?activity&sortBy=" + sortBy + "&recordbots=OFF").addContent(T_stop_bot);
        }
        else
        {
            div.addPara().addXref("?activity&sortBy=" + sortBy + "&recordbots=ON").addContent(T_start_bot);
        }
       	
        
        // 4) Display the results Table
        // TABLE: activeUsers
        Table activeUsers = div.addTable("users",1,1);
        activeUsers.setHead(T_activity_head.parameterize(CurrentActivityAction.MAX_EVENTS));
        Row row = activeUsers.addRow(Row.ROLE_HEADER);
        if (sortBy == EventSort.TIME)
        {
            row.addCell().addHighlight("bold").addXref("?activity&sortBy=" + EventSort.TIME).addContent(T_activity_sort_time);
        }
        else
        {
            row.addCell().addXref("?activity&sortBy=" + EventSort.TIME).addContent(T_activity_sort_time);
        }
        
        if (sortBy == EventSort.SESSION)
        {
            row.addCell().addHighlight("bold").addXref("?activity&sortBy=" + EventSort.SESSION).addContent(T_activity_sort_user);
        }
        else
        {
            row.addCell().addXref("?activity&sortBy=" + EventSort.SESSION).addContent(T_activity_sort_user);
        }
        
        if (sortBy == EventSort.IP)
        {
            row.addCell().addHighlight("bold").addXref("?activity&sortBy=" + EventSort.IP).addContent(T_activity_sort_ip);
        }
        else
        {
            row.addCell().addXref("?activity&sortBy=" + EventSort.IP).addContent(T_activity_sort_ip);
        }
        
        if (sortBy == EventSort.URL)
        {
            row.addCell().addHighlight("bold").addXref("?activity&sortBy=" + EventSort.URL).addContent(T_activity_sort_url);
        }
        else
        {
            row.addCell().addXref("?activity&sortBy=" + EventSort.URL).addContent(T_activity_sort_url);
        }
        
        if (sortBy == EventSort.AGENT)
        {
            row.addCell().addHighlight("bold").addXref("?activity&sortBy=" + EventSort.AGENT).addContent(T_activity_sort_agent);
        }
        else
        {
            row.addCell().addXref("?activity&sortBy=" + EventSort.AGENT).addContent(T_activity_sort_agent);
        }
   
        // Keep track of how many individual anonymous users there are, each unique anonymous
        // user is assigned an index based upon the servlet session id.
        HashMap<String,Integer> anonymousHash = new HashMap<String,Integer>();
        int anonymousCount = 1;
		
        int shown = 0;
        for (CurrentActivityAction.Event event : events)
        {	
            if (event == null)
            {
                continue;
            }
			
            shown++;
			
            Message timeStampMessage = null;
            long ago = System.currentTimeMillis() - event.getTimeStamp();

            if (ago > 2*60*60*1000)
            {
                timeStampMessage = T_hours.parameterize((ago / (60 * 60 * 1000)));
            }
            else if (ago > 60*1000)
            {
                timeStampMessage = T_minutes.parameterize((ago / (60 * 1000)));
            }
            else
            {
                timeStampMessage = T_seconds.parameterize((ago / (1000)));
            }
        	
        	
            Row eventRow = activeUsers.addRow();

            eventRow.addCellContent(timeStampMessage);
            int eid = event.getEPersonID();
            EPerson eperson = EPerson.find(context, eid);
            if (eperson != null)
            {
                String name = eperson.getFullName();
                eventRow.addCellContent(name);
            }
            else
            {
                // Is this a new anonymous user?
                if (!anonymousHash.containsKey(event.getSessionID()))
                {
                    anonymousHash.put(event.getSessionID(), anonymousCount++);
                }
				
                eventRow.addCellContent(T_activity_anonymous.parameterize(anonymousHash.get(event.getSessionID())));
            }
            eventRow.addCellContent(event.getIP());
            eventRow.addCell().addXref(contextPath+"/"+event.getURL()).addContent("/"+event.getURL());
            eventRow.addCellContent(event.getDectectedBrowser());
        }
		
        if (shown == 0)
        {
            activeUsers.addRow().addCell(1, 5).addContent(T_activity_none);
        }
    }
	
    /**
     * Comparator to sort activity events by their access times.
     */
    public static class ActivitySort<E extends CurrentActivityAction.Event> implements Comparator<E>, Serializable
    {
        // Sort parameter
        private EventSort sortBy;

        public ActivitySort(EventSort sortBy)
        {
                this.sortBy = sortBy;
        }
		
        /**
         * Compare these two activity events based upon the given sort parameter. In the case of a tie,
         * always fallback to sorting based upon the timestamp.
         */
        @Override
        public int compare(E a, E b) 
        {
            // Protect against null events while sorting
            if (a != null && b == null)
            {
                return 1; // A > B
            }
            else if (a == null && b != null)
            {
                return -1; // B > A
            }
            else if (a == null && b == null)
            {
                return 0; // A == B
            }
			
            // Sort by the given ordering matrix
            if (EventSort.URL == sortBy)
            {
                String aURL = a.getURL();
                String bURL = b.getURL();
                int cmp = aURL.compareTo(bURL);
                if (cmp != 0)
                {
                    return cmp;
                }
            }
            else if (EventSort.AGENT == sortBy)
            {
                String aAgent = a.getDectectedBrowser();
                String bAgent = b.getDectectedBrowser();
                int cmp = aAgent.compareTo(bAgent);
                if (cmp != 0)
                {
                    return cmp;
                }
            }
            else if (EventSort.IP == sortBy)
            {
                String aIP = a.getIP();
                String bIP = b.getIP();
                int cmp = aIP.compareTo(bIP);
                if (cmp != 0)
                {
                    return cmp;
                }
				
            }
            else if (EventSort.SESSION == sortBy)
            {
                // Ensure that all sessions with an EPersonID associated are
                // ordered to the top. Otherwise fall back to comparing session
                // IDs. Unfortunately, we cannot compare eperson names because 
                // we do not have access to a context object.
                if (a.getEPersonID() > 0  && b.getEPersonID() < 0)
                {
                    return 1; // A > B
                }
                else if (a.getEPersonID() < 0  && b.getEPersonID() > 0)
                {
                    return -1; // B > A
                }
				
                String aSession = a.getSessionID();
                String bSession = b.getSessionID();
                int cmp = aSession.compareTo(bSession);
                if (cmp != 0)
                {
                    return cmp;
                }
            }
			
            // Always fall back to sorting by time, when events are equal.
            if (a.getTimeStamp() > b.getTimeStamp())
            {
                return 1;  // A > B
            }
            else if (a.getTimeStamp() > b.getTimeStamp())
            {
                return -1; // B > A
            }
            return 0; // A == B
        }
    }
	
		
    /**
     * Add a section that allows management of the OAI harvester.
     * @throws SQLException 
     */
    private void addHarvest(Division div) throws WingException, SQLException 
    {
        // Remember we're in the harvest section
        div.addHidden("harvest").setValue("true");

        List harvesterControls = div.addList("oai-harvester-controls",List.TYPE_FORM);
        harvesterControls.setHead(T_harvest_scheduler_head);
        harvesterControls.addLabel(T_harvest_label_status);
        Item status = harvesterControls.addItem();
        status.addContent(HarvestScheduler.getStatus());
        status.addXref(contextPath + "/admin/panel?harvest", "(refresh)");

        harvesterControls.addLabel(T_harvest_label_actions);
        Item actionsItem = harvesterControls.addItem();
        if (HarvestScheduler.hasStatus(HarvestScheduler.HARVESTER_STATUS_STOPPED)) {
            actionsItem.addButton("submit_harvest_start").setValue(T_harvest_submit_start);
            actionsItem.addButton("submit_harvest_reset").setValue(T_harvest_submit_reset);
        }
        if (HarvestScheduler.hasStatus(HarvestScheduler.HARVESTER_STATUS_PAUSED))
        {
            actionsItem.addButton("submit_harvest_resume").setValue(T_harvest_submit_resume);
        }
        if (HarvestScheduler.hasStatus(HarvestScheduler.HARVESTER_STATUS_RUNNING) ||
                    HarvestScheduler.hasStatus(HarvestScheduler.HARVESTER_STATUS_SLEEPING))
        {
            actionsItem.addButton("submit_harvest_pause").setValue(T_harvest_submit_pause);
        }
        if (!HarvestScheduler.hasStatus(HarvestScheduler.HARVESTER_STATUS_STOPPED))
        {
            actionsItem.addButton("submit_harvest_stop").setValue(T_harvest_submit_stop);
        }
		
        // Can be retrieved via "{context-path}/admin/collection?collectionID={id}"
        String baseURL = contextPath + "/admin/collection?collectionID=";

        harvesterControls.addLabel(T_harvest_label_collections);
        Item allCollectionsItem = harvesterControls.addItem();
        java.util.List<Integer> allCollections =  HarvestedCollection.findAll(context);
        for (Integer oaiCollection : allCollections) {
                allCollectionsItem.addXref(baseURL + oaiCollection, oaiCollection.toString());
        }
        harvesterControls.addLabel(T_harvest_label_active);
        Item busyCollectionsItem = harvesterControls.addItem();
        java.util.List<Integer> busyCollections =  HarvestedCollection.findByStatus(context, HarvestedCollection.STATUS_BUSY);
        for (Integer busyCollection : busyCollections) {
                busyCollectionsItem.addXref(baseURL + busyCollection, busyCollection.toString());
        }
        harvesterControls.addLabel(T_harvest_label_queued);
        Item queuedCollectionsItem = harvesterControls.addItem();
        java.util.List<Integer> queuedCollections =  HarvestedCollection.findByStatus(context, HarvestedCollection.STATUS_QUEUED);
        for (Integer queuedCollection : queuedCollections) {
                queuedCollectionsItem.addXref(baseURL + queuedCollection, queuedCollection.toString());
        }
        harvesterControls.addLabel(T_harvest_label_oai_errors);
        Item oaiErrorsItem = harvesterControls.addItem();
        java.util.List<Integer> oaiErrors =  HarvestedCollection.findByStatus(context, HarvestedCollection.STATUS_OAI_ERROR);
        for (Integer oaiError : oaiErrors) {
                oaiErrorsItem.addXref(baseURL + oaiError, oaiError.toString());
        }
        harvesterControls.addLabel(T_harvest_label_internal_errors);
        Item internalErrorsItem = harvesterControls.addItem();
        java.util.List<Integer> internalErrors =  HarvestedCollection.findByStatus(context, HarvestedCollection.STATUS_UNKNOWN_ERROR);
        for (Integer internalError : internalErrors) {
                internalErrorsItem.addXref(baseURL + internalError, internalError.toString());
        }
		
        // OAI Generator settings
        List generatorSettings = div.addList("oai-generator-settings");
        generatorSettings.setHead(T_harvest_head_generator_settings);

        generatorSettings.addLabel(T_harvest_label_oai_url);
        String oaiUrl = ConfigurationManager.getProperty("oai", "dspace.oai.url");
        if (!StringUtils.isEmpty(oaiUrl))
        {
            generatorSettings.addItem(oaiUrl);
        }

        generatorSettings.addLabel(T_harvest_label_oai_source);
        String oaiAuthoritativeSource = ConfigurationManager.getProperty("oai", "ore.authoritative.source");
        if (!StringUtils.isEmpty(oaiAuthoritativeSource))
        {
            generatorSettings.addItem(oaiAuthoritativeSource);
        }
        else
        {
            generatorSettings.addItem("oai");
        }
		
        // OAI Harvester settings (just iterate over all the values that start with "harvester")
        List harvesterSettings = div.addList("oai-harvester-settings");
        harvesterSettings.setHead(T_harvest_head_harvester_settings);

        String metaString = "harvester.";
        Enumeration pe = ConfigurationManager.propertyNames();
        while (pe.hasMoreElements())
        {
            String key = (String)pe.nextElement();
            if (key.startsWith(metaString)) {
            	harvesterSettings.addLabel(key);
            	harvesterSettings.addItem(ConfigurationManager.getProperty(key) + " ");
            }
        }
    }
	
    
    /**
     * Release all Cocoon resources.
     * @see org.apache.avalon.framework.activity.Disposable#dispose()
     */
    @Override
    public void dispose() 
    {
        if (this.serviceManager != null) 
        {
            this.serviceManager.release(this.storePersistent);
            this.serviceManager.release(this.storeJanitor);
            this.serviceManager.release(this.storeDefault);
            this.serviceManager.release(this.settings);
            this.storePersistent = null;
            this.storeJanitor = null; 	
            this.storeDefault = null;
            this.settings = null;
        }
        super.dispose();
    }
}
