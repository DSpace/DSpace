<%--
  - edit-metadata.jsp
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
  - Edit metadata form
  -
  - Attributes to pass in to this page:
  -    submission.info   - the SubmissionInfo object
  -    submission.inputs - the DCInputSet
  -    submission.page   - the step in submission
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="javax.servlet.ServletException" %>

<%@ page import="org.dspace.app.webui.jsptag.PopupTag" %>
<%@ page import="org.dspace.app.webui.util.DCInput" %>
<%@ page import="org.dspace.app.webui.util.DCInputSet" %>
<%@ page import="org.dspace.app.webui.servlet.SubmitServlet" %>
<%@ page import="org.dspace.app.webui.util.JSPManager" %>
<%@ page import="org.dspace.app.webui.util.SubmissionInfo" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.content.DCDate" %>
<%@ page import="org.dspace.content.DCLanguage" %>
<%@ page import="org.dspace.content.DCPersonName" %>
<%@ page import="org.dspace.content.DCSeriesNumber" %>
<%@ page import="org.dspace.content.DCValue" %>
<%@ page import="org.dspace.content.Item" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%!

    void doPersonalName(javax.servlet.jsp.JspWriter out, Item item,
      String fieldName, String element, String qualifier, boolean repeatable,
      int fieldCountIncr, String label) 
      throws java.io.IOException 
    {

      DCValue[] defaults = item.getDC(element, qualifier, Item.ANY);
      int fieldCount = defaults.length + fieldCountIncr;
      StringBuffer headers = new StringBuffer();
      StringBuffer sb = new StringBuffer();
      org.dspace.content.DCPersonName dpn;
      StringBuffer name = new StringBuffer();
      StringBuffer first = new StringBuffer(); 
      StringBuffer last = new StringBuffer();
      
      if (fieldCount == 0)
         fieldCount = 1;

      //Width hints used here to affect whole table 
      headers.append("<tr><td width=\"40%\">&nbsp;</td>")
             .append("<td class=\"submitFormDateLabel\" width=\"5%\">")
             .append("Last name<br>e.g. <strong>Smith</strong></td>")
             .append("<td class=\"submitFormDateLabel\" width=\"5%\">")
             .append("First name(s) + \"Jr\"<br> e.g. <strong>Donald Jr</strong></td>")
             .append("<td width=\"40%\">&nbsp;</td>")
             .append("</tr>");
      out.write(headers.toString());


      for (int i = 0; i < fieldCount; i++) 
      {
	 first.setLength(0);
	 first.append(fieldName).append("_first");
	 if (repeatable)
	    first.append('_').append(i);

	 last.setLength(0);
	 last.append(fieldName).append("_last");
	 if (repeatable)
	    last.append('_').append(i);
	    
	 if (i == 0) 
	    sb.append("<tr><td class=\"submitFormLabel\">")
	      .append(label)
	      .append("</td>");
	 else
	    sb.append("<tr><td>&nbsp;</td>");

         if (i < defaults.length)
	    dpn = new org.dspace.content.DCPersonName(defaults[i].value);
	 else
	    dpn = new org.dspace.content.DCPersonName();
	 
         sb.append("<td><input type=text name=\"")
           .append(last.toString())
           .append("\" size=23 value=\"")
           .append(dpn.getLastName())
	   .append("\"></td>\n<td><input type=text name=\"")
	   .append(first.toString())
           .append("\" size=23 value=\"")
           .append(dpn.getFirstNames()).append("\"></td>\n");

	 if (repeatable && i < defaults.length) 
	 {
	    name.setLength(0);
	    name.append(dpn.getLastName())
	        .append(' ')
	        .append(dpn.getFirstNames());
	    // put a remove button next to filled in values
	    sb.append("<td><input type=submit name=\"submit_")
	      .append(fieldName)
	      .append("_remove_")
	      .append(i)
	      .append("\" value=\"Remove This Entry\"> </td></tr>");
	 } 
	 else if (repeatable && i == fieldCount - 1) 
	 {
	    // put a 'more' button next to the last space
	    sb.append("<td><input type=submit name=\"submit_")
	      .append(fieldName)
	      .append("_more\" value=\"Add More\"> </td></tr>");
	 } 
	 else 
	 {
	    // put a blank if nothing else
	    sb.append("<td>&nbsp;</td></tr>");
	 }
      }

      out.write(sb.toString());
    }

    void doDate(javax.servlet.jsp.JspWriter out, Item item,
      String fieldName, String element, String qualifier, boolean repeatable,
      int fieldCountIncr, String label) 
      throws java.io.IOException 
    {

      DCValue[] defaults = item.getDC(element, qualifier, Item.ANY);
      int fieldCount = defaults.length + fieldCountIncr;
      StringBuffer sb = new StringBuffer();
      org.dspace.content.DCDate dateIssued;

      if (fieldCount == 0)
         fieldCount = 1;

      for (int i = 0; i < fieldCount; i++) 
      {
	 if (i == 0) 
	    sb.append("<tr><td class=\"submitFormLabel\">")
	      .append(label)
	      .append("</td>");
	 else
	    sb.append("<tr><td>&nbsp;</td>");

         if (i < defaults.length)
            dateIssued = new org.dspace.content.DCDate(defaults[i].value);
         else
            dateIssued = new org.dspace.content.DCDate("");
    
         sb.append("<td colspan=2 nowrap class=\"submitFormDateLabel\">")
            .append("Month:<select name=\"")
	    .append(fieldName)
	    .append("_month");
         if (repeatable)
            sb.append('_').append(i);
         sb.append("\"><option value=\"-1\"")
            .append((dateIssued.getMonth() == -1 ? " SELECTED" : ""))
	    .append(">(No month)</option>");
    
         for (int j = 1; j < 13; j++) 
	 {
            sb.append("<option value=\"")
	      .append(j)
	      .append((dateIssued.getMonth() == j ? "\" SELECTED" : "\"" ))
	      .append(">")
	      .append(org.dspace.content.DCDate.getMonthName(j))
	      .append("</option>");
         }
    
         sb.append("</select>")
            .append("Day:<input type=text name=\"")
	    .append(fieldName)
	    .append("_day");
         if (repeatable)
            sb.append("_").append(i);
         sb.append("\" size=2 maxlength=2 value=\"")
            .append((dateIssued.getDay() > 0 ? 
	             String.valueOf(dateIssued.getDay()) : "" ))
	    .append("\">Year:<input type=text name=\"")
	    .append(fieldName)
	    .append("_year");
         if (repeatable)
            sb.append("_").append(i);
         sb.append("\" size=4 maxlength=4 value=\"")
            .append((dateIssued.getYear() > 0 ? 
	         String.valueOf(dateIssued.getYear()) : "" ))
	    .append("\"></td>\n");
    
	 if (repeatable && i < defaults.length) 
	 {
	    // put a remove button next to filled in values
	    sb.append("<td><input type=submit name=\"submit_")
	      .append(fieldName)
	      .append("_remove_")
	      .append(i)
	      .append("\" value=\"Remove This Entry\"> </td></tr>");
	 } 
	 else if (repeatable && i == fieldCount - 1) 
	 {
	    // put a 'more' button next to the last space
	    sb.append("<td><input type=submit name=\"submit_")
	      .append(fieldName)
	      .append("_more\" value=\"Add More\"> </td></tr>");
	 } 
	 else 
	 {
	    // put a blank if nothing else
	    sb.append("<td>&nbsp;</td></tr>");
	 }
      }

      out.write(sb.toString());
    }

    void doSeriesNumber(javax.servlet.jsp.JspWriter out, Item item,
      String fieldName, String element, String qualifier, boolean repeatable,
      int fieldCountIncr, String label) 
      throws java.io.IOException 
    {

      DCValue[] defaults = item.getDC(element, qualifier, Item.ANY);
      int fieldCount = defaults.length + fieldCountIncr;
      StringBuffer sb = new StringBuffer();
      org.dspace.content.DCSeriesNumber sn;

      if (fieldCount == 0)
         fieldCount = 1;

      for (int i = 0; i < fieldCount; i++) 
      {
	 if (i == 0) 
	    sb.append("<tr><td class=\"submitFormLabel\">")
	      .append(label)
	      .append("</td>");
	 else
	    sb.append("<tr><td>&nbsp;</td>");

         if (i < defaults.length)
           sn = new org.dspace.content.DCSeriesNumber(defaults[i].value);
         else
           sn = new org.dspace.content.DCSeriesNumber();

         sb.append("<td><input type=text name=\"")
           .append(fieldName)
	   .append("_series");
         if (repeatable)
           sb.append("_").append(i);
         sb.append("\" size=23 value=\"")
           .append(sn.getSeries())
	   .append("\"></td>\n<td><input type=text name=\"")
	   .append(fieldName)
	   .append("_number");
         if (repeatable)
           sb.append("_").append(i);
         sb.append("\" size=23 value=\"")
           .append(sn.getNumber())
	   .append("\"></td>\n");

	 if (repeatable && i < defaults.length) 
	 {
	    // put a remove button next to filled in values
	    sb.append("<td><input type=submit name=\"submit_")
	      .append(fieldName)
	      .append("_remove_")
	      .append(i)
	      .append("\" value=\"Remove This Entry\"> </td></tr>");
	 } 
	 else if (repeatable && i == fieldCount - 1) 
	 {
	    // put a 'more' button next to the last space
	    sb.append("<td><input type=submit name=\"submit_")
	      .append(fieldName)
	      .append("_more\" value=\"Add More\"> </td></tr>");
	 } 
	 else 
	 {
	    // put a blank if nothing else
	    sb.append("<td>&nbsp;</td></tr>");
	 }
      }

      out.write(sb.toString());
    }

    void doTextArea(javax.servlet.jsp.JspWriter out, Item item,
      String fieldName, String element, String qualifier, boolean repeatable,
      int fieldCountIncr, String label) 
      throws java.io.IOException 
    {

      DCValue[] defaults = item.getDC(element, qualifier, Item.ANY);
      int fieldCount = defaults.length + fieldCountIncr;
      StringBuffer sb = new StringBuffer();
      String val;

      if (fieldCount == 0)
         fieldCount = 1;

      for (int i = 0; i < fieldCount; i++) 
      {
	 if (i == 0) 
	    sb.append("<tr><td class=\"submitFormLabel\">")
	      .append(label)
	      .append("</td>");
	 else
	    sb.append("<tr><td>&nbsp;</td>");

         if (i < defaults.length)
           val = defaults[i].value;
         else
           val = "";

         sb.append("<td colspan=2><textarea name=\"")
           .append(fieldName);
         if (repeatable)
           sb.append("_").append(i);
         sb.append("\" rows=4 cols=45 wrap=soft>")
           .append(val)
	   .append("</textarea></td>\n");

	 if (repeatable && i < defaults.length) 
	 {
	    // put a remove button next to filled in values
	    sb.append("<td><input type=submit name=\"submit_")
	      .append(fieldName)
	      .append("_remove_")
	      .append(i)
	      .append("\" value=\"Remove This Entry\"> </td></tr>");
	 } 
	 else if (repeatable && i == fieldCount - 1) 
	 {
	    // put a 'more' button next to the last space
	    sb.append("<td><input type=submit name=\"submit_")
	      .append(fieldName)
	      .append("_more\" value=\"Add More\"> </td></tr>");
	 } 
	 else 
	 {
	    // put a blank if nothing else
	    sb.append("<td>&nbsp;</td></tr>");
	 }
      }

      out.write(sb.toString());
    }

    void doOneBox(javax.servlet.jsp.JspWriter out, Item item,
      String fieldName, String element, String qualifier, boolean repeatable,
      int fieldCountIncr, String label) 
      throws java.io.IOException 
    {

      DCValue[] defaults = item.getDC(element, qualifier, Item.ANY);
      int fieldCount = defaults.length + fieldCountIncr;
      StringBuffer sb = new StringBuffer();
      String val;

      if (fieldCount == 0)
         fieldCount = 1;

      for (int i = 0; i < fieldCount; i++) 
      {
	 if (i == 0) 
	    sb.append("<tr><td class=\"submitFormLabel\">")
	      .append(label)
	      .append("</td>");
	 else
	    sb.append("<tr><td>&nbsp;</td>");

         if (i < defaults.length)
           val = defaults[i].value;
         else
           val = "";

         sb.append("<td colspan=2><input type=text name=\"")
           .append(fieldName);
         if (repeatable)
           sb.append("_").append(i);
         sb.append("\" size=50 value=\"")
           .append(val)
	   .append("\"></td>\n");

	 if (repeatable && i < defaults.length) 
	 {
	    // put a remove button next to filled in values
	    sb.append("<td><input type=submit name=\"submit_")
	      .append(fieldName)
	      .append("_remove_")
	      .append(i)
	      .append("\" value=\"Remove This Entry\"> </td></tr>");
	 } 
	 else if (repeatable && i == fieldCount - 1) 
	 {
	    // put a 'more' button next to the last space
	    sb.append("<td><input type=submit name=\"submit_")
	      .append(fieldName)
	      .append("_more\" value=\"Add More\"> </td></tr>");
	 } 
	 else 
	 {
	    // put a blank if nothing else
	    sb.append("<td>&nbsp;</td></tr>");
	 }
      }

      out.write(sb.toString());
    }

    void doTwoBox(javax.servlet.jsp.JspWriter out, Item item,
      String fieldName, String element, String qualifier, boolean repeatable,
      int fieldCountIncr, String label) 
      throws java.io.IOException 
    {
      DCValue[] defaults = item.getDC(element, qualifier, Item.ANY);
      int fieldCount = defaults.length + fieldCountIncr;
      StringBuffer sb = new StringBuffer();
      StringBuffer headers = new StringBuffer();

      if (element.equals("relation") && qualifier.equals("ispartofseries"))
      {
         //Width hints used here to affect whole table 
         headers.append("<tr><td width=\"40%\">&nbsp;</td>")
             .append("<td class=\"submitFormDateLabel\" width=\"5%\">")
             .append("Series Name</td>")
             .append("<td class=\"submitFormDateLabel\" width=\"5%\">")
             .append("Report or Paper No.</td>")
             .append("<td width=\"40%\">&nbsp;</td>")
             .append("</tr>");
         out.write(headers.toString());
      }

      if (fieldCount == 0)
         fieldCount = 1;

      for (int i = 0; i < fieldCount; i++) 
      {
	 if (i == 0) 
	    sb.append("<tr><td class=\"submitFormLabel\">")
	      .append(label)
	      .append("</td>");
	 else
	    sb.append("<tr><td>&nbsp;</td>");

         if (i < defaults.length)
           sb.append("<td align=\"left\"><input type=text name=\"")
             .append(fieldName)
             .append("_").append(i)
             .append("\" size=\"15\" value=\"")
             .append(defaults[i].value)
	     .append("\">&nbsp;<input type=submit name=\"submit_")
	     .append(fieldName)
	     .append("_remove_")
	     .append(i)
	     .append("\" value=\"Remove\"></td>\n");
         else 
	 {
           sb.append("<td align=\"left\"><input type=text name=\"")
             .append(fieldName)
             .append("_").append(i)
             .append("\" size=\"15\"></td>\n");
	 }
	 i++;
	 if (i < defaults.length)
           sb.append("<td align=\"left\"><input type=text name=\"")
             .append(fieldName)
             .append("_").append(i)
             .append("\" size=\"15\" value=\"")
             .append(defaults[i].value)
	     .append("\">&nbsp;<input type=submit name=\"submit_")
	     .append(fieldName)
	     .append("_remove_")
	     .append(i)
	     .append("\" value=\"Remove\"></td></tr>\n");
	 else 
	 {
           sb.append("<td align=\"left\"><input type=text name=\"")
             .append(fieldName)
             .append("_").append(i)
             .append("\" size=\"15\">");

	   if (i+1 >= fieldCount) 
	   {
	     sb.append("<td><input type=submit name=\"submit_")
	       .append(fieldName)
	       .append("_more\" value=\"Add More\"></td>\n"); 
	   } 
	   else 
	   {
	     sb.append("</td>");
	   }
	   sb.append("<td>&nbsp;</td></tr>");
	 }
      }

      out.write(sb.toString());
    }

    void doQualdropValue(javax.servlet.jsp.JspWriter out, Item item,
      String fieldName, String element, DCInputSet inputs, boolean repeatable,
      int fieldCountIncr, List qualMap, String label) 
      throws java.io.IOException 
    {
		DCValue[] unfiltered = item.getDC(element, Item.ANY, Item.ANY);
		// filter out both unqualified and qualified values occuring elsewhere in inputs
		ArrayList filtered = new ArrayList();
		for (int i = 0; i < unfiltered.length; i++)
		{
			String fieldName = unfiltered[i].element + "." + unfiltered[i].qualifier;
			if ( ! inputs.isFieldPresent(fieldName) )
			{
				filtered.add( unfiltered[i] );
			} 
		}
		DCValue[] defaults = (DCValue[])filtered.toArray(new DCValue[0]);
      //DCValue[] defaults = item.getDC(element, Item.ANY, Item.ANY);
      int fieldCount = defaults.length + fieldCountIncr;
      StringBuffer sb = new StringBuffer();
      String   q, v, currentQual, currentVal;

      if (fieldCount == 0)
         fieldCount = 1;

      for (int j = 0; j < fieldCount; j++) 
      {

         if (j < defaults.length) 
	 {
            currentQual = defaults[j].qualifier;
            currentVal = defaults[j].value;
         }
	 else 
	 {
	    currentQual = "";
	    currentVal = "";
	 }

	 if (j == 0) 
	    sb.append("<tr><td class=\"submitFormLabel\">")
              .append(label)
	      .append("</td>");
	 else
            sb.append("<tr><td>&nbsp;</td>");

	 // do the dropdown box
	 sb.append("<td colspan=2><select name=\"")
           .append(fieldName)
	   .append("_qualifier");
         if (repeatable) 
           sb.append("_").append(j);
         sb.append("\">");
         for (int i = 0; i < qualMap.size(); i+=2)
         {
	   q = (String)qualMap.get(i);
	   v = (String)qualMap.get(i+1);
           sb.append("<option")
	     .append((v.equals(currentQual) ? " SELECTED ": "" ))
	     .append(" VALUE=\"")
	     .append(v)
	     .append("\">")
	     .append(q)
	     .append("</option>");
         }
      
	 // do the input box
         sb.append("</select>&nbsp;<input type=text name=\"")
           .append(fieldName)
	   .append("_value");
         if (repeatable)
           sb.append("_").append(j);
         sb.append("\" size=34 value=\"")
           .append(currentVal)
	   .append("\"></td>\n");

	 if (repeatable && j < defaults.length) 
	 {
	    // put a remove button next to filled in values
	    sb.append("<td><input type=submit name=\"submit_")
	      .append(fieldName)
	      .append("_remove_")
	      .append(j)
	      .append("\" value=\"Remove This Entry\"> </td></tr>");
	 } 
	 else if (repeatable && j == fieldCount - 1) 
	 {
	    // put a 'more' button next to the last space
	    sb.append("<td><input type=submit name=\"submit_")
	      .append(fieldName)
	      .append("_more\" value=\"Add More\"> </td></tr>");
	 } 
	 else 
	 {
	    // put a blank if nothing else
	    sb.append("<td>&nbsp;</td></tr>");
	 }
      }

      out.write(sb.toString());
    }

    void doDropDown(javax.servlet.jsp.JspWriter out, Item item,
      String fieldName, String element, String qualifier, boolean repeatable,
      List valueList, String label) 
      throws java.io.IOException 
    {
      DCValue[] defaults = item.getDC(element, qualifier, Item.ANY);
      StringBuffer sb = new StringBuffer();
      Iterator vals;
      String display, value;
      int j;

      sb.append("<tr><td class=\"submitFormLabel\">")
	.append(label)
	.append("</td>");

      sb.append("<td colspan=2>")
        .append("<select name=\"")
	.append(fieldName)
	.append("\"");
      if (repeatable)
	sb.append(" size=6  multiple");
      sb.append(">");

      for (int i = 0; i < valueList.size(); i += 2)
      {
         display = (String)valueList.get(i);
	 value = (String)valueList.get(i+1);
	 for (j = 0; j < defaults.length; j++) 
	 {
	     if (value.equals(defaults[j].value))
	         break;
         }
	 sb.append("<option ")
	   .append(j < defaults.length ? " selected " : "")
	   .append("value=\"")
	   .append(value)
	   .append("\">")
	   .append(display)
	   .append("</option>");
      }

      sb.append("</select></td></tr>");
      out.write(sb.toString());
    }
%>

<%
    SubmissionInfo si =
        (SubmissionInfo) request.getAttribute("submission.info");

    Item item = si.submission.getItem();

    final int halfWidth = 23;
    final int fullWidth = 50;
    final int twothirdsWidth = 34;

    DCInputSet inputSet = 
    	(DCInputSet) request.getAttribute("submission.inputs");

    Integer pageNumStr =
        (Integer) request.getAttribute("submission.page");
    int pageNum = pageNumStr.intValue();
%>

<dspace:layout locbar="off" navbar="off" title="Describe Your Item">

  <form action="<%= request.getContextPath() %>/submit#<%= si.jumpToField%>" method=post>

    <jsp:include page="/submit/progressbar.jsp">
      <jsp:param name="current_stage" value="<%= pageNum %>"/>
      <jsp:param name="stage_reached" value="<%= SubmitServlet.getStepReached(si) %>"/>
      <jsp:param name="md_pages" value="<%= si.numMetadataPages %>"/>
    </jsp:include>

    <H1>Submit: Describe Your Item</H1>

<%
     if (pageNum == SubmitServlet.EDIT_METADATA_1) 
     {
%>
        <P>Please fill in the requested information about your submission below.  In
        most browsers, you can use the tab key to move the cursor to the next input
        box or button, to save you having to use the mouse each time.
        <dspace:popup page="/help/index.html#describe2">(More Help...)</dspace:popup></P>
<%
     } 
     else 
     {
%>
    <P>Please fill further information about your submission below.
        <dspace:popup page="/help/index.html#describe3">(More Help...)</dspace:popup></P>
    
<%
     }
%>

     <%-- HACK: a <center> tag seems to be the only way to convince certain --%>
     <%--       browsers to center the table. --%>
     <center>
     <table>
<%
	 int pageIdx = pageNum - SubmitServlet.EDIT_METADATA_1; 
     DCInput[] inputs = inputSet.getPageRows(pageIdx, si.submission.hasMultipleTitles(),
                                                si.submission.isPublishedBefore() );
     for (int z = 0; z < inputs.length; z++) 
     { 
       String dcElement = inputs[z].getElement();
       String dcQualifier = inputs[z].getQualifier();
       String fieldName;
       int fieldCountIncr;
       boolean repeatable;

       if (dcQualifier != null && !dcQualifier.equals("*"))
          fieldName = dcElement + '_' + dcQualifier;
       else
          fieldName = dcElement;

       //if (inputs[z].isRequired()) {
         // si.jumpToField = fieldName;
       //}


       StringBuffer sb;
       if ((si.missingFields != null) && (si.missingFields.contains(new Integer(z))))
       {
          String req = inputs[z].getWarning();
	  	  int anchor = req.indexOf("</td>");
          sb = new StringBuffer(req);
          sb.insert(anchor, "<a name=\""+fieldName+"\"></a>");
       }
       else
       {
          sb = new StringBuffer(inputs[z].getHints());
       }
       out.write(sb.toString());

       repeatable = inputs[z].getRepeatable();
       fieldCountIncr = 0;
       if (repeatable) 
       { 
         fieldCountIncr = 1;
         if (si.moreBoxesFor != null && si.moreBoxesFor.equals(fieldName)) 
	 {
           fieldCountIncr = 2;
         }
       }

       String inputType = inputs[z].getInputType();
       String label = inputs[z].getLabel();
       if (inputType.equals("name")) 
       {
           doPersonalName(out, item, fieldName, dcElement, dcQualifier,
	     				  repeatable, fieldCountIncr, label);
       } 
       else if (inputType.equals("date")) 
       {
           doDate(out, item, fieldName, dcElement, dcQualifier, 
	     		  repeatable, fieldCountIncr, label);
       } 
       else if (inputType.equals("series")) 
       {
           doSeriesNumber(out, item, fieldName, dcElement, dcQualifier, 
	                      repeatable, fieldCountIncr, label);

       } 
       else if (inputType.equals("qualdrop_value")) 
       {
           doQualdropValue(out, item, fieldName, dcElement, inputSet, repeatable,
                           fieldCountIncr, inputs[z].getPairs(), label);
       } 
       else if (inputType.equals("textarea")) 
       {
	   	   doTextArea(out, item, fieldName, dcElement, dcQualifier, 
	     			  repeatable, fieldCountIncr, label);

       } 
       else if (inputType.equals("dropdown")) 
       {
	   		doDropDown(out, item, fieldName, dcElement, dcQualifier, 
	     			   repeatable, inputs[z].getPairs(), label);
       } 
       else if (inputType.equals("twobox")) 
       {
	   		doTwoBox(out, item, fieldName, dcElement, dcQualifier, 
	     			 repeatable, fieldCountIncr, label);
       } 
       else 
       {
	   		doOneBox(out, item, fieldName, dcElement, dcQualifier, 
	     			 repeatable, fieldCountIncr, label);
       }
%>

<%-- HACK: Using this line to give the browser hints as to the widths of cells --%>
       <tr>
         <td width="40%">&nbsp;</td>
         <td colspan=2 width=5%>&nbsp;</td>
         <td width="40%">&nbsp;</td>
       </tr>

<% 
     } // end of 'for rows'
%>
            </table>
        </center>
        
<%-- HACK:  Need a space - is there a nicer way to do this than <BR> or a --%>
<%--        blank <P>? --%>
        <P>&nbsp;</P>

<%-- Hidden fields needed for submit servlet to know which item to deal with --%>
        <%= SubmitServlet.getSubmissionParameters(si) %>
        <input type=hidden name="step" value="<%= pageNum %>">
        <center>
            <table border=0 width=80%>
                <tr>
                    <td width="100%">&nbsp;</td>
                    <td>
                        <input type=submit name="submit_prev" value="&lt; Previous">
                    </td>
                    <td>
                        <input type=submit name="submit_next" value="Next &gt;">
                    </td>
                    <td>&nbsp;&nbsp;&nbsp;</td>
                    <td align=right>
                        <input type=submit name="submit_cancel" value="Cancel/Save">
                    </td>
                </tr>
            </table>
        </center>
    </form>

</dspace:layout>
