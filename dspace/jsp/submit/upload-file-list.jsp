<%--
  - upload-file-list.jsp
  -
  - Version: $Revision$
  -
  - Date: $Date$
  -
  - Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
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
%>

<dspace:layout locbar="nolink" navbar="off" parenttitle="Submit"
title="Uploaded Files">

    <form action="<%= request.getContextPath() %>/submit" method=post>

        <jsp:include page="/submit/progressbar.jsp">
            <jsp:param name="current_stage" value="<%= SubmitServlet.UPLOAD_FILES %>"/>
            <jsp:param name="stage_reached" value="<%= SubmitServlet.getStepReached(si) %>"/>
        </jsp:include>

        <H1>Submit: <%= (justUploaded ? "File Uploaded Successfully" : "Uploaded Files") %></H1>
    
<%
    if (justUploaded)
    {
%>
        <P><strong>Your file was successfully uploaded.</strong></P>
<%
    }
%>
        <P>The table below shows the files you have uploaded for this item. <A TARGET="dspace.help" HREF="<%= request.getContextPath() %>/help/index.html#uploadedfile">(More Help...)</A></P>
        
        <table class="miscTable" align=center>
            <tr>
                <th class="oddRowOddCol">File</th>
                <th class="oddRowEvenCol">Size</th>
                <th class="oddRowOddCol">Description</th>
                <th class="oddRowEvenCol">File Format</th>
<%
    if (showChecksums)
    {
%>
                <th class="oddRowOddCol">Checksum</th>
<%
    }
    
    // Don't display last column ("Remove") in workflow mode
    if (!SubmitServlet.isWorkflow(si))
    {
        // Whether it's an odd or even column depends on whether we're showing checksums
        String column = (showChecksums ? "Even" : "Odd");
%>
                <th class="oddRow<%= column %>Col">&nbsp;</th>
<%
    }
%>
            </tr>

<%
    String row = "even";

    Bitstream[] bitstreams = si.submission.getItem().getNonInternalBitstreams();

    for (int i = 0; i < bitstreams.length; i++)
    {
        BitstreamFormat format = bitstreams[i].getFormat();
        String description = bitstreams[i].getFormatDescription();
        String supportLevel = "supported";

        if(format.getSupportLevel() == 1)
        {
            supportLevel = "known";
        }

        if(format.getSupportLevel() == 0)
        {
            supportLevel = "unsupported";
        }

%>
            <tr>
                <td class="<%= row %>RowOddCol"><A HREF="<%= request.getContextPath() %>/retrieve/<%= bitstreams[i].getID() %>" target="_blank"><%= bitstreams[i].getName() %></A></td>
                <td class="<%= row %>RowEvenCol"><%= bitstreams[i].getSize() %> bytes</td>
                <td class="<%= row %>RowOddCol">
                    <%= (bitstreams[i].getDescription() == null || bitstreams[i].getDescription().equals("")
                        ? "<em>None</em>"
                        : bitstreams[i].getDescription()) %>
                    <input type=submit name="submit_describe_<%= bitstreams[i].getID() %>" value="Change">
                </td>
                <td class="<%= row %>RowEvenCol">
                    <%= description %> <A TARGET="dspace.help" HREF="<%= request.getContextPath() %>/help/formats.html#<%= supportLevel %>">(<%= supportLevel %>)</A>
                    <input type=submit name="submit_format_<%= bitstreams[i].getID() %>" value="Change">
                </td>
<%
        // Checksum
        if (showChecksums)
        {
%>
                <td class="<%= row %>RowOddCol">
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
                <td class="<%= row %>Row<%= column %>Col">
                    <input type=submit name="submit_remove_<%= bitstreams[i].getID() %>" value="Remove">
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

<%-- HACK:  Need a space - is there a nicer way to do this than <BR> or a --%>
<%--        blank <P>? --%>
        <BR>

<%-- Show information about how to verify correct upload, but not in workflow
     mode! --%>
<%
    if (SubmitServlet.isWorkflow(si))
    {
%>
        <P class="uploadHelp">You can verify that the file(s) have been uploaded correctly by:</P>
        <UL class="uploadHelp">
            <LI class="uploadHelp">Clicking on the filenames above.  This will download the file in a
            new browser window, so that you can check the contents.</LI>
<%
        if (showChecksums)
        {
%>
            <LI class="uploadHelp">Comparing checksums displayed above with checksums worked out on
            your local computer.  They should be exactly the same. <A target="dspace.help" HREF="<%= request.getContextPath() %>/help/index.html#checksum">Click
            here to find out how to do this.</A></LI>
<%
        }
        else
        {
%>
            <LI class="uploadHelp">The system can calculate a checksum you can verify.  <A target="dspace.help" HREF="<%= request.getContextPath() %>/help/index.html#checksum">Click
            here for more information.</A> <input type=submit name=submit_show_checksums value="Show checksums"></LI>
<%
        }
%>
        </UL>

        <BR>
<%
    }
%>    

<%-- Hidden fields needed for submit servlet to know which item to deal with --%>
        <%= SubmitServlet.getSubmissionParameters(si) %>
        <input type=hidden name=step value=<%= SubmitServlet.FILE_LIST %>>

<%-- HACK: Center used to align table; CSS and align=center ignored by some browsers --%>
        <center>
<%
    // Don't allow files to be added in workflow mode
    if (!SubmitServlet.isWorkflow(si))
    {
%>
            <p><input type=submit name=submit_more value="Add Another File"></p>
<%
    }
%>
            <table border=0 width=80%>
                <tr>
                    <td width="100%">&nbsp;</td>
                    <td>
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
