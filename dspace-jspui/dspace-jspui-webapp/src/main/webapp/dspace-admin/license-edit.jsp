<%-- license-edit.jsp
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
  - License Edit Form JSP
  -
  - Attributes:
  -  license - The license to edit
   --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    // Get the existing license
    String license = (String)request.getAttribute("license");
    if (license == null)
    {
    	license = "";
    }

    // Are there any messages to show?
    String message = (String)request.getAttribute("edited");
    boolean edited = false;
    if ((message != null) && (message.equals("true")))
    {
    	edited = true;
    }
    message = (String)request.getAttribute("empty");
    boolean empty = false;
    if ((message != null) && (message.equals("true")))
    {
    	empty = true;
    }
    
%>

<dspace:layout titlekey="jsp.dspace-admin.license-edit.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

    <h1><fmt:message key="jsp.dspace-admin.license-edit.heading"/></h1>
    
    <form action="<%= request.getContextPath() %>/dspace-admin/license-edit" method="post">

    <%
    	if (edited)
    	{
    		%>
	    		<p>
	    			<strong><fmt:message key="jsp.dspace-admin.license-edit.edited"/></strong>
    			</p>
    		<%
    	}
    %>
    <%
    	if (empty)
    	{
    		%>
	    		<p>
	    			<strong><fmt:message key="jsp.dspace-admin.license-edit.empty"/></strong>
    			</p>
    		<%
    	}
    %>
    
    <p><fmt:message key="jsp.dspace-admin.license-edit.description"/></p>
    <p><textarea name="license" rows="15" cols="70"><%= license %></textarea><br />
       <input type="submit" name="submit_save" value="<fmt:message key="jsp.dspace-admin.general.save"/>" />
       <input type="submit" name="submit_cancel" value="<fmt:message key="jsp.dspace-admin.general.cancel"/>" />
    </p>
    </form>
</dspace:layout>
