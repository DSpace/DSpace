<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - request-free-acess JSP
  -
  - Attributes:
  -    token  - token from the request item
  -    title - 
  -    handle - URL of handle item
  -    
  --%>

<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ page import="org.dspace.app.webui.servlet.RequestItemServlet"%>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%
    String token = request.getParameter("token");

    String handle = (String) request.getAttribute("handle");
    if (handle == null)
        handle = "";
	
    String title = (String) request.getAttribute("title");
    if (title == null)
        title = "";
%>

<dspace:layout locbar="off" navbar="default" titlekey="jsp.request.item.request-free-acess.title" >

<p class="alert alert-success"><fmt:message key="jsp.request.item.request-free-acess.info1"/></p>

<p><b><fmt:message key="jsp.request.item.request-free-acess.info2" /></b></p>

    <form name="form1" action="<%= request.getContextPath() %>/request-item" method="post" class="form-horizontal">
        <input type="hidden" name="token" value='<%= token %>'>
        <input type="hidden" name="step" value='<%= RequestItemServlet.RESUME_FREEACESS %>'>
        <div class="form-group">
	        <label for="name" class="control-label col-md-2"><fmt:message key="jsp.request.item.request-free-acess.name"/></label>
	        <div class="col-md-5">
	        	<input type="text" class="form-control" name="name" size="50" value="" />
	        </div>	
        </div>
        <div class="form-group">
        <label for="email" class="control-label col-md-2"><fmt:message key="jsp.request.item.request-free-acess.email"/></label>
        <div class="col-md-5">
        	<input type="text" name="email" class="form-control" size="50" value="" />
        </div>
        </div>
        <input class="btn btn-success col-md-offset-2" type="submit" name="submit_free" value="<fmt:message key="jsp.request.item.request-free-acess.free"/>"  />
    </form>

</dspace:layout>