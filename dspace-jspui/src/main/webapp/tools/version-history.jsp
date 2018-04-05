<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@page import="java.util.List"%>
<%--
  - Version history table with functionalities
  -
  - Attributes:
   --%>

<%@page import="org.dspace.app.webui.util.UIUtil"%>
<%@page import="org.dspace.core.Context"%>
<%@page import="org.dspace.content.Item"%>
<%@page import="org.dspace.eperson.EPerson"%>
<%@page import="org.dspace.versioning.Version"%>
<%@page import="org.dspace.app.webui.util.VersionUtil"%>
<%@page import="org.dspace.versioning.VersionHistory"%>
<%@page import="org.dspace.versioning.factory.VersionServiceFactory"%>
<%@ page import="java.util.UUID" %>
<%@page import="org.dspace.core.ConfigurationManager" %>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>
<%@taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
    UUID itemID = (UUID)request.getAttribute("itemID");
    String versionID = (String)request.getAttribute("versionID");
    Item item = (Item) request.getAttribute("item");
    Boolean removeok = UIUtil.getBoolParameter(request, "delete");
    Context context = UIUtil.obtainContext(request);
    boolean show_submitter = ((Boolean) request.getAttribute("showSubmitter")).booleanValue();

    request.setAttribute("LanguageSwitch", "hide");
%>
<c:set var="dspace.layout.head.last" scope="request">
<script type="text/javascript">
var j = jQuery.noConflict();
</script>
</c:set>
<dspace:layout style="submission" titlekey="jsp.version.history.title">

 <div class="modal fade" id="myModal">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
        <h4 class="modal-title"><fmt:message key="jsp.version.history.delete.warning.head1"/></h4>
      </div>
      <div class="modal-body">
        <p><fmt:message key="jsp.version.history.delete.warning.para1"/><br/><fmt:message key="jsp.version.history.delete.warning.para2"/></p>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-default" data-dismiss="modal"><fmt:message key="jsp.version.history.popup.close"/></button>
        <button type="button" class="btn btn-danger" onclick="j('#myModal').modal('hide');j('#submit_delete').click();"><fmt:message key="jsp.version.history.popup.delete"/></button>
      </div>
    </div><!-- /.modal-content -->
  </div><!-- /.modal-dialog -->
</div><!-- /.modal -->
	
	
    <h1><fmt:message key="jsp.version.history.head2"/></h1>
		
	<% if(removeok) { %><div class="alert alert-success"><fmt:message key="jsp.version.history.delete.success.message"/></div><% } %>
 <form action="<%= request.getContextPath() %>/tools/history" method="post">
	<input type="hidden" name="itemID" value="<%= itemID %>" />
	<input type="hidden" name="versionID" value="<%= versionID %>" />                
    <%-- Versioning table --%>
<%
                
        List<Version> allVersions = (List<Version>) request.getAttribute("allVersions");
						 
%>
	<div id="versionHistory">
	<p class="alert alert-info"><fmt:message key="jsp.version.history.legend"/></p>
	
	
	<table class="table">
		<tr>
			<th id="t0"></th>
			<th id="t1" class="oddRowEvenCol"><fmt:message key="jsp.version.history.column1"/></th>
			<th 			
				id="t2" class="oddRowOddCol"><fmt:message key="jsp.version.history.column2"/></th>
			<% if (show_submitter) { %>
                        <th 
				id="t3" class="oddRowEvenCol"><fmt:message key="jsp.version.history.column3"/></th>
                        <% } %>
			<th 				
				id="t4" class="oddRowOddCol"><fmt:message key="jsp.version.history.column4"/></th>
			<th 
				id="t5" class="oddRowEvenCol"><fmt:message key="jsp.version.history.column5"/> </th>
		</tr>
		
                <% for(Version versRow : allVersions) {
			EPerson versRowPerson = versRow.getEPerson();
			String[] identifierPath = UIUtil.getItemIdentifier(context, versRow.getItem());
                        String url = identifierPath[0];
                        String identifier;
                        if (ConfigurationManager.getBooleanProperty("webui.identifier.strip-prefixes", true))
                        {
                            identifier = identifierPath[2];
                        } else {
                            identifier = identifierPath[3];
                        }

		%>	
		<tr>
			<td headers="t0"><input type="checkbox" class="remove" name="remove" value="<%=versRow.getID()%>"/></td>
			<td headers="t1" class="oddRowEvenCol"><%= versRow.getVersionNumber() %></td>
			<td headers="t2" class="oddRowOddCol"><a href="<%= url %>"><%= identifier %></a><%= item.getID()==versRow.getItem().getID()?"<span class=\"glyphicon glyphicon-asterisk\"></span>":""%></td>
                        <% if (show_submitter) { %>
			<td headers="t3" class="oddRowEvenCol"><a href="mailto:<%= versRowPerson.getEmail() %>"><%=versRowPerson.getFullName() %></a></td>
                        <% } %>
			<td headers="t4" class="oddRowOddCol"><%= versRow.getVersionDate() %></td>
			<td headers="t5" class="oddRowEvenCol"><%= versRow.getSummary() %><a class="btn btn-default pull-right" href="<%= request.getContextPath() %>/tools/version?itemID=<%= versRow.getItem().getID()%>&versionID=<%= versRow.getID() %>&submit_update_version"><span class="glyphicon glyphicon-pencil"></span>&nbsp;<fmt:message key="jsp.version.history.update"/></a></td>
		</tr>
		<% } %>
	</table>
	
	<input  data-toggle="modal" href="#myModal" class="btn btn-danger" type="button" id="fake_submit_delete" value="<fmt:message key="jsp.version.history.delete"/>"/> <input class="btn btn-default" type="submit" value="<fmt:message key="jsp.version.history.return"/>" name="submit_cancel"/>
	</div>
    <div style="display: none">
    	<input type="submit" value="<fmt:message key="jsp.version.history.delete"/>" name="submit_delete" id="submit_delete"/>
    </div>    
</form>


</dspace:layout>
