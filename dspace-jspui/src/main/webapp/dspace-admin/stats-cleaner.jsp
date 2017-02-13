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

<%
	List<Term> terms = (List<Term>) request.getAttribute("terms");
	List<Term> dnsTerms = (List<Term>) request.getAttribute("dnsterms");
	List<Term> domainDnsTerms = (List<Term>) request.getAttribute("domaindnsterms");
	Boolean deleted = (Boolean) request.getAttribute("deleted");
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
<% for (Term t : terms) { %>
 <form action="" method="post">
 	<div class="form-group">
	 <div class="container alert alert-info"> 
  		<div class="col-md-2"><button class="btn btn-danger" type="submit" value="delete"><i class="fa fa-trash-o "></i></button></div>
  		<div class="col-md-10">
 			<%= t.getTerm() %> <span class="badge">[<%= t.getFrequency() %>]</span>
			<input type="hidden"  name="userAgent" value="<%= t.getTerm() %>" />
		</div>
	</div>
	</div>
</form>
<% } %>
</div>
<div class="col-md-4">
<h3><fmt:message key="jsp.dspace-admin.stats-cleaner.dns"/></h3>
<% for (Term t : dnsTerms) { %>
<form action="" method="post">
 	<div class="form-group">
	 <div class="container alert alert-info"> 
  		<div class="col-md-2"><button class="btn btn-danger" type="submit" value="delete"><i class="fa fa-trash-o "></i></button></div>
  		<div class="col-md-10">
 			<%= t.getTerm() %> <span class="badge">[<%= t.getFrequency() %>]</span>
			<input type="hidden"  name="dns" value="<%= t.getTerm() %>" />
		</div>
	</div>
	</div>
</form>
<% } %>
</div>
<div class="col-md-4">
<h3><fmt:message key="jsp.dspace-admin.stats-cleaner.domaindns"/></h3>
<% for (Term t : domainDnsTerms) { %>
<form action="" method="post">
 	<div class="form-group">
	 <div class="container alert alert-info"> 
  		<div class="col-md-2"><button class="btn btn-danger" type="submit" value="delete"><i class="fa fa-trash-o "></i></button></div>
  		<div class="col-md-10">
 			<%= t.getTerm() %> <span class="badge">[<%= t.getFrequency() %>]</span>
			<input type="hidden"  name="domaindns" value="<%= t.getTerm() %>" />
		</div>
	</div>
	</div>
</form>
<% } %>
</div>
</dspace:layout>
