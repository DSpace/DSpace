<%--
  - upload-file-list.jsp
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
  -   submission.info   - the SubmissionInfo object
  -   just.uploaded     - Boolean indicating if a file has just been uploaded
  -                       so a nice thank you can be displayed.
  -   show.checksums    - Boolean indicating whether to show checksums
  -
  - FIXME: Assumes each bitstream in a separate bundle.
  -        Shouldn't be a problem for early adopters.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.app.webui.servlet.SubmitServlet" %>
<%@ page import="org.dspace.app.webui.util.SubmissionInfo" %>
<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.content.BitstreamFormat" %>
<%@ page import="org.dspace.content.Bundle" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
    SubmissionInfo si =
        (SubmissionInfo) request.getAttribute("submission.info");

    boolean justUploaded = ((Boolean) request.getAttribute("just.uploaded")).booleanValue();
    boolean showChecksums = ((Boolean) request.getAttribute("show.checksums")).booleanValue();
%>

<dspace:layout locbar="off" navbar="off" titlekey="jsp.submit.upload-file-list.title">

    <form action="<%= request.getContextPath() %>/submit" method="post">

        <jsp:include page="/submit/progressbar.jsp">
            <jsp:param name="current_stage" value="<%= SubmitServlet.UPLOAD_FILES %>"/>
            <jsp:param name="stage_reached" value="<%= SubmitServlet.getStepReached(si) %>"/>
            <jsp:param name="md_pages" value="<%= si.numMetadataPages %>"/>
        </jsp:include>

<%--        <h1>Submit: <%= (justUploaded ? "File Uploaded Successfully" : "Uploaded Files") %></h1> --%>
    
<%
    if (justUploaded)
    {
%>
		<h1><fmt:message key="jsp.submit.upload-file-list.heading1"/></h1>
        <p><fmt:message key="jsp.submit.upload-file-list.info1"/></p>
<%
    }
    else
    {
%>
	    <h1><fmt:message key="jsp.submit.upload-file-list.heading2"/></h1>
<%
    }
%>
        <div><fmt:message key="jsp.submit.upload-file-list.info2"/>&nbsp;&nbsp;&nbsp;<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") + \"#uploadedfile\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup></div>
        
        <table class="miscTable" align="center" summary="Table dispalying your submitted files">
            <tr>
				<th id="t1" class="oddRowEvenCol"><fmt:message key="jsp.submit.upload-file-list.tableheading1"/></th>
                <th id="t2" class="oddRowOddCol"><fmt:message key="jsp.submit.upload-file-list.tableheading2"/></th>
                <th id="t3" class="oddRowEvenCol"><fmt:message key="jsp.submit.upload-file-list.tableheading3"/></th>
                <th id="t4" class="oddRowOddCol"><fmt:message key="jsp.submit.upload-file-list.tableheading4"/></th>
                <th id="t5" class="oddRowEvenCol"><fmt:message key="jsp.submit.upload-file-list.tableheading5"/></th>
<%
    if (showChecksums)
    {
%>

                <th id="t6" class="oddRowOddCol"><fmt:message key="jsp.submit.upload-file-list.tableheading6"/></th>
<%
    }
    
    // Don't display last column ("Remove") in workflow mode
    if (!SubmitServlet.isWorkflow(si))
    {
        // Whether it's an odd or even column depends on whether we're showing checksums
        String column = (showChecksums ? "Even" : "Odd");
%>
                <th id="t7" class="oddRow<%= column %>Col">&nbsp;</th>
<%
    }
%>
            </tr>

<%
    String row = "even";

    Bitstream[] bitstreams = si.submission.getItem().getNonInternalBitstreams();
    Bundle[] bundles = null;

    if (bitstreams[0] != null) {
        bundles = bitstreams[0].getBundles();
    }

    for (int i = 0; i < bitstreams.length; i++)
    {
        BitstreamFormat format = bitstreams[i].getFormat();
        String description = bitstreams[i].getFormatDescription();
        String supportLevel = LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.upload-file-list.supportlevel1");

        if(format.getSupportLevel() == 1)
        {
            supportLevel = LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.upload-file-list.supportlevel2");
        }

        if(format.getSupportLevel() == 0)
        {
            supportLevel = LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.upload-file-list.supportlevel3");
        }

        // Full param to dspace:popup must be single variable
        String supportLevelLink = "/help/formats.jsp#" + supportLevel;
%>
            <tr>
		<td headers="t1" class="<%= row %>RowEvenCol" align="center">
		    <input type="radio" name="primary_bitstream_id" value="<%= bitstreams[i].getID() %>"
			   <% if (bundles[0] != null) {
				if (bundles[0].getPrimaryBitstreamID() == bitstreams[i].getID()) { %>
			       	  <%="checked" %>
			   <%   }
			      } %> />
		</td>
                <td headers="t2" class="<%= row %>RowOddCol"><a href="<%= request.getContextPath() %>/retrieve/<%= bitstreams[i].getID() %>/<%= org.dspace.app.webui.util.UIUtil.encodeBitstreamName(bitstreams[i].getName()) %>" target="_blank"><%= bitstreams[i].getName() %></a></td>
                <td headers="t3" class="<%= row %>RowEvenCol"><%= bitstreams[i].getSize() %> bytes</td>
                <td headers="t4" class="<%= row %>RowOddCol">
                    <%= (bitstreams[i].getDescription() == null || bitstreams[i].getDescription().equals("")
                        ? LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.upload-file-list.empty1")
                        : bitstreams[i].getDescription()) %>
                    <input type="submit" name="submit_describe_<%= bitstreams[i].getID() %>" value="<fmt:message key="jsp.submit.upload-file-list.button1"/>" />
                </td>
                <td headers="t5" class="<%= row %>RowEvenCol">
                    <%= description %> <dspace:popup page="<%= supportLevelLink %>">(<%= supportLevel %>)</dspace:popup>
                    <input type="submit" name="submit_format_<%= bitstreams[i].getID() %>" value="<fmt:message key="jsp.submit.upload-file-list.button1"/>" />
                </td>
<%
        // Checksum
        if (showChecksums)
        {
%>
                <td headers="t6" class="<%= row %>RowOddCol">
                    <code><%= bitstreams[i].getChecksum() %> (<%= bitstreams[i].getChecksumAlgorithm() %>)</code>
                </td>
<%
        }

        // Don't display "remove" button in workflow mode
        if (!SubmitServlet.isWorkflow(si))
        {
            // Whether it's an odd or even column depends on whether we're showing checksums
            String column = (showChecksums ? "Even" : "Odd");
%>
                <td headers="t7" class="<%= row %>Row<%= column %>Col">
                    <input type="submit" name="submit_remove_<%= bitstreams[i].getID() %>" value="<fmt:message key="jsp.submit.upload-file-list.button2"/>" />
                </td>
<%
        }
%>
            </tr>
<%
        row = (row.equals("even") ? "odd" : "even");
    }
%>
        </table>

<%-- HACK:  Need a space - is there a nicer way to do this than <br> or a --%>
<%--        blank <p>? --%>
        <br />

<%-- Show information about how to verify correct upload, but not in workflow
     mode! --%>
<%
    if (!SubmitServlet.isWorkflow(si))
    {
%>
        <p class="uploadHelp"><fmt:message key="jsp.submit.upload-file-list.info3"/></p>
        <ul class="uploadHelp">
            <li class="uploadHelp"><fmt:message key="jsp.submit.upload-file-list.info4"/></li>
<%
        if (showChecksums)
        {
%>
            <li class="uploadHelp"><fmt:message key="jsp.submit.upload-file-list.info5"/>
            <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") + \"#checksum\"%>"><fmt:message key="jsp.submit.upload-file-list.help1"/></dspace:popup></li>
<%
        }
        else
        {
%>
            <li class="uploadHelp"><fmt:message key="jsp.submit.upload-file-list.info6"/>
            <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") + \"#checksum\"%>"><fmt:message key="jsp.submit.upload-file-list.help2"/></dspace:popup> <input type="submit" name="submit_show_checksums" value="<fmt:message key="jsp.submit.upload-file-list.button3"/>" /></li>
<%
        }
%>
        </ul>
        <br />
<%
    }
%>    

<%-- Hidden fields needed for submit servlet to know which item to deal with --%>
        <%= SubmitServlet.getSubmissionParameters(si) %>
        <input type="hidden" name="step" value="<%= SubmitServlet.FILE_LIST %>" />

<%-- HACK: Center used to align table; CSS and align="center" ignored by some browsers --%>
        <center>
<%
    // Don't allow files to be added in workflow mode
    if (!SubmitServlet.isWorkflow(si))
    {
%>
            <p><input type="submit" name="submit_more" value="<fmt:message key="jsp.submit.upload-file-list.button4"/>" /></p>
<%
    }
%>
            <table border="0" width="80%">
                <tr>
                    <td width="100%">&nbsp;</td>
                    <td>
                        <input type="submit" name="submit_prev" value="<fmt:message key="jsp.submit.upload-file-list.button5"/>" />
                    </td>
                    <td>
                        <input type="submit" name="submit_next" value="<fmt:message key="jsp.submit.upload-file-list.button6"/>" />
                    </td>
                    <td>&nbsp;&nbsp;&nbsp;</td>
                    <td align="right">
                        <input type="submit" name="submit_cancel" value="<fmt:message key="jsp.submit.upload-file-list.button7"/>" />
                    </td>
                </tr>
            </table>
        </center>

    </form>

</dspace:layout>
