<%--
  - controlledvocabulary.jsp
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
  -- This jsp is reponsible for displaying controlled vocabularies in a 
  -- popup window during the description phases of submitted items.
  -- This jsp holds the content of that popup window.
  --%>

  
<%@ page language="java" contentType="text/html;charset=iso-8859-1" %>
<%@ page  import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>

<head> 
<title><fmt:message key="jsp.controlledvocabulary.controlledvocabulary.title"/></title>
<meta http-equiv="Content-Type" content="text/html;charset=iso-8859-1"/>


<link rel="shortcut icon" href="<%= request.getContextPath() %>/favicon.ico" type="image/x-icon"/>
<link type="text/css" rel="stylesheet" href="<%= request.getContextPath() %>/styles.css.jsp"/>

<style type="text/css">
	body {background-color: #ffffff}
</style>


<script type="text/javascript" language="JavaScript" src="<%= request.getContextPath() %>/utils.js"></script>


  <script type="text/javascript">
	<%
		if(request.getParameter("ID")!=null) {
			session.setAttribute("controlledvocabulary.ID", request.getParameter("ID"));
		}
	%>

	<%-- This function is not included in scripts file 
	     because it has to be changed dynamically --%>

	function sendBackToParentWindow(node) {
		var resultPath = "";
		var firstNode = 1;
		var pathSeparator = "::";
		
		
		while(node != null) {
			if(firstNode == 1) {
				resultPath = getTextValue(node);
				firstNode = 0;
			} else {
				resultPath = getTextValue(node) + pathSeparator + resultPath;
			}
			node = getParentTextNode(node);
		}
		
		window.opener.document.edit_metadata.<%=session.getAttribute("controlledvocabulary.ID")%>.value= resultPath;
		
		self.close();
		return false;
	}
  </script>
</head>





<body>

<%
	String filter = (String)session.getAttribute("controlledvocabulary.filter");
	filter = filter==null?"":filter;
	String ID = (String)session.getAttribute("controlledvocabulary.ID");
	
	if(request.getParameter("vocabulary")!=null) {
		session.setAttribute("controlledvocabulary.vocabulary", request.getParameter("vocabulary"));
	}
	String vocabulary = (String)session.getAttribute("controlledvocabulary.vocabulary");
%>

<br/>

<div style="margin-left:10px">

	<fmt:message key="jsp.controlledvocabulary.controlledvocabulary.trimmessage"/>

<table>
<tr>
<td>
    <fmt:message key="jsp.controlledvocabulary.controlledvocabulary.filter"/> 
</td>
<td>    
	<form name="filterVocabulary" 
		  method="post" 
		  action="<%= request.getContextPath()%>/controlledvocabulary">
	  
	  <input style="border-width:1px;border-style:solid;" name="filter" type="text" id="filter" size="35" value="<%= filter %>"/>
	  <input type="submit" name="submit" value="<fmt:message key='jsp.controlledvocabulary.controlledvocabulary.trimbutton'/>"/>
	  <input type="hidden" name="ID" value="<%= ID %>"/>
	  <input type="hidden" name="action" value="filter"/>
	  <input type="hidden" name="callerUrl" value="<%= request.getContextPath()%>/controlledvocabulary/controlledvocabulary.jsp"/>
	</form>
</td>
<td>
	<form name="clearFilter" method="post" action="<%= request.getContextPath() %>/controlledvocabulary">
	  <input type="hidden" name="ID" value="<%= ID %>"/>
	  <input type="hidden" name="filter" value=""/>
	  <input type="submit" name="submit" value="<fmt:message key='jsp.controlledvocabulary.controlledvocabulary.clearbutton'/>"/>
	  <input type="hidden" name="action" value="filter"/> 
	  <input type="hidden" name="callerUrl" value="<%= request.getContextPath()%>/controlledvocabulary/controlledvocabulary.jsp"/>
    </form>
</td>
</tr>
<tr>
<td colspan="3" class="submitFormHelpControlledVocabularies">
	<dspace:popup page="/help/index.html#controlledvocabulary"><fmt:message key="jsp.controlledvocabulary.controlledvocabulary.help-link"/></dspace:popup>
</td>
</tr>
</table>

</div>

<h1><fmt:message key="jsp.controlledvocabulary.controlledvocabulary.title"/></h1>

<dspace:controlledvocabulary filter="<%= filter %>" vocabulary="<%= vocabulary %>"/> 

<br/>
<center>
	<input type="button" name="cancel" onclick="window.close();" value="<fmt:message key="jsp.controlledvocabulary.controlledvocabulary.closebutton"/>" />
</center>
</body>
</html>
