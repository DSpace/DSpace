<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Renders a whole HTML page for displaying item metadata.  Simply includes
  - the relevant item display component in a standard HTML page.
  -
  - Attributes:
  -    display.all - Boolean - if true, display full metadata record
  -    item        - the Item to display
  -    collections - Array of Collections this item appears in.  This must be
  -                  passed in for two reasons: 1) item.getCollections() could
  -                  fail, and we're already committed to JSP display, and
  -                  2) the item might be in the process of being submitted and
  -                  a mapping between the item and collection might not
  -                  appear yet.  If this is omitted, the item display won't
  -                  display any collections.
  -    admin_button - Boolean, show admin 'edit' button
  --%>
<%@page import="org.dspace.core.Constants"%>
<%@page import="org.dspace.eperson.EPerson"%>
<%@page import="org.dspace.versioning.VersionHistory"%>
<%@page import="org.elasticsearch.common.trove.strategy.HashingStrategy"%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.DCValue" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.handle.HandleManager" %>
<%@ page import="org.dspace.license.CreativeCommons" %>
<%@page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>
<%@page import="org.dspace.versioning.Version"%>
<%@page import="org.dspace.core.Context"%>
<%@page import="org.dspace.app.webui.util.VersionUtil"%>
<%@page import="org.dspace.app.webui.util.UIUtil"%>
<%@page import="org.dspace.authorize.AuthorizeManager"%>
<%
    // Attributes
    Boolean displayAllBoolean = (Boolean) request.getAttribute("display.all");
    boolean displayAll = (displayAllBoolean != null && displayAllBoolean.booleanValue());
    Boolean suggest = (Boolean)request.getAttribute("suggest.enable");
    boolean suggestLink = (suggest == null ? false : suggest.booleanValue());
    Item item = (Item) request.getAttribute("item");
    Collection[] collections = (Collection[]) request.getAttribute("collections");
    Boolean admin_b = (Boolean)request.getAttribute("admin_button");
    boolean admin_button = (admin_b == null ? false : admin_b.booleanValue());
    
    // get the workspace id if one has been passed
    Integer workspace_id = (Integer) request.getAttribute("workspace_id");

    // get the handle if the item has one yet
    String handle = item.getHandle();

    // CC URL & RDF
    String cc_url = CreativeCommons.getLicenseURL(item);
    String cc_rdf = CreativeCommons.getLicenseRDF(item);

    // Full title needs to be put into a string to use as tag argument
    String title = "";
    if (handle == null)
 	{
		title = "Workspace Item";
	}
	else 
	{
		DCValue[] titleValue = item.getDC("title", null, Item.ANY);
		if (titleValue.length != 0)
		{
			title = titleValue[0].value;
		}
		else
		{
			title = "Item " + handle;
		}
	}
    
    boolean versioningEnabled = ConfigurationManager
            .getBooleanProperty("versioning", "enabled");
    String messageVersionNoticeHead = null;
    String messageVersionNoticeHelp = null;
    boolean hasVersionButton = false;
    boolean hasVersionHistory = false;
    boolean authorizeToVersion = false;
    VersionHistory history = null;
    Context context = null;
    if (versioningEnabled)
    {

        context = UIUtil.obtainContext(request);
        authorizeToVersion = AuthorizeManager.isAdmin(context, item.getOwningCollection());
        if (authorizeToVersion)
        {

            if (VersionUtil.isLatest(context, item)
                    && item.isArchived())
            {
                hasVersionButton = true;
            }

        }

        if (VersionUtil.hasVersionHistory(context, item))
        {
            hasVersionHistory = true;
            history = VersionUtil.retrieveVersionHistory(context, item);
        }
        
        //Check if we have a history for the item
        Version latestVersion = VersionUtil.checkLatestVersion(context,
                item);

        if (latestVersion != null)
        {
            if (latestVersion != null
                    && latestVersion.getItemID() != item.getID())
            {
                //We have a newer version
                Item latestVersionItem = latestVersion.getItem();
                if (latestVersionItem.isArchived())
                {
                    //Available, add a link for the user alerting him that a new version is available        
                    messageVersionNoticeHead = LocaleSupport
                            .getLocalizedMessage(pageContext,
                                    "jsp.version.notice.new_version_head");
                    messageVersionNoticeHelp = LocaleSupport
                            .getLocalizedMessage(pageContext,
                                    "jsp.version.notice.new_version_help");
                    String url = HandleManager.resolveToURL(context, latestVersionItem.getHandle());
                    messageVersionNoticeHelp += "<a href='"+url+"'>"+latestVersionItem.getHandle()+"</a>";
                }
                else
                {
                    //We might be dealing with a workflow/workspace item
                    messageVersionNoticeHead = LocaleSupport
                            .getLocalizedMessage(pageContext,
                                    "jsp.version.notice.workflow_version_head");
                    messageVersionNoticeHelp = LocaleSupport
                            .getLocalizedMessage(pageContext,
                                    "jsp.version.notice.workflow_version_help");

                }
            }
        }
    }
%>

<%@page import="org.dspace.app.webui.servlet.MyDSpaceServlet"%>
<dspace:layout title="<%= title %>">


<%
    if (handle != null)
    {
%>

    <table align="center" class="miscTable">
		<%		
		if (messageVersionNoticeHead != null)
		   {
		%>
		<tr><td>
		<b><%=messageVersionNoticeHead%></b>		
		<%=messageVersionNoticeHelp%>
		</td>
		</tr>
		<%
		    }
		%>
		<tr>
            <td class="evenRowEvenCol" align="center">
                <%-- <strong>Please use this identifier to cite or link to this item:
                <code><%= HandleManager.getCanonicalForm(handle) %></code></strong>--%>
                <strong><fmt:message key="jsp.display-item.identifier"/>
                <code><%= HandleManager.getCanonicalForm(handle) %></code></strong>
            </td>
<%
        if (admin_button)  // admin edit button
        { %>
            <td class="evenRowEvenCol" align="center">
                <form method="post" action="<%= request.getContextPath() %>/mydspace">
                    <input type="hidden" name="item_id" value="<%= item.getID() %>" />
                    <input type="hidden" name="step" value="<%= MyDSpaceServlet.REQUEST_EXPORT_ARCHIVE %>" />
                    <input type="submit" name="submit" value="<fmt:message key="jsp.mydspace.request.export.item"/>" />
                </form>
                <form method="post" action="<%= request.getContextPath() %>/mydspace">
                    <input type="hidden" name="item_id" value="<%= item.getID() %>" />
                    <input type="hidden" name="step" value="<%= MyDSpaceServlet.REQUEST_MIGRATE_ARCHIVE %>" />
                    <input type="submit" name="submit" value="<fmt:message key="jsp.mydspace.request.export.migrateitem"/>" />
                </form>
                <form method="post" action="<%= request.getContextPath() %>/dspace-admin/metadataexport">
                    <input type="hidden" name="handle" value="<%= item.getHandle() %>" />
                    <input type="submit" name="submit" value="<fmt:message key="jsp.general.metadataexport.button"/>" />
                </form>

            </td>
            <td class="evenRowEvenCol" align="center">
                <form method="get" action="<%= request.getContextPath() %>/tools/edit-item">
                    <input type="hidden" name="item_id" value="<%= item.getID() %>" />
                    <%--<input type="submit" name="submit" value="Edit...">--%>
                    <input type="submit" name="submit" value="<fmt:message key="jsp.general.edit.button"/>" />
                </form>
            </td>
<%      } %>
        </tr>
        <% if(hasVersionButton || hasVersionHistory) { %>
        <tr>       
        
			<td class="evenRowEvenCol" align="center">
				<% if(hasVersionButton) { %>       
                <form method="get" action="<%= request.getContextPath() %>/tools/version">
                    <input type="hidden" name="itemID" value="<%= item.getID() %>" />                    
                    <input type="submit" name="submit" value="<fmt:message key="jsp.general.version.button"/>" />
                </form>
                <% } %> 
                <% if(hasVersionHistory && authorizeToVersion) { %>			                
                <form method="get" action="<%= request.getContextPath() %>/tools/history">
                    <input type="hidden" name="itemID" value="<%= item.getID() %>" />
                    <input type="hidden" name="versionID" value="<%= history.getVersion(item)!=null?history.getVersion(item).getVersionId():null %>" />                    
                    <input type="submit" name="submit" value="<fmt:message key="jsp.general.version.history.button"/>" />
                </form>         	         	
				<% } %>
			</td>
		
		</tr>
       <% } %>
    </table>
    <br />
<%
    }

    String displayStyle = (displayAll ? "full" : "");
%>
    <dspace:item-preview item="<%= item %>" />
    <dspace:item item="<%= item %>" collections="<%= collections %>" style="<%= displayStyle %>" />

<%
    String locationLink = request.getContextPath() + "/handle/" + handle;

    if (displayAll)
    {
%>

    <div align="center">
<%
        if (workspace_id != null)
        {
%>
    <form method="post" action="<%= request.getContextPath() %>/view-workspaceitem">
        <input type="hidden" name="workspace_id" value="<%= workspace_id.intValue() %>" />
        <input type="submit" name="submit_simple" value="<fmt:message key="jsp.display-item.text1"/>" />
    </form>
<%
        }
        else
        {
%>
    <form method="get" action="<%=locationLink %>">
        <input type="hidden" name="mode" value="simple"/>
        <input type="submit" name="submit_simple" value="<fmt:message key="jsp.display-item.text1"/>" />
    </form>
<%
        }
%>
    </div>
<%
    }
    else
    {
%>
    <div align="center">
<%
        if (workspace_id != null)
        {
%>
    <form method="post" action="<%= request.getContextPath() %>/view-workspaceitem">
        <input type="hidden" name="workspace_id" value="<%= workspace_id.intValue() %>" />
        <input type="submit" name="submit_full" value="<fmt:message key="jsp.display-item.text2"/>" />
    </form>
<%
        }
        else
        {
%>
    <form method="get" action="<%=locationLink %>">
        <input type="hidden" name="mode" value="full"/>
        <input type="submit" name="submit_simple" value="<fmt:message key="jsp.display-item.text2"/>" />
    </form>
<%
        }
        if (suggestLink)
        {
%>
    <a href="<%= request.getContextPath() %>/suggest?handle=<%= handle %>" target="new_window">
       <fmt:message key="jsp.display-item.suggest"/></a>
<%
        }
%>
    </div>
<%
    }
%>

<div align="center">
    <a class="statisticsLink" href="<%= request.getContextPath() %>/handle/<%= handle %>/statistics"><fmt:message key="jsp.display-item.display-statistics"/></a>
</div>

<%
    if (workspace_id != null)
    {
%>
<div align="center">
   <form method="post" action="<%= request.getContextPath() %>/workspace">
        <input type="hidden" name="workspace_id" value="<%= workspace_id.intValue() %>"/>
        <input type="submit" name="submit_open" value="<fmt:message key="jsp.display-item.back_to_workspace"/>"/>
    </form>
</div>
<%
    }
%>
    <%-- SFX Link --%>
<%
    if (ConfigurationManager.getProperty("sfx.server.url") != null)
    {
        String sfximage = ConfigurationManager.getProperty("sfx.server.image_url");
        if (sfximage == null)
        {
            sfximage = request.getContextPath() + "/image/sfx-link.gif";
        }
%>
    <p align="center">
        <a href="<dspace:sfxlink item="<%= item %>"/>" /><img src="<%= sfximage %>" border="0" alt="SFX Query" /></a>
    </p>
<%
    }
%>
    <%-- Create Commons Link --%>
<%
    if (cc_url != null)
    {
%>
    <p class="submitFormHelp"><fmt:message key="jsp.display-item.text3"/> <a href="<%= cc_url %>"><fmt:message key="jsp.display-item.license"/></a><br/>
    <a href="<%= cc_url %>"><img src="<%= request.getContextPath() %>/image/cc-somerights.gif" border="0" alt="Creative Commons" /></a>
    </p>
    <!--
    <%= cc_rdf %>
    -->
<%
    }
%>
    <%-- Versioning table --%>
<%
    if (versioningEnabled && hasVersionHistory)
    {
        boolean item_history_view_admin = ConfigurationManager
                .getBooleanProperty("versioning", "item.history.view.admin");
        if(!item_history_view_admin || authorizeToVersion) {         
			 
						 
%>
	<div id="versionHistory">
	<h2><fmt:message key="jsp.version.history.head2" /></h2>
	
	
	<table class="miscTable">
		<tr>
			<th id="t1" class="oddRowEvenCol"><fmt:message key="jsp.version.history.column1"/></th>
			<th 			
				id="t2" class="oddRowOddCol"><fmt:message key="jsp.version.history.column2"/></th>
			<th 
				 id="t3" class="oddRowEvenCol"><fmt:message key="jsp.version.history.column3"/></th>
			<th 
				
				id="t4" class="oddRowOddCol"><fmt:message key="jsp.version.history.column4"/></th>
			<th 
				 id="t5" class="oddRowEvenCol"><fmt:message key="jsp.version.history.column5"/> </th>
		</tr>
		
		<% for(Version versRow : history.getVersions()) {  
		
			EPerson versRowPerson = versRow.getEperson();
			String[] identifierPath = VersionUtil.addItemIdentifier(item, versRow);
            //Skip items currently in submission
            if(VersionUtil.isItemInSubmission(context, versRow.getItem()))
            {
                continue;
            }
		%>	
		<tr>			
			<td headers="t1" class="oddRowEvenCol"><%= versRow.getVersionNumber() %></td>
			<td headers="t2" class="oddRowOddCol"><a href="<%= request.getContextPath() + identifierPath[0] %>"><%= identifierPath[1] %></a><%= item.getID()==versRow.getItemID()?"*":""%></td>
			<td headers="t3" class="oddRowEvenCol"><% if(authorizeToVersion) { %><a
				href="mailto:<%= versRowPerson.getEmail() %>"><%=versRowPerson.getFullName() %></a><% } else { %><%=versRowPerson.getFullName() %><% } %></td>
			<td headers="t4" class="oddRowOddCol"><%= versRow.getVersionDate() %></td>
			<td headers="t5" class="oddRowEvenCol"><%= versRow.getSummary() %></td>
		</tr>
		<% } %>
	</table>
	<p><fmt:message key="jsp.version.history.legend"/></p>
	</div>
	
    
<%
        }
    }
%>
    <p class="submitFormHelp"><fmt:message key="jsp.display-item.copyright"/></p>
</dspace:layout>
