<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Component which displays a login form and associated information
  --%>
  
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<table class="miscTable" align="center" width="70%">
  <tr>
    <td class="evenRowEvenCol">
     <form name="loginform" id="loginform" method="post" action="<%= request.getContextPath() %>/password-login">  
      <p><strong><a href="<%= request.getContextPath() %>/register"><fmt:message key="jsp.components.login-form.newuser"/></a></strong></p>
	  <p><fmt:message key="jsp.components.login-form.enter"/></p>

        <table border="0" cellpadding="5" align="center">
          <tr>
            <td class="standard" align="right"><label for="tlogin_email"><strong><fmt:message key="jsp.components.login-form.email"/></strong></label></td>
            <td><input type="text" name="login_email" id="tlogin_email" tabindex="1" /></td>
          </tr>        
          <tr>
            <td class="standard" align="right"><label for="tlogin_password"><strong><fmt:message key="jsp.components.login-form.password"/></strong></label></td>
            <td><input type="password" name="login_password" id="tlogin_password" tabindex="2" /></td>
          </tr>      
          <tr>
            <td align="center" colspan="2">
              <input type="submit" name="login_submit" value="<fmt:message key="jsp.components.login-form.login"/>" tabindex="3" />
            </td>
          </tr>
        </table>

      </form>
      <script type="text/javascript">
		document.loginform.login_email.focus();
	  </script>
	  <p><a href="<%= request.getContextPath() %>/forgot"><fmt:message key="jsp.components.login-form.forgot"/></a></p></td>
  </tr>
</table>
