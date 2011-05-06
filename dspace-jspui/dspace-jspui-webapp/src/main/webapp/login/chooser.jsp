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
