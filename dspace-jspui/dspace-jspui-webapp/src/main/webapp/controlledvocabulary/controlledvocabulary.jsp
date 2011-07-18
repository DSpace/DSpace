<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
  
<%--
  -- This jsp is reponsible for displaying controlled vocabularies in a 
  -- popup window during the description phases of submitted items.
  -- This jsp holds the content of that popup window.
  --%>

  
<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ page  import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>

<head> 
<title><fmt:message key="jsp.controlledvocabulary.controlledvocabulary.title"/></title>
<meta http-equiv="Content-Type" content="text/html;charset=UTF-8"/>


<link rel="shortcut icon" href="<%= request.getContextPath() %>/favicon.ico" type="image/x-icon"/>
<link type="text/css" rel="stylesheet" href="<%= request.getContextPath() %>/styles.css"/>

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
