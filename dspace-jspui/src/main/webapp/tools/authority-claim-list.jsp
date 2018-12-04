<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@ page language="java" pageEncoding="UTF-8" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.content.DSpaceObject" %>
<%@ page import="org.dspace.content.Metadatum" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>
<%@page import="org.dspace.discovery.configuration.*"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@page import="org.apache.lucene.search.spell.JaroWinklerDistance"%> 

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%
	double checksimilarity = Double.parseDouble((String)request.getAttribute("checksimilarity"));
	Map<String, Map<String, List<String[]>>> result = (Map<String, Map<String, List<String[]>>>) request.getAttribute("result");
	Map<String, Map<String, Boolean>> haveSimilar = (Map<String, Map<String, Boolean>>)request.getAttribute("haveSimilar");
	Map<String, DSpaceObject> mapItem = (Map<String, DSpaceObject>)request.getAttribute("items");
	String crisID = (String)request.getAttribute("crisID");
	Map<String,DiscoveryViewConfiguration> mapViewMetadata = (Map<String,DiscoveryViewConfiguration>) request.getAttribute("viewMetadata");
	String selectorViewMetadata = (String)request.getAttribute("selectorViewMetadata");
	JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance();	
%>
<c:set var="selectallbutton"><fmt:message key="jsp.dspace.authority-list.selectallbutton" /></c:set>
<c:set var="deselectallbutton"><fmt:message key="jsp.dspace.authority-list.deselectallbutton" /></c:set>
<dspace:layout titlekey="jsp.dspace.authority-listclaim.title">
<style>
.list-group-item {
	border: none;
}

.dt-buttons {
	float: right !important;
}
</style>
<h3><fmt:message key="jsp.dspace.authority-listclaim.info-heading" /></h3>

<fmt:message key="jsp.dspace.authority-listclaim.info" />

<form method="post">

<table id="mytable" class="table table-bordered table-condensed display">
<thead>
	<tr>
		<th>&nbsp;</th>
		<th>&nbsp;</th>
	</tr>
</thead>
<tbody>
<%
    for (String handlekey : result.keySet())
        {

            Map<String, List<String[]>> subresult = (Map<String, List<String[]>>) result
                    .get(handlekey);
            Map<String, Boolean> subHaveSimilar = (Map<String, Boolean>) haveSimilar
                    .get(handlekey);

            DSpaceObject item = mapItem.get(handlekey);
%>  

<tr>
	<td id="checkbox_data_<%= item.getID() %>"><div id="data_<%= item.getID() %>" class="data" data-identifier="<%= item.getID() %>">&nbsp;</div></td>
	<td>
		<p style="display:none" id="foundyourauthority_<%= item.getID() %>" class="text-warning"><fmt:message key="jsp.authority-claim.found.your.authority"/></p>
		<p style="display:none" id="founddifferentauthority_<%= item.getID() %>" class="text-danger"><fmt:message key="jsp.authority-claim.found.different.authority"/></p>
		<p style="display:none" id="foundrequestforclaim_<%= item.getID() %>" class="text-warning"><fmt:message key="jsp.authority-claim-list.found.local.message"/></p>
		<dspace:discovery-artifact style="global" artifact="<%= item %>" view="<%= mapViewMetadata.get(\"publications\") %>" selectorCssView="<%=selectorViewMetadata %>"/>
		<ul class="nav nav-tabs" role="tablist" id="ul<%= item.getID() %>">
		<%
		    // Keep a count of the number of values of each element+qualifier
		    // key is "element" or "element_qualifier" (String)
		    // values are Integers - number of values that element/qualifier so far
		    Map<String, Integer> dcCounter = new HashMap<String, Integer>();
		    
		    int i = 0;
		    String alreadyactive = null;
		    for (String key : subresult.keySet())
		    {
		        String labelTab = "jsp.dspace.authority-claim-" + key;
		        String keyID = item.getID() + "_" + key;
		        boolean active = false;
				//check if there are similarity before to build content 
				int preCountSimilarity = 0;
				for(String[] record : subresult.get(key)) { 
			        String value = record[0];
			        String authority = record[1];
			        String confidence = record[2];
			        String language = record[3];
			        String similar = record[4];
				 	   if(StringUtils.isNotBlank(value) && StringUtils.isNotBlank(similar)) {
						    if(value.equals(similar) || jaroWinklerDistance.getDistance(value,similar)>checksimilarity || value.startsWith(similar) || similar.startsWith(value)) {
						        preCountSimilarity++;
						    }
				 	   }
			    }
		        if(preCountSimilarity>1) {
			        active = true;
			        alreadyactive = keyID; 
		        }
		%>
		
				  <li id="li_<%= keyID %>" class="nav-item  <%= active?"active":""%>" >
				    <a class="nav-link" id="<%= keyID %>-tab" data-toggle="tab" href="#<%= keyID %>" role="tab" aria-controls="<%= keyID %>" <%= active?"aria-selected=\"true\"":""%>><fmt:message key="<%= labelTab %>" /></a>
				  </li>
		  
		<%
			i++;
		    } %>
		</ul>
			
		<div class="tab-content" id="myTabContent<%= item.getID() %>" >
		  
		<%    
		i = 0;
		int countPanelHide = 0;
		for (String key : subresult.keySet())
		{
		    boolean active = false;
		    String keyID = item.getID() + "_" + key;
		    if(alreadyactive!=null) {
		        if(alreadyactive.equals(keyID)) {
		            active = true;
		        }
		    }
		%>
		
		  <div class="tab-pane <%= active?"active":""%>" id="<%= keyID %>" role="tabpanel" aria-labelledby="<%= keyID %>-tab">
		    <div class="row">
		      <div class="col-md-12">
		<%	
			boolean hiddenSelectCheckbox = false;

			//check if the local.message.claim exist... if exist show only a warning and remove checkbox selection
			Metadatum[] requestPendings = item.getMetadataByMetadataString("local.message.claim");
			for(Metadatum requestPending : requestPendings) {
			    String vvv = requestPending.value;
				if(StringUtils.isNotBlank(vvv)) {
				    if(vvv.contains(crisID) && vvv.contains(key)) { 
				    	hiddenSelectCheckbox = true;
				    	break;
				    }
				}
			}
			
			//check if there are similarity before to build content 
			int preCountSimilarity = 0;
			for(String[] record : subresult.get(key)) { 
		        String value = record[0];
		        String authority = record[1];
		        String confidence = record[2];
		        String language = record[3];
		        String similar = record[4];
			 	   if(StringUtils.isNotBlank(value) && StringUtils.isNotBlank(similar)) {
					    if(value.equals(similar) || jaroWinklerDistance.getDistance(value,similar)>checksimilarity || value.startsWith(similar) || similar.startsWith(value)) {
					        preCountSimilarity++;
					    }
			 	   }
		    }
			if(preCountSimilarity==0) {
			    countPanelHide++;
		%>
				 <script type="text/javascript">
					jQuery("#<%= keyID %>").hide();
					jQuery("#li_<%= keyID %>").hide();					
				</script>
		<%  }
			else if(hiddenSelectCheckbox) {
		%>
				<script type="text/javascript">
					jQuery("#checkbox_data_<%= item.getID() %>").addClass("hidden-select-checkbox");
					jQuery("#<%= keyID %>").toggle();
					jQuery("#li_<%= keyID %>").toggle();
					jQuery("#foundrequestforclaim_<%= item.getID() %>").toggle();
				</script>
		<%  } else { %>			      
		      	<div class="col-md-5">
		<%      
				int countSimilar = 0;
				int countSimilarWithAuthority = 0;
				boolean showFoundYourAuthority = false;
				boolean showFoundDifferentAuthority = false;
				for(String[] record : subresult.get(key)) { 
			        Integer count = dcCounter.get(keyID);
			        if (count == null)
			        {
			            count = new Integer(0);
			        }
			        
			        // Increment counter in map
			        dcCounter.put(keyID, new Integer(count.intValue() + 1));
			
			        // We will use two digits to represent the counter number in the parameter names.
			        // This means a string sort can be used to put things in the correct order even
			        // if there are >= 10 values for a particular element/qualifier.  Increase this to
			        // 3 digits if there are ever >= 100 for a single element/qualifer! :)
			        String sequenceNumber = count.toString();
			        
			        while (sequenceNumber.length() < 2)
			        {
			            sequenceNumber = "0" + sequenceNumber;
			        }
			        
			        String fieldNameIdx = "value_" + keyID + "_" + sequenceNumber;
			        String languageIdx = "language_" + keyID + "_" + sequenceNumber;
			        String authorityName = "choice_" + keyID + "_authority_" + sequenceNumber;
			        String confidenceName = "choice_" + keyID + "_confidence_" + sequenceNumber;
			        String option = sequenceNumber + "_" + keyID;
			        
			        String value = record[0];
			        String authority = record[1];
			        String confidence = record[2];
			        String language = record[3];
			        String similar = record[4];
				 %>
				<% 	if(crisID.equals(authority)) {
				    	countSimilar++;
				    	if(confidence.equals("600")) {
			    			showFoundYourAuthority = true;
				    	}
				 	} else {
				 	   if(StringUtils.isNotBlank(value) && StringUtils.isNotBlank(similar)) {
						    if(value.equals(similar) || jaroWinklerDistance.getDistance(value,similar)>checksimilarity || value.startsWith(similar) || similar.startsWith(value)) {
						        countSimilar++;						        
								if(StringUtils.isNotBlank(authority) && confidence.equals("600")) {
						    		showFoundDifferentAuthority = true;
						    		countSimilarWithAuthority++;
							    }
						    }
						}
				   	}
				%>
		    	<input type="hidden" id="<%= fieldNameIdx%>" name="<%= fieldNameIdx%>" value="<%= value %>"/>
		        <input type="hidden" id="<%= languageIdx %>" name="<%= languageIdx %>" value="<%= (language == null ? "" : language.trim()) %>"/>
		        <input type="hidden" id="<%= authorityName %>" name="<%= authorityName %>" value="<%= authority==null?"":authority %>"/>
		        <input type="hidden" id="<%= confidenceName %>" name="<%= confidenceName %>" value="<%= confidence %>"/>
				
			<% } %>

				<% if(showFoundYourAuthority) { %>
					<script type="text/javascript">
						jQuery("#foundyourauthority_<%= item.getID() %>").toggle();
						jQuery("#checkbox_data_<%= item.getID() %>").addClass("hidden-select-checkbox");
						jQuery("#myTabContent<%= item.getID() %>").toggle();
						jQuery("#ul<%= item.getID() %>").toggle();
					</script>					
				<% } else if(showFoundDifferentAuthority && !showFoundYourAuthority && countSimilar==1) { %>
					<script type="text/javascript">
						jQuery("#founddifferentauthority_<%= item.getID() %>").toggle();
					</script>						    
				<%						    
				   } 
				%>
							
			<label for="userchoice_<%= keyID %>"> <fmt:message key="jsp.authority-claim.choice.fromdropdown"/></label>
			<select class="form-check-input" name="userchoice_<%= keyID %>" id="userchoice_<%= keyID %>">
					<%        
				Integer subCount = new Integer(0);
				for(String[] record : subresult.get(key)) { 
			        
			        String sequenceNumber = subCount.toString();
			        
			        while (sequenceNumber.length() < 2)
			        {
			            sequenceNumber = "0" + sequenceNumber;
			        }
			        
			        String option = sequenceNumber + "_" + keyID;
			        
			        String value = record[0];
			        String similar = record[4];
				 %>
		          <option value="<%= option %>"
		          <%if(StringUtils.isNotBlank(value) && StringUtils.isNotBlank(similar)) {
							    if(value.equals(similar) || jaroWinklerDistance.getDistance(similar,value)>checksimilarity || value.startsWith(similar) || similar.startsWith(value)) {
							        %>
							        <%= "selected" %>
							        <%    		
							    }
						    }
		          %>>
		            <%= value %>
		          </option>
			<% 
					subCount++;
				} %>
			</select>
				</div>
				<div class="col-md-7">
					  <div class="form-group">
					    <label for="requestNote_<%= keyID %>"><fmt:message key="jsp.authority-claim.label.requestnote"/></label>
					    <textarea class="form-control" name="requestNote_<%= keyID %>" id="requestNote_<%= keyID %>" rows="3" cols="100"></textarea>
					  </div>
				</div>

		<%      
				if(countSimilar==1 || ((countSimilar-countSimilarWithAuthority)==1)) {
				    countPanelHide++;
		%>
					<script type="text/javascript">
						jQuery("#<%= keyID %>").hide();
						jQuery("#li_<%= keyID %>").hide();
					</script>	
		<% 
				}
		%>
		<% 
			
			}
			
		%>	
				</div>
		    </div>
		  </div>
		
		<%	
			i++;
		}
		%>
		</div>

		<input type="hidden" name="handle_<%= item.getID() %>" value="<%= handlekey %>"/>
	</td>    
	</tr>		
	<%
	
	if(i==countPanelHide) {
	%>
		<script type="text/javascript">
			jQuery("#myTabContent<%= item.getID() %>").toggle();
		</script>	
	<%   
	}
	}
	%>
</tbody>
<tfoot>
</tfoot>
</table>
	<div class="row col-md-12 pull-right">
        <input class="btn btn-primary pull-right col-md-3" type="submit" id="submit_approve" name="submit_approve" value="<fmt:message key="jsp.authority.listclaim.approve"/>" />
        <input class="btn btn-warning pull-right col-md-3" type="submit" id="submit_reject" name="submit_reject" value="<fmt:message key="jsp.authority.listclaim.reject"/>" />
		<input class="btn btn-default pull-right col-md-3" type="submit" name="submit_cancel" value="<fmt:message key="jsp.authority.listclaim.cancel"/>" />
	</div>	
<script type="text/javascript">
j(document).ready(function() {
	
    var table = j('#mytable').DataTable( {
		dom: 'Blfrtip',
        buttons: [
            'selectAll',
            'selectNone'
        ],
        language: {
            buttons: {
                selectAll: "${selectallbutton}",
                selectNone: "${deselectallbutton}"
            }
        },
        columnDefs: [ 
        {
            orderable: false,
            className: 'select-checkbox',
            targets:   0
        }
        ],
        select: {
            style:    'os',
            selector: 'td:first-child'
        },
        order: [[ 1, 'asc' ]]
    } );
    
    j(".hidden-select-checkbox").removeClass();
    
	j( "form input[type=submit]" ).click(function( event ) {
		var buttonPressed = this.getAttribute("name");
		if((buttonPressed=='submit_approve' || buttonPressed=='submit_reject')) {
			
			var successExit = false;			
			table.rows('.selected').data().each( function ( i ) {
					var parentCss = j("#checkbox_" + j(i[0]).attr('id')).attr('class');
					if(parentCss.includes("select-checkbox")) { 
						successExit = true;					
						var htmlid = j(i[0]).attr("data-identifier");
						j('<input>').attr({
						    type: 'hidden',
						    id: 'selectedId' + htmlid,
						    name: 'selectedId',
						    value: htmlid
						}).appendTo('form');
						return false;
					}
            } );
			if(!successExit) {
				$('#authority-claim-no-selected').modal('show');
				return false;
			}
		}

	});
	
});
</script>
</form>

	    <div class="modal fade" id="authority-claim-no-selected" tabindex="-1" role="dialog" aria-labelledby="<fmt:message key="jsp.authority.listclaim.no-selected"/>" aria-hidden="true">
				<div class="modal-dialog">
					<div class="modal-content">
						<div class="modal-header">
							<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
							<h4 class="modal-title"><fmt:message key="jsp.authority.listclaim.no-selected"/></h4>
						</div>
						<div class="modal-body with-padding">
							<p><fmt:message key="jsp.authority.listclaim.no-selected-body"/></p>
						</div>
						<div class="modal-footer">							 
							 <button type="button" class="btn btn-default" data-dismiss="modal"><fmt:message key="jsp.authority.listclaim.no-selected-body-dismissmodal"/></button>
						</div>
					</div>
					<!-- /.modal-content -->
				</div>
				<!-- /.modal-dialog -->
		</div>
</dspace:layout>
