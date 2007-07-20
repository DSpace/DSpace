<%--
  - progressbar.jsp
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
  - Progress bar for submission form.  Note this must be included within
  - the FORM element in the submission form, since it contains navigation
  - buttons.
  -
  - Parameters: None
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.Iterator" %>

<%@ page import="org.dspace.app.webui.servlet.SubmissionController" %>
<%@ page import="org.dspace.app.webui.submit.JSPStepManager" %>
<%@ page import="org.dspace.submit.AbstractProcessingStep" %>

<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.app.util.SubmissionInfo" %>
<%@ page import="org.dspace.app.util.SubmissionConfig" %>
<%@ page import="org.dspace.app.util.SubmissionStepConfig" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.apache.log4j.Logger" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
    /** log4j logger */
    Logger log = Logger.getLogger("progressbar.jsp");

	// Obtain DSpace context
    Context context = UIUtil.obtainContext(request);
    
    //get submission information object
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);

    //get configuration for our current Submission process
    SubmissionConfig subConfig = subInfo.getSubmissionConfig();

    //get configuration for our current step & page in submission process
    SubmissionStepConfig currentStepConfig = SubmissionController.getCurrentStepConfig(request, subInfo);
	int currentPage = AbstractProcessingStep.getCurrentPage(request);

	//get last step & page reached
	int stepReached = SubmissionController.getStepReached(subInfo);
	int pageReached = JSPStepManager.getPageReached(subInfo);

    // Are we in workflow mode?
    boolean workflowMode = false;
    if (stepReached == -1)
    {
        workflowMode = true;
    }
%>

<!--Progress Bar-->
<center>
    <table class="submitProgressTable" border="0" cellspacing="0" cellpadding="0">
        <tr>
<%    
    //get progress bar info, used to build progress bar
	HashMap progressBarInfo = (HashMap) subInfo.getProgressBarInfo();

    if((progressBarInfo!=null) && (progressBarInfo.keySet()!=null))
    {
	   //get iterator
	   Set keys = progressBarInfo.keySet();
	   Iterator barIterator = keys.iterator();

	   //loop through all steps & print out info     
       while(barIterator.hasNext())
        {
		  //this is a string of the form: stepNum.pageNum
	      String stepAndPage = (String) barIterator.next();
		
		  //get heading from hashmap
		  String heading = (String) progressBarInfo.get(stepAndPage);

		  //if the heading contains a period (.), then assume
          //it is referencing a property in Messages.properties
          if(heading.indexOf(".") >= 0)
          {
             heading = LocaleSupport.getLocalizedMessage(pageContext, heading);
          }

		  //split into stepNum and pageNum
		  String[] fields = stepAndPage.split("\\.");  //split on period
          int stepNum = Integer.parseInt(fields[0]);
		  int pageNum = Integer.parseInt(fields[1]);
          
		  //if anywhere in last step (i.e. submission is completed), disable EVERYTHING (not allowed to jump back)
		  if(stepReached >= subConfig.getNumberOfSteps())
          {
			 if(stepNum==subConfig.getNumberOfSteps())
			 {
			   // Show "Complete" step as the current step
    %>
               <td><input class="submitProgressButtonCurrent" disabled="disabled" type="submit" name="<%=AbstractProcessingStep.PROGRESS_BAR_PREFIX + stepAndPage%>" value="<%=heading%>" /></td>
    <%
             }
        	 else
        	 {
			   // submission is completed, so cannot jump back to any steps
    %>
               <td><input class="submitProgressButtonDone" disabled="disabled" type="submit" name="<%=AbstractProcessingStep.PROGRESS_BAR_PREFIX + stepAndPage%>" value="<%=heading%>" /></td>
    <%
        	 }
           }
		   //if this is the current step & page, highlight it as "current"
		   else if((stepNum == currentStepConfig.getStepNumber()) && (pageNum == currentPage))
           {
	         %>
		     <td><input class="submitProgressButtonCurrent" disabled="disabled" type="submit" name="<%=AbstractProcessingStep.PROGRESS_BAR_PREFIX + stepAndPage%>" value="<%=heading%>" /></td>
        	 <%
           }
		   else if(workflowMode) //if in workflow mode, can jump to any step/page
    	   {
		     %>
            <td><input class="submitProgressButtonDone" type="submit" name="<%=AbstractProcessingStep.PROGRESS_BAR_PREFIX + stepAndPage%>" value="<%=heading%>" /></td>
			 <%
    	   }
		  //else if this step & page has been completed
		  else if( (stepNum < stepReached) || ((stepNum == stepReached) && (pageNum <= pageReached)) )
    	  {
%>
            <td><input class="submitProgressButtonDone" type="submit" name="<%=AbstractProcessingStep.PROGRESS_BAR_PREFIX + stepAndPage%>" value="<%=heading%>" /></td>
<%
          }
		  else //else this is a step that has not been done, yet
          {
            // Stage hasn't been completed yet (can't be jumped to)
%>
		    <td><input class="submitProgressButtonNotDone" disabled="disabled" type="submit" name="<%=AbstractProcessingStep.PROGRESS_BAR_PREFIX + stepAndPage%>" value="<%=heading%>" /></td>
<%
          }
	   }//end while
   }
%>
        </tr>
    </table>
</center>
