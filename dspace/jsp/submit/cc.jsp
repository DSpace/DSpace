<%--
  - cc.jsp
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
  - Show the user a license which they may grant or reject
  -
  - Attributes to pass in:
  -    submission.info  - the SubmissionInfo object
  -    license          - the license text to display
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="org.dspace.app.webui.servlet.SubmitServlet" %>
<%@ page import="org.dspace.app.webui.util.SubmissionInfo" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    SubmissionInfo si =
        (SubmissionInfo) request.getAttribute("submission.info");

    String license = (String) request.getAttribute("license");
    String reqURL = request.getRequestURL().toString();
    int firstIndex = reqURL.indexOf("://") + 3;
    int secondIndex = reqURL.indexOf("/", firstIndex);
    String baseURL = reqURL.substring(0, secondIndex) + request.getContextPath();
    String ssURL = baseURL + "/submit/cc.css";
    String exitURL = baseURL + "/submit/cc-license.jsp?license_url=[license_url]";
%>

<dspace:layout locbar="off" navbar="off" title="DSpace Distribution License">

    <form name="foo" id="license_form" action="<%= request.getContextPath() %>/submit" method=post>

        <jsp:include page="/submit/progressbar.jsp">
            <jsp:param name="current_stage" value="<%= SubmitServlet.CC_LICENSE %>"/>
            <jsp:param name="stage_reached" value="<%= SubmitServlet.getStepReached(si) %>"/>
        </jsp:include>

        <H1>Submit: Use a Creative Commons License</H1>

	<INPUT TYPE="submit" name="submit_cancel" value="Cancel/Save" />

	<INPUT TYPE="submit" name="submit_no_cc" value="Don't Use Creative Commons" />
<br />

	<IFRAME src="http://creativecommons.org/license/?partner=dspace&stylesheet=<%= java.net.URLEncoder.encode(ssURL) %>&exit_url=<%= java.net.URLEncoder.encode(exitURL) %>" width="100%" height="600">Your browser must support IFrames to use this feature
	</IFRAME>
        <%= SubmitServlet.getSubmissionParameters(si) %>
        <input type=hidden name=step value=<%= SubmitServlet.CC_LICENSE %>>
	<input type=hidden name=cc_license_url value="" />
	<input type=hidden name=submit_grant value="I Grant the License" />

    </form>
</dspace:layout>
