<%--
  - search.jsp
  -
  - Version: $Revision$
  -
  - Date: $Date$
  -
  - Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
  - Institute of Technology.  All rights reserved.
  -
  - Redistribution and use in source and binary forms, with or without
  - modification, are permitted provided that the following conditions are
  - met:
  -
  - - Redistributions of source code must retain the above copyright
  - notice, this list of conditions and the following disclaimer.
  -
  - - Redistributions in binary form must reproduce the above copyright
  - notice, this list of conditions and the following disclaimer in the
  - documentation and/or other materials provided with the distribution.
  -
  - - Neither the name of the Hewlett-Packard Company nor the name of the
  - Massachusetts Institute of Technology nor the names of their
  - contributors may be used to endorse or promote products derived from
  - this software without specific prior written permission.
  -
  - THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  - ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  - LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  - A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  - HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  - INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
  - BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
  - OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  - ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  - TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
  - USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
  - DAMAGE.
  --%>

<%-- 
	This jsp shows a search form with a controlled vocabulary tree.
	The branches of the tree can be expanded/collapsed by means of javascript.
	To be WAI compatible a new version of this jsp must be developed.
	The add-on may be turn off in dspace.cfg
--%>  

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page  import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ taglib uri="/WEB-INF/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
	String filter = (String)(session.getAttribute("conceptsearch.filter") 
		!= null?session.getAttribute("conceptsearch.filter"):"");
%>


<dspace:layout locbar="nolink" title="<%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.controlledvocabulary.search.title") %>">

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
	  		 size="15" value="<%= filter %>" 
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
