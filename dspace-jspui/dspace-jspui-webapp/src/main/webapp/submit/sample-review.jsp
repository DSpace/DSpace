<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Sample Review JSP
  -
  - This is a sample JSP that works in conjuction with
  - the org.dspace.submit.step.SampleStep class
  -
  - This JSP is meant to be a template for similar review JSPs.
  -
  - Parameters to pass in to this page (from review.jsp)
  -    submission.jump - the step and page number (e.g. stepNum.pageNum) to create a "jump-to" link
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="org.dspace.app.webui.servlet.SubmissionController" %>
<%@ page import="org.dspace.app.util.SubmissionInfo" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.core.Context" %>


<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="javax.servlet.jsp.PageContext" %>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
    request.setAttribute("LanguageSwitch", "hide");

    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request);    

	//get submission information object
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);

	//get the step number (for jump-to link)
	String stepJump = (String) request.getParameter("submission.jump");
%>   

<%-- ====================================================== --%>
<%--                  SAMPLE REVIEW PAGE                    --%>
<%-- ====================================================== --%>
                    <table width="100%">
                        <tr>
                            <td width="100%">
								<p>
                                A <strong>review JSP</strong> creates the <em>review section</em> for a single Step
								in the submission process.  In this case, this <em>review section</em> is for the Sample Step.
                                A review JSP should consist of a single table, which contains two main columns:
								</p>
								<ul>
								<li>The left column (this column) should contain all of the information which was gathered 
							    from this step (or at least anything that a user may want to review).</li>
								<li>The right column should contain a submit button (see the "Correct one of these" button to the right->) which will jump the user back to this Step
							    in the submission process.</li>
								</ul>
							    <p>
                                To see sample code, please visit the JSP which built this review section.
							    This JSP is located at <em>/jsp/submit/sample-review.jsp</em>
								</p> 								
                            </td>
                            <td valign="middle">
                                    <input type="submit" name="submit_jump_<%= stepJump %>" value="<fmt:message key="jsp.submit.review.button.correct"/>" />
                            </td>
                        </tr>
                    </table>
