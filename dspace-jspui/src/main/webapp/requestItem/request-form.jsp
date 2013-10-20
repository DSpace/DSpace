<%--
  - RequestItem-form.jsp
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

<%@ page import="org.dspace.app.webui.servlet.RequestItemServlet"%>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

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
    
    String bitstream_id = (String) request.getAttribute("bitstream-id");
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
	         <span class="input-group-addon"><input type="radio" class="form-control" name="allfiles" value="true" <%=allfiles?"checked":""%> /></span>
	         <span class="form-control"><fmt:message key="jsp.request.item.request-form.yes"/></span>
	        </div>
	        <div class="input-group">
	         <span class="input-group-addon"><input type="radio" class="form-control" name="allfiles" value="false" <%=allfiles?"":"checked"%> /></span>
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