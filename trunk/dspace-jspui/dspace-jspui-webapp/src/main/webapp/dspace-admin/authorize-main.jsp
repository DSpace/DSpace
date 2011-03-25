<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

<%--
  - main page for authorization editing
  -
  - Attributes:
  -   none
  -
  - Returns:
  -   submit_community
  -   submit_collection
  -   submit_item
  -       item_handle
  -       item_id
  -   submit_advanced
  -
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.content.Collection" %>

<% request.setAttribute("LanguageSwitch", "hide"); %>

<%
// this space intentionally left blank
%>

<dspace:layout titlekey="jsp.dspace-admin.authorize-main.title"
               navbar="admin"
               locbar="link"
               parenttitle="general.administer"
               parentlink="/dspace-admin">

    <%-- <h1>Administer Authorization Policies</h1> --%>
    <h1><fmt:message key="jsp.dspace-admin.authorize-main.adm"/></h1>
  <table width="95%">
    <tr>
      <td align="left">
          <%-- <h3>Choose a resource to manage policies for:</h3> --%>
		  <h3><fmt:message key="jsp.dspace-admin.authorize-main.choose"/></h3>
      </td>
      <td align="right" class="standard">
        <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#authorize\"%>"><fmt:message key="jsp.help"/></dspace:popup>
      </td>
    </tr>
  </table>
    
    
    <form method="post" action="">    

    <center>
        <table width="70%">
            <tr>
                <td align="center">
                    <%-- <input type="submit" name="submit_community" value="Manage a Community's Policies"> --%>
                    <input type="submit" name="submit_community" value="<fmt:message key="jsp.dspace-admin.authorize-main.manage1"/>" />
                </td>
            </tr>
            <tr>
                <td align="center">
                    <%-- <input type="submit" name="submit_collection" value="Manage Collection's Policies"> --%>
                    <input type="submit" name="submit_collection" value="<fmt:message key="jsp.dspace-admin.authorize-main.manage2"/>" />
                </td>
            </tr>
            <tr>
                <td align="center">
                    <%-- <input type="submit" name="submit_item" value="Manage An Item's Policies"> --%>
                    <input type="submit" name="submit_item" value="<fmt:message key="jsp.dspace-admin.authorize-main.manage3"/>" />
                </td>
            </tr>
            <tr>
                <td align="center">
                    <%-- <input type="submit" name="submit_advanced" value="Advanced/Item Wildcard Policy Admin Tool"> --%>
                    <input type="submit" name="submit_advanced" value="<fmt:message key="jsp.dspace-admin.authorize-main.advanced"/>" />
                </td>
            </tr>
        </table>
    </center>        

    </form>
</dspace:layout>
