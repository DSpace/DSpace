<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Page that displays the list of choices of login pages
  - offered by multiple stacked authentication methods.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="java.util.Iterator" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="java.sql.SQLException" %>

<%@ page import="org.apache.log4j.Logger" %>

<%@ page import="org.dspace.app.webui.util.JSPManager" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.authenticate.AuthenticationManager" %>
<%@ page import="org.dspace.authenticate.AuthenticationMethod" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.core.LogManager" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<dspace:layout navbar="off" locbar="off" titlekey="jsp.login.chooser.title" nocache="true">

    <table border="0" width="90%">
        <tr>
            <td align="left">
                <%-- <H1>Log In to DSpace</H1> --%>
                <h1><fmt:message key="jsp.login.chooser.heading"/></h1>
            </td>
            <td align="right" class="standard">
                <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") + \"#login\" %>"><fmt:message key="jsp.help"/></dspace:popup>
            </td>
        </tr>
    </table>
    <p></p>
    <table border="0" width="90%">
            <tr>
                <td align="left">
                    <a href="https://fed.princeton.edu/cas/login?service=http://dataspace.princeton.edu/mydspace">
                        <H1> Princeton CAS Authentication</H1>
                    </a>
                </td>

            </tr>
    </table>




</dspace:layout>
