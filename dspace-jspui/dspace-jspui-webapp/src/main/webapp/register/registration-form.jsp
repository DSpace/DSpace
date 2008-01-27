<%--
  - registration-form.jsp
  -
  - Version: $Revision$
  -
  - Date: $Date$
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
  - Registration information form
  -
  - Form where new users enter their personal information and select a
  - password.
  -
  - Attributes to pass in:
  -
  -   eperson          - the EPerson who's registering
  -   token            - the token key they've been given for registering
  -   set.password     - if Boolean true, the user can set a password
  -   missing.fields   - if a Boolean true, the user hasn't entered enough
  -                      information on the form during a previous attempt
  -   password.problem - if a Boolean true, there's a problem with password
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>
	
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.app.webui.servlet.RegisterServlet" %>
<%@ page import="org.dspace.eperson.EPerson" %>

<%
    EPerson eperson = (EPerson) request.getAttribute( "eperson" );
    String token = (String) request.getAttribute("token");
    String netid = (String) request.getParameter("netid");
    String email = (String) request.getParameter("email");	

    Boolean attr = (Boolean) request.getAttribute("missing.fields");
    boolean missingFields = (attr != null && attr.booleanValue());

    attr = (Boolean) request.getAttribute("password.problem");
    boolean passwordProblem = (attr != null && attr.booleanValue());

    attr = (Boolean) request.getAttribute("set.password");
    boolean setPassword = (attr != null && attr.booleanValue());
%>

<dspace:layout titlekey="jsp.register.registration-form.title" nocache="true">

    <%-- <h1>Registration Information</h1> --%>
	<h1><fmt:message key="jsp.register.registration-form.title"/></h1>
    
<%
    if (missingFields)
    {
%>
    <%-- <p><strong>Please fill out all of the required fields.</strong></p> --%>
	<p><strong><fmt:message key="jsp.register.registration-form.instruct1"/></strong></p>
<%
    }

    if( passwordProblem)
    {
%>
    <%-- <p><strong>The passwords you enter below must match, and need to be at
    least 6 characters long.</strong></p> --%>
	<p><strong><fmt:message key="jsp.register.registration-form.instruct2"/></strong></p>
<%
    }
%>

    <%-- <p>Please enter the following information.  The fields marked with a * are
    required.</p> --%>
	<p><fmt:message key="jsp.register.registration-form.instruct3"/></p>
    <form action="<%= request.getContextPath() %>/register" method="post">
    <% if (netid!=null) { %> <input type="hidden" name="netid" value="<%= netid %>" /> <% } %>
    <% if (email!=null) { %> <input type="hidden" name="email" value="<%= email %>" /> <% } %>
        <dspace:include page="/register/profile-form.jsp" />
<%

    if (setPassword)
    {
%>
        <%-- <p>Please choose a password and enter it into the box below, and confirm it by typing it
        again into the second box.  It should be at least six characters long.</p> --%>
		<p><fmt:message key="jsp.register.registration-form.instruct4"/></p>

        <table class="misc" align="center">
            <tr>
                <td class="oddRowEvenCol">
                    <table border="0" cellpadding="5">
                        <tr>
                            <%-- <td align="right" class="standard"><strong>Password:</strong></td> --%>
							<td align="right" class="standard"><label for="tpassword"><strong><fmt:message key="jsp.register.registration-form.pswd.field"/></strong></label></td>
                            <td class="standard"><input type="password" name="password" id="tpassword" /></td>
                        </tr>
                        <tr>
                            <%-- <td align="right" class="standard"><strong>Again to Confirm:</strong></td> --%>
							<td align="right" class="standard"><label for="tpassword_confirm"><strong><fmt:message key="jsp.register.registration-form.confirm.field"/></strong></label></td>
                            <td class="standard"><input type="password" name="password_confirm" id="tpassword_confirm" /></td>
                        </tr>
                    </table>
                </td>
            </tr>
        </table>
<%
    }
%>

        <input type="hidden" name="step" value="<%= RegisterServlet.PERSONAL_INFO_PAGE %>"/>
        <input type="hidden" name="token" value="<%= token %>"/>
        
        <%-- <p align="center"><input type="submit" name="submit" value="Complete Registration"></p> --%>
		<p align="center"><input type="submit" name="submit" value="<fmt:message key="jsp.register.registration-form.complete.button"/>" /></p>
    </form>
</dspace:layout>
