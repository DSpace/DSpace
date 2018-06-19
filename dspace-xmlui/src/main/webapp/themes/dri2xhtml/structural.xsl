<?xml version="1.0" encoding="UTF-8"?>
<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<!--
    TODO: Describe this XSL file
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
        xmlns:confman="org.dspace.core.ConfigurationManager"
        exclude-result-prefixes="mets xlink xsl dim xhtml mods dc confman">
    
    <xsl:output indent="yes"/>
    
    <!-- Global variables -->
    
    <!--
        Context path provides easy access to the context-path parameter. This is
        used when building urls back to the site, they all must include the
        context-path paramater.
    -->
    <xsl:variable name="context-path" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
    
    <!--
        Theme path represents the full path back to theme. This is useful for
        accessing static resources such as images or javascript files. Simply
        prepend this variable and then add the file name, thus
        {$theme-path}/images/myimage.jpg will result in the full path from the
        HTTP root down to myimage.jpg. The theme path is composed of the
        "[context-path]/themes/[theme-dir]/".
    -->
    <xsl:variable name="theme-path" select="concat($context-path,'/themes/',/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path'])"/>

    <!--
        Full URI of the current page. Composed of scheme, server name and port and request URI.
    -->
    <xsl:variable name="current-uri">
        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='scheme']"/>
        <xsl:text>://</xsl:text>
        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='serverName']"/>
        <xsl:text>:</xsl:text>
        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='serverPort']"/>
        <xsl:text>/</xsl:text>
        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='URI']"/>
    </xsl:variable>

    <!--
        Requested Page URI. Some functions may alter behavior of processing depending if URI matches a pattern.
        Specifically, adding a static page will need to override the DRI, to directly add content.
    -->
    <xsl:variable name="request-uri" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='URI']"/>
    
    <!--
    This stylesheet will be written in several stages:
        1. Establish all the templates that catch all the elements
        2. Finish implementing the XHTML output within the templates
        3. Figure out the special case stuff as well as the small details
        4. Clean up the code
    
    Currently in stage 3...
        
    Last modified on: 3/15/2006
    -->
    
    <!-- This stylesheet's purpose is to translate a DRI document to an HTML one, a task which it accomplishes
        through interative application of templates to select elements. While effort has been made to
        annotate all templates to make this stylesheet self-documenting, not all elements are used (and
        therefore described) here, and those that are may not be used to their full capacity. For this reason,
        you should consult the DRI Schema Reference manual if you intend to customize this file for your needs.
    -->
        
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
        <html>
            <!-- First of all, build the HTML head element -->
            <xsl:call-template name="buildHead"/>
            <!-- Then proceed to the body -->
            <xsl:choose>
              <xsl:when test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='framing'][@qualifier='popup']">
                <xsl:apply-templates select="dri:body/*"/>
                <!-- add setup JS code if this is a choices lookup page -->
                <xsl:if test="dri:body/dri:div[@n='lookup']">
                  <xsl:call-template name="choiceLookupPopUpSetup"/>
                </xsl:if>
              </xsl:when>
              <xsl:otherwise>
            <body>
                
                <div id="ds-main">
                    <!--
                        The header div, complete with title, subtitle, trail and other junk. The trail is
                        built by applying a template over pageMeta's trail children. -->
                    <xsl:call-template name="buildHeader"/>
                    
                    <!--
                        Goes over the document tag's children elements: body, options, meta. The body template
                        generates the ds-body div that contains all the content. The options template generates
                        the ds-options div that contains the navigation and action options available to the
                        user. The meta element is ignored since its contents are not processed directly, but
                        instead referenced from the different points in the document. -->
                    <xsl:apply-templates />

                    <!--
                        The footer div, dropping whatever extra information is needed on the page. It will
                        most likely be something similar in structure to the currently given example. -->
                    <xsl:call-template name="buildFooter"/>
                    
                </div>
            </body>
              </xsl:otherwise>
            </xsl:choose>
        </html>
    </xsl:template>
    
    
    <!-- The HTML head element contains references to CSS as well as embedded JavaScript code. Most of this
        information is either user-provided bits of post-processing (as in the case of the JavaScript), or
        references to stylesheets pulled directly from the pageMeta element. -->
    <xsl:template name="buildHead">
        <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
            <meta name="Generator">
              <xsl:attribute name="content">
                <xsl:text>DSpace</xsl:text>
                <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='dspace'][@qualifier='version']">
                  <xsl:text> </xsl:text>
                  <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='dspace'][@qualifier='version']"/>
                </xsl:if>
              </xsl:attribute>
            </meta>
            <!-- Add stylesheets -->
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
                                //Clear default text of empty text areas on focus
                                function tFocus(element)
                                {
                                        if (element.value == '<i18n:text>xmlui.dri2xhtml.default.textarea.value</i18n:text>'){element.value='';}
                                }
                                //Clear default text of empty text areas on submit
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
            </script>

            <!-- add "shared" javascript from static, path is relative to webapp root -->
            <xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='javascript'][@qualifier='url']">
                <script type="text/javascript">
                    <xsl:attribute name="src">
                        <xsl:value-of select="."/>
                    </xsl:attribute>&#160;</script>
            </xsl:for-each>

            <!-- add "shared" javascript from static, path is relative to webapp root -->
            <xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='javascript'][@qualifier='static']">
                <script type="text/javascript">
                    <xsl:attribute name="src">
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                        <xsl:text>/</xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:attribute>&#160;</script>
            </xsl:for-each>

            <!-- Add theme javascript  -->
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
            
            <xsl:call-template name="buildHead-google-analytics" />
            
            <!-- Add the title in -->
            <xsl:variable name="page_title" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='title']" />
            <title>
                <xsl:choose>
                    <xsl:when test="starts-with($request-uri, 'page/about')">
                        <xsl:text>About This Repository</xsl:text>
                    </xsl:when>
                    <xsl:when test="not($page_title) or (string-length($page_title) &lt; 1)">
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
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
    
    
    <xsl:template name="buildHead-google-analytics">
            <!-- Add a google analytics script if the key is present -->
            <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='google'][@qualifier='analytics']">
                <script><xsl:text>
                    (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
                    (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
                    m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
                    })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

                    ga('create', '</xsl:text><xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='google'][@qualifier='analytics']"/><xsl:text>', '</xsl:text><xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='serverName']"/><xsl:text>');
                    ga('send', 'pageview');
                </xsl:text></script>
            </xsl:if>
    </xsl:template>

    
    <!-- The header (distinct from the HTML head element) contains the title, subtitle, login box and various
        placeholders for header images -->
    <xsl:template name="buildHeader">
        <div id="ds-header">
            <a>
                <xsl:attribute name="href">
                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                    <xsl:text>/</xsl:text>
                </xsl:attribute>
                <span id="ds-header-logo">&#160;</span>
            </a>
            <h1 class="pagetitle">
                <xsl:choose>
                        <!-- protection against an empty page title -->
                        <xsl:when test="not(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='title']) or (string-length(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='title']) &lt; 1)">
                                <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                        </xsl:when>
                        <xsl:otherwise>
                                <xsl:copy-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='title']/node()"/>
                        </xsl:otherwise>
                </xsl:choose>
                        
            </h1>
            <h2 class="static-pagetitle"><i18n:text>xmlui.dri2xhtml.structural.head-subtitle</i18n:text></h2>
            
            
            <ul id="ds-trail">
                <xsl:choose>
                        <xsl:when test="starts-with($request-uri, 'page/about')">
                            <xsl:text>About This Repository</xsl:text>
                        </xsl:when>
                        <xsl:when test="count(/dri:document/dri:meta/dri:pageMeta/dri:trail) = 0">
                                <li class="ds-trail-link first-link"> - </li>
                        </xsl:when>
                        <xsl:otherwise>
                                <xsl:apply-templates select="/dri:document/dri:meta/dri:pageMeta/dri:trail"/>
                        </xsl:otherwise>
                </xsl:choose>
            </ul>
           
            
            <xsl:choose>
                <xsl:when test="/dri:document/dri:meta/dri:userMeta/@authenticated = 'yes'">
                    <div id="ds-user-box">
                        <p>
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
                            <xsl:text> | </xsl:text>
                            <a>
                                <xsl:attribute name="href">
                                    <xsl:value-of select="/dri:document/dri:meta/dri:userMeta/
                                        dri:metadata[@element='identifier' and @qualifier='logoutURL']"/>
                                </xsl:attribute>
                                <i18n:text>xmlui.dri2xhtml.structural.logout</i18n:text>
                            </a>
                        </p>
                        <p>
                            <xsl:call-template name="languageSelection" />
                        </p>
                    </div>
                </xsl:when>
                <xsl:otherwise>
                    <div id="ds-user-box">
                        <p>
                            <a>
                                <xsl:attribute name="href">
                                    <xsl:value-of select="/dri:document/dri:meta/dri:userMeta/
                                        dri:metadata[@element='identifier' and @qualifier='loginURL']"/>
                                </xsl:attribute>
                                <i18n:text>xmlui.dri2xhtml.structural.login</i18n:text>
                            </a>
                        </p>
                        <p>
                            <xsl:call-template name="languageSelection" />
                        </p>
                    </div>
                </xsl:otherwise>
            </xsl:choose>
            
        </div>
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


    <!-- Like the header, the footer contains various miscellaneous text, links, and image placeholders -->
    <xsl:template name="buildFooter">
        <div id="ds-footer">
            <i18n:text>xmlui.dri2xhtml.structural.footer-promotional</i18n:text>
            <div id="ds-footer-links">
                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                        <xsl:text>/contact</xsl:text>
                    </xsl:attribute>
                    <i18n:text>xmlui.dri2xhtml.structural.contact-link</i18n:text>
                </a>
                <xsl:text> | </xsl:text>
                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                        <xsl:text>/feedback</xsl:text>
                    </xsl:attribute>
                    <i18n:text>xmlui.dri2xhtml.structural.feedback-link</i18n:text>
                </a>
            </div>
            <!-- Invisible link to HTML sitemap (for search engines) -->
            <a>
                <xsl:attribute name="href">
                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                    <xsl:text>/htmlmap</xsl:text>
                </xsl:attribute>
            </a>
        </div>
        <!--
            <a href="http://di.tamu.edu">
                <div id="ds-footer-logo"></div>
            </a>
            <p>
            This website is using Manakin, a new front end for DSpace created by Texas A&amp;M University
            Libraries. The interface can be extensively modified through Manakin Aspects and XSL based Themes.
            For more information visit
            <a href="http://di.tamu.edu">http://di.tamu.edu</a> and
            <a href="http://dspace.org">http://dspace.org</a>
            </p>
        -->
    </xsl:template>
    
    
    
    <!--
        The trail is built one link at a time. Each link is given the ds-trail-link class, with the first and
        the last links given an additional descriptor.
    -->
    <xsl:template match="dri:trail">
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
        The meta, body, options elements; the three top-level elements in the schema
-->
    
    
    
    
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

            <!-- Check for the custom pages -->

            <xsl:choose>
                <xsl:when test="starts-with($request-uri, 'page/about')">
                    <div>
                        <h1>About This Repository</h1>
                        <p>To add your own content to this page, edit webapps/xmlui/themes/dri2xhtml/structural.xsl and
                            add your own content to the title, trail, and body. If you wish to add additional pages, you
                            will need to create an additional xsl:when block and match the request-uri to whatever page
                            you are adding. Currently, static pages created through altering XSL are only available
                            under the URI prefix of page/.</p>
                    </div>
                </xsl:when>
                <!-- Otherwise use default handling of body -->
                <xsl:otherwise>
                    <xsl:apply-templates />
                </xsl:otherwise>
            </xsl:choose>


            <xsl:copy-of select="$SFXLink"/>
        </div>
    </xsl:template>

    <!--
        This variable contains a link to SFX or a SFX button (link with an image).
        Relies on sfx.server.url and sfx.server.image_url in the config file, which are transferred in dri:pageMeta.
    -->
    <xsl:variable name="SFXLink">
        <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='sfx'][@qualifier='server']">
            <a>
                <xsl:attribute name="href">
                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='sfx'][@qualifier='server']"/>
                </xsl:attribute>
                <xsl:choose>
                    <xsl:when test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='sfx'][@qualifier='image_url']">
                        <img>
                            <xsl:attribute name="src">
                                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='sfx'][@qualifier='image_url']"/>
                            </xsl:attribute>
                            <xsl:attribute name="alt">
                                <xsl:text>Find Full text</xsl:text>
                            </xsl:attribute>
                        </img>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text>Find Full text</xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
            </a>
        </xsl:if>
    </xsl:variable>
    
    <!--
        The template to handle dri:options. Since it contains only dri:list tags (which carry the actual
        information), the only things than need to be done is creating the ds-options div and applying
        the templates inside it.
        
        In fact, the only bit of real work this template does is add the search box, which has to be
        handled specially in that it is not actually included in the options div, and is instead built
        from metadata available under pageMeta.
    -->
    <!-- TODO: figure out why i18n tags break the go button -->
    <xsl:template match="dri:options">
        <div id="ds-options">
            <h3 id="ds-search-option-head" class="ds-option-set-head"><i18n:text>xmlui.dri2xhtml.structural.search</i18n:text></h3>
            <div id="ds-search-option" class="ds-option-set">
                <!-- The form, complete with a text box and a button, all built from attributes referenced
                    from under pageMeta. -->
                <form id="ds-search-form" method="post">
                    <xsl:attribute name="action">
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath']"/>
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='simpleURL']"/>
                    </xsl:attribute>
                    <fieldset>
                        <input class="ds-text-field " type="text">
                            <xsl:attribute name="name">
                                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='queryField']"/>
                            </xsl:attribute>
                        </input>
                        <input class="ds-button-field " name="submit" type="submit" i18n:attr="value" value="xmlui.general.go" >
                            <xsl:attribute name="onclick">
                                <xsl:text>
                                    var radio = document.getElementById(&quot;ds-search-form-scope-container&quot;);
                                    if (radio != undefined &amp;&amp; radio.checked)
                                    {
                                    var form = document.getElementById(&quot;ds-search-form&quot;);
                                    form.action=
                                </xsl:text>
                                <xsl:text>&quot;</xsl:text>
                                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath']"/>
                                <xsl:text>/handle/&quot; + radio.value + &quot;</xsl:text>
                                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='simpleURL']"/>
                                <xsl:text>&quot; ; </xsl:text>
                                <xsl:text>
                                    }
                                </xsl:text>
                            </xsl:attribute>
                        </input>
                        <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='focus'][@qualifier='container']">
                            <label>
                                <input id="ds-search-form-scope-all" type="radio" name="scope" value="" checked="checked"/>
                                <i18n:text>xmlui.dri2xhtml.structural.search</i18n:text>
                            </label>
                            <br/>
                            <label>
                                <input id="ds-search-form-scope-container" type="radio" name="scope">
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="substring-after(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='focus'][@qualifier='container'],':')"/>
                                    </xsl:attribute>
                                </input>
                                <xsl:choose>
                                    <xsl:when test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='focus'][@qualifier='containerType']/text() = 'type:community'">
                                            <i18n:text>xmlui.dri2xhtml.structural.search-in-community</i18n:text>
                                    </xsl:when>
                                    <xsl:otherwise>
                                            <i18n:text>xmlui.dri2xhtml.structural.search-in-collection</i18n:text>
                                    </xsl:otherwise>
                                                                                              
                                </xsl:choose>
                            </label>
                        </xsl:if>
                    </fieldset>
                </form>
                <!-- Only add if the advanced search url is different from the simple search -->
                <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='advancedURL'] != /dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='simpleURL']">
                    <!-- The "Advanced search" link, to be perched underneath the search box -->
                    <a>
                        <xsl:attribute name="href">
                            <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='advancedURL']"/>
                        </xsl:attribute>
                        <i18n:text>xmlui.dri2xhtml.structural.search-advanced</i18n:text>
                    </a>
                </xsl:if>
            </div>
            
            <!-- Once the search box is built, the other parts of the options are added -->
            <xsl:apply-templates />

            <!-- DS-984 Add RSS Links to Options Box -->
            <xsl:if test="count(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='feed']) != 0">
                <h3 id="ds-feed-option-head" class="ds-option-set-head">
                    <i18n:text>xmlui.feed.header</i18n:text>
                </h3>
                <div id="ds-feed-option" class="ds-option-set">
                    <ul>
                        <xsl:call-template name="addRSSLinks"/>
                    </ul>
                </div>
            </xsl:if>

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
    
    
      
    
    
    
    
    
<!--
        Structural elements from here on out. These are the tags that contain static content under body, i.e.
        lists, tables and divs. They are also used in making forms and referencing metadata from the object
        store through the use of reference elements. The lists used by options also have templates here.
-->
    
    
    
    
    <!-- First and foremost come the div elements, which are the only elements directly under body. Every
        document has a body and every body has at least one div, which may in turn contain other divs and
        so on. Divs can be of two types: interactive and non-interactive, as signified by the attribute of
        the same name. The two types are handled separately.
    -->
    
    <!-- Non-interactive divs get turned into HTML div tags. The general process, which is found in many
        templates in this stylesheet, is to call the template for the head element (creating the HTML h tag),
        handle the attributes, and then apply the templates for the all children except the head. The id
        attribute is -->
    <xsl:template match="dri:div" priority="1">
        <xsl:apply-templates select="dri:head"/>
        <xsl:apply-templates select="@pagination">
            <xsl:with-param name="position">top</xsl:with-param>
        </xsl:apply-templates>
        <div>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-static-div</xsl:with-param>
            </xsl:call-template>
            <xsl:choose>
                    <!--  does this element have any children -->
                    <xsl:when test="child::node()">
                        <xsl:apply-templates select="*[not(name()='head')]"/>
                    </xsl:when>
                        <!-- if no children are found we add a space to eliminate self closing tags -->
                        <xsl:otherwise>
                                &#160;
                        </xsl:otherwise>
                </xsl:choose>
        </div>
        <xsl:variable name="itemDivision">
                        <xsl:value-of select="@n"/>
                </xsl:variable>
                <xsl:variable name="xrefTarget">
                        <xsl:value-of select="./dri:p/dri:xref/@target"/>
                </xsl:variable>
                <xsl:if test="$itemDivision='item-view'">
                    <xsl:call-template name="cc-license">
                        <xsl:with-param name="metadataURL" select="./dri:referenceSet/dri:reference/@url"/>
                    </xsl:call-template>
                </xsl:if>
        <xsl:apply-templates select="@pagination">
            <xsl:with-param name="position">bottom</xsl:with-param>
        </xsl:apply-templates>
    </xsl:template>
    
    <!-- Interactive divs get turned into forms. The priority attribute on the template itself
        signifies that this template should be executed if both it and the one above match the
        same element (namely, the div element).
        
        Strictly speaking, XSL should be smart enough to realize that since one template is general
        and other more specific (matching for a tag and an attribute), it should apply the more
        specific once is it encounters a div with the matching attribute. However, the way this
        decision is made depends on the implementation of the XSL parser is not always consistent.
        For that reason explicit priorities are a safer, if perhaps redundant, alternative. -->
    <xsl:template match="dri:div[@interactive='yes']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <xsl:apply-templates select="@pagination">
            <xsl:with-param name="position">top</xsl:with-param>
        </xsl:apply-templates>
        <form>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-interactive-div</xsl:with-param>
            </xsl:call-template>
            <xsl:attribute name="action"><xsl:value-of select="@action"/></xsl:attribute>
            <xsl:attribute name="method"><xsl:value-of select="@method"/></xsl:attribute>
            <xsl:if test="@method='multipart'">
                <xsl:attribute name="method">post</xsl:attribute>
                <xsl:attribute name="enctype">multipart/form-data</xsl:attribute>
            </xsl:if>
            <xsl:attribute name="onsubmit">javascript:tSubmit(this);</xsl:attribute>
            <!--For Item Submission process, disable ability to submit a form by pressing 'Enter'-->
            <xsl:if test="starts-with(@n,'submit')">
                <xsl:attribute name="onkeydown">javascript:return disableEnterKey(event);</xsl:attribute>
            </xsl:if>
            <xsl:apply-templates select="*[not(name()='head')]"/>
          
        </form>
        <!-- JS to scroll form to DIV parent of "Add" button if jump-to -->
        <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='page'][@qualifier='jumpTo']">
          <script type="text/javascript">
            <xsl:text>var button = document.getElementById('</xsl:text>
            <xsl:value-of select="translate(@id,'.','_')"/>
            <xsl:text>').elements['</xsl:text>
            <xsl:value-of select="concat('submit_',/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='page'][@qualifier='jumpTo'],'_add')"/>
            <xsl:text>'];</xsl:text>
            <xsl:text>
                      if (button != null) {
                        var n = button.parentNode;
                        for (; n != null; n = n.parentNode) {
                            if (n.tagName == 'DIV') {
                              n.scrollIntoView(false);
                              break;
                           }
                        }
                      }
            </xsl:text>
          </script>
        </xsl:if>
        <xsl:apply-templates select="@pagination">
            <xsl:with-param name="position">bottom</xsl:with-param>
        </xsl:apply-templates>
    </xsl:template>
    
    
    <!-- Special case for divs tagged as "notice" -->
    <xsl:template match="dri:div[@n='general-message']" priority="3">
        <div>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-notice-div</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </div>
    </xsl:template>
    
    
    <!-- Next come the three structural elements that divs that contain: table, p, and list. These are
        responsible for display of static content, forms, and option lists. The fourth element under
        body, referenceSet, is used to reference blocks of metadata and will be discussed further down.
    -->
    
    
    <!-- First, the table element, used for rendering data in tabular format. In DRI tables consist of
        an optional head element followed by a set of row tags. Each row in turn contains a set of cells.
        Rows and cells can have different roles, the most common ones being header and data (with the
        attribute omitted in the latter case). -->
    <xsl:template match="dri:table">
        <xsl:apply-templates select="dri:head"/>
        <table>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-table</xsl:with-param>
            </xsl:call-template>
            <!-- rows and cols attributes are not allowed in strict
            <xsl:attribute name="rows"><xsl:value-of select="@rows"/></xsl:attribute>
            <xsl:attribute name="cols"><xsl:value-of select="@cols"/></xsl:attribute>

            <xsl:if test="count(dri:row[@role='header']) &gt; 0">
                    <thead>
                        <xsl:apply-templates select="dri:row[@role='header']"/>
                    </thead>
            </xsl:if>
            <tbody>
                <xsl:apply-templates select="dri:row[not(@role='header')]"/>
            </tbody>
            -->
            <xsl:apply-templates select="dri:row"/>
        </table>
    </xsl:template>
    
    <!-- Header row, most likely filled with header cells -->
    <xsl:template match="dri:row[@role='header']" priority="2">
        <tr>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-table-header-row</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </tr>
    </xsl:template>
    
    <!-- Header cell, assumed to be one since it is contained in a header row -->
    <xsl:template match="dri:row[@role='header']/dri:cell | dri:cell[@role='header']" priority="2">
        <th>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-table-header-cell
                    <xsl:if test="(position() mod 2 = 0)">even</xsl:if>
                    <xsl:if test="(position() mod 2 = 1)">odd</xsl:if>
                </xsl:with-param>
            </xsl:call-template>
            <xsl:if test="@rows">
                <xsl:attribute name="rowspan">
                    <xsl:value-of select="@rows"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:if test="@cols">
                <xsl:attribute name="colspan">
                    <xsl:value-of select="@cols"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:apply-templates />
        </th>
    </xsl:template>
    
    
    <!-- Normal row, most likely filled with data cells -->
    <xsl:template match="dri:row" priority="1">
        <tr>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-table-row
                    <xsl:if test="(position() mod 2 = 0)">even</xsl:if>
                    <xsl:if test="(position() mod 2 = 1)">odd</xsl:if>
                </xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </tr>
    </xsl:template>
    
    <!-- Just a plain old table cell -->
    <xsl:template match="dri:cell" priority="1">
        <td>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-table-cell
                    <xsl:if test="(position() mod 2 = 0)">even</xsl:if>
                    <xsl:if test="(position() mod 2 = 1)">odd</xsl:if>
                </xsl:with-param>
            </xsl:call-template>
            <xsl:if test="@rows">
                <xsl:attribute name="rowspan">
                    <xsl:value-of select="@rows"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:if test="@cols">
                <xsl:attribute name="colspan">
                    <xsl:value-of select="@cols"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:apply-templates />
        </td>
    </xsl:template>
    
    
    
      
    
    
    <!-- Second, the p element, used for display of text. The p element is a rich text container, meaning it
        can contain text mixed with inline elements like hi, xref, figure and field. The cell element above
        and the item element under list are also rich text containers.
    -->
    <xsl:template match="dri:p">
        <p>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-paragraph</xsl:with-param>
            </xsl:call-template>
            <xsl:choose>
                <!--  does this element have any children -->
                <xsl:when test="child::node()">
                        <xsl:apply-templates />
                        </xsl:when>
                        <!-- if no children are found we add a space to eliminate self closing tags -->
                        <xsl:otherwise>
                                &#160;
                        </xsl:otherwise>
            </xsl:choose>
            
        </p>
    </xsl:template>
    
    
    
    <!-- Finally, we have the list element, which is used to display set of data. There are several different
        types of lists, as signified by the type attribute, and several different templates to handle them. -->
    
    <!-- First list type is the bulleted list, a list with no real labels and no ordering between elements. -->
    <xsl:template match="dri:list[@type='bulleted']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <ul>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-bulleted-list</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="*[not(name()='head')]" mode="nested"/>
        </ul>
    </xsl:template>

    <!-- The item template creates an HTML list item element and places the contents of the DRI item inside it.
        Additionally, it checks to see if the currently viewed item has a label element directly preceeding it,
        and if it does, applies the label's template before performing its own actions. This mechanism applies
        to the list item templates as well. -->
    <xsl:template match="dri:list[@type='bulleted']/dri:item" priority="2" mode="nested">
        <li>
            <xsl:if test="name(preceding-sibling::*[position()=1]) = 'dri:label'">
                <xsl:apply-templates select="preceding-sibling::*[position()=1]"/>
            </xsl:if>
            <xsl:apply-templates />
        </li>
    </xsl:template>

    <!-- The case of nested lists is handled in a similar way across all lists. You match the sub-list based on
        its parent, create a list item approtiate to the list type, fill its content from the sub-list's head
        element and apply the other templates normally. -->
    <xsl:template match="dri:list[@type='bulleted']/dri:list" priority="3" mode="nested">
        <li>
            <xsl:apply-templates select="."/>
        </li>
    </xsl:template>


    <!-- Second type is the ordered list, which is a list with either labels or names to designate an ordering
        of some kind. -->
    <xsl:template match="dri:list[@type='ordered']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <ol>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-ordered-list</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="*[not(name()='head')]" mode="nested">
                <xsl:sort select="dri:item/@n"/>
            </xsl:apply-templates>
        </ol>
    </xsl:template>

    <xsl:template match="dri:list[@type='ordered']/dri:item" priority="2" mode="nested">
        <li>
            <xsl:if test="name(preceding-sibling::*[position()=1]) = 'label'">
                <xsl:apply-templates select="preceding-sibling::*[position()=1]"/>
            </xsl:if>
            <xsl:apply-templates />
        </li>
    </xsl:template>

    <xsl:template match="dri:list[@type='ordered']/dri:list" priority="3" mode="nested">
        <li>
            <xsl:apply-templates select="."/>
        </li>
    </xsl:template>


    <!-- Progress list used primarily in forms that span several pages. There isn't a template for the nested
        version of this list, mostly because there isn't a use case for it. -->
    <xsl:template match="dri:list[@type='progress']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <ul>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-progress-list</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="dri:item"/>
        </ul>
    </xsl:template>

    <xsl:template match="dri:list[@type='progress']/dri:item" priority="2">
        <li>
            <xsl:attribute name="class">
                <xsl:value-of select="@rend"/>
                <xsl:if test="position()=1">
                    <xsl:text> first</xsl:text>
                </xsl:if>
                <xsl:if test="descendant::dri:field[@type='button']">
                    <xsl:text> button</xsl:text>
                </xsl:if>
                <xsl:if test="position()=last()">
                    <xsl:text> last</xsl:text>
                </xsl:if>
            </xsl:attribute>
            <xsl:apply-templates />
        </li>
        <xsl:if test="not(position()=last())">
            <li class="arrow">
                <xsl:text>&#8594;</xsl:text>
            </li>
        </xsl:if>
    </xsl:template>


    <!-- The third type of list is the glossary (gloss) list. It is essentially a list of pairs, consisting of
        a set of labels, each followed by an item. Unlike the ordered and bulleted lists, gloss is implemented
        via HTML definition list (dd) element. It can also be changed to work as a two-column table. -->
    <xsl:template match="dri:list[@type='gloss']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <dl>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-gloss-list</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="*[not(name()='head')]" mode="nested"/>
        </dl>
    </xsl:template>

    <xsl:template match="dri:list[@type='gloss']/dri:item" priority="2" mode="nested">
        <dd>
            <xsl:apply-templates />
        </dd>
    </xsl:template>

    <xsl:template match="dri:list[@type='gloss']/dri:label" priority="2" mode="nested">
        <dt>
            <span>
                <xsl:attribute name="class">
                    <xsl:text>ds-gloss-list-label </xsl:text>
                    <xsl:value-of select="@rend"/>
                </xsl:attribute>
                <xsl:apply-templates />
                <xsl:text>:</xsl:text>
            </span>
        </dt>
    </xsl:template>

    <xsl:template match="dri:list[@type='gloss']/dri:list" priority="3" mode="nested">
        <dd>
            <xsl:apply-templates select="."/>
        </dd>
    </xsl:template>


    <!-- The next list type is one without a type attribute. In this case XSL makes a decision: if the items
        of the list have labels the the list will be made into a table-like structure, otherwise it is considered
        to be a plain unordered list and handled generically. -->
    <!-- TODO: This should really be done with divs and spans instead of tables. Form lists have already been
        converted so the solution here would most likely mirror that one -->
    <xsl:template match="dri:list[not(@type)]" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <xsl:if test="count(dri:label)>0">
            <table>
                <xsl:call-template name="standardAttributes">
                    <xsl:with-param name="class">ds-gloss-list</xsl:with-param>
                </xsl:call-template>
                <xsl:apply-templates select="dri:item" mode="labeled"/>
            </table>
        </xsl:if>
        <xsl:if test="count(dri:label)=0">
            <ul>
                <xsl:call-template name="standardAttributes">
                    <xsl:with-param name="class">ds-simple-list</xsl:with-param>
                </xsl:call-template>
                <xsl:apply-templates select="dri:item" mode="nested"/>
            </ul>
        </xsl:if>
    </xsl:template>

    <xsl:template match="dri:list[not(@type)]/dri:item" priority="2" mode="labeled">
        <tr>
            <xsl:attribute name="class">
                <xsl:text>ds-table-row </xsl:text>
                <xsl:if test="(position() mod 2 = 0)">even </xsl:if>
                <xsl:if test="(position() mod 2 = 1)">odd </xsl:if>
                <xsl:value-of select="@rend"/>
            </xsl:attribute>
            <xsl:if test="name(preceding-sibling::*[position()=1]) = 'label'">
                <xsl:apply-templates select="preceding-sibling::*[position()=1]" mode="labeled"/>
            </xsl:if>
            <td>
                <xsl:apply-templates />
            </td>
        </tr>
    </xsl:template>

    <xsl:template match="dri:list[not(@type)]/dri:label" priority="2" mode="labeled">
        <td>
            <xsl:if test="count(./node())>0">
                <span>
                    <xsl:attribute name="class">
                        <xsl:text>ds-gloss-list-label </xsl:text>
                        <xsl:value-of select="@rend"/>
                    </xsl:attribute>
                    <xsl:apply-templates />
                    <xsl:text>:</xsl:text>
                </span>
            </xsl:if>
        </td>
    </xsl:template>

    <xsl:template match="dri:list[not(@type)]/dri:item" priority="2" mode="nested">
        <li>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-simple-list-item</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
            <!-- Wrap orphaned sub-lists into the preceding item -->
            <xsl:variable name="node-set1" select="./following-sibling::dri:list"/>
            <xsl:variable name="node-set2" select="./following-sibling::dri:item[1]/following-sibling::dri:list"/>
            <xsl:apply-templates select="$node-set1[count(.|$node-set2) != count($node-set2)]"/>
        </li>
    </xsl:template>





    <!-- Special treatment of a list type "form", which is used to encode simple forms and give them structure.
        This is done partly to ensure that the resulting HTML form follows accessibility guidelines. -->

    <xsl:template match="dri:list[@type='form']" priority="3">
    <xsl:choose>
       <xsl:when test="ancestor::dri:list[@type='form']">

            <li>
            <fieldset>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">
                    <!-- Provision for the sub list -->
                    <xsl:text>ds-form-</xsl:text>
                    <xsl:if test="ancestor::dri:list[@type='form']">
                        <xsl:text>sub</xsl:text>
                    </xsl:if>
                    <xsl:text>list </xsl:text>
                    <xsl:if test="count(dri:item) > 3">
                        <xsl:text>thick </xsl:text>
                    </xsl:if>
                </xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="dri:head"/>

            <ol>
                <xsl:apply-templates select="*[not(name()='label' or name()='head')]" />
            </ol>
            </fieldset>
            </li>
            </xsl:when>
            <xsl:otherwise>
            <fieldset>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">
                    <!-- Provision for the sub list -->
                    <xsl:text>ds-form-</xsl:text>
                    <xsl:if test="ancestor::dri:list[@type='form']">
                        <xsl:text>sub</xsl:text>
                    </xsl:if>
                    <xsl:text>list </xsl:text>
                    <xsl:if test="count(dri:item) > 3">
                        <xsl:text>thick </xsl:text>
                    </xsl:if>
                </xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="dri:head"/>

            <ol>
                <xsl:apply-templates select="*[not(name()='label' or name()='head')]" />
            </ol>
            </fieldset>
            </xsl:otherwise>
            </xsl:choose>
    </xsl:template>

    <!-- TODO: Account for the dri:hi/dri:field kind of nesting here and everywhere else... -->
    <xsl:template match="dri:list[@type='form']/dri:item" priority="3">
        <li>
                <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">
                    <xsl:text>ds-form-item </xsl:text>
                <xsl:choose>
                    <!-- Makes sure that the dark always falls on the last item -->
                    <xsl:when test="count(../dri:item) mod 2 = 0">
                        <xsl:if test="count(../dri:item) > 3">
                            <xsl:if test="(count(preceding-sibling::dri:item) mod 2 = 0)">even </xsl:if>
                            <xsl:if test="(count(preceding-sibling::dri:item) mod 2 = 1)">odd </xsl:if>
                        </xsl:if>
                    </xsl:when>
                    <xsl:when test="count(../dri:item) mod 2 = 1">
                        <xsl:if test="count(../dri:item) > 3">
                            <xsl:if test="(count(preceding-sibling::dri:item) mod 2 = 1)">even </xsl:if>
                            <xsl:if test="(count(preceding-sibling::dri:item) mod 2 = 0)">odd </xsl:if>
                        </xsl:if>
                    </xsl:when>
                </xsl:choose>
                <!-- The last row is special if it contains only buttons -->
                <xsl:if test="position()=last() and dri:field[@type='button'] and not(dri:field[not(@type='button')])">last </xsl:if>
                <!-- The row is also tagged specially if it contains another "form" list -->
                <xsl:if test="dri:list[@type='form']">sublist </xsl:if>
                </xsl:with-param>
            </xsl:call-template>

            <xsl:choose>
                <xsl:when test="dri:field[@type='composite']">
                    <xsl:call-template name="pick-label"/>
                    <xsl:apply-templates mode="formComposite"/>
                </xsl:when>
                <xsl:when test="dri:list[@type='form']">
                    <xsl:apply-templates />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template name="pick-label"/>
                    <div class="ds-form-content">
                        <xsl:apply-templates />
                        <!-- special name used in submission UI review page -->
                        <xsl:if test="@n = 'submit-review-field-with-authority'">
                          <xsl:call-template name="authorityConfidenceIcon">
                            <xsl:with-param name="confidence" select="substring-after(./@rend, 'cf-')"/>
                          </xsl:call-template>
                        </xsl:if>
                    </div>
                </xsl:otherwise>
            </xsl:choose>
        </li>
    </xsl:template>

    <!-- An item in a nested "form" list -->
    <xsl:template match="dri:list[@type='form']//dri:list[@type='form']/dri:item" priority="3">
        <li>
                <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">
                    <xsl:text>ds-form-item </xsl:text>

                <!-- Row counting voodoo, meant to impart consistent row alternation colors to the form lists.
                    Should probably be chnaged to a system that is more straightforward. -->
                <xsl:choose>
                    <xsl:when test="(count(../../..//dri:item) - count(../../..//dri:list[@type='form'])) mod 2 = 0">
                        <!--<xsl:if test="count(../dri:item) > 3">-->
                            <xsl:if test="(count(preceding-sibling::dri:item | ../../preceding-sibling::dri:item/dri:list[@type='form']/dri:item) mod 2 = 0)">even </xsl:if>
                            <xsl:if test="(count(preceding-sibling::dri:item | ../../preceding-sibling::dri:item/dri:list[@type='form']/dri:item) mod 2 = 1)">odd </xsl:if>

                    </xsl:when>
                    <xsl:when test="(count(../../..//dri:item) - count(../../..//dri:list[@type='form'])) mod 2 = 1">
                        <!--<xsl:if test="count(../dri:item) > 3">-->
                            <xsl:if test="(count(preceding-sibling::dri:item | ../../preceding-sibling::dri:item/dri:list[@type='form']/dri:item) mod 2 = 1)">even </xsl:if>
                            <xsl:if test="(count(preceding-sibling::dri:item | ../../preceding-sibling::dri:item/dri:list[@type='form']/dri:item) mod 2 = 0)">odd </xsl:if>

                    </xsl:when>
                </xsl:choose>
                <!--
                <xsl:if test="position()=last() and dri:field[@type='button'] and not(dri:field[not(@type='button')])">last</xsl:if>
                    -->
               </xsl:with-param>
            </xsl:call-template>

            <xsl:call-template name="pick-label"/>

            <xsl:choose>
                <xsl:when test="dri:field[@type='composite']">
                    <xsl:apply-templates mode="formComposite"/>
                </xsl:when>
                <xsl:otherwise>
                    <div class="ds-form-content">
                        <xsl:apply-templates />
                        <!-- special name used in submission UI review page -->
                        <xsl:if test="@n = 'submit-review-field-with-authority'">
                          <xsl:call-template name="authorityConfidenceIcon">
                            <xsl:with-param name="confidence" select="substring-after(./@rend, 'cf-')"/>
                          </xsl:call-template>
                        </xsl:if>
                    </div>
                </xsl:otherwise>
            </xsl:choose>
        </li>
    </xsl:template>

    <xsl:template name="pick-label">
        <xsl:choose>
            <xsl:when test="dri:field/dri:label">
                <label class="ds-form-label">
                        <xsl:choose>
                                <xsl:when test="./dri:field/@id">
                                        <xsl:attribute name="for">
                                                <xsl:value-of select="translate(./dri:field/@id,'.','_')"/>
                                        </xsl:attribute>
                                </xsl:when>
                                <xsl:otherwise></xsl:otherwise>
                        </xsl:choose>
                    <xsl:apply-templates select="dri:field/dri:label" mode="formComposite"/>
                    <xsl:text>:</xsl:text>
                </label>
            </xsl:when>
            <xsl:when test="string-length(string(preceding-sibling::*[1][local-name()='label'])) > 0">
                <xsl:choose>
                        <xsl:when test="./dri:field/@id">
                                <label>
                                        <xsl:apply-templates select="preceding-sibling::*[1][local-name()='label']"/>
                                    <xsl:text>:</xsl:text>
                                </label>
                        </xsl:when>
                        <xsl:otherwise>
                                <span>
                                        <xsl:apply-templates select="preceding-sibling::*[1][local-name()='label']"/>
                                    <xsl:text>:</xsl:text>
                                </span>
                        </xsl:otherwise>
                </xsl:choose>

            </xsl:when>
            <xsl:when test="dri:field">
                <xsl:choose>
                        <xsl:when test="preceding-sibling::*[1][local-name()='label']">
                                <label class="ds-form-label">
                                        <xsl:choose>
                                                <xsl:when test="./dri:field/@id">
                                                        <xsl:attribute name="for">
                                                                <xsl:value-of select="translate(./dri:field/@id,'.','_')"/>
                                                        </xsl:attribute>
                                                </xsl:when>
                                                <xsl:otherwise></xsl:otherwise>
                                        </xsl:choose>
                                    <xsl:apply-templates select="preceding-sibling::*[1][local-name()='label']"/>&#160;
                                </label>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:apply-templates select="preceding-sibling::*[1][local-name()='label']"/>&#160;
                            </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <!-- If the label is empty and the item contains no field, omit the label. This is to
                    make the text inside the item (since what else but text can be there?) stretch across
                    both columns of the list. -->
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="dri:list[@type='form']/dri:label" priority="3">
                <xsl:attribute name="class">
                <xsl:text>ds-form-label</xsl:text>
                <xsl:if test="@rend">
                    <xsl:text> </xsl:text>
                    <xsl:value-of select="@rend"/>
                </xsl:if>
        </xsl:attribute>
        <xsl:choose>
                <xsl:when test="following-sibling::dri:item[1]/dri:field/@id">
                        <xsl:attribute name="for">
                                <xsl:value-of select="translate(following-sibling::dri:item[1]/dri:field/@id,'.','_')" />
                        </xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                </xsl:otherwise>
        </xsl:choose>
        <xsl:apply-templates />
    </xsl:template>


    <xsl:template match="dri:field/dri:label" mode="formComposite">
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="dri:list[@type='form']/dri:head" priority="5">
        <legend>
                <xsl:apply-templates />
        </legend>
    </xsl:template>
    
    <!-- NON-instance composite fields (i.e. not repeatable) -->
    <xsl:template match="dri:field[@type='composite']" mode="formComposite">
        <div class="ds-form-content">
            <xsl:variable name="confidenceIndicatorID" select="concat(translate(@id,'.','_'),'_confidence_indicator')"/>
            <xsl:apply-templates select="dri:field" mode="compositeComponent"/>
            <xsl:choose>
              <xsl:when test="dri:params/@choicesPresentation = 'suggest'">
                <xsl:message terminate="yes">
                  <xsl:text>ERROR: Input field with "suggest" (autocomplete) choice behavior is not implemented for Composite (e.g. "name") fields.</xsl:text>
                </xsl:message>
              </xsl:when>
              <!-- lookup popup includes its own Add button if necessary. -->
              <xsl:when test="dri:params/@choicesPresentation = 'lookup'">
                <xsl:call-template name="addLookupButton">
                  <xsl:with-param name="isName" select="'true'"/>
                  <xsl:with-param name="confIndicator" select="$confidenceIndicatorID"/>
                </xsl:call-template>
              </xsl:when>
            </xsl:choose>
            <xsl:if test="dri:params/@authorityControlled">
              <xsl:variable name="confValue" select="dri:field/dri:value[@type='authority'][1]/@confidence"/>
              <xsl:call-template name="authorityConfidenceIcon">
                <xsl:with-param name="confidence" select="$confValue"/>
                <xsl:with-param name="id" select="$confidenceIndicatorID"/>
              </xsl:call-template>
              <xsl:call-template name="authorityInputFields">
                <xsl:with-param name="name" select="@n"/>
                <xsl:with-param name="authValue" select="dri:field/dri:value[@type='authority'][1]/text()"/>
                <xsl:with-param name="confValue" select="$confValue"/>
              </xsl:call-template>
            </xsl:if>
            <div class="spacer">&#160;</div>
            <xsl:apply-templates select="dri:field/dri:error" mode="compositeComponent"/>
            <xsl:apply-templates select="dri:error" mode="compositeComponent"/>
            <xsl:apply-templates select="dri:help" mode="compositeComponent"/>
        </div>
    </xsl:template>
    
    
        
        
        
    <!-- Next, special handling is performed for lists under the options tag, making them into option sets to
        reflect groups of similar options (like browsing, for example). -->
    
    <!-- The template that applies to lists directly under the options tag that have other lists underneath
        them. Each list underneath the matched one becomes an option-set and is handled by the appropriate
        list templates. -->
    <xsl:template match="dri:options/dri:list[dri:list]" priority="4">
        <xsl:apply-templates select="dri:head"/>
        <div>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-option-set</xsl:with-param>
            </xsl:call-template>
            <ul class="ds-options-list">
                <xsl:apply-templates select="*[not(name()='head')]" mode="nested"/>
            </ul>
        </div>
    </xsl:template>
    
    <!-- Special case for nested options lists -->
    <xsl:template match="dri:options/dri:list/dri:list" priority="3" mode="nested">
        <li>
            <xsl:apply-templates select="dri:head" mode="nested"/>
            <ul class="ds-simple-list">
                <xsl:apply-templates select="dri:item" mode="nested"/>
            </ul>
        </li>
    </xsl:template>
    
    <xsl:template match="dri:options/dri:list" priority="3">
        <xsl:apply-templates select="dri:head"/>
        <div>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-option-set</xsl:with-param>
            </xsl:call-template>
            <ul class="ds-simple-list">
                <xsl:apply-templates select="dri:item" mode="nested"/>
            </ul>
        </div>
    </xsl:template>
    
    <!-- Quick patch to remove empty lists from options -->
    <xsl:template match="dri:options//dri:list[count(child::*)=0]" priority="5" mode="nested">
    </xsl:template>
    <xsl:template match="dri:options//dri:list[count(child::*)=0]" priority="5">
    </xsl:template>
    
    
    <xsl:template match="dri:options/dri:list/dri:head" priority="3">
        <h3>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-option-set-head</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </h3>
    </xsl:template>
    
    <!-- Items inside option lists are excluded from the "orphan roundup" mechanism -->
    <xsl:template match="dri:options//dri:item" mode="nested" priority="3">
        <li>
            <xsl:apply-templates />
        </li>
    </xsl:template>
    
    
    
    <!-- Finally, the following templates match list types not mentioned above. They work for lists of type
        'simple' as well as any unknown list types. -->
    <xsl:template match="dri:list" priority="1">
        <xsl:apply-templates select="dri:head"/>
        <ul>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-simple-list</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="*[not(name()='head')]" mode="nested"/>
        </ul>
    </xsl:template>


    <!-- Generic label handling: simply place the text of the element followed by a period and space. -->
    <xsl:template match="dri:label" priority="1" mode="nested">
        <xsl:copy-of select="./node()"/>
    </xsl:template>

    <!-- Generic item handling for cases where nothing special needs to be done -->
    <xsl:template match="dri:item" mode="nested">
        <li>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-simple-list-item</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </li>
    </xsl:template>

    <xsl:template match="dri:list/dri:list" priority="1" mode="nested">
        <li>
            <xsl:apply-templates select="."/>
        </li>
    </xsl:template>


    
    <!-- From here on out come the templates for supporting elements that are contained within structural
        ones. These include head (in all its myriad forms), rich text container elements (like hi and figure),
        as well as the field tag and its related elements. The head elements are done first. -->
    
    <!-- The first (and most complex) case of the header tag is the one used for divisions. Since divisions can
        nest freely, their headers should reflect that. Thus, the type of HTML h tag produced depends on how
        many divisions the header tag is nested inside of. -->
    <!-- The font-sizing variable is the result of a linear function applied to the character count of the heading text -->
    <xsl:template match="dri:div/dri:head" priority="3">
        <xsl:variable name="head_count" select="count(ancestor::dri:div)"/>
        <!-- with the help of the font-sizing variable, the font-size of our header text is made continuously variable based on the character count -->
        <xsl:variable name="font-sizing" select="365 - $head_count * 80 - string-length(current())"></xsl:variable>
        <xsl:element name="h{$head_count}">
            <!-- in case the chosen size is less than 120%, don't let it go below. Shrinking stops at 120% -->
            <xsl:choose>
                <xsl:when test="$font-sizing &lt; 120">
                    <xsl:attribute name="style">font-size: 120%;</xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="style">font-size: <xsl:value-of select="$font-sizing"/>%;</xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-div-head</xsl:with-param>
            </xsl:call-template>

            <xsl:choose>
                <xsl:when test="string-length(./node()) &lt; 1">
                    <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:element>
    </xsl:template>
    
    <!-- The second case is the header on tables, which always creates an HTML h3 element -->
    <xsl:template match="dri:table/dri:head" priority="2">
        <h3>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-table-head</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </h3>
    </xsl:template>
    
    <!-- The third case is the header on lists, which creates an HTML h3 element for top level lists and
        and h4 elements for all sublists. -->
    <xsl:template match="dri:list/dri:head" priority="2" mode="nested">
        <h3>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-list-head</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </h3>
    </xsl:template>
    
    <xsl:template match="dri:list/dri:list/dri:head" priority="3" mode="nested">
        <h4>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-sublist-head</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </h4>
    </xsl:template>
    
    <!-- The fourth case is the header on referenceSets, to be discussed below, which creates an HTML h2 element
        for all cases. The reason for this simplistic approach has to do with referenceSets being handled
        differently in many cases, making it difficult to treat them as either divs (with scaling headers) or
        lists (with static ones). In this case, the simplest solution was chosen, although it is subject to
        change in the future. -->
    <xsl:template match="dri:referenceSet/dri:head" priority="2">
        <h3>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-list-head</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </h3>
    </xsl:template>
    
    <!-- Finally, the generic header element template, given the lowest priority, is there for cases not
        covered above. It assumes nothing about the parent element and simply generates an HTML h3 tag -->
    <xsl:template match="dri:head" priority="1">
        <h3>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-head</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </h3>
    </xsl:template>
    
    
    
    
    <!-- Next come the components of rich text containers, namely: hi, xref, figure and, in case of interactive
        divs, field. All these can mix freely with text as well as contain text of their own. The templates for
        the first three elements are fairly straightforward, as they simply create HTML span, a, and img tags,
        respectively. -->
    
    <xsl:template match="dri:hi">
        <span>
            <xsl:attribute name="class">emphasis</xsl:attribute>
            <xsl:if test="@rend">
                <xsl:attribute name="class"><xsl:value-of select="@rend"/></xsl:attribute>
            </xsl:if>
            <xsl:apply-templates />
        </span>
    </xsl:template>
    
    <xsl:template match="dri:xref">
        <a>
            <xsl:if test="@target">
                <xsl:attribute name="href"><xsl:value-of select="@target"/></xsl:attribute>
            </xsl:if>
            
            <xsl:if test="@rend">
                <xsl:attribute name="class"><xsl:value-of select="@rend"/></xsl:attribute>
            </xsl:if>
            
            <xsl:if test="@n">
                <xsl:attribute name="name"><xsl:value-of select="@n"/></xsl:attribute>
            </xsl:if>

            <xsl:if test="@onclick">
                <xsl:attribute name="onclick"><xsl:value-of select="@onclick"/></xsl:attribute>
            </xsl:if>

            <xsl:apply-templates />
        </a>
    </xsl:template>
    
    <xsl:template match="dri:figure">
        <xsl:if test="@target">
            <a>
                <xsl:attribute name="href"><xsl:value-of select="@target"/></xsl:attribute>
                <xsl:if test="@title">
                	<xsl:attribute name="title"><xsl:value-of select="@title"/></xsl:attribute>
                </xsl:if>
                <xsl:if test="@rend">
                	<xsl:attribute name="class"><xsl:value-of select="@rend"/></xsl:attribute>
                </xsl:if>
                <img>
                    <xsl:attribute name="src"><xsl:value-of select="@source"/></xsl:attribute>
                    <xsl:attribute name="alt"><xsl:apply-templates /></xsl:attribute>
                <xsl:attribute name="border"><xsl:text>none</xsl:text></xsl:attribute>
                </img>
            </a>
        </xsl:if>
        <xsl:if test="not(@target)">
            <img>
                <xsl:attribute name="src"><xsl:value-of select="@source"/></xsl:attribute>
                <xsl:attribute name="alt"><xsl:apply-templates /></xsl:attribute>
            </img>
        </xsl:if>
    </xsl:template>
    
    
    
    
    
    
    
    
    
    <!-- The handling of the special case of instanced composite fields under "form" lists -->
    <xsl:template match="dri:field[@type='composite'][dri:field/dri:instance | dri:params/@operations]" mode="formComposite" priority="2">
        <xsl:variable name="confidenceIndicatorID" select="concat(translate(@id,'.','_'),'_confidence_indicator')"/>
        <div class="ds-form-content">
            <xsl:apply-templates select="dri:field" mode="compositeComponent"/>
            <xsl:if test="contains(dri:params/@operations,'add')">
                <!-- Add buttons should be named "submit_[field]_add" so that we can ignore errors from required fields when simply adding new values-->
               <input type="submit" value="Add" name="{concat('submit_',@n,'_add')}" class="ds-button-field ds-add-button">
                  <!-- Make invisible if we have choice-lookup operation that provides its own Add. -->
                  <xsl:if test="dri:params/@choicesPresentation = 'lookup'">
                    <xsl:attribute name="style">
                      <xsl:text>display:none;</xsl:text>
                    </xsl:attribute>
            </xsl:if>
               </input>
            </xsl:if>
            <!-- insert choice mechansim and/or Add button here -->
            <xsl:choose>
              <xsl:when test="dri:params/@choicesPresentation = 'suggest'">
                <xsl:message terminate="yes">
                  <xsl:text>ERROR: Input field with "suggest" (autocomplete) choice behavior is not implemented for Composite (e.g. "name") fields.</xsl:text>
                </xsl:message>
              </xsl:when>
              <!-- lookup popup includes its own Add button if necessary. -->
              <xsl:when test="dri:params/@choicesPresentation = 'lookup'">
                <xsl:call-template name="addLookupButton">
                  <xsl:with-param name="isName" select="'true'"/>
                  <xsl:with-param name="confIndicator" select="$confidenceIndicatorID"/>
                </xsl:call-template>
              </xsl:when>
            </xsl:choose>
            <!-- place to store authority value -->
            <xsl:if test="dri:params/@authorityControlled">
              <xsl:call-template name="authorityConfidenceIcon">
                <xsl:with-param name="confidence" select="dri:value[@type='authority']/@confidence"/>
                <xsl:with-param name="id" select="$confidenceIndicatorID"/>
              </xsl:call-template>
              <xsl:call-template name="authorityInputFields">
                <xsl:with-param name="name" select="@n"/>
                <xsl:with-param name="authValue" select="dri:value[@type='authority']/text()"/>
                <xsl:with-param name="confValue" select="dri:value[@type='authority']/@confidence"/>
              </xsl:call-template>
            </xsl:if>
            <div class="spacer">&#160;</div>
            <xsl:apply-templates select="dri:field/dri:error" mode="compositeComponent"/>
            <xsl:apply-templates select="dri:error" mode="compositeComponent"/>
            <xsl:apply-templates select="dri:help" mode="compositeComponent"/>
            <xsl:if test="dri:instance or dri:field/dri:instance">
                <div class="ds-previous-values">
                    <xsl:call-template name="fieldIterator">
                        <xsl:with-param name="position">1</xsl:with-param>
                    </xsl:call-template>
                    <xsl:if test="contains(dri:params/@operations,'delete') and (dri:instance or dri:field/dri:instance)">
                        <!-- Delete buttons should be named "submit_[field]_delete" so that we can ignore errors from required fields when simply removing values-->
                        <input type="submit" value="Remove selected" name="{concat('submit_',@n,'_delete')}" class="ds-button-field ds-delete-button" />
                    </xsl:if>
                    <xsl:for-each select="dri:field">
                        <xsl:apply-templates select="dri:instance" mode="hiddenInterpreter"/>
                    </xsl:for-each>
                </div>
            </xsl:if>
        </div>
    </xsl:template>
    
    
    
    
    <!-- TODO: The field section works but needs a lot of scrubbing. I would say a third of the
        templates involved are either bloated or superfluous. -->
           
    
    <!-- Things I know:
        1. I can tell a field is multivalued if it has instances in it
        2. I can't really do that for composites, although I can check its
            component fields for condition 1 above.
        3. Fields can also be inside "form" lists, which is its own unique condition
    -->
        
    <!-- Fieldset (instanced) field stuff, in the case of non-composites -->
    <xsl:template match="dri:field[dri:field/dri:instance | dri:params/@operations]" priority="2">
        <!-- Create the first field normally -->
        <xsl:apply-templates select="." mode="normalField"/>
        <!-- Follow it up with an ADD button if the add operation is specified. This allows
            entering more than one value for this field. -->
        <xsl:if test="contains(dri:params/@operations,'add')">
            <!-- Add buttons should be named "submit_[field]_add" so that we can ignore errors from required fields when simply adding new values-->
            <input type="submit" value="Add" name="{concat('submit_',@n,'_add')}" class="ds-button-field ds-add-button">
              <!-- Make invisible if we have choice-lookup popup that provides its own Add. -->
              <xsl:if test="dri:params/@choicesPresentation = 'lookup'">
                <xsl:attribute name="style">
                  <xsl:text>display:none;</xsl:text>
                </xsl:attribute>
        </xsl:if>
           </input>
        </xsl:if>
        <br/>
        <xsl:apply-templates select="dri:help" mode="help"/>
        <xsl:apply-templates select="dri:error" mode="error"/>
        <xsl:if test="dri:instance">
            <div class="ds-previous-values">
                <!-- Iterate over the dri:instance elements contained in this field. The instances contain
                    stored values as either "interpreted", "raw", or "default" values. -->
                <xsl:call-template name="simpleFieldIterator">
                    <xsl:with-param name="position">1</xsl:with-param>
                </xsl:call-template>
                <!-- Conclude with a DELETE button if the delete operation is specified. This allows
                    removing one or more values stored for this field. -->
                <xsl:if test="contains(dri:params/@operations,'delete') and dri:instance">
                    <!-- Delete buttons should be named "submit_[field]_delete" so that we can ignore errors from required fields when simply removing values-->
                    <input type="submit" value="Remove selected" name="{concat('submit_',@n,'_delete')}" class="ds-button-field ds-delete-button" />
                </xsl:if>
                <!-- Behind the scenes, add hidden fields for every instance set. This is to make sure that
                    the form still submits the information in those instances, even though they are no
                    longer encoded as HTML fields. The DRI Reference should contain the exact attributes
                    the hidden fields should have in order for this to work properly. -->
                <xsl:apply-templates select="dri:instance" mode="hiddenInterpreter"/>
            </div>
        </xsl:if>
    </xsl:template>
    
    <!-- The iterator is a recursive function that creates a checkbox (to be used in deletion) for
        each value instance and interprets the value inside. It also creates a hidden field from the
        raw value contained in the instance. -->
    <xsl:template name="simpleFieldIterator">
        <xsl:param name="position"/>
        <xsl:if test="dri:instance[position()=$position]">
            <input type="checkbox" value="{concat(@n,'_',$position)}" name="{concat(@n,'_selected')}"/>
            <xsl:apply-templates select="dri:instance[position()=$position]" mode="interpreted"/>

            <!-- look for authority value in instance. -->
            <xsl:if test="dri:instance[position()=$position]/dri:value[@type='authority']">
              <xsl:call-template name="authorityConfidenceIcon">
                <xsl:with-param name="confidence" select="dri:instance[position()=$position]/dri:value[@type='authority']/@confidence"/>
              </xsl:call-template>
            </xsl:if>
            <br/>
            <xsl:call-template name="simpleFieldIterator">
                <xsl:with-param name="position"><xsl:value-of select="$position + 1"/></xsl:with-param>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>
    
    <!-- Authority: added fields for auth values as well. -->
    <!-- Common case: use the raw value of the instance to create the hidden field -->
    <xsl:template match="dri:instance" mode="hiddenInterpreter">
        <input type="hidden">
            <xsl:attribute name="name"><xsl:value-of select="concat(../@n,'_',position())"/></xsl:attribute>
            <xsl:attribute name="value">
                <xsl:value-of select="dri:value[@type='raw']"/>
            </xsl:attribute>
        </input>
        <!-- XXX do we want confidence icon here?? -->
        <xsl:if test="dri:value[@type='authority']">
          <xsl:call-template name="authorityInputFields">
            <xsl:with-param name="name" select="../@n"/>
            <xsl:with-param name="position" select="position()"/>
            <xsl:with-param name="authValue" select="dri:value[@type='authority']/text()"/>
            <xsl:with-param name="confValue" select="dri:value[@type='authority']/@confidence"/>
          </xsl:call-template>
        </xsl:if>
    </xsl:template>
    
    <!-- Select box case: use the selected options contained in the instance to create the hidden fields -->
    <xsl:template match="dri:field[@type='select']/dri:instance" mode="hiddenInterpreter">
        <xsl:variable name="position" select="position()"/>
        <xsl:for-each select="dri:value[@type='option']">
            <input type="hidden">
                <xsl:attribute name="name">
                    <xsl:value-of select="concat(../../@n,'_',$position)"/>
                </xsl:attribute>
                <!-- Since the dri:option and dri:values inside a select field are related by the return
                    value, encoded in @returnValue and @option attributes respectively, the option
                    attribute can be used directly instead of being resolved to the the correct option -->
                <xsl:attribute name="value">
                    <!--<xsl:value-of select="../../dri:option[@returnValue = current()/@option]"/>-->
                    <xsl:value-of select="@option"/>
                </xsl:attribute>
            </input>
        </xsl:for-each>
    </xsl:template>
      
    
    
    <!-- Composite instanced field stuff -->
    <!-- It is also the one that receives the special error and help handling -->
    <xsl:template match="dri:field[@type='composite'][dri:field/dri:instance | dri:params/@operations]" priority="3">
        <!-- First is special, so first we grab all the values from the child fields.
            We do this by applying normal templates to the field, which should ignore instances. -->
        <span class="ds-composite-field">
            <xsl:apply-templates select="dri:field" mode="compositeComponent"/>
        </span>
        <xsl:apply-templates select="dri:field/dri:error" mode="compositeComponent"/>
        <xsl:apply-templates select="dri:error" mode="compositeComponent"/>
        <xsl:apply-templates select="dri:help" mode="compositeComponent"/>
        <!-- Insert choice mechanism here.
             Follow it up with an ADD button if the add operation is specified. This allows
            entering more than one value for this field. -->

        <xsl:if test="contains(dri:params/@operations,'add')">
            <!-- Add buttons should be named "submit_[field]_add" so that we can ignore errors from required fields when simply adding new values-->
           <input type="submit" value="Add" name="{concat('submit_',@n,'_add')}" class="ds-button-field ds-add-button">
              <!-- Make invisible if we have choice-lookup popup that provides its own Add. -->
              <xsl:if test="dri:params/@choicesPresentation = 'lookup'">
                <xsl:attribute name="style">
                  <xsl:text>display:none;</xsl:text>
                </xsl:attribute>
        </xsl:if>
           </input>
        </xsl:if>

        <xsl:variable name="confidenceIndicatorID" select="concat(translate(@id,'.','_'),'_confidence_indicator')"/>
        <xsl:if test="dri:params/@authorityControlled">
          <!-- XXX note that this is wrong and won't get any authority values, but
             - for instanced inputs the entry box starts out empty anyway.
            -->
          <xsl:call-template name="authorityConfidenceIcon">
            <xsl:with-param name="confidence" select="dri:value[@type='authority']/@confidence"/>
            <xsl:with-param name="id" select="$confidenceIndicatorID"/>
          </xsl:call-template>
          <xsl:call-template name="authorityInputFields">
            <xsl:with-param name="name" select="@n"/>
            <xsl:with-param name="id" select="@id"/>
            <xsl:with-param name="authValue" select="dri:value[@type='authority']/text()"/>
            <xsl:with-param name="confValue" select="dri:value[@type='authority']/@confidence"/>
          </xsl:call-template>
        </xsl:if>
        <xsl:choose>
          <xsl:when test="dri:params/@choicesPresentation = 'suggest'">
            <xsl:call-template name="addAuthorityAutocomplete">
              <xsl:with-param name="confidenceIndicatorID" select="$confidenceIndicatorID"/>
            </xsl:call-template>
          </xsl:when>
          <!-- lookup popup includes its own Add button if necessary. -->
          <!-- XXX does this need a Confidence Icon? -->
          <xsl:when test="dri:params/@choicesPresentation = 'lookup'">
            <xsl:call-template name="addLookupButton">
              <xsl:with-param name="isName" select="'true'"/>
              <xsl:with-param name="confIndicator" select="$confidenceIndicatorID"/>
            </xsl:call-template>
          </xsl:when>
        </xsl:choose>
        <br/>
        <xsl:if test="dri:instance or dri:field/dri:instance">
            <div class="ds-previous-values">
                <xsl:call-template name="fieldIterator">
                    <xsl:with-param name="position">1</xsl:with-param>
                </xsl:call-template>
                <!-- Conclude with a DELETE button if the delete operation is specified. This allows
                    removing one or more values stored for this field. -->
                <xsl:if test="contains(dri:params/@operations,'delete') and (dri:instance or dri:field/dri:instance)">
                    <!-- Delete buttons should be named "submit_[field]_delete" so that we can ignore errors from required fields when simply removing values-->
                    <input type="submit" value="Remove selected" name="{concat('submit_',@n,'_delete')}" class="ds-button-field ds-delete-button" />
                </xsl:if>
                <xsl:for-each select="dri:field">
                    <xsl:apply-templates select="dri:instance" mode="hiddenInterpreter"/>
                </xsl:for-each>
            </div>
        </xsl:if>
    </xsl:template>
        
    <!-- The iterator is a recursive function that creates a checkbox (to be used in deletion) for
        each value instance and interprets the value inside. It also creates a hidden field from the
        raw value contained in the instance.
        
         What makes it different from the simpleFieldIterator is that it works with a composite field's
        components rather than a single field, which requires it to consider several sets of instances. -->
    <xsl:template name="fieldIterator">
        <xsl:param name="position"/>
        <!-- add authority value for this instance -->
        <xsl:if test="dri:instance[position()=$position]/dri:value[@type='authority']">
          <xsl:call-template name="authorityInputFields">
            <xsl:with-param name="name" select="@n"/>
            <xsl:with-param name="position" select="$position"/>
            <xsl:with-param name="authValue" select="dri:instance[position()=$position]/dri:value[@type='authority']/text()"/>
            <xsl:with-param name="confValue" select="dri:instance[position()=$position]/dri:value[@type='authority']/@confidence"/>
          </xsl:call-template>
        </xsl:if>
        <xsl:choose>
            <!-- First check to see if the composite itself has a non-empty instance value in that
                position. In that case there is no need to go into the individual fields. -->
            <xsl:when test="count(dri:instance[position()=$position]/dri:value[@type != 'authority'])">
                <input type="checkbox" value="{concat(@n,'_',$position)}" name="{concat(@n,'_selected')}"/>
                <xsl:apply-templates select="dri:instance[position()=$position]" mode="interpreted"/>
                <xsl:call-template name="authorityConfidenceIcon">
                  <xsl:with-param name="confidence" select="dri:instance[position()=$position]/dri:value[@type='authority']/@confidence"/>
                </xsl:call-template>
                <br/>
                <xsl:call-template name="fieldIterator">
                    <xsl:with-param name="position"><xsl:value-of select="$position + 1"/></xsl:with-param>
                </xsl:call-template>
            </xsl:when>
            <!-- Otherwise, build the string from the component fields -->
            <xsl:when test="dri:field/dri:instance[position()=$position]">
                <input type="checkbox" value="{concat(@n,'_',$position)}" name="{concat(@n,'_selected')}"/>
                <xsl:apply-templates select="dri:field" mode="compositeField">
                    <xsl:with-param name="position" select="$position"/>
                </xsl:apply-templates>
                <br/>
                <xsl:call-template name="fieldIterator">
                    <xsl:with-param name="position"><xsl:value-of select="$position + 1"/></xsl:with-param>
                </xsl:call-template>
            </xsl:when>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="dri:field[@type='text' or @type='textarea']" mode="compositeField">
        <xsl:param name="position">1</xsl:param>
        <xsl:if test="not(position()=1)">
            <xsl:text>, </xsl:text>
        </xsl:if>
        <!--<input type="hidden" name="{concat(@n,'_',$position)}" value="{dri:instance[position()=$position]/dri:value[@type='raw']}"/>-->
        <xsl:choose>
            <xsl:when test="dri:instance[position()=$position]/dri:value[@type='interpreted']">
                <span class="ds-interpreted-field"><xsl:apply-templates select="dri:instance[position()=$position]/dri:value[@type='interpreted']" mode="interpreted"/></span>
            </xsl:when>
            <xsl:when test="dri:instance[position()=$position]/dri:value[@type='raw']">
                <span class="ds-interpreted-field"><xsl:apply-templates select="dri:instance[position()=$position]/dri:value[@type='raw']" mode="interpreted"/></span>
            </xsl:when>
            <xsl:when test="dri:instance[position()=$position]/dri:value[@type='default']">
                <span class="ds-interpreted-field"><xsl:apply-templates select="dri:instance[position()=$position]/dri:value[@type='default']" mode="interpreted"/></span>
            </xsl:when>
            <xsl:otherwise>
                <span class="ds-interpreted-field">No value submitted.</span>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    
    <xsl:template match="dri:field[@type='select']" mode="compositeField">
        <xsl:param name="position">1</xsl:param>
        <xsl:if test="not(position()=1)">
            <xsl:text>, </xsl:text>
        </xsl:if>
        <xsl:for-each select="dri:instance[position()=$position]/dri:value[@type='option']">
            <!--<input type="hidden" name="{concat(@n,'_',$position)}" value="{../../dri:option[@returnValue = current()/@option]}"/>-->
        </xsl:for-each>
        <xsl:choose>
            <xsl:when test="dri:instance[position()=$position]/dri:value[@type='interpreted']">
                <span class="ds-interpreted-field"><xsl:apply-templates select="dri:instance[position()=$position]/dri:value[@type='interpreted']" mode="interpreted"/></span>
            </xsl:when>
            <xsl:when test="dri:instance[position()=$position]/dri:value[@type='option']">
                <span class="ds-interpreted-field">
                    <xsl:for-each select="dri:instance[position()=$position]/dri:value[@type='option']">
                        <xsl:if test="position()=1">
                            <xsl:text>(</xsl:text>
                        </xsl:if>
                        <xsl:value-of select="../../dri:option[@returnValue = current()/@option]"/>
                        <xsl:if test="position()=last()">
                            <xsl:text>)</xsl:text>
                        </xsl:if>
                        <xsl:if test="not(position()=last())">
                            <xsl:text>, </xsl:text>
                        </xsl:if>
                    </xsl:for-each>
                </span>
            </xsl:when>
            <xsl:otherwise>
                <span class="ds-interpreted-field">No value submitted.</span>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!-- TODO: make this work? Maybe checkboxes and radio buttons should not be instantiated... -->
    <xsl:template match="dri:field[@type='checkbox' or @type='radio']" mode="compositeField">
        <xsl:param name="position">1</xsl:param>
        <xsl:if test="not(position()=1)">
            <xsl:text>, </xsl:text>
        </xsl:if>
        <span class="ds-interpreted-field">Checkbox</span>
    </xsl:template>
    
    
    
    
    
    
    
    
    
    
    <xsl:template match="dri:field[@type='select']/dri:instance" mode="interpreted">
        <span class="ds-interpreted-field">
            <xsl:for-each select="dri:value[@type='option']">
                <!--<input type="hidden" name="{concat(../@n,'_',position())}" value="{../../dri:option[@returnValue = current()/@option]}"/>-->
                <xsl:if test="position()=1">
                    <xsl:text>(</xsl:text>
                </xsl:if>
                <xsl:value-of select="../../dri:option[@returnValue = current()/@option]"/>
                <xsl:if test="position()=last()">
                    <xsl:text>)</xsl:text>
                </xsl:if>
                <xsl:if test="not(position()=last())">
                    <xsl:text>, </xsl:text>
                </xsl:if>
            </xsl:for-each>
        </span>
    </xsl:template>
    
    
    <xsl:template match="dri:instance" mode="interpreted">
        <!--<input type="hidden" name="{concat(../@n,'_',position())}" value="dri:value[@type='raw']"/>-->
        <xsl:choose>
            <xsl:when test="dri:value[@type='interpreted']">
                <span class="ds-interpreted-field"><xsl:apply-templates select="dri:value[@type='interpreted']" mode="interpreted"/></span>
            </xsl:when>
            <xsl:when test="dri:value[@type='raw']">
                <span class="ds-interpreted-field"><xsl:apply-templates select="dri:value[@type='raw']" mode="interpreted"/></span>
            </xsl:when>
            <xsl:when test="dri:value[@type='default']">
                <span class="ds-interpreted-field"><xsl:apply-templates select="dri:value[@type='default']" mode="interpreted"/></span>
            </xsl:when>
            <xsl:otherwise>
                <span class="ds-interpreted-field">No value submitted.</span>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
        
    
    
    
    <xsl:template match="dri:value" mode="interpreted">
        <xsl:apply-templates />
    </xsl:template>
    
    <!--
    <xsl:template match="dri:field">
    
        Possible child elements:
        params(one), help(zero or one), error(zero or one), value(any), field(one or more – only with the composite type)
        
        Possible attributes:
        @n, @id, @rend
        @disabled
        @required
        @type =
            button: A button input control that when activated by the user will submit the form, including all the fields, back to the server for processing.
            checkbox: A boolean input control which may be toggled by the user. A checkbox may have several fields which share the same name and each of those fields may be toggled independently. This is distinct from a radio button where only one field may be toggled.
            file: An input control that allows the user to select files to be submitted with the form. Note that a form which uses a file field must use the multipart method.
            hidden: An input control that is not rendered on the screen and hidden from the user.
            password: A single-line text input control where the input text is rendered in such a way as to hide the characters from the user.
            radio:  A boolean input control which may be toggled by the user. Multiple radio button fields may share the same name. When this occurts, only one field may be selected to be true. This is distinct from a checkbox where multiple fields may be toggled.
            select: A menu input control which allows the user to select from a list of available options.
            text: A single-line text input control.
            textarea: A multi-line text input control.
            composite: A composite input control combines several input controls into a single field. The only fields that may be combined together are: checkbox, password, select, text, and textarea. When fields are combined together they can possess multiple combined values.
    </xsl:template>
        -->
    
    
    
    <!-- The handling of component fields, that is fields that are part of a composite field type -->
    <xsl:template match="dri:field" mode="compositeComponent">
        <xsl:choose>
                <xsl:when test="@type = 'checkbox'  or @type='radio'">
                    <xsl:apply-templates select="." mode="normalField"/>
                    <xsl:if test="dri:label">
                        <br/>
                        <xsl:apply-templates select="dri:label" mode="compositeComponent"/>
                    </xsl:if>
                </xsl:when>
                <xsl:otherwise>
                        <label class="ds-composite-component">
                            <xsl:if test="position()=last()">
                                <xsl:attribute name="class">ds-composite-component last</xsl:attribute>
                            </xsl:if>
                            <xsl:apply-templates select="." mode="normalField"/>
                            <xsl:if test="dri:label">
                                <br/>
                                <xsl:apply-templates select="dri:label" mode="compositeComponent"/>
                            </xsl:if>
                        </label>
                </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="dri:error" mode="compositeComponent">
        <xsl:apply-templates select="." mode="error"/>
    </xsl:template>
    
    <xsl:template match="dri:help" mode="compositeComponent">
        <span class="composite-help"><xsl:apply-templates /></span>
    </xsl:template>
    
    
        
    <!-- The handling of the field element is more complex. At the moment, the handling of input fields in the
        DRI schema is very similar to HTML, utilizing the same controlled vocabulary in most cases. This makes
        converting DRI fields to HTML inputs a straightforward, if a bit verbose, task. We are currently
        looking at other ways of encoding forms, so this may change in the future. -->
    <!-- The simple field case... not part of a complex field and does not contain instance values -->
    <xsl:template match="dri:field">
        <xsl:apply-templates select="." mode="normalField"/>
        <xsl:if test="not(@type='composite') and ancestor::dri:list[@type='form']">
            <!--
            <xsl:if test="not(@type='checkbox') and not(@type='radio') and not(@type='button')">
                <br/>
            </xsl:if>
            -->
            <xsl:apply-templates select="dri:help" mode="help"/>
            <xsl:apply-templates select="dri:error" mode="error"/>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="dri:field" mode="normalField">
        <xsl:variable name="confidenceIndicatorID" select="concat(translate(@id,'.','_'),'_confidence_indicator')"/>
        <xsl:choose>
            <!-- TODO: this has changed dramatically (see form3.xml) -->
                        <xsl:when test="@type= 'select'">
                                <select>
                                    <xsl:call-template name="fieldAttributes"/>
                                    <xsl:apply-templates/>
                                </select>
                        </xsl:when>
            <xsl:when test="@type= 'textarea'">
                                <textarea>
                                    <xsl:call-template name="fieldAttributes"/>
                                    
                                    <!--
                                        if the cols and rows attributes are not defined we need to call
                                        the templates for them since they are required attributes in strict xhtml
                                     -->
                                    <xsl:choose>
                                        <xsl:when test="not(./dri:params[@cols])">
                                                        <xsl:call-template name="textAreaCols"/>
                                        </xsl:when>
                                    </xsl:choose>
                                    <xsl:choose>
                                        <xsl:when test="not(./dri:params[@rows])">
                                                <xsl:call-template name="textAreaRows"/>
                                        </xsl:when>
                                    </xsl:choose>
                                    
                                    <xsl:apply-templates />
                                    <xsl:choose>
                                        <xsl:when test="./dri:value[@type='raw']">
                                            <xsl:copy-of select="./dri:value[@type='raw']/node()"/>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:copy-of select="./dri:value[@type='default']/node()"/>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                    <xsl:if  test="string-length(./dri:value) &lt; 1">
                                       <i18n:text>xmlui.dri2xhtml.default.textarea.value</i18n:text>
                                    </xsl:if>
                                </textarea>
                                    

              <!-- add place to store authority value -->
              <xsl:if test="dri:params/@authorityControlled">
                <xsl:variable name="confidence">
                  <xsl:if test="./dri:value[@type='authority']">
                   <xsl:value-of select="./dri:value[@type='authority']/@confidence"/>
                  </xsl:if>
                </xsl:variable>
                <!-- add authority confidence widget -->
                <xsl:call-template name="authorityConfidenceIcon">
                  <xsl:with-param name="confidence" select="$confidence"/>
                  <xsl:with-param name="id" select="$confidenceIndicatorID"/>
                </xsl:call-template>
                <xsl:call-template name="authorityInputFields">
                  <xsl:with-param name="name" select="@n"/>
                  <xsl:with-param name="id" select="@id"/>
                  <xsl:with-param name="authValue" select="dri:value[@type='authority']/text()"/>
                  <xsl:with-param name="confValue" select="dri:value[@type='authority']/@confidence"/>
                  <xsl:with-param name="confIndicatorID" select="$confidenceIndicatorID"/>
                  <xsl:with-param name="unlockButton" select="dri:value[@type='authority']/dri:field[@rend='ds-authority-lock']/@n"/>
                  <xsl:with-param name="unlockHelp" select="dri:value[@type='authority']/dri:field[@rend='ds-authority-lock']/dri:help"/>
                </xsl:call-template>
              </xsl:if>
              <!-- add choice mechanisms -->
              <xsl:choose>
                <xsl:when test="dri:params/@choicesPresentation = 'suggest'">
                  <xsl:call-template name="addAuthorityAutocomplete">
                    <xsl:with-param name="confidenceIndicatorID" select="$confidenceIndicatorID"/>
                    <xsl:with-param name="confidenceName">
                      <xsl:value-of select="concat(@n,'_confidence')"/>
                    </xsl:with-param>
                  </xsl:call-template>
            </xsl:when>
                <xsl:when test="dri:params/@choicesPresentation = 'lookup'">
                  <xsl:call-template name="addLookupButton">
                    <xsl:with-param name="isName" select="'false'"/>
                    <xsl:with-param name="confIndicator" select="$confidenceIndicatorID"/>
                  </xsl:call-template>
                </xsl:when>
              </xsl:choose>
            </xsl:when>

            <!-- This is changing dramatically -->
            <xsl:when test="@type= 'checkbox' or @type= 'radio'">
                <fieldset>
                    <xsl:call-template name="standardAttributes">
                                    <xsl:with-param name="class">
                                        <xsl:text>ds-</xsl:text><xsl:value-of select="@type"/><xsl:text>-field </xsl:text>
                                        <xsl:if test="dri:error">
                                            <xsl:text>error </xsl:text>
                                        </xsl:if>
                                    </xsl:with-param>
                                </xsl:call-template>
                                <xsl:attribute name="id"><xsl:value-of select="generate-id()"/></xsl:attribute>
                    <xsl:if test="dri:label">
                        <legend><xsl:apply-templates select="dri:label" mode="compositeComponent" /></legend>
                    </xsl:if>
                    <xsl:apply-templates />
                </fieldset>
            </xsl:when>
            <!--
                <input>
                            <xsl:call-template name="fieldAttributes"/>
                    <xsl:if test="dri:value[@checked='yes']">
                                <xsl:attribute name="checked">checked</xsl:attribute>
                    </xsl:if>
                    <xsl:apply-templates/>
                </input>
                -->
            <xsl:when test="@type= 'composite'">
                <!-- TODO: add error and help stuff on top of the composite -->
                <span class="ds-composite-field">
                    <xsl:apply-templates select="dri:field" mode="compositeComponent"/>
                </span>
                <xsl:apply-templates select="dri:field/dri:error" mode="compositeComponent"/>
                <xsl:apply-templates select="dri:error" mode="compositeComponent"/>
                <xsl:apply-templates select="dri:field/dri:help" mode="compositeComponent"/>
                <!--<xsl:apply-templates select="dri:help" mode="compositeComponent"/>-->
            </xsl:when>
                    <!-- text, password, file, and hidden types are handled the same.
                        Buttons: added the xsl:if check which will override the type attribute button
                            with the value 'submit'. No reset buttons for now...
                    -->
                    <xsl:otherwise>
                        <input>
                            <xsl:call-template name="fieldAttributes"/>
                            <xsl:if test="@type='button'">
                                <xsl:attribute name="type">submit</xsl:attribute>
                            </xsl:if>
                            <xsl:attribute name="value">
                                <xsl:choose>
                                    <xsl:when test="./dri:value[@type='raw']">
                                        <xsl:value-of select="./dri:value[@type='raw']"/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of select="./dri:value[@type='default']"/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:attribute>
                            <xsl:if test="dri:value/i18n:text">
                                <xsl:attribute name="i18n:attr">value</xsl:attribute>
                            </xsl:if>
                            <xsl:apply-templates />
                        </input>

                        <xsl:variable name="confIndicatorID" select="concat(@id,'_confidence_indicator')"/>
                        <xsl:if test="dri:params/@authorityControlled">
                          <xsl:variable name="confidence">
                            <xsl:if test="./dri:value[@type='authority']">
                              <xsl:value-of select="./dri:value[@type='authority']/@confidence"/>
                            </xsl:if>
                          </xsl:variable>
                          <!-- add authority confidence widget -->
                          <xsl:call-template name="authorityConfidenceIcon">
                            <xsl:with-param name="confidence" select="$confidence"/>
                            <xsl:with-param name="id" select="$confidenceIndicatorID"/>
                          </xsl:call-template>
                          <xsl:call-template name="authorityInputFields">
                            <xsl:with-param name="name" select="@n"/>
                            <xsl:with-param name="id" select="@id"/>
                            <xsl:with-param name="authValue" select="dri:value[@type='authority']/text()"/>
                            <xsl:with-param name="confValue" select="dri:value[@type='authority']/@confidence"/>
                          </xsl:call-template>
                        </xsl:if>
                        <xsl:choose>
                          <xsl:when test="dri:params/@choicesPresentation = 'suggest'">
                            <xsl:call-template name="addAuthorityAutocomplete">
                              <xsl:with-param name="confidenceIndicatorID" select="$confidenceIndicatorID"/>
                              <xsl:with-param name="confidenceName">
                                <xsl:value-of select="concat(@n,'_confidence')"/>
                              </xsl:with-param>
                            </xsl:call-template>
                          </xsl:when>
                          <xsl:when test="dri:params/@choicesPresentation = 'lookup'">
                            <xsl:call-template name="addLookupButton">
                              <xsl:with-param name="isName" select="'false'"/>
                              <xsl:with-param name="confIndicator" select="$confidenceIndicatorID"/>
                            </xsl:call-template>
                          </xsl:when>
                        </xsl:choose>
                    </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!-- A set of standard attributes common to all fields -->
    <xsl:template name="fieldAttributes">
        <xsl:call-template name="standardAttributes">
            <xsl:with-param name="class">
                <xsl:text>ds-</xsl:text><xsl:value-of select="@type"/><xsl:text>-field </xsl:text>
                <xsl:if test="dri:error">
                    <xsl:text>error </xsl:text>
                </xsl:if>
            </xsl:with-param>
        </xsl:call-template>
        <xsl:if test="@disabled='yes'">
            <xsl:attribute name="disabled">disabled</xsl:attribute>
        </xsl:if>
        <xsl:if test="@type != 'checkbox' and @type != 'radio' ">
                <xsl:attribute name="name"><xsl:value-of select="@n"/></xsl:attribute>
        </xsl:if>
        <xsl:if test="@type != 'select' and @type != 'textarea' and @type != 'checkbox' and @type != 'radio' ">
                <xsl:attribute name="type"><xsl:value-of select="@type"/></xsl:attribute>
        </xsl:if>
        <xsl:if test="@type= 'textarea'">
                <xsl:attribute name="onfocus">javascript:tFocus(this);</xsl:attribute>
        </xsl:if>
    </xsl:template>
        
    <!-- Since the field element contains only the type attribute, all other attributes commonly associated
        with input fields are stored on the params element. Rather than parse the attributes directly, this
        template generates a call to attribute templates, something that is not done in XSL by default. The
        templates for the attributes can be found further down. -->
    <xsl:template match="dri:params">
        <xsl:apply-templates select="@*"/>
    </xsl:template>
    
    
    <xsl:template match="dri:field[@type='select']/dri:option">
        <option>
            <xsl:attribute name="value"><xsl:value-of select="@returnValue"/></xsl:attribute>
            <xsl:if test="../dri:value[@type='option'][@option = current()/@returnValue]">
                <xsl:attribute name="selected">selected</xsl:attribute>
            </xsl:if>
            <xsl:apply-templates />
        </option>
    </xsl:template>
    
    <xsl:template match="dri:field[@type='checkbox' or @type='radio']/dri:option">
        <label>
            <input>
                <xsl:attribute name="name"><xsl:value-of select="../@n"/></xsl:attribute>
                <xsl:attribute name="type"><xsl:value-of select="../@type"/></xsl:attribute>
                <xsl:attribute name="value"><xsl:value-of select="@returnValue"/></xsl:attribute>
                <xsl:if test="../dri:value[@type='option'][@option = current()/@returnValue]">
                    <xsl:attribute name="checked">checked</xsl:attribute>
                </xsl:if>
                <xsl:if test="../@disabled='yes'">
                    <xsl:attribute name="disabled">disabled</xsl:attribute>
                </xsl:if>
            </input>
            <xsl:apply-templates />
        </label>
    </xsl:template>
    
    
    
    <!-- A special case for the value element under field of type 'select'. Instead of being used to create
        the value attribute of an HTML input tag, these are used to create selection options.
    <xsl:template match="dri:field[@type='select']/dri:value" priority="2">
        <option>
            <xsl:attribute name="value"><xsl:value-of select="@optionValue"/></xsl:attribute>
            <xsl:if test="@optionSelected = 'yes'">
                <xsl:attribute name="selected">selected</xsl:attribute>
            </xsl:if>
            <xsl:apply-templates />
        </option>
    </xsl:template>-->
    
    <!-- In general cases the value of this element is used directly, so the template does nothing. -->
    <xsl:template match="dri:value" priority="1">
    </xsl:template>
    
    <!-- The field label is usually invoked directly by a higher level tag, so this template does nothing. -->
    <xsl:template match="dri:field/dri:label" priority="2">
    </xsl:template>
    
    <xsl:template match="dri:field/dri:label" mode="compositeComponent">
        <xsl:apply-templates />
    </xsl:template>
    
    <!-- The error field handling -->
    <xsl:template match="dri:error">
        <xsl:attribute name="title"><xsl:value-of select="."/></xsl:attribute>
        <xsl:if test="i18n:text">
            <xsl:attribute name="i18n:attr">title</xsl:attribute>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="dri:error" mode="error">
        <span class="error">* <xsl:apply-templates/></span>
    </xsl:template>
    
    
    
    <!-- Help elements are turning into tooltips. There might be a better way to do this -->
    <xsl:template match="dri:help">
        <xsl:attribute name="title"><xsl:value-of select="."/></xsl:attribute>
        <xsl:if test="i18n:text">
            <xsl:attribute name="i18n:attr">title</xsl:attribute>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="dri:help" mode="help">
        <!-- Only create the <span> if there is content in the <dri:help> node -->
        <xsl:if test="./text() or ./node()">
            <span class="field-help">
                <xsl:apply-templates />
            </span>
        </xsl:if>
    </xsl:template>
    
    
    
    <!-- The last thing in the structural elements section are the templates to cover the attribute calls.
        Although, by default, XSL only parses elements and text, an explicit call to apply the attributes
        of children tags can still be made. This, in turn, requires templates that handle specific attributes,
        like the kind you see below. The chief amongst them is the pagination attribute contained by divs,
        which creates a new div element to display pagination information. -->
    
    <xsl:template match="@pagination">
        <xsl:param name="position"/>
        <xsl:choose>
            <xsl:when test=". = 'simple'">
                <div class="pagination {$position}">
                    <xsl:if test="parent::node()/@previousPage">
                        <a class="previous-page-link">
                            <xsl:attribute name="href">
                                <xsl:value-of select="parent::node()/@previousPage"/>
                            </xsl:attribute>
                            <i18n:text>xmlui.dri2xhtml.structural.pagination-previous</i18n:text>
                        </a>
                    </xsl:if>
                    <p class="pagination-info">
                        <i18n:translate>
                            <xsl:choose>
                                <xsl:when test="parent::node()/@itemsTotal = -1">
                                    <i18n:text>xmlui.dri2xhtml.structural.pagination-info.nototal</i18n:text>
                                </xsl:when>
                                <xsl:otherwise>
                                    <i18n:text>xmlui.dri2xhtml.structural.pagination-info</i18n:text>
                                </xsl:otherwise>
                            </xsl:choose>
                            <i18n:param><xsl:value-of select="parent::node()/@firstItemIndex"/></i18n:param>
                            <i18n:param><xsl:value-of select="parent::node()/@lastItemIndex"/></i18n:param>
                            <i18n:param><xsl:value-of select="parent::node()/@itemsTotal"/></i18n:param>
                        </i18n:translate>
                        <!--
                        <xsl:text>Now showing items </xsl:text>
                        <xsl:value-of select="parent::node()/@firstItemIndex"/>
                        <xsl:text>-</xsl:text>
                        <xsl:value-of select="parent::node()/@lastItemIndex"/>
                        <xsl:text> of </xsl:text>
                        <xsl:value-of select="parent::node()/@itemsTotal"/>
                            -->
                    </p>
                    <xsl:if test="parent::node()/@nextPage">
                        <a class="next-page-link">
                            <xsl:attribute name="href">
                                <xsl:value-of select="parent::node()/@nextPage"/>
                            </xsl:attribute>
                            <i18n:text>xmlui.dri2xhtml.structural.pagination-next</i18n:text>
                        </a>
                    </xsl:if>
                </div>
            </xsl:when>
            <xsl:when test=". = 'masked'">
                <div class="pagination-masked {$position}">
                    <xsl:if test="not(parent::node()/@firstItemIndex = 0 or parent::node()/@firstItemIndex = 1)">
                        <a class="previous-page-link">
                            <xsl:attribute name="href">
                                <xsl:value-of select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
                                <xsl:value-of select="parent::node()/@currentPage - 1"/>
                                <xsl:value-of select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
                            </xsl:attribute>
                            <i18n:text>xmlui.dri2xhtml.structural.pagination-previous</i18n:text>
                        </a>
                    </xsl:if>
                    <p class="pagination-info">
                        <i18n:translate>
                            <xsl:choose>
                                <xsl:when test="parent::node()/@itemsTotal = -1">
                                    <i18n:text>xmlui.dri2xhtml.structural.pagination-info.nototal</i18n:text>
                                </xsl:when>
                                <xsl:otherwise>
                                    <i18n:text>xmlui.dri2xhtml.structural.pagination-info</i18n:text>
                                </xsl:otherwise>
                            </xsl:choose>
                            <i18n:param><xsl:value-of select="parent::node()/@firstItemIndex"/></i18n:param>
                            <i18n:param><xsl:value-of select="parent::node()/@lastItemIndex"/></i18n:param>
                            <i18n:param><xsl:value-of select="parent::node()/@itemsTotal"/></i18n:param>
                        </i18n:translate>
                    </p>
                    <ul class="pagination-links">
                        <xsl:if test="(parent::node()/@currentPage - 4) &gt; 0">
                            <li class="first-page-link">
                                <a>
                                    <xsl:attribute name="href">
                                        <xsl:value-of select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
                                        <xsl:text>1</xsl:text>
                                        <xsl:value-of select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
                                    </xsl:attribute>
                                    <xsl:text>1</xsl:text>
                                </a>
                                <xsl:text> . . . </xsl:text>
                            </li>
                        </xsl:if>
                        <xsl:call-template name="offset-link">
                            <xsl:with-param name="pageOffset">-3</xsl:with-param>
                        </xsl:call-template>
                        <xsl:call-template name="offset-link">
                            <xsl:with-param name="pageOffset">-2</xsl:with-param>
                        </xsl:call-template>
                        <xsl:call-template name="offset-link">
                            <xsl:with-param name="pageOffset">-1</xsl:with-param>
                        </xsl:call-template>
                        <xsl:call-template name="offset-link">
                            <xsl:with-param name="pageOffset">0</xsl:with-param>
                        </xsl:call-template>
                        <xsl:call-template name="offset-link">
                            <xsl:with-param name="pageOffset">1</xsl:with-param>
                        </xsl:call-template>
                        <xsl:call-template name="offset-link">
                            <xsl:with-param name="pageOffset">2</xsl:with-param>
                        </xsl:call-template>
                        <xsl:call-template name="offset-link">
                            <xsl:with-param name="pageOffset">3</xsl:with-param>
                        </xsl:call-template>
                        <xsl:if test="(parent::node()/@currentPage + 4) &lt;= (parent::node()/@pagesTotal)">
                            <li class="last-page-link">
                                <xsl:text> . . . </xsl:text>
                                <a>
                                    <xsl:attribute name="href">
                                        <xsl:value-of select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
                                        <xsl:value-of select="parent::node()/@pagesTotal"/>
                                        <xsl:value-of select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
                                    </xsl:attribute>
                                    <xsl:value-of select="parent::node()/@pagesTotal"/>
                                </a>
                            </li>
                        </xsl:if>
                    </ul>
                    <xsl:if test="not(parent::node()/@lastItemIndex = parent::node()/@itemsTotal)">
                        <a class="next-page-link">
                            <xsl:attribute name="href">
                                <xsl:value-of select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
                                <xsl:value-of select="parent::node()/@currentPage + 1"/>
                                <xsl:value-of select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
                            </xsl:attribute>
                            <i18n:text>xmlui.dri2xhtml.structural.pagination-next</i18n:text>
                        </a>
                    </xsl:if>
                    <xsl:if test="parent::node()/dri:div[@n = 'masked-page-control']">
                        <xsl:apply-templates select="parent::node()/dri:div[@n='masked-page-control']/dri:div">
                            <xsl:with-param name="position" select="$position"/>
                        </xsl:apply-templates>
                    </xsl:if>
                </div>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="dri:div[@n = 'masked-page-control']" priority="5">
        <!--Do not render this division, this is handled by the xsl-->
    </xsl:template>

    <xsl:template match="dri:div[@n ='search-controls-gear']">
        <xsl:param name="position"/>
        <div>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class"><xsl:value-of select="$position"/></xsl:with-param>
            </xsl:call-template>

            <xsl:apply-templates/>
        </div>
    </xsl:template>



    <!-- A quick helper function used by the @pagination template for repetitive tasks -->
    <xsl:template name="offset-link">
        <xsl:param name="pageOffset"/>
        <xsl:if test="((parent::node()/@currentPage + $pageOffset) &gt; 0) and
            ((parent::node()/@currentPage + $pageOffset) &lt;= (parent::node()/@pagesTotal))">
            <li class="page-link">
                <xsl:if test="$pageOffset = 0">
                    <xsl:attribute name="class">current-page-link</xsl:attribute>
                </xsl:if>
                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
                        <xsl:value-of select="parent::node()/@currentPage + $pageOffset"/>
                        <xsl:value-of select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
                    </xsl:attribute>
                    <xsl:value-of select="parent::node()/@currentPage + $pageOffset"/>
                </a>
            </li>
        </xsl:if>
    </xsl:template>
    
    
    <!-- checkbox and radio fields type uses this attribute -->
    <xsl:template match="@returnValue">
        <xsl:attribute name="value"><xsl:value-of select="."/></xsl:attribute>
    </xsl:template>
    
    <!-- used for image buttons -->
    <xsl:template match="@source">
        <xsl:attribute name="src"><xsl:value-of select="."/></xsl:attribute>
    </xsl:template>
    
    <!-- size and maxlength used by text, password, and textarea inputs -->
    <xsl:template match="@size">
        <xsl:attribute name="size"><xsl:value-of select="."/></xsl:attribute>
    </xsl:template>

     <!-- used by select element -->
    <xsl:template match="@evtbehavior">
        <xsl:param name="behavior" select="."/>
        <xsl:if test="normalize-space($behavior)='submitOnChange'">
            <xsl:attribute name="onchange">this.form.submit();</xsl:attribute>
                </xsl:if>
    </xsl:template>

    <xsl:template match="@maxlength">
        <xsl:attribute name="maxlength"><xsl:value-of select="."/></xsl:attribute>
    </xsl:template>
    
    <!-- "multiple" attribute is used by the <select> input method -->
    <xsl:template match="@multiple[.='yes']">
        <xsl:attribute name="multiple">multiple</xsl:attribute>
    </xsl:template>
    
    <!-- rows and cols attributes are used by textarea input -->
    <xsl:template match="@rows">
        <xsl:attribute name="rows"><xsl:value-of select="."/></xsl:attribute>
    </xsl:template>
    
    <xsl:template match="@cols">
        <xsl:attribute name="cols"><xsl:value-of select="."/></xsl:attribute>
    </xsl:template>

    <!-- Add the HTML5 autofocus attribute to the input field -->
    <xsl:template match="@autofocus">
        <xsl:attribute name="autofocus"><xsl:value-of select="."/></xsl:attribute>
    </xsl:template>

    <!-- The general "catch-all" template for attributes matched, but not handled above -->
    <xsl:template match="@*"></xsl:template>
    
    
    
    
    
    
    
<!-- This is the end of the structural elements section. From here to the end of the document come
    templates devoted to handling the referenceSet and reference elements. Although they are considered
    structural elements, neither of the two contains actual content. Instead, references contain references
    to object metadata under objectMeta, while referenceSets group references together.
-->
    
          
    <!-- Starting off easy here, with a summaryList -->
    
    <!-- Current issues:
        
        1. There is no check for the repository identifier. Need to fix that by concatenating it with the
            object identifier and using the resulting string as the key on items and reps.
        2. The use of a key index across the object store is cryptic and counterintuitive and most likely
            could benefit from better documentation.
    -->
    
    <!-- When you come to an referenceSet you have to make a decision. Since it contains objects, and each
        object is its own entity (and handled in its own template) the decision of the overall structure
        would logically (and traditionally) lie with this template. However, to accomplish this we would
        have to look ahead and check what objects are included in the set, which involves resolving the
        references ahead of time and getting the information from their METS profiles directly.
    
        Since this approach creates strong coupling between the set and the objects it contains, and we
        have tried to avoid that, we use the "pioneer" method. -->
    
    <!-- Summarylist case. This template used to apply templates to the "pioneer" object (the first object
        in the set) and let it figure out what to do. This is no longer the case, as everything has been
        moved to the list model. A special theme, called TableTheme, has beeen created for the purpose of
        preserving the pioneer model. -->
    <xsl:template match="dri:referenceSet[@type = 'summaryList']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <!-- Here we decide whether we have a hierarchical list or a flat one -->
        <xsl:choose>
            <xsl:when test="descendant-or-self::dri:referenceSet/@rend='hierarchy' or ancestor::dri:referenceSet/@rend='hierarchy'">
                <ul>
                    <xsl:apply-templates select="*[not(name()='head')]" mode="summaryList"/>
                </ul>
            </xsl:when>
            <xsl:otherwise>
                <ul class="ds-artifact-list">
                    <xsl:apply-templates select="*[not(name()='head')]" mode="summaryList"/>
                </ul>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- First, the detail list case -->
    <xsl:template match="dri:referenceSet[@type = 'detailList']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <ul class="ds-referenceSet-list">
            <xsl:apply-templates select="*[not(name()='head')]" mode="detailList"/>
        </ul>
    </xsl:template>


    <!-- Next up is the summary view case that at this point applies only to items, since communities and
        collections do not have two separate views. -->
    <xsl:template match="dri:referenceSet[@type = 'summaryView']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <xsl:apply-templates select="*[not(name()='head')]" mode="summaryView"/>
    </xsl:template>

    <!-- Finally, we have the detailed view case that is applicable to items, communities and collections.
        In DRI it constitutes a standard view of collections/communities and a complete metadata listing
        view of items. -->
    <xsl:template match="dri:referenceSet[@type = 'detailView']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <xsl:apply-templates select="*[not(name()='head')]" mode="detailView"/>
    </xsl:template>
    
    
    
    
    
    <!-- The following options can be appended to the external metadata URL to request specific
        sections of the METS document:
        
        sections:
        
        A comma-separated list of METS sections to included. The possible values are: "metsHdr", "dmdSec",
        "amdSec", "fileSec", "structMap", "structLink", "behaviorSec", and "extraSec". If no list is provided then *ALL*
        sections are rendered.
        
        
        dmdTypes:
        
        A comma-separated list of metadata formats to provide as descriptive metadata. The list of avaialable metadata
        types is defined in the dspace.cfg, disseminationcrosswalks. If no formats are provided them DIM - DSpace
        Intermediate Format - is used.
        
        
        amdTypes:
        
        A comma-separated list of metadata formats to provide administative metadata. DSpace does not currently
        support this type of metadata.
        
        
        fileGrpTypes:
        
        A comma-separated list of file groups to render. For DSpace a bundle is translated into a METS fileGrp, so
        possible values are "THUMBNAIL","CONTENT", "METADATA", etc... If no list is provided then all groups are
        rendered.
        
        
        structTypes:
        
        A comma-separated list of structure types to render. For DSpace there is only one structType: LOGICAL. If this
        is provided then the logical structType will be rendered, otherwise none will. The default operation is to
        render all structure types.
    -->
    
    <!-- Then we resolve the reference tag to an external mets object -->
    <xsl:template match="dri:reference" mode="summaryList">
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!-- Since this is a summary only, grab the descriptive metadata, and the thumbnails -->
            <xsl:text>?sections=dmdSec,fileSec&amp;fileGrpTypes=THUMBNAIL</xsl:text>
            <!-- An example of requesting a specific metadata standard (MODS and QDC crosswalks only work for items)->
            <xsl:if test="@type='DSpace Item'">
                <xsl:text>&amp;dmdTypes=DC</xsl:text>
            </xsl:if>-->
        </xsl:variable>
        <xsl:comment> External Metadata URL: <xsl:value-of select="$externalMetadataURL"/> </xsl:comment>
        <li>
            <xsl:attribute name="class">
                <xsl:text>ds-artifact-item </xsl:text>
                <xsl:choose>
                    <xsl:when test="position() mod 2 = 0">even</xsl:when>
                    <xsl:otherwise>odd</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:apply-templates select="document($externalMetadataURL)" mode="summaryList"/>
            <xsl:apply-templates />
        </li>
    </xsl:template>
    
    <xsl:template match="dri:reference" mode="detailList">
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!-- No options selected, render the full METS document -->
        </xsl:variable>
        <xsl:comment> External Metadata URL: <xsl:value-of select="$externalMetadataURL"/> </xsl:comment>
        <li>
            <xsl:apply-templates select="document($externalMetadataURL)" mode="detailList"/>
            <xsl:apply-templates />
        </li>
    </xsl:template>
    
    <xsl:template match="dri:reference" mode="summaryView">
        <xsl:variable name='METSRIGHTS-enabled' select="contains(confman:getProperty('plugin.named.org.dspace.content.crosswalk.DisseminationCrosswalk'), 'METSRIGHTS')" />
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!-- If this is an Item, display the METSRIGHTS section, so we
                 know which files have access restrictions.
                 This requires the METSRightsCrosswalk to be enabled! -->
            <xsl:if test="@type='DSpace Item' and $METSRIGHTS-enabled">
                <xsl:text>?rightsMDTypes=METSRIGHTS</xsl:text>
            </xsl:if>
        </xsl:variable>
        <xsl:comment> External Metadata URL: <xsl:value-of select="$externalMetadataURL"/> </xsl:comment>
        <xsl:apply-templates select="document($externalMetadataURL)" mode="summaryView"/>
        <xsl:apply-templates />
    </xsl:template>
    
    <xsl:template match="dri:reference" mode="detailView">
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!-- No options selected, render the full METS document -->
        </xsl:variable>
        <xsl:comment> External Metadata URL: <xsl:value-of select="$externalMetadataURL"/> </xsl:comment>
        <xsl:apply-templates select="document($externalMetadataURL)" mode="detailView"/>
        <xsl:apply-templates />
    </xsl:template>
    
    
    
    
    
    <!-- The standard attributes template -->
    <!-- TODO: should probably be moved up some, since it is commonly called -->
    <xsl:template name="standardAttributes">
        <xsl:param name="class"/>
        <xsl:if test="@id">
            <xsl:attribute name="id"><xsl:value-of select="translate(@id,'.','_')"/></xsl:attribute>
        </xsl:if>
        <xsl:attribute name="class">
            <xsl:value-of select="normalize-space($class)"/>
            <xsl:if test="@rend">
                <xsl:text> </xsl:text>
                <xsl:value-of select="@rend"/>
            </xsl:if>
        </xsl:attribute>

    </xsl:template>
    
    <!-- templates for required textarea attributes used if not found in DRI document -->
    <xsl:template name="textAreaCols">
      <xsl:attribute name="cols">20</xsl:attribute>
    </xsl:template>
    
    <xsl:template name="textAreaRows">
      <xsl:attribute name="rows">5</xsl:attribute>
    </xsl:template>
    
    
    
    <!-- This does it for all the DRI elements. The only thing left to do is to handle Cocoon's i18n
        transformer tags that are used for text translation. The templates below simply push through
        the i18n elements so that they can translated after the XSL step. -->   
    <xsl:template match="i18n:text">
       <xsl:param name="text" select="."/>
       <xsl:choose>
         <xsl:when test="contains($text, '&#xa;')">
           <xsl:value-of select="substring-before($text, '&#xa;')"/>
           <ul>
                <xsl:attribute name="style">float:left; list-style-type:none; text-align:left;</xsl:attribute>
                <xsl:call-template name="linebreak">
                  <xsl:with-param name="text" select="substring-after($text,'&#xa;')"/>
                </xsl:call-template>
           </ul>
         </xsl:when>
         <xsl:otherwise>
           <xsl:copy-of select="$text"/>
         </xsl:otherwise>
       </xsl:choose>
    </xsl:template>

    <!-- Function to replace \n -->
    <xsl:template name="linebreak">
       <xsl:param name="text" select="."/>
       <xsl:choose>
         <xsl:when test="contains($text, '&#xa;')">
           <li>
           <xsl:value-of select="substring-before($text, '&#xa;')"/>
           </li>
           <xsl:call-template name="linebreak">
             <xsl:with-param name="text" select="substring-after($text,'&#xa;')"/>
           </xsl:call-template>
         </xsl:when>
         <xsl:otherwise>
           <xsl:value-of select="$text"/>
         </xsl:otherwise>
       </xsl:choose>
    </xsl:template>
    
    <xsl:template match="i18n:translate">
        <xsl:copy-of select="."/>
    </xsl:template>
    
    <xsl:template match="i18n:param">
        <xsl:copy-of select="."/>
    </xsl:template>
    
    <!-- =============================================================== -->
    <!-- - - - - - New templates for Choice/Authority control - - - - -  -->
    
    <!-- choose 'hidden' for invisible auth, 'text' lets CSS control it. -->
    <xsl:variable name="authorityInputType" select="'text'"/>
    
    <!-- add button to invoke Choices lookup popup.. assume
      -  that the context is a dri:field, where dri:params/@choices is true.
     -->
    <xsl:template name="addLookupButton">
      <xsl:param name="isName" select="'missing value'"/>
      <!-- optional param if you want to send authority value to diff field -->
      <xsl:param name="authorityInput" select="concat(@n,'_authority')"/>
      <!-- optional param for confidence indicator ID -->
      <xsl:param name="confIndicator" select="''"/>
      <input type="button" name="{concat('lookup_',@n)}" class="ds-button-field ds-add-button" >
        <xsl:attribute name="value">
          <xsl:text>Lookup</xsl:text>
          <xsl:if test="contains(dri:params/@operations,'add')">
            <xsl:text> &amp; Add</xsl:text>
          </xsl:if>
        </xsl:attribute>
        <xsl:attribute name="onClick">
          <xsl:text>javascript:DSpaceChoiceLookup('</xsl:text>
          <!-- URL -->
          <xsl:value-of select="concat($context-path,'/admin/lookup')"/>
          <xsl:text>', '</xsl:text>
          <!-- field -->
          <xsl:value-of select="dri:params/@choices"/>
          <xsl:text>', '</xsl:text>
          <!-- formID -->
          <xsl:value-of select="translate(ancestor::dri:div[@interactive='yes']/@id,'.','_')"/>
          <xsl:text>', '</xsl:text>
          <!-- valueInput -->
          <xsl:value-of select="@n"/>
          <xsl:text>', '</xsl:text>
          <!-- authorityInput, name of field to get authority -->
          <xsl:value-of select="$authorityInput"/>
          <xsl:text>', '</xsl:text>
          <!-- Confidence Indicator's ID so lookup can frob it -->
          <xsl:value-of select="$confIndicator"/>
          <xsl:text>', </xsl:text>
          <!-- Collection ID for context -->
          <xsl:choose>
            <xsl:when test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='choice'][@qualifier='collection']">
                <xsl:text>'</xsl:text>
              <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='choice'][@qualifier='collection']"/>
                <xsl:text>'</xsl:text>
            </xsl:when>
            <xsl:otherwise>
              <xsl:text>-1</xsl:text>
            </xsl:otherwise>
          </xsl:choose>
          <xsl:text>, </xsl:text>
          <!-- isName -->
          <xsl:value-of select="$isName"/>
          <xsl:text>, </xsl:text>
          <!-- isRepating -->
          <xsl:value-of select="boolean(contains(dri:params/@operations,'add'))"/>
          <xsl:text>);</xsl:text>
        </xsl:attribute>
      </input>
    </xsl:template>

    <!-- Fragment to display an authority confidence icon.
       -  Insert an invisible 1x1 image which gets "covered" by background
       -  image as dictated by the CSS, so icons are easily adjusted in CSS.
       -  "confidence" param is confidence _value_, i.e. symbolic name
      -->
    <xsl:template name="authorityConfidenceIcon">
      <!-- default confidence value won't show any image. -->
      <xsl:param name="confidence" select="'blank'"/>
      <xsl:param name="id" select="''"/>
      <xsl:variable name="lcConfidence" select="translate($confidence,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')"/>
      <img>
        <xsl:if test="string-length($id) > 0">
          <xsl:attribute name="id">
             <xsl:value-of select="$id"/>
          </xsl:attribute>
        </xsl:if>
        <xsl:attribute name="src">
           <xsl:value-of select="concat($theme-path,'/images/invisible.gif')"/>
        </xsl:attribute>
        <xsl:attribute name="class">
          <xsl:text>ds-authority-confidence </xsl:text>
          <xsl:choose>
            <xsl:when test="string-length($lcConfidence) > 0">
              <xsl:value-of select="concat('cf-',$lcConfidence,' ')"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:text>cf-blank </xsl:text>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
        <xsl:attribute name="title">
          <xsl:text>xmlui.authority.confidence.description.cf_</xsl:text>
          <xsl:value-of select="$lcConfidence"/>
        </xsl:attribute>
      </img>
    </xsl:template>

    <!-- Fragment to include an authority confidence hidden input
       - assumes @n is the name of the field.
       -  param is confidence _value_, i.e. integer 0-6
      -->
    <xsl:template name="authorityConfidenceInput">
      <xsl:param name="confidence"/>
      <xsl:param name="name"/>
      <input class="ds-authority-confidence-input" type="hidden">
        <xsl:attribute name="name">
          <xsl:value-of select="$name"/>
        </xsl:attribute>
        <xsl:attribute name="value">
          <xsl:value-of select="$confidence"/>
        </xsl:attribute>
      </input>
    </xsl:template>


    <!-- insert fields needed by Scriptaculous autocomplete -->
    <xsl:template name="addAuthorityAutocompleteWidgets">
      <!-- "spinner" indicator to signal "loading", managed by autocompleter -->
      <!--  put it next to input field -->
      <span style="display:none;">
        <xsl:attribute name="id">
         <xsl:value-of select="concat(translate(@id,'.','_'),'_indicator')"/>
        </xsl:attribute>
        <img alt="Loading...">
          <xsl:attribute name="src">
           <xsl:value-of select="concat($theme-path,'/images/suggest-indicator.gif')"/>
          </xsl:attribute>
        </img>
      </span>
      <!-- This is the anchor for autocomplete popup, div id="..._container" -->
      <!--  put it below input field, give ID to autocomplete below -->
      <div class="autocomplete">
        <xsl:attribute name="id">
         <xsl:value-of select="concat(translate(@id,'.','_'),'_container')"/>
        </xsl:attribute>
        <xsl:text> </xsl:text>
      </div>
    </xsl:template>

    <!-- adds autocomplete fields and setup script to "normal" submit input -->
    <xsl:template name="addAuthorityAutocomplete">
      <xsl:param name="confidenceIndicatorID" select="''"/>
      <xsl:param name="confidenceName" select="''"/>
      <xsl:call-template name="addAuthorityAutocompleteWidgets"/>
      <xsl:call-template name="autocompleteSetup">
        <xsl:with-param name="formID"        select="translate(ancestor::dri:div[@interactive='yes']/@id,'.','_')"/>
        <xsl:with-param name="metadataField" select="@n"/>
        <xsl:with-param name="inputName"     select="@n"/>
        <xsl:with-param name="authorityName" select="concat(@n,'_authority')"/>
        <xsl:with-param name="containerID"   select="concat(translate(@id,'.','_'),'_container')"/>
        <xsl:with-param name="indicatorID"   select="concat(translate(@id,'.','_'),'_indicator')"/>
        <xsl:with-param name="isClosed"      select="contains(dri:params/@choicesClosed,'true')"/>
        <xsl:with-param name="confidenceIndicatorID" select="$confidenceIndicatorID"/>
        <xsl:with-param name="confidenceName" select="$confidenceName"/>
        <xsl:with-param name="collectionID">
          <xsl:choose>
            <xsl:when test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='choice'][@qualifier='collection']">
              <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='choice'][@qualifier='collection']"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:text>-1</xsl:text>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:with-param>
      </xsl:call-template>
    </xsl:template>

    <!-- generate the script that sets up autocomplete feature on input field -->
    <!-- ..it has lots of params -->
    <xsl:template name="autocompleteSetup">
      <xsl:param name="formID" select="'missing value'"/>
      <xsl:param name="metadataField" select="'missing value'"/>
      <xsl:param name="inputName" select="'missing value'"/>
      <xsl:param name="authorityName" select="''"/>
      <xsl:param name="containerID" select="'missing value'"/>
      <xsl:param name="collectionID" select="'-1'"/>
      <xsl:param name="indicatorID" select="'missing value'"/>
      <xsl:param name="confidenceIndicatorID" select="''"/>
      <xsl:param name="confidenceName" select="''"/>
      <xsl:param name="isClosed" select="'false'"/>
      <script type="text/javascript">
        <xsl:text>var gigo = DSpaceSetupAutocomplete('</xsl:text>
        <xsl:value-of select="$formID"/>
        <xsl:text>', { metadataField: '</xsl:text>
        <xsl:value-of select="$metadataField"/>
        <xsl:text>', isClosed: '</xsl:text>
        <xsl:value-of select="$isClosed"/>
        <xsl:text>', inputName: '</xsl:text>
        <xsl:value-of select="$inputName"/>
        <xsl:text>', authorityName: '</xsl:text>
        <xsl:value-of select="$authorityName"/>
        <xsl:text>', containerID: '</xsl:text>
        <xsl:value-of select="$containerID"/>
        <xsl:text>', indicatorID: '</xsl:text>
        <xsl:value-of select="$indicatorID"/>
        <xsl:text>', confidenceIndicatorID: '</xsl:text>
        <xsl:value-of select="$confidenceIndicatorID"/>
        <xsl:text>', confidenceName: '</xsl:text>
        <xsl:value-of select="$confidenceName"/>
        <xsl:text>', collection: </xsl:text>
        <xsl:value-of select="$collectionID"/>
        <xsl:text>, contextPath: '</xsl:text>
        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
        <xsl:text>'});</xsl:text>
      </script>
    </xsl:template>

    <!-- add the extra _authority{_n?} and _confidence input fields -->
    <xsl:template name="authorityInputFields">
      <xsl:param name="name" select="''"/>
      <xsl:param name="id" select="''"/>
      <xsl:param name="position" select="''"/>
      <xsl:param name="authValue" select="''"/>
      <xsl:param name="confValue" select="''"/>
      <xsl:param name="confIndicatorID" select="''"/>
      <xsl:param name="unlockButton" select="''"/>
      <xsl:param name="unlockHelp" select="''"/>
      <xsl:variable name="authFieldID" select="concat(translate(@id,'.','_'),'_authority')"/>
      <xsl:variable name="confFieldID" select="concat(translate(@id,'.','_'),'_confidence')"/>
      <!-- the authority key value -->
      <input>
        <xsl:attribute name="class">
          <xsl:text>ds-authority-value </xsl:text>
          <xsl:if test="$unlockButton">
            <xsl:text>ds-authority-visible </xsl:text>
          </xsl:if>
        </xsl:attribute>
        <xsl:attribute name="type"><xsl:value-of select="$authorityInputType"/></xsl:attribute>
        <xsl:attribute name="readonly"><xsl:text>readonly</xsl:text></xsl:attribute>
        <xsl:attribute name="name">
          <xsl:value-of select="concat($name,'_authority')"/>
          <xsl:if test="$position">
            <xsl:value-of select="concat('_', $position)"/>
          </xsl:if>
        </xsl:attribute>
        <xsl:if test="$id">
          <xsl:attribute name="id">
            <xsl:value-of select="$authFieldID"/>
          </xsl:attribute>
        </xsl:if>
        <xsl:attribute name="value">
          <xsl:value-of select="$authValue"/>
        </xsl:attribute>
        <!-- this updates confidence after a manual change to authority value -->
        <xsl:attribute name="onChange">
          <xsl:text>javascript: return DSpaceAuthorityOnChange(this, '</xsl:text>
          <xsl:value-of select="$confFieldID"/>
          <xsl:text>','</xsl:text>
          <xsl:value-of select="$confIndicatorID"/>
          <xsl:text>');</xsl:text>
        </xsl:attribute>
      </input>
      <!-- optional "unlock" button on (visible) authority value field -->
      <xsl:if test="$unlockButton">
        <input type="image" class="ds-authority-lock is-locked ">
          <xsl:attribute name="onClick">
            <xsl:text>javascript: return DSpaceToggleAuthorityLock(this, '</xsl:text>
            <xsl:value-of select="$authFieldID"/>
            <xsl:text>');</xsl:text>
          </xsl:attribute>
          <xsl:attribute name="src">
             <xsl:value-of select="concat($theme-path,'/images/invisible.gif')"/>
          </xsl:attribute>
          <xsl:attribute name="i18n:attr">title</xsl:attribute>
          <xsl:attribute name="title">
            <xsl:value-of select="$unlockHelp"/>
          </xsl:attribute>
        </input>
      </xsl:if>
      <input class="ds-authority-confidence-input" type="hidden">
        <xsl:attribute name="name">
          <xsl:value-of select="concat($name,'_confidence')"/>
          <xsl:if test="$position">
            <xsl:value-of select="concat('_', $position)"/>
          </xsl:if>
        </xsl:attribute>
        <xsl:if test="$id">
          <xsl:attribute name="id">
            <xsl:value-of select="$confFieldID"/>
          </xsl:attribute>
        </xsl:if>
        <xsl:attribute name="value">
          <xsl:value-of select="$confValue"/>
        </xsl:attribute>
      </input>
    </xsl:template>
    
    <!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  -->
    <!-- Special Transformations for Choice Authority lookup popup page -->

    <!-- indicator spinner -->
    <xsl:template match="dri:item[@id='aspect.general.ChoiceLookupTransformer.item.select']/dri:figure">
      <img id="lookup_indicator_id" alt="Loading..." style="display:none;">
        <xsl:attribute name="src">
         <xsl:value-of select="concat($theme-path,'/images/lookup-indicator.gif')"/>
        </xsl:attribute>
      </img>
    </xsl:template>
    
    <!-- This inline JS must be added to the popup page for choice lookups -->
    <xsl:template name="choiceLookupPopUpSetup">
      <script type="text/javascript">
        var form = document.getElementById('aspect_general_ChoiceLookupTransformer_div_lookup');
        DSpaceChoicesSetup(form);
      </script>
    </xsl:template>

    <!-- Special select widget for lookup popup -->
    <xsl:template match="dri:field[@id='aspect.general.ChoiceLookupTransformer.field.chooser']">
      <div>
        <select onChange="javascript:DSpaceChoicesSelectOnChange();">
          <xsl:call-template name="fieldAttributes"/>
          <xsl:apply-templates/>
          <xsl:comment>space filler because "unclosed" select annoys browsers</xsl:comment>
        </select>
        <img class="choices-lookup" id="lookup_indicator_id" alt="Loading..." style="display:none;">
          <xsl:attribute name="src">
           <xsl:value-of select="concat($theme-path,'/images/lookup-indicator.gif')"/>
          </xsl:attribute>
        </img>
      </div>
    </xsl:template>

    <!-- Generate buttons with onClick attribute, since it is the easiest
       - way to set a single event handler in a browser-independent manner.
      -->

    <!-- choice popup "accept" button -->
    <xsl:template match="dri:field[@id='aspect.general.ChoiceLookupTransformer.field.accept']">
      <xsl:call-template name="choiceLookupButton">
        <xsl:with-param name="onClick" select="'javascript:DSpaceChoicesAcceptOnClick();'"/>
      </xsl:call-template>
    </xsl:template>

    <!-- choice popup "more" button -->
    <xsl:template match="dri:field[@id='aspect.general.ChoiceLookupTransformer.field.more']">
      <xsl:call-template name="choiceLookupButton">
        <xsl:with-param name="onClick" select="'javascript:DSpaceChoicesMoreOnClick();'"/>
      </xsl:call-template>
    </xsl:template>

    <!-- choice popup "cancel" button -->
    <xsl:template match="dri:field[@id='aspect.general.ChoiceLookupTransformer.field.cancel']">
      <xsl:call-template name="choiceLookupButton">
        <xsl:with-param name="onClick" select="'javascript:DSpaceChoicesCancelOnClick();'"/>
      </xsl:call-template>
    </xsl:template>

    <!-- button markup: special handling needed because these must not be <input type=submit> -->
    <xsl:template name="choiceLookupButton">
      <xsl:param name="onClick"/>
      <input type="button" onClick="{$onClick}">
        <xsl:call-template name="fieldAttributes"/>
        <xsl:attribute name="value">
            <xsl:choose>
                <xsl:when test="./dri:value[@type='raw']">
                    <xsl:value-of select="./dri:value[@type='raw']"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="./dri:value[@type='default']"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:attribute>
        <xsl:if test="dri:value/i18n:text">
            <xsl:attribute name="i18n:attr">value</xsl:attribute>
        </xsl:if>
        <xsl:apply-templates />
      </input>
    </xsl:template>

    <!-- - - - - - End templates for Choice/Authority control - - - - -  -->
    <!-- =============================================================== -->


    <!-- - - - - - template for harvesting - - - - -  -->
    <xsl:template match="dri:field[@id='aspect.administrative.collection.SetupCollectionHarvestingForm.field.oai-set-comp' and @type='composite']" mode="formComposite" priority="2">
        <xsl:for-each select="dri:field[@type='radio']">
            <div class="ds-form-content">
                <xsl:for-each select="dri:option">
                    <input type="radio">
                        <xsl:attribute name="id"><xsl:value-of select="@returnValue"/></xsl:attribute>
                        <xsl:attribute name="name"><xsl:value-of select="../@n"/></xsl:attribute>
                        <xsl:attribute name="value"><xsl:value-of select="@returnValue"/></xsl:attribute>
                        <xsl:if test="../dri:value[@type='option'][@option = current()/@returnValue]">
                            <xsl:attribute name="checked">checked</xsl:attribute>
                        </xsl:if>
                    </input>
                    <label>
                        <xsl:attribute name="for"><xsl:value-of select="@returnValue"/></xsl:attribute>
                        <xsl:value-of select="text()"/>
                    </label>
                    <xsl:if test="@returnValue = 'specific'">
                        <xsl:apply-templates select="../../dri:field[@n='oai_setid']"/>
                    </xsl:if>
                    <br/>
                </xsl:for-each>
            </div>
        </xsl:for-each>
    </xsl:template>



    <!-- Add each RSS feed from meta to a list -->
    <xsl:template name="addRSSLinks">
        <xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='feed']">
            <li>
                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="."/>
                    </xsl:attribute>

                     <xsl:attribute name="style">
                        <xsl:text>background: url(</xsl:text>
                        <xsl:value-of select="$context-path"/>
                        <xsl:text>/static/icons/feed.png) no-repeat</xsl:text>
                    </xsl:attribute>
                    <xsl:choose>
                        <xsl:when test="contains(., 'rss_1.0')">
                            <xsl:text>RSS 1.0</xsl:text>
                        </xsl:when>
                        <xsl:when test="contains(., 'rss_2.0')">
                            <xsl:text>RSS 2.0</xsl:text>
                        </xsl:when>
                        <xsl:when test="contains(., 'atom_1.0')">
                            <xsl:text>Atom</xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="@qualifier"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </a>
            </li>
        </xsl:for-each>
    </xsl:template>

    <!-- Template for the bitstream reordering -->
    <xsl:template match="dri:cell[starts-with(@id, 'aspect.administrative.item.EditItemBitstreamsForm.cell.bitstream_order_')]" priority="2">
        <td>
            <xsl:call-template name="standardAttributes"/>
            <xsl:apply-templates select="*[not(@type='button')]" />
            <!-- A div that will indicate the old & the new order -->
            <div>
                <span>
                    <!-- Give this one an ID so that the javascript can change his value -->
                    <xsl:attribute name="id">
                        <xsl:value-of select="dri:field/@id"/>
                        <xsl:text>_new</xsl:text>
                    </xsl:attribute>
                    <xsl:value-of select="dri:field/dri:value"/>
                </span>
                <xsl:text> (</xsl:text>
                <i18n:text>xmlui.administrative.item.EditItemBitstreamsForm.previous_order</i18n:text>
                <xsl:value-of select="dri:field/dri:value"/>
                <xsl:text>)</xsl:text>
            </div>
        </td>
        <td>
            <xsl:apply-templates select="dri:field[@type='button']"/>
        </td>
    </xsl:template>



    <xsl:template match="dri:list[@type='dsolist']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <ul class="ds-artifact-list">
            <xsl:apply-templates select="*[not(name()='head')]" mode="dsoList"/>
        </ul>
    </xsl:template>

    <xsl:template match="dri:list/dri:list" mode="dsoList" priority="7">
        <xsl:apply-templates select="dri:head"/>
        <ul>
            <xsl:apply-templates select="*[not(name()='head')]" mode="dsoList"/>
        </ul>
    </xsl:template>

    <xsl:template match="dri:list/dri:list/dri:list" mode="dsoList" priority="8">
        <li>
            <xsl:attribute name="class">
                <xsl:text>ds-artifact-item clearfix </xsl:text>
                <xsl:choose>
                    <xsl:when test="position() mod 2 = 0">even</xsl:when>
                    <xsl:otherwise>odd</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <!--
                Retrieve the type from our name, the name contains the following format:
                    {handle}:{metadata}
            -->
            <xsl:variable name="handle">
                <xsl:value-of select="substring-before(@n, ':')"/>
            </xsl:variable>
            <xsl:variable name="type">
                <xsl:value-of select="substring-after(@n, ':')"/>
            </xsl:variable>
            <xsl:variable name="externalMetadataURL">
                <xsl:text>cocoon://metadata/handle/</xsl:text>
                <xsl:value-of select="$handle"/>
                <xsl:text>/mets.xml</xsl:text>
                <!-- Since this is a summary only grab the descriptive metadata, and the thumbnails -->
                <xsl:text>?sections=dmdSec,fileSec&amp;fileGrpTypes=THUMBNAIL</xsl:text>
                <!-- An example of requesting a specific metadata standard (MODS and QDC crosswalks only work for items)->
                <xsl:if test="@type='DSpace Item'">
                    <xsl:text>&amp;dmdTypes=DC</xsl:text>
                </xsl:if>-->
            </xsl:variable>


            <xsl:choose>
                <xsl:when test="$type='community'">
                    <xsl:call-template name="communitySummaryList">
                        <xsl:with-param name="handle">
                            <xsl:value-of select="$handle"/>
                        </xsl:with-param>
                        <xsl:with-param name="externalMetadataUrl">
                            <xsl:value-of select="$externalMetadataURL"/>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:when>
                <xsl:when test="$type='collection'">
                    <xsl:call-template name="collectionSummaryList">
                        <xsl:with-param name="handle">
                            <xsl:value-of select="$handle"/>
                        </xsl:with-param>
                        <xsl:with-param name="externalMetadataUrl">
                            <xsl:value-of select="$externalMetadataURL"/>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:when>
                <xsl:when test="$type='item'">
                    <xsl:call-template name="itemSummaryList">
                        <xsl:with-param name="handle">
                            <xsl:value-of select="$handle"/>
                        </xsl:with-param>
                        <xsl:with-param name="externalMetadataUrl">
                            <xsl:value-of select="$externalMetadataURL"/>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:when>
            </xsl:choose>
        </li>
    </xsl:template>

    <xsl:template name="communitySummaryList">
        <xsl:param name="handle"/>
        <xsl:param name="externalMetadataUrl"/>

        <xsl:variable name="metsDoc" select="document($externalMetadataUrl)"/>

        <div class="artifact-title">
            <a href="{$metsDoc/mets:METS/@OBJID}">
                <xsl:choose>
                    <xsl:when test="dri:list[@n=(concat($handle, ':dc.title'))]">
                        <xsl:apply-templates select="dri:list[@n=(concat($handle, ':dc.title'))]/dri:item"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                    </xsl:otherwise>
                </xsl:choose>
            </a>
            <!--Display community strengths (item counts) if they exist-->
            <xsl:if test="string-length($metsDoc/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='format'][@qualifier='extent'][1]) &gt; 0">
                <xsl:text> [</xsl:text>
                <xsl:value-of
                        select="$metsDoc/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='format'][@qualifier='extent'][1]"/>
                <xsl:text>]</xsl:text>
            </xsl:if>
        </div>
    </xsl:template>

    <xsl:template name="collectionSummaryList">
        <xsl:param name="handle"/>
        <xsl:param name="externalMetadataUrl"/>

        <xsl:variable name="metsDoc" select="document($externalMetadataUrl)"/>

        <div class="artifact-title">
            <a href="{$metsDoc/mets:METS/@OBJID}">
                <xsl:choose>
                    <xsl:when test="dri:list[@n=(concat($handle, ':dc.title'))]">
                        <xsl:apply-templates select="dri:list[@n=(concat($handle, ':dc.title'))]/dri:item"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                    </xsl:otherwise>
                </xsl:choose>
            </a>

        </div>
        <!--Display collection strengths (item counts) if they exist-->
        <xsl:if test="string-length($metsDoc/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='format'][@qualifier='extent'][1]) &gt; 0">
            <xsl:text> [</xsl:text>
            <xsl:value-of
                    select="$metsDoc/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='format'][@qualifier='extent'][1]"/>
            <xsl:text>]</xsl:text>
        </xsl:if>


    </xsl:template>

    <xsl:template name="itemSummaryList">
        <xsl:param name="handle"/>
        <xsl:param name="externalMetadataUrl"/>

        <xsl:variable name="metsDoc" select="document($externalMetadataUrl)"/>

        <div class="artifact-description">
            <div class="artifact-title">
                <xsl:element name="a">
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="$metsDoc/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/@withdrawn">
                                <xsl:value-of select="$metsDoc/mets:METS/@OBJEDIT"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="concat($context-path, '/handle/', $handle)"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <xsl:choose>
                        <xsl:when test="dri:list[@n=(concat($handle, ':dc.title'))]">
                            <xsl:apply-templates select="dri:list[@n=(concat($handle, ':dc.title'))]/dri:item"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:element>
                <!-- Generate COinS with empty content per spec but force Cocoon to not create a minified tag  -->
                <span class="Z3988">
                    <xsl:attribute name="title">
                        <xsl:for-each select="$metsDoc/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim">
                            <xsl:call-template name="renderCOinS"/>
                        </xsl:for-each>
                    </xsl:attribute>
                    &#xFEFF; <!-- non-breaking space to force separating the end tag -->
                </span>
            </div>
            <div class="artifact-info">
                <span class="author">
                    <xsl:choose>
                        <xsl:when test="dri:list[@n=(concat($handle, ':dc.contributor.author'))]">
                            <xsl:for-each select="dri:list[@n=(concat($handle, ':dc.contributor.author'))]/dri:item">
                                <xsl:variable name="author">
                                    <xsl:value-of select="."/>
                                </xsl:variable>
                                <span>
                                    <!--Check authority in the mets document-->
                                    <xsl:if test="$metsDoc/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='contributor' and @qualifier='author' and . = $author]/@authority">
                                        <xsl:attribute name="class">
                                            <xsl:text>ds-dc_contributor_author-authority</xsl:text>
                                        </xsl:attribute>
                                    </xsl:if>
                                    <xsl:apply-templates select="."/>
                                </span>

                                <xsl:if test="count(following-sibling::dri:item) != 0">
                                    <xsl:text>; </xsl:text>
                                </xsl:if>
                            </xsl:for-each>
                        </xsl:when>
                        <xsl:when test="dri:list[@n=(concat($handle, ':dc.creator'))]">
                            <xsl:for-each select="dri:list[@n=(concat($handle, ':dc.creator'))]/dri:item">
                                <xsl:apply-templates select="."/>
                                <xsl:if test="count(following-sibling::dri:item) != 0">
                                    <xsl:text>; </xsl:text>
                                </xsl:if>
                            </xsl:for-each>
                        </xsl:when>
                        <xsl:when test="dri:list[@n=(concat($handle, ':dc.contributor'))]">
                            <xsl:for-each select="dri:list[@n=(concat($handle, ':dc.contributor'))]/dri:item">
                                <xsl:apply-templates select="."/>
                                <xsl:if test="count(following-sibling::dri:item) != 0">
                                    <xsl:text>; </xsl:text>
                                </xsl:if>
                            </xsl:for-each>
                        </xsl:when>
                        <xsl:otherwise>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-author</i18n:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </span>
                <xsl:text> </xsl:text>
                <xsl:if test="dri:list[@n=(concat($handle, ':dc.date.issued'))] or dri:list[@n=(concat($handle, ':dc.publisher'))]">
                    <span class="publisher-date">
                        <xsl:text>(</xsl:text>
                        <xsl:if test="dri:list[@n=(concat($handle, ':dc.publisher'))]">
                            <span class="publisher">
                                <xsl:apply-templates select="dri:list[@n=(concat($handle, ':dc.publisher'))]/dri:item"/>
                            </span>
                            <xsl:text>, </xsl:text>
                        </xsl:if>
                        <span class="date">
                            <xsl:value-of
                                    select="substring(dri:list[@n=(concat($handle, ':dc.date.issued'))]/dri:item,1,10)"/>
                        </span>
                        <xsl:text>)</xsl:text>
                    </span>
                </xsl:if>
                <xsl:choose>
                    <xsl:when test="dri:list[@n=(concat($handle, ':dc.description.abstract'))]/dri:item/dri:hi">
                        <div class="abstract">
                            <xsl:for-each select="dri:list[@n=(concat($handle, ':dc.description.abstract'))]/dri:item">
                                <xsl:apply-templates select="."/>
                                <xsl:text>...</xsl:text>
                                <br/>
                            </xsl:for-each>

                        </div>
                    </xsl:when>
                    <xsl:when test="dri:list[@n=(concat($handle, ':fulltext'))]">
                        <div class="abstract">
                            <xsl:for-each select="dri:list[@n=(concat($handle, ':fulltext'))]/dri:item">
                                <xsl:apply-templates select="."/>
                                <xsl:text>...</xsl:text>
                                <br/>
                            </xsl:for-each>
                        </div>
                    </xsl:when>
                </xsl:choose>
            </div>
        </div>

        <!--Generates thumbnails (if present)-->
        <xsl:apply-templates select="$metsDoc/mets:METS/mets:fileSec" mode="artifact-preview"/>

    </xsl:template>
    
    <!-- Display language selection if more than 1 language is supported -->
    <xsl:template name="languageSelection">
        <xsl:if test="count(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='page'][@qualifier='supportedLocale']) &gt; 1">
            <div id="ds-language-selection">
                <xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='page'][@qualifier='supportedLocale']">
                    <xsl:variable name="locale" select="."/>
                    <a>
                        <xsl:attribute name="href">
                            <xsl:value-of select="$current-uri"/>
                            <xsl:text>?locale-attribute=</xsl:text>
                            <xsl:value-of select="$locale"/>
                        </xsl:attribute>
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='supportedLocale'][@qualifier=$locale]"/>
                    </a>
                </xsl:for-each>
            </div>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
