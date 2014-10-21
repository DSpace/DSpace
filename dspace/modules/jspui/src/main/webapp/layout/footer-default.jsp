<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

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
    List<String> messages = (List<String>)request.getAttribute("dspace.layout.messages");
    if (messages != null)
    {
%>
                        <tr>
                            <td class="pageFootnote">
                              <table class="miscTable" width="100%">
<%
                                  boolean even = true;
                                  for (Iterator<String> i = (Iterator<String>) messages.iterator(); i.hasNext(); ) {
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
                                <fmt:message key="jsp.layout.footer-default.text"/>&nbsp;
                                  <a href="<%= request.getContextPath() %>/htmlmap"></a>
                            </td>
                            <td nowrap="nowrap" valign="middle"> <%-- nowrap, valign for broken NS 4.x --%>
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
        </table>

        <script src="//s3.amazonaws.com/umdheader.umd.edu/app/js/main.min.js"></script>
        <script> umdHeader.init(); </script>

        <script type="text/javascript">

var _gaq = _gaq || [];
_gaq.push(['_setAccount', 'UA-25605069-1']);
_gaq.push(['_trackPageview']);

(function() { var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true; ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js'; var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s); })();

        </script>
    </body>
</html>
