<%--
  - review.jsp
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
  - Review submission page
  -
  - Attributes to pass in to this page:
  -    submission.info  - the SubmissionInfo object
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>

<%@ page import="org.dspace.app.webui.servlet.SubmitServlet" %>
<%@ page import="org.dspace.app.webui.util.SubmissionInfo" %>
<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.content.BitstreamFormat" %>
<%@ page import="org.dspace.content.DCDate" %>
<%@ page import="org.dspace.content.DCLanguage" %>
<%@ page import="org.dspace.content.DCValue" %>
<%@ page import="org.dspace.content.Item" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    SubmissionInfo si =
        (SubmissionInfo) request.getAttribute("submission.info");

    Item item = si.submission.getItem();

    // Names of each identifier type
    Map identifierQualNames = new HashMap();
    identifierQualNames.put( "govdoc", "Gov't Doc #" );
    identifierQualNames.put( "uri", "URI" );
    identifierQualNames.put( "isbn", "ISBN" );
    identifierQualNames.put( "issn", "ISSN" );
    identifierQualNames.put( "ismn", "ISMN" );
    identifierQualNames.put( "other", "Other" );
%>

<dspace:layout locbar="off" navbar="off" title="Verify Submission">

    <form action="<%= request.getContextPath() %>/submit" method=post>
        <jsp:include page="/submit/progressbar.jsp">
            <jsp:param name="current_stage" value="<%= SubmitServlet.REVIEW_SUBMISSION %>"/>
            <jsp:param name="stage_reached" value="<%= SubmitServlet.getStepReached(si) %>"/>
        </jsp:include>

        <H1>Submit: Verify Submission</H1>

        <P><strong>Not quite there yet,</strong> but nearly!</P>

        <P>Please spend a few minutes to examine what you've just submitted below.
        If anything is wrong, please go back and correct it by using the buttons
        next to the error, or by clicking on the progress bar at the top of the
        page.
        <dspace:popup page="/help/index.html#verify">(More Help...)</dspace:popup></P>

        <P><strong>If everything is OK,</strong> please click the "Next" button at the bottom of the page.</P>

        <P>You can safely check the files you've uploaded - a new window will
        be opened to display them.</P>

        <table align=center class=miscTable width=80%>
<%-- ====================================================== --%>
<%--                  INITIAL QUESTIONS                     --%>
<%-- ====================================================== --%>
            <tr>
                <td class="oddRowOddCol">
                    <table>
                        <tr>
                            <td width=100%>
                                <table>
                                    <tr>
                                        <td class=metadataFieldLabel>Item has more than one title:</td>
                                        <td class=metadataFieldValue><%= (si.submission.hasMultipleTitles() ? "Yes" : "No") %></td>
                                    </tr>
                                    <tr>
                                        <td class=metadataFieldLabel>Previously published item:</td>
                                        <td class=metadataFieldValue><%= (si.submission.isPublishedBefore() ? "Yes" : "No") %></td>
                                    </tr>
                                    <tr>
                                        <td class=metadataFieldLabel>Item consists of more than one file:</td>
                                        <td class=metadataFieldValue><%= (si.submission.hasMultipleFiles() ? "Yes" : "No") %></td>
                                    </tr>
                                </table>
                            </td>
                            <td valign=middle>
                                    <input type=submit name=submit_jump_<%= SubmitServlet.INITIAL_QUESTIONS %> value="Correct one of these">
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>

<%-- ====================================================== --%>
<%--             DESCRIBE ITEM PAGE 1 ELEMENTS              --%>
<%-- ====================================================== --%>
            <tr>
                <td class="evenRowOddCol">
                    <table>
                        <tr>
                            <td width=100%>
                                <table>
                                    <tr>
                                        <td class=metadataFieldLabel>Authors:</td>
                                        <td class=metadataFieldValue>
<%
    DCValue[] authors = item.getDC("contributor", "author", Item.ANY);
    if (authors.length == 0)
    {
%>
                                            <em>None</em>
<%
    }
    else
    {
        for (int i = 0; i < authors.length; i++)
        {                                    
%>
                                            <%= authors[i].value %><br>
<%
        }
    }
%>
                                        </td>
                                    </tr>
<%
    DCValue[] titles = item.getDC("title", null, Item.ANY);
    String title = "<em>None</em>";
    if (titles.length > 0)
    {
        title = titles[0].value;
    }
%>    
                                    <tr>
                                        <td class=metadataFieldLabel>Title:</td>
                                        <td class=metadataFieldValue>
                                            <%= title %>
                                        </td>
                                    </tr>
<%
    if (si.submission.hasMultipleTitles())
    {
        DCValue[] altTitles = item.getDC("title", "alternative", Item.ANY);
%>
                                    <tr>
                                        <td class=metadataFieldLabel>Alternative Titles:</td>
                                        <td class=metadataFieldValue>
<%
        if (altTitles.length == 0)
        {
%>
                                            <em>None</em>
<%
        }
        else
        {
            for(int i = 0; i < altTitles.length ; i++)
            {
%>
                                            <%= altTitles[i].value %><br>
<%
            }
        }
%>
                                        </td>
                                    </tr>
<%
    }

    if (si.submission.isPublishedBefore())
    {
        DCValue[] dateIssued = item.getDC("date", "issued", Item.ANY);
%>

                                    <tr>
                                        <td class=metadataFieldLabel>Date Issued:</td>
                                        <td class=metadataFieldValue>
<%
        if (dateIssued.length == 0)
        {
%>
                                            <em>None</em>
<%
        }
        else
        {
%>
                                            <dspace:date date="<%= new DCDate(dateIssued[0].value) %>" />
<%
        }
%>
                                        </td>
                                    </tr>
<%
        DCValue[] publisher = item.getDC("publisher", null, Item.ANY);
%>
                                    <tr>
                                        <td class=metadataFieldLabel>Publisher:</td>
                                        <td class=metadataFieldValue>
<%
        if (publisher.length == 0)
        {
%>
                                            <em>None</em>
<%
        }
        else
        {
%>
                                            <%= publisher[0].value %>
<%
        }
%>
                                        </td>
                                    </tr>
<%
        DCValue[] citation = item.getDC("identifier", "citation", Item.ANY);
%>
                                    <tr>
                                        <td class=metadataFieldLabel>Citation:</td>
                                        <td class=metadataFieldValue>
<%
        if (citation.length == 0)
        {
%>
                                            <em>None</em>
<%
        }
        else
        {
%>
                                            <%= citation[0].value %>
<%
        }
%>
                                        </td>
                                    </tr>
<%
    }
%>                                    
                                    <tr>
                                        <td class=metadataFieldLabel>Series/Report&nbsp;No:</td>
                                        <td class=metadataFieldValue>
<%
    DCValue[] seriesNumbers = item.getDC("relation","ispartofseries", Item.ANY);
    if (seriesNumbers.length == 0)
    {
%>
                                            <em>None</em>
<%
    }
    else
    {
        for (int i = 0; i < seriesNumbers.length ; i++)
        {
%>
                                            <%= seriesNumbers[i].value %><br>
<%
        }
    }
%>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class=metadataFieldLabel>Identifiers:</td>
                                        <td class=metadataFieldValue>
<%
    DCValue[] identifiers = item.getDC("identifier", Item.ANY, Item.ANY);

    for (int i = 0; i < identifiers.length; i++)
    {
        // Skip citation, handled above
        if (!identifiers[i].qualifier.equals("citation"))
        {
%>
                        <%= identifierQualNames.get(identifiers[i].qualifier) %>: <%= identifiers[i].value %><br>
<%
        }
    }
%>
                                        </td>
                                    </tr>
<%
    DCValue[] typeDC = item.getDC("type", null, Item.ANY);
    String type = "<em>None</em>";
    if (typeDC.length > 0)
    {
        type = typeDC[0].value;
    }
%>    
                                    <tr>
                                        <td class=metadataFieldLabel>Type:</td>
                                        <td class=metadataFieldValue>
                                            <%= type %>
                                        </td>
                                    </tr>
<%
    DCValue[] langArray = item.getDC("language", "iso", null);
    DCLanguage language = new DCLanguage("");
    if (langArray.length > 0)
    {
        language = new DCLanguage(langArray[0].value);
    }
%>
                                    <tr>
                                        <td class=metadataFieldLabel>Language:</td>
                                        <td class=metadataFieldValue><%= language.getDisplayName() %></td>
                                    </tr>
                                </table>
                            </td>
                            <td valign=middle>
                                    <input type=submit name=submit_jump_<%= SubmitServlet.EDIT_METADATA_1 %> value="Correct one of these">
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
<%-- ====================================================== --%>
<%--             DESCRIBE ITEM PAGE 2 ELEMENTS              --%>
<%-- ====================================================== --%>
            <tr>
                <td class=oddRowOddCol>
                    <table>
                        <tr>
                            <td width=100%>
                                <table>
                                    <tr>
                                        <td class=metadataFieldLabel>Keywords:</td>
                                        <td class=metadataFieldValue>
<%
    DCValue[] keywords = item.getDC("subject", null, Item.ANY);

    if (keywords.length == 0)
    {
%>
                                            <em>None</em>
<%
    }
    else
    {
        for (int i = 0; i < keywords.length; i++)
        {
%>
<%= keywords[i].value %><br>
<%
        }
    }

    DCValue[] abstr = item.getDC("description", "abstract", Item.ANY);
    DCValue[] sponsors = item.getDC("description", "sponsorship", Item.ANY);
    DCValue[] otherDesc = item.getDC("description", null, Item.ANY);
%>
                                        </td>
                                    </tr>                
                                    <tr>
                                        <td class=metadataFieldLabel>Abstract:</td>
                                        <td class=metadataFieldValue><%= (abstr.length == 0 ? "<em>None</em>" : abstr[0].value) %></td>
                                    </tr>
                                    <tr>
                                        <td class=metadataFieldLabel>Sponsors:</td>
                                        <td class=metadataFieldValue><%= (sponsors.length == 0 ? "<em>None</em>" : sponsors[0].value) %></td>
                                    </tr>
                                    <tr>
                                        <td class=metadataFieldLabel>Other&nbsp;Description:</td>
                                        <td class=metadataFieldValue><%= (otherDesc.length == 0 ? "<em>None</em>" : otherDesc[0].value) %></td>
                                    </tr>
                                </table>
                    </td>
                            <td valign=middle align=right>
                                    <input type=submit name=submit_jump_<%= SubmitServlet.EDIT_METADATA_2 %> value="Correct one of these">
                            </td>
                  </tr>
                </table>
                </td>
            </tr>
<%-- ====================================================== --%>
<%--                    UPLOADED_FILES                      --%>
<%-- ====================================================== --%>
            <tr>
                <td class=evenRowOddCol>
                    <table>
                        <tr>
                            <td width=100%>
                                <table>
                                    <tr>
                                        <td class=metadataFieldLabel><%= (si.submission.hasMultipleFiles() ? "Uploaded&nbsp;Files:" : "Uploaded&nbsp;File:") %></td>
                                        <td class=metadataFieldValue>
<%
    Bitstream[] bitstreams = item.getNonInternalBitstreams();

    for (int i = 0; i < bitstreams.length ; i++)
    {
        BitstreamFormat format = bitstreams[i].getFormat();
%>
                                            <A HREF="<%= request.getContextPath() %>/retrieve/<%= bitstreams[i].getID() %>/<%= java.net.URLEncoder.encode(bitstreams[i].getName()) %>" target="_blank"><%= bitstreams[i].getName() %></A> - <%= bitstreams[i].getFormatDescription() %>
<%
        switch (format.getSupportLevel())
        {
        case 0:
            %>(Unknown)<%
            break;
        case 1:
            %>(Unsupported)<%
            break;
        case 2:
            %>(Supported)<%
        }
%>        
                                            <br>
<%
    }
%>
                                        </td>
                                    </tr>
                                </table>
                    </td>
                            <td valign=middle align=right>
<%
    // Can't edit files in workflow mode
    if(!SubmitServlet.isWorkflow(si))
    {
%>
                                    <input type=submit name=submit_jump_<%= SubmitServlet.UPLOAD_FILES %>
                                     value="<%= (si.submission.hasMultipleFiles() ? "Add or Remove a File" : "Upload a different file") %>">
<%
    }
    else
    {
%>
                                    <input type=submit name=submit_jump_<%= SubmitServlet.UPLOAD_FILES %>
                                     value="Edit File Details">
<%
    }
%>
                            </td>
                  </tr>
                </table>
                </td>
            </tr>
                                    
        </table>    

        <%= SubmitServlet.getSubmissionParameters(si) %>
        <input type=hidden name=step value=<%= SubmitServlet.REVIEW_SUBMISSION %>>

        <P>&nbsp;</P>
    
        <center>
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
