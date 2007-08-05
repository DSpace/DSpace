<%--
  - review-upload.jsp
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
  - Review file upload info
  -
  - Parameters to pass in to this page (from review.jsp)
  -    submission.jump - the step and page number (e.g. stepNum.pageNum) to create a "jump-to" link
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="org.dspace.app.webui.servlet.SubmissionController" %>
<%@ page import="org.dspace.app.util.SubmissionInfo" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.content.BitstreamFormat" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.core.Utils" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="javax.servlet.jsp.PageContext" %>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request);    

	//get submission information object
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);

	//get the step number (for jump-to link)
	String stepJump = (String) request.getParameter("submission.jump");

    Item item = subInfo.getSubmissionItem().getItem();
%>


<%-- ====================================================== --%>
<%--                    UPLOADED_FILES                      --%>
<%-- ====================================================== --%>
                    <table width="100%">
                        <tr>
                            <td width="100%">
                                <table>
                                    <tr>
                                        <td class="metadataFieldLabel"><%= (subInfo.getSubmissionItem().hasMultipleFiles() ? LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.upload1") : LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.upload2")) %></td>
                                        <td class="metadataFieldValue">
<%
    Bitstream[] bitstreams = item.getNonInternalBitstreams();

	if(bitstreams.length > 0)
	{
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
	}
	else { //otherwise, no files uploaded
%>
		<fmt:message key="jsp.submit.review.no_md"/>
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
    if(!subInfo.isInWorkflow())
    {
%>
                                    <input type="submit" name="submit_jump_<%=stepJump%>"
                                     value="<%= (subInfo.getSubmissionItem().hasMultipleFiles() ? LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.button.upload1") : LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.button.upload2")) %>" />
<%
    }
    else
    {
%>

                                    <input type="submit" name="submit_jump_<%=stepJump%>"
                                     value="<fmt:message key="jsp.submit.review.button.edit"/>" />
<%
    }
%>
                            </td>
                  </tr>
                </table>

