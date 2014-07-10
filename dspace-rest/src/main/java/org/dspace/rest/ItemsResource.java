/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.handle.HandleManager;
import org.dspace.rest.common.ItemReturn;
import org.dspace.rest.search.Search;
//import org.dspace.search.DSQuery;
//import org.dspace.search.QueryArgs;
//import org.dspace.search.QueryResults;
import org.dspace.usage.UsageEvent;
import org.dspace.utils.DSpace;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: peterdietz
 * Date: 9/19/13
 * Time: 4:54 PM
 * To change this template use File | Settings | File Templates.
 */
@Path("/items")
public class ItemsResource {
	
	private static final String OFFSET = "offset";
	private static final String LIMIT = "limit";
	private static final String EXPAND = "expand";
	private static final String ORDER_ASC = "order_asc";
	private static final String ORDER_DESC = "order_desc";

	private static final String SEARCH_PREFIX="item.search.";
	private static final String SORT_PREFIX="item.sort.";
	
	private static final boolean writeStatistics;
	private static final int maxPagination;
	private static final HashMap<String,String> searchMapping;
	private static final HashMap<String,String> sortMapping;
	private static final ArrayList<String> reservedWords;
	private static final String searchClass;
	
	static{
		writeStatistics=ConfigurationManager.getBooleanProperty("rest","stats",false);
		maxPagination=ConfigurationManager.getIntProperty("rest", "max_pagination");
		HashMap<String,String> sm = new HashMap<String,String>();
		HashMap<String,String> sortm = new HashMap<String,String>();
		Enumeration<?> propertyNames = ConfigurationManager.getProperties("rest").propertyNames();
        while(propertyNames.hasMoreElements())
        {
            String key = ((String) propertyNames.nextElement()).trim();
            if (key.startsWith(SEARCH_PREFIX)){
            	sm.put(key.substring(SEARCH_PREFIX.length()), ConfigurationManager.getProperty("rest", key));
            }
            if (key.startsWith(SORT_PREFIX)){
            	sortm.put(key.substring(SORT_PREFIX.length()), ConfigurationManager.getProperty("rest", key));
            }
        }
        searchMapping = sm;
        sortMapping = sortm;

        searchClass=ConfigurationManager.getProperty("rest","implementing.search.class");
        
        ArrayList<String> reservedWord= new ArrayList<String>();
        reservedWord.add(OFFSET);
        reservedWord.add(LIMIT);
        reservedWord.add(EXPAND);
        reservedWord.add(ORDER_ASC);
        reservedWord.add(ORDER_DESC);
        reservedWords=reservedWord;
	}
	
	/** log4j category */
    private static final Logger log = Logger.getLogger(ItemsResource.class);
    //ItemList - Not Implemented

    private static org.dspace.core.Context context;
    
    @GET
    @Path("/")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public org.dspace.rest.common.ItemReturn list(
    		@QueryParam(EXPAND) String expand,
    		@QueryParam(LIMIT) Integer size, 
    		@QueryParam(OFFSET) Integer offset,
    		@Context HttpServletRequest request)  throws WebApplicationException {
    	
    	try {
            if(context == null || !context.isValid()) {
                context = new org.dspace.core.Context();
                //Failed SQL is ignored as a failed SQL statement, prevent: current transaction is aborted, commands ignored until end of transaction block
                context.getDBConnection().setAutoCommit(true);
            }
            //make sure maximum count per page is more than allowed
            if(size==null || size>maxPagination){
            	size=maxPagination;
            }
            if(offset==null){
            	offset=0;
            }
            ArrayList<org.dspace.rest.common.Item> selectedItems= new ArrayList<org.dspace.rest.common.Item>();
            ItemIterator items = org.dspace.content.Item.findAll(context);
            int count=0;
            int added=0;
            org.dspace.content.Item item;
            while(items.hasNext() && added<size){
            	item = items.next();
            	if(count>=offset && added<(offset+size)){
            		if(AuthorizeManager.authorizeActionBoolean(context, item, org.dspace.core.Constants.READ)) {
                        selectedItems.add(new org.dspace.rest.common.Item(item, expand, context));
                        added++;
                    }
            	}
            	count++;
            }
            
            org.dspace.rest.common.Context item_context = new org.dspace.rest.common.Context();
            item_context.setLimit(size);
            item_context.setOffset(offset);
            StringBuffer requestURL = request.getRequestURL();
            String queryString = request.getQueryString();

            if (queryString == null) {
            	item_context.setQuery(requestURL.toString());
            } else {
            	item_context.setQuery(requestURL.append('?').append(queryString).toString());
            }
            
            item_context.setTotal_count(Item.countAll(context));
            
            ItemReturn item_return= new ItemReturn();
            item_return.setContext(item_context);
            item_return.setItem(selectedItems);
            return(item_return);
           
            
            
    	 } catch (SQLException e)  {
             log.error(e.getMessage());
             throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
         }
    	
    }

    /**
     * Get a specific Item by its Handle: prefix/suffix
     */
    @GET
    @Path("/{prefix}/{suffix}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public org.dspace.rest.common.Item getItem(@PathParam("prefix") String prefix, @PathParam("suffix") String suffix,  @QueryParam(EXPAND) String expand,
    		@QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent, @QueryParam("xforwarderfor") String xforwarderfor,
    		@Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException {
    	
    	
        try {
            if(context == null || !context.isValid()) {
                context = new org.dspace.core.Context();
                //Failed SQL is ignored as a failed SQL statement, prevent: current transaction is aborted, commands ignored until end of transaction block
                context.getDBConnection().setAutoCommit(true);
            }
            
            org.dspace.content.DSpaceObject dso = HandleManager.resolveToObject(context, prefix + "/" + suffix);
            if(dso instanceof org.dspace.content.Item){
            	org.dspace.content.Item item = (org.dspace.content.Item)dso;
	            if(AuthorizeManager.authorizeActionBoolean(context, item, org.dspace.core.Constants.READ)) {
	            	if(writeStatistics){
	    				writeStats(item, user_ip, user_agent, xforwarderfor, headers, request);
	    			}
	                return new org.dspace.rest.common.Item(item, expand, context);
	            } else {
	                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
	            }
            } else {
            	throw new WebApplicationException(Response.Status.NO_CONTENT);
            }

        } catch (SQLException e)  {
            log.error(e.getMessage());
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get an Item by its internal item_id
     */
    @GET
    @Path("/{item_id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public org.dspace.rest.common.Item getItem(@PathParam("item_id") Integer item_id, @QueryParam(EXPAND) String expand,
    		@QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent, @QueryParam("xforwarderfor") String xforwarderfor,
    		@Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException {
    	
    	
        try {
            if(context == null || !context.isValid()) {
                context = new org.dspace.core.Context();
                //Failed SQL is ignored as a failed SQL statement, prevent: current transaction is aborted, commands ignored until end of transaction block
                context.getDBConnection().setAutoCommit(true);
            }

            org.dspace.content.Item item = org.dspace.content.Item.find(context, item_id);

            if(AuthorizeManager.authorizeActionBoolean(context, item, org.dspace.core.Constants.READ)) {
            	if(writeStatistics){
    				writeStats(item, user_ip, user_agent, xforwarderfor, headers, request);
    			}
                return new org.dspace.rest.common.Item(item, expand, context);
            } else {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }

        } catch (SQLException e)  {
            log.error(e.getMessage());
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
    
    
    // /items/search?q=Albert Einstein
    @GET
    @Path("/search")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public org.dspace.rest.common.ItemReturn search(
    		@QueryParam("q") String query,
    		@QueryParam(EXPAND) String expand,
    		@QueryParam(LIMIT) Integer limit, 
    		@QueryParam(OFFSET) Integer offset,
    		@QueryParam(ORDER_ASC) String order_asc,
    		@QueryParam(ORDER_DESC) String order_desc,
    		@Context HttpServletRequest request) throws WebApplicationException{
        try {
            if(context == null || !context.isValid()) {
                context = new org.dspace.core.Context();
                //Failed SQL is ignored as a failed SQL statement, prevent: current transaction is aborted, commands ignored until end of transaction block
                context.getDBConnection().setAutoCommit(true);
            }
            if(limit==null || limit>maxPagination){
    			limit=maxPagination;
    		}
    		if(offset==null){
    			offset=0;
    		}
            
            if(query!=null){
            	return luceneSearch(query, expand, limit, offset, request, order_asc, order_desc);
            } else {
            	return parameterSearch(expand, limit, offset, request, order_asc, order_desc);
            }

        } catch (SQLException e) {
            log.error(e.getMessage());
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
    
    
    
    @GET
    @Path("/search/help")
    @Produces({MediaType.TEXT_HTML})
    public String search_help(){
    	BufferedReader br=null;
    	InputStream input=null;
    	String marker1="\\{searchfields\\}";
    	String marker2="\\{sortfields\\}";
    	StringBuilder content = new StringBuilder();
    	StringBuilder searchfields= new StringBuilder();
    	StringBuilder sortfields= new StringBuilder();
    	try {
			input= this.getClass().getClassLoader().getResourceAsStream("/html/searchHelp.html");
			br = new BufferedReader(new InputStreamReader(input));
			String line;
			while((line=br.readLine())!=null){
				content.append(line);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			log.error(e);
		} catch (IOException e) {
			e.printStackTrace();
			log.error(e);
		} finally {
			if(input!=null){
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
					log.error(e);
				}
			}
			if(br!=null){
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
					log.error(e);
				}
			}
		}
    	Iterator<String> search =searchMapping.keySet().iterator();
    	while(search.hasNext()){
    		searchfields.append("<tr><td>"+search.next()+"</td></tr>");
    	}
    	Iterator<String> sort = sortMapping.keySet().iterator();
    	while(sort.hasNext()){
    		sortfields.append("<tr><td>"+sort.next()+"</td></tr>");
    	}
    	String all=content.toString();
    	all=all.replaceAll(marker1, searchfields.toString());
    	all=all.replaceAll(marker2, sortfields.toString());
    	
    	return all;
    }

        
    
	private org.dspace.rest.common.ItemReturn luceneSearch(String query,
	String expand, Integer limit, Integer offset,
	HttpServletRequest request, String order_asc, String order_desc) throws IOException, SQLException,
	WebApplicationException {
		org.dspace.rest.common.ItemReturn item_return = new org.dspace.rest.common.ItemReturn();
		org.dspace.rest.common.Context item_context = new org.dspace.rest.common.Context();
		item_context.setLimit(limit);
		item_context.setOffset(offset);
		item_return.setContext(item_context);
		if(order_asc!=null && order_desc!=null){
			log.error("Both order ascending and order descending set - invalid use");
			item_context.addError("It is not allowed to set both parameters 'order_asc' and 'order_desc'.");
			return item_return;
		}
		String sortfield=null;
		String field=null;
		String sortorder=null;
		if(order_asc!=null){
			sortorder="asc";
			field=order_asc;
		} else if (order_desc!=null){
			sortorder="desc";
			field=order_desc;
		}
		
		if(field!=null && sortMapping.containsKey(field)){
			sortfield = sortMapping.get(field);
		} else if(field!=null){
			log.error("order field " + field+ " not supported");
			item_context.addError("not recognised order field: " + field);
			return item_return;
		}
		
		try{
			Class<?> clazz = Class.forName(searchClass);
			Constructor<?> constructor = clazz.getConstructor();
			Search instance = (Search) constructor.newInstance();
			item_return.setItem(instance.searchAll(context, query, expand, limit, offset, sortfield, sortorder));
			item_context.setTotal_count(instance.getTotalCount());
		} catch(ClassNotFoundException ex) {
			item_context.addError("'implementing.search.class' does not point to an existing class");
			log.error(ex);
		} catch(NoSuchMethodException ex) {
			item_context.addError("'implementing.search.class' does have an empty contructor");
			log.error(ex);
		} catch(InstantiationException ex) {
			item_context.addError("constructor for 'implementing.search.class' could not be instantiated");
			log.error(ex);
		} catch(IllegalAccessException ex) {
			item_context.addError("'caught IllegalAccessException for instance of 'implementing.search.class'");
			log.error(ex);
		} catch(InvocationTargetException ex) {
			item_context.addError("'caught InvocationTargetException for instance of 'implementing.search.class'");
			log.error(ex);
		}
    		
		return item_return;
	}
    
	
	private  ArrayList<Integer> luceneIdSearch(String query,
	HttpServletRequest request) throws IOException, SQLException,
	WebApplicationException {
		
		try{
			Class<?> clazz = Class.forName(searchClass);
			Constructor<?> constructor = clazz.getConstructor();
			Search instance = (Search) constructor.newInstance();
			return instance.searchIdAll(context, query);
		} catch(ClassNotFoundException ex) {
			log.error(ex);
		} catch(NoSuchMethodException ex) {
			log.error(ex);
		} catch(InstantiationException ex) {
			log.error(ex);
		} catch(IllegalAccessException ex) {
			log.error(ex);
		} catch(InvocationTargetException ex) {
			log.error(ex);
		}
    		
		return null;
	}
	
	private org.dspace.rest.common.ItemReturn parameterSearch(
			String expand, Integer limit, Integer offset,
			HttpServletRequest request, String order_asc, String order_desc) throws IOException, SQLException,
			WebApplicationException {
				
		org.dspace.rest.common.ItemReturn item_return = new org.dspace.rest.common.ItemReturn();
		org.dspace.rest.common.Context item_context = new org.dspace.rest.common.Context();
		item_context.setLimit(limit);
		item_context.setOffset(offset);
		item_return.setContext(item_context);
		StringBuffer requestURL = request.getRequestURL();
		String queryString = request.getQueryString();

		if (queryString == null) {
			item_context.setQuery(requestURL.toString());
		} else {
			item_context.setQuery(requestURL.append('?').append(URLDecoder.decode(queryString,"UTF-8")).toString());
		}		
		if(searchClass==null){
			log.error("'implementing.search.class' not set in rest config");
			item_context.addError("'implementing.search.class' not set in rest config");
			return item_return;
		}		

		Map<String,String[]> requestMap=request.getParameterMap();
		
		
		HashMap<String,String> querymap= new HashMap<String,String>();
		Iterator<String> requestKeys=requestMap.keySet().iterator();
		while(requestKeys.hasNext()){
			String key = requestKeys.next();
			String[] values = requestMap.get(key);
			log.debug("key, value " + key + " " + values);
			if(searchMapping.containsKey(key) && values!=null){
				for(String value : values){
					querymap.put(searchMapping.get(key), URLDecoder.decode(value,"UTF-8"));
					log.debug("segments " + key + " " +"not decoded "+ value +" decoded "+ URLDecoder.decode(value,"UTF-8"));
				}
			} else if(!reservedWords.contains(key)){
				log.error("query parameter " + key + " not supported or value null");
				item_context.addError("not recognised query parameter: " + key);
				return item_return;
			}
		}
		
		if(order_asc!=null && order_desc!=null){
			log.error("Both order ascending and order descending set - invalid use");
			item_context.addError("It is not allowed to set both parameters 'order_asc' and 'order_desc'.");
			return item_return;
		}
		String sortfield=null;
		String field=null;
		String sortorder=null;
		if(order_asc!=null){
			sortorder="asc";
			field=order_asc;
		} else if (order_desc!=null){
			sortorder="desc";
			field=order_desc;
		}
		
		if(field!=null && sortMapping.containsKey(field)){
			sortfield = sortMapping.get(field);
		} else if(field!=null){
			log.error("order field " + field+ " not supported");
			item_context.addError("not recognised order field: " + field);
			return item_return;
		}

		
		
		try{
			Class<?> clazz = Class.forName(searchClass);
			Constructor<?> constructor = clazz.getConstructor();
			Search instance = (Search) constructor.newInstance();
			item_return.setItem(instance.search(context, querymap, expand, limit, offset,sortfield,sortorder));
			item_context.setTotal_count(instance.getTotalCount());
		} catch(ClassNotFoundException ex) {
			item_context.addError("'implementing.search.class' does not point to an existing class");
			log.error(ex);
		} catch(NoSuchMethodException ex) {
			item_context.addError("'implementing.search.class' does have an empty contructor");
			log.error(ex);
		} catch(InstantiationException ex) {
			item_context.addError("constructor for 'implementing.search.class' could not be instantiated");
			log.error(ex);
		} catch(IllegalAccessException ex) {
			item_context.addError("'caught IllegalAccessException for instance of 'implementing.search.class'");
			log.error(ex);
		} catch(InvocationTargetException ex) {
			item_context.addError("'caught InvocationTargetException for instance of 'implementing.search.class'");
			log.error(ex);
		}
    		
		return item_return;
	}
	
	private ArrayList<Integer> parameterIdSearch(
			HttpServletRequest request) throws IOException, SQLException,
			WebApplicationException {
				
		if(searchClass==null){
			log.error("'implementing.search.class' not set in rest config");
			return null;
		}		

		Map<String,String[]> requestMap=request.getParameterMap();
		
		
		HashMap<String,String> querymap= new HashMap<String,String>();
		Iterator<String> requestKeys=requestMap.keySet().iterator();
		while(requestKeys.hasNext()){
			String key = requestKeys.next();
			String[] values = requestMap.get(key);
			log.debug("key, value " + key + " " + values);
			if(searchMapping.containsKey(key) && values!=null){
				for(String value : values){
					querymap.put(searchMapping.get(key), URLDecoder.decode(value,"UTF-8"));
					log.debug("segments " + key + " " +"not decoded "+ value +" decoded "+ URLDecoder.decode(value,"UTF-8"));
				}
			} else if(!reservedWords.contains(key)){
				log.error("query parameter " + key + " not supported or value null");
				return null;
			}
		}

			
		try{
			Class<?> clazz = Class.forName(searchClass);
			Constructor<?> constructor = clazz.getConstructor();
			Search instance = (Search) constructor.newInstance();
			return instance.searchId(context, querymap);
		} catch(ClassNotFoundException ex) {
			log.error(ex);
		} catch(NoSuchMethodException ex) {
			log.error(ex);
		} catch(InstantiationException ex) {
			log.error(ex);
		} catch(IllegalAccessException ex) {
			log.error(ex);
		} catch(InvocationTargetException ex) {
			log.error(ex);
		}
    		
		return null;
	}

	
    private void writeStats(org.dspace.content.DSpaceObject dso, String user_ip, String user_agent,
			String xforwarderfor, HttpHeaders headers,
			HttpServletRequest request) {
		
		if(user_ip==null || user_ip.length()==0){
			new DSpace().getEventService().fireEvent(
                     new UsageEvent(
                                     UsageEvent.Action.VIEW,
                                     request,
                                     context,
                                     dso));
		} else{
    		new DSpace().getEventService().fireEvent(
                     new UsageEvent(
                                     UsageEvent.Action.VIEW,
                                     user_ip,
                                     user_agent,
                                     xforwarderfor,
                                     context,
                                     dso));
		}
		log.debug("fired event");
    		
    		
	}
}
