<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@page import="org.apache.solr.client.solrj.response.TermsResponse.Term"%>
<%@page import="java.util.List"%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.app.webui.util.CurateTaskResult" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.apache.solr.client.solrj.response.FacetField.Count" %>

<%
	List<Count> terms = (List<Count>) request.getAttribute("terms");
	List<Count> dnsTerms = (List<Count>) request.getAttribute("dnsterms");
	List<Count> domainDnsTerms = (List<Count>) request.getAttribute("domaindnsterms");
	Boolean deleted = (Boolean) request.getAttribute("deleted");
	Integer prevUserAgent = (Integer) request.getAttribute("previoususeragent");
	Integer nextUserAgent = (Integer) request.getAttribute("nextuseragent");
	Integer prevDns = (Integer) request.getAttribute("previousdns");
	Integer nextDns = (Integer) request.getAttribute("nextdns");
	Integer prevDomainDns = (Integer) request.getAttribute("previousdomaindns");
	Integer nextDomainDns = (Integer) request.getAttribute("nextdomaindns");

%>
<dspace:layout 
			   style="submission"
			   titlekey="jsp.dspace-admin.stats-cleaner.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">


  <h1><fmt:message key="jsp.dspace-admin.stats-cleaner.heading"/></h1>

<% if (deleted != null && deleted) { %>
<div class="alert alert-success">Deleted</div>
<% } %>
<div class="col-md-4">
  <h3><fmt:message key="jsp.dspace-admin.stats-cleaner.useragent"/></h3>
<% for (Count t : terms) { %>
 <form action="" method="post">
 	<div class="form-group">
	 <div class="container alert alert-info"> 
  		<div class="col-md-2"><button class="btn btn-danger" type="submit" value="delete"><i class="fa fa-trash-o "></i></button></div>
  		<div class="col-md-10">
 			<%= t.getName() %> <span class="badge">[<%= t.getCount() %>]</span>
			<input type="hidden"  name="userAgent" value="<%= t.getName() %>" />
		</div>
	</div>
	</div>
</form>
<% } %>
<div class="pull-left">
<% if(prevUserAgent!= null ){%>
<form action="" method="post">
<input type="hidden" name="userAgentoffset" value="<%= prevUserAgent %>" />
<button class="btn btn-info" type="submit"> << </button>
</form>
<%} %>
</div>
<div class="pull-right">
<% if(nextUserAgent!= null ){%>
<form action="" method="post">
<input type="hidden" name="userAgentoffset" value="<%= nextUserAgent %>" />
<button class="btn btn-info" type="submit"> >> </button>
</form>
<%} %>
</div>
</div>
<div class="col-md-4">
<h3><fmt:message key="jsp.dspace-admin.stats-cleaner.dns"/></h3>
<% for (Count t : dnsTerms) { %>
<form action="" method="post">
 	<div class="form-group">
	 <div class="container alert alert-info"> 
  		<div class="col-md-2"><button class="btn btn-danger" type="submit" value="delete"><i class="fa fa-trash-o "></i></button></div>
  		<div class="col-md-10">
 			<%= t.getName() %> <span class="badge">[<%= t.getCount() %>]</span>
			<input type="hidden"  name="dns" value="<%= t.getName() %>" />
		</div>
	</div>
	</div>
</form>
<% } %>
</div>
<div class="col-md-4">
<h3><fmt:message key="jsp.dspace-admin.stats-cleaner.domaindns"/></h3>
<% for (Count t : domainDnsTerms) { %>
<form action="" method="post">
 	<div class="form-group">
	 <div class="container alert alert-info"> 
  		<div class="col-md-2"><button class="btn btn-danger" type="submit" value="delete"><i class="fa fa-trash-o "></i></button></div>
  		<div class="col-md-10">
 			<%= t.getName() %> <span class="badge">[<%= t.getCount() %>]</span>
			<input type="hidden"  name="domaindns" value="<%= t.getName() %>" />
		</div>
	</div>
	</div>
</form>
<% } %>
</div>
</dspace:layout>
