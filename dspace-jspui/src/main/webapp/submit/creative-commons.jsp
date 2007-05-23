<%--
  - creative-commons.jsp
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
  -    cclicense.exists   - boolean to indicate CC license already exists
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

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
    String ssURL = baseURL + "/submit/creative-commons.css";
    String exitURL = baseURL + "/submit/cc-license.jsp?license_url=[license_url]";
    Boolean lExists = (Boolean)request.getAttribute("cclicense.exists");
    boolean licenseExists = (lExists == null ? false : lExists.booleanValue());
%>

<dspace:layout locbar="off"
               navbar="off"
               titlekey="jsp.submit.creative-commons.title"
               nocache="true">

    <form name="foo" id="license_form" action="<%= request.getContextPath() %>/submit" method="post">

        <jsp:include page="/submit/progressbar.jsp">
            <jsp:param name="current_stage" value="<%= SubmitServlet.CC_LICENSE %>"/>
            <jsp:param name="stage_reached" value="<%= SubmitServlet.getStepReached(si) %>"/>
            <jsp:param name="md_pages" value="<%= si.numMetadataPages %>"/>
        </jsp:include>

        <%-- <h1>Submit: Use a Creative Commons License</h1> --%>
		<h1><fmt:message key="jsp.submit.creative-commons.heading"/></h1>
<br />

<%
        if (licenseExists)
        {
%>
        <%-- <p>You have already chosen a Creative Commons license and added it to this item.
        You may:</p> --%>
		<p><fmt:message key="jsp.submit.creative-commons.info1"/></p>
    <%-- <ul>
            <li>Press the 'Next' button below to <em>keep</em> the license previously chosen.</li>
            <li>Press the 'Skip Creative Commons' button below to <em>remove</em> the current choice, and forego a Creative Commons license.</li>
            <li>Complete the selection process below to <em>replace</em> the current choice.</li>
         </ul> --%>
		 <ul>
            <li><fmt:message key="jsp.submit.creative-commons.choice1"/></li>
            <li><fmt:message key="jsp.submit.creative-commons.choice2"/></li>
            <li><fmt:message key="jsp.submit.creative-commons.choice3"/></li>
         </ul>
<%
        }
        else
        {
%>
        <%-- <p>To license your Item under Creative Commons, follow the instructions below. You will be given an opportunity to review your selection.
        Follow the 'proceed' link to add the license. If you wish to omit a Creative Commons license, press the 'Skip Creative Commons' button.</p> --%>
		<p><fmt:message key="jsp.submit.creative-commons.info2"/></p>
<%
        }
%>  

	<%-- <iframe src="http://creativecommons.org/license/?partner=dspace&stylesheet=<%= java.net.URLEncoder.encode(ssURL) %>&exit_url=<%= java.net.URLEncoder.encode(exitURL) %>" width="100%" height="540">Your browser must support IFrames to use this feature
	</iframe> --%>
	<iframe src="http://creativecommons.org/license/?partner=dspace&amp;stylesheet=<%= java.net.URLEncoder.encode(ssURL) %>&amp;exit_url=<%= java.net.URLEncoder.encode(exitURL) %>" width="100%" height="540"><fmt:message key="jsp.submit.creative-commons.info3"/>
	</iframe>

        <%= SubmitServlet.getSubmissionParameters(si) %>
    <input type="hidden" name="step" value="<%= SubmitServlet.CC_LICENSE %>" />
	<input type="hidden" name="cc_license_url" value="" />
	<input type="hidden" name="submit_grant" value="I Grant the License" />
        <center>
            <table border="0" width="80%">
                <tr>
                    <td width="100%">&nbsp;</td>
                    <td>
                        <%-- <input type="submit" name="submit_prev" value="&lt; Previous"> --%>
						<input type="submit" name="submit_prev" value="<fmt:message key="jsp.submit.general.previous"/>" />
                    </td>
<%
     if (licenseExists)
     {
%>
                    <td>
                        <%-- <input type="submit" name="submit_next" value="Next &gt;"> --%>
						<input type="submit" name="submit_next" value="<fmt:message key="jsp.submit.general.next"/>" />
                    </td>
<%
     }
%>
                    <td>
                        <%-- <input type="submit" name="submit_no_cc" value="Skip Creative Commons &gt;"/> --%>
						<input type="submit" name="submit_no_cc" value="<fmt:message key="jsp.submit.creative-commons.skip.button"/>"/>
                    </td>
                    <td>&nbsp;&nbsp;&nbsp;</td>
                    <td align="right">
                        <%-- <input type="submit" name="submit_cancel" value="Cancel/Save"/> --%>
						<input type="submit" name="submit_cancel" value="<fmt:message key="jsp.submit.general.cancel-or-save.button"/>"/>
                    </td>
                </tr>
            </table>
        </center>
    </form>
</dspace:layout>
