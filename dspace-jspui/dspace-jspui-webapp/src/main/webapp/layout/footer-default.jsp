<%--
  -- footer-home.jsp
  --
  -- Version: $Revision: 3705 $
  --
  -- Date: $Date: 2009-04-11 19:02:24 +0200 (Sat, 11 Apr 2009) $
  --
  -- Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
  -- Institute of Technology.  All rights reserved.
  --
  -- Redistribution and use in source and binary forms, with or without
  -- modification, are permitted provided that the following conditions are
  -- met:
  --
  -- - Redistributions of source code must retain the above copyright
  -- notice, this list of conditions and the following disclaimer.
  --
  -- - Redistributions in binary form must reproduce the above copyright
  -- notice, this list of conditions and the following disclaimer in the
  -- documentation and/or other materials provided with the distribution.
  --
  -- - Neither the name of the Hewlett-Packard Company nor the name of the
  -- Massachusetts Institute of Technology nor the names of their
  -- contributors may be used to endorse or promote products derived from
  -- this software without specific prior written permission.
  --
  -- THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  -- ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  -- LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  -- A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  -- HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  -- INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
  -- BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
  -- OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  -- ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  -- TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
  -- USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
  -- DAMAGE.
  --%>

<%--
  - Footer for home page
  --%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>

<%@ page import="java.net.URLEncoder" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>

<%
    String sidebar = (String) request.getAttribute("dspace.layout.sidebar");
    int overallColSpan = 3;
    if (sidebar == null)
    {
        overallColSpan = 2;
    }
%>
                    <%-- End of page content --%>
                    <p>&nbsp;</p>
                </td>

            <%-- Right-hand side bar if appropriate --%>
<%
    if (sidebar != null)
    {
%>
                <td class="sidebar">
                    <%= sidebar %>
                </td>
<%
    }
%>
            </tr>

            <%-- Page footer --%>
             <tr class="pageFooterBar">
                <td colspan="<%= overallColSpan %>" class="pageFootnote">
                    <table class="pageFooterBar" width="100%">
<%
    List messages = request.getAttribute("dspace.layout.messages");
    if (messages != null)
    {
%>
                        <tr>
                            <td class="pageFootnote">
                              <table class="miscTable" width="100%">
<%
                                  boolean even = true;
                                  for (Iterator i = ((List) messages.iterator(); i.hasNext(); ) {
                                    String tdclass = (even ? "evenRowEvenCol" : "oddRowEvenCol");
%>
                                    <tr>
                                      <td class="<%= tdclass %>"><%= i.next() %></td>
                                    </tr>
<%
                                    even = !even;
                                  }
%>

                              </table>
                            </td>
                            <td nowrap="nowrap" valign="middle"> <%-- nowrap, valign for broken NS 4.x --%>
                            </td>
                        </tr>
<%
    }
%>
                        <tr>
                            <td class="pageFootnote">
                                <fmt:message key="jsp.layout.footer-default.text"/>&nbsp;-
                                 <br>
                                 <a href="<%=request.getContextPath()%>/all.jsp">All Contents</a>
                            </td>
                            <td nowrap="nowrap" valign="middle"> <%-- nowrap, valign for broken NS 4.x --%>
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
        </table>
    </body>
</html>
