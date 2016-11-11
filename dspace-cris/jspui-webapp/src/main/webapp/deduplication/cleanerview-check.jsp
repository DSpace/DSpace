<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@page contentType="text/html;charset=UTF-8" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@page import="org.dspace.content.Item"%>
<%@page import="org.dspace.content.Metadatum" %>
<%@ page import="java.util.Map"%>
<%@ page import="java.util.List"%>
<%@ page import="java.util.Date"%>
<%@ page import="java.text.SimpleDateFormat"%>
<%@ page import="org.dspace.app.webui.servlet.MyDSpaceServlet"%>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>
<%@ page import="org.dspace.app.webui.util.UIUtil"%>
<%@ page import="org.apache.commons.lang.StringUtils"%>

<%
    int REMOVE_ITEM_PAGE = MyDSpaceServlet.REMOVE_ITEM_PAGE;
	Map<Integer, Item> extraInfo = (Map<Integer, Item>) request.getAttribute("extraInfo");
	Map<Integer, String> itemTypeInfo = (Map<Integer, String>) request.getAttribute("itemTypeInfo");
	Map<String, Map<Integer, String[]>> grid = (Map<String, Map<Integer, String[]>>)request.getAttribute("grid");
	Map<String, List<String>> gridTwiceGroups = (Map<String, List<String>>)request.getAttribute("gridTwiceGroups");
	
	int start = (Integer)request.getAttribute("start");
	int rows = (Integer)request.getAttribute("rows");
	long count = (Long)request.getAttribute("count");
	int rule = (Integer)request.getAttribute("rule");
	int scopeDedup = (Integer)request.getAttribute("scope");
	String signatureType = (String)request.getAttribute("signatureType");
	Boolean mergeByUser = (Boolean)request.getAttribute("mergeByUser");
	String navbar = "admin";
	if(mergeByUser != null && mergeByUser) { 
	    navbar="off";
	}
%>

<dspace:layout style="submission" locbar="link" titlekey="jsp.dspace-admin.deduplication" navbar="<%=navbar%>">
	<c:set var="buttondeletemessage"><fmt:message key="jsp.tools.general.delete"/></c:set>
	
	<div class="modal fade" id="dialog-confirm-itemid" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
	  <div class="modal-dialog">
	    <div class="modal-content">
	      <div class="modal-header">
	        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
	        <h4 class="modal-title"><fmt:message key="jsp.tools.deduplicate.itemid.description"/></h4>
	      </div>
	      <div class="modal-body with-padding">
	      	<div class="clearfix">
	      		&nbsp;
	      	</div>
	        <p  class="message"><fmt:message key="jsp.tools.deduplication.itemsubmit.alert"/></p>
	      </div>
	      <div class="modal-footer">
	        <button type="button" class="btn btn-default" data-dismiss="modal" id="button-confirm"><fmt:message key="jsp.tools.deduplication.confirm.alert.ok"/></button>
	      </div>
	    </div>
	  </div>
	</div>
	<div class="modal fade" id="dialog-confirm-delete" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
	  <div class="modal-dialog">
	    <div class="modal-content">
	      <div class="modal-header">
	        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
	        <h4 class="modal-title"><fmt:message key="jsp.tools.deduplication.confirm.delete"/></h4>
	      </div>
	      <div class="modal-body with-padding">
	      	<div class="clearfix">
	      		&nbsp;
	      	</div>
	        <p class="message"><fmt:message key="jsp.tools.confirm-delete-item.info"/></p>
	      </div>
	      <div class="modal-footer">
	        <button type="button" class="btn btn-default" data-dismiss="modal"><fmt:message key="jsp.tools.deduplication.confirm.delete.close"/></button>
	        <button type="button" class="btn btn-primary" id="button-confirm-delete"><fmt:message key="jsp.tools.deduplication.confirm.delete.ok"/></button>
	      </div>
	    </div>
	  </div>
	</div>
	
	
	<script><!--
	j(document).ready(function() {
		
		j( "#dialog-confirm-itemid" ).modal({		
			show: false,
		});
		j( "#dialog-confirm-delete" ).modal({		
			show: false,
		});
		
		j.each(j('.btn-danger'), function(index, button){		
			var temp = button.value;
			button.value = '${buttondeletemessage}';
			j(button).data("item_id",temp);
		});
		
		j('#button-confirm-delete').click(function() {
			var request = j.ajax({
				  type: "POST",
				  url: "<%= request.getContextPath() %>/mydspace",
				  data: "item_id="+item_id+"&step=<%= REMOVE_ITEM_PAGE %>&submit_delete",
				  statusCode: {
					200: function() {
						j("#dedupButtonDelete_"+ item_id).hide();
						j("#dedupRow_" + item_id).addClass("strike");
				    }
				  }
	
				})
	
			request.fail(function(jqXHR, textStatus) {
				  alert( "Request failed: " + textStatus );
			});
	
			j("#dialog-confirm-delete").modal( "hide" );
		});
		
		j('.btn-danger').click(function() {
			item_id = j(this).data('item_id');
			j( "#dialog-confirm-delete" ).modal("show");	
		});
		
		j(".check").click(function(){
			var checkboxid = j(this).prop("id")
			var formName = checkboxid.replace("dedupcheckall","itemform");
			var el = document.getElementsByName(formName)[0].elements;

			for(var i=0; i < el.length; i++){
				if((el[i].type == 'checkbox') && (el[i].name == 'itemstomerge')){
					el[i].checked= j(this).prop("checked");
				}
			}
		});

		
	});
	

	function countChecked(gridSize, formName){
		if(gridSize==2){
			return true;
		}
		var count = 0;
		var el = document.getElementsByName(formName)[0].elements;
		for(var i=0; i < el.length; i++){
			if((el[i].type == 'checkbox') && (el[i].name == 'itemstomerge')){
				if(el[i].checked){
					count++;
					if(count>=2){
						return true;
					}
				}
			}
		}
		$("#dialog-confirm-itemid .message").text('<fmt:message key="jsp.dspace-admin.deduplication.groupchoice-alert" />');
		$("#dialog-confirm-itemid").modal("show");
		return false;
	}
	
	function toggleRadio(itemId, formName, elementCheckbox){
		var el = document.getElementsByName(formName)[0].elements;
		for(var i=0; i < el.length; i++){
			if((el[i].type == 'radio') && (el[i].id == ('target_'+itemId))){
				if(elementCheckbox.checked){
					el[i].disabled=false;
				} else {
					el[i].checked=false;
					el[i].disabled=true;
				}
				break;
			}
		}
	}

	--></script>
	<div class="row-fluid">
	    <div class="col-lg-12">      
	      <h1><fmt:message key="jsp.dspace-admin.deduplication.signature.${signatureType}"/></h1>   
	    </div>
	</div>
	<div class="row-fluid">
	    <div class="col-lg-12">      
			<p><fmt:message key="jsp.dspace-admin.deduplication.groupchoice-description" /></p>
	    </div>
	</div>
	<style>
	.strike {
		text-decoration: line-through;
	}
	</style>

	<div class="row-fluid">
		<div class="col-lg-12">
			<div class="form-inline">
				<div class="form-group">
					<form method="post" action="<%= request.getContextPath() %>/tools/duplicate" name="paginateformprevious">	
						<input type="hidden" name="rows" value="<%= rows %>"/>
						<input type="hidden" name="rule" value="${rule}"/>
						<input type="hidden" name="scope" value="<%= scopeDedup%>"/>
						<input type="hidden" name="signatureType" value="<%= signatureType%>"/>
						<input type="hidden" name="start" value="<%= (start==0?start:(start - rows)) %>"/> 
						<% if(start > 0) { %> <input  class="btn btn-default btn-sm" type="submit" name="submitcheck" value="<fmt:message key="jsp.tools.general.previous"/>" /> <% } %> 
					</form>
				</div>
				<c:if test="${count>0}">
					<div class="form-group">
						<fmt:message key="jsp.tools.general.header.information.founded"><fmt:param value="${count}"></fmt:param></fmt:message>
						<fmt:message key="jsp.tools.general.header.information.page"><fmt:param><%= rows<count?((((start * rows) + rows)/((int)count)) + 1):1 %></fmt:param><fmt:param><%= (((count%rows)==0)?(((int)(count/rows))):(int)(count/rows)+1) %></fmt:param></fmt:message>
					</div>
				</c:if>
				<div class="form-group">
					<form method="post" action="<%= request.getContextPath() %>/tools/duplicate" name="paginateformnext">	
						<input type="hidden" name="rows" value="<%= rows %>"/>
						<input type="hidden" name="rule" value="${rule}"/>
						<input type="hidden" name="scope" value="<%= scopeDedup%>"/>
						<input type="hidden" name="signatureType" value="<%= signatureType%>"/>
						<input type="hidden" name="start" value="<%= start + rows %>"/>
						<% if((rows+start) < count) { %>  <input  class="btn btn-default btn-sm"  type="submit"  name="submitcheck" value="<fmt:message key="jsp.tools.general.next"/>" /> <% } %> 
					</form>
				</div>
			</div>
		</div>
	</div>
	<br/>
	<div class="row-fluid">
		<div class="col-lg-12">
			<%
			int i = 0;
			for(String key : grid.keySet()) {
				String group = key;
				List<String> twiced = gridTwiceGroups.get(group);
			%>
			<div class="panel panel-default panel-success">
				<form class="form" method="post" onsubmit="return countChecked(<%= grid.get(key).size() %>, 'itemform_<%= i %>')" action="<%= request.getContextPath() %>/tools/duplicate" name="itemform_<%= i %>">
					<input type="hidden" name="mergeByUser" value="<%= mergeByUser%>"/>
					<%
						String formID="itemform_" + i;
					%>
					<div class="panel-heading">
						<h6 class="panel-title"><i class="fa-stack"></i> <%= group %> [<%= grid.get(key).size() %> item(s)]</h6>
						<% if(twiced!=null && !twiced.isEmpty()) { %>
							<fmt:message key="jsp.layout.submit.checkduplicate.match.othercriteria"/> 
							<% for(String twice : twiced) { %>
								<span><%= twice %></span>
							<% } %>
						<% } %> 
						
						<% if(grid.get(key).size()>1) {%>
							<input class="pull-right btn btn-xs btn-primary" type="submit" name="submittargetchoice" value='<fmt:message key="jsp.layout.submit.checkduplicate.compare"/>'/>
							<input class="btn btn-default pull-right btn btn-xs" type="submit" name="submitunrelatedall" value='<fmt:message key="jsp.layout.submit.checkduplicate.reject"/>'/>				
						<%} %>
						<% if(grid.get(key).size()>2){%>
						   <input type="checkbox" id="dedupcheckall_<%=i %>" class="pull-left check" />&nbsp;<fmt:message key="jsp.layout.submit.checkall"/>
						<%} %>
					</div>
					<table class="table table-striped table-bordered">
						<% for(int itemID : grid.get(key).keySet()) {
							Item item = extraInfo.get(itemID);
						 	pageContext.setAttribute("item", item);
						%>
						<tr id="dedupRow_<%= itemID%>">
							<% if(grid.get(key).size()<=2){%>
								<input type="hidden" name="itemstomerge" id="itemstomerge" value="<%= itemID%>"/>
							<%} else {%>
								<td width="37px"><input type="checkbox" name="itemstomerge" id="itemstomerge_<%= itemID%>" value="<%= itemID%>" onclick="toggleRadio(<%= itemID%>,'<%= formID %>',this);"/></td>
							<%} %>
							<td width="220px">
						 		<div><b><fmt:message key="jsp.layout.submit.checkduplicate.type"/></b> <%= item.getParentObject().getName() %></div>
						 		<div><b><fmt:message key="jsp.layout.submit.checkduplicate.itemID"/></b> <a href="<%= request.getContextPath() %>/tools/edit-item?item_id=<%= itemID %>" target="_blank"><%= itemID %></a></div>
								<%
							 	if(item.getHandle()!=null){
								%>
						 			<div><b><fmt:message key="jsp.layout.submit.checkduplicate.handle"/></b> <%= item.getHandle()%></div>
								<%
							 	}
								%>
								<div>
									<b><fmt:message key="jsp.layout.submit.checkduplicate.lastmodified" /></b>
						 			<%
						 			Date lastMod = item.getLastModified();
						 			if(lastMod.getYear()>0){
							 			SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd hh:mm");
							 			%>
							 			<%= dt1.format(lastMod) %>
						 			<%}%>
								</div>
						 		<div>
						 			<b><fmt:message key="jsp.layout.submit.checkduplicate.status"/></b>
									<c:set var="itemStatus"><%= UIUtil.getItemStatus(request, item) %></c:set>
									<% if (item.isWithdrawn()) { %>
										<fmt:message key="jsp.display-item.preview.status.withdrawn">
											<fmt:param><fmt:message key="jsp.display-item.preview.status.${itemStatus}"/></fmt:param>
										</fmt:message>
									<% } else { %>
										<fmt:message key="jsp.display-item.preview.status.${itemStatus}"/>
									<% } %>
								</div>
			  				</td>
					  		<td>
								<%= grid.get(key).get(itemID)[0] %>
							</td>
							<% if(mergeByUser==null || !mergeByUser) { %>
							<td width="100px">
								<button class="btn btn-danger btn-sm"  value="<%= itemID%>" type="button" id="dedupButtonDelete_<%= itemID %>" ><span class="fa fa-trash-o"> </span> ${buttondeletemessage}</button>
							</td>
							<% } %>
							<td width="50px" align="center"><input type="radio" name="target" id="target_<%= itemID%>" value="<%= itemID%>" 
							<%
								if((grid.get(key).size()<=2)){
									%>checked="true"<%
								}
							%>
							
							<% if(grid.get(key).size()>2){
									%>disabled="disabled"<%
								}
							%>
								/></td>
						</tr>
						<% } %>
					</table>
					<!--  input class="btn btn-sm btn-primary" type="submit" name="submittargetchoice" value='<fmt:message key="jsp.layout.submit.checkduplicate.merge"/>'/ -->
					<!--  input class="btn btn-sm btn-default" type="submit" name="submitunrelatedall" value='<fmt:message key="jsp.layout.submit.checkduplicate.reject"/>'/ -->
					<input type="hidden" name="rule" value="${rule}"/>
					<input type="hidden" name="scope" value="<%= scopeDedup%>"/>
					<input type="hidden" name="signatureType" value="<%= signatureType%>"/>
				</form>
			</div>
			<% 
			i++;
			}
			%>
			<div class="form-inline">
				<div class="form-group">
					<form method="post" action="<%= request.getContextPath() %>/tools/duplicate" name="paginateformprevious">	
						<input type="hidden" name="rows" value="<%= rows %>"/>
						<input type="hidden" name="rule" value="${rule}"/>
						<input type="hidden" name="scope" value="<%= scopeDedup%>"/>
						<input type="hidden" name="signatureType" value="<%= signatureType%>"/>
						<input type="hidden" name="start" value="<%= (start==0?start:(start - rows)) %>"/>  
						<% if(start > 0) { %> <input  class="btn btn-default btn-sm" type="submit" name="submitcheck" value="<fmt:message key="jsp.tools.general.previous"/>" /> <% } %> 
					</form>
				</div>
				<c:if test="${count>0}">
					<div class="form-group">
						<fmt:message key="jsp.tools.general.header.information.founded"><fmt:param value="${count}"></fmt:param></fmt:message>
						<fmt:message key="jsp.tools.general.header.information.page"><fmt:param><%= rows<count?((((start * rows) + rows)/((int)count)) + 1):1 %></fmt:param><fmt:param><%= (((count%rows)==0)?(((int)(count/rows))):(int)(count/rows)+1) %></fmt:param></fmt:message>
					</div>
				</c:if>
				<div class="form-group">
					<form method="post" action="<%= request.getContextPath() %>/tools/duplicate" name="paginateformnext">	
						<input type="hidden" name="rows" value="<%= rows %>"/>
						<input type="hidden" name="rule" value="${rule}"/>
						<input type="hidden" name="scope" value="<%= scopeDedup%>"/>
						<input type="hidden" name="signatureType" value="<%= signatureType%>"/>
						<input type="hidden" name="start" value="<%= start + rows %>"/>
						<% if((rows+start) < count) { %>  <input  class="btn btn-default btn-sm"  type="submit"  name="submitcheck" value="<fmt:message key="jsp.tools.general.next"/>" /> <% } %> 
					</form>
				</div>
			</div>
		</div>
	</div>
</dspace:layout>