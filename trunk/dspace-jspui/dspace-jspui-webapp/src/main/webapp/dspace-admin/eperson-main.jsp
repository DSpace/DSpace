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
	
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
   boolean noEPersonSelected = (request.getAttribute("no_eperson_selected") != null);
%>

<dspace:layout titlekey="jsp.dspace-admin.eperson-main.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

    <%-- <h1>Administer EPeople</h1> --%>
    <h1><fmt:message key="jsp.dspace-admin.eperson-main.heading"/></h1>
  <table width="95%">
    <tr>
      <td align="left">
        <%-- <h3>Choose an action:</h3> --%>
        <h3><fmt:message key="jsp.dspace-admin.eperson-main.choose"/></h3>
      </td>
      <td align="right" class="standard">
        <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"epeople\"%>"><fmt:message key="jsp.help"/></dspace:popup>
      </td>
    </tr>
  </table>

<% if (noEPersonSelected)
	{ %><p><strong>
	     <fmt:message key="jsp.dspace-admin.eperson-main.noepersonselected"/>
	   </strong></p>
<%  } %>
    
    <form name="epersongroup" method="post" action="">    

    <center>
        <table width="90%">
            <tr>
                <td colspan="3" align="center">
                    <%-- <input type="submit" name="submit_add" value="Add EPerson..."> --%>
                    <input type="submit" name="submit_add" value="<fmt:message key="jsp.dspace-admin.eperson-main.add"/>" />
                </td>
            </tr>
            <tr>
            	<%-- <td colspan="3"><strong>OR</strong></td> --%>
            	<td colspan="3"><strong><fmt:message key="jsp.dspace-admin.eperson-main.or"/></strong></td>
            </tr>
            <tr>
                <td>
                    <dspace:selecteperson multiple="false" />
                </td>
                <td>
                	<%-- then&nbsp;<input type="submit" name="submit_edit" value="Edit..." onclick="javascript:finishEPerson();"> --%>
                	<fmt:message key="jsp.dspace-admin.eperson-main.then"/>&nbsp;<input type="submit" name="submit_edit" value="<fmt:message key="jsp.dspace-admin.general.edit"/>" onclick="javascript:finishEPerson();"/>
                </td>
                <td>
                	<%-- <input type="submit" name="submit_delete" value="Delete..." onclick="javascript:finishEPerson();"> --%>
                	<input type="submit" name="submit_delete" value="<fmt:message key="jsp.dspace-admin.general.delete-w-confirm"/>" onclick="javascript:finishEPerson();"/>
                </td>
            </tr>
        </table>
    </center>        

    </form>
</dspace:layout>
