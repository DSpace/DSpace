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

<%
    String sidebar = (String) request.getAttribute("dspace.layout.sidebar");
    String itemVideoPreview = (String)request.getAttribute("item.video.preview.script");
    String extraHeadDataLast = (String)request.getAttribute("dspace.layout.head.last");
    String itemAudioPreview=(String)request.getAttribute("item.audio.preview.script");
%>

<%-- Right-hand side bar if appropriate --%>
<%
    if (sidebar != null) {
%>
                </div>
                <div class="col-md-3 col-sm-3 col-xs-8 sidebar-section sidebar-offcanvas">
                <!--<div class="col-md-3 col-sm-3 col-xs-6">-->
                    <%= sidebar%>
                </div>
           </div>       
<%
    }
%>
            </div>
        </main>
</div>
        <%-- Page footer --%>
        <footer class="navbar navbar-default navbar-bottom">

            <div class="icesi-university-info container text-center">
                <p>
                    Universidad Icesi, Calle 18 No. 122-135, Cali-Colombia Teléfono: +57 (2) 555 2334 | Fax: +57 (2) 555 1441 <br>
                    Copyright © 2014 <a href="http://www.icesi.edu.co/" target="_blank">www.icesi.edu.co</a> - 
                    <a title="Política de privacidad" href="/politica_privacidad.php">Política de privacidad</a> - 
                    <a href="/politica_de_tratamiento_de_datos_personales.php">Política de tratamiento de datos personales</a>
                </p>
                <small>La Universidad Icesi es una Institución de Educación Superior que se encuentra sujeta a inspección y vigilancia por parte del Ministerio de Educación Nacional</small>
            </div>

            <div id="designedby" class="container text-muted">
                <fmt:message key="jsp.layout.footer-default.theme-by"/> <a href="http://www.cineca.it">Cineca</a>
                <div id="footer_feedback" class="pull-right">                                    
                    <p class="text-muted"><fmt:message key="jsp.layout.footer-default.text"/>&nbsp;-
                        <a target="_blank" href="<%= request.getContextPath()%>/feedback"><fmt:message key="jsp.layout.footer-default.feedback"/></a>
                        <a href="<%= request.getContextPath()%>/htmlmap"></a></p>
                </div>
            </div>
        </footer>
                
        <!-- updated version of jquery -->
        <!-- uncomment for production enviroment -->
        <!--<script src="//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>-->
        <!--<script>window.jQuery || document.write("&lt;script src='<%= request.getContextPath() %>/static/js/jquery/jquery-1.10.2.min.js' &gt; &lt;\/script &gt; &lt;script src='<%= request.getContextPath() %>/static/js/jquery/jquery-ui-1.10.3.custom.min' &gt; &lt;\/script&gt;");</script>-->
        
        <script type='text/javascript' src="<%= request.getContextPath()%>/static/js/jquery/jquery-1.10.2.min.js"></script>
        <script type='text/javascript' src='<%= request.getContextPath()%>/static/js/jquery/jquery-ui-1.10.3.custom.min.js'></script>
        <script type='text/javascript' src='<%= request.getContextPath()%>/static/js/bootstrap/bootstrap.min.js'></script>
        <script type='text/javascript' src='<%= request.getContextPath()%>/static/js/holder.js'></script>
        <script type="text/javascript" src="<%= request.getContextPath()%>/utils.js"></script>
        <script type="text/javascript" src="<%= request.getContextPath()%>/static/js/choice-support.js"></script>
        <script type="text/javascript" src="<%= request.getContextPath()%>/static/js/jplayer/jquery.jplayer.min.js"></script>
        <!-- AddThis scripts for social media -->
        <!-- AddThis Smart Layers BEGIN -->
        <!-- Go to http://www.addthis.com/get/smart-layers to customize -->
        <script type="text/javascript" src="//s7.addthis.com/js/300/addthis_widget.js#pubid=ra-4e8dda135264b2f8"></script>
        <script type="text/javascript">
          addthis.layers({
            'theme' : 'transparent',
            'domain': 'bibliotecadigital.icesi.edu.co',
            'linkFilter' : function(link, layer) {
                console.log(link.title + ' - ' + link.url + " - " + layer);
                return link;
            },
            'share' : {
              'position' : 'left',
              'numPreferredServices' : 4,
              'services': 'facebook,twitter,google_plusone_share,more'
            }   
          });
        </script>
        <!-- AddThis Smart Layers END -->
        
        <!-- Custom scripsts -->
        <script src="<%= request.getContextPath() %>/static/js/app.js"></script>
<% 
    if (itemVideoPreview!=null ){
%> 
        <%= itemVideoPreview %>
<%
    }
    
    if(itemAudioPreview != null)
    {
  %> 
    <!-- Load Soundcloud SDK -->
    <script src="//connect.soundcloud.com/sdk.js"></script>
    <%= itemAudioPreview %>
  <% 
    }

    if (extraHeadDataLast != null)
    { 
%>
	<%= extraHeadDataLast %>
<%
    }
%>
    </body>
</html>