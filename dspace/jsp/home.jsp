<%--
  - home.jsp
  -
  - Version: $Revision$
  -
  - Date: $Date$
  -
  - Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
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

<%-- Home page JSP --%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<dspace:layout style="home" title="Home">

  <h1>DSpace @ MIT Home</h1>

  <form action="<%= request.getContextPath() %>/simple-search" method=GET>
    <table border=0 cellpadding=4 cellspacing=8>
      <tr>
        <td colspan=2 bgcolor="#e0e0e0">
          <table width=100% border=0 cellpadding=6>
            <tr>
              <td valign=middle><h2>What can you find in DSpace?</h2></td>
              <td valign=middle align=right class="standard"><a href="http://www.dspace.org/">More Information</a>&nbsp;&nbsp;&nbsp;</td>
            </tr>
          </table>
          <table width=100%>
            <tr>
              <td width="50%">
                <UL>
                  <li>Articles</li>
                  <li>Technical Reports</li>
                  <li>Working Papers</li>
                </ul>
              </td>
              <td width="50%">
                <ul>
                  <li>Datasets</li>
                  <li>Images</li>
                  <li>Videos....and more</li>
                </ul>
              </td>
            </tr>
          </table>
        </td>
      </tr>
      <tr>
        <td width="50%" align=center bgcolor="#e0e0e0" valign=top>
          <H2>Search</H2>
          <P class="submitFormHelp">Enter some text in the box below to search DSpace.</P>
          <P><input type=text name=query size=20>&nbsp;<input type=submit name=submit value="Go"></P>
        </td>
        <td width="50%" align=center bgcolor="#e0e0e0" valign=top>
          <H2>Submit</H2>
          <P class="submitFormHelp">Submit your digital content to DSpace!  To
          start the submission process, select the button below.</P>
          <P align=center><A HREF="<%= request.getContextPath() %>/submit">Start Submitting</A></P>
          <P class="submitFormHelp"><strong>Authorized Users Only</strong></P>
        </td>
      </tr>
      <tr>
        <td bgcolor="#e0e0e0" valign=top>
          <H2 align=center>Browse</H2>
          <P class="submitFormHelp">You can also browse a DSpace index:</P>
          <UL>
            <LI><A HREF="<%= request.getContextPath() %>/community-list">Communities and collections</A></LI>
            <LI><A HREF="<%= request.getContextPath() %>/browse-title">Titles</A></LI>
            <LI><A HREF="<%= request.getContextPath() %>/browse-author">Authors</A></LI>
            <LI><A HREF="<%= request.getContextPath() %>/browse-date">Dates</A></LI>
          </UL>
        </td>
        <td align=center bgcolor="#e0e0e0" valign=top>
          <H2>My DSpace</H2>
          <P class="submitFormHelp">You can visit "My DSpace" to resume half-finished
          submissions and check on the progress of previous submissions.</P>
          <P align=center><A HREF="<%= request.getContextPath() %>/mydspace">Visit My DSpace</A></P>
          <P class="submitFormHelp"><strong>Authorized Users Only</strong></P>
        </td>
      </tr>
    </table>
  </form>

<%--  <dspace:sidebar>
    <%@ include file="news.jsp" %>
  </dspace:sidebar>
--%>
</dspace:layout>
