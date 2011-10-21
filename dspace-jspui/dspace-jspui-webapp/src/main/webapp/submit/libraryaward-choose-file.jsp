<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@page import="java.util.List"%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.webui.servlet.SubmissionController" %>
<%@ page import="org.dspace.submit.AbstractProcessingStep" %>
<%@ page import="edu.umd.lib.dspace.submit.step.LibraryAwardUploadStep" %>
<%@ page import="org.dspace.app.util.DCInputSet" %>
<%@ page import="org.dspace.app.util.DCInputsReader" %>
<%@ page import="org.dspace.app.util.SubmissionInfo" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>


<%
    request.setAttribute("LanguageSwitch", "hide");

    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request);    

	//get submission information object
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);
   
 	// Determine whether a file is REQUIRED to be uploaded (default to true)
 	boolean fileRequired = ConfigurationManager.getBooleanProperty("webui.submit.upload.required", true);
 	
 	// List of bitsreams (descriptions) still needed
 	List<String> needed = LibraryAwardUploadStep.getNeededBitstreams(context, request, subInfo);
%>


<dspace:layout locbar="off"
               navbar="off"
               titlekey="jsp.submit.choose-file.title"
               nocache="true">

    <form method="post" action="<%= request.getContextPath() %>/submit" enctype="multipart/form-data" onkeydown="return disableEnterKey(event);">
		
		<jsp:include page="/submit/progressbar.jsp"/>
		<%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
        <%= SubmissionController.getSubmissionParameters(context, request) %>

        <%-- <h1>Submit: Upload a File</h1> --%>
		<h1>Submit: Upload Files</h1>
    
		<div class="submitFormHelp">
          You must submit one each of these files, one at a time: Essay, Research Paper, Bibliography, and Letter of Support.  
          They must be all be submitted in PDF format.
        </div>
    
        <table border="0" align="center">

            <tr>
                <td colspan="2">&nbsp;</td>
            </tr>
            <tr>
                <td class="submitFormHelp" colspan="2">
                </td>
            </tr>

            <tr>
                <%-- <td class="submitFormLabel">File Description:</td> --%>
				<td class="submitFormLabel"><label for="tdescription"><fmt:message key="jsp.submit.choose-file.filedescr"/></label></td>
                <td>
                    <% if (needed.size() > 0) { %>
	                    <select name="description" id="tdescription">
	                        <% for (String desc : needed) { %>
							    <option value="<%= desc %>"><%= desc %></option>
	                        <% } %>
					    </select>
				    <% } else { %>
				        <input type="text" name="description" id="tdescription" size="40"/>
				    <% } %>
                </td>
            </tr>

            <tr>
                <td class="submitFormLabel">
                    <%-- Document File: --%>
					<label for="tfile"><fmt:message key="jsp.submit.choose-file.document"/></label>
                </td>
                <td>
                    <input type="file" size="40" name="file" id="tfile" />
                </td>
            </tr>

        </table>
        
		<%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
        <%= SubmissionController.getSubmissionParameters(context, request) %>
    
        <p>&nbsp;</p>

        <center>
            <table border="0" width="80%">
                <tr>
                    <td width="100%">&nbsp;</td>
               	<%  //if not first step, show "Previous" button
					if(!SubmissionController.isFirstStep(request, subInfo))
					{ %>
                    <td>
                        <input type="submit" name="<%=AbstractProcessingStep.PREVIOUS_BUTTON%>" value="<fmt:message key="jsp.submit.general.previous"/>" />
                    </td>
				<%  } %>
                    <td>
                        <input type="submit" name="<%=LibraryAwardUploadStep.SUBMIT_UPLOAD_BUTTON%>" value="<fmt:message key="jsp.submit.general.next"/>" />
                    </td> 
                    <%
                        //if upload is set to optional, or user returned to this page after pressing "Add Another File" button
                    	if (!fileRequired || UIUtil.getSubmitButton(request, "").equals(LibraryAwardUploadStep.SUBMIT_MORE_BUTTON))
                        {
                    %>
                        	<td>
                                <input type="submit" name="<%=LibraryAwardUploadStep.SUBMIT_SKIP_BUTTON%>" value="<fmt:message key="jsp.submit.choose-file.skip"/>" />
                            </td>
                    <%
                        }
                    %>   
                              
                    <td>&nbsp;&nbsp;&nbsp;</td>
                    <td align="right">
                        <input type="submit" name="<%=AbstractProcessingStep.CANCEL_BUTTON%>" value="<fmt:message key="jsp.submit.general.cancel-or-save.button"/>" />
                    </td>
                </tr>
            </table>
        </center>  
    </form>

</dspace:layout>
