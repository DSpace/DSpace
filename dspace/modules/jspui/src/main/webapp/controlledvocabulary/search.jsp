<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%-- 
	This jsp shows a search form with a controlled vocabulary tree.
	The branches of the tree can be expanded/collapsed by means of javascript.
	To be WAI compatible a new version of this jsp must be developed.
	The add-on may be turn off in dspace.cfg
--%>  

<%@page import="org.dspace.core.Utils"%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page  import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ taglib uri="/WEB-INF/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
	String filter = (String)(session.getAttribute("conceptsearch.filter") 
		!= null?session.getAttribute("conceptsearch.filter"):"");
%>


<dspace:layout locbar="nolink" title="<%= LocaleSupport.getLocalizedMessage(pageContext, \"jsp.controlledvocabulary.search.title\") %>">

<h1><fmt:message key="jsp.controlledvocabulary.search.heading"/></h1>

<div align="right">
	<dspace:popup page="/help/index.html#subjectsearch"><fmt:message key="jsp.help"/></dspace:popup>
</div>

<p><fmt:message key="jsp.controlledvocabulary.search.on-page-help.para1"/></p>
<p><fmt:message key="jsp.controlledvocabulary.search.on-page-help.para2"/></p>


<div style="margin-left:20px">	

<div style="margin-left:20px">

<fmt:message key="jsp.controlledvocabulary.search.trimmessage"/>
<table>
<tr>
<td valign="middle">
	<fmt:message key="jsp.controlledvocabulary.controlledvocabulary.filter"/>
</td>
<td>
    
	<form name="filterVocabulary" method="post" action="<%= request.getContextPath() %>/subject-search">
	  <input style="border-width:1px;border-style:solid;" 
	  		 name="filter" type="text" id="filter" 
	  		 size="15" value="<%= Utils.addEntities(filter) %>" 
	  		 title="<%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.controlledvocabulary.search.trimmessage") %>"/>
	  <input type="submit" name="submit" value="<%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.controlledvocabulary.search.trimbutton") %>"/>
	  <input type="hidden" name="action" value="filter"/>
	</form>
</td>
<td>
	<form name="clearFilter" method="post" action="<%= request.getContextPath() %>/subject-search">
  		<input type="hidden" name="filter" value=""/>
  		<input type="submit" name="submit" value="<%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.controlledvocabulary.search.clearbutton") %>"/>
  		<input type="hidden" name="action" value="filter"/> 
  	</form>
</td>
</tr>
</table>  	
  	
</div>

	<form action="<%= request.getContextPath() %>/subject-search"  method="post">
	
		<dspace:controlledvocabulary filter="<%= filter %>" allowMultipleSelection="true"/> 
			
		<br/>
		<input type="hidden" name="action" value="search"/>
		<input type="submit" name="submit" value="<%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.controlledvocabulary.search.searchbutton") %>"/>
	</form>
</div>


</dspace:layout>
