<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://displaytag.sf.net" prefix="display"%>

<c:set var="dspace.layout.head.last" scope="request">
	<script type="text/javascript" src="<%=request.getContextPath()%>/js/jquery.dataTables.min.js"></script>
	<link href="<%= request.getContextPath() %>/css/jquery.dataTables.css" rel="stylesheet" type="text/css">
    <link href="<%= request.getContextPath() %>/css/relationManagement.css" rel="stylesheet" type="text/css">
    <script type="text/javascript">
    	function rebuildSelectList()
    	{
    		j('div.relation_management_selected.placeholder').remove();
    		var i = j('div.relation_management_selected').length;
    		for (var idx = 1; idx + i <= 10 || idx <= 2; idx++)
   			{
    			var divPlaceholder = j('<div class="relation_management_selected placeholder">');
    			divPlaceholder.append('<div class="relation_management_selected_count">'+(idx + i) +'</div>');
    			divPlaceholder.append('<div class="relation_management_selected_desc">&nbsp;</div>');
    			j('#relation_management_selectedDiv').append(divPlaceholder);
   			}
    		j('.relation_management_selected_count').each(function(index) {
    			j(this).text(index+1);
    		});
    		j( "#relation_management_selectedDiv" ).sortable({
            	items: "div.relation_management_selected:not(.placeholder)"
            });
            j( "div.relation_management_selected:not(.placeholder)" ).each(function(index){
            	j(this).mouseover(function(){
            		j(this).find('div.relation_management_selected_count').addClass('showDragHandle');
            		j(this).addClass("sortableCursor");
            	});
            	j(this).mouseleave(function(){
            		j(this).find('div.relation_management_selected_count').removeClass('showDragHandle');
            		j(this).removeClass("sortableCursor");
            	});
            });
    	}
    	
    	var j = jQuery;
	    j(document).ready(function() {
	            
	            	//,${ishideEnabled},${isUnlinkEnabled});
	    		j('#submit_save').attr('disabled', 'disabled');
	            j('#savemessage').hide();
	            searchDataTable = j('#relation_management_searchTable').dataTable( {
	                "bProcessing": true,
	                "bServerSide": true,
	                "sAjaxSource": '${relationType}.json',
	                "sAjaxDataProp": "relatedObjects",
	                "fnRowCallback": function( nRow, aData, iDisplayIndex ) {
                        /* bind the row object to the tr element */
                        j(nRow).data('row', aData);
                    },
	                "aoColumns": [
	                              { "mData": "uuid" },
	                              { "mData": "relationPreference" },
	                              <c:forEach items="${columns}" var="col" varStatus="status">
	                              { "mData": "descriptionColumns.${status.count -1}" },
	                              </c:forEach>
	                              { "mData": "relationPreference" }],
                    "aoColumnDefs": [
	                              {
	                                  "mRender": function ( data, type, row ) {
	                                	  var changed = j('#change-'+row.uuid).length>0;
	                                	  var switchVar = data;
	                                	  if (changed)
	                                	  {
	                                		  var input = j('#change-'+row.uuid).find('input');
	                                		  if (input.length > 0)
	                                		  {
	                                			  if (input.attr('id') == 'toHide')
	                                			  {
	                                				  switchVar = 'hided';  	
	                                			  }
	                                			  else if (input.attr('id') == 'toUnLink')
	                                			  {
	                                				  switchVar = 'unlinked';
	                                			  }
	                                			  else
												  {
	                                				  switchVar = null;
	                                			  }
	                                		  }
	                                		  else
	                                		  {
	                                			  switchVar = 'selected';
	                                		  }
	                                	  }
	                                	  if (switchVar == null)
										  {
												var out = "";
	                                		  	if (${isSelectEnabled})
												{
													out += '<a href="#" id="selected_'+data+'_'+row.uuid+'" class="action"><fmt:message key="jsp.layout.cris.relationmanagement.${confName}.status.selected"/></a>';
												}
												if (${isHideEnabled})
												{
													out += '&nbsp;<a href="#" id="hided_'+data+'_'+row.uuid+'" class="action"><fmt:message key="jsp.layout.cris.relationmanagement.${confName}.status.hided"/></a>';
												}
												if (${isUnlinkEnabled})
												{
													out += '&nbsp;<a href="#" id="unlinked_'+data+'_'+row.uuid+'" class="action"><fmt:message key="jsp.layout.cris.relationmanagement.${confName}.status.unlinked"/>';
												}
												return out;
											}
											else if (switchVar == 'selected')
											{
												var out = '<a href="#" id="null_'+data+'_'+row.uuid+'" class="action"><fmt:message key="jsp.layout.cris.relationmanagement.${confName}.status.normal"/></a>';
												if (${isHideEnabled})
												{
													out += '&nbsp;<a href="#" id="hided_'+data+'_'+row.uuid+'" class="action"><fmt:message key="jsp.layout.cris.relationmanagement.${confName}.status.hided"/></a>';
												}
												if (${isUnlinkEnabled})
												{
													out += '&nbsp;<a href="#" id="unlinked_'+data+'_'+row.uuid+'" class="action"><fmt:message key="jsp.layout.cris.relationmanagement.${confName}.status.unlinked"/>';
												}
												return out;
											}
											else if (switchVar == 'hided')
											{
												var out = '<a href="#" id="null_'+data+'_'+row.uuid+'" class="action"><fmt:message key="jsp.layout.cris.relationmanagement.${confName}.status.normal"/></a>';
												if (${isSelectEnabled})
												{
													out += '&nbsp;<a href="#" id="selected_'+data+'_'+row.uuid+'" class="action"><fmt:message key="jsp.layout.cris.relationmanagement.${confName}.status.selected"/></a>';
												}
												if (${isUnlinkEnabled})
												{
													out += '&nbsp;<a href="#" id="unlinked_'+data+'_'+row.uuid+'" class="action"><fmt:message key="jsp.layout.cris.relationmanagement.${confName}.status.unlinked"/>';
												}
												return out;
											}
											else if (switchVar == 'unlinked')
											{
												var out = '<a href="#" id="null_'+data+'_'+row.uuid+'" class="action"><fmt:message key="jsp.layout.cris.relationmanagement.${confName}.status.normal"/></a>';
												if (${isSelectEnabled})
												{
													out += '&nbsp;<a href="#" id="selected_'+data+'_'+row.uuid+'" class="action"><fmt:message key="jsp.layout.cris.relationmanagement.${confName}.status.selected"/></a>';
												}
												if (${isHideEnabled})
												{
													out += '&nbsp;<a href="#" id="hided_'+data+'_'+row.uuid+'" class="action"><fmt:message key="jsp.layout.cris.relationmanagement.${confName}.status.hided"/></a>';
												}
												return out;
											}
		                                	return data;
	                                  },
	                                  "aTargets": [ ${fn:length(columns)+2} ]
	                              },
	                              {
	                                  "mRender": function ( data, type, row ) {
										var out ="";
	                                	if (data == null)
										{
											out += '<fmt:message key="jsp.layout.cris.relationmanagement.${confName}.status.normal"/>';
										}
										else if (data == 'selected')
										{
											out += '<fmt:message key="jsp.layout.cris.relationmanagement.${confName}.status.selected"/>';
										}
										else if (data == 'hided')
										{
											out += '<fmt:message key="jsp.layout.cris.relationmanagement.${confName}.status.hided"/>';
										}
										else if (data == 'unlinked')
										{
											out += '<fmt:message key="jsp.layout.cris.relationmanagement.${confName}.status.unlinked"/>';
										}
										else
										{
											out = data;		
										}
	                                	if (j('#change-'+row.uuid).length>0)
										{
											out += "*";		
										}
	                                	return out;
	                                  },
	                                  "aTargets": [ 1 ]
	                              },	                              
	                              { "bVisible": false,  "aTargets": [ 0 ] },
	                              { "bSortable": false,  "aTargets": [ 0, 1
	                              <c:forEach items="${columns}" var="col" varStatus="status">
	                                   <c:if test="${not col.sortable}">, ${1+status.count}</c:if>                                
	                              </c:forEach>                                     
	                              , ${fn:length(columns) + 2}] }]
	            });
	            j('#relation_management_searchTable').on('click', 'a.action', function(event){
	            	event.preventDefault();
		            var row = j(this).parent().parent().data('row');
	            	var actionId = j(this).attr('id');
		            var actionType = actionId.split('_')[0];
		            var currentStatus = actionId.split('_')[1];
		            var uuid = actionId.split('_')[2];
		            j('#change-'+uuid).remove();
		            var oldSelect = j('#select-'+uuid);
		            if (oldSelect.length)
	            	{
		            	oldSelect.remove();
		            	rebuildSelectList();
	            	}
		            var change = j('<div id="change-'+uuid+'" class="changelog">');
		            var changeS = j('<div class="change-status '+actionType+'">');
		            var changeD = j('<div class="change-description '+actionType+'">');
		            var changeSText= "";
		            var inputId = null;
		            if (actionType == 'selected')
		            {
		            	change = j('<div id="select-reorder" class="changelog">');
						changeSText = 'S';
						inputId = 'orderedSelected';
			            var desc = row.descriptionColumns[0];
			            for (var i = 1; i < row.descriptionColumns.length; i++)
		            	{
			        		desc += '<br>'+row.descriptionColumns[i];
		            	}
		            	var selectChange = j('<div id="select-'+uuid+'" class="relation_management_selected">');
		            	selectChange.html('<div class="relation_management_selected_count">&nbsp;</div>'+'<div class="relation_management_selected_desc">'+desc+'</div><input id="'+inputId+'" name="'+inputId+'" type="hidden" value="'+uuid+'">');
		            	j('#relation_management_selectedDiv').append(selectChange);
		            	rebuildSelectList();
		            	if (j('div.relation_management_selected').length > 1 && j('#select-reorder').length == 0)
	            		{
		            		changeS.html('O');
		            		changeD.append(j('<span>').html('Selected list re-ordered'));
				            change.append(changeS).append(changeD);
				            j('#relation_management_changesLog_queue').append(change);    
	            		}
		            	change = j('<div id="change-'+uuid+'" class="changelog">');
			            changeS = j('<div class="change-status '+actionType+'">');
			            changeD = j('<div class="change-description '+actionType+'">');
		            }
		            if (actionType != ''+row.relationPreference)
		            {
			            if (actionType == 'null')
						{
							changeSText = 'A';
							inputId = 'toActivate';
						}
						else if (actionType == 'hided')
						{
							changeSText = 'H';
							inputId = 'toHide';
						}
						else if (actionType == 'unlinked')
						{
							changeSText = 'U';
							inputId = 'toUnLink';
						}
			            changeS.html(changeSText);
			            var desc = row.descriptionColumns[0];
			            for (var i = 1; i < row.descriptionColumns.length; i++)
		            	{
			        		desc += '<br>'+row.descriptionColumns[i];
		            	}
			            changeD.append(j('<span>').html(desc));
			            change.append(changeS).append(changeD);
			            if (inputId != 'orderedSelected')
			            {
			            	change.append(j('<input id="'+inputId+'" name="'+inputId+'" type="hidden">').val(uuid));	
			            }
		            	j('#relation_management_changesLog_queue').append(change);
		            	j('#submit_save').removeAttr('disabled');
		            	j('#savemessage').show();
		            	j('#disabledsavemessage').hide();
		            }
		            else
	            	{
		            	if (j('div.changelog').length == 0)
		            	{
		            		j('#submit_save').attr('disabled', 'disabled');
				            j('#savemessage').hide();
				            j('#disabledsavemessage').show();
		            	}
	            	}
		            searchDataTable.fnDraw(true);
		        });
	            j( "#relation_management_selectedDiv" ).sortable({
	            	items: "div.relation_management_selected:not(.placeholder)",
	            	update: function( event, ui ) {
		            	if (j('div.relation_management_selected').length > 1 && j('#select-reorder').length == 0)
	            		{
		            		var change = j('<div id="select-reorder" class="changelog">');
				            var changeS = j('<div class="change-status selected">');
				            var changeD = j('<div class="change-description selected">');
		            		changeS.html('O');
		            		changeD.append(j('<span>').html('Selected list re-ordered'));
				            change.append(changeS).append(changeD);
				            j('#relation_management_changesLog_queue').append(change);
				            j('#submit_save').removeAttr('disabled');
				            j('#savemessage').show();
				            j('#disabledsavemessage').hide();
	            		}
	            		rebuildSelectList();
	            	}
	            });
	            j( "div.relation_management_selected:not(.placeholder)" ).each(function(index){
	            	j(this).mouseover(function(){
	            		j(this).find('div.relation_management_selected_count').toggleClass('showDragHandle');
	            	});
	            	j(this).mouseleave(function(){
	            		j(this).find('div.relation_management_selected_count').toggleClass('showDragHandle');
	            	});
	            });
	    });
    </script>
</c:set>

<dspace:layout style="submission" titlekey="jsp.layout.cris.relationmanagement.title.${confName}">

<div id="content">
	<h1><fmt:message key="jsp.layout.cris.relationmanagement.title.${confName}"/></h1>
	<p><fmt:message key="jsp.layout.cris.relationmanagement.description.${confName}"/></p>
	<form action="" method="post">
	<div id="relation_management_ria">
	<div id="relation_management_searchDiv">
		<h3><fmt:message key="jsp.layout.cris.relationmanagement.all.${confName}" /></h3>
		<table id="relation_management_searchTable">
			<thead>
				<th><fmt:message key="jsp.layout.cris.relationmanagement.table.uuid" /></th>
				<th><fmt:message key="jsp.layout.cris.relationmanagement.table.relationPreference" /></th>
				<c:forEach items="${columns}" var="col" varStatus="status">
				<th><fmt:message key="jsp.layout.cris.relationmanagement.table.${confName}.${col.name}" /></th>
				</c:forEach>
				<th><fmt:message key="jsp.layout.cris.relationmanagement.table.actions" /></th>
				</thead>
			<tbody>
				<tr>
					<td colspan="${fn:length(columns) + 1}" class="dataTables_empty"><fmt:message key="jsp.layout.cris.relationmanagement.loading" /></td>
				</tr>
			</tbody>
		</table>
	</div>
	<c:if test="${isSelectEnabled}">
	<div id="relation_management_selectedDiv" class="${confName}">
		<h3><fmt:message key="jsp.layout.cris.relationmanagement.selected.${confName}" /></h3>
		<c:forEach items="${selected}" var="obj" varStatus="status">
		<div id="select-${obj.uuid}" class="relation_management_selected">
			<div class="relation_management_selected_count">
				${status.count}
			</div>
			<div class="relation_management_selected_desc">
			<input type="hidden" name="orderedSelected" id="orderedSelected" value="${obj.uuid}" />
			<c:forEach items="${obj.descriptionColumns}" var="col" varStatus="colStatus">
			<span class="relation_management_selectedList ${confName} column${colStatus.count}">${col}</span><br/>
			</c:forEach>
			</div>
		</div>
		</c:forEach>
		<c:if test="${fn:length(selected) >= 0}">
			<c:forEach var="i" begin="${fn:length(selected)+1}" end="10">
			<div class="relation_management_selected placeholder">
				<div class="relation_management_selected_count">
					${i}
				</div>
				<div class="relation_management_selected_desc">&nbsp;
				</div>
			</div>	
			</c:forEach>
		</c:if>
		<c:if test="${fn:length(selected) > 10}">
			<c:forEach var="i" begin="1" end="2">
			<div class="relation_management_selected placeholder">
				<div class="relation_management_selected_count">
					${fn:length(selected) + i}
				</div>
				<div class="relation_management_selected_desc">&nbsp;
				</div>
			</div>	
			</c:forEach>
		</c:if>
	</div>
	</c:if>
	</div>
	<div id="relation_management_changesLog">
		<h3 id="relation_management_changesLog_title"><fmt:message key="jsp.layout.cris.relationmanagement.changelog" /></h3>
		<div id="relation_management_changesLog_queue">&nbsp;</div>
		<br/>
		<div id="buttons">
			<span id="savemessage" class="alert alert-danger"><fmt:message key="jsp.layout.cris.relationmanagement.save.msg" /></span>
			<span id="disabledsavemessage" class="alert alert-info"><fmt:message key="jsp.layout.cris.relationmanagement.save.disabled" /></span>
			<input type="submit" class="btn btn-primary" name="submit_save" id="submit_save" value="<fmt:message key="jsp.layout.cris.relationmanagement.save" />" />
			<input type="submit" class="btn btn-default" name="submit_exit" id="submit_exit" value="<fmt:message key="jsp.layout.cris.relationmanagement.exit" />" />
		</div>
	</div>
	</form>
</div> 	
</dspace:layout>
