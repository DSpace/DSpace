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

        <%
    String sidebar = (String) request.getAttribute("dspace.layout.sidebar");
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
             <footer class="navbar navbar-inverse navbar-bottom">
             <div id="designedby" class="container text-muted">

    <!--PR -->
    <a href="https://www.prchecker.info/" title="PRchecker.info" target="_blank" rel="nofollow">
    <img src="https://pr-v2.prchecker.info/getpr.v2.php?codex=aHR0cDovL2Vzc3Vpci5zdW1kdS5lZHUudWEv&amp;tag=2"
    alt="PRchecker.info" style="border:0;"></a>
    <!--/PR -->
    <!--LiveInternet counter--><script type="text/javascript"><!--
    document.write("<a href='https://www.liveinternet.ru/click' "+
    "target=_blank><img src='//counter.yadro.ru/hit?t14.6;r"+
    escape(document.referrer)+((typeof(screen)=="undefined")?"":
    ";s"+screen.width+"*"+screen.height+"*"+(screen.colorDepth?
    screen.colorDepth:screen.pixelDepth))+";u"+escape(document.URL)+
    ";"+Math.random()+
    "' alt='' title='LiveInternet: показано число просмотров за 24"+
    " часа, посетителей за 24 часа и за сегодня' "+
    "border='0' width='88' height='31'><\/a>")
    //--></script><!--/LiveInternet-->

			<div id="footer_feedback" class="pull-right">                                    
                                <p class="text-muted"><fmt:message key="jsp.layout.footer-default.text"/>&nbsp;-
                                <a target="_blank" href="<%= request.getContextPath() %>/feedback"><fmt:message key="jsp.layout.footer-default.feedback"/></a>
                                <a href="<%= request.getContextPath() %>/htmlmap"></a></p>
                                </div>
			</div>
    </footer>
    <script type='text/javascript' src='<%= request.getContextPath() %>/static/js/jquery/jquery-ui-1.10.3.custom.min.js'></script>
    <script type='text/javascript' src='<%= request.getContextPath() %>/static/js/bootstrap/bootstrap.min.js'></script>
    <script type='text/javascript' src='<%= request.getContextPath() %>/static/js/holder.min.js'></script>
    <script type="text/javascript" src="<%= request.getContextPath() %>/utils.js"></script>
    <script type="text/javascript" src="<%= request.getContextPath() %>/static/js/choice-support.min.js"> </script>
    <script type="text/javascript" src="<%= request.getContextPath() %>/static/js/bootstrap-tree.js"> </script>
    <script src="<%= request.getContextPath() %>/static/js/yearpicker.min.js"></script>
    </body>
</html>