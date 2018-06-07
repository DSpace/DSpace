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
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%
	
    Map<String, List<String[]>> result = (Map<String, List<String[]>>) request.getAttribute("result");
	String handle = (String)request.getAttribute("handle");
	Map<String, Boolean> haveSimilar = (Map<String, Boolean>)request.getAttribute("haveSimilar");
	
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

<ul class="nav nav-tabs" id="myTab" role="tablist">
<%
    // Keep a count of the number of values of each element+qualifier
    // key is "element" or "element_qualifier" (String)
    // values are Integers - number of values that element/qualifier so far
    Map<String, Integer> dcCounter = new HashMap<String, Integer>();
    
    int i = 0;
    for (String key : result.keySet())
    {

%>
        

  <li class="nav-item  <%= i==0?"active":""%>">
    <a class="nav-link" id="<%= key %>-tab" data-toggle="tab" href="#<%= key %>" role="tab" aria-controls="<%= key %>" <%= i==0?"aria-selected=\"true\"":""%>><fmt:message key="<%= key %>" /></a>
  </li>
  
<%
	i++;
    } %>
</ul>


<div class="tab-content" id="myTabContent">
<% 

i = 0;
for (String key : result.keySet())
{

%>

  <div class="tab-pane <%= i==0?"active":""%>" id="<%= key %>" role="tabpanel" aria-labelledby="<%= key %>-tab">
    <div class="row">
      <div class="col-sm-12">
      
      	<div class="col-sm-5">
        <div class="form-check">
          <input class="form-check-input" type="radio" name="userchoice_<%= key %>" id="dolater_<%= key %>" value="dolater_<%= key %>" <%= haveSimilar.get(key)?"":"checked" %>/>
          
          <label class="form-check-label" for="none_<%= key %>">
            <fmt:message key="jsp.authority-claim.choice.dolater"/>
          </label>
        </div>       
              
        <div class="form-check">
          <input class="form-check-input" type="radio" name="userchoice_<%= key %>" id="none_<%= key %>" value="none_<%= key %>"/>
          
          <label class="form-check-label" for="none_<%= key %>">
            <fmt:message key="jsp.authority-claim.choice.none"/>
          </label>
        </div> 
<%        

		for(String[] record : result.get(key)) { 
	        Integer count = dcCounter.get(key);
	        if (count == null)
	        {
	            count = new Integer(0);
	        }
	        
	        // Increment counter in map
	        dcCounter.put(key, new Integer(count.intValue() + 1));
	
	        // We will use two digits to represent the counter number in the parameter names.
	        // This means a string sort can be used to put things in the correct order even
	        // if there are >= 10 values for a particular element/qualifier.  Increase this to
	        // 3 digits if there are ever >= 100 for a single element/qualifer! :)
	        String sequenceNumber = count.toString();
	        
	        while (sequenceNumber.length() < 2)
	        {
	            sequenceNumber = "0" + sequenceNumber;
	        }
	        
	        String fieldNameIdx = "value_" + key + "_" + sequenceNumber;
	        String languageIdx = "language_" + key + "_" + sequenceNumber;
	        String authorityName = "choice_" + key + "_authority_" + sequenceNumber;
	        String confidenceName = "choice_" + key + "_confidence_" + sequenceNumber;
	        String option = sequenceNumber + "_" + key;
	        
	        String value = record[0];
	        String authority = record[1];
	        String confidence = record[2];
	        String language = record[3];
	        String similar = record[4];
		 %>


    	<input type="hidden" id="<%= fieldNameIdx%>" name="<%= fieldNameIdx%>" value="<%= value %>"/>
        <input type="hidden" id="<%= languageIdx %>" name="<%= languageIdx %>" value="<%= (language == null ? "" : language.trim()) %>"/>
        <input type="hidden" id="<%= authorityName %>" name="<%= authorityName %>" value="<%= authority %>"/>
        <input type="hidden" id="<%= confidenceName %>" name="<%= confidenceName %>" value="<%= confidence %>"/>


        <div class="form-check">
          <input class="form-check-input" type="radio" name="userchoice_<%= key %>" id="<%= option %>" value="<%= option %>" <%= value.equals(similar)?"checked":"" %>/>
          <label class="form-check-label" for="<%= option %>">
            <%= value %>
          </label>
        </div>
	<% } %>
		</div>
		<div class="col-sm-5">
			  <div class="form-group">
			    <label for="requestNote_<%= key %>"><fmt:message key="jsp.authority-claim.label.requestnote"/></label>
			    <textarea class="form-control" name="requestNote_<%= key %>" id="requestNote_<%= key %>" rows="3"></textarea>
			  </div>
		</div>
      </div>
    </div>
  </div>

<% 
	i++;
} %>
</div>
        <input type="hidden" name="handle" value="<%= handle %>"/>
					
        <%-- <input type="submit" name="submit" value="Update" /> --%>
        <input class="btn btn-primary pull-right col-md-3" type="submit" name="submit" value="<fmt:message key="jsp.tools.general.update"/>" />
        <%-- <input type="submit" name="submit_cancel" value="Cancel" /> --%>
		<input class="btn btn-default pull-right col-md-3" type="submit" name="submit_cancel" value="<fmt:message key="jsp.tools.general.cancel"/>" />
	
</form>

</dspace:layout>
