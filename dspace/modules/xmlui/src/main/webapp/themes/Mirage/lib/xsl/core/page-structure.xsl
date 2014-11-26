<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<!--
    Main structure of the page, determines where
    header, footer, body, navigation are structurally rendered.
    Rendering of the header, footer, trail and alerts

    Author: art.lowel at atmire.com
    Author: lieven.droogmans at atmire.com
    Author: ben at atmire.com
    Author: Alexey Maslov

-->

<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
                xmlns:dri="http://di.tamu.edu/DRI/1.0/"
                xmlns:mets="http://www.loc.gov/METS/"
                xmlns:xlink="http://www.w3.org/TR/xlink/"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
                xmlns:xhtml="http://www.w3.org/1999/xhtml"
                xmlns:mods="http://www.loc.gov/mods/v3"
                xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns="http://www.w3.org/1999/xhtml"
                exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc">

    <xsl:output indent="yes"/>

    <!-- Initialize info on build date for use in the footer -->
    <xsl:variable name="dryadrelease" select="document('../../../meta/version.xml')"/>

    <!--
        The starting point of any XSL processing is matching the root element. In DRI the root element is document,
        which contains a version attribute and three top level elements: body, options, meta (in that order).

        This template creates the html document, giving it a head and body. A title and the CSS style reference
        are placed in the html head, while the body is further split into several divs. The top-level div
        directly under html body is called "ds-main". It is further subdivided into:
            "ds-header"  - the header div containing title, subtitle, trail and other front matter
            "ds-body"    - the div containing all the content of the page; built from the contents of dri:body
            "ds-options" - the div with all the navigation and actions; built from the contents of dri:options
            "ds-footer"  - optional footer div, containing misc information

        The order in which the top level divisions appear may have some impact on the design of CSS and the
        final appearance of the DSpace page. While the layout of the DRI schema does favor the above div
        arrangement, nothing is preventing the designer from changing them around or adding new ones by
        overriding the dri:document template.
    -->
    <xsl:template match="dri:document">
         <html class="no-js" lang="en">
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
                <xsl:when
                        test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='framing'][@qualifier='popup']">
                    <xsl:apply-templates select="dri:body/*"/>
                </xsl:when>
                <xsl:otherwise>
                    <div id="ds-main">
                        <!--The header div, complete with title, subtitle and other junk-->
                        <xsl:call-template name="buildHeader"/>

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
                    <xsl:value-of
                            select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                    <xsl:text>/themes/</xsl:text>
                    <xsl:value-of
                            select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                    <xsl:text>/images/favicon.ico</xsl:text>
                </xsl:attribute>
            </link>
            <link rel="apple-touch-icon">
                <xsl:attribute name="href">
                    <xsl:value-of
                            select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                    <xsl:text>/themes/</xsl:text>
                    <xsl:value-of
                            select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                    <xsl:text>/images/apple-touch-icon.png</xsl:text>
                </xsl:attribute>
            </link>

            <meta name="Generator">
                <xsl:attribute name="content">
                    <xsl:text>DSpace</xsl:text>
                    <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='dspace'][@qualifier='version']">
                        <xsl:text> </xsl:text>
                        <xsl:value-of
                                select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='dspace'][@qualifier='version']"/>
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
                        <xsl:value-of
                                select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                        <xsl:text>/themes/</xsl:text>
                        <xsl:value-of
                                select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                        <xsl:text>/</xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:attribute>
                </link>
            </xsl:for-each>

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
                        <xsl:value-of
                                select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='scheme']"/>
                        <xsl:text>://</xsl:text>
                        <xsl:value-of
                                select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='serverName']"/>
                        <xsl:text>:</xsl:text>
                        <xsl:value-of
                                select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='serverPort']"/>
                        <xsl:value-of select="$context-path"/>
                        <xsl:text>/</xsl:text>
                        <xsl:value-of
                                select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='opensearch'][@qualifier='context']"/>
                        <xsl:text>description.xml</xsl:text>
                    </xsl:attribute>
                    <xsl:attribute name="title">
                        <xsl:value-of
                                select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='opensearch'][@qualifier='shortName']"/>
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

                //add "" to search text so the seach with doi and hdl will return item
                var defaultedElements = document.getElementsByTagName("textarea");
                   for (var i = 0; i != defaultedElements.length; i++) {
                       if (defaultedElements[i].value == ' ') {
                           defaultedElements[i].value = '';
                       }
                   }
                   var queryTexts = document.getElementsByName("query");
                   for (var i = 0; i != queryTexts.length; i++) {
                       var value = queryTexts[i].value;
                       if (value.indexOf(' ') == - 1) {
                           if (value.indexOf("doi:") == 0 || value.indexOf("DOI:") == 0 || value.indexOf("http:") == 0 || value.indexOf("pmid:") == 0 || value.indexOf("PMID:") == 0) {
                               queryTexts[i].value = '"' + value + '"';
                           }
                           
                           if (value.indexOf("hdl:") == 0) {
                               queryTexts[i].value = value.substring(4, value.length);
                           }
                       }
                   }


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
                key = window.event.keyCode; //Internet Explorer
                else
                key = e.which; //Firefox and Netscape

                if(key == 13) //if "Enter" pressed, then disable!
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
                for( var i=0; i
                <xsl:text disable-output-escaping="yes">&lt;</xsl:text> this.funcs.length; i++ )
                {
                this.funcs[i]();
                }
                };

                var runAfterJSImports = new FnArray();
            </script>

            <!-- Modernizr enables HTML5 elements & feature detects -->
            <script type="text/javascript">
                <xsl:attribute name="src">
                    <xsl:value-of
                            select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                    <xsl:text>/themes/</xsl:text>
                    <xsl:value-of
                            select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                    <xsl:text>/lib/js/modernizr-1.5.min.js</xsl:text>
                </xsl:attribute>
                &#160;
            </script>

            <!-- Add the title in -->
            <xsl:variable name="page_title"
                          select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='title']"/>
            <title>
                <xsl:choose>
                    <xsl:when test="not($page_title)">
                        <xsl:text>Dryad</xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:copy-of select="$page_title/node()"/>
                        <xsl:text> - Dryad</xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
            </title>

            <!-- Head metadata in item pages -->
            <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='xhtml_head_item']">
                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='xhtml_head_item']"
                              disable-output-escaping="yes"/>
            </xsl:if>

            <!-- Add all Google Scholar Metadata values -->
            <xsl:for-each
                    select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[substring(@element, 1, 9) = 'citation_']">
                <meta name="{@element}" content="{.}"></meta>
            </xsl:for-each>

        </head>
    </xsl:template>


    <!-- The header (distinct from the HTML head element) contains the title, subtitle, login box and various
        placeholders for header images -->
    <xsl:template name="buildHeader">
        <div id="ds-header-wrapper">
            <div id="ds-header" class="clearfix">
                <a id="skip-nav" href="#ds-body" class="accessibly-hidden">Skip navigation</a>
                <a id="ds-header-logo-link">
                    <xsl:attribute name="href">
                        <xsl:value-of
                                select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                        <xsl:text>/</xsl:text>
                    </xsl:attribute>
                    <xsl:choose>
                        <xsl:when test="$meta[@element='request'][@qualifier='realServerPort'][. = '9999']">
                            <span class="ds-header-logo" id="ds-header-logo-dev">&#160;</span>
                        </xsl:when>
                        <xsl:when test="$meta[@element='request'][@qualifier='realServerPort'][. = '7777']">
                            <span class="ds-header-logo" id="ds-header-logo-demo">&#160;</span>
                        </xsl:when>
                        <xsl:when test="$meta[@element='request'][@qualifier='realServerPort'][. = '8888']">
                            <span class="ds-header-logo" id="ds-header-logo-staging">&#160;</span>
                        </xsl:when>
                        <xsl:when test="$meta[@element='request'][@qualifier='realServerPort'][. = '6666']">
                            <span class="ds-header-logo" id="ds-header-logo-mrc">&#160;</span>
                        </xsl:when>
                        <xsl:otherwise>
                            <span class="ds-header-logo" id="ds-header-logo-prod">&#160;</span>
                        </xsl:otherwise>
                    </xsl:choose>
                    <span id="ds-header-logo-text">mirage</span>
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

                <div id="sharing-tools">
                    <a href="http://twitter.com/datadryad">
                        <img src="/themes/Mirage/images/dryad_twit_icon.png" alt="Follow us on Twitter"/>
                    </a>
                    <a href="http://www.facebook.com/DataDryad">
                        <img src="/themes/Mirage/images/dryad_fb_icon2.png" alt="Find us on Facebook"/>
                    </a>
		    <!-- We don't currently have a Google Plus page...
                    <a href="">
                        <img src="/themes/Mirage/images/dryad_gplus_icon.png"/>
                    </a>
		    -->
                    <a href="http://blog.datadryad.org/feed/">
                        <img src="/themes/Mirage/images/dryad_rss_icon.png" alt="RSS feed - Dryad News"/>
                    </a>
                </div>

                <div id="main-menu">
                    <xsl:if test="/dri:document/dri:meta/dri:userMeta/@authenticated = 'yes'">
                        <xsl:attribute name="class">
                            <xsl:text>authenticated-menu</xsl:text>
                        </xsl:attribute>
                    </xsl:if>
                    <ul class="sf-menu">
                        <li>
                            <a href="">About</a>
                            <ul>
                                <li>
                                    <a href="/pages/repository">Repository features and technology</a>
                                </li>
                                <li>
                                    <a href="/pages/organization">The organization</a>
                                </li>
                                <li>
                                    <a href="http://blog.datadryad.org" target="_blank">News and views</a>
                                </li>
                                <li>
                                    <a href="/pages/whoWeAre">Who we are</a>
                                </li>
                                <li>
                                    <a href="/pages/employment">Employment Opportunities</a>
                                </li>
                                <li>
                                  <a href="/pages/faq">Frequently asked questions</a>
                                </li>
                            </ul>
                        </li>
                        <li>
                            <a href="">For researchers</a>
                            <ul>
                                <li>
                                  <a href="/pages/faq#depositing">Submit data</a>
                                </li>
                                <li>
                                  <a href="/pages/faq#using">Use data</a>
                                </li>
                                <li>
                                  <a href="/pages/integratedJournals">Look up your journal</a>
                                </li>
                                <li>
                                  <a href="/pages/institutionalSponsors">Institutional sponsors</a>
                                </li>
                                <li>
                                    <a href="/pages/policies">Terms of service</a>
                                </li>                            
                            </ul>
                        </li>
                        <li>
                            <a href="">For organizations</a>
                            <ul>
                                <li>
                                    <a href="/pages/journalIntegration">Journal integration</a>
                                </li>                            
                                <li>
                                    <a href="/pages/membershipOverview">Membership</a>
                                </li>                            
                                <li>
                                    <a href="/pages/pricing">Pricing plans</a>
                                </li>                            
                            </ul>
                        </li>
                        <li>
                            <a href="/feedback">Contact us</a>
                        </li>





                        <xsl:choose>
                            <xsl:when test="/dri:document/dri:meta/dri:userMeta/@authenticated = 'yes'">
                                <li>
                                    <a>
                                        <xsl:attribute name="href">
                                            <xsl:value-of select="/dri:document/dri:meta/dri:userMeta/
                                        dri:metadata[@element='identifier' and @qualifier='url']"/>
                                        </xsl:attribute>
                                        <i18n:text>xmlui.dri2xhtml.structural.profile</i18n:text>
                                        <xsl:value-of select="/dri:document/dri:meta/dri:userMeta/
                                    dri:metadata[@element='identifier' and @qualifier='firstName']"/>
                                        <xsl:text> </xsl:text>
                                        <xsl:value-of select="/dri:document/dri:meta/dri:userMeta/
                                    dri:metadata[@element='identifier' and @qualifier='lastName']"/>
                                    </a>

                                    <ul>
                                        <!--remove the extra login link-->
                                        <xsl:apply-templates select="/dri:document/dri:options/dri:list[@n='account']/dri:item" mode="menu"/>

                                        <xsl:if test="/dri:document/dri:options/dri:list[@n='context']/*">
                                            <xsl:for-each select="/dri:document/dri:options/dri:list[@n='context']/dri:item">
                                                <xsl:choose>
                                                    <xsl:when test="position()=1">
                                                        <li class="menu-border">
                                                            <xsl:apply-templates select="." mode="menu"/>
                                                        </li>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <li>
                                                            <xsl:apply-templates select="." mode="menu"/>
                                                        </li>
                                                    </xsl:otherwise>
                                                </xsl:choose>

                                            </xsl:for-each>
                                        </xsl:if>

                                        <xsl:if test="/dri:document/dri:options/dri:list[@n='administrative']/*">
                                            <xsl:for-each select="/dri:document/dri:options/dri:list[@n='administrative']/dri:item">
                                                <xsl:choose>
                                                    <xsl:when test="position()=1">
                                                        <li class="menu-border">
                                                            <xsl:apply-templates select ="*"/>
                                                        </li>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <li>
                                                            <xsl:apply-templates select ="*"/>
                                                        </li>
                                                    </xsl:otherwise>
                                                </xsl:choose>

                                            </xsl:for-each>

                                            <xsl:for-each select="/dri:document/dri:options/dri:list[@n='administrative']/dri:list">
                                                <li>
                                                    <a href="#">
                                                        <i18n:text><xsl:value-of select="dri:head"/></i18n:text>
                                                    </a>
                                                    <ul>
                                                        <xsl:for-each select="dri:item">
                                                            <li>
                                                                <xsl:apply-templates select="*"/>
                                                            </li>
                                                        </xsl:for-each>
                                                    </ul>
                                                </li>
                                            </xsl:for-each>
                                        </xsl:if>
                                    </ul>

                                </li>
                            </xsl:when>
                            <xsl:otherwise>
                                <li class="no-hover-highlight">
                                    <a href="/login">
                                        <span id="login-item">Login</span>
                                        <span class="accessibly-hidden"> or </span>
                                        <span id="sign-up-item">Sign Up</span>
                                    </a>
                                </li>
                            </xsl:otherwise>
                        </xsl:choose>
                    </ul>
                </div>
            </div>
        </div>
    </xsl:template>


    <!-- The header (distinct from the HTML head element) contains the title, subtitle, login box and various
        placeholders for header images -->
    <xsl:template name="buildTrail">
        <div id="ds-trail-wrapper">
            <ul id="ds-trail">
                <xsl:choose>
                    <xsl:when test="count(/dri:document/dri:meta/dri:pageMeta/dri:trail) = 0">
                        <li class="ds-trail-link first-link">-</li>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:apply-templates select="/dri:document/dri:meta/dri:pageMeta/dri:trail"/>
                    </xsl:otherwise>
                </xsl:choose>
            </ul>
        </div>
    </xsl:template>

    <xsl:template match="dri:trail">
        <!--put an arrow between the parts of the trail-->
        <xsl:if test="position()>1">
            <li class="ds-trail-arrow">
                <xsl:text>&#8594;</xsl:text>
            </li>
        </xsl:if>
        <li>
            <xsl:attribute name="class">
                <xsl:text>ds-trail-link </xsl:text>
                <xsl:if test="position()=1">
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
                        <xsl:apply-templates/>
                    </a>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates/>
                </xsl:otherwise>
            </xsl:choose>
        </li>
    </xsl:template>


    <!-- Like the header, the footer contains various miscellanious text, links, and image placeholders -->
    <xsl:template name="buildFooter">
        <div id="ds-footer-wrapper">
            <div id="ds-footer">

                <div id="ds-footer-right">
                    <a>
                        <xsl:attribute name="href">
                            <xsl:value-of
                                    select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                            <xsl:text>/pages/policies</xsl:text>
                        </xsl:attribute>
                        <i18n:text>xmlui.dri2xhtml.structural.footer-terms-of-service</i18n:text>
                    </a>
		     | 
                    <a>
                        <xsl:attribute name="href">
                            <xsl:value-of
                                    select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                            <xsl:text>/feedback</xsl:text>
                        </xsl:attribute>
                        <i18n:text>xmlui.dri2xhtml.structural.contact-link</i18n:text>
                    </a>
                </div>

                <p style="margin: 0;">
                    <!-- 'Dryad is...' -->
                    <i18n:text>xmlui.dri2xhtml.structural.footer-promotional1</i18n:text>
                </p>

                <p style="clear: both; float: right; margin-top: 11px; color: #999;">
                    <!-- latest Dryad build info (and node/site name, if available) -->
                    <i18n:text>xmlui.dri2xhtml.structural.footer-promotional2</i18n:text>
                    <xsl:value-of select="$dryadrelease/release/date"/> 
                    <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='dryad'][@qualifier='node']">
                        <i18n:text>xmlui.dri2xhtml.structural.footer-node</i18n:text>
                        <xsl:text> </xsl:text>
                        <xsl:value-of
                            select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='dryad'][@qualifier='node']"/>
                    </xsl:if>
                </p>
                <!--Git Commit hash rendered in HTML comment-->
                <xsl:comment>Git Commit Hash: <xsl:value-of select="$dryadrelease/release/version"/></xsl:comment>

                <!-- Powered by... -->
                <div id="ds-footer-left" style="color: #999;">
                    <i18n:text>xmlui.dri2xhtml.structural.footer-powered-by</i18n:text>
                    <xsl:text> </xsl:text>
                    <a class="single-image-link" href="http://www.dspace.org/" target="_blank">
                      <img class="powered-by" src="/themes/Mirage/images/powered-by-dspace.png" alt="DSpace" />
                      <span class="accessibly-hidden"> (opens in a new window)</span>
                    </a>
                </div>

                <!--Invisible link to HTML sitemap (for search engines) -->
                <a class="hidden">
                    <xsl:attribute name="href">
                        <xsl:value-of
                                select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                        <xsl:text>/htmlmap</xsl:text>
                    </xsl:attribute>
                    <xsl:text>sitemap</xsl:text>
                </a>
            </div>
        </div>
    </xsl:template>

    <!--
            The meta, body, options elements; the three top-level elements in the schema
    -->


    <!--
        The template to handle the dri:body element. It simply creates the ds-body div and applies
        templates of the body's child elements (which consists entirely of dri:div tags).
    -->
    <xsl:template match="dri:body">
        <div id="ds-body">
            <xsl:if test="not(/dri:document/dri:options/dri:list[@n='Payment'] or /dri:document/dri:options/dri:list[@n='discovery'] or /dri:document/dri:options/dri:list[@n='DryadSubmitData'] or /dri:document/dri:options/dri:list[@n='DryadSearch'] or /dri:document/dri:options/dri:list[@n='DryadConnect'])">
                <xsl:attribute name="style">
                    <xsl:text>width:100%</xsl:text>
                </xsl:attribute>
            </xsl:if>
            <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='alert'][@qualifier='message']">
                <div id="ds-system-wide-alert">
                    <p>
                        <xsl:copy-of
                                select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='alert'][@qualifier='message']/node()"/>
                    </p>
                </div>
            </xsl:if>
            <xsl:apply-templates/>
        </div>
    </xsl:template>


    <!-- Currently the dri:meta element is not parsed directly. Instead, parts of it are referenced from inside
        other elements (like reference). The blank template below ends the execution of the meta branch -->
    <xsl:template match="dri:meta">
    </xsl:template>

    <!-- Meta's children: userMeta, pageMeta, objectMeta and repositoryMeta may or may not have templates of
        their own. This depends on the meta template implementation, which currently does not go this deep.
    <xsl:template match="dri:userMeta" />
    <xsl:template match="dri:pageMeta" />
    <xsl:template match="dri:objectMeta" />
    <xsl:template match="dri:repositoryMeta" />
    -->

    <xsl:template name="addJavascript">
        <script type="text/javascript">
            <xsl:text disable-output-escaping="yes">var JsHost = (("https:" == document.location.protocol) ? "https://" : "http://");
            document.write(unescape("%3Cscript src='" + JsHost + "ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js' type='text/javascript'%3E%3C/script%3E"));</xsl:text>
        </script>

        <xsl:variable name="localJQuerySrc">
            <xsl:value-of
                    select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
            <xsl:text>/static/js/jquery-1.9.1.min.js</xsl:text>
        </xsl:variable>

        <script type="text/javascript">
            <xsl:text disable-output-escaping="yes">!window.jQuery &amp;&amp; document.write('&lt;script type="text/javascript" src="</xsl:text><xsl:value-of
                select="$localJQuerySrc"/><xsl:text
                disable-output-escaping="yes">"&gt;&#160;&lt;\/script&gt;')</xsl:text>
        </script>

        <!-- include the jQuery Migrate plugin to support deprecated APIs like jQuery.browser (used in some of plugins) -->
        <script type="text/javascript">
            <xsl:attribute name="src">
                <xsl:value-of
                        select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/themes/</xsl:text>
                <xsl:value-of
                        select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                <xsl:text>/lib/js/jquery-migrate-1.1.1.min.js</xsl:text>
            </xsl:attribute>
            &#160;
        </script>

        <script type="text/javascript">
            <xsl:attribute name="src">
                <xsl:value-of
                        select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/themes/</xsl:text>
                <xsl:value-of
                        select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                <xsl:text>/lib/js/jquery.bxslider.min.js</xsl:text>
            </xsl:attribute>
            &#160;
        </script>

        <script type="text/javascript">
            <xsl:attribute name="src">
                <xsl:value-of
                        select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/themes/</xsl:text>
                <xsl:value-of
                        select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                <xsl:text>/lib/js/jquery.dataTables.js</xsl:text>
            </xsl:attribute>
            &#160;
        </script>

        <script type="text/javascript">
            <xsl:attribute name="src">
                <xsl:value-of
                        select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/themes/</xsl:text>
                <xsl:value-of
                        select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                <xsl:text>/lib/js/jquery.hoverIntent.js</xsl:text>
            </xsl:attribute>
            &#160;
        </script>

        <script type="text/javascript">
            <xsl:attribute name="src">
                <xsl:value-of
                        select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/themes/</xsl:text>
                <xsl:value-of
                        select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                <xsl:text>/lib/js/superfish.js</xsl:text>
            </xsl:attribute>
            &#160;
        </script>

        <script type="text/javascript">
            <xsl:attribute name="src">
                <xsl:value-of
                        select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/themes/</xsl:text>
                <xsl:value-of
                        select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                <xsl:text>/lib/js/supersubs.js</xsl:text>
            </xsl:attribute>
            &#160;
        </script>

        <script type="text/javascript">
            <xsl:attribute name="src">
                <xsl:value-of
                        select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/themes/</xsl:text>
                <xsl:value-of
                        select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                <xsl:text>/lib/js/supposition-BLACK-BLOG-MODS.js</xsl:text>
            </xsl:attribute>
            &#160;
        </script>

        <script type="text/javascript">
            $('input#aspect_discovery_SimpleSearch_field_query').attr('placeholder','Enter keyword, author, title, DOI, etc. Example: herbivory');
        </script>

        <!-- Emulate HTML5 placeholder behavior (prompting text in input fields) with assigned CSS class. -->
        <script type="text/javascript">
            <xsl:attribute name="src">
                <xsl:value-of
                        select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/themes/</xsl:text>
                <xsl:value-of
                        select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                <xsl:text>/lib/js/jquery.complete-placeholder.min.js</xsl:text>
            </xsl:attribute>
            &#160;
        </script>

        <script type="text/javascript" language="javascript" src="http://platform.twitter.com/widgets.js">
            <xsl:text>&#160;</xsl:text>
        </script>

        <!-- Add theme javascipt  -->
        <xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='javascript'][not(@qualifier)]">
            <script type="text/javascript">
                <xsl:attribute name="src">
                    <xsl:value-of
                            select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                    <xsl:text>/themes/</xsl:text>
                    <xsl:value-of
                            select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                    <xsl:text>/</xsl:text>
                    <xsl:value-of select="."/>
                </xsl:attribute>
                &#160;
            </script>
        </xsl:for-each>

        <!-- add "shared" javascript from static, path is relative to webapp root-->
        <xsl:for-each
                select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='javascript'][@qualifier='static']">
            <!--This is a dirty way of keeping the scriptaculous stuff from choice-support
            out of our theme without modifying the administrative and submission sitemaps.
            This is obviously not ideal, but adding those scripts in those sitemaps is far
            from ideal as well-->
            <xsl:choose>
                <xsl:when test="text() = 'static/js/choice-support.js'">
                    <script type="text/javascript">
                        <xsl:attribute name="src">
                            <xsl:value-of
                                    select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                            <xsl:text>/themes/</xsl:text>
                            <xsl:value-of
                                    select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                            <xsl:text>/lib/js/choice-support.js</xsl:text>
                        </xsl:attribute>
                        &#160;
                    </script>
                </xsl:when>
                <xsl:when test="not(starts-with(text(), 'static/js/scriptaculous'))">
                    <script type="text/javascript">
                        <xsl:attribute name="src">
                            <xsl:value-of
                                    select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                            <xsl:text>/</xsl:text>
                            <xsl:value-of select="."/>
                        </xsl:attribute>
                        &#160;
                    </script>
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
                <xsl:value-of
                        select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/themes/</xsl:text>
                <xsl:value-of
                        select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                <xsl:text>/lib/js/DD_belatedPNG_0.0.8a.js?v=1</xsl:text>
            </xsl:attribute>
            &#160;
        </script>
        <script type="text/javascript">
            <xsl:text>DD_belatedPNG.fix('.ds-header-logo');DD_belatedPNG.fix('#ds-footer-logo');jQuery.each(jQuery('img[src$=png]'), function() {DD_belatedPNG.fixPng(this);});</xsl:text>
        </script>
        <xsl:text disable-output-escaping="yes">&lt;![endif]--&gt;</xsl:text>

        <!-- Include all on-document-ready JS, incl. some for specific pages -->
        <script type="text/javascript">
            <xsl:attribute name="src">
                <xsl:value-of
                        select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/themes/</xsl:text>
                <xsl:value-of
                        select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                <xsl:text>/lib/js/dryad-pages.js</xsl:text>
            </xsl:attribute>
            &#160;
        </script>

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
           </xsl:text>
            </script>
        </xsl:if>
        <!-- UserVoice SDK -->
        <script><xsl:text>(function(){var uv=document.createElement('script');uv.type='text/javascript';uv.async=true;uv.src='//widget.uservoice.com/oW4J4by2WMgw3H4qYuJsDQ.js';var s=document.getElementsByTagName('script')[0];s.parentNode.insertBefore(uv,s)})()</xsl:text></script>
        <!-- A function to launch the UserVoice Classic Widget -->
        <script>
        <xsl:text>
        UserVoice = window.UserVoice || [];
        function showClassicWidget() {
          UserVoice.push(['showLightbox', 'classic_widget', {
            mode: 'feedback',
            primary_color: '#88c033',
            link_color: '#333333',
            forum_id: 197408,
            feedback_tab_name: 'Ideas Forum'
          }]);
        }
        </xsl:text>
        </script>
    </xsl:template>

</xsl:stylesheet>
