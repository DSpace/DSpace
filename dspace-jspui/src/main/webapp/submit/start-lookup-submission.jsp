<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - UI page for start of a submission with lookup on external sources.
  -
  - Required attributes:
  -    collections - Array of collection objects to show in the drop-down.
  	   collectionID - the collection ID of preference for the user
  	   identifiers2providers
       searchProviders
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.submit.lookup.SubmissionLookupProvider" %>
<%@ page import="java.lang.Boolean" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core"
    prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>
	
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    String contextPath = "/dspace-jspui";
	request.setAttribute("LanguageSwitch", "hide");

    //get collections to choose from
    Collection[] collections =
        (Collection[]) request.getAttribute("collections");

    //get community handle
    int communityId = (Integer) request.getAttribute("collectionID");
    
    //check if we need to display the "no collection selected" error
    Boolean noCollection = (Boolean) request.getAttribute("no.collection");
    Boolean nosuuid = (Boolean) request.getAttribute("nouuid");
    Boolean expired = (Boolean) request.getAttribute("expired");
    
    Map<String, List<SubmissionLookupProvider>> identifiers2providers = (Map<String, List<SubmissionLookupProvider>>) request.getAttribute("identifiers2providers");
    List<SubmissionLookupProvider> searchProviders = (List<SubmissionLookupProvider>) request.getAttribute("searchProviders");
    List<String> identifiers = (List<String>) request.getAttribute("identifiers");
    String uuid = (String) request.getAttribute("s_uuid");
%>
<c:set var="dspace.layout.head" scope="request">
	<style type="text/css">
		
	#link-ricerca-identificatore {cursor: pointer; font-weight: bold; color: #FF6600;}
	.sl-result {padding: 10px;}
	.sl-result:HOVER {background-color: #5C9CCC;}
	.sl-result-title, .sl-result-authors, .sl-result-date {display: block;}
	.sl-result-title {font-weight: bold;}
	.sl-result-authors {font-style: italic;}
	.sl-result-date {margin-bottom: 10px;}
	div.submission-lookup-details {max-height: 400px; height: 400px; overflow-y: auto;}
	div.submission-lookup-details table {padding: 10px;margin-bottom:10px;}
	div.submission-lookup-details table td {vertical-align: top;}
	div.submission-lookup-details table td.submission-lookup-label {font-weight: bold;margin-right:10px;}
	.submission-lookup-providers img {vertical-align: middle; margin-right: 10px;}
	.invalid-value {border: 1px solid #FF6600;}
	#tabs-search-accordion td.submission-lookup-providers {text-align: left; padding-left: 10px;}
	
	</style>	
	<script type='text/javascript'>var dspaceContextPath = "<%=request.getContextPath()%>";</script>		
</c:set>
<c:set var="dspace.layout.head.last" scope="request">		
	<script type="text/javascript" src="<%= request.getContextPath() %>/static/js/submission-lookup.js"></script>	
</c:set>

<dspace:layout locbar="off"
               navbar="off"
               titlekey="jsp.submit.start-lookup-submission.title"
               nocache="true">

    <h1><fmt:message key="jsp.submit.start-lookup-submission.heading"/></h1>
    <div id="jserrormessage" style="display: none"><fmt:message key="jsp.submit.start-lookup-submission.js.errormessage"/></div>
    <div id="jsseedetailsbuttonmessage" style="display: none"><fmt:message key="jsp.submit.start-lookup-submission.js.detailsbuttonmessage"/></div>
    <div id="jsfilldatabuttonmessage" style="display: none"><fmt:message key="jsp.submit.start-lookup-submission.js.filldataandstartbuttonmessage"/></div>
    <div id="jstitlepopupmessage" style="display: none"><fmt:message key="jsp.submit.start-lookup-submission.js.titlepopupmessage"/></div>
    
<%  if (collections.length > 0)
    {
		//if no collection was selected, display an error
		if((noCollection != null) && (noCollection.booleanValue()==true))
		{
%>
                <div class="error-message">
					<p><fmt:message key="jsp.submit.start-lookup-submission.no-collection"/></p>
				</div>
<%
		}
		//if no collection was selected, display an error
		if((nosuuid != null) && (nosuuid.booleanValue()==true))
		{
%>
                <div class="error-message">
					<p><fmt:message key="jsp.submit.start-lookup-submission.nosuuid"/></p>
				</div>
<%
		}
		//if no collection was selected, display an error
		if((expired != null) && (expired.booleanValue()==true))
		{
%>
                <div class="error-message">
					<p><fmt:message key="jsp.submit.start-lookup-submission.expired"/></p>
				</div>
<%
		}
%>            

<div id="tabs">
	<ul>
		<li><a href="#tabs-search"><fmt:message key="jsp.submit.start-lookup-submission.tabs.search" /></a></li>
		<li><a href="#tabs-result"><fmt:message key="jsp.submit.start-lookup-submission.tabs.result" /></a></li>
	</ul>
	<div id="tabs-search">
	<form id="form-submission" action="" method="post">
		<input type="hidden" id="suuid" name="suuid" value="<%= uuid %>"/>
		<input type="hidden" id="iuuid" name="iuuid" value=""/>
		<input type="hidden" id="collectionid" name="collectionid" value=""/>
		
		<div id="tabs-search-accordion">
<%		
	if (searchProviders != null && searchProviders.size() > 0) {
	%>
		<h3><a href="#"><fmt:message key="jsp.submit.start-lookup-submission.search"/></a></h3>
		<div>
		
<%	
		for (SubmissionLookupProvider provider : searchProviders)
		{			
%>
		<img style="vertical-align: middle;" src="<%= request.getContextPath() %>/image/submission-lookup-small-<%= provider.getShortName() %>.jpg" />
<% 
		}
	%>
	<p><fmt:message key="jsp.submit.start-lookup-submission.search.hints"/></p>
		<br/><br/><table>
		<tr><td style="vertical-align: top;"><span><fmt:message key="jsp.submit.start-lookup-submission.search.title"/>:</span></td><td> 
		<textarea class="submission-lookup-search" name="search_title" id="search_title" cols="50" row="4"></textarea>
		<br/>&nbsp;</td><td>&nbsp;</td></tr>

		<tr><td><span><fmt:message key="jsp.submit.start-lookup-submission.search.year"/>:</span></td><td> 
		<input class="submission-lookup-search" type="text" size="7" name="search_year" id="search_year" />
		<br/>&nbsp;</td><td>&nbsp;</td></tr>
		
		<tr><td><span><fmt:message key="jsp.submit.start-lookup-submission.search.authors"/>:</span></td><td> 
		<textarea class="submission-lookup-search" name="search_authors" id="search_authors"cols="50" row="4"></textarea>
		<br/>&nbsp;</td><td>&nbsp;</td></tr>
		
		<tr><td>&nbsp;</td><td>&nbsp;</td>
		<td align="right">
			<button type="button" id="search_go"><fmt:message key="jsp.submit.start-lookup-submission.search-go"/></button>
			<button type="button" class="exit"><fmt:message key="jsp.submit.start-lookup-submission.exit"/></button>
		<br/>&nbsp;</td></tr>
		</table>
	</div>
<% } %>	
	<h3><a href="#"><fmt:message key="jsp.submit.start-lookup-submission.identifiers"/></a></h3>
	<div>
<% if (identifiers != null && identifiers.size()>0) {
	%>
		
		<p><fmt:message key="jsp.submit.start-lookup-submission.identifiers.hints"/></p>
		<br/><br/><table>
<%	
		for (String identifier : identifiers)
		{			
%>
<c:set var="identifier"><%= identifier %></c:set>
		<tr><td><span class="submission-lookup-label"><fmt:message key="jsp.submit.start-lookup-submission.identifier-${identifier}"/>:</span></td><td> 
		<span class="submission-lookup-hint"><fmt:message key="jsp.submit.start-lookup-submission.identifier-${identifier}.hint"/></span><br/>
		<input class="submission-lookup-identifier" type="text" size="50" name="identifier_<%= identifier%>" id="identifier_<%= identifier%>" /><br/>&nbsp;</td><td class="submission-lookup-providers">
<%	
			for (SubmissionLookupProvider provider : identifiers2providers.get(identifier))
			{			
%>
		<img src="<%= request.getContextPath() %>/image/submission-lookup-small-<%= provider.getShortName() %>.jpg" />&nbsp;
<% 
			}
%></td></tr><%
		} %>				
		<tr><td>&nbsp;</td><td>&nbsp;</td><td><br/>
		<button type="button" id="lookup_idenfifiers"><fmt:message key="jsp.submit.start-lookup-submission.identifier.lookup"/></button>
		<button type="button" class="exit"><fmt:message key="jsp.submit.start-lookup-submission.exit"/></button></td></tr>
	</table>
	</div>
<% 
		
	} %>
	
	<h3><a href="#"><fmt:message key="jsp.submit.start-lookup-submission.manual-submission"/></a></h3>
	<div id="manual-accordion">&nbsp;</div>
	</div>
</div>

	<div id="tabs-result">
		<div id="empty-result">
			<p><fmt:message key="jsp.submit.start-lookup-submission.noresult"/></p>
		</div>
		<div id="result-list"></div>
		<div id="manual-submission">
			<fmt:message key="jsp.submit.start-lookup-submission.select.collection.label"/>
			<select id="select-collection-manual">
				<option value="-1"><fmt:message key="jsp.submit.start-lookup-submission.select.collection.defaultoption"/></option>
				<% for (Collection c : collections) { %>
				<option value="<%= c.getID() %>"><%= c.getName() %></option>
				<% }  %>
			</select>
			<button id="manual-submission-button" type="button"><fmt:message key="jsp.submit.start-lookup-submission.button.manual-submission"/> </button>
		</div>	
	</div>
</div>

		<div id="hidden-area" style="display: none;">
			<div id="select-collection-div">
				<select id="select-collection">
					<% for (Collection c : collections) { %>
					<option value="<%= c.getID() %>"><%= c.getName() %></option>
					<% }  %>
				</select>
			</div>
		</div>
<div id="no-collection-warn" title="<fmt:message key="jsp.submit.start-lookup-submission.no-collection-warn.title" />">
<p><fmt:message key="jsp.submit.start-lookup-submission.no-collection-warn.hint" /></p>
</div>
<div id="loading-search-result" title="<fmt:message key="jsp.submit.start-lookup-submission.search-loading.title" />">
<p><fmt:message key="jsp.submit.start-lookup-submission.search-loading.hint" /></p>
<center><img src="<%= request.getContextPath()  %>/sherpa/image/ajax-loader-big.gif"/></center>
</div>
      
<%  } else { %>
	<p class="submitFormWarn"><fmt:message key="jsp.submit.select-collection.none-authorized"/></p>
<%  } %>

	   <p><fmt:message key="jsp.general.goto"/><br />
	   <a href="<%= request.getContextPath() %>"><fmt:message key="jsp.general.home"/></a><br />
	   <a href="<%= request.getContextPath() %>/mydspace"><fmt:message key="jsp.general.mydspace" /></a>
	   </p>
	<script type="text/javascript"><!--
    	var j = jQuery.noConflict();
    	j("#tabs").tabs({
    		beforeActivate: function( event, ui ) {
    			if ('tabs-result' == j(ui.newPanel).attr('id'))
   				{
    				j('#manual-submission').appendTo(j(ui.newPanel)); 
    			}
    			else
   				{
    				j('#manual-submission').appendTo(j('#manual-accordion'));
   				}
    		}
    	});
    	j('#tabs-search-accordion').accordion({ 
    			beforeActivate: function( event, ui ) {
    				if ('manual-accordion' == ui.newPanel.attr('id'))
   					{
    					j('#manual-submission').appendTo(ui.newPanel);	
   					}
    			}
    	});
    	j('#link-ricerca-identificatore').click(function(){
    		j('#tabs-search-accordion').accordion({'active': 1});
    	});
    	j('button').button();
    	j('#manual-submission-button').click(function(event){
    		var colman = j('#select-collection-manual').val();
    		if (colman != -1)
    		{
    			j('#collectionid').val(colman);
    			j('#form-submission').submit();
    		}
    		else
   			{
    			j('#no-collection-warn').dialog("open");
   			}
    	});
    	j('#no-collection-warn').dialog({autoOpen: false,modal: true});
    	j('#lookup_idenfifiers').click(function(){
    		submissionLookupIdentifiers(j('input.submission-lookup-identifier'));
    	});
    	j('#search_go').click(function(){
    		submissionLookupSearch(j('.submission-lookup-search'));
    	});
    	j('button.exit').click(function(event){
    		event.preventDefault();
    		window.location = "<%= request.getContextPath() %>/mydspace";
    	});
    	j('#loading-search-result').dialog({
    		autoOpen: false,
    		modal: true,
    		onClose: function(){
    			j(this).data('ajaxCall').abort();
    		},
    		width: 600
    	});
    --></script>
	   
</dspace:layout>