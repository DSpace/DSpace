<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

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
	<div class="col-md-10">
         <div class="row">
             <span class="metadataFieldLabel col-md-4"><fmt:message key="jsp.submit.review.init-question1"/></span>
             <span class="metadataFieldValue col-md-8"><%= (si.hasMultipleTitles() ? LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.state1") : LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.state2")) %></span>
         </div>
         <div class="row">
             <span class="metadataFieldLabel col-md-4"><fmt:message key="jsp.submit.review.init-question2"/></span>
             <span class="metadataFieldValue col-md-8"><%= (si.isPublishedBefore() ? LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.state1") : LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.state2")) %></span>
         </div>
         <div class="row">
             <span class="metadataFieldLabel col-md-4"><fmt:message key="jsp.submit.review.init-question3"/></span>
             <span class="metadataFieldValue col-md-8"><%= (si.hasMultipleFiles() ? LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.state1") : LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.state2")) %></span>
         </div>
    </div>
	<div class="col-md-2">
            <input class="btn btn-default" type="submit" name="submit_jump_<%=stepJump%>" value="<fmt:message key="jsp.submit.review.button.correct"/>" />
    </div>
