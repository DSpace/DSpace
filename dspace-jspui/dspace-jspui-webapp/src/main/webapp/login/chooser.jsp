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

<%@ page import="java.net.URL" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="java.sql.SQLException" %>

<%@ page import="org.apache.log4j.Logger" %>

<%@ page import="org.dspace.app.webui.util.JSPManager" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.authenticate.AuthenticationManager" %>
<%@ page import="org.dspace.authenticate.AuthenticationMethod" %>
<%@ page import="org.dspace.authenticate.CASAuthentication" %>
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

<%
    // Get the CAS url
    String casurl = null;
    String query = null;
    Context context = UIUtil.obtainContext(request);

    Iterator ai = AuthenticationManager.authenticationMethodIterator();
    while (ai.hasNext())
    {
      AuthenticationMethod am = (AuthenticationMethod)ai.next();
      if (am instanceof CASAuthentication) {
        if (request.getSession().getAttribute("cas.session.gateway") == null) {
          // We haven't tried a CAS gateway request yet, try one now
          request.getSession().setAttribute("cas.session.gateway", "true");

          String gatewayUrl = am.loginPageURL(context, request, response) + "&gateway=true";
          response.sendRedirect(response.encodeRedirectURL(gatewayUrl));
          return;

        } else {
          String[] parts = am.loginPageURL(context, request, response).split("\\?");
          casurl = parts[0];
          query = parts[1];
        }
      }
    }
%>

          <form 
             method="GET" 
             action="<%= casurl.toString() %>" 
             id="umLogin"
          />
<%
            String keyvals[] = query.split("&");
            for (int i=0; i < keyvals.length; i++) {
              String keyval[] = keyvals[i].split("=");
              %> <input type="hidden" name="<%= keyval[0] %>" value="<%= keyval[1] %>"> <%
            }
%>
          </form>
      
          <p><fmt:message key="jsp.login.chooser.chooseyour"/></p>

          <ul>
            <li><a href="#" onclick="document.getElementById('umLogin').submit(); return false">UM campus community</a> <span class="explain">(you will need your UM Directory ID and Password)</span><br>
              <br>
            </li>
            <li>
              <a href="#" onclick="toggleOtherLoginDisplay(); return false">Others</a>
      
              <div id="otherLogin" style="display:none">
                <dspace:include page="/components/login-form.jsp" />
              </div>
            </li>
          </ul>            
        </td>
      </tr>
    </table>


</dspace:layout>
