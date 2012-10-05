
<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
                xmlns:dri="http://di.tamu.edu/DRI/1.0/"
                xmlns:mets="http://www.loc.gov/METS/"
                xmlns:xlink="http://www.w3.org/TR/xlink/"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
                xmlns:xhtml="http://www.w3.org/1999/xhtml"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:java="http://xml.apache.org/xalan/java"
                xmlns:mods="http://www.loc.gov/mods/v3"
                xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:confman="org.dspace.core.ConfigurationManager"
                xmlns="http://www.w3.org/1999/xhtml"
                exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc confman">

    <xsl:output indent="yes"/>


    <!-- Se redefine este template para insertar el menu superior -->
    <xsl:template match="dri:document">
        <html class="no-js">
            <!-- First of all, build the HTML head element -->
            <xsl:call-template name="buildHead"/>
            <!-- Then proceed to the body -->

            <!--paulirish.com/2008/conditional-stylesheets-vs-css-hacks-answer-neither/-->
            <xsl:text disable-output-escaping="yes">&lt;!--[if lt IE 7 ]&gt; &lt;body class="ie6"&gt; &lt;![endif]--&gt;
                &lt;!--[if IE 7 ]&gt;    &lt;body class="ie7"&gt; &lt;![endif]--&gt;
                &lt;!--[if IE 8 ]&gt;    &lt;body class="ie8"&gt; &lt;![endif]--&gt;
                &lt;!--[if IE 9 ]&gt;    &lt;body class="ie9"&gt; &lt;![endif]--&gt;
                &lt;!--[if (gt IE 9)|!(IE)]&gt;&lt;!--&gt;&lt;body&gt;&lt;!--&lt;![endif]--&gt;</xsl:text>

            <xsl:choose>
                <xsl:when test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='framing'][@qualifier='popup']">
                    <xsl:apply-templates select="dri:body/*"/>
                </xsl:when>
                <xsl:otherwise>
                    <div id="ds-main">
                        <!--The header div, complete with title, subtitle and other junk-->
                        <xsl:call-template name="buildHeader"/>

                        <xsl:call-template name="menuSuperior"/>

                        <!--The trail is built by applying a template over pageMeta's trail children. -->
                        <xsl:call-template name="buildTrail"/>

                        <!--javascript-disabled warning, will be invisible if javascript is enabled-->
                        <div id="no-js-warning-wrapper" class="hidden">
                            <div id="no-js-warning">
                                <div class="notice failure">
                                    <xsl:text>JavaScript is disabled for your browser. Some features of this site may not work without it.</xsl:text>
                                </div>
                            </div>
                        </div>


                        <!--ds-content is a groups ds-body and the navigation together and used to put the clearfix on, center, etc.
                            ds-content-wrapper is necessary for IE6 to allow it to center the page content-->
                        <div id="ds-content-wrapper">
                            <div id="ds-content" class="clearfix">
                                <!--
                               Goes over the document tag's children elements: body, options, meta. The body template
                               generates the ds-body div that contains all the content. The options template generates
                               the ds-options div that contains the navigation and action options available to the
                               user. The meta element is ignored since its contents are not processed directly, but
                               instead referenced from the different points in the document. -->
                                <xsl:apply-templates/>
                            </div>
                        </div>


                        <!--
                            The footer div, dropping whatever extra information is needed on the page. It will
                            most likely be something similar in structure to the currently given example. -->
                        <xsl:call-template name="buildRedesSociales"/>
                        <xsl:call-template name="buildFooter"/>

                    </div>

                </xsl:otherwise>
            </xsl:choose>
            <!-- Javascript at the bottom for fast page loading -->
            <xsl:call-template name="addJavascript"/>

            <xsl:text disable-output-escaping="yes">&lt;/body&gt;</xsl:text>
        </html>
    </xsl:template>

    <!-- The HTML head element contains references to CSS as well as embedded JavaScript code. Most of this
        information is either user-provided bits of post-processing (as in the case of the JavaScript), or
        references to stylesheets pulled directly from the pageMeta element. -->
    <xsl:template name="buildHead">
        <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>

            <!-- Always force latest IE rendering engine (even in intranet) & Chrome Frame -->
            <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1"/>

            <!--  Mobile Viewport Fix
                  j.mp/mobileviewport & davidbcalhoun.com/2010/viewport-metatag
            device-width : Occupy full width of the screen in its current orientation
            initial-scale = 1.0 retains dimensions instead of zooming out if page height > device height
            maximum-scale = 1.0 retains dimensions instead of zooming in if page width < device width
            -->
            <meta name="viewport" content="width=device-width; initial-scale=1.0; maximum-scale=1.0;"/>

            <link rel="shortcut icon">
                <xsl:attribute name="href">
                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                    <xsl:text>/themes/</xsl:text>
                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                    <xsl:text>/images/favicon.ico</xsl:text>
                </xsl:attribute>
            </link>
            <link rel="apple-touch-icon">
                <xsl:attribute name="href">
                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                    <xsl:text>/themes/</xsl:text>
                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                    <xsl:text>/images/apple-touch-icon.png</xsl:text>
                </xsl:attribute>
            </link>

            <meta name="Generator">
                <xsl:attribute name="content">
                    <xsl:text>DSpace</xsl:text>
                    <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='dspace'][@qualifier='version']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='dspace'][@qualifier='version']"/>
                    </xsl:if>
                </xsl:attribute>
            </meta>
            <!-- Add stylsheets -->
            <xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='stylesheet']">
                <link rel="stylesheet" type="text/css">
                    <xsl:attribute name="media">
                        <xsl:value-of select="@qualifier"/>
                    </xsl:attribute>
                    <xsl:attribute name="href">
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                        <xsl:text>/themes/</xsl:text>
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                        <xsl:text>/</xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:attribute>
                </link>
            </xsl:for-each>

            <link rel="stylesheet" type="text/css" media="screen">
                <xsl:attribute name="href">
                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                    <xsl:text>/themes/</xsl:text>
                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                    <xsl:text>/lib/css/metadataGenerator.css</xsl:text>
                </xsl:attribute>
            </link>

            <!-- Add syndication feeds -->
            <xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='feed']">
                <link rel="alternate" type="application">
                    <xsl:attribute name="type">
                        <xsl:text>application/</xsl:text>
                        <xsl:value-of select="@qualifier"/>
                    </xsl:attribute>
                    <xsl:attribute name="href">
                        <xsl:value-of select="."/>
                    </xsl:attribute>
                </link>
            </xsl:for-each>

            <!--  Add OpenSearch auto-discovery link -->
            <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='opensearch'][@qualifier='shortName']">
                <link rel="search" type="application/opensearchdescription+xml">
                    <xsl:attribute name="href">
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='scheme']"/>
                        <xsl:text>://</xsl:text>
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='serverName']"/>
                        <xsl:text>:</xsl:text>
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='serverPort']"/>
                        <xsl:value-of select="$context-path"/>
                        <xsl:text>/</xsl:text>
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='opensearch'][@qualifier='autolink']"/>
                    </xsl:attribute>
                    <xsl:attribute name="title" >
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='opensearch'][@qualifier='shortName']"/>
                    </xsl:attribute>
                </link>
            </xsl:if>

            <!-- The following javascript removes the default text of empty text areas when they are focused on or submitted -->
            <!-- There is also javascript to disable submitting a form when the 'enter' key is pressed. -->
            <script type="text/javascript">
                //Clear default text of emty text areas on focus
                function tFocus(element)
                {
                if (element.value == '<i18n:text>xmlui.dri2xhtml.default.textarea.value</i18n:text>'){element.value='';}
                }
                //Clear default text of emty text areas on submit
                function tSubmit(form)
                {
                var defaultedElements = document.getElementsByTagName("textarea");
                for (var i=0; i != defaultedElements.length; i++){
                if (defaultedElements[i].value == '<i18n:text>xmlui.dri2xhtml.default.textarea.value</i18n:text>'){
                defaultedElements[i].value='';}}
                }
                //Disable pressing 'enter' key to submit a form (otherwise pressing 'enter' causes a submission to start over)
                function disableEnterKey(e)
                {
                var key;

                if(window.event)
                key = window.event.keyCode;     //Internet Explorer
                else
                key = e.which;     //Firefox and Netscape

                if(key == 13)  //if "Enter" pressed, then disable!
                return false;
                else
                return true;
                }

                function FnArray()
                {
                this.funcs = new Array;
                }

                FnArray.prototype.add = function(f)
                {
                if( typeof f!= "function" )
                {
                f = new Function(f);
                }
                this.funcs[this.funcs.length] = f;
                };

                FnArray.prototype.execute = function()
                {
                for( var i=0; i <xsl:text disable-output-escaping="yes">&lt;</xsl:text> this.funcs.length; i++ )
                {
                this.funcs[i]();
                }
                };

                var runAfterJSImports = new FnArray();
            </script>

            <!-- Modernizr enables HTML5 elements & feature detects -->
            <script type="text/javascript">
                <xsl:attribute name="src">
                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                    <xsl:text>/themes/</xsl:text>
                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                    <xsl:text>/lib/js/modernizr-1.7.min.js</xsl:text>
                </xsl:attribute>&#160;</script>

            <!-- Add the title in -->
            <xsl:variable name="page_title" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='title']" />
            <title>
                <xsl:choose>
                    <xsl:when test="starts-with($request-uri, 'page/about')">
                        <xsl:text>About This Repository</xsl:text>
                    </xsl:when>
                    <xsl:when test="not($page_title)">
                        <xsl:text>  </xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:copy-of select="$page_title/node()" />
                    </xsl:otherwise>
                </xsl:choose>
            </title>

            <!-- Head metadata in item pages -->
            <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='xhtml_head_item']">
                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='xhtml_head_item']"
                              disable-output-escaping="yes"/>
            </xsl:if>

            <!-- Add all Google Scholar Metadata values -->
            <xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[substring(@element, 1, 9) = 'citation_']">
                <meta name="{@element}" content="{.}"></meta>
            </xsl:for-each>

        </head>
    </xsl:template>

    <!-- The header (distinct from the HTML head element) contains the title, subtitle, login box and various
placeholders for header images -->
    <xsl:template name="buildHeader">
        <div id="ds-header-wrapper">
            <div id="ds-header" class="clearfix">
                <a id="ds-header-logo-link">
                    <xsl:attribute name="href">
                        <xsl:value-of
                                select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                        <xsl:text>/</xsl:text>
                    </xsl:attribute>
                    <span id="ds-header-logo">&#160;</span>
                    <span id="ds-header-logo-text"><i18n:text>xmlui.general.dspace_home</i18n:text></span>
                </a>
                <h1 class="pagetitle visuallyhidden">
                    <xsl:choose>
                        <!-- protectiotion against an empty page title -->
                        <xsl:when test="not(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='title'])">
                            <xsl:text> </xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:copy-of
                                    select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='title']/node()"/>
                        </xsl:otherwise>
                    </xsl:choose>

                </h1>
                <h2 class="static-pagetitle visuallyhidden">
                    <i18n:text>xmlui.dri2xhtml.structural.head-subtitle</i18n:text>
                </h2>


                <xsl:choose>
                    <xsl:when test="/dri:document/dri:meta/dri:userMeta/@authenticated = 'yes'">
                        <!-- Genero la seccion de la cuenta del usuario -->
                        <div id="ds-user-box">
                            <div id='div-menu-lateral-cuenta-superior'>
                                <div id='div-menu-lateral-cuenta-superior-izquierdo'>
                                    <!--     				      	<h1><i18n:text>sedici.menuLateral.cuenta.usuario</i18n:text></h1> -->
                                    <h2><xsl:call-template name="buildUsuarioName"/></h2>
                                </div>
                                <div id='div-menu-lateral-cuenta-superior-derecho'>
                                    <a href="{$context-path}/logout"><i18n:text>sedici.menuLateral.cuenta.logout</i18n:text></a>
                                </div>
                            </div>
                            <div id='div-menu-lateral-cuenta-inferior'>
                                <ul>
                                    <li><a href="{$context-path}/profile"><i18n:text>sedici.menuLateral.cuenta.perfil</i18n:text></a></li>
                                    <li><a href="{$context-path}/submissions"><i18n:text>sedici.menuLateral.cuenta.submissions</i18n:text></a></li>
                                    <xsl:if test="count(dri:list[@id='aspect.viewArtifacts.Navigation.list.account']/dri:item)>3">
                                        <li><a href="{$context-path}/admin/export"><i18n:text>sedici.menuLateral.cuenta.export</i18n:text></a></li>
                                    </xsl:if>
                                </ul>
                            </div>

                        </div>
                    </xsl:when>
                    <xsl:otherwise>
                        <div id="ds-user-box">
                            <p>
                                <a>
                                    <xsl:attribute name="href">
                                        <xsl:value-of select="/dri:document/dri:meta/dri:userMeta/dri:metadata[@element='identifier' and @qualifier='loginURL']"/>
                                    </xsl:attribute>
                                    <i18n:text>xmlui.dri2xhtml.structural.login</i18n:text>
                                </a>
                            </p>
                        </div>
                    </xsl:otherwise>
                </xsl:choose>

            </div>
        </div>
    </xsl:template>
    <xsl:template name="buildRedesSociales">
        <div id="redesSociales">
            <img width="274" height="72" border="0" usemap="#Map">
                <xsl:attribute name="src">
                    <xsl:value-of select="//dri:pageMeta/dri:metadata[@element='contextPath']"/>
                    <xsl:text>/themes/Sedici/images/redes3.png</xsl:text>
                </xsl:attribute>
            </img>

            <map name="Map">
                <area title="RSS" alt="RSS - Sedici" coords="4,0,69,74" shape="rect" >
                    <xsl:attribute name="href">
                        <xsl:value-of select="//dri:pageMeta/dri:metadata[@element='feed_site' and @qualifier='atom+xml']" />
                    </xsl:attribute>
                </area>
                <area target="_blank" title="Twitter" alt="Twitter - Sedici" href="http://twitter.com/sedici_unlp" coords="72,0,136,74" shape="rect" />
                <area title="Blog" alt="Blog- Sedici" href="http://sedici.unlp.edu.ar/blog/" target="_blank" coords="139,0,195,74" shape="rect" />
                <area target="_blank" title="Facebook" alt="Facebook - Sedici" href="http://www.facebook.com/home.php#!/pages/SeDiCI/106194779404955?ref=ts" coords="198,0,266,74" shape="rect" />
            </map>
        </div>
    </xsl:template>
    <!-- Creo el footer especifico para SeDiCI -->
    <xsl:template name="buildFooter">
        <div id="footer">

            <div id="footercol1">
                <div class="datos_unlp">
                    <strong>2003-2012 &#xA9; <a href="http://prebi.unlp.edu.ar/" target="_blank">PrEBi</a></strong>
                    <br/>
                    <a href="http://www.unlp.edu.ar" target="_blank">Universidad Nacional de La Plata</a>
                    <br/>
                    Todos los derechos reservados conforme a la ley 11.723
                </div>
            </div>
            <div id="footercol2">
                <div class="datos_sedici">
                    <strong>SeDiCI - Servicio de Difusión de la Creación Intelectual</strong>
                    <br/>
                    Calle 49 y 115 s/n 1er piso - Edificio ex Liceo
                    <br/>
                    1900 La Plata, Buenos Aires - Tel 0221 423 6696/6677 (int. 141)
                    <div id="comollegar">
                        <a title="Como llegar a SeDiCI">
                            <xsl:attribute name="href">
                                <xsl:value-of select="//dri:pageMeta/dri:metadata[@element='contextPath']"/>
                                <xsl:text>/pages/comoLlegar</xsl:text>
                            </xsl:attribute>
                            <img class="vermapa">
                                <xsl:attribute name="src">
                                    <xsl:value-of select="//dri:pageMeta/dri:metadata[@element='contextPath']"/>
                                    <xsl:text>/themes/Sedici/images/img1.png</xsl:text>
                                </xsl:attribute>
                            </img>
                        </a>
                    </div>
                </div>
                <div id="news-contacto-etc">
                    <a href="http://mail.prebi.unlp.edu.ar:8080/lists" target="_blank" title="Suscríbase al newsletter">
                        <img class="newsletter">
                            <xsl:attribute name="src">
                                <xsl:value-of select="//dri:pageMeta/dri:metadata[@element='contextPath']"/>
                                <xsl:text>/themes/Sedici/images/img2.png</xsl:text>
                            </xsl:attribute>
                        </img>
                    </a>

                    <a title="Contáctese">
                        <xsl:attribute name="href">
                            <xsl:value-of select="//dri:pageMeta/dri:metadata[@element='contextPath']"/>
                            <xsl:text>/feedback</xsl:text>
                        </xsl:attribute>
                        <img class="contacto">
                            <xsl:attribute name="src">
                                <xsl:value-of select="//dri:pageMeta/dri:metadata[@element='contextPath']"/>
                                <xsl:text>/themes/Sedici/images/img3.png</xsl:text>
                            </xsl:attribute>
                        </img>
                    </a>
                </div>
            </div>
        </div>
    </xsl:template>

    <xsl:template name="buildUsuarioName">
        <xsl:value-of select="/dri:document/dri:meta/dri:userMeta/dri:metadata[@element='identifier' and @qualifier='firstName']"/>
        <xsl:text> </xsl:text>
        <xsl:value-of select="/dri:document/dri:meta/dri:userMeta/dri:metadata[@element='identifier' and @qualifier='lastName']"/>
    </xsl:template>

    <xsl:template name="addJavascript">
        <xsl:variable name="jqueryVersion">
            <xsl:text>1.6.2</xsl:text>
        </xsl:variable>

        <xsl:variable name="protocol">
            <xsl:choose>
                <xsl:when test="starts-with(confman:getProperty('dspace.baseUrl'), 'https://')">
                    <xsl:text>https://</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>http://</xsl:text>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <script type="text/javascript" src="{concat($protocol, 'ajax.googleapis.com/ajax/libs/jquery/', $jqueryVersion ,'/jquery.min.js')}">&#160;</script>

        <xsl:variable name="localJQuerySrc">
            <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
            <xsl:text>/static/js/jquery-</xsl:text>
            <xsl:value-of select="$jqueryVersion"/>
            <xsl:text>.min.js</xsl:text>
        </xsl:variable>

        <script type="text/javascript">
            <xsl:text disable-output-escaping="yes">!window.jQuery &amp;&amp; document.write('&lt;script type="text/javascript" src="</xsl:text><xsl:value-of
                select="$localJQuerySrc"/><xsl:text disable-output-escaping="yes">"&gt;&#160;&lt;\/script&gt;')</xsl:text>
        </script>

        <!-- Add theme javascipt  -->
        <xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='javascript'][not(@qualifier)]">
            <script type="text/javascript">
                <xsl:attribute name="src">
                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                    <xsl:text>/themes/</xsl:text>
                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                    <xsl:text>/</xsl:text>
                    <xsl:value-of select="."/>
                </xsl:attribute>&#160;</script>
        </xsl:for-each>

        <!-- add "shared" javascript from static, path is relative to webapp root-->
        <xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='javascript'][@qualifier='static']">
            <!--This is a dirty way of keeping the scriptaculous stuff from choice-support
            out of our theme without modifying the administrative and submission sitemaps.
            This is obviously not ideal, but adding those scripts in those sitemaps is far
            from ideal as well-->
            <!-- En caso de que cargamos en el DRI un choice-support especial (el NO ESTATICO) en el DRI, el choice-support.js viejo debe obviarse. -->
            <xsl:choose>
                <xsl:when test="text() = 'static/js/choice-support.js'">
                    <xsl:if test="not(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='javascript'][not(@qualifier)][text()='lib/js/choice-support.js'])">
                        <script type="text/javascript">
                            <xsl:attribute name="src">
                                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                                <xsl:text>/themes/</xsl:text>
                                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                                <xsl:text>/lib/js/choice-support.js</xsl:text>
                            </xsl:attribute>&#160;
                        </script>
                    </xsl:if>
                </xsl:when>
                <xsl:when test="not(starts-with(text(), 'static/js/scriptaculous'))">
                    <script type="text/javascript">
                        <xsl:attribute name="src">
                            <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                            <xsl:text>/</xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:attribute>&#160;</script>
                </xsl:when>
            </xsl:choose>


            <!--             <xsl:choose> -->
            <!--                 <xsl:when test="(text() = 'static/js/choice-support.js') and (/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='javascript'][not(@qualifier)][text()='lib/js/choice-support.js'])"> -->

            <!--                 </xsl:when> -->

            <!--                 <xsl:otherwise> -->
            <!--                 <script type="text/javascript"> -->
            <!-- 	                        <xsl:attribute name="src"> -->
            <!-- 	                            <xsl:value-of -->
            <!-- 	                                    select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/> -->
            <!-- 	                            <xsl:text>/</xsl:text> -->
            <!-- 	                            <xsl:value-of select="."/> -->
            <!-- 	                        </xsl:attribute>&#160;</script> -->
            <!-- 	                <xsl:if test="not(starts-with(text(), 'static/js/scriptaculous'))"> -->
            <!-- 	                    <script type="text/javascript"> -->
            <!-- 	                        <xsl:attribute name="src"> -->
            <!-- 	                            <xsl:value-of -->
            <!-- 	                                    select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/> -->
            <!-- 	                            <xsl:text>/</xsl:text> -->
            <!-- 	                            <xsl:value-of select="."/> -->
            <!-- 	                        </xsl:attribute>&#160;</script> -->
            <!-- 	                </xsl:if> -->
            <!--                 </xsl:otherwise> -->
            <!--             </xsl:choose> -->
        </xsl:for-each>

        <!-- add setup JS code if this is a choices lookup page -->
        <xsl:if test="dri:body/dri:div[@n='lookup']">
            <xsl:call-template name="choiceLookupPopUpSetup"/>
        </xsl:if>

        <!--PNG Fix for IE6-->
        <xsl:text disable-output-escaping="yes">&lt;!--[if lt IE 7 ]&gt;</xsl:text>
        <script type="text/javascript">
            <xsl:attribute name="src">
                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/themes/</xsl:text>
                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                <xsl:text>/lib/js/DD_belatedPNG_0.0.8a.js?v=1</xsl:text>
            </xsl:attribute>&#160;</script>
        <script type="text/javascript">
            <xsl:text>DD_belatedPNG.fix('#ds-header-logo');DD_belatedPNG.fix('#ds-footer-logo');$.each($('img[src$=png]'), function() {DD_belatedPNG.fixPng(this);});</xsl:text>
        </script>
        <xsl:text disable-output-escaping="yes" >&lt;![endif]--&gt;</xsl:text>


        <script type="text/javascript">
            runAfterJSImports.execute();
        </script>

        <!-- Add a google analytics script if the key is present -->
        <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='google'][@qualifier='analytics']">
            <script type="text/javascript"><xsl:text>
                   var _gaq = _gaq || [];
                   _gaq.push(['_setAccount', '</xsl:text><xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='google'][@qualifier='analytics']"/><xsl:text>']);
                   _gaq.push(['_trackPageview']);

                   (function() {
                       var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
                       ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
                       var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
                   })();
           </xsl:text></script>
        </xsl:if>

        <script type="text/javascript">
			<xsl:text>
			$(document).ready(function() {
				//add indicators and hovers to submenu parents
				$("#topNav li.main").each(function() {
					if ($(this).children("ul").length) {

						//show subnav on hover
						$(this).mouseenter(function() {
							$(this).children("ul").show();
						});

						//hide submenus on exit
						$(this).mouseleave(function() {
							$(this).children("ul").hide();
						});
					}
				});
			});

			$(document).ready(function() {
				//click event on confident indicators
				$("img.ds-authority-confidence").click(function() {
					var self = $(this).siblings('.ds-authority-label').toggle();
				});
			});

			</xsl:text>
        </script>

        <script type="text/javascript">
            <xsl:attribute name="src">
                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]" />
                <xsl:text>/themes/</xsl:text>
                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']" />
                <xsl:text>/lib/js/slides.min.jquery.js</xsl:text>
            </xsl:attribute>
            &#160;
        </script>

        <script type="text/javascript">
            <xsl:attribute name="src">
                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]" />
                <xsl:text>/themes/</xsl:text>
                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']" />
                <xsl:text>/lib/js/metadataGenerator.js</xsl:text>
            </xsl:attribute>
            &#160;
        </script>

        <script type="text/javascript">
            $(function() {
            $( "#community-tabs" ).tabs({
            collapsible: false,
            fx: {opacity: 'toggle', duration: 'fast'}
            });
            });
        </script>

        <!-- When on submission process, disable buttons on submission -->
        <xsl:if test="dri:body/dri:div[contains(@rend,'submission')]">
            <script type="text/javascript">
                $(function() {

                    $('form.submission').submit(function() {
                        if($(this).data("submitted") === true)
                            return false;
                        else
                            $(this).data("submitted", true);
                    });

                    // Variable que guarda el valor del select para chequear si hay que disparar el alert() o no
                    var dcTypeElement = $('form.submission select[name="dc_type"]');
                    var oldTypeValue = dcTypeElement.val();
                    $('form.submission select[name="dc_type"]').change(function() {
                        var permitirSubmit = false;
                        if(oldTypeValue == "")
                            permitirSubmit = true;
                        else
                            permitirSubmit = confirm("¿Está seguro que desea cambiar el tipo de documento?");

                        if(permitirSubmit) {
                            //Limpiamos el subtype para evitar que quede inconsistente
                            $('form.submission select[name="sedici_subtype"]').val("");
                            $('form.submission').submit();
                        } else {
                            dcTypeElement.val(oldTypeValue);
                        }
                    });

                });
            </script>
        </xsl:if>

        <!-- When on submission process, disable buttons on submission -->
        <xsl:if test="dri:body/dri:div[contains(@rend,'administrative community') or contains(@rend,'administrative collection')]">
            <script type="text/javascript">
                $(function() {
                    $("form.administrative.community textarea[name='introductory_text']").metadataGenerator();
                    $("form.administrative.collection textarea[name='introductory_text']").metadataGenerator();
                });
            </script>
        </xsl:if>

    </xsl:template>

    <!--
        The template to handle the dri:body element. It simply creates the ds-body div and applies
        templates of the body's child elements (which consists entirely of dri:div tags).
    -->
    <xsl:template match="dri:body">
        <div id="ds-body">
            <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='alert'][@qualifier='message']">
                <div id="ds-system-wide-alert">
                    <p>
                        <xsl:copy-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='alert'][@qualifier='message']/node()"/>
                    </p>
                </div>
            </xsl:if>

            <xsl:if test="/dri:document/dri:body/dri:div[@id='ar.edu.unlp.sedici.util.FlashMessagesTransformer.div.flash-message']">
                <xsl:apply-templates select="/dri:document/dri:body/dri:div[@id='ar.edu.unlp.sedici.util.FlashMessagesTransformer.div.flash-message']">
                    <xsl:with-param name="muestra">true</xsl:with-param>
                </xsl:apply-templates>
            </xsl:if>



            <xsl:apply-templates select="dri:div[@id='aspect.discovery.SiteViewer.div.front-page-search']">
                <xsl:with-param name="muestra">true</xsl:with-param>
            </xsl:apply-templates>



            <xsl:apply-templates/>


            <!-- <xsl:if test="not(java:ar.edu.unlp.sedici.xmlui.xsl.XslExtensions.matches(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='URI'], 'handle/\d+/\d+/submit(.*)'))">
            <xsl:apply-templates select='/dri:document/dri:options/dri:list[@id="aspect.statistics.Navigation.list.statistics"]'/>
        </xsl:if>    -->



        </div>
    </xsl:template>

    <xsl:template match="dri:div[@id='ar.edu.unlp.sedici.util.FlashMessagesTransformer.div.flash-message']">
        <xsl:param name="muestra">false</xsl:param>
        <xsl:if test="$muestra='true'">
            <div>
                <xsl:attribute name="id">ar_edu_unlp_sedici_util_FlashMessagesTransformer_div_flash-message</xsl:attribute>
                <xsl:attribute name="class">ds-notice-div <xsl:value-of select="@rend"/></xsl:attribute>
                <xsl:apply-templates select="dri:p"/>
            </div>
        </xsl:if>
    </xsl:template>


    <xsl:template name="busqueda_inicio" match="dri:div[@id='aspect.discovery.SiteViewer.div.front-page-search']">
        <xsl:param name="muestra">false</xsl:param>
        <xsl:if test="$muestra = 'true'">
            <form>
                <xsl:attribute name="action">
                    <xsl:value-of select="@action"/>
                </xsl:attribute>
                <xsl:attribute name="method">
                    <xsl:value-of select="@method"/>
                </xsl:attribute>
                <xsl:apply-templates/>
            </form>
        </xsl:if>
    </xsl:template>


    <xsl:template name="buildTrail">
        <div id="ds-trail-wrapper">
            <ul id="ds-trail">
                <!-- No muestro ni el home ni el base en caso de ser uno solo -->
                <xsl:choose>
                    <xsl:when test="count(/dri:document/dri:meta/dri:pageMeta/dri:trail) >2">
                        <xsl:apply-templates select="/dri:document/dri:meta/dri:pageMeta/dri:trail"/>
                    </xsl:when>
                    <xsl:otherwise>

                    </xsl:otherwise>
                </xsl:choose>
            </ul>
        </div>
    </xsl:template>

    <!-- The header (distinct from the HTML head element) contains the title, subtitle, login box and various
placeholders for header images -->

    <xsl:template match="dri:trail">
        <!-- Este if controla que la ultima ocurrencia del trail no se muestre -->
        <xsl:if test="count(/dri:document/dri:meta/dri:pageMeta/dri:trail) - 1 > (position()-1)">
            <!--put an arrow between the parts of the trail-->
            <xsl:if test="position()>1">
                <xsl:if test="position()>2">
                    <li class="ds-trail-arrow">
                        <xsl:text>&#8594;</xsl:text>
                    </li>
                </xsl:if>
                <li>
                    <xsl:attribute name="class">
                        <xsl:text>ds-trail-link </xsl:text>
                        <xsl:if test="position()=2">
                            <xsl:text>first-link </xsl:text>
                        </xsl:if>
                        <xsl:if test="position()=(last()-1)">
                            <xsl:text>last-link</xsl:text>
                        </xsl:if>
                    </xsl:attribute>
                    <!-- Determine whether we are dealing with a link or plain text trail link -->
                    <xsl:choose>
                        <xsl:when test="./@target">
                            <a>
                                <xsl:attribute name="href">
                                    <xsl:value-of select="./@target"/>
                                </xsl:attribute>
                                <xsl:apply-templates />
                            </a>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:apply-templates />
                        </xsl:otherwise>
                    </xsl:choose>
                </li>
            </xsl:if>
        </xsl:if>
    </xsl:template>


    <xsl:template name="cc-license">
        <xsl:param name="metadataURL"/>
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="$metadataURL"/>
            <xsl:text>?sections=dmdSec,fileSec&amp;fileGrpTypes=THUMBNAIL</xsl:text>
        </xsl:variable>

        <xsl:variable name="ccLicenseName"
                      select="document($externalMetadataURL)//dim:field[@element='rights']"
                />
        <xsl:variable name="ccLicenseUri"
                      select="document($externalMetadataURL)//dim:field[@element='rights'][@qualifier='uri']"
                />
        <xsl:variable name="handleUri">
            <xsl:for-each select="document($externalMetadataURL)//dim:field[@element='identifier' and @qualifier='uri']">
                <a>
                    <xsl:attribute name="href">
                        <xsl:copy-of select="./node()"/>
                    </xsl:attribute>
                    <xsl:copy-of select="./node()"/>
                </a>
                <xsl:if test="count(following-sibling::dim:field[@element='identifier' and @qualifier='uri']) != 0">
                    <xsl:text>, </xsl:text>
                </xsl:if>
            </xsl:for-each>
        </xsl:variable>

        <xsl:if test="$ccLicenseName and $ccLicenseUri and contains($ccLicenseUri, 'creativecommons')">
            <div about="{$handleUri}">
                <xsl:attribute name="style">
                    <xsl:text>margin:0em 2em 0em 2em; padding-bottom:0em;</xsl:text>
                </xsl:attribute>
                <a rel="license"
                   href="{$ccLicenseUri}"
                   alt="{$ccLicenseName}"
                   title="{$ccLicenseName}"
                        >
                    <img>
                        <xsl:attribute name="src">
                            <xsl:value-of select="concat($theme-path,'/images/cc-ship.gif')"/>
                        </xsl:attribute>
                        <xsl:attribute name="alt">
                            <xsl:value-of select="$ccLicenseName"/>
                        </xsl:attribute>
                        <xsl:attribute name="style">
                            <xsl:text>float:left; margin:0em 1em 0em 0em; border:none;</xsl:text>
                        </xsl:attribute>
                    </img>
                </a>
                <span>
                    <xsl:attribute name="style">
                        <xsl:text>vertical-align:middle; text-indent:0 !important;</xsl:text>
                    </xsl:attribute>
                    <i18n:text>xmlui.dri2xhtml.METS-1.0.cc-license-text</i18n:text>
                    <xsl:value-of select="$ccLicenseName"/>
                </span>
            </div>
        </xsl:if>
    </xsl:template>




</xsl:stylesheet>