<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Advanced Search JSP
  -
  -
  -
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.search.QueryResults" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>


<%
    Community [] communityArray = (Community[] )request.getAttribute("communities");
	String query1 			= request.getParameter("query1") == null ? "" : request.getParameter("query1");
	String query2 			= request.getParameter("query2") == null ? "" : request.getParameter("query2");
	String query3 			= request.getParameter("query3") == null ? "" : request.getParameter("query3");
	String query4 			= request.getParameter("query4") == null ? "" : request.getParameter("query4");
	String query5 			= request.getParameter("query5") == null ? "" : request.getParameter("query5");
	String query6 			= request.getParameter("query6") == null ? "" : request.getParameter("query6");

	String field1 			= request.getParameter("field1") == null ? "ANY" : request.getParameter("field1");
	String field2 			= request.getParameter("field2") == null ? "ANY" : request.getParameter("field2");
	String field3 			= request.getParameter("field3") == null ? "ANY" : request.getParameter("field3");
	String field4 			= request.getParameter("field4") == null ? "ANY" : request.getParameter("field4");
	String field5 			= request.getParameter("field5") == null ? "ANY" : request.getParameter("field5");
	String field6 			= request.getParameter("field6") == null ? "ANY" : request.getParameter("field6");

	String conjunction1 	= request.getParameter("conjunction1") == null ? "AND" : request.getParameter("conjunction1");
	String conjunction2 	= request.getParameter("conjunction2") == null ? "AND" : request.getParameter("conjunction2");
	
	String searchIn			= request.getParameter("searchIn") == null ? "all" : request.getParameter("searchIn");

    QueryResults qResults = (QueryResults)request.getAttribute("queryresults");
	
%>

<dspace:layout locbar="nolink" titlekey="jsp.search.advanced.title">

<form action="<%= request.getContextPath() %>/simple-search" method="get">
<input type="hidden" name="advanced" value="true"/>
<table class="miscTable" align="center" summary="Search DSpace Form">
    <tr>
      <td class="oddRowEvenCol" align="center">	
      	<p><strong><fmt:message key="jsp.search.advanced.search"/></strong>&nbsp;
		<select name="location">
			<option selected="selected" value="/"><fmt:message key="jsp.general.genericScope"/></option>

<%
        for (int i = 0; i < communityArray.length; i++)
        {
%>
			<option value="<%= communityArray[i].getHandle() %>"><%= communityArray[i].getMetadata("name") %></option>
<%
        }
%>
		</select>
              </p>
         <table cellspacing="2" border="0" width="80%">
		  <tr>
                <td class="evenRowEvenCol">
                    <table border="0">
            <tr>
            <td width="12%" align="left" valign="top"></td>
              <td width="20%" align="left" valign="top" nowrap="nowrap">
                <%-- Search type: <br> --%>
                <label for="tfield1"><fmt:message key="jsp.search.advanced.type"/></label> <br/>
                  <select onchange="onFieldChange('<fmt:message key="jsp.search.advanced.searchfor"/>', '<%= dateIndexString %>');" name="field1" id="tfield1">
                    <option value="ANY" <%= field1.equals("ANY") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.keyword"/></option>
                    <option value="author" <%= field1.equals("author") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.author"/></option>
                    <option value="title" <%= field1.equals("title") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.title"/></option>
                    <option value="keyword" <%= field1.equals("keyword") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.subject"/></option>
                    <option value="abstract" <%= field1.equals("abstract") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.abstract"/></option>
                    <option value="series" <%= field1.equals("series") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.series"/></option>
                    <option value="sponsor" <%= field1.equals("sponsor") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.sponsor"/></option>
                    <option value="identifier" <%= field1.equals("identifier") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.id"/></option>
                    <option value="language" <%= field1.equals("language") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.language"/></option>
                  </select>
            </td>
			
			<td><table border="0" cellpadding="0" cellspacing="0">
            <tr id="date_row" height="25px">
               <%
                String displayType = "none"; 
                String displayType_2 = "none";
                if (field1.equals("date")){
                	displayType = "block";
                	if (field4 != null && field4.equals("between")){
                		displayType_2 = "block";
                	}
                }
             %>
            <td id="tfield4_column" width="100px" align="left" valign="top" nowrap="nowrap" style="display:<%= displayType%>">
                <%-- Search type: <br> --%>
                
                <label id="tfield4_label" for="tfield4" style="display:<%= displayType%>; font-family: "verdana","Arial","Helvetica",sans-serif; font-size: 12pt; font-weight: none; color: #000000"><fmt:message key="jsp.search.advanced.option.option"/></label>
                  <select onchange="onOptionChange('<fmt:message key="jsp.search.advanced.option.from"/>','<fmt:message key="jsp.search.advanced.searchfor"/>');" name="field4" id="tfield4" style="display:<%= displayType%>; font-family:Arial, Helvetica, sans-serif;font-size:12px;height:20px;width:100px">
                  	<option value="equal" <%= field4.equals("equal") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.option.equal"/></option>
                    <option value="before" <%= field4.equals("before") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.option.before"/></option>
                    <option value="after" <%= field4.equals("after") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.option.after"/></option>
                  	<option value="between" <%= field4.equals("between") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.option.between"/></option>
                  </select>
            </td>

			<td align="left" valign="top" nowrap="nowrap" width="68%">
                <%-- Search for: <br> --%>
                <% if (field1 != null && field4!=null && field1.equals("date") && field4.equals("between")){ %>
              	<label id="tquery1_label" for="tquery1"><fmt:message key="jsp.search.advanced.option.from"/></label> <br/>
              	<%} else {%>
              	<label id="tquery1_label" for="tquery1"><fmt:message key="jsp.search.advanced.searchfor"/></label> <br/>
              	<%}%>
                <input onkeyup="validateDateNumber(true, '<%= dateIndexString %>');" type="text" name="query1" id="tquery1" value="<%=StringEscapeUtils.escapeHtml(query1)%>" size="30" />
                <br/>
                <label id="tquery4_label" for="tquery4" style="display:<%= displayType_2%> ;font-family: "verdana","Arial","Helvetica",sans-serif; font-size: 12pt; font-weight: none; color: #000000"><fmt:message key="jsp.search.advanced.option.to"/></label>
                <input onkeyup="validateDateNumber(true, '<%= dateIndexString %>');" type="text" name="query4" id="tquery4"  value="<%=StringEscapeUtils.escapeHtml(query4)%>" size="30" style="display:<%= displayType_2%>;"/>
              <p/>
            </td>

			</tr>
          </table>
            </td>
          </tr>
          <tr>
            <td width="12%" align="left" valign="top">
              <select name="conjunction1">
                <option value="AND" <%= conjunction1.equals("AND") ? "selected=\"selected\"" : "" %>> <fmt:message key="jsp.search.advanced.logical.and" /> </option>
		<option value="OR" <%= conjunction1.equals("OR") ? "selected=\"selected\"" : "" %>> <fmt:message key="jsp.search.advanced.logical.or" /> </option>
                <option value="NOT" <%= conjunction1.equals("NOT") ? "selected=\"selected\"" : "" %>> <fmt:message key="jsp.search.advanced.logical.not" /> </option>
              </select>
            </td>
            <td width="20%" align="left" valign="top" nowrap="nowrap">
                  <select onchange="onFieldChange2('<%= dateIndexString %>');" name="field2" id="tfield2">
                    <option value="ANY" <%= field2.equals("ANY") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.keyword"/></option>
                    <option value="author" <%= field2.equals("author") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.author"/></option>
                    <option value="title" <%= field2.equals("title") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.title"/></option>
                    <option value="keyword" <%= field2.equals("keyword") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.subject"/></option>
                    <option value="abstract" <%= field2.equals("abstract") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.abstract"/></option>
                    <option value="series" <%= field2.equals("series") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.series"/></option>
                    <option value="sponsor" <%= field2.equals("sponsor") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.sponsor"/></option>
                    <option value="identifier" <%= field2.equals("identifier") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.id"/></option>
                    <option value="language" <%= field2.equals("language") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.language"/></option>
                  </select>
           </td>

		   <td>
           <table border="0" cellpadding="0" cellspacing="0">
            <tr id="date_row2" height="25px">
              <%
                String displayType2 = "none"; 
                String displayType2_2 = "none";
                if (field2.equals("date")){
                	displayType2 = "block";
                	if (field5 != null && field5.equals("between")){
                		displayType2_2 = "block";
                	}
                }
             %>
            <td id="tfield5_column" width="100px" align="left" valign="top" nowrap="nowrap" style="display:<%= displayType2%>">
                <%-- Search type: <br> --%>
                <label id="tfield5_label" for="tfield5" style="display:none; font-family: "verdana","Arial","Helvetica",sans-serif; font-size: 12pt; font-weight: none; color: #000000"><fmt:message key="jsp.search.advanced.option.option"/></label>
                  <select onchange="onOptionChange2('<fmt:message key="jsp.search.advanced.option.from"/>','<fmt:message key="jsp.search.advanced.searchfor"/>');" name="field5" id="tfield5" style="display:<%= displayType2%> ;font-family:Arial, Helvetica, sans-serif;font-size:12px;height:20px;width:100px">
                  	<option value="equal" <%= field5.equals("equal") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.option.equal"/></option>
                    <option value="before" <%= field5.equals("before") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.option.before"/></option>
                    <option value="after" <%= field5.equals("after") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.option.after"/></option>
                  	<option value="between" <%= field5.equals("between") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.option.between"/></option>
                  </select>
            </td>

            <td align="left" valign="top" nowrap="nowrap" width="68%">
                <% if (field2 != null && field5!=null && field2.equals("date") && field5.equals("between")){ %>
              	<label id="tquery2_label" for="tquery2" style="display:block;font-family: "verdana","Arial","Helvetica",sans-serif; font-size: 12pt; font-weight: none; color: #000000"><fmt:message key="jsp.search.advanced.option.from"/></label>
              	<%} else {%>
              	<label id="tquery2_label" for="tquery2" style="display:none;font-family: "verdana","Arial","Helvetica",sans-serif; font-size: 12pt; font-weight: none; color: #000000"><fmt:message key="jsp.search.advanced.searchfor"/></label>
              	<%}%>
              <input onkeyup="validateDateNumber2(true, '<%= dateIndexString %>');" type="text" name="query2" id="tquery2" value="<%=StringEscapeUtils.escapeHtml(query2)%>" size="30"/>
               <br/>
               <label id="tquery5_label" for="tquery5" style="display:<%= displayType2_2%> ;font-family: "verdana","Arial","Helvetica",sans-serif; font-size: 12pt; font-weight: none; color: #000000"><fmt:message key="jsp.search.advanced.option.to"/></label>
               <input onkeyup="validateDateNumber2(true, '<%= dateIndexString %>');" type="text" name="query5" id="tquery5" style="display:<%= displayType2_2%>" value="<%=StringEscapeUtils.escapeHtml(query5)%>" size="30" style="font-family:Arial, Helvetica, sans-serif;font-size:12px;height:15px;"/>
              <p/>
            </td>
			</tr>
          </table>
            </td>
          </tr>
          <tr>
            <td width="12%" align="left" valign="top">
              <select name="conjunction2">
                <option value="AND" <%= conjunction2.equals("AND") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.logical.and" /> </option>
                <option value="OR" <%= conjunction2.equals("OR") ? "selected=\"selected\"" : "" %>> <fmt:message key="jsp.search.advanced.logical.or" /> </option>
                <option value="NOT" <%= conjunction2.equals("NOT") ? "selected=\"selected\""  : "" %>> <fmt:message key="jsp.search.advanced.logical.not" /> </option>
              </select>
            </td>
            <td width="20%" align="left" valign="top" nowrap="nowrap">

                  <select onchange="onFieldChange3('<%= dateIndexString %>');" name="field3" id="tfield3">
                    <option value="ANY" <%= field3.equals("ANY") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.keyword"/></option>
                    <option value="author" <%= field3.equals("author") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.author"/></option>
                    <option value="title" <%= field3.equals("title") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.title"/></option>
                    <option value="keyword" <%= field3.equals("keyword") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.subject"/></option>
                    <option value="abstract" <%= field3.equals("abstract") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.abstract"/></option>
                    <option value="series" <%= field3.equals("series") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.series"/></option>
                    <option value="sponsor" <%= field3.equals("sponsor") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.sponsor"/></option>
                    <option value="identifier" <%= field3.equals("identifier") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.id"/></option>
                    <option value="language" <%= field3.equals("language") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.language"/></option>
                  </select>
                  <br/>
            </td>
			<td>
           <table border="0" cellpadding="0" cellspacing="0">
            <tr id="date_row3" height="25px">
               <%
                String displayType3 = "none"; 
                String displayType3_2 = "none";
                if (field3.equals("date")){
                	displayType = "block";
                	if (field6 != null && field6.equals("between")){
                		displayType3_2 = "block";
                	}
                }
             %>
            <td id="tfield6_column" width="100px" align="left" valign="top" nowrap="nowrap" style="display:<%= displayType3%>">
                <%-- Search type: <br> --%>
                <label id="tfield6_label" for="tfield6" style="display:none; font-family: "verdana","Arial","Helvetica",sans-serif; font-size: 12pt; font-weight: none; color: #000000"><fmt:message key="jsp.search.advanced.option.option"/></label>
                  <select onchange="onOptionChange3('<fmt:message key="jsp.search.advanced.option.from"/>','<fmt:message key="jsp.search.advanced.searchfor"/>');" name="field6" id="tfield6" style="display:<%= displayType3%> ;font-family:Arial, Helvetica, sans-serif;font-size:12px;height:20px;width:100px">
                  	<option value="equal" <%= field6.equals("equal") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.option.equal"/></option>
                    <option value="before" <%= field6.equals("before") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.option.before"/></option>
                    <option value="after" <%= field6.equals("after") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.option.after"/></option>
                  	<option value="between" <%= field6.equals("between") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.option.between"/></option>
                  </select>
            </td>
            <td align="left" valign="top" nowrap="nowrap" width="68%">
                 <% if (field3 != null && field6!=null && field3.equals("date") && field6.equals("between")){ %>
              	<label id="tquery3_label" for="tquery3" style="display:block;font-family: "verdana","Arial","Helvetica",sans-serif; font-size: 12pt; font-weight: none; color: #000000"><fmt:message key="jsp.search.advanced.option.from"/></label>
              	<%} else {%>
              	<label id="tquery3_label" for="tquery3" style="display:none;font-family: "verdana","Arial","Helvetica",sans-serif; font-size: 12pt; font-weight: none; color: #000000"><fmt:message key="jsp.search.advanced.searchfor"/></label>
              	<%}%>           
               <input onkeyup="validateDateNumber3(true, '<%= dateIndexString %>');" type="text" name="query3" id="tquery3" value="<%=StringEscapeUtils.escapeHtml(query3)%>" size="30"/>
               <br/>
                <label id="tquery6_label" for="tquery6" style="display:<%= displayType3_2%>;font-family: "verdana","Arial","Helvetica",sans-serif; font-size: 12pt; font-weight: none; color: #000000"><fmt:message key="jsp.search.advanced.option.to"/></label>
                <input onkeyup="validateDateNumber3(true, '<%= dateIndexString %>');" type="text" name="query6" id="tquery6" style="display:<%= displayType3_2%>" value="<%=StringEscapeUtils.escapeHtml(query6)%>" size="30" style="font-family:Arial, Helvetica, sans-serif;font-size:12px;height:15px;"/>
              <p/>
            </td>
			</tr>
		  </table>
        </td>
  </tr>
  </table>
</td>
</tr>
  <tr>
    <td valign="bottom" align="right" nowrap="nowrap">
      &nbsp; &nbsp; &nbsp;
      <%-- <input type="submit" name="submit" value="Search"> --%>
	  <div id="error_log" style="color:red; display:none" align="left"></div>
      <input type="submit" name="submit" value="<fmt:message key="jsp.search.advanced.search2"/>" />
            &nbsp;  &nbsp; &nbsp;
      <%-- <input type="reset" name="reset" value=" Clear "> --%>
      <input type="reset" name="reset" value=" <fmt:message key="jsp.search.advanced.clear"/>" />
    </td>
  </tr>
</table>
</td>
</tr>
</table>
</form>

<% if( request.getParameter("query") != null)
{
    if( qResults.getErrorMsg()!=null )
    {
    	String qError = "jsp.search.error." + qResults.getErrorMsg();
    %>
        <p align="center" class="submitFormWarn"><fmt:message key="<%= qError %>"/></p>
     <%
    }else
    { %>
        <p align="center"><fmt:message key="jsp.search.general.noresults"/></p>
     <%
     }
}
%>

</dspace:layout>