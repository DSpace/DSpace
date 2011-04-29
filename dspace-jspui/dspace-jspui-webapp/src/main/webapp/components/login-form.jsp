<%--
  - login-form.jsp
  -
  - Version: $Revision: 3705 $
  -
  - Date: $Date: 2009-04-11 13:02:24 -0400 (Sat, 11 Apr 2009) $
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
  - Component which displays a login form and associated information
  --%>
  
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<table class="miscTable" align="center" width="70%">
  <tr>
    <td class="evenRowEvenCol">
     <form method="post" action="<%= request.getContextPath() %>/password-login">  
      <p><strong><a href="<%= request.getContextPath() %>/register"><fmt:message key="jsp.components.login-form.newuser"/></a></strong></p>
	  <p><fmt:message key="jsp.components.login-form.enter"/></p>

        <table border="0" cellpadding="5" align="center">
          <tr>
            <td class="standard" align="right"><label for="tlogin_email"><strong><fmt:message key="jsp.components.login-form.email"/></strong></label></td>
            <td><input type="text" name="login_email" id="tlogin_email" /></td>
          </tr>        
          <tr>
            <td class="standard" align="right"><label for="tlogin_password"><strong><fmt:message key="jsp.components.login-form.password"/></strong></label></td>
            <td><input type="password" name="login_password" id="tlogin_password" /></td>
          </tr>      
          <tr>
            <td align="center" colspan="2">
              <input type="submit" name="login_submit" value="<fmt:message key="jsp.components.login-form.login"/>" />
            </td>
          </tr>
        </table>

      </form>
	  <p><a href="<%= request.getContextPath() %>/forgot"><fmt:message key="jsp.components.login-form.forgot"/></a></p></td>
  </tr>
</table>
