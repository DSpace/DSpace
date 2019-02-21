<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@ page language="java" pageEncoding="UTF-8" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="org.dspace.content.Metadatum" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>
<%@page import="org.dspace.discovery.configuration.*"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@page import="org.apache.lucene.search.spell.JaroWinklerDistance"%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%
	
	double checksimilarity = Double.parseDouble((String)request.getAttribute("checksimilarity"));
    Map<String, List<String[]>> result = (Map<String, List<String[]>>) request.getAttribute("result");
	String handle = (String)request.getAttribute("handle");
	Item item = (Item)request.getAttribute("item");
	String crisID = (String)request.getAttribute("crisID");
	Map<String, Boolean> haveSimilar = (Map<String, Boolean>)request.getAttribute("haveSimilar");
	Map<String,DiscoveryViewConfiguration> mapViewMetadata = (Map<String,DiscoveryViewConfiguration>) request.getAttribute("viewMetadata");
	String selectorViewMetadata = (String)request.getAttribute("selectorViewMetadata");
	JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance();
%>


<dspace:layout titlekey="jsp.dspace.authority-claim.title">

<h3><fmt:message key="jsp.dspace.authority-claim.info-heading" /></h3>

<fmt:message key="jsp.dspace.authority-claim.info" />
<ul>
	<fmt:message key="jsp.dspace.authority-claim.info.case1" />
	<fmt:message key="jsp.dspace.authority-claim.info.case2" />
	<fmt:message key="jsp.dspace.authority-claim.info.case3" />
</ul>

<form method="post">

<p style="display:none" id="foundyourauthority_<%= item.getID() %>" class="text-warning"><fmt:message key="jsp.authority-claim.found.your.authority"/></p>
<p style="display:none" id="founddifferentauthority_<%= item.getID() %>" class="text-danger"><fmt:message key="jsp.authority-claim.found.different.authority"/></p>
<p style="display:none" id="foundrequestforclaim_<%= item.getID() %>" class="text-warning"><fmt:message key="jsp.authority-claim.found.local.message"/></p>
<dspace:discovery-artifact style="global" artifact="<%= item %>" view="<%= mapViewMetadata.get(\"publications\") %>" selectorCssView="<%=selectorViewMetadata %>"/>

<div id="wrapperMyTabContent<%= item.getID() %>" >
<ul class="nav nav-tabs" role="tablist" id="ul<%= item.getID() %>">
<%
    // Keep a count of the number of values of each element+qualifier
    // key is "element" or "element_qualifier" (String)
    // values are Integers - number of values that element/qualifier so far
    Map<String, Integer> dcCounter = new HashMap<String, Integer>();
    

	int i = 0;
	for (String key : result.keySet())
	{
	    String labelTab = "jsp.dspace.authority-claim-" + key;
	    String keyID = item.getID() + "_" + key;	
	
%>

		  <li id="li_<%= keyID %>" class="nav-item  <%= i==0?"active":""%>" >
		    <a class="nav-link" id="<%= keyID %>-tab" data-toggle="tab" href="#<%= keyID %>" role="tab" aria-controls="<%= keyID %>" <%= i==0?"aria-selected=\"true\"":""%>><fmt:message key="<%= labelTab %>" /></a>
		  </li>
  
<%
	i++;
    } %>
</ul>


<div class="tab-content" id="myTabContent<%= item.getID() %>" >
<% 

i = 0;
boolean localMessageFound = false;

for (String key : result.keySet())
{	
    String keyID = item.getID() + "_" + key;
%>

  <div class="tab-pane <%= i==0?"active":""%>" id="<%= keyID %>" role="tabpanel" aria-labelledby="<%= keyID %>-tab">
    <div class="row">
      <div class="col-md-12">
      
		<%	

		//check if the local.message.claim exist... if exist show only a warning
		Metadatum[] requestPendings = item.getMetadataByMetadataString("local.message.claim");
		for(Metadatum requestPending : requestPendings) {
		    String vvv = requestPending.value;
			if(StringUtils.isNotBlank(vvv)) {
			    if(vvv.contains(crisID) && vvv.contains(key)) { 
			        localMessageFound = true;
			    	break;
			    }
			}
		}
		
		if(localMessageFound) {
		%>
				<script type="text/javascript">
					jQuery("#foundrequestforclaim_<%= item.getID() %>").show();
					jQuery("#wrapperMyTabContent<%= item.getID() %>").hide();
				</script>
					
		<% } %>			      
		
		<div class="col-md-5">
		<%
		int countSimilar = 0;
	    boolean showFoundYourAuthorityLowConfidence = false;		
		boolean showFoundYourAuthority = false;
		boolean showFoundDifferentAuthority = false;
		for(String[] record : result.get(key)) { 
	        
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
					</script>					
				<% } else if(showFoundDifferentAuthority && !showFoundYourAuthority && countSimilar==1) { %>
					<script type="text/javascript">
						jQuery("#founddifferentauthority_<%= item.getID() %>").toggle();
					</script>						    
				<%						    
				   } 
				%>
				
				
        	<label for="userchoice_<%= keyID %>"><fmt:message key="jsp.authority-claim.choice.fromdropdown"/></label>
			<select class="form-check-input" name="userchoice_<%= keyID %>" id="userchoice_<%= keyID %>">
					<%
					Integer subCount = new Integer(0);
					for(String[] record : result.get(key)) { 
				
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
      </div>
    </div>
  </div>
<% 
	i++;
} %>


        <input type="hidden" name="handle_<%= item.getID() %>" value="<%= handle %>"/>
        <input type="hidden" name="selectedId" value="<%= item.getID() %>"/>
        <input class="btn btn-primary pull-right col-md-3" type="submit" name="submit_approve" value="<fmt:message key="jsp.tools.general.approve"/>" />
		<input class="btn btn-default pull-right col-md-3" type="submit" name="submit_cancel" value="<fmt:message key="jsp.tools.general.cancel"/>" />
	<%	
				if(localMessageFound) {
	%>
				<script type="text/javascript">
					jQuery('input[name="submit_approve"]').toggle();
				</script>
	<%
				}
	%>				
</div>	
</div>
</form>

</dspace:layout>
