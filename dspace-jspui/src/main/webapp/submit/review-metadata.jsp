<%--
  - review-metadata.jsp
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
  - Review metadata page(s)
  -
  - Parameters to pass in to this page (from review.jsp)
  -    submission.jump - the step and page number (e.g. stepNum.pageNum) to create a "jump-to" link
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="java.io.IOException" %>

<%@ page import="org.dspace.submit.step.DescribeStep" %>
<%@ page import="org.dspace.app.webui.servlet.SubmissionController" %>
<%@ page import="org.dspace.app.util.SubmissionInfo" %>
<%@ page import="org.dspace.content.InProgressSubmission" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.app.util.DCInputsReader" %>
<%@ page import="org.dspace.app.util.DCInputSet" %>
<%@ page import="org.dspace.app.util.DCInput" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.DCDate" %>
<%@ page import="org.dspace.content.DCLanguage" %>
<%@ page import="org.dspace.content.DCValue" %>
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

	//get the step number (for jump-to link and to determine page)
	String stepJump = (String) request.getParameter("submission.jump");

	//extract out the step & page numbers from the stepJump (format: stepNum.pageNum)
	//(since there are multiple pages, we need to know which page we are reviewing!)
    String[] fields = stepJump.split("\\.");  //split on period
    int stepNum = Integer.parseInt(fields[0]);
	int pageNum = Integer.parseInt(fields[1]);

    Item item = subInfo.getSubmissionItem().getItem();
    
	//get the inputs reader
	DCInputsReader inputsReader = DescribeStep.getInputsReader();

	// determine collection
    Collection c = subInfo.getSubmissionItem().getCollection();

	//load the input set for the current collection
    DCInputSet inputSet = inputsReader.getInputs(c.getHandle());
%>

<%!void layoutSection(HttpServletRequest request, 
                       javax.servlet.jsp.JspWriter out,
                       DCInputSet inputSet,
                       SubmissionInfo subInfo,
                       Item item,
                       int pageNum,
                       PageContext pageContext)
        throws ServletException, IOException
    {
       InProgressSubmission ip = subInfo.getSubmissionItem();

	   //need to actually get the rows for pageNum-1 (since first page is index 0)
	   DCInput[] inputs = inputSet.getPageRows(pageNum-1,
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
          row.append("<td width=\"40%\" class=\"metadataFieldLabel\">");
          row.append(inputs[z].getLabel());
          row.append("</td>");
          row.append("<td width=\"60%\" class=\"metadataFieldValue\">");

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
                else if (inputType.equals("dropdown") || inputType.equals("list"))
                {
                   String storedVal = values[i].value;
                   String displayVal = inputs[z].getDisplayString(pairsName,
                                                                storedVal);
                   row.append(Utils.addEntities(displayVal));
                }
                else if (inputType.equals("qualdrop_value"))
                {
                   String qual = values[i].qualifier;
                   if(qual==null) qual = "";
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
    }%>


<%-- ====================================================== --%>
<%--             DESCRIBE ITEM ELEMENTS                     --%>
<%-- ====================================================== --%>
            <table width="100%">
               <tr>
                   <td width="100%">
                   <table width="700px">

<%
	    layoutSection(request, out, inputSet, subInfo, item, pageNum, pageContext);
%>
					</table>
				    </td>
				    <td valign="middle">
					 <input type="submit" name="submit_jump_<%=stepJump%>" value="<fmt:message key="jsp.submit.review.button.correct"/>" />
				    </td>
				</tr>
			</table>
