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

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="java.net.URLEncoder" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>

<%
    String sidebar = (String) request.getAttribute("dspace.layout.sidebar");
    String feedData = "ALL:" + ConfigurationManager.getProperty("webui.feed.formats");
%>

<%-- Right-hand side bar if appropriate --%>
<%
    if (sidebar != null)
    {
%>
</div>
<div class="col-md-3">
    <%= sidebar %>
</div>
</div>
<%
    }
%>
</div>
</main>
<%-- Page footer --%>
<footer class="navbar navbar-inverse navbar-bottom navbar-fixed-bottom">
    <div id="designedby" class="container text-muted">

        <!--PR -->
        <a href="http://www.prchecker.info/" title="PRchecker.info" target="_blank" rel="nofollow">
            <img src="http://pr-v2.prchecker.info/getpr.v2.php?codex=aHR0cDovL2Vzc3Vpci5zdW1kdS5lZHUudWEv&amp;tag=2" alt="PRchecker.info" style="border:0;"></a>
        <!--/PR -->
        <!--LiveInternet counter--><script type="text/javascript"><!--
    document.write("<a href='http://www.liveinternet.ru/click' "+
            "target=_blank><img src='//counter.yadro.ru/hit?t14.6;r"+
            escape(document.referrer)+((typeof(screen)=="undefined")?"":
            ";s"+screen.width+"*"+screen.height+"*"+(screen.colorDepth?
                    screen.colorDepth:screen.pixelDepth))+";u"+escape(document.URL)+
            ";"+Math.random()+
            "' alt='' title='LiveInternet: показано число просмотров за 24"+
            " часа, посетителей за 24 часа и за сегодня' "+
            "border='0' width='88' height='31'><\/a>")
    //--></script><!--/LiveInternet-->

        <%
            String[] fmts = feedData.substring(feedData.indexOf(':')+1).split(",");
            String icon = null;
            int width = 0;
            for (int j = 0; j < fmts.length; j++)
            {
                if ("rss_1.0".equals(fmts[j]))
                {
                    icon = "rss1.gif";
                    width = 80;
                }
                else if ("rss_2.0".equals(fmts[j]))
                {
                    icon = "rss2.gif";
                    width = 80;
                }
                else
                {
                    icon = "rss.gif";
                    width = 36;
                }
        %>
        <!--Openstat--><span id="openstat2145233"></span><script type="text/javascript">
        var openstat = { counter: 2145233, image: 5065, next: openstat }; (function(d, t, p) {
            var j = d.createElement(t); j.async = true; j.type = "text/javascript";
            j.src = ("https:" == p ? "https:" : "http:") + "//openstat.net/cnt.js";
            var s = d.getElementsByTagName(t)[0]; s.parentNode.insertBefore(j, s);
        })(document, "script", document.location.protocol);
    </script><!--/Openstat-->
        <%
            }
        %>
        <div id="footer_feedback" class="pull-right" style="font-size:10pt">
            <p class="text-muted"><fmt:message key="jsp.layout.footer-default.text"/>&nbsp;-
                <a target="_blank" href="<%= request.getContextPath() %>/feedback"><fmt:message key="jsp.layout.footer-default.feedback"/></a>
                <a href="<%= request.getContextPath() %>/htmlmap"></a></p>
        </div>
    </div>
</footer>

</body>
</html>