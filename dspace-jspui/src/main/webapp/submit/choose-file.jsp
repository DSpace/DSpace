<%--
  - choose-file.jsp
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
  - Select file to upload
  -
  - Attributes to pass in to this page:
  -    submission.info  - the SubmissionInfo object
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.app.webui.servlet.SubmitServlet" %>
<%@ page import="org.dspace.app.webui.util.SubmissionInfo" %>


<%
    SubmissionInfo si =
        (SubmissionInfo) request.getAttribute("submission.info");
%>


<dspace:layout locbar="off"
               navbar="off"
               titlekey="jsp.submit.choose-file.title"
               nocache="true">

    <form method="post" action="<%= request.getContextPath() %>/submit" enctype="multipart/form-data">

        <jsp:include page="/submit/progressbar.jsp">
            <jsp:param name="current_stage" value="<%= SubmitServlet.UPLOAD_FILES %>"/>
            <jsp:param name="stage_reached" value="<%= SubmitServlet.getStepReached(si) %>"/>
            <jsp:param name="md_pages" value="<%= si.numMetadataPages %>"/>
        </jsp:include>

        <%-- <h1>Submit: Upload a File</h1> --%>
		<h1><fmt:message key="jsp.submit.choose-file.heading"/></h1>
    
        <%-- <p>Please enter the name of
        <%= (si.submission.hasMultipleFiles() ? "one of the files" : "the file" ) %> on your
        local hard drive corresponding to your item.  If you click "Browse...", a
        new window will appear in which you can locate and select the file on your
        local hard drive. <object><dspace:popup page="/help/index.html#upload">(More Help...)</dspace:popup></object></p> --%>

		<div><fmt:message key="jsp.submit.choose-file.info1"/>
			<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") + \"#upload\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup></div>
        
        <%-- <p class="submitFormHelp"><strong>Netscape users please note:</strong> By
        default, the window brought up by clicking "Browse..." will only display
        files of type HTML.  If the file you are uploading isn't an HTML file,
        you will need to select the option to display files of other types.
        <object><dspace:popup page="/help/index.html#netscapeupload">Instructions for Netscape users</dspace:popup></object> are available.</p> --%>
		<div class="submitFormHelp"><fmt:message key="jsp.submit.choose-file.info3"/>
        <dspace:popup page="${helpPage}#netscapeupload"><fmt:message key="jsp.submit.choose-file.info4"/></dspace:popup></div>
        
<%-- FIXME: Collection-specific stuff should go here? --%>
        <%-- <p class="submitFormHelp">Please also note that the DSpace system is
        able to preserve the content of certain types of files better than other
        types.
        <object><dspace:popup page="/help/formats.jsp">Information about file types</dspace:popup></object> and levels of
        support for each are available.</p> --%>
        
		<p class="submitFormHelp"><fmt:message key="jsp.submit.choose-file.info6"/>
        <object><dspace:popup page="/help/formats.jsp"><fmt:message key="jsp.submit.choose-file.info7"/></dspace:popup></object>
        </p>
    
        <table border="0" align="center">
            <tr>
                <td class="submitFormLabel">
                    <%-- Document File: --%>
					<label for="tfile"><fmt:message key="jsp.submit.choose-file.document"/></label>
                </td>
                <td>
                    <input type="file" size="40" name="file" id="tfile" />
                </td>
            </tr>
<%
    if (si.submission.hasMultipleFiles())
    {
%>
            <tr>
                <td colspan="2">&nbsp;</td>
            </tr>
            <tr>
                <td class="submitFormHelp" colspan="2">
                    <%-- Please give a brief description of the contents of this file, for
                    example "Main article", or "Experiment data readings." --%>
					<fmt:message key="jsp.submit.choose-file.info9"/>
                </td>
            </tr>
            <tr>
                <%-- <td class="submitFormLabel">File Description:</td> --%>
				<td class="submitFormLabel"><label for="tdescription"><fmt:message key="jsp.submit.choose-file.filedescr"/></label></td>
                <td><input type="text" name="description" id="tdescription" size="40"/></td>
            </tr>
<%
    }
%>
        </table>
        
        <%= SubmitServlet.getSubmissionParameters(si) %>
        <input type="hidden" name="step" value="<%= SubmitServlet.CHOOSE_FILE %>" />
    
        <p>&nbsp;</p>

        <center>
            <table border="0" width="80%">
                <tr>
                    <td width="100%">&nbsp;</td>
                    <td>
                        <!-- <input type="submit" name="submit_prev" value="&lt; Previous"> -->
						<input type="submit" name="submit_prev" value="<fmt:message key="jsp.submit.general.previous"/>" />
                    </td>
                    <td>
                        <!-- <input type="submit" name="submit_next" value="Next &gt;"> -->
						<input type="submit" name="submit_next" value="<fmt:message key="jsp.submit.general.next"/>" />
                    </td>
                    <td>&nbsp;&nbsp;&nbsp;</td>
                    <td align="right">
                        <!-- <input type="submit" name="submit_cancel" value="Cancel/Save"> -->
						<input type="submit" name="submit_cancel" value="<fmt:message key="jsp.submit.general.cancel-or-save.button"/>" />
                    </td>
                </tr>
            </table>
        </center>  
    </form>

</dspace:layout>
