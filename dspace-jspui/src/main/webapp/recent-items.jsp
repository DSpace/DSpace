<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="java.sql.*" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>

<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.content.DSpaceObject" %>
<%@ page import="org.dspace.browse.BrowseEngine" %>
<%@ page import="org.dspace.browse.BrowserScope" %>
<%@ page import="org.dspace.browse.BrowseIndex" %>
<%@ page import="org.dspace.browse.BrowseInfo" %>
<%@ page import="org.dspace.browse.BrowseException" %>
<%@ page import="org.dspace.sort.SortOption" %>
<%@ page import="org.dspace.sort.SortException" %>
<%@ page import="org.dspace.content.Item" %>


<% 
	org.dspace.core.Context context = org.dspace.app.webui.util.UIUtil.obtainContext(request); 
%>

<dspace:layout locbar="nolink" titlekey="jsp.collection-home.recentsub" feedData="NONE">

<h2><fmt:message key="jsp.collection-home.recentsub"/></h2>

<table align="center" width="95%" border="0">
            <tr>
            	<td colspan="2"><span style="width: 100%; display: block; border-bottom: 1px dotted #777;" /></td>
            </tr>

<%
	Item[] items = null;

	try {
		// get our configuration
		String source = ConfigurationManager.getProperty("recent.submissions.sort-option");
		String count = "50";
		
		// prep our engine and scope
		BrowseEngine be = new BrowseEngine(context);
		BrowserScope bs = new BrowserScope(context);
		BrowseIndex bi = BrowseIndex.getItemBrowseIndex();
		
		// fill in the scope with the relevant gubbins
		bs.setBrowseIndex(bi);
		bs.setOrder(SortOption.DESCENDING);
		bs.setResultsPerPage(Integer.parseInt(count));
	    for (SortOption so : SortOption.getSortOptions()) {
	        if (so.getName().equals(source))
	            bs.setSortBy(so.getNumber());
	    }
		
		BrowseInfo results = be.browseMini(bs);
		
		items = results.getItemResults(context);
		
	} catch (SortException se) {
	    se.printStackTrace();
	} catch (BrowseException e) {
		e.printStackTrace();
	}

    java.util.Locale sessionLocale = org.dspace.app.webui.util.UIUtil.getSessionLocale(request);
    String locale = sessionLocale.toString();
	
	if (items != null) {
		for (Item item : items) {
			org.dspace.content.Metadatum[] dcv = item.getMetadata("dc", "title", null, Item.ANY);
			String displayTitle = "Untitled";
			if (dcv != null)
			{
				if (dcv.length > 0)
				{
					displayTitle = dcv[0].value;
				}
			}

			org.dspace.content.Metadatum[] dcvTypes = item.getMetadata("dc", "type", null, Item.ANY);
			String displayType = "Unknown";
			if (dcvTypes != null)
			{
				if (dcvTypes.length > 0)
				{
					displayType = ua.edu.sumdu.essuir.utils.EssuirUtils.getTypeLocalized(dcvTypes[0].value, locale);
				}
			}
			
			
            %>
            <tr height="30">
            	<td><a href="<%= request.getContextPath() %>/handle/<%= item.getHandle() %>"><%= displayTitle %></a></td>
            	<td align="right">[<%= displayType.replace(" ", "&nbsp;") %>]</td>
            </tr>
            
            <tr>
            	<td colspan="2"><span style="width: 100%; display: block; border-bottom: 1px dotted #777;" /></td>
            </tr>
            <%
		}
	}
%>
</table>
</dspace:layout>
