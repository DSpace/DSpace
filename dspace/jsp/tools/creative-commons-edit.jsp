<%--
  - creative-commons-edit.jsp
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

<%@ page import="org.dspace.app.webui.servlet.SubmitServlet" %>
<%@ page import="org.dspace.app.webui.util.SubmissionInfo" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
 

    String reqURL = request.getRequestURL().toString();
    int firstIndex = reqURL.indexOf("://") + 3;
    int secondIndex = reqURL.indexOf("/", firstIndex);
    String baseURL = reqURL.substring(0, secondIndex) + request.getContextPath();
    String ssURL = baseURL + "/submit/creative-commons.css";
    // Use the submit process' cc-license component
    String exitURL = baseURL + "/submit/cc-license.jsp?license_url=[license_url]";
%>

<dspace:layout navbar="admin"
               locbar="link"
               parentlink="/dspace-admin"
               parenttitlekey="jsp.administer"
               titlekey="jsp.tools.creative-commons-edit.title" nocache="true">
<table cellspacing="8" cellpadding="24" class="pagecontent">
  <tr>
   <td>
    <h1><fmt:message key="jsp.tools.creative-commons-edit.heading1"/></h1>
    <form name="ccform" id="license_form" action="" method="get">
	<iframe src="http://creativecommons.org/license/?partner=dspace&amp;stylesheet=<%= java.net.URLEncoder.encode(ssURL) %>&amp;exit_url=<%= java.net.URLEncoder.encode(exitURL) %>" width="100%" height="540">Your browser must support IFrames to use this feature
	</iframe>

        <input type="hidden" name="item_id" value='<%=request.getParameter("item_id")%>' />
        <input type="hidden" name="cc_license_url" value="" />
    </form> 
	</td>
  </tr>
</table>
</dspace:layout>
