<!--
	/* Created for LINDAT/CLARIN */
    Main structure of the UFAL-POINT repository pages
    header
    main-contents
        trail-bar
        browse-view
        item-view
        administration
        ...
    footer
    
    Author: Amir Kamran
-->

<xsl:stylesheet version="1.0"
    xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
    xmlns:dri="http://di.tamu.edu/DRI/1.0/"
    xmlns:mets="http://www.loc.gov/METS/"
    xmlns:xlink="http://www.w3.org/TR/xlink/"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    xmlns:mods="http://www.loc.gov/mods/v3"
    xmlns:confman="org.dspace.core.ConfigurationManager"
    xmlns="http://www.w3.org/1999/xhtml"
    exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods confman">

    <xsl:output indent="yes" />

    <xsl:variable name="aaiURL">
        <xsl:value-of select="confman:getProperty('lr', 'lr.aai.url')"/>
    </xsl:variable>
                    
    <xsl:template match="dri:document">
    	<xsl:text disable-output-escaping='yes'>&lt;!DOCTYPE html></xsl:text>
        <html>
            <!-- First of all, build the HTML head element -->
            <xsl:call-template name="buildHead"/>
            
            <!-- Then proceed to the body -->

            <!--paulirish.com/2008/conditional-stylesheets-vs-css-hacks-answer-neither/-->
            <xsl:text disable-output-escaping="yes">&lt;!--[if lt IE 7 ]&gt; &lt;body class="ie6"&gt; &lt;![endif]--&gt;
                &lt;!--[if IE 7 ]&gt;    &lt;body id="lindat-repository" class="ie7"&gt; &lt;![endif]--&gt;
                &lt;!--[if IE 8 ]&gt;    &lt;body id="lindat-repository" class="ie8"&gt; &lt;![endif]--&gt;
                &lt;!--[if IE 9 ]&gt;    &lt;body id="lindat-repository" class="ie9"&gt; &lt;![endif]--&gt;
                &lt;!--[if (gt IE 9)|!(IE)]&gt;&lt;!--&gt;&lt;body id="lindat-repository"&gt;&lt;!--&lt;![endif]--&gt;</xsl:text>

            <!-- Common Header -->
            <xsl:call-template name="buildHeader" />
            
            <xsl:apply-templates select="dri:body" />

            <!-- Common Footer -->
            <xsl:call-template name="buildFooter" />            

            <!-- Javascript at the bottom for fast page loading -->
            <xsl:call-template name="addJavascript" />                            

            <xsl:text disable-output-escaping="yes">&lt;/body&gt;</xsl:text>
        </html>
    </xsl:template>

    <!-- The HTML head element contains references to CSS as well as embedded JavaScript code. Most of this
    information is either user-provided bits of post-processing (as in the case of the JavaScript), or
    references to stylesheets pulled directly from the pageMeta element. -->
    <xsl:template name="buildHead">
        <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />

            <!-- Always force latest IE rendering engine (even in intranet) & Chrome Frame -->
            <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />

            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
            
            <link rel="shortcut icon">
                <xsl:attribute name="href">
                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]" />
                    <xsl:text>/themes/</xsl:text>
                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']" />
                    <xsl:text>/images/favicon.ico</xsl:text>
                </xsl:attribute>
            </link>
            
            <!-- link rel="apple-touch-icon">
                <xsl:attribute name="href">
                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]" />
                    <xsl:text>/themes/</xsl:text>
                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']" />
                    <xsl:text>/images/apple-touch-icon.png</xsl:text>
                </xsl:attribute>
            </link-->

            <meta name="Generator">
                <xsl:attribute name="content">
                <xsl:text>DSpace</xsl:text>
                <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='dspace'][@qualifier='version']">
                  <xsl:text> </xsl:text>
                  <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='dspace'][@qualifier='version']" />
                </xsl:if>
              </xsl:attribute>
            </meta>
            
            <!-- Add stylsheets -->
                        
            <xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='stylesheet']">
                <link rel="stylesheet" type="text/css">
                    <xsl:attribute name="media">
                        <xsl:value-of select="@qualifier" />
                    </xsl:attribute>
                    <xsl:attribute name="href">
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]" />
                        <xsl:text>/themes/</xsl:text>
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']" />
                        <xsl:text>/</xsl:text>
                        <xsl:value-of select="." />
                    </xsl:attribute>
                </link>
            </xsl:for-each>

            <!-- Add Lindat stylesheet -->
            <link rel="stylesheet" href="{$theme-path}/lib/lindat/public/css/lindat.css" media="screen" />
            
            <!-- Bootstrap stylesheets -->
            <link rel="stylesheet" href="{$theme-path}/lib/bootstrap/css/bootstrap.min.css" media="screen" />
            <link rel="stylesheet" href="{$theme-path}/lib/bootstrap/css/font-awesome.min.css" media="screen" />
            <link rel="stylesheet" href="{$theme-path}/lib/bootstrap/css/ufal-theme.css" media="screen" />
            <link rel="stylesheet" href="{$theme-path}/lib/bootstrap/css/repository.css" media="screen" />

	    <link rel="stylesheet" href="{$theme-path}/lib/css/print.css" media="print" />

            <!-- datepicker -->
            <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='include-library'][@qualifier='datepicker']">
                <link rel="stylesheet" href="{$theme-path}/lib/bootstrap/css/datepicker.css" />
            </xsl:if>

	    
	    <!-- license selector -->
        <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='include-library'][@qualifier='licenseselect']">
            <link rel="stylesheet" href="{$theme-path}/lib/lindat-license-selector/license-selector.min.css"> </link>
        </xsl:if>
	    

	    <!-- jquery-ui -->
            <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='include-library'][@qualifier='jquery-ui']">
                <link rel="stylesheet" href="{$theme-path}/lib/css/ui-lightness/jquery-ui.css"> </link>
            </xsl:if>

	    <!-- authority css -->
            <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='include-library'][@qualifier='authority-control']">
                <link rel="stylesheet" href="{$theme-path}/lib/css/authority-control.css"> </link>
            </xsl:if>
            
            
			<xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='include-library'][@qualifier='jqplot']">
				<link rel="stylesheet" href="{$theme-path}/lib/js/jqplot/jquery.jqplot.css"> </link>
				<link rel="stylesheet" href="{$theme-path}/lib/css/jqplot.css"> </link>
	        	</xsl:if>            
            
            <!-- select2 -->
            <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='include-library'][@qualifier='select2']">
                <link rel="stylesheet" href="{$theme-path}/lib/select2/select2.css" />
                <link rel="stylesheet" href="{$theme-path}/lib/select2/select2-bootstrap.css" />
            </xsl:if>
            
            <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='include-library'][@qualifier='bootstrap-toggle']">
                <link rel="stylesheet" href="{$theme-path}/lib/bootstrap/css/bootstrap2-toggle.min.css" />
            </xsl:if>            

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
                
                // Clear default text of emty text areas on focus
                function tFocus(element) {
                    if (element.value == '<i18n:text>xmlui.dri2xhtml.default.textarea.value</i18n:text>'){
                        element.value='';
                    }
                }
                
                // Clear default text of emty text areas on submit
                function tSubmit(form) {
                    var defaultedElements = document.getElementsByTagName("textarea");
                    for (var i=0; i != defaultedElements.length; i++){
                        if (defaultedElements[i].value == '<i18n:text>xmlui.dri2xhtml.default.textarea.value</i18n:text>'){
                            defaultedElements[i].value='';
                        }
                    }
                }
                
                // Disable pressing 'enter' key to submit a form (otherwise pressing 'enter' causes a submission to start over)
                function disableEnterKey(e) {
                    var key;
                    if(window.event)
                        key = window.event.keyCode; //Internet Explorer
                    else
                        key = e.which; //Firefox and Netscape
        
                    if(key == 13) //if "Enter" pressed, then disable!
                        return false;
                    else
                        return true;
                }
        
                function FnArray() {
                    this.funcs = new Array;
                }
        
                FnArray.prototype.add = function(f) {
                    if( typeof f!= "function" ) {
                        f = new Function(f);
                    }
                    this.funcs[this.funcs.length] = f;
                };
        
                FnArray.prototype.execute = function() {
                    for( var i=0; i<xsl:text disable-output-escaping="yes">&lt;</xsl:text>this.funcs.length; i++ ){
                        this.funcs[i]();
                    }
                };
        
                var runAfterJSImports = new FnArray();
            </script>
            
            <!-- Add the title in -->
            <xsl:variable name="page_title" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='title']" />
            <title>
                <xsl:choose>
                        <xsl:when test="normalize-space($static-page-name) != ''">
                                <xsl:value-of select="document(concat('../../html/', $static-page-name, '.xml'))/page/title"/>
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
                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='xhtml_head_item']" disable-output-escaping="yes" />
            </xsl:if>

            <!-- Add all Google Scholar Metadata values -->
            <xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[substring(@element, 1, 9) = 'citation_']">
                <meta name="{@element}" content="{.}"></meta>
            </xsl:for-each>

            <link href="{concat($aaiURL, '/discojuice/discojuice.css')}" type="text/css" rel="stylesheet" />

        </head>
    </xsl:template>

    <!-- The header (distinct from the HTML head element) contains the title, subtitle, login box and various
        placeholders for header images -->
    <xsl:template name="buildTrail">
        <div class="row">
            <ul class="breadcrumb no-radius no-margin" style="padding: 10px;">
                <xsl:choose>
                    <xsl:when test="starts-with($request-uri, 'page')">
                    	<li>
                        <a>
                            <xsl:attribute name="href">
                                        <xsl:value-of
                                select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]" />
                                        <xsl:text>/</xsl:text>
                                      </xsl:attribute>
                            <i18n:text>xmlui.general.dspace_home</i18n:text>
                        </a>
                        </li>
                        <xsl:choose>
                            <xsl:when test="normalize-space($static-page-name) != ''">
                                <li class="active">
                                    <xsl:value-of select="document(concat('../../html/', $static-page-name, '.xml'))/page/title-menu" />
                                </li>
                            </xsl:when>                                                                         
                        </xsl:choose>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:apply-templates select="/dri:document/dri:meta/dri:pageMeta/dri:trail" />
                    </xsl:otherwise>
                </xsl:choose>
        	<xsl:if test="not(//dri:div[@n='general-query'])">
        		<div class="col-xs-12 visible-xs">&#160;</div>
	        	<form class="pull-right col-sm-3" style="top: -6px;" method="post">
		            <xsl:attribute name="action">
		                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath']"/>
		                <xsl:value-of
		                        select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='simpleURL']"/>
		            </xsl:attribute>
		            <div class="input-group input-group-sm">
	                <input type="text" class="form-control small-search-input" placeholder="xmlui.general.search" i18n:attr="placeholder">
	                     <xsl:attribute name="name">
	                         <xsl:value-of
	                                 select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='queryField']"/>
	                     </xsl:attribute>                
	                </input>
	                <span class="input-group-btn">
	                	<button class="btn small-search-btn btn-default" name="submit" type="submit"><i class="fa fa-search" style="color: #7479B8;">&#160;</i></button>
	                </span>
	                </div>
	            </form> 
            </xsl:if>                                                                     
            </ul>
        </div>
       	<div class="visible-xs text-center" style="margin-top: 5px;">
			<button id="showhidemenu" type="button" class="btn btn-default btn-sm" style="border-radius: 30px; width: 100%;">
					<i class="fa fa-align-justify">&#160;</i> Show/Hide Menu
			</button>        	        		
       	</div>        
    </xsl:template>
    
    <xsl:template match="dri:trail">
        <li>
            <xsl:attribute name="class">
                <xsl:if test="position()=last()">
                    <xsl:text>active</xsl:text>
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
    </xsl:template>

    <!--
        The template to handle the dri:body element. It simply creates the ds-body div and applies
        templates of the body's child elements (which consists entirely of dri:div tags).
    -->
    <xsl:template match="dri:body">

        <xsl:call-template name="navbar" />
        
        <div class="container-fluid">
            
            <div class="container">
            
                <xsl:call-template name="buildTrail" />

	            <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='alert'][@qualifier='message']">
	                <div id="ds-system-wide-alert" class="alert alert-danger">
	                    <h4>
	                        <xsl:copy-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='alert'][@qualifier='message']/node()"/>
	                    </h4>
	                </div>
	            </xsl:if>
                
                <div class="row contents">

                    <!-- sidebar -->
                    <xsl:apply-templates select="/dri:document/dri:options" />
                
                    <div id="main-contents" class="col-sm-9">
						<!-- Check for the custom pages -->
                        <xsl:choose>
            
                            <xsl:when test="normalize-space($static-page-name) != ''">
                                <div>
                                    <xsl:copy-of select="document(concat('../../html/', $static-page-name, '.html'))" />
                                </div>
                            </xsl:when>
            
							<!-- Otherwise use default handling of body -->
                            <xsl:otherwise>
                                <xsl:apply-templates />
                            </xsl:otherwise>
                        </xsl:choose>
                   </div>
                </div>
                                   
            </div>
        </div>
    </xsl:template>

    <!-- Currently the dri:meta element is not parsed directly. Instead, parts of it are referenced from inside
        other elements (like reference). The blank template below ends the execution of the meta branch -->
    <xsl:template match="dri:meta" />

    <xsl:template name="addJavascript">
        <xsl:variable name="jqueryVersion">
            <xsl:text>1.7</xsl:text>
        </xsl:variable>

        <xsl:variable name="protocol">
            <xsl:choose>
                <xsl:when test="starts-with(confman:getProperty('dspace.baseUrl'), 'https://')">
                    <xsl:text>https://</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                <!-- UFAL #303 - dspace thinks after redirect that it is still in http -->
                    <xsl:text>https://</xsl:text>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <script type="text/javascript" src="{concat($protocol, 'ajax.googleapis.com/ajax/libs/jquery/', $jqueryVersion ,'/jquery.min.js')}">&#160;</script>
        <script type="text/javascript" src="{$theme-path}/lib/js/jquery-ui.js">&#160;</script>
        <script type="text/javascript" src="{$theme-path}/lib/js/jquery.i18n.js">&#160;</script>

        <script type="text/javascript" src="{concat($aaiURL, '/discojuice/discojuice-2.1.en.min.js')}">&#160;</script>
        <script type="text/javascript" src="{concat($aaiURL, '/aai.js')}">&#160;</script>

        <xsl:variable name="localJQuerySrc">
                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
            <xsl:text>/static/js/jquery-</xsl:text>
            <xsl:value-of select="$jqueryVersion"/>
            <xsl:text>.min.js</xsl:text>
        </xsl:variable>

        <script type="text/javascript">
            <xsl:text disable-output-escaping="yes">!window.jQuery &amp;&amp; document.write('&lt;script type="text/javascript" src="</xsl:text>
            <xsl:value-of select="$localJQuerySrc" />
            <xsl:text disable-output-escaping="yes">"&gt;&#160;&lt;\/script&gt;')</xsl:text>
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
            <xsl:choose>
                <xsl:when test="text() = 'static/js/choice-support.js'">
                    <script type="text/javascript">
                        <xsl:attribute name="src">
                            <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                            <xsl:text>/themes/</xsl:text>
                            <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                            <xsl:text>/lib/js/choice-support.js</xsl:text>
                        </xsl:attribute>&#160;</script>
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
            <xsl:attribute name="src">
                 <xsl:value-of select="$theme-path" />
                 <xsl:text>/lib/bootstrap/js/bootstrap.min.js</xsl:text>
            </xsl:attribute>&#160;</script>

        <script type="text/javascript">
            <xsl:attribute name="src">
                 <xsl:value-of select="$theme-path" />
                 <xsl:text>/lib/bootstrap/js/bootstrap3-typeahead.js</xsl:text>
            </xsl:attribute>&#160;</script>
        
        <script type="text/javascript">
            <xsl:attribute name="src">
                 <xsl:value-of select="$theme-path" />
                 <xsl:text>/lib/bootstrap/js/ufal.min.js</xsl:text>
            </xsl:attribute>&#160;</script>

        <script type="text/javascript">
            <xsl:attribute name="src">
                 <xsl:value-of select="$theme-path" />
                 <xsl:text>/lib/lindat/public/js/lindat-refbox.js</xsl:text>
            </xsl:attribute>&#160;</script>


        <!-- UFAL additional libraries
        -->
        
        <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='include-library'][@qualifier='jqplot']">
        	<script type="text/javascript" src="{$theme-path}/lib/js/jqplot/jquery.jqplot.min.js">&#160;</script>
            <script type="text/javascript" src="{$theme-path}/lib/js/jqplot/plugins/jqplot.canvasTextRenderer.min.js">&#160;</script>
            <script type="text/javascript" src="{$theme-path}/lib/js/jqplot/plugins/jqplot.canvasAxisLabelRenderer.min.js">&#160;</script>
            <script type="text/javascript" src="{$theme-path}/lib/js/jqplot/plugins/jqplot.highlighter.min.js">&#160;</script>
            <script type="text/javascript" src="{$theme-path}/lib/js/jqplot/plugins/jqplot.cursor.min.js">&#160;</script>
            <script type="text/javascript" src="{$theme-path}/lib/js/jqplot/plugins/jqplot.dateAxisRenderer.min.js">&#160;</script>
            <script type="text/javascript" src="{$theme-path}/lib/js/jqplot/plugins/jqplot.enhancedLegendRenderer.js">&#160;</script>
            <script type="text/javascript" src="{$theme-path}/lib/js/piwik_charts.js">&#160;</script>
        </xsl:if>        
        
        <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='include-library'][@qualifier='datepicker']">
            <script type="text/javascript" src="{$theme-path}/lib/bootstrap/js/bootstrap-datepicker.js">&#160;</script>
        </xsl:if>

        <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='include-library'][@qualifier='dragNdrop']">
            <script type="text/javascript" src="{$theme-path}/lib/js/dragndrop.js">&#160;</script>
            <script type="text/javascript" src="{$theme-path}/lib/js/fileupload.js">&#160;</script>
        </xsl:if>

        <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='include-library'][@qualifier='extrametadata']">
            <script type="text/javascript" src="{$theme-path}/lib/js/extrametadata.js">&#160;</script>
        </xsl:if>

        <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='include-library'][@qualifier='statistic-map']">
            <script type='text/javascript' src='https://www.google.com/jsapi'>&#160;</script>
            <script type="text/javascript" src="{$theme-path}/lib/js/statistics.js">&#160;</script>
        </xsl:if>

        <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='include-library'][@qualifier='select2']">
            <script type="text/javascript" src="{$theme-path}/lib/select2/select2.js">&#160;</script>
        </xsl:if>

        <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='URI']='admin/panel'">
            <script type="text/javascript" src="{$theme-path}/lib/js/ufal-controlpanel.js">&#160;</script>
        </xsl:if>

        <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='include-library'][@qualifier='handles']">
            <script type="text/javascript" src="{$theme-path}/lib/js/ufal-handles.js">&#160;</script>
        </xsl:if>

        <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='include-library'][@qualifier='select-collection']">
            <script type="text/javascript" src="{$theme-path}/lib/js/ufal-select-collection.js">&#160;</script>
        </xsl:if>

        <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='include-library'][@qualifier='licenseselect']">
            <script type="text/javascript" src="{$theme-path}/lib/lindat-license-selector/lodash.min.js">&#160;</script>
            <script type="text/javascript" src="{$theme-path}/lib/lindat-license-selector/license-selector.min.js">&#160;</script>
        </xsl:if>
        
        <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='include-library'][@qualifier='bootstrap-toggle']">
            <script type="text/javascript" src="{$theme-path}/lib/bootstrap/js/bootstrap2-toggle.min.js">&#160;</script>
        </xsl:if>            
        
        <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='include-library'][@qualifier='submission']">
            <script type="text/javascript" src="{$theme-path}/lib/js/ufal-submission.js">&#160;</script>
        </xsl:if>

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

</xsl:stylesheet>




