<%--
  - request-letter.jsp
  -
  - Version: $Revision: 1.0 $
  -
  - Date: $Date: 2004/12/29 19:51:49 $
  -
  - Copyright (c) 2004, University of Minho
  -   All rights reserved.
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
  - request-information JSP
  -
  - Attributes:
  -    requestItem.problem  - if present, report that all fields weren't filled out
  -    authenticated.email - email of authenticated user, if any
  -	   handle - URL of handle item
  --%>

<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ page import="org.dspace.app.webui.servlet.RequestItemServlet"%>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%
    String token = request.getParameter("token");

    String subject = (String) request.getAttribute("subject");
    if (subject == null)
        subject = "";
	
    String message = (String) request.getAttribute("message");
    if (message == null)
        message = "";

    boolean resp = (Boolean) request.getAttribute("response");
%>

<dspace:layout locbar="off" navbar="default" titlekey="jsp.request.item.request-letter.title" >

<% if(resp) { %>
    <h2><fmt:message key="jsp.request.item.request-letter.accept.heading" /></h2>
    <p><fmt:message key="jsp.request.item.request-letter.accept.info" /></p>
<% } else { %>
    <h2><fmt:message key="jsp.request.item.request-letter.reject.heading" /></h2>
    <p><fmt:message key="jsp.request.item.request-letter.reject.info" /></p>
<% } %>
    <form name="form1" action="<%= request.getContextPath() %>/request-item" method="post" class="form-horizontal">
        <input type="hidden" name="token" value='<%= token %>' />
        <input type="hidden" name="accept_request" value="<%= resp %>" />
        <input type="hidden" name="step" value="<%=RequestItemServlet.RESUME_REQUEST %>" />
        <div class="form-group">
	        <label for="subject" class="control-label col-md-2"><fmt:message key="jsp.request.item.request-letter.subject"/></label>
	        <div class="col-md-10">
	        	<input type="text" class="form-control" name="subject" value='<%= subject %>' />
	        </div>
        </div>
        <div class="form-group">
	        <label for="message" class="control-label col-md-2"><fmt:message key="jsp.request.item.request-letter.message"/></label>
	        <div class="col-md-10">
        		<textarea class="form-control" name="message" rows="20" cols="100"><%= message %></textarea>
        	</div>
        </div>
        <div class="btn btn-group col-md-4 pull-right row">
			<input type="submit" name="submit_back" class="btn btn-default col-md-6" value="<fmt:message key="jsp.request.item.request-letter.back"/>" >
	        <input type="submit" class="btn btn-<%= resp?"success":"reject" %> col-md-6" name="submit_next" value="<fmt:message key="jsp.request.item.request-letter.next"/>" >
	    </div>    
    </form>

</dspace:layout>
