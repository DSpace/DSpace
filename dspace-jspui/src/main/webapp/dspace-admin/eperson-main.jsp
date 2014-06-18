<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

<%--
  - main page for eperson admin
  -
  - Attributes:
  -   no_eperson_selected - if a user tries to edit or delete an EPerson without
  -                         first selecting one
  -   reset_password - if a user tries to reset password of an EPerson and the email with token is
  -                    send successfull 
  -
  - Returns:
  -   submit_add    - admin wants to add an eperson
  -   submit_browse - admin wants to browse epeople
  -
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
	
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
   boolean noEPersonSelected = (request.getAttribute("no_eperson_selected") != null);
   boolean resetPassword = (request.getAttribute("reset_password") != null);
   boolean loginAs = ConfigurationManager.getBooleanProperty("webui.user.assumelogin", false);
%>

<dspace:layout style="submission" titlekey="jsp.dspace-admin.eperson-main.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

    <%-- <h1>Administer EPeople</h1> --%>
    <h1><fmt:message key="jsp.dspace-admin.eperson-main.heading"/>
    <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"epeople\"%>"><fmt:message key="jsp.help"/></dspace:popup>
    </h1>
  
    <%-- <h3>Choose an action:</h3> --%>
    <h3><fmt:message key="jsp.dspace-admin.eperson-main.choose"/></h3>
  
  
  

<% if (noEPersonSelected)
	{ %><p class="alert alert-warning">
	     <fmt:message key="jsp.dspace-admin.eperson-main.noepersonselected"/>
	   </p>
<%  } %>
<% if (resetPassword)
	{ %><p class="alert alert-success">
	     <fmt:message key="jsp.dspace-admin.eperson-main.ResetPassword.success_notice"/>
	   </p>
<%  } %>    
    <form name="epersongroup" method="post" action="">    
			<div class="row">
            <%-- <input type="submit" name="submit_add" value="Add EPerson..."> --%>
            	<input class="btn btn-success col-md-2 col-md-offset-5" type="submit" name="submit_add" value="<fmt:message key="jsp.dspace-admin.eperson-main.add"/>" />
			</div>
			<br/>

	        <fmt:message key="jsp.dspace-admin.eperson-main.or"/>
			
            <div class="row">
	            <div class="col-md-6">
	            <dspace:selecteperson multiple="false" />
	            </div>
            
            <%-- then&nbsp;<input type="submit" name="submit_edit" value="Edit..." onclick="javascript:finishEPerson();"> --%>
			<div class="col-md-2">
						<fmt:message key="jsp.dspace-admin.eperson-main.then"/>
			</div>
			<div class="col-md-4">
			<input type="submit" class="btn btn-default col-md-4" name="submit_edit" value="<fmt:message key="jsp.dspace-admin.general.edit"/>" onclick="javascript:finishEPerson();"/>
						
            <% if(loginAs) { %>&nbsp;<input type="submit" class="btn btn-default col-md-4" name="submit_login_as" value="<fmt:message key="jsp.dspace-admin.eperson-main.LoginAs.submit"/>" onclick="javascript:finishEPerson();"/> <% } %>
            
            <%-- <input type="submit" name="submit_delete" value="Delete..." onclick="javascript:finishEPerson();"> --%>
            <input type="submit" class="btn btn-danger col-md-4" name="submit_delete" value="<fmt:message key="jsp.dspace-admin.general.delete-w-confirm"/>" onclick="javascript:finishEPerson();"/>
            
            </div>
            </div>
    </form>
 
</dspace:layout>
