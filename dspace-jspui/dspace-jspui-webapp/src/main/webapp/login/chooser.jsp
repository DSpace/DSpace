<%--
  - chooser.jsp
  -
  - Version: $Revision: 3705 $
  -
  - Date: $Date: 2009-04-11 19:02:24 +0200 (Sat, 11 Apr 2009) $
  -
  - Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
  - Institute of Technology.  All rights reserved.
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
    <table class="miscTable" align="center" width="70%">
      <tr>
        <td class="evenRowEvenCol">
          <h2><fmt:message key="jsp.login.chooser.chooseyour"/></h2>
          <ul>
<%
    Iterator ai = AuthenticationManager.authenticationMethodIterator();
    AuthenticationMethod am;
    Context context = null;
    try
    {
    	context = UIUtil.obtainContext(request);
    	int count = 0;
    	String url = null;
    	while (ai.hasNext())
    	{
        am = (AuthenticationMethod)ai.next();
        if ((url = am.loginPageURL(context, request, response)) != null)
        {
%>
            <li><p><strong><a href="<%= url %>">
		<%-- This kludge is necessary because fmt:message won't
                     evaluate its attributes, so we can't use it on java expr --%>
                <%= javax.servlet.jsp.jstl.fmt.LocaleSupport.getLocalizedMessage(pageContext, am.loginPageTitle(context)) %>
                        </a></strong></p></li>
<%
        }
        }
    }
    catch(SQLException se)
    {
    	// Database error occurred.
        Logger log = Logger.getLogger("org.dspace.jsp");
        log.warn(LogManager.getHeader(context,
                "database_error",
                se.toString()), se);

        // Also email an alert
        UIUtil.sendAlert(request, se);
        JSPManager.showInternalError(request, response);
    }
    finally 
    {
    	context.abort();
    }
  
%>
          </ul>
        </td>
      </tr>
    </table>


</dspace:layout>
