<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Feedback form JSP
  -
  - Attributes:
  -    feedback.problem  - if present, report that all fields weren't filled out
  -    authenticated.email - email of authenticated user, if any
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    boolean problem = (request.getParameter("feedback.problem") != null);
    String email = request.getParameter("email");

    if (email == null || email.equals(""))
    {
        email = (String) request.getAttribute("authenticated.email");
    }

    if (email == null)
    {
        email = "";
    }

    String feedback = request.getParameter("feedback");
    if (feedback == null)
    {
        feedback = "";
    }

    String fromPage = request.getParameter("fromPage");
    if (fromPage == null)
    {
		fromPage = "";
    }
    
    String crisClaimedProfile = (String)request.getAttribute("feedback.crisclaim");
%>

<dspace:layout titlekey="jsp.feedback.form.title">
    <%-- <h1>Feedback Form</h1> --%>
    <h1><fmt:message key="jsp.feedback.form.title"/></h1>

<%
    if (crisClaimedProfile!=null)
    {
%>
        <p><fmt:message key="jsp.feedback.form.isaclaimprofile"><fmt:param><%= crisClaimedProfile %></fmt:param></fmt:message></p>
<%
    } else {
%>
    	<%-- <p>Thanks for taking the time to share your feedback about the
    	DSpace system. Your comments are appreciated!</p> --%>
    	<p><fmt:message key="jsp.feedback.form.text1"/></p>
<%
    }
%>
<%
    if (problem)
    {
%>
        <%-- <p><strong>Please fill out all of the information below.</strong></p> --%>
        <p><strong><fmt:message key="jsp.feedback.form.text2"/></strong></p>
<%
    }
%>
    <form action="<%= request.getContextPath() %>/feedback" method="post">

		<div class="form-group">
			<div class="input-group-addon">
				<span class="col-md-2"><label for="temail"><fmt:message
							key="jsp.feedback.form.email" /></label></span> <span class="col-md-5"><input
					class="form-control" type="text" name="email" id="temail" size="50"
					value="<%=StringEscapeUtils.escapeHtml(email)%>" /></span>
			</div>			
		</div>
		<div class="form-group">
			<div class="input-group-addon">
				<span class="col-md-2"><label for="tfeedback"><fmt:message
							key="jsp.feedback.form.comment" /></label></span> <span class="col-md-5">
							
							<%
    if (crisClaimedProfile!=null)
    {
%>
            <textarea class="form-control" name="feedback" id="tfeedback" rows="6" cols="50"><fmt:message key="jsp.feedback.textclaim"><fmt:param><%= crisClaimedProfile %></fmt:param></fmt:message></textarea>
<%
    } else {
%>
			<textarea class="form-control" name="feedback" id="tfeedback" rows="6" cols="50"><%=StringEscapeUtils.escapeHtml(feedback)%></textarea>
<%
    }
%>
				
				</span>
			</div>			
		</div>
		
		<div class="btn-group">        	 
            <input class="btn btn-default" type="submit" name="submit" value="<fmt:message key="jsp.feedback.form.send"/>" />
   		</div>
    </form>

</dspace:layout>
