<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Review file upload info
  -
  - Parameters to pass in to this page (from review.jsp)
  -    submission.jump - the step and page number (e.g. stepNum.pageNum) to create a "jump-to" link
  --%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@page import="org.dspace.core.ConfigurationManager"%>
<%@page import="org.dspace.authorize.AuthorizeServiceImpl"%>
<%@page import="java.util.Date"%>
<%@page import="org.dspace.authorize.ResourcePolicy"%>
<%@page import="java.util.List"%>
<%@ page import="org.dspace.app.webui.servlet.SubmissionController" %>
<%@ page import="org.dspace.app.util.SubmissionInfo" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.content.BitstreamFormat" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.core.Utils" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="javax.servlet.jsp.PageContext" %>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
    request.setAttribute("LanguageSwitch", "hide");

    //get the step number (for jump-to link)
    String stepJump = (String) request.getParameter("submission.jump");
    Boolean isDiscoverable = (Boolean) request.getAttribute("submission.item.isdiscoverable");
    List<ResourcePolicy> rpolicies = (List<ResourcePolicy>)request.getAttribute("submission.item.rpolicies");
    Boolean advanced = (Boolean) request.getAttribute("advancedEmbargo");
%>


<%-- ====================================================== --%>
<%--                    REVIEW POLICY                      --%>
<%-- ====================================================== --%>
<div class="col-md-10">
	<div class="row">
		<span class="metadataFieldLabel col-md-4"><fmt:message key="jsp.submit.access.private_setting.heading"/></span> 
		<span
			class="metadataFieldValue col-md-8"> <% if(isDiscoverable) { %>
			<fmt:message
				key="jsp.submit.access.private_setting.review.discoverable" /> <% } else { %>
			<fmt:message
				key="jsp.submit.access.private_setting.review.notdiscoverable" /> <% } %>
		</span>
	</div>
</div>

<% if(rpolicies!=null && !rpolicies.isEmpty()) { %>
<div class="col-md-10">
		<% if(advanced) { %>
			<div class="row">
				<span class="metadataFieldLabel col-md-10"><fmt:message key="jsp.submit.access.plist.heading"/></span>
			</div>
			<div class="container row">	
				<dspace:policieslist policies="<%= rpolicies %>" showButton="false" />
			</div>
		<% } else { %>
			<% Date startDate = rpolicies.get(0).getStartDate(); 
		    if(startDate!=null) { %>
			<div class="row">
			<span class="metadataFieldLabel col-md-4"><fmt:message key="jsp.submit.access.embargo_setting.heading"/></span>
			<span
			class="metadataFieldValue col-md-8">

		    	<fmt:message key="jsp.submit.access.review.embargoed"><fmt:param><%= startDate %></fmt:param></fmt:message>		    
		    </span>
		    </div>
		    <% } %>
		<% } %>
	
</div>
<% } %>

<div class="col-md-2">
	<input class="btn btn-default" type="submit"
		name="submit_jump_<%=stepJump%>"
		value="<fmt:message key="jsp.submit.access.review.button"/>" />
</div>
