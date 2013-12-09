/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeManager;
import javax.servlet.http.HttpServletRequest;
import org.dspace.handle.HandleManager;
import org.dspace.search.DSQuery;
import org.dspace.search.QueryArgs;
import org.dspace.search.QueryResults;

import java.net.URLDecoder;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.dspace.content.ItemIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.rest.common.ItemReturn;
import org.dspace.rest.search.Search;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.usage.UsageEvent;
import org.dspace.utils.DSpace;
import java.util.List;

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
            
            
           	item_context.setTotal_count(org.dspace.content.Item.getCount(context));
            
            ItemReturn item_return= new ItemReturn();
            item_return.setContext(item_context);
            item_return.setItem(selectedItems);
            return(item_return);
           
            
            
    	 } catch (SQLException e)  {
             log.error(e.getMessage());
             throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
         }
    	
    }
    
    @GET
    @Path("/{prefix}/{suffix}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public org.dspace.rest.common.Item getItem(@PathParam("prefix") Integer prefix, @PathParam("suffix") Integer suffix,  @QueryParam(EXPAND) String expand,
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
            	return luceneSearch(query, expand, limit, offset, request);
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
    	StringBuilder content= new StringBuilder();
    	content.append("<H1>Search by lucene query</H1>");
    	content.append("Use parameter 'q' and specify a correct lucene search query. E.g. /items/search?q=Albert Einstein<br/>");
    	content.append("Additionally you can also provide 'expand', 'limit', and 'offset' parameters to refine the search results.<br/>");
    	content.append("<H1>Search by parameters</H1>");
    	content.append("You can search on the following fields:");
    	content.append("<table border =\"1\">");
    	Iterator<String> search =searchMapping.keySet().iterator();
    	while(search.hasNext()){
    		content.append("<tr><td>"+search.next()+"</td></tr>");
    	}
    	content.append("</table>");
    	content.append("All fields in a query are takens as 'and' catenations.<br/>");
    	content.append("For an exact matching string surround string with '\"', e.g. \"Albert Einstein\". Leaving out '\"' searches for items which either Albert or Einstein.<br/>");
    	content.append("You have to URL encode special characters in your query, e.g. \"Alpha &amp; Omega\" should be submitted as \"Alpha %26 Omega\"<br/>");
    	content.append("Example: /items/search?author=\"Albert Einstein\"&amp;publisher=Oxford<br/>searches for content with the author Albert Einstein which were published by someone with Oxford in their name.");
    	content.append("Sorting can be don either ascending or descanding on one field. Use paramter 'order_asc' or 'order_desc'. You can sort by the following fields: ");
    	Iterator<String> sort = sortMapping.keySet().iterator();
    	content.append("<table border =\"1\">");
    	while(sort.hasNext()){
    		content.append("<tr><td>"+sort.next()+"</td></tr>");
    	}
    	content.append("</table>");
    	content.append("Additionally you can also provide 'expand', 'limit', and 'offset' parameters to refine the search results.<br/>");
    	content.append("Example: /items/search?author=\"Albert Einstein\"&amp;publisher=Oxford&amp;order_desc=title&amp;expand=metadata&amp;limit=10<br/>" +
    			"This searches for content by Albert Einstein and published by someone with Oxford in the titel; the results will contain a maximum of 10 " +
    			"itmes sorted by title in descending order with all metadata information displayed.");
    	return new String("<html><title>Search Help Page</title><body>" + content.toString() + "</body></html>");
    }
    
	private org.dspace.rest.common.ItemReturn luceneSearch(String query,
			String expand, Integer limit, Integer offset,
			HttpServletRequest request) throws IOException, SQLException,
			WebApplicationException {
		
		QueryArgs queryArgs = new QueryArgs();
		queryArgs.setQuery(query);
		QueryResults queryResults = DSQuery.doQuery(context, queryArgs);

		List<String> handleList = queryResults.getHitHandles();
		List<org.dspace.rest.common.Item> dsoList = new ArrayList<org.dspace.rest.common.Item>();
		int added=0;
		int count=0;
		for(String handle : handleList) {
		    org.dspace.content.DSpaceObject dso = HandleManager.resolveToObject(context, handle);
		    if(dso instanceof  org.dspace.content.Item){
		    	if(count>=offset && added<(offset+limit)){
		        	org.dspace.content.Item item = ( org.dspace.content.Item)dso;
		        	if(AuthorizeManager.authorizeActionBoolean(context, item, org.dspace.core.Constants.READ)) {
		        	   dsoList.add(new org.dspace.rest.common.Item(item, expand, context));
		        	   added++;
		        	}
		    	} 
		    	if(added>=limit){
		    		break;
		    	}
		    }
		    count++;
		}
		
		org.dspace.rest.common.ItemReturn item_return = new org.dspace.rest.common.ItemReturn();
		org.dspace.rest.common.Context item_context = new org.dspace.rest.common.Context();
		item_context.setLimit(limit);
		item_context.setOffset(offset);
		item_context.setTotal_count(handleList.size());
		StringBuffer requestURL = request.getRequestURL();
		String queryString = request.getQueryString();

		if (queryString == null) {
			item_context.setQuery(requestURL.toString());
		} else {
			item_context.setQuery(requestURL.append('?').append(queryString).toString());
		}
		item_return.setContext(item_context);
		item_return.setItem(dsoList);
		
		

		return item_return;
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
