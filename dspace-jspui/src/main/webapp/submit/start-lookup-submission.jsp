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
<%@ page import="org.dspace.app.util.CollectionUtils" %>
<%@ page import="org.dspace.app.util.CollectionsTree" %>
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
    CollectionsTree tree= CollectionUtils.getCollectionsTree(collections, false);

    //get collection id from the collection home
    int collection_id = (Integer) request.getAttribute("collection_id");
    
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
 
<%!
void generateCollectionTree(javax.servlet.jsp.JspWriter out, CollectionsTree tree ) 
		throws java.io.IOException {
	if(tree==null){
		return;
	}
	if (tree.getCurrent() != null)
	{
		out.print("<optgroup label=\""+tree.getCurrent().getName()+"\">");
	}
	if (tree.getCollections() != null){
		for (Collection col : tree.getCollections())
		{
			out.print("<option value=\""+col.getID()+"\">"+col.getName()+"</option>");	
		}
	}
	if (tree.getSubTree() != null)
	{
		for (CollectionsTree subTree: tree.getSubTree())
		{
			generateCollectionTree(out, subTree);
		}
	}
	if (tree.getCurrent() != null)
	{
		out.print("</optgroup>");
	}
}
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
	.img-thumbnail {height: 35px !important;}
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
    
<%  if (collections.length > 0)
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
	<ul>
		<li><a href="#tabs-search"><fmt:message key="jsp.submit.start-lookup-submission.tabs.search" /></a></li>
		<%		
	if ((searchProviders != null && searchProviders.size() > 0) || (identifiers != null && identifiers.size()>0)) {
	%>
		<li><a href="#tabs-result"><fmt:message key="jsp.submit.start-lookup-submission.tabs.result" /></a></li>
	<% } %>
	</ul>
	<div id="tabs-search">
	<!-- da qui -->
	<div class="panel-group" id="accordion">
	  <div class="panel panel-default">
	    <div class="panel-heading" data-toggle="collapse" data-parent="#accordion" href="#collapseOne">
	      <h4 class="panel-title">
	        <a>
	          <i span class="fa fa-chevron-down"></i> <fmt:message key="jsp.submit.start-lookup-submission.manual-submission"/>
	        </a>
	      </h4>
	    </div>
	    <div id="collapseOne" class="panel-collapse collapse in">
	      <div class="panel-body">
	      	<div id="manual-submission">
				<form class="form-horizontal" id="form-submission" action="" method="post">
					<div class="form-group">
						<label for="select-collection-manual" class="col-sm-2 control-label"><fmt:message key="jsp.submit.start-lookup-submission.select.collection.label"/></label>
						<div class="col-sm-7">
								<dspace:selectcollection klass="form-control" id="select-collection-manual" collection="<%= collection_id %>"/>
						</div>
						<button class="btn btn-success" id="manual-submission-button" type="button"><fmt:message key="jsp.submit.start-lookup-submission.button.manual-submission"/> </button>
					</div>
					<input type="hidden" id="iuuid" name="iuuid" value=""/>
					<input type="hidden" id="fuuid" name="fuuid" value=""/>
					<input type="hidden" id="suuid" name="suuid" value="<%= uuid %>"/>
					<input type="hidden" id="collectionid" name="collectionid" value=""/>
					<input type="hidden" id="iuuid_batch" name="iuuid_batch" value=""/>
					<input type="hidden" id="colid_batch" name="colid_batch" value=""/>
					<input type="hidden" id="filePath" name="filePath" value=""/>
					<input type="hidden" id="filename" name="filename" value=""/>					
				</form>
			</div>
	      </div>
	    </div>
	  </div>
<%		
	if (searchProviders != null && searchProviders.size() > 0) {
	%>
	  <div class="panel panel-default">	
	    <div class="panel-heading" data-toggle="collapse" data-parent="#accordion" href="#collapseTwo">
	      <h4 class="panel-title">
	        <a>
	          <i span class="fa fa-chevron-right"></i> <fmt:message key="jsp.submit.start-lookup-submission.search"/>
	        </a>
	      </h4>
	    </div>
	    <div id="collapseTwo" class="panel-collapse collapse">
	      <div class="panel-body">
	      	<form id="form-submission-search" action="" method="post">
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
		</form>
	</div>
	    </div>
	  </div>
<% } %>	

<% if (identifiers != null && identifiers.size()>0) {
	%>
			<div class="panel panel-default">
	    <div class="panel-heading" data-toggle="collapse" data-parent="#accordion" href="#collapseThree">
	      <h4 class="panel-title">
	        <a>
	          <i span class="fa fa-chevron-right"></i> <fmt:message key="jsp.submit.start-lookup-submission.identifiers"/>
	        </a>
	      </h4>
	    </div>
	    <div id="collapseThree" class="panel-collapse collapse">
	      <div class="panel-body">
		<form class="form-horizontal" id="form-submission-identifier" action="" method="post">
		<input type="hidden" id="suuid-identifier" name="suuid" value="<%= uuid %>"/>
		<input type="hidden" id="iuuid-identifier" name="iuuid" value=""/>
		<input type="hidden" id="fuuid-identifier" name="fuuid" value=""/>
		<input type="hidden" id="collectionid-identifier" name="collectionid" value=""/>
		<p class="help-block"><fmt:message key="jsp.submit.start-lookup-submission.identifiers.hints"/></p>
<%	
		for (String identifier : identifiers)
		{			
%>
<c:set var="identifier"><%= identifier %></c:set>
	<div class="form-group">
		<span class="col-md-3">
			<label for="identifier_<%= identifier%>"><span class="submission-lookup-label"><fmt:message key="jsp.submit.start-lookup-submission.identifier-${identifier}"/>:</span>
			</label>
		</span>
		<span class="col-md-9">		
<%	
			for (String provider : identifiers2providers.get(identifier))
			{			
%>
			<img class="img-thumbnail" src="<%= request.getContextPath() %>/image/submission-lookup-small-<%= provider %>.jpg" />
<% 
			}
%>
		</span>	 
		<span class="clearfix"></span>
		<span class="col-md-3 help-block submission-lookup-hint"><fmt:message key="jsp.submit.start-lookup-submission.identifier-${identifier}.hint"/></span></label>
		<div class="col-md-9">
		<input type="text" class="form-control  submission-lookup-identifier" name="identifier_<%= identifier%>" id="identifier_<%= identifier%>" />
	</div>
		</div><%
		} %>				
		<button class="btn btn-primary col-md-2 pull-right" type="button" id="lookup_idenfifiers"><fmt:message key="jsp.submit.start-lookup-submission.identifier.lookup"/></button>
	</form>
	</div>
</div>
</div>
<% 
		
	} %>
	<% if (fileLoaders != null && fileLoaders.size()>0) {
	%>
	  <div class="panel panel-default">
	    <div class="panel-heading" data-toggle="collapse" data-parent="#accordion" href="#collapseFour">
	      <h4 class="panel-title">
	        <a>
	          <i span class="fa fa-chevron-right"></i> <fmt:message key="jsp.submit.start-lookup-submission.byfile"/>
	        </a>
	      </h4>
	    </div>
	    <div id="collapseFour" class="panel-collapse collapse">
	      <div class="panel-body">
	<form class="form-horizontal" id="form-submission-loader" action="" method="post">
		<input type="hidden" id="suuid-loader" name="suuid" value="<%= uuid %>"/>
		<input type="hidden" id="iuuid-loader" name="iuuid" value=""/>
		<input type="hidden" id="fuuid-loader" name="fuuid" value=""/>
		<input type="hidden" id="collectionid-loader" name="collectionid" value=""/>
		
	<p class="help-block"><fmt:message key="jsp.submit.start-lookup-submission.byfile.hints"/></p>
	
	<div class="form-group">
			<label class="col-md-3" for="provider_loader"><fmt:message key="jsp.submit.start-lookup-submission.byfile.chooseprovider"/>:</label>
	<div class="col-md-6">
	<select class="form-control submission-file-loader" name="provider_loader" id="provider_loader">
			<option value="-1"><fmt:message key="jsp.submit.start-lookup-submission.select.fileformat.defaultoption"/></option>
	<%	
			for (String dataLoader : fileLoaders){			
				String fileLoaderKey = "jsp.submit.start-lookup-submission.select.fileformat." + dataLoader;
	%>
				<option value="<%= dataLoader %>"><fmt:message key="<%= fileLoaderKey %>"/></option>
	<% 
		}
	%>
	</select> 
	</div>
	</div>
	<div class="form-group">
			<label class="col-md-3" for="file_upload"><fmt:message key="jsp.submit.start-lookup-submission.byfile.file"/>:</label>
				<div class="col-md-6"> 
					<input class="submission-file-loader" type="file" name="file_upload" id="file_upload" />
				</div>
		</div>
		<div class="form-group">
		    <div class="col-md-offset-3 col-md-6">
		      <div class="checkbox">
		        <label>
		          <input class="submission-file-loader submission-preview-loader" type="checkbox" name="preview_loader" id="preview_loader" value="<%= Boolean.TRUE%>"/><fmt:message key="jsp.submit.start-lookup-submission.byfile.filepreview"/>
		        </label>
			</div>
	</div>
  	</div>
  
	<div class="form-group" id="select-collection-file-div">
				<label class="col-md-3" for="select-collection-file"><fmt:message key="jsp.submit.start-lookup-submission.byfile.filecollection"/>:</label>
				<div class="col-md-6">
                                  <dspace:selectcollection klass="form-control submission-file-loader" name="select-collection-file" id="select-collection-file" collection="<%= collection_id %>"/>
				</div>
	</div>
		<button class="btn btn-primary col-md-2 pull-right" type="button" id="loadfile_go"><fmt:message key="jsp.submit.start-lookup-submission.byfile.process"/></button>
		</form>
	</div>
	    </div>
	  </div>
  </div>
<% 
		
	} %>
</div>
	<%		
	if ((searchProviders != null && searchProviders.size() > 0) || (identifiers != null && identifiers.size()>0)) {
	%>
 	<div id="tabs-result">
		<div id="empty-result">
			<p class="alert alert-warning"><fmt:message key="jsp.submit.start-lookup-submission.noresult"/></p>
			<div id="no_result_manual_submission"></div>
		</div>
		<div id="result-form">
			<form class="form-horizontal" id="form-submission-identifiers" action="" method="post">
				<div class="form-group">
					<label for="select-collection-manual" class="col-sm-2 control-label"><fmt:message key="jsp.submit.start-lookup-submission.select.collection.label"/></label>
					<div class="col-sm-7">
							<dspace:selectcollection klass="form-control" id="select-collection-identifier" collection="<%= collection_id %>"/>
					</div>
					<button class="btn btn-success" id="identifier-submission-button" type="button"><fmt:message key="jsp.submit.general.submit"/> </button>
					</div>
				<input type="hidden" id="iuuid" name="iuuid" value=""/>
				<input type="hidden" id="fuuid" name="fuuid" value=""/>
				<input type="hidden" id="suuid" name="suuid" value="<%= uuid %>"/>
				<input type="hidden" id="collectionid" name="collectionid" value=""/>
				<input type="hidden" id="iuuid_batch" name="iuuid_batch" value=""/>
				<input type="hidden" id="filePath" name="filePath" value=""/>
			</form>
			<input type="checkbox" id="checkallresults" name="checkallresults"><fmt:message key="jsp.submit.start-lookup-submission.js.checkallresults"/>
			<h4 id="no-record" style="display:none"><span class="label label-warning"></span><fmt:message key="jsp.submit.start-lookup-submission.norecordselected" /></span></h4>
			<h4 id="no-collection" style="display:none"><span class="label label-warning"><fmt:message key="jsp.submit.start-lookup-submission.nocollectionselected" /></span></h4>
		</div>
		<div id="result-list"></div>
	</div>
<% 
		
	} %>
		<div class="clearFix">&nbsp;</div>
        <button type="button" class="btn btn-default col-md-2 pull-right exit"><fmt:message key="jsp.submit.start-lookup-submission.exit"/></button>
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
      <div class="modal-body with-padding">
       		<p class="alert alert-warning"><fmt:message key="jsp.submit.start-lookup-submission.no-collection-warn.hint" /></p>
      </div>
      <div class="modal-footer">
      		<button type="button" class="btn btn-default" data-dismiss="modal"><fmt:message key="jsp.submit.start-lookup-submission.no-collection.dialog.return" /></button>
      </div>
    </div><!-- /.modal-content -->
  </div><!-- /.modal-dialog -->
</div><!-- /.modal -->

<div id="no-record-warn" class="modal">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
        <h4 class="modal-title"><fmt:message key="jsp.submit.start-lookup-submission.no-record-warn.title" /></h4>
      </div>
      <div class="modal-body with-padding">
       		<p class="alert alert-warning"><fmt:message key="jsp.submit.start-lookup-submission.no-record-warn.hint" /></p>
      </div>
      <div class="modal-footer">
      		<button type="button" class="btn btn-default" data-dismiss="modal"><fmt:message key="jsp.submit.start-lookup-submission.no-record.dialog.return" /></button>
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
      <div class="modal-body with-padding">
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
      <div class="modal-body with-padding">
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
      <div class="modal-body with-padding">
       		
      </div>
      <div class="modal-footer">
        
      </div>
    </div><!-- /.modal-content -->
  </div><!-- /.modal-dialog -->
</div><!-- /.modal -->
      
<div id="error-file-result" class="modal">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
        <h4 class="modal-title"><fmt:message key="jsp.submit.start-lookup-submission.error.title" /></h4>
      </div>
      <div class="modal-body with-padding">
       		<p class="help-block"><fmt:message key="jsp.submit.start-lookup-submission.error.hint" /></p>
       		<p id="error-file-exception" style="display:none;"></p>
      </div>
    </div><!-- /.modal-content -->
  </div><!-- /.modal-dialog -->
</div><!-- /.modal -->
      
<%  } else { %>
	<p class="submitFormWarn"><fmt:message key="jsp.submit.select-collection.none-authorized"/></p>
<%  } %>
	<script type="text/javascript"><!--
    	j("#tabs").tabs({
    		beforeActivate: function( event, ui ) {
    			 j("li.active").toggleClass("active");
    		},
    		create: function( event, ui ) {
            j("div.ui-tabs").toggleClass("ui-tabs ui-widget ui-widget-content ui-corner-all tabbable");
            j("ul.ui-tabs-nav").toggleClass("ui-tabs-nav ui-helper-reset ui-helper-clearfix ui-widget-header ui-corner-all nav nav-tabs");
            j("li.ui-tabs-active").toggleClass("ui-state-default ui-corner-top ui-tabs-active ui-state-active active");
            j("li.ui-state-default").toggleClass("ui-state-default ui-corner-top");
            j("div.ui-tabs-panel").toggleClass("ui-tabs-panel ui-widget-content ui-corner-bottom tab-content with-padding");
            },
        activate: function( event, ui ) {
            j("li.ui-tabs-active").toggleClass("ui-tabs-active ui-state-active active");
            if ('tabs-result' == ui.newPanel.attr('id'))
   				{
				j('#manual-submission>form').appendTo("#no_result_manual_submission");	
    		}
            else{
            	j('#no_result_manual_submission>form').appendTo("#manual-submission");	
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
    	
    	j('#accordion').on('show.bs.collapse', function(a) {
    		j('i.fa-chevron-down').toggleClass('fa-chevron-down').toggleClass('fa-chevron-right');
    		j(a.target).prev().find('a>i').toggleClass('fa-chevron-down').toggleClass('fa-chevron-right');
    		//j('#accordion div.panel div.panel-heading h4.panel-title a i').toggleClass('fa-chevron-down');
    	});
    	
    --></script>
	   
</dspace:layout>
