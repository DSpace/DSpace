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
           <form method="post" action="<%= request.getContextPath() %>/ldap-login">
	    <p><strong><a href="<%= request.getContextPath() %>/register"><fmt:message key="jsp.components.ldap-form.newuser"/></a></strong></p>            
	    <p><fmt:message key="jsp.components.ldap-form.enter"/></p>
 
               <table border="0" cellpadding="5" align="center">
                    <tr>
                        <td class="standard" align="right"><strong><fmt:message key="jsp.components.ldap-form.username-or-email"/></strong></td>
                        <td><input tabindex="1" type="text" name="login_netid"></td>
                    </tr>
                    <tr>
            		<td class="standard" align="right"><strong><fmt:message key="jsp.components.ldap-form.password"/></strong></td>
                        <td><input tabindex="2" type="password" name="login_password"></td>
                    </tr>
                    <tr>
                        <td align="center" colspan="2">
			                <input type="submit" tabindex="3" name="login_submit" value="<fmt:message key="jsp.components.ldap-form.login.button"/>">
                        </td>
                    </tr>
                </table>
            </form>
        </td>
    </tr>
</table>
