<%--
  - advanced.jsp
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
  - Advanced Search JSP
  -
  - 
  -   
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.content.Community"   %>

<%
    Community [] communityArray = (Community[] )request.getAttribute("communities");
	String query1 			= request.getParameter("query1") == null ? "" : request.getParameter("query1");
	String query2 			= request.getParameter("query2") == null ? "" : request.getParameter("query2");
	String query3 			= request.getParameter("query3") == null ? "" : request.getParameter("query3");
    	
	String field1 			= request.getParameter("field1") == null ? "ANY" : request.getParameter("field1");
	String field2 			= request.getParameter("field2") == null ? "ANY" : request.getParameter("field2");
	String field3 			= request.getParameter("field3") == null ? "ANY" : request.getParameter("field3");

	String conjunction1 	= request.getParameter("conjunction1") == null ? "AND" : request.getParameter("conjunction1");
	String conjunction2 	= request.getParameter("conjunction2") == null ? "AND" : request.getParameter("conjunction1");

%>

<dspace:layout locbar="nolink" title="Advanced Search">

<form action="<%= request.getContextPath() %>/simple-search" method=GET>
<input type=hidden name=advanced value="true">
<table class=miscTable align=center>
    <tr>
      <td class="oddRowEvenCol" align=center>
      	<p><strong>Search:</strong>&nbsp;
		<select name="location">
			<option selected value="/">All of DSpace</option>
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
         <table cellspacing=2 border=0 width="80%">
		  <tr>
                <td class="evenRowEvenCol">
                    <table border=0>
            <tr>
            <td width="12%" align="left" valign="top"></td>
              <td width="20%" align="left" valign="top" nowrap>
                Search type: <br>

                  <select name="field1">
                    <option value="ANY" <%= field1.equals("ANY") ? "selected" : "" %>>Keyword</option>
                    <option value="author" <%= field1.equals("author") ? "selected" : "" %>>Author</option>
                    <option value="title" <%= field1.equals("title") ? "selected" : "" %>>Title</option>
                    <option value="keyword" <%= field1.equals("keyword") ? "selected" : "" %>>Subject</option>
                    <option value="abstract" <%= field1.equals("abstract") ? "selected" : "" %>>Abstract</option>
                    <option value="series" <%= field1.equals("series") ? "selected" : "" %>>Series</option>
                    <option value="sponsor" <%= field1.equals("sponsor") ? "selected" : "" %>>Sponsor</option>
                    <option value="identifier" <%= field1.equals("identifier") ? "selected" : "" %>>Identifier</option>
                  </select>
                </p>
            </td>
            <td align="left" valign="top" nowrap width="68%">

              	Search for: <br>
                <input type="text" name="query1" value="<%= query1 %>" size="30">
                <br>
              </p>
            </td>
          </tr>
          <tr>
            <td width="12%" align="left" valign="top">
              <select name="conjunction1">
                <option value="AND" <%= conjunction1.equals("AND") ? "selected" : "" %>> AND </option>
                <option value="OR" <%= conjunction1.equals("OR") ? "selected" : "" %>> OR </option>
                <option value="NOT" <%= conjunction1.equals("NOT") ? "selected" : "" %>> NOT </option>
              </select>
            </td>
            <td width="20%" align="left" valign="top" nowrap>
                  <select name="field2">
                    <option value="ANY" <%= field2.equals("ANY") ? "selected" : "" %>>Keyword</option>
                    <option value="author" <%= field2.equals("author") ? "selected" : "" %>>Author</option>
                    <option value="title" <%= field2.equals("title") ? "selected" : "" %>>Title</option>
                    <option value="keyword" <%= field2.equals("keyword") ? "selected" : "" %>>Subject</option>
                    <option value="abstract" <%= field2.equals("abstract") ? "selected" : "" %>>Abstract</option>
                    <option value="series" <%= field2.equals("series") ? "selected" : "" %>>Series</option>
                    <option value="sponsor" <%= field2.equals("sponsor") ? "selected" : "" %>>Sponsor</option>
                    <option value="identifier" <%= field2.equals("identifier") ? "selected" : "" %>>Identifier</option>
                  </select>
           </td>
            <td align="left" valign="top" nowrap width="68%">
              <input type="text" name="query2" value="<%= query2 %>" size="30">
            </td>
          </tr>
          <tr>
            <td width="12%" align="left" valign="top">
              <select name="conjunction2">
                <option value="AND" <%= conjunction2.equals("AND") ? "selected" : "" %>> AND </option>
                <option value="OR" <%= conjunction2.equals("OR") ? "selected" : "" %>> OR </option>
                <option value="NOT" <%= conjunction2.equals("NOT") ? "selected" : "" %>> NOT </option>
              </select>
            </td>
            <td width="20%" align="left" valign="top" nowrap>

                  <select name="field3">
                    <option value="ANY" <%= field3.equals("ANY") ? "selected" : "" %>>Keyword</option>
                    <option value="author" <%= field3.equals("author") ? "selected" : "" %>>Author</option>
                    <option value="title" <%= field3.equals("title") ? "selected" : "" %>>Title</option>
                    <option value="keyword" <%= field3.equals("keyword") ? "selected" : "" %>>Subject</option>
                    <option value="abstract" <%= field3.equals("abstract") ? "selected" : "" %>>Abstract</option>
                    <option value="series" <%= field3.equals("series") ? "selected" : "" %>>Series</option>
                    <option value="sponsor" <%= field3.equals("sponsor") ? "selected" : "" %>>Sponsor</option>
                    <option value="identifier" <%= field3.equals("identifier") ? "selected" : "" %>>Identifier</option>
                  </select>
                  <br>
            </td>
            <td align="left" valign="top" nowrap width="68%">
              <input type="text" name="query3" value="<%= query3 %>" size="30">
            </td>

  </tr>
  </table>
  <tr>
    <td valign=bottom align=right NOWRAP>
      &nbsp; &nbsp; &nbsp;
      <input type="submit" name="submit" value="Search">
            &nbsp;  &nbsp; &nbsp;

      <input type="reset" name="reset" value=" Clear ">
    </td>
  </tr>
</table>
</table>
</form>

<% if( request.getParameter("query") != null )
{
 %>
    <P align=center>Search produced no results.</P>
<%
}
%>

</dspace:layout>
