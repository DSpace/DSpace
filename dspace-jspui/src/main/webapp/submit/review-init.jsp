<%--
  - review-init.jsp
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
  - Review initial question page
  -
  - Parameters to pass in to this page (from review.jsp)
  -    submission.jump - the step and page number (e.g. stepNum.pageNum) to create a "jump-to" link
  --%>
 
 <%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
 <%@ page contentType="text/html;charset=UTF-8" %>
 <%@ page import="org.dspace.app.webui.servlet.SubmissionController" %>
 <%@ page import="org.dspace.app.util.SubmissionInfo" %>
 <%@ page import="org.dspace.app.webui.util.UIUtil" %>
 <%@ page import="org.dspace.core.Context" %>
 <%@ page import="org.dspace.content.InProgressSubmission" %>
 <%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
 
 <%
    request.setAttribute("LanguageSwitch", "hide");

    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request);    

	//get submission information object
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);
	
	InProgressSubmission si = subInfo.getSubmissionItem();

	//get the step number (for jump-to link and to determine page)
	String stepJump = (String) request.getParameter("submission.jump");
 %>
  
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
                                           <td class="metadataFieldValue"><%= (si.hasMultipleTitles() ? LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.state1") : LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.state2")) %></td>
                                       </tr>
                                       <tr>
                                           <td class="metadataFieldLabel"><fmt:message key="jsp.submit.review.init-question2"/></td>
                                           <td class="metadataFieldValue"><%= (si.isPublishedBefore() ? LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.state1") : LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.state2")) %></td>
                                       </tr>
                                       <tr>
                                           <td class="metadataFieldLabel"><fmt:message key="jsp.submit.review.init-question3"/></td>
                                           <td class="metadataFieldValue"><%= (si.hasMultipleFiles() ? LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.state1") : LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.state2")) %></td>
                                       </tr>
                                   </table>
                               </td>
                               <td valign="middle">
                                       <input type="submit" name="submit_jump_<%=stepJump%>" value="<fmt:message key="jsp.submit.review.button.correct"/>" />
                               </td>
                           </tr>
                       </table>
                   </td>
                </tr>
