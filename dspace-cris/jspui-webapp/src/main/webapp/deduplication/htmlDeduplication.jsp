<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>

<%--create further template --%>

<%
	String[] dedupMyDSpaceColumn = UIUtil.getUITableColumn("deduplication","dedup");
	int indexColTmp = 0;
%>
	<script type="text/javascript"><!--
    
		DUPLICATION_COL = [ 
    	<% for(String col : dedupMyDSpaceColumn) { 
    	    String rendering = UIUtil.getUITableRenderingColumn("deduplication","dedup", col);
    	    indexColTmp++;
    	%>{
    	"mData" : "<%= col %>"
    	 <% if(rendering!=null) { %>,    	 
    	"mRender": function ( data, type, full ) {
      	  	return <%= rendering%>(data, type, full);
        }<% } %>}
        <% if(indexColTmp<dedupMyDSpaceColumn.length) {%>,<% } %>
        <% } %>
    ];
   	-->
   	</script>   
<!-- html deduplication -->
<% if (ConfigurationManager.getBooleanProperty("deduplication", "deduplication.submission.feature",false)) {%>

<div class="modal fade" id="duplicateboxignoremsg" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
        <h4 class="modal-title" id="duplicateboxignoremsgtitle"><fmt:message key="jsp.duplicate.foundbox.ignoremsg.title"/></h4>        
      </div>
      <div class="modal-body with-padding">
        <p id="duplicateboxignoremsgbody"><fmt:message key="jsp.duplicate.foundbox.ignoremsg.body"/></p>
        <textarea class="form-control" id="duplicateboxignoremsgTextarea" rows="4"></textarea>
        <div class="alert alert-danger hidden" role="alert">
          <fmt:message key="jsp.duplicate.popup.validator.notemptyormaxlength"/>
        </div>
      </div>
      <div class="modal-footer">
        <button id="ignoreduppopup" type="button" class="btn btn-primary" data-dismiss="modal"><fmt:message key="jsp.duplicate.button.ignore"/></button>
        <button id="ignoreduppopupcancel" type="button" class="btn btn-default" data-dismiss="modal"><fmt:message key="jsp.duplicate.button.cancel"/></button>
      </div>
    </div>
  </div>
</div>

<div class="modal fade" id="duplicatebox" role="dialog" aria-labelledby="largeModal" data-target="#duplicatebox" aria-hidden="true">
  <div class="modal-dialog modal-lg">
    <div class="modal-content">
      <div class="modal-header">
        <p class="modal-title" id="duplicateboxtitle"><fmt:message key="jsp.duplicate.foundbox.info"/></p>
      </div>
      <div class="modal-body with-padding">
       <div class="table-responsive">
		<table id="duplicateboxtable" class="table table-striped table-bordered table-condensed">
			<thead>
				<tr>
		<%
		for (int idx = 0; idx < dedupMyDSpaceColumn.length; idx++)
		{ %><c:set var="column"><%=dedupMyDSpaceColumn[idx]%></c:set>
			<th><fmt:message key="jsp.itemrow.column.${column}"/></th>
		<% } %>
				</tr>
			</thead>
			<tbody>
		            <tr>
					<td class="text-center"><fmt:message key="jsp.duplicate.description.loading" /></td>
		            </tr>
			</tbody>
			
		</table>
		</div>
		</div>		
		 <div class="modal-footer">
			<% if(si.isInWorkflow() || si.isEditing()) { %>
				<form id="form-submission-itemid_list" name="formItemIdList" role="form" method="post" action="<%= request.getContextPath() %>/tools/duplicate" class="form-horizontal form-bordered">
					<input type="hidden" value="0" name="scope" />
					<input type="hidden" value="submitcheck" name="submitcheck" />					
					<input type="hidden" value="" name="itemid_list" id="itemid_list" />
					<input type="hidden" value="true" name="mergeByUser" id="mergeByUser" />
				</form>
				
		 		<button id="mergepopup" class="btn btn-primary" data-dismiss="modal" type="button"><fmt:message key="jsp.dedup.table.actions.merge"/></button>
    		<% } %>
        	<button id="cancelpopup" class="btn btn-default" data-dismiss="modal" type="button"><fmt:message key="jsp.duplicate.button.cancel"/></button>
      	</div>
      </div>
    </div>
  </div>
<% } %>
