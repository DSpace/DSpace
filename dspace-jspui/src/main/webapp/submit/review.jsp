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
  -    submission.inputs - the DCInputSet object
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="java.io.IOException" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="javax.servlet.jsp.PageContext" %>

<%@ page import="org.dspace.app.webui.servlet.SubmitServlet" %>
<%@ page import="org.dspace.app.util.DCInputSet" %>
<%@ page import="org.dspace.app.util.DCInput" %>
<%@ page import="org.dspace.app.webui.util.SubmissionInfo" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.content.BitstreamFormat" %>
<%@ page import="org.dspace.content.DCDate" %>
<%@ page import="org.dspace.content.DCLanguage" %>
<%@ page import="org.dspace.content.DCValue" %>
<%@ page import="org.dspace.content.InProgressSubmission" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.core.Utils" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

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
                       Item item,
                       int pageNum,
                       PageContext pageContext)
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
          row.append("<td class=\"metadataFieldLabel\">");
          row.append(inputs[z].getLabel());
          row.append("</td>");
          row.append("<td class=\"metadataFieldValue\">");

          if (inputType.equals("qualdrop_value"))
          {
             values = item.getMetadata(inputs[z].getSchema(), inputs[z].getElement(), Item.ANY, Item.ANY);
          }
          else
          {
             values = item.getMetadata(inputs[z].getSchema(), inputs[z].getElement(), inputs[z].getQualifier(), Item.ANY);
          }
          if (values.length == 0) 
          {
             row.append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.no_md"));
          }
          else 
          {
             for (int i = 0; i < values.length; i++)
             {
                if (inputType.equals("date"))
                {
                   DCDate date = new DCDate(values[i].value);
                   row.append(UIUtil.displayDate(date, false, true, request));
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

<dspace:layout locbar="off" navbar="off" titlekey="jsp.submit.review.title" nocache="true">

    <form action="<%= request.getContextPath() %>/submit" method="post">
        <jsp:include page="/submit/progressbar.jsp">
            <jsp:param name="current_stage" value="<%= SubmitServlet.REVIEW_SUBMISSION %>"/>
            <jsp:param name="stage_reached" value="<%= SubmitServlet.getStepReached(si) %>"/>
            <jsp:param name="md_pages" value="<%= si.numMetadataPages %>"/>
        </jsp:include>
        <h1><fmt:message key="jsp.submit.review.heading"/></h1>

        <p><fmt:message key="jsp.submit.review.info1"/></p>

        <div><fmt:message key="jsp.submit.review.info2"/>
        &nbsp;&nbsp;<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") + \"#verify\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup></div>

        <p><fmt:message key="jsp.submit.review.info3"/></p>

        <p><fmt:message key="jsp.submit.review.info4"/></p>

        <table align="center" class="miscTable" width="80%">
<%-- ====================================================== --%>
<%--                  INITIAL QUESTIONS                     --%>
<%-- ====================================================== --%>
            <tr>
                <td class="oddRowOddCol">
                    <table>
                        <tr>
                            <td width="100%">
                                <table>
                                    <tr>
                                        <td class="metadataFieldLabel"><fmt:message key="jsp.submit.review.init-question1"/></td>
                                        <td class="metadataFieldValue"><%= (si.submission.hasMultipleTitles() ? LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.state1") : LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.state2")) %></td>
                                    </tr>
                                    <tr>
                                        <td class="metadataFieldLabel"><fmt:message key="jsp.submit.review.init-question2"/></td>
                                        <td class="metadataFieldValue"><%= (si.submission.isPublishedBefore() ? LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.state1") : LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.state2")) %></td>
                                    </tr>
                                    <tr>
                                        <td class="metadataFieldLabel"><fmt:message key="jsp.submit.review.init-question3"/></td>
                                        <td class="metadataFieldValue"><%= (si.submission.hasMultipleFiles() ? LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.state1") : LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.state2")) %></td>
                                    </tr>
                                </table>
                            </td>
                            <td valign="middle">
                                    <input type="submit" name="submit_jump_<%= SubmitServlet.INITIAL_QUESTIONS %>" value="<fmt:message key="jsp.submit.review.button.correct"/>" />
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
                            <td width="100%">
                                <table>

<%
	    layoutSection(request, out, inputSet, si, item, i, pageContext);
	%>
					</table>
				    </td>
				    <td valign="middle">
					 <input type="submit" name="submit_jump_<%= SubmitServlet.EDIT_METADATA_1 + i %>" value="<fmt:message key="jsp.submit.review.button.correct"/>" />
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
			<td class="oddRowOddCol">
			    <table>
				<tr>
				    <td width="100%">
					<table>
	    layoutSection(request, out, inputSet, si, item, 1, pageContext);
                                </table>
                    </td>
                            <td valign="middle" align="right">
                                    <input type="submit" name="submit_jump_<%= SubmitServlet.EDIT_METADATA_2 %>" value="<fmt:message key="jsp.submit.review.button.correct"/>" />
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
                <td class="evenRowOddCol">
                    <table>
                        <tr>
                            <td width="100%">
                                <table>
                                    <tr>
                                        <td class="metadataFieldLabel"><%= (si.submission.hasMultipleFiles() ? LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.upload1") : LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.upload2")) %></td>
                                        <td class="metadataFieldValue">
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
                                            <a href="<%= request.getContextPath() %>/<%= downloadLink %>/<%= UIUtil.encodeBitstreamName(bitstreams[i].getName()) %>" target="_blank"><%= bitstreams[i].getName() %></a> - <%= bitstreams[i].getFormatDescription() %>
<%
        switch (format.getSupportLevel())
        {
        case 0:
            %><fmt:message key="jsp.submit.review.unknown"/><%
            break;
        case 1:
            %><fmt:message key="jsp.submit.review.known"/><%
            break;
        case 2:
            %><fmt:message key="jsp.submit.review.supported"/><%
        }
%>        
                                            <br />
<%
    }
%>
                                        </td>
                                    </tr>
                                </table>
                    </td>
                            <td valign="middle" align="right">
<%
    // Can't edit files in workflow mode
    if(!SubmitServlet.isWorkflow(si))
    {
%>
                                    <input type="submit" name="submit_jump_<%= SubmitServlet.UPLOAD_FILES %>"
                                     value="<%= (si.submission.hasMultipleFiles() ? LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.button.upload1") : LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.button.upload2")) %>" />
<%
    }
    else
    {
%>

                                    <input type="submit" name="submit_jump_<%= SubmitServlet.UPLOAD_FILES %>"
                                     value="<fmt:message key="jsp.submit.review.button.edit"/>" />
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
        <input type="hidden" name="step" value="<%= SubmitServlet.REVIEW_SUBMISSION %>" />

        <p>&nbsp;</p>
    
        <center>
            <table border="0" width="80%">
                <tr>
                    <td width="100%">&nbsp;</td>
                    <td>
                        <input type="submit" name="submit_prev" value="<fmt:message key="jsp.submit.review.button.previous"/>" />
                    </td>
                    <td>
                        <input type="submit" name="submit_next" value="<fmt:message key="jsp.submit.review.button.next"/>" />
                    </td>
                    <td>&nbsp;&nbsp;&nbsp;</td>

                    <td align="right">
                        <input type="submit" name="submit_cancel" value="<fmt:message key="jsp.submit.review.button.cancelsave"/>" />
                    </td>
                </tr>
            </table>
        </center>

    </form>

</dspace:layout>
