<%--
  - forgot-password.jsp
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
  - Forgotten password DSpace form
  -
  - Form where new users enter their email address to get a token to enter a
  - new password.
  -
  - Attributes to pass in:
  -     retry  - if set, this is a retry after the user entered an invalid email
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.app.webui.servlet.RegisterServlet" %>

<%
    boolean retry = (request.getAttribute("retry") != null);
%>

<dspace:layout titlekey="jsp.register.forgot-password.title">

    <%-- <h1>Forgotten Password</h1> --%>
	<h1><fmt:message key="jsp.register.forgot-password.title"/></h1>
    
<%
    if (retry)
    {
%>
    <%-- <p><strong>The e-mail address you entered was not recognized.  Please
    try again.</strong></p> --%>
	<p><strong><fmt:message key="jsp.register.forgot-password.info1"/></strong></p>
<%
    }
%>
    <%-- <p>Please enter your e-mail
    address in the box below and click "I Forgot My Password".  You'll be sent
    an e-mail which will allow you to set a new password.</p> --%>
	<p><fmt:message key="jsp.register.forgot-password.info2"/></p>
    
    <form action="<%= request.getContextPath() %>/forgot" method="post">
        <input type="hidden" name="step" value="<%= RegisterServlet.ENTER_EMAIL_PAGE %>"/>

        <center>
            <table class="miscTable">
                <tr>
                    <td class="oddRowEvenCol">
                        <table border="0" cellpadding="5">
                            <tr>
                                <%-- <td class="standard"><strong>E-mail Address:</strong></td> --%>
								<td class="standard"><strong><label for="temail"><fmt:message key="jsp.register.forgot-password.email.field"/></strong></label></td>
                                <td class="standard"><input type="text" name="email" id="temail" /></td>
                            </tr>
                            <tr>
                                <td align="center" colspan="2">
                                    <%-- <input type="submit" name="submit" value="I Forgot My Password"> --%>
									<input type="submit" name="submit" value="<fmt:message key="jsp.register.forgot-password.forgot.button"/>" />
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>
        </center>
    </form>
    
</dspace:layout>
