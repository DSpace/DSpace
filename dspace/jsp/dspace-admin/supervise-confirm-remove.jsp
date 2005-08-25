<%--
  - supervise-confirm-remove.jsp
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
   - This page lists the current supervisory settings for workspace items
   -
   - Attributes:
   -    wsItem  - An item that is going to be removed
   -    group   - the group supervising the item
   --%>
   
   

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page import="org.dspace.content.DCValue" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.eperson.Group" %>
<%@ page import="org.dspace.content.WorkspaceItem" %>

<%
    // get item and group out of the request
    WorkspaceItem wsItem = (WorkspaceItem) request.getAttribute("wsItem");
    Group group = (Group) request.getAttribute("group");
%>

<dspace:layout titlekey="jsp.dspace-admin.supervise-confirm-remove.title"
               navbar="admin"
               locbar="link"
               parentlink="/dspace-admin"
               parenttitlekey="jsp.administer">

<h1><fmt:message key="jsp.dspace-admin.supervise-confirm-remove.heading"/></h1>

<h3><fmt:message key="jsp.dspace-admin.supervise-confirm-remove.subheading"/></h3>

<br/><br/>

<div align="center"/>

<%
        DCValue[] titleArray = wsItem.getItem().getDC("title", null, Item.ANY);
//        String title = (titleArray.length > 0 ? titleArray[0].value : "Untitled");
        EPerson submitter = wsItem.getItem().getSubmitter();
%>

<strong><fmt:message key="jsp.dspace-admin.supervise-confirm-remove.titleheader"/></strong>:
<br/>
<%
		if (titleArray.length > 0)
		{
%>
			<%= titleArray[0].value %>
<%
		}
		else
		{
%>
			<fmt:message key="jsp.general.untitled"/>
<%
		}
%>
<br/><br/>
<strong><fmt:message key="jsp.dspace-admin.supervise-confirm-remove.authorheader"/></strong>:
<br/>
<a href="mailto:<%= submitter.getEmail() %>"><%= submitter.getFullName() %></a>
<br/><br/>
<strong><fmt:message key="jsp.dspace-admin.supervise-confirm-remove.supervisorgroupheader"/></strong>:
<br/>
<%= group.getName() %>
<br/><br/>

<fmt:message key="jsp.dspace-admin.supervise-confirm-remove.confirm"/>

<%-- form to request removal of supervisory linking --%>
<form method="post" action="">
    <input type="hidden" name="gID" value="<%= group.getID() %>"/>
    <input type="hidden" name="siID" value="<%= wsItem.getID() %>"/>
    <input type="submit" name="submit_doremove" value="<fmt:message key="jsp.dspace-admin.general.remove"/>"/>
    <input type="submit" name="submit_base" value="<fmt:message key="jsp.dspace-admin.general.cancel"/>"/>
</form>

</dspace:layout>
