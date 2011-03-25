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
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>


<%
    Community [] communityArray = (Community[] )request.getAttribute("communities");
	String query1 			= request.getParameter("query1") == null ? "" : request.getParameter("query1");
	String query2 			= request.getParameter("query2") == null ? "" : request.getParameter("query2");
	String query3 			= request.getParameter("query3") == null ? "" : request.getParameter("query3");

	String field1 			= request.getParameter("field1") == null ? "ANY" : request.getParameter("field1");
	String field2 			= request.getParameter("field2") == null ? "ANY" : request.getParameter("field2");
	String field3 			= request.getParameter("field3") == null ? "ANY" : request.getParameter("field3");

	String conjunction1 	= request.getParameter("conjunction1") == null ? "AND" : request.getParameter("conjunction1");
	String conjunction2 	= request.getParameter("conjunction2") == null ? "AND" : request.getParameter("conjunction2");

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
                  <select name="field1" id="tfield1">
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

            <td align="left" valign="top" nowrap="nowrap" width="68%">
                <%-- Search for: <br> --%>
              	<label for="tquery1"><fmt:message key="jsp.search.advanced.searchfor"/></label> <br/>
                <input type="text" name="query1" id="tquery1" value="<%=StringEscapeUtils.escapeHtml(query1)%>" size="30" />
                <br/>
              <p/>
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
                  <select name="field2">
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
            <td align="left" valign="top" nowrap="nowrap" width="68%">
              <input type="text" name="query2" value="<%=StringEscapeUtils.escapeHtml(query2)%>" size="30"/>
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

                  <select name="field3">
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
            <td align="left" valign="top" nowrap="nowrap" width="68%">
              <input type="text" name="query3" value="<%=StringEscapeUtils.escapeHtml(query3)%>" size="30"/>
            </td>

  </tr>
  </table>
</td>
</tr>
  <tr>
    <td valign="bottom" align="right" nowrap="nowrap">
      &nbsp; &nbsp; &nbsp;
      <%-- <input type="submit" name="submit" value="Search"> --%>
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
