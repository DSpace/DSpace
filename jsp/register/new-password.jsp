<%--
  - new-password.jsp
  -
  - Version: $Revision: 1.5 $
  -
  - Date: $Date: 2004/02/23 18:32:42 $
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
  - New password form
  -
  - Form where users can enter a new password, after having been sent a token
  - because they forgot the old one.
  -
  - Attributes to pass in:
  -    eperson          - the eperson
  -    token            - the token associated with this password setting
  -    password.problem - Boolean true if the user has already tried a password
  -                       which is for some reason unnacceptible
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.app.webui.servlet.RegisterServlet" %>
<%@ page import="org.dspace.eperson.EPerson" %>

<%
    EPerson eperson = (EPerson) request.getAttribute("eperson");
    String token = (String) request.getAttribute("token");
    Boolean attr = (Boolean) request.getAttribute("password.problem");

    boolean passwordProblem = (attr != null && attr.booleanValue());
%>

<dspace:layout title="Enter New Password" nocache="true">

    <h1>Enter a New Password</H1>
    
    <P>Hello <%= eperson.getFullName() %>,</P>
    
<%
    if (passwordProblem)
    {
%>
    <P><strong>The passwords you enter below must match, and need to be at
    least 6 characters long.</strong></P>
<%
    }
%>
    
    <P>Please enter a new password into the box below, and confirm it by typing it
    again into the second box.  It should be at least six characters long.</P>

    <form action="<%= request.getContextPath() %>/forgot" method=POST>
        <table class="misc" align="center">
            <tr>
                <td class="oddRowEvenCol">
                    <table border=0 cellpadding=5>
                        <tr>
                            <td align=right class=standard><strong>New Password:</strong></td>
                            <td class=standard><input type=password name="password"></td>
                        </tr>
                        <tr>
                            <td align=right class=standard><strong>Again to Confirm:</strong></td>
                            <td class=standard><input type=password name="password_confirm"></td>
                        </tr>
                        <tr>
                            <td align=center colspan=2>
                                <input type=submit name=submit value="Set New Password">
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
        </table>

        <input type=hidden name=step value="<%= RegisterServlet.NEW_PASSWORD_PAGE %>">
        <input type=hidden name=token value="<%= token %>">
    </form>
    
</dspace:layout>
