<%
    String sidebar = (String) request.getAttribute("dspace.layout.sidebar");
    String navbar = (String) request.getAttribute("dspace.layout.navbar");
    int overallColSpan = 1;

    if (sidebar == null)
    {
        overallColSpan++;
    }
    
    if (!navbar.equals("off"))
    {
        overallColSpan++;
    }
%>
                    <%-- End of page content --%>
                    <P>&nbsp;</P>
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
<%
    String fromPage = (String) request.getAttribute( "dspace.original.url" );
    if (fromPage == null)
    {
        fromPage = request.getServletPath();
    }
    fromPage = java.net.URLEncoder.encode(fromPage);
%>
             <tr class="pageFooterBar">
                <td colspan=<%= overallColSpan %> class="pageFootnote">
                    Copyright&nbsp;&copy;&nbsp;2002&nbsp;<a target=_blank href="http://web.mit.edu/">MIT</a>&nbsp;and&nbsp;<a target=_blank href="http://www.hp.com/">Hewlett-Packard</a>&nbsp;-
                    <a target=_blank href="<%= request.getContextPath() %>/feedback?fromPage=<%= fromPage %>">Feedback</a>
                </td>
            </tr>
        </table>
    </body>
</html>
