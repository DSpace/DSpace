<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

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
