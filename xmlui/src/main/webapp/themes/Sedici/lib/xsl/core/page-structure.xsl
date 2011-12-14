
<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/"
	xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
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
                        
                        <xsl:call-template name="buildMenuSuperior"/>

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
                        <xsl:call-template name="buildFooter"/>

                    </div>

                </xsl:otherwise>
            </xsl:choose>
                <!-- Javascript at the bottom for fast page loading -->
              <xsl:call-template name="addJavascript"/>

            <xsl:text disable-output-escaping="yes">&lt;/body&gt;</xsl:text>
        </html>
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
            <!-- Puesto que cargamos en el DRI un choice-support especial en el DRI, el choice-support.js viejo debe obviarse. -->
            <xsl:choose>
                <xsl:when test="text() = 'static/js/choice-support.js'">

                </xsl:when>

                <xsl:when test="not(starts-with(text(), 'static/js/scriptaculous'))">
                    <script type="text/javascript">
                        <xsl:attribute name="src">
                            <xsl:value-of
                                    select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                            <xsl:text>/</xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:attribute>&#160;</script>
                </xsl:when>
            </xsl:choose>
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
 
            <xsl:apply-templates select="dri:div[@id='aspect.discovery.SiteViewer.div.front-page-search']">
            <xsl:with-param name="muestra">true</xsl:with-param>
            </xsl:apply-templates>

            
            
            <xsl:apply-templates/>  


        </div>
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
    
        
<!-- The header (distinct from the HTML head element) contains the title, subtitle, login box and various
     placeholders for header images -->

    <xsl:template match="dri:trail">
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
	                <xsl:if test="position()=last()">
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
    </xsl:template>

 
   <xsl:template name="buildMenuSuperior">
    <ul id="ds-menu-superior">              
        <xsl:apply-templates select="/dri:document/dri:options/dri:list/dri:list[@id='aspect.browseArtifacts.Navigation.list.global']" mode="menuSuperior"/>
                    
        <li>
        	<i18n:text>sedici.menuSuperior.staticPages.institucional</i18n:text>
			<ul>
				<li><a href="/dspace/pages/queEsSedici"><i18n:text>sedici.menuSuperior.staticPages.queEsSedici</i18n:text></a></li>
				<li><a href="/dspace/pages/politica"><i18n:text>sedici.menuSuperior.staticPages.politica</i18n:text></a></li>
			</ul>
		</li>
		<li><a href="/dspace/pages/links"><i18n:text>sedici.menuSuperior.staticPages.links</i18n:text></a></li>
        <li><a href="/dspace/pages/staff"><i18n:text>sedici.menuSuperior.staticPages.staff</i18n:text></a></li>
        <li><a href="/dspace/pages/comoLlegar"><i18n:text>sedici.menuSuperior.staticPages.comoLlegar</i18n:text></a></li>
		<li>
			<i18n:text>sedici.menuSuperior.staticPages.informacion</i18n:text>
			<ul>
			  <li><a href="/dspace/pages/comoAgregarTrabajos"><i18n:text>sedici.menuSuperior.staticPages.agregacion</i18n:text></a></li>
			  <li><a href="/dspace/pages/FAQ"> <i18n:text>sedici.menuSuperior.staticPages.faq</i18n:text></a></li>
			</ul>
		
		</li>
		<li>
			<a href="/dspace/pages/contacto"><i18n:text>sedici.menuSuperior.staticPages.contacto</i18n:text></a>
		</li>
            
            
     </ul>
    </xsl:template>
    
    <!--give nested navigation list the class sublist-->
    <xsl:template match="dri:options/dri:list/dri:list" priority="3" mode="menuSuperior">
        <li>
            <i18n:text><xsl:value-of select="dri:head"/></i18n:text>
            <ul>
                <xsl:apply-templates select="dri:item" mode="nested"/>
            </ul>
        </li>
    </xsl:template>
    
</xsl:stylesheet>