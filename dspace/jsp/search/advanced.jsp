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
         <table cellspacing=2 border=1 width="80%">
		  <tr>
                <td class="evenRowEvenCol">
                    <table border=0>
            <tr>
            <td width="12%" align="left" valign="top"></td>
              <td width="20%" align="left" valign="top" nowrap>
                Search type: <br>

                  <select name="field1">
                    <option value="ANY" selected>Keyword</option>
                    <option value="author">Author</option>
                    <option value="title">Title</option>
                    <option value="subject">Subject</option>
                    <option value="abstract">Abstract</option>
                    <option value="series">Series</option>
                    <option value="sponsor">Sponsor</option>
                    <option value="identifier">ISSN/ISBN</option>
                  </select>
                </p>
            </td>
            <td align="left" valign="top" nowrap width="68%">

              	Search for: <br>
                <input type="text" name="query1" size="30">
                <br>
              </p>
            </td>
          </tr>
          <tr>
            <td width="12%" align="left" valign="top">
              <select name="conjunction1">
                <option value="AND" selected> AND </option>
                <option value="OR"> OR </option>

                <option value="NOT"> NOT </option>
              </select>
            </td>
            <td width="20%" align="left" valign="top" nowrap>
                  <select name="field2">
                    <option value="ANY" selected>Keyword</option>
                    <option value="author">Author</option>
                    <option value="title">Title</option>
                    <option value="subject">Subject</option>
                    <option value="abstract">Abstract</option>
                    <option value="series">Series</option>
                    <option value="sponsor">Sponsor</option>
                    <option value="identifier">ISSN/ISBN</option>
                  </select>
           </td>
            <td align="left" valign="top" nowrap width="68%">
              <input type="text" name="query2" size="30">
            </td>
          </tr>
          <tr>
            <td width="12%" align="left" valign="top">
              <select name="conjunction2">

                <option value="AND" selected> AND </option>
                <option value="OR"> OR </option>
                <option value="NOT"> NOT </option>
              </select>
            </td>
            <td width="20%" align="left" valign="top" nowrap>

                  <select name="field3">
                    <option value="ANY" selected>Keyword</option>
                    <option value="author">Author</option>
                    <option value="title">Title</option>
                    <option value="subject">Subject</option>
                    <option value="abstract">Abstract</option>
                    <option value="series">Series</option>
                    <option value="sponsor">Sponsor</option>
                    <option value="identifier">ISSN/ISBN</option>
                  </select>
                  <br>
            </td>
            <td align="left" valign="top" nowrap width="68%">
              <input type="text" name="query3" size="30">
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

</dspace:layout>
