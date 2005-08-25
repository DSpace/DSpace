<%--
  - upload-error.jsp
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
  - List of uploaded files
  -
  - Attributes to pass in to this page:
  -    submission.info  - the SubmissionInfo object
  -
  - FIXME: Merely iterates through bundles, treating all bit-streams as
  -        separate documents.  Shouldn't be a problem for early adopters.
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
%>

<dspace:layout locbar="off"
               navbar="off"
               titlekey="jsp.submit.upload-error.title"
               nocache="true">

    <form action="<%= request.getContextPath() %>/submit" method="post">

        <jsp:include page="/submit/progressbar.jsp">
            <jsp:param name="current_stage" value="<%= SubmitServlet.UPLOAD_FILES %>"/>
            <jsp:param name="stage_reached" value="<%= SubmitServlet.getStepReached(si) %>"/>
            <jsp:param name="md_pages" value="<%= si.numMetadataPages %>"/>
        </jsp:include>

        <%-- <h1>Submit: Error Uploading File</h1> --%>
		<h1><fmt:message key="jsp.submit.upload-error.heading"/></h1>

        <%-- <p>There was a problem uploading your file.  Either the filename you entered
        was incorrect, or there was a network problem which prevented the file from
        reaching us correctly.  Please try again.</p> --%>
		<p><fmt:message key="jsp.submit.upload-error.info"/></p>

        <%= SubmitServlet.getSubmissionParameters(si) %>
        <input type="hidden" name="step" value="<%= SubmitServlet.UPLOAD_ERROR %>" />
<%-- HACK: <center> tag needed for broken Netscape 4.78 behaviour --%>
        <center>
            <%-- <p><input type="submit" name="submit" value="Retry Upload" /></p> --%>
			<p><input type="submit" name="submit" value="<fmt:message key="jsp.submit.upload-error.retry.button"/>" /></p>
        </center>
    </form>

</dspace:layout>
