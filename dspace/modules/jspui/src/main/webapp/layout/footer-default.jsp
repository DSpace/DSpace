<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Footer for home page
  --%>

<%@page import="org.dspace.core.ConfigurationManager"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<%@ page contentType="text/html;charset=UTF-8"%>

<%@ page import="java.net.URLEncoder"%>
<%@ page import="org.dspace.app.webui.util.UIUtil"%>

<%
	String sidebar = (String) request
			.getAttribute("dspace.layout.sidebar");
%>

<%-- Right-hand side bar if appropriate --%>
<%
	if (sidebar != null) {
%>
</div>
<div class="col-md-3">
	<%=sidebar%>
</div>
</div>
<%
	}
%>
</div>
</main>
<%-- Page footer --%>
<footer class="navbar navbar-inverse navbar-bottom">

	<div id="tede-footer" class="container text-muted" style="padding-left: 0px;">
		
		<div class="text-center col-md-12">
       		
       		<div class="footer-logo pull-right">
			
				<a href="http://www.ibict.br/" class="default-padding-left" target="_blank">
					<img class="footer-logo pull-left" src="<%= request.getContextPath() %>/image/ibict-60.png"></a>
			
       		</div>
			
		</div>
		
	</div>

</footer>
</body>
</html>