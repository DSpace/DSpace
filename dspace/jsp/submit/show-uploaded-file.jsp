<%--
  - show-uploaded-file.jsp
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
  - Show uploaded file (single-file submission mode)
  -
  - Attributes to pass in
  -    submission.info  - the SubmissionInfo object
  -    just.uploaded    - Boolean indicating whether the user has just
  -                       uploaded a file OK
  -    show.checksums   - Boolean indicating whether to show checksums
  -
  - FIXME: Merely iterates through bundles, treating all bit-streams as
  -        separate documents.  Shouldn't be a problem for early adopters.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="org.dspace.app.webui.servlet.SubmitServlet" %>
<%@ page import="org.dspace.app.webui.util.SubmissionInfo" %>
<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.content.BitstreamFormat" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    SubmissionInfo si =
        (SubmissionInfo) request.getAttribute("submission.info");

    boolean justUploaded = ((Boolean) request.getAttribute("just.uploaded")).booleanValue();
    boolean showChecksums = ((Boolean) request.getAttribute("show.checksums")).booleanValue();

    // Get the bitstream
    Bitstream[] all = si.submission.getItem().getNonInternalBitstreams();
    Bitstream bitstream = all[0];
    BitstreamFormat format = bitstream.getFormat();
%>

<dspace:layout locbar="off" navbar="off" title="Uploaded File" nocache="true">

    <form action="<%= request.getContextPath() %>/submit" method=post>

        <jsp:include page="/submit/progressbar.jsp">
            <jsp:param name="current_stage" value="<%= SubmitServlet.UPLOAD_FILES %>"/>
            <jsp:param name="stage_reached" value="<%= SubmitServlet.getStepReached(si) %>"/>
            <jsp:param name="md_pages" value="<%= si.numMetadataPages %>"/>
        </jsp:include>

<%
    if (justUploaded)
    {
%>
        <H1>Submit: File Uploaded Successfully</H1>

        <P><strong>Your file was successfully uploaded.</strong></P>
<%
    }
    else
    {
%>
        <H1>Submit: Uploaded File</H1>
<%
    }
%>
        <P>Here are the details of the file you have uploaded.  Please check the
        details before going to the next step.
        <dspace:popup page="/help/index.html#uploadedfile">(More Help...)</dspace:popup></P>

        <table class="miscTable" align=center>
            <tr>
                <th class="oddRowOddCol">File</th>
                <th class="oddRowEvenCol">Size</th>
                <th class="oddRowOddCol">File Format</th>
<%
    if (showChecksums)
    {
%>
                <th class="oddRowEvenCol">Checksum</th>
<%
    }
%>
            </tr>
            <tr>
                <td class="evenRowOddCol"><A HREF="<%= request.getContextPath() %>/retrieve/<%= bitstream.getID() %>/<%= org.dspace.app.webui.util.UIUtil.encodeBitstreamName(bitstream.getName()) %>" target="_blank"><%= bitstream.getName() %></A></td>
                <td class="evenRowEvenCol"><%= bitstream.getSize() %> bytes</td>
                <td class="evenRowOddCol">
<%
    String description = bitstream.getFormatDescription();
    String supportLevel = "supported";

    if (format.getSupportLevel() == 1)
    {
        supportLevel = "known";
    }

    if (format.getSupportLevel() == 0)
    {
        supportLevel = "unsupported";
    }

    // Full param to dspace:popup must be single variable
    String supportLevelLink = "/help/formats.jsp#" + supportLevel;
%>
                    <%= description %> <dspace:popup page="<%= supportLevelLink %>">(<%= supportLevel %>)</dspace:popup>
                </td>
<%
    if (showChecksums)
    {
%>
                <td class="evenRowEvenCol">
                    <code><%= bitstream.getChecksum() %> (<%= bitstream.getChecksumAlgorithm() %>)</code>
                </td>
<%
    }
%>
            </tr>
        </table>

        <center>
            <P>
                <input type=submit name="submit_format_<%= bitstream.getID() %>" value="Click here if this is the wrong format">
            </P>
        </center>
        
        <center>
            <P>
                <input type=submit name="submit_remove_<%= bitstream.getID() %>" value="Click here if this is the wrong file">
            </P>
        </center>

        <BR>        
<%-- Show information about how to verify correct upload --%>
        <P class="uploadHelp">You can verify that the file has been uploaded correctly by:</P>
        <UL class="uploadHelp">
            <LI class="uploadHelp">Clicking on the filename above.  This will download the file in a
            new browser window, so that you can check the contents.</LI>
<%
    if (showChecksums)
    {
%>
            <LI class="uploadHelp">Comparing the checksum displayed above with a checksum worked out on
            your local computer.  They should be exactly the same.
            <dspace:popup page="/help/index.html#checksum">Click here to find out how to do this.</dspace:popup></LI>
<%
    }
    else
    {
%>
            <LI class="uploadHelp">The system can calculate a checksum you can verify.
            <dspace:popup page="/help/index.html#checksum">Click here for more information.</dspace:popup> <input type=submit name=submit_show_checksums value="Show checksums"></LI>
<%
    }
%>
        </UL>
        <BR>

<%-- Hidden fields needed for submit servlet to know which item to deal with --%>
        <%= SubmitServlet.getSubmissionParameters(si) %>
        <input type=hidden name=step value=<%= SubmitServlet.FILE_LIST %>>

<%-- HACK: Center used to align table; CSS and align=center ignored by some browsers --%>
        <center>
            <table border=0 width=80%>
                <tr>
                    <td width="100%">&nbsp;</td>
                    <td align>
                        <input type=submit name=submit_prev value="&lt; Previous">
                    </td>
                    <td>
                        <input type=submit name=submit_next value="Next &gt;">
                    </td>
                    <td>&nbsp;&nbsp;&nbsp;</td>
                    <td align=right>
                        <input type=submit name=submit_cancel value="Cancel/Save">
                    </td>
                </tr>
            </table>
        </center>
    </form>

</dspace:layout>
        
