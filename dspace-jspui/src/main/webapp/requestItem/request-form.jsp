<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Sugest form JSP
  -
  - Attributes:
  -    requestItem.problem  - if present, report that all fields weren't filled out
  -    authenticated.email - email of authenticated user, if any
  -	   handle - URL of handle item
  --%>

<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ page import="java.util.*"%>
<%@ page import="org.dspace.app.webui.servlet.*" %>

<%
	request.setCharacterEncoding("UTF-8");
	
    boolean problem = (request.getAttribute("requestItem.problem") != null);

    String email = (String) request.getAttribute("email");
    if (email == null)
        email = "";

    String userName = (String) request.getAttribute("reqname");
    if (userName == null)
        userName = "";

    String handle = (String) request.getAttribute("handle");
    if (handle == null )
        handle = "";
	
    String title = (String) request.getAttribute("title");
    if (title == null)
        title = "";
		
    String coment = (String) request.getAttribute("coment");
    if (coment == null)
        coment = "";

    UUID bitstream_id = (UUID) request.getAttribute("bitstream-id");
    boolean allfiles = (request.getAttribute("allfiles") != null);

%>

<dspace:layout locbar="off" navbar="default" titlekey="jsp.request.item.request-form.title" >

<h2><fmt:message key="jsp.request.item.request-form.info2">
<fmt:param><a href="<%=request.getContextPath()%>/handle/<%=handle %>"><%=title %></a></fmt:param>
</fmt:message></h2>

<%
    	if (problem)
    	{
%>
        <p class="alert alert-warning"><fmt:message key="jsp.request.item.request-form.problem"/></p>
<%
    	}
%>
    <form name="form1" class="form-horizontal" action="<%= request.getContextPath() %>/request-item" method="post">
		<div class="form-group">
         <label for="reqname" class="control-label col-md-2"><fmt:message key="jsp.request.item.request-form.reqname"/></label>
         <div class="col-md-10">
         	<input class="form-control" type="text" name="reqname" size="50" value="<%= userName %>">
         </div>
        </div>
        <div class="form-group">
         <label for="email" class="control-label col-md-2"><fmt:message key="jsp.request.item.request-form.email"/></label>
         <div class="col-md-10">
			<input type="text" class="form-control" name="email" size="50" value="<%= email %>">
         </div>
        </div>
        <div class="form-group">
         <label for="allfiles" class="control-label col-md-2"><fmt:message key="jsp.request.item.request-form.allfiles"/></label>
         <div class="col-md-10">
	        <div class="input-group"> 
	         <span class="input-group-addon"><input type="radio" name="allfiles" value="true" <%=allfiles?"checked":""%> /></span>
	         <span class="form-control"><fmt:message key="jsp.request.item.request-form.yes"/></span>
	        </div>
	        <div class="input-group">
	         <span class="input-group-addon"><input type="radio" name="allfiles" value="false" <%=allfiles?"":"checked"%> /></span>
	         <span class="form-control"><fmt:message key="jsp.request.item.request-form.no"/></span>
	        </div> 
         </div>
        </div>
        <div class="form-group">
         <label for="coment" class="control-label col-md-2"><fmt:message key="jsp.request.item.request-form.coment"/></label>
		 <div class="col-md-10">
	         <textarea class="form-control" name="coment" rows="6" cols="46" wrap=soft><%= coment %></textarea>
    	 </div>
        </div>
        
         <input type="hidden" name="handle" value='<%= handle %>' />
         <input type="hidden" name="bitstream-id" value='<%= bitstream_id %>' />
         <input type="hidden" name="step" value="<%=RequestItemServlet.ENTER_FORM_PAGE %>" />
        <div class="btn btn-group col-md-4 pull-right row">
         <a class="btn btn-default col-md-6" href="<%=request.getContextPath()%>/handle/<%=handle %>"><fmt:message key="jsp.request.item.request-form.cancel" /></a>
         <button type="submit" name="submit" class="btn btn-primary col-md-6" value="true"><fmt:message key="jsp.request.item.request-form.go"/></button>
        </div> 
    </form>

</dspace:layout>