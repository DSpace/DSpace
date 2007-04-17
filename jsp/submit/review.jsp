<%--
  - review.jsp
  -
  - Version: $Revision: 1.15 $
  -
  - Date: $Date: 2005/03/11 04:49:53 $
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
  -    submission.inputs - the DCInputSet object
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="java.io.IOException" %>

<%@ page import="org.dspace.app.webui.servlet.SubmitServlet" %>
<%@ page import="org.dspace.app.webui.util.SubmissionInfo" %>
<%@ page import="org.dspace.content.InProgressSubmission" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.app.webui.util.DCInputSet" %>
<%@ page import="org.dspace.app.webui.util.DCInput" %>
<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.content.BitstreamFormat" %>
<%@ page import="org.dspace.content.DCDate" %>
<%@ page import="org.dspace.content.DCLanguage" %>
<%@ page import="org.dspace.content.DCValue" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.core.Utils" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    SubmissionInfo si =
        (SubmissionInfo) request.getAttribute("submission.info");

    Item item = si.submission.getItem();
    
    DCInputSet inputSet =
    	(DCInputSet) request.getAttribute("submission.inputs");
%>

<%!

    void layoutSection(HttpServletRequest request, 
                       javax.servlet.jsp.JspWriter out,
                       DCInputSet inputSet,
                       SubmissionInfo si,
                       Item item, int pageNum)
        throws ServletException, IOException
    {
       InProgressSubmission ip = si.submission;
	   DCInput[] inputs = inputSet.getPageRows(pageNum,
	                                           ip.hasMultipleTitles(),
	                                           ip.isPublishedBefore());  
       for (int z = 0; z < inputs.length; z++) 
       { 
          String inputType = inputs[z].getInputType();
          String pairsName = inputs[z].getPairsType();
          String value;
          DCValue[] values;
          StringBuffer row = new StringBuffer();
          
          row.append("<tr>");
          row.append("<td class=metadataFieldLabel>");
          row.append(inputs[z].getLabel());
          row.append("</td>");
          row.append("<td class=metadataFieldValue>");

          if (inputType.equals("qualdrop_value"))
          {
             values = item.getDC(inputs[z].getElement(), Item.ANY, Item.ANY);
          }
          else
          {
             values = item.getDC(inputs[z].getElement(), inputs[z].getQualifier(), Item.ANY);
          }
          if (values.length == 0) 
          {
             row.append("<em>None</em>").append("</td>").append("</tr>");
          }
          else 
          {
             for (int i = 0; i < values.length; i++)
             {
                if (inputType.equals("date"))
                {
                   DCDate date = new DCDate(values[i].value);
                   row.append(UIUtil.displayDate(date, false, true));
                }
                else if (inputType.equals("dropdown"))
                {
                   String storedVal = values[i].value;
                   String displayVal = inputs[z].getDisplayString(pairsName,
                                                                storedVal);
                   row.append(Utils.addEntities(displayVal));
                }
                else if (inputType.equals("qualdrop_value"))
                {
                   String qual = values[i].qualifier;
                   String displayQual = inputs[z].getDisplayString(pairsName, 
                                                                 qual);
                   String displayValue = Utils.addEntities(values[i].value);
                   if (displayQual != null)
                   {
                       row.append(displayQual + ":" + displayValue);
                   }
                }
                else 
                {
                   row.append(Utils.addEntities(values[i].value));
                }
                row.append("<br />");
             }
          }
          row.append("</td>");
          row.append("</tr>");
   
          out.write(row.toString());
       }
    }
%>

<dspace:layout locbar="off" navbar="off" title="Verify Submission" nocache="true">

    <form action="<%= request.getContextPath() %>/submit" method=post>
        <jsp:include page="/submit/progressbar.jsp">
            <jsp:param name="current_stage" value="<%= SubmitServlet.REVIEW_SUBMISSION %>"/>
            <jsp:param name="stage_reached" value="<%= SubmitServlet.getStepReached(si) %>"/>
            <jsp:param name="md_pages" value="<%= si.numMetadataPages %>"/>
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
<%--             DESCRIBE ITEM ELEMENTS                     --%>
<%-- ====================================================== --%>
<%
     for ( int i = 0; i < inputSet.getNumberPages(); i++ )
     {
%>
            <tr>
                <td class="evenRowOddCol">
                    <table>
                        <tr>
                            <td width=100%>
                                <table>

<%
    layoutSection(request, out, inputSet, si, item, i);
%>
                                </table>
                            </td>
                            <td valign=middle>
                                 <input type=submit name=submit_jump_<%= SubmitServlet.EDIT_METADATA_1 + i %> value="Correct one of these">
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
<%
	}
%>
<%-- ====================================================== --%>
<%--             DESCRIBE ITEM PAGE 2 ELEMENTS              --%>
<%-- ====================================================== 
            <tr>
                <td class=oddRowOddCol>
                    <table>
                        <tr>
                            <td width=100%>
                                <table>
    layoutSection(request, out, inputSet, si, item, 1);
                                </table>
                    </td>
                            <td valign=middle align=right>
                                    <input type=submit name=submit_jump_<%= SubmitServlet.EDIT_METADATA_2 %> value="Correct one of these">
                            </td>
                  </tr>
                </table>
                </td>
            </tr>
--%>
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
        // Work out whether to use /retrieve link for simple downloading,
        // or /html link for HTML files
        BitstreamFormat format = bitstreams[i].getFormat();
        String downloadLink = "retrieve/" + bitstreams[i].getID();
        if (format != null && format.getMIMEType().equals("text/html"))
        {
            downloadLink = "html/db-id/" + item.getID();
        }
%>
                                            <A HREF="<%= request.getContextPath() %>/<%= downloadLink %>/<%= UIUtil.encodeBitstreamName(bitstreams[i].getName()) %>" target="_blank"><%= bitstreams[i].getName() %></A> - <%= bitstreams[i].getFormatDescription() %>
<%
        switch (format.getSupportLevel())
        {
        case 0:
            %>(Unknown)<%
            break;
        case 1:
            %>(Known)<%
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
