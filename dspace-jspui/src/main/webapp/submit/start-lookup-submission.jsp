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
<%@ page import="java.lang.Boolean" %>
<%@ page import="java.util.*" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core"
    prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>
	
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    String contextPath = "/dspace-jspui";
	request.setAttribute("LanguageSwitch", "hide");

    //get collections to choose from
    List<Collection> collections =
        (List<Collection>) request.getAttribute("collections");

    //get collection id from the collection home
	Object collection_id_object = request.getAttribute("collection_id");

	String collection_id;

	if(collection_id_object instanceof UUID){
		UUID uuid = (UUID) collection_id_object;
		collection_id = uuid.toString();
	}
	else {
		collection_id = (String) collection_id_object;
	}

    //check if we need to display the "no collection selected" error
    Boolean noCollection = (Boolean) request.getAttribute("no.collection");
    Boolean nosuuid = (Boolean) request.getAttribute("nouuid");
    Boolean expired = (Boolean) request.getAttribute("expired");
    
    Map<String, List<String>> identifiers2providers = (Map<String, List<String>>) request.getAttribute("identifiers2providers");
    List<String> searchProviders = (List<String>) request.getAttribute("searchProviders");
    List<String> fileLoaders = (List<String>) request.getAttribute("fileLoaders");
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
	.invalid-value {border: 1px solid #FF6600;}
	</style>	
	<script type='text/javascript'>var dspaceContextPath = "<%=request.getContextPath()%>";</script>		
</c:set>
<c:set var="dspace.layout.head.last" scope="request">		
	<script type="text/javascript" src="<%= request.getContextPath() %>/static/js/submission-lookup.js"></script>
</c:set>

<dspace:layout style="submission" locbar="off"
               navbar="off"
               titlekey="jsp.submit.start-lookup-submission.title"
               nocache="true">

    <h1><fmt:message key="jsp.submit.start-lookup-submission.heading"/></h1>
    <div id="jserrormessage" style="display: none"><fmt:message key="jsp.submit.start-lookup-submission.js.errormessage"/></div>
    <div id="jsseedetailsbuttonmessage" style="display: none"><fmt:message key="jsp.submit.start-lookup-submission.js.detailsbuttonmessage"/></div>
    <div id="jsfilldatabuttonmessage" style="display: none"><fmt:message key="jsp.submit.start-lookup-submission.js.filldataandstartbuttonmessage"/></div>
    
<%  if (collections.size() > 0)
    {
		//if no collection was selected, display an error
		if((noCollection != null) && (noCollection.booleanValue()==true))
		{
%>
                <div class="alert alert-warning">
					<p><fmt:message key="jsp.submit.start-lookup-submission.no-collection"/></p>
				</div>
<%
		}
		//if no collection was selected, display an error
		if((nosuuid != null) && (nosuuid.booleanValue()==true))
		{
%>
                <div class="alert alert-warning">
					<p><fmt:message key="jsp.submit.start-lookup-submission.nosuuid"/></p>
				</div>
<%
		}
		//if no collection was selected, display an error
		if((expired != null) && (expired.booleanValue()==true))
		{
%>
                <div class="alert alert-warning">
					<p><fmt:message key="jsp.submit.start-lookup-submission.expired"/></p>
				</div>
<%
		}
%>            

<div id="tabs">
	<ul class="nav nav-tabs">
		<li class="active"><a href="#tabs-search"><fmt:message key="jsp.submit.start-lookup-submission.tabs.search" /></a></li>
		<li><a href="#tabs-result"><fmt:message key="jsp.submit.start-lookup-submission.tabs.result" /></a></li>
	</ul>
	<div class="tab-content">
	<div class="tab-pane" id="tabs-search">

		
	<div id="tabs-search-accordion">
	<h3><a href="#"><fmt:message key="jsp.submit.start-lookup-submission.manual-submission"/></a></h3>
		<div id="manual-accordion">&nbsp;</div>
<%		
	if (searchProviders != null && searchProviders.size() > 0) {
	%>
		<h3><a href="#"><fmt:message key="jsp.submit.start-lookup-submission.search"/></a></h3>
		<div id="search-accordion">
		<form class="form-horizontal" id="form-submission-search" action="" method="post">
		<input type="hidden" id="suuid-search" name="suuid" value="<%= uuid %>"/>
		<input type="hidden" id="iuuid-search" name="iuuid" value=""/>
		<input type="hidden" id="fuuid-search" name="fuuid" value=""/>
		<input type="hidden" id="collectionid-search" name="collectionid" value=""/>
<%	
		for (String provider : searchProviders)
		{			
%>
		<img class="img-thumbnail" src="<%= request.getContextPath() %>/image/submission-lookup-small-<%= provider %>.jpg" />
<% 
		}
	%>
		<p class="help-block"><fmt:message key="jsp.submit.start-lookup-submission.search.hints"/></p>
		<div class="form-group">
			<label for="search_title"><fmt:message key="jsp.submit.start-lookup-submission.search.title"/>:</label> 
			<textarea class="form-control submission-lookup-search" name="search_title" id="search_title" cols="50" row="4"></textarea>
		</div>
		<div class="form-group">
			<label for="search_year"><fmt:message key="jsp.submit.start-lookup-submission.search.year"/>:</label> 
			<input class="form-control submission-lookup-search" type="text" size="7" name="search_year" id="search_year" />
		</div>
		
		<div class="form-group">
			<label for="search_authors"><fmt:message key="jsp.submit.start-lookup-submission.search.authors"/>:</label> 
			<textarea class="form-control submission-lookup-search" name="search_authors" id="search_authors"cols="50" row="4"></textarea>
		</div>
		
		<div class="row">			
			<button type="button" class="btn btn-primary col-md-2 pull-right" id="search_go"><fmt:message key="jsp.submit.start-lookup-submission.search-go"/></button>
		</div>
		</form>
	</div>
<% } %>	
	

	<h3><a href="#"><fmt:message key="jsp.submit.start-lookup-submission.identifiers"/></a></h3>
	<div id="identifier-accordion">
		<form class="form-horizontal" id="form-submission-identifier" action="" method="post">
		<input type="hidden" id="suuid-identifier" name="suuid" value="<%= uuid %>"/>
		<input type="hidden" id="iuuid-identifier" name="iuuid" value=""/>
		<input type="hidden" id="fuuid-identifier" name="fuuid" value=""/>
		<input type="hidden" id="collectionid-identifier" name="collectionid" value=""/>
<% if (identifiers != null && identifiers.size()>0) {
	%>
		
		<p class="help-block"><fmt:message key="jsp.submit.start-lookup-submission.identifiers.hints"/></p>
<%	
		for (String identifier : identifiers)
		{			
%>
<c:set var="identifier"><%= identifier %></c:set>
	<div class="form-group">
		<label class="col-md-3" for="identifier_<%= identifier%>"><span class="submission-lookup-label"><fmt:message key="jsp.submit.start-lookup-submission.identifier-${identifier}"/>:</span> 
		<span class="help-block submission-lookup-hint"><fmt:message key="jsp.submit.start-lookup-submission.identifier-${identifier}.hint"/></span></label>
		<div class="col-md-9">
		<div class="col-md-4">
		<input class="form-control  submission-lookup-identifier" type="text" name="identifier_<%= identifier%>" id="identifier_<%= identifier%>" />
		</div>
		<div class="col-md-7">
<%	
			for (String provider : identifiers2providers.get(identifier))
			{			
%>
		
			<img class="img-thumbnail" src="<%= request.getContextPath() %>/image/submission-lookup-small-<%= provider %>.jpg" />
		
<% 
			}
%></div></div></div><%
		} %>				
	<div class="row">	
		&nbsp;<button class="btn btn-primary col-md-2 pull-right" type="button" id="lookup_idenfifiers"><fmt:message key="jsp.submit.start-lookup-submission.identifier.lookup"/></button>
	</div>
	</form>
	</div>
<% 
		
	} %>

	<% if (fileLoaders != null && fileLoaders.size()>0) {
	%>
	<h3><a href="#"><fmt:message key="jsp.submit.start-lookup-submission.byfile"/></a></h3>
	<div id="file-accordion" class="container">	
	<form class="form-horizontal" id="form-submission-loader" action="" method="post">
		<input type="hidden" id="suuid-loader" name="suuid" value="<%= uuid %>"/>
		<input type="hidden" id="iuuid-loader" name="iuuid" value=""/>
		<input type="hidden" id="fuuid-loader" name="fuuid" value=""/>
		<input type="hidden" id="collectionid-loader" name="collectionid" value=""/>
	<p class="help-block"><fmt:message key="jsp.submit.start-lookup-submission.byfile.hints"/></p>
	
	<div class="form-group">
	<label class="col-md-3" for="provider_loader"><span class="submission-lookup-label"><fmt:message key="jsp.submit.start-lookup-submission.byfile.chooseprovider"/>:</span></label>
	<div class="col-md-6">
	<select class="form-control submission-file-loader" name="provider_loader" id="provider_loader">
	<option value="-1"><fmt:message key="jsp.submit.start-lookup-submission.select.collection.defaultoption"/></option>
	<%	
	for (String dataLoader : fileLoaders)
		{			
	%>
				<option value="<%= dataLoader %>"><%= dataLoader %></option>
	<% 
		}
	%>
	</select> 
	</div>
	</div>
	<div class="form-group">
			<label class="col-md-3" for="file_upload"><fmt:message key="jsp.submit.start-lookup-submission.byfile.file"/>:</label>
			<div class="col-md-7"> 
			<input class="form-control submission-file-loader" type="file" name="file_upload" id="file_upload" />
			</div>
	</div>
	
	<div class="container checkbox">
      <input class="submission-file-loader submission-preview-loader" type="checkbox" name="preview_loader" id="preview_loader" value="<%= Boolean.TRUE%>"/><span class="help-block"><fmt:message key="jsp.submit.start-lookup-submission.byfile.filepreview"/></span>
  	</div>
  
	<div class="form-group" id="select-collection-file-div">
				<label class="col-md-3" for="select-collection-file"><fmt:message key="jsp.submit.start-lookup-submission.byfile.filecollection"/>:</label>
				<div class="col-md-6">
                                  <dspace:selectcollection klass="form-control submission-file-loader" name="select-collection-file" id="select-collection-file" collection="<%= collection_id %>"/>
				</div>
				<button class="btn btn-primary col-md-2 pull-right" type="button" id="loadfile_go"><fmt:message key="jsp.submit.start-lookup-submission.byfile.process"/></button>
	</div>
		</form>
	</div>
<% 
		
	} %>

	</div>

</div>

<div class="tab-pane" id="tabs-result">
		<div id="empty-result">
			<p class="alert alert-warning"><fmt:message key="jsp.submit.start-lookup-submission.noresult"/></p>
		</div>
		<div id="result-list"></div>
		<div id="manual-submission">
			<div class="form-group">
			<div class="col-md-3">
			<label for="select-collection-manual"><fmt:message key="jsp.submit.start-lookup-submission.select.collection.label"/></label>
			</div>
			<div class="col-md-7">
                          <dspace:selectcollection klass="form-control" id="select-collection-manual" collection="<%= collection_id %>"/>
			</div>
			</div>
			<form class="form-horizontal" id="form-submission" action="" method="post">
			<input type="hidden" id="iuuid" name="iuuid" value=""/>
			<input type="hidden" id="fuuid" name="fuuid" value=""/>
			<input type="hidden" id="suuid" name="suuid" value="<%= uuid %>"/>
			<input type="hidden" id="collectionid" name="collectionid" value=""/>
			<div class="btn-group">
				<button class="btn btn-success col-md-offset-5" id="manual-submission-button" type="button"><fmt:message key="jsp.submit.start-lookup-submission.button.manual-submission"/> </button>
			</div>
			</form>
		</div>	
	</div>
	<div class="row container">
        <button type="button" class="btn btn-default col-md-2 pull-right exit"><fmt:message key="jsp.submit.start-lookup-submission.exit"/></button>
    </div>
</div>
</div>
		<div id="hidden-area" style="display: none;">
			<div id="select-collection-div">
                          <dspace:selectcollection klass="form-control" id="select-collection" collection="<%= collection_id %>"/>
			</div>
		</div>
		

<div id="no-collection-warn" class="modal">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
        <h4 class="modal-title"><fmt:message key="jsp.submit.start-lookup-submission.no-collection-warn.title" /></h4>
      </div>
      <div class="modal-body">
       		<p class="alert alert-warning"><fmt:message key="jsp.submit.start-lookup-submission.no-collection-warn.hint" /></p>
      </div>
      <div class="modal-footer">
      		<button type="button" class="btn btn-default" data-dismiss="modal"><fmt:message key="jsp.submit.start-lookup-submission.no-collection.dialog.return" /></button>
      </div>
    </div><!-- /.modal-content -->
  </div><!-- /.modal-dialog -->
</div><!-- /.modal -->

<div id="loading-search-result" class="modal">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
        <h4 class="modal-title"><fmt:message key="jsp.submit.start-lookup-submission.search-loading.title" /></h4>
      </div>
      <div class="modal-body">
       		<p class="help-block"><fmt:message key="jsp.submit.start-lookup-submission.search-loading.hint" /></p>
      </div>
      <div class="modal-footer">
        <img src="<%= request.getContextPath()  %>/sherpa/image/ajax-loader-big.gif"/>
      </div>
    </div><!-- /.modal-content -->
  </div><!-- /.modal-dialog -->
</div><!-- /.modal -->

<div id="loading-file-result" class="modal">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
        <h4 class="modal-title"><fmt:message key="jsp.submit.start-lookup-submission.search-loading.title" /></h4>
      </div>
      <div class="modal-body">
       		<p class="help-block"><fmt:message key="jsp.submit.start-lookup-submission.search-loading.hint" /></p>
      </div>
      <div class="modal-footer">
        <img src="<%= request.getContextPath()  %>/sherpa/image/ajax-loader-big.gif"/>
      </div>
    </div><!-- /.modal-content -->
  </div><!-- /.modal-dialog -->
</div><!-- /.modal -->

<div id="loading-details" class="modal">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
        <h4 class="modal-title"><fmt:message key="jsp.submit.start-lookup-submission.js.titlepopupmessage" /></h4>
      </div>
      <div class="modal-body">
       		
      </div>
      <div class="modal-footer">
        
      </div>
    </div><!-- /.modal-content -->
  </div><!-- /.modal-dialog -->
</div><!-- /.modal -->
      
<%  } else { %>
	<p class="submitFormWarn"><fmt:message key="jsp.submit.select-collection.none-authorized"/></p>
<%  } %>
	<br/>
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
    			heightStyle: "content",
    			collapsible: true,
    			active: false,
    			beforeActivate: function( event, ui ) {
    				if ('manual-accordion' == ui.newPanel.attr('id'))
   					{
    					j('#manual-submission').appendTo(ui.newPanel);	
   					}
    			}
    	});
    	j('#link-ricerca-identificatore').click(function(){
    		j('#tabs-search-accordion').accordion({'active': 2});
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
    			j('#no-collection-warn').modal('show');
   			}
    	});
    	j('#lookup_idenfifiers').click(function(){
    		submissionLookupIdentifiers(j('input.submission-lookup-identifier'));
    	});
    	j('#search_go').click(function(){
    		submissionLookupSearch(j('.submission-lookup-search'));
    	});
    	j('#loadfile_go').click(function(){
    		j('#select-collection').val(j('#select-collection-file').val());
    		submissionLookupFile(j('#form-submission-loader'));
    	});
    	j('button.exit').click(function(event){
    		event.preventDefault();
    		window.location = "<%= request.getContextPath() %>/mydspace";
    	});
    	j('#loading-search-result').on('hide.bs.modal', function () {
    		j(this).data('ajaxCall').abort();
    	});
    	j('#loading-details').on('hidden.bs.modal', function () {
  			 j('#hidden-area').append(j('#select-collection-div'));
  			 j('#loading-details .modal-body').empty();
  			 j('#loading-details .modal-footer').empty();
   		});
    	j(".submission-preview-loader").click(function() {
    		if(j(this).is (':checked')) {
    			j("#select-collection-file-div").hide();
    		}
    		else {
    			j("#select-collection-file-div").show();
    		}
    	});
    	j('#tabs-search-accordion').accordion({'active': 0});
    --></script>
	   
</dspace:layout>
