<?xml version="1.0" encoding="UTF-8"?>

<!--
    Dryad stylesheet

    This stylesheet overrides and extends the basic dri2xhtml of Manakin.

    Original authors: Amol Bapat, Ryan Scherle, Kevin Clarke
-->

<xsl:stylesheet xmlns="http://www.w3.org/1999/xhtml" xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
                xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:mets="http://www.loc.gov/METS/"
                xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
                xmlns:mods="http://www.loc.gov/mods/v3" xmlns:xlink="http://www.w3.org/TR/xlink/"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:encoder="xalan://java.net.URLEncoder" xmlns:decoder="xalan://java.net.URLDecoder"
                exclude-result-prefixes="xalan encoder decoder" xmlns:exslt="http://exslt.org/dynamic"
                version="1.0">

    <xsl:import href="../dri2xhtml.xsl"/>

    <xsl:import href="DryadItemSummary.xsl"/>
    <xsl:import href="DryadFooter.xsl"/>
    <xsl:import href="DryadSearch.xsl"/>
    <xsl:import href="integrated-view.xsl"/>

    <xsl:output method="xml" version="1.0" encoding="utf-8" indent="yes"/>

    <!-- Check to see if XHTML pages are included via this XSLT. If a page is found,
      it is placed in the "doc" variable. 
      -->
    <xsl:variable name="meta" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata"/>
    <xsl:variable name="pageName" select="$meta[@element='request'][@qualifier='URI']"/>
    <!--xsl:variable name="doc" select="document(concat('pages/', $pageName, '.xhtml'))"/-->


    <xsl:template match="dri:xref[@rend='embed']">
               <xsl:variable name="url" select="concat('pages/',@target)" />
               <xsl:copy-of select="document(string($url))/html/*"/>
           </xsl:template>






    <!-- Overwriting the default DSpace dri:body template to check for pages -->
    <xsl:template match="dri:body">
        <div id="ds-body">
            <xsl:if test="$meta[@element='alert'][@qualifier='message']">
                <div id="ds-system-wide-alert">
                    <p>
                        <xsl:copy-of select="$meta[@element='alert'][@qualifier='message']/node()"/>
                    </p>
                </div>
            </xsl:if>
            <xsl:choose>
                <!--<xsl:when test="$doc">-->
                    <!--<xsl:copy-of select="$doc//div[@id='ds-body']/*"/>-->
                    <!--&lt;!&ndash; hint 'error=' is used to indicate we should append a feedback form &ndash;&gt;-->
                    <!--<xsl:if test="$meta[@qualifier='queryString'][starts-with(., 'error=')]">-->
                        <!--<div style="margin-top: 20px;">-->
                            <!--<xsl:variable name="report_text">-->
                                <!--<xsl:call-template name="parse-query-param">-->
                                    <!--<xsl:with-param name="param-name">body</xsl:with-param>-->
                                <!--</xsl:call-template>-->
                            <!--</xsl:variable>-->
                            <!--<form action="/feedback" method="post" onsubmit="javascript:tSubmit(this);">-->
                                <!--<input name="email" type="hidden" value="help@datadryad.org"/>-->
                                <!--<textarea name="comments" onfocus="javascript:tFocus(this);" cols="60" rows="15">-->
                                    <!--<xsl:value-of select="decoder:decode($report_text)"/>-->
                                <!--</textarea>-->
                                <!--<br/>-->
                                <!--<input name="submit" type="submit" value="Send Feedback"/>-->
                            <!--</form>-->
                        <!--</div>-->
                    <!--</xsl:if>-->
                <!--</xsl:when>-->
                <xsl:otherwise>
                    <xsl:apply-templates/>
                </xsl:otherwise>
            </xsl:choose>
        </div>
    </xsl:template>

    <xsl:template match="*[@rend='ame-suggest-widget']">
        <xsl:param name="containerID" select="concat(translate(@id,'.','_'),'_container')"/>
        <xsl:param name="treeID" select="concat(translate(@id,'.','_'),'_tree')"/>
        <xsl:param name="itemID" select="//dri:field[@n='ame-item-id']/dri:value/text()"/>
        <xsl:param name="field" select="@n"/>
        <xsl:param name="buttonID" select="concat(translate(@id,'.','_'),'_add')"/>

        <div class="ame-suggest-container">
            <xsl:attribute name="id">
                <xsl:value-of select="$containerID"/>
            </xsl:attribute>

            <div class="ame-suggest-header">
                <font>Suggested Terms
                    <a title="These terms have been automatically selected from a controlled vocabulary based on the article title, description, and any existing keywords.">
                        ?
                    </a>
                </font>
            </div>
            <div class="ame-suggest-control">
                <xsl:attribute name="id">
                    <xsl:value-of select="$treeID"/>
                </xsl:attribute>
                &#160;
            </div>
            <div class="ame-suggest-buttons">
                <input class="ame-suggest-add" type="button" value="Add Selected">
                    <xsl:attribute name="id">
                        <xsl:value-of select="$buttonID"/>
                    </xsl:attribute>
                </input>
            </div>
        </div>

        <xsl:call-template name="ameSetup">
            <xsl:with-param name="itemID" select="$itemID"/>
            <xsl:with-param name="containerID" select="$containerID"/>
            <xsl:with-param name="treeID" select="$treeID"/>
            <xsl:with-param name="field" select="$field"/>
            <xsl:with-param name="buttonID" select="$buttonID"/>
        </xsl:call-template>
        <div class="ame-suggest-end">&#160;</div>
    </xsl:template>

    <xsl:template name="ameSetup">
        <xsl:param name="itemID" select="'mising value'"/>
        <xsl:param name="containerID" select="'missing value'"/>
        <xsl:param name="treeID" select="'missing value'"/>
        <xsl:param name="field" select="'missing value'"/>
        <xsl:param name="buttonID" select="'missing value'"/>
        <script type="text/javascript">
            <xsl:text>var ame = AMESuggestSetup('</xsl:text>
            <xsl:value-of select="$itemID"/>
            <xsl:text>','</xsl:text>
            <xsl:value-of select="$containerID"/>
            <xsl:text>','</xsl:text>
            <xsl:value-of select="$treeID"/>
            <xsl:text>','</xsl:text>
            <xsl:value-of select="$field"/>
            <xsl:text>','</xsl:text>
            <xsl:value-of select="$buttonID"/>
            <xsl:text>');</xsl:text>
        </script>
    </xsl:template>


    <xsl:template match="*[@rend='blog-box']">
        <div id="dryad_blog">
            <h3>
                <i18n:text>
                    <xsl:value-of select=".//dri:head"/>
                </i18n:text>
                <xsl:text>&#160;&#160;</xsl:text>
                <a href="http://blog.datadryad.org/feed/">
                    <img src="/themes/Dryad/images/rss.jpg" style="border: 0px;" alt="RSS icon"/>
                </a>
            </h3>
            <ul>
                <xsl:for-each select=".//dri:item/dri:xref">
                    <li>
                        <a href="{string(./@target)}">
                            <xsl:value-of select="."/>
                        </a>
                    </li>
                </xsl:for-each>
            </ul>
        </div>
    </xsl:template>

    <!-- ########################## Head statement ######################## -->
    <xsl:template name="buildHead">
        <head>
	  <meta name="viewport" content="width=device-width, initial-scale=1.0" />      
	  <!--[if lt IE 9]>
	<script src="http://css3-mediaqueries-js.googlecode.com/svn/trunk/css3-mediaqueries.js"></script>
	<![endif]-->
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
            <meta name="google-site-verification" content="IqB7A6dUGs-0ncAgB3f0PXxeO_OcjyVAtRNdBFie4AM"/>
            <!-- Add stylsheets -->
            <xsl:for-each select="$meta[@element='stylesheet']">
                <link rel="stylesheet" type="text/css">
                    <xsl:attribute name="media">
                        <xsl:value-of select="@qualifier"/>
                    </xsl:attribute>
                    <xsl:attribute name="href">
                        <xsl:value-of select="$meta[@element='contextPath'][not(@qualifier)]"/>
                        <xsl:text>/themes/</xsl:text>
                        <xsl:value-of select="$meta[@element='theme'][@qualifier='path']"/>
                        <xsl:text>/</xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:attribute>
                </link>
            </xsl:for-each>

            <link type="application/rss+xml" rel="alternate" href="/feed/rss_2.0/10255/3"/>
            <link type="application/rss+xml" rel="alternate" href="/feed/rss_2.0/10255/3"/>
            <link type="application/atom+xml" rel="alternate" href="/feed/atom_1.0/10255/3"/>
            <link rel="icon" type="image/ico" href="/themes/Dryad/images/favicon.ico"/>

            <script type="text/javascript" language="javascript" src="http://platform.twitter.com/widgets.js">
                <xsl:text>&#160;</xsl:text>
            </script>

            <script type="text/javascript" language="javascript" src="/themes/Dryad/lib/editor.js">
                <xsl:text>&#160;</xsl:text>
            </script>


            <script type="text/javascript" language="javascript" src="/themes/Dryad/lib/util.js">
                <xsl:text>&#160;</xsl:text>
            </script>

            <!-- Add theme javascipt  -->
            <xsl:for-each
                    select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='javascript'][not(@qualifier)]">
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
                <script type="text/javascript">
                    <xsl:attribute name="src">
                        <xsl:value-of
                                select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                        <xsl:text>/</xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:attribute>
                    <xsl:text>&#160;</xsl:text>
                </script>
            </xsl:for-each>

            <!-- Add a google analytics script if the key is present -->
            <xsl:if test="$meta[@element='google'][@qualifier='analytics']">
                <script type="text/javascript">
                    <xsl:text>var gaJsHost = (("https:" == document.location.protocol) ? "https://ssl." : "http://www.");</xsl:text>
                    <xsl:text>document.write(unescape("%3Cscript src='" + gaJsHost + "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));</xsl:text>
                </script>

                <script type="text/javascript">
                    <xsl:text>try {</xsl:text>
                    <xsl:text>var pageTracker = _gat._getTracker("</xsl:text><xsl:value-of
                        select="$meta[@element='google'][@qualifier='analytics']"/><xsl:text>");</xsl:text>
                    <xsl:text>pageTracker._trackPageview();</xsl:text>
                    <xsl:text>} catch(err) {}</xsl:text>
                </script>
            </xsl:if>


            <!-- Add the title in, preferring package but falling back to file, empty -->
            <xsl:variable name="pkg_page_title" select="$meta[@element='title' and not(@qualifier)]"/>
            <xsl:variable name="file_page_title" select="$meta[@element='title' and @qualifier='package']"/>
            <title>
                <xsl:choose>
                    <!--<xsl:when test="$doc">-->
                        <!--<xsl:value-of select="$doc/html/head/title"/>-->
                    <!--</xsl:when>-->
                    <xsl:when test="$file_page_title">
                        <i18n:text>xmlui.dryad.page_title</i18n:text>
                        <xsl:value-of
                                select="//*[@id='org.datadryad.dspace.xmlui.aspect.browse.ItemViewer.div.item-view']/dri:head"/>
                    </xsl:when>
                    <xsl:when test="$pkg_page_title">
                        <xsl:copy-of select="$pkg_page_title/node()"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text> </xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
            </title>

            <!-- Head metadata in item pages -->
            <xsl:if test="$meta[@element='xhtml_head_item']">
                <xsl:value-of select="$meta[@element='xhtml_head_item']" disable-output-escaping="yes"/>
            </xsl:if>

            <script type="text/javascript">jQuery.noConflict();</script>


        </head>
    </xsl:template>

    <xsl:template name="buildHeader">
        <div id="ds-header">
            <!--add functional javasript-->
            <xsl:for-each select="$meta[@element='functjavascript']">
                <script type="text/javascript">
                    <xsl:choose>
                        <xsl:when test="@absolutePath='true'">
                            <xsl:attribute name="src">
                                <xsl:value-of select="."/>
                            </xsl:attribute>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:attribute name="src">
                                <xsl:value-of select="$meta[@element='contextPath'][not(@qualifier)]"/>
                                <xsl:text>/aspects/</xsl:text>
                                <xsl:value-of select="."/>
                            </xsl:attribute>
                        </xsl:otherwise>
                    </xsl:choose>
                    &#160;
                </script>
            </xsl:for-each>
            <a>
                <xsl:attribute name="href">
                    <xsl:variable name="contextpath" select="$meta[@element='contextPath'][not(@qualifier)]"/>
                    <xsl:choose>
                        <xsl:when test="$contextpath != ''">
                            <xsl:value-of select="$contextpath"/>
                        </xsl:when>
                        <xsl:otherwise>/</xsl:otherwise>
                    </xsl:choose>
                </xsl:attribute>
                <xsl:choose>
                    <xsl:when test="$meta[@element='request'][@qualifier='realServerPort'][. = '9999']">
                        <img id="ds-header-logo" src="{$theme-path}/images/dryadLogo-dev.png" alt="dryad dev logo"/>
                    </xsl:when>
                    <xsl:when test="$meta[@element='request'][@qualifier='realServerPort'][. = '7777']">
                        <img id="ds-header-logo" src="{$theme-path}/images/dryadLogo-demo.png" alt="dryad dev logo"/>
                    </xsl:when>
                    <xsl:when test="$meta[@element='request'][@qualifier='realServerPort'][. = '8888']">
                        <img id="ds-header-logo" src="{$theme-path}/images/dryadLogo-staging.png" alt="dryad staging logo"/>
                    </xsl:when>
                    <xsl:when test="$meta[@element='request'][@qualifier='realServerPort'][. = '6666']">
                        <img id="ds-header-logo" src="{$theme-path}/images/dryadLogo-mrc.png" alt="dryad mrc logo"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <img id="ds-header-logo" src="{$theme-path}/images/dryadLogo.png" alt="dryad logo"/>
                    </xsl:otherwise>
                </xsl:choose>
            </a>

            <span>
                <xsl:attribute name="class">
                    <xsl:text>ds-trail</xsl:text>
                    <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='URI'] = 'submissions'">
                        <xsl:text>-shown</xsl:text>
                    </xsl:if>
                </xsl:attribute>
                <xsl:choose>
                    <xsl:when test="count(/dri:document/dri:meta/dri:pageMeta/dri:trail) = 0">
                        <span class="ds-trail-link first-link">-</span>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:apply-templates select="/dri:document/dri:meta/dri:pageMeta/dri:trail"/>
                    </xsl:otherwise>
                </xsl:choose>
            </span>

            <xsl:choose>
                <xsl:when test="/dri:document/dri:meta/dri:userMeta/@authenticated = 'yes'">
                    <div id="ds-user-box">
                        <p>
                            <a>
                                <xsl:attribute name="href">
                                    <xsl:value-of
                                            select="/dri:document/dri:meta/dri:userMeta/
                                        dri:metadata[@element='identifier' and @qualifier='url']"
                                            />
                                </xsl:attribute>
                                <i18n:text>xmlui.dri2xhtml.structural.profile</i18n:text>
                                <xsl:value-of
                                        select="/dri:document/dri:meta/dri:userMeta/
                                    dri:metadata[@element='identifier' and @qualifier='firstName']"/>
                                <xsl:text> </xsl:text>
                                <xsl:value-of
                                        select="/dri:document/dri:meta/dri:userMeta/
                                    dri:metadata[@element='identifier' and @qualifier='lastName']"
                                        />
                            </a>
                            <xsl:text> | </xsl:text>
                            <a>
                                <xsl:attribute name="href">
                                    <xsl:value-of
                                            select="/dri:document/dri:meta/dri:userMeta/
                                        dri:metadata[@element='identifier' and @qualifier='logoutURL']"
                                            />
                                </xsl:attribute>
                                <i18n:text>xmlui.dri2xhtml.structural.logout</i18n:text>
                            </a>
                        </p>
                        <!-- temporary hack -->
                        <!--<xsl:if
                              test="$meta[@element='request'][@qualifier='URI'][not(contains(., 'discover')) and string-length(.) &gt; 1]">-->
                        <!--						<xsl:if
                                  test="$meta[@element='request'][@qualifier='URI'][string-length(.) &gt; 1]">-->
                        <form action="/discover" method="get" onsubmit="javascript:tSubmit(this);">
                            <p>
                                <input name="query" type="text" value=""/>
                                <input name="submit" type="submit" value=" Search Data "/>
                                <a href="/searching" alt="How searching works in Dryad">
                                    <img src="/themes/Dryad/images/help.png" alt="How searching works in Dryad"/>
                                </a>
                                <input name="location" type="hidden" value="l2"/>
                            </p>
                        </form>
                        <!--						</xsl:if>-->
                    </div>
                </xsl:when>
                <xsl:otherwise>
                    <div id="ds-user-box">
                        <p>
                            <a>
                                <xsl:attribute name="href">
                                    <xsl:value-of
                                            select="/dri:document/dri:meta/dri:userMeta/
                                        dri:metadata[@element='identifier' and @qualifier='loginURL']"
                                            />
                                </xsl:attribute>
                                <i18n:text>xmlui.dri2xhtml.structural.login</i18n:text>
                            </a>
                        </p>
                        <!--						<xsl:if
                   test="$meta[@element='request'][@qualifier='URI'][not(contains(., 'discover')) and string-length(.) &gt; 1]">-->
                        <!--						<xsl:if
                                  test="$meta[@element='request'][@qualifier='URI'][string-length(.) &gt; 1]">-->
                        <form action="/discover" method="get" onsubmit="javascript:tSubmit(this);">
                            <p>
                                <input name="query" type="text" value=""/>
                                <input name="submit" type="submit" value=" Search Data "/>
                                <a href="/searching" alt="How searching works in Dryad">
                                    <img src="/themes/Dryad/images/help.png" alt="How searching works in Dryad"/>
                                </a>
                                <input name="location" type="hidden" value="l2"/>
                            </p>
                        </form>
                        <!--						</xsl:if>-->
                    </div>
                </xsl:otherwise>
            </xsl:choose>

        </div>
    </xsl:template>

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
            <xsl:apply-templates/>
        </div>
    </xsl:template>


    <!-- ###################################################
  TEMPLATES THAT HIDE NORMAL DSPACE FUNCTIONALITY
  (some of these may be reinstated in the future)
  ###################################################  -->

    <!-- Related items (on item pages?) -->
    <xsl:template
            match="dri:div[@id='aspect.discovery.RelatedItems.div.test']"/>

    <!-- "old" DSpace browsing system -->
    <xsl:template
            match="dri:list[@id='org.datadryad.dspace.xmlui.aspect.browse.Navigation.list.DryadBrowse'][preceding-sibling::node()/@id='aspect.discovery.SimpleSearch.list.discovery' or following-sibling::node()/@id='aspect.discovery.SimpleSearch.list.discovery']"/>

    <!-- DSpace statistics -->
    <xsl:template
            match="dri:list[@id='aspect.statistics.Navigation.list.statistics']"/>

    <!-- Progress buttons in the submission system -->
    <xsl:template
            match="dri:list[@id='aspect.submission.StepTransformer.list.submit-progress']"/>


    <!-- capture and move notices: the message at the top of th page is dynamic to capture and move it occurs to build it dynamically and not hardcoded it. -->
    <!--xsl:template match="/dri:document/dri:body//dri:div[@id='org.datadryad.dspace.xmlui.aspect.browse.ItemViewer.div.notice'][@rend='notice']"/-->

    <xsl:template match="dri:trail">
        <span>
            <xsl:attribute name="class">
                <xsl:text>ds-trail-link </xsl:text>
                <xsl:if test="position()=1">
                    <xsl:text>first-link</xsl:text>
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
                    <!--&gt;-->
                    <!--<xsl:if test="position()=last() and $doc">-->
                        <!--<xsl:value-of select="$doc/html/head/title"/>-->
                    <!--</xsl:if>-->
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates/>
                </xsl:otherwise>
            </xsl:choose>
        </span>

    </xsl:template>

    <xsl:template match="dri:field[@id='aspect.submission.submit.OverviewStep.field.submit_next']">
        <input>
            <xsl:attribute name="name">
                <xsl:value-of select="@n"/>
            </xsl:attribute>
            <xsl:if test="@disabled">
                <xsl:attribute name="disabled">
                    <xsl:value-of select="@disabled"/>
                </xsl:attribute>
            </xsl:if>

            <xsl:attribute name="name">
                <xsl:value-of select="@n"/>
            </xsl:attribute>
            <!--<xsl:call-template name="fieldAttributes"/>-->
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
            <xsl:if test="dri:error">
                <span class="error">
                    <i18n:text>
                        <xsl:value-of select="dri:error"/>
                    </i18n:text>
                </span>
            </xsl:if>
            <!--<xsl:apply-templates />-->
        </input>
    </xsl:template>

    <!-- eliminate wierd font sizing algorithm see line 1362 in structural.xsl -->
    <xsl:template match="dri:div/dri:head" priority="3">
        <xsl:variable name="head_count" select="count(ancestor::dri:div)"/>
        <xsl:element name="h{$head_count}">
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-div-head</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates/>
        </xsl:element>
    </xsl:template>


    <!-- copied from structural.xsl line 2328, use to change listing behavior of nested references. -->
    <xsl:template match="dri:reference/dri:referenceSet[@type='summaryList']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <!-- Here we decide whether we have a hierarchical list or a flat one -->
        <xsl:choose>
            <xsl:when
                    test="descendant-or-self::dri:referenceSet/@rend='hierarchy' or ancestor::dri:referenceSet/@rend='hierarchy'">
                <ul>
                    <xsl:apply-templates select="*[not(name()='head')]" mode="summaryList"/>
                </ul>
            </xsl:when>
            <xsl:otherwise>

                <!-- we can drop javascript / css handlers in here to toggle lists of sub items... -->
                <ul class="ds-artifact-list">
                    <xsl:apply-templates select="*[not(name()='head')]" mode="summaryList"/>
                </ul>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--
        This will resolve any nested summaryList
        reference tag to a mets document and apply a new mode for nested items.

    -->
    <xsl:template match="dri:reference/dri:referenceSet[@type='summaryList']/dri:reference"
                  mode="summaryList">
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!-- Since this is a summary only grab the descriptive metadata, and the thumbnails -->
            <xsl:text>?sections=dmdSec,fileSec&amp;fileGrpTypes=THUMBNAIL</xsl:text>
            <!-- An example of requesting a specific metadata standard (MODS and QDC crosswalks only work for items)->
            <xsl:if test="@type='DSpace Item'">
                <xsl:text>&amp;dmdTypes=DC</xsl:text>
            </xsl:if>-->
        </xsl:variable>
        <xsl:comment>External Metadata URL:
            <xsl:value-of select="$externalMetadataURL"/>
        </xsl:comment>
        <li>
            <xsl:attribute name="class">
                <xsl:text>ds-artifact-item </xsl:text>
                <xsl:choose>
                    <xsl:when test="position() mod 2 = 0">even</xsl:when>
                    <xsl:otherwise>odd</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:apply-templates select="document($externalMetadataURL)//dim:dim" mode="nestedSummaryList"/>
            <xsl:apply-templates/>
        </li>
    </xsl:template>

    <!--
        This is used for subitems in groups publications (subitems may be any type).
        Alter this to change the fields shown for subitems of groups
    -->
    <xsl:template match="dim:dim" mode="nestedSummaryList">
        <xsl:variable name="type">
            <xsl:value-of select="dim:field[@element='type' and @mdschema='dc']"/>
        </xsl:variable>
        <xsl:attribute name="class">
            <xsl:text>ds-artifact-item even</xsl:text>
        </xsl:attribute>
        <xsl:variable name="doiIdentifier"
                      select=".//dim:field[@element='identifier'][@mdschema='dc'][not(@qualifier)][1]"/>
        <xsl:variable name="itemWithdrawn" select="@withdrawn"/>
        <div>
            <xsl:attribute name="class">artifact-description dataset</xsl:attribute>
            <span class="Z3988">
                <xsl:attribute name="title">
                    <xsl:call-template name="renderCOinS"/>
                </xsl:attribute>
                <xsl:text>&#160;</xsl:text>
            </span>
            <div class="artifact-title">
                <xsl:element name="a">
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="$itemWithdrawn">
                                <xsl:value-of select="ancestor::mets:METS/@OBJEDIT"/>
                            </xsl:when>
                            <xsl:when test="$doiIdentifier">
                                <xsl:text>/resource/</xsl:text>
                                <xsl:copy-of
                                        select=".//dim:field[@element='identifier'][@mdschema='dc'][not(@qualifier)]"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="ancestor::mets:METS/@OBJID"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <xsl:choose>
                        <xsl:when test="dim:field[@element='title']">
                            <xsl:value-of select="dim:field[@element='title'][1]/node()"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:element>
            </div>
        </div>
    </xsl:template>

    <xsl:template match="dim:field" mode="itemDetailView-DIM">
        <xsl:if test="not(./@qualifier = 'manuscriptNumber')">
            <tr>
                <xsl:attribute name="class">
                    <xsl:text>ds-table-row </xsl:text>
                    <xsl:if test="(position() div 2 mod 2 = 0)">even</xsl:if>
                    <xsl:if test="(position() div 2 mod 2 = 1)">odd</xsl:if>
                </xsl:attribute>
                <td>
                    <xsl:value-of select="./@mdschema"/>
                    <xsl:text>:</xsl:text>
                    <xsl:value-of select="./@element"/>
                    <xsl:if test="./@qualifier">
                        <xsl:text>.</xsl:text>
                        <xsl:value-of select="./@qualifier"/>
                    </xsl:if>
                </td>
                <td>
                    <xsl:copy-of select="./node()"/>
                </td>
                <td>
                    <xsl:value-of select="./@language"/>
                </td>
            </tr>
        </xsl:if>
    </xsl:template>

    <!--Render the edit embargo page-->
    <xsl:template match="dri:div[@id = 'aspect.administrative.item.EditItemEmbargoForm.div.edit_embargo_div']">
        <fieldset class="ds-form-list">
            <legend>
                <i18n:text>xmlui.administrative.item.EditItemEmbargoForm.legend</i18n:text>
            </legend>

            <xsl:apply-templates/>
        </fieldset>
    </xsl:template>

    <xsl:template match="dri:xref[../../@id='aspect.submission.Navigation.list.submitNow'][@rend='submitnowbutton']">
        <a class="button-link">
            <xsl:attribute name="href">
                <xsl:value-of select="@target"/>
            </xsl:attribute>
            <span>
                <xsl:apply-templates/>
            </span>
        </a>
    </xsl:template>

    <xsl:template match="dri:xref[../../@id='aspect.submission.Navigation.list.submitNow'][not(@rend)]">
        <a>
            <xsl:attribute name="href">
                <xsl:value-of select="@target"/>
            </xsl:attribute>
            <xsl:attribute name="target">_blank</xsl:attribute>
            <span>
                <xsl:apply-templates/>
            </span>
        </a>
    </xsl:template>

    <xsl:template match="dri:referenceSet[@rend='hierarchy'][@type='detailList']">
        <!-- ignore so no collection display on item pages: ticket 1351 -->
    </xsl:template>


    <!-- Overwriting the structural XSL's general paragraph processing -->
    <xsl:template match="dri:p">
        <xsl:choose>
            <!-- don't process (ignore) top item-view-toggle - ticket 1345 -->
            <xsl:when test="@rend='item-view-toggle item-view-toggle-top'"/>
            <xsl:otherwise>
                <p>
                    <xsl:call-template name="standardAttributes">
                        <xsl:with-param name="class">ds-paragraph</xsl:with-param>
                    </xsl:call-template>
                    <xsl:choose>
                        <!--  does this element have any children -->
                        <xsl:when test="child::node()">
                            <xsl:apply-templates/>
                        </xsl:when>
                        <!-- if no children are found we add a space to eliminate self closing tags -->
                        <xsl:otherwise> &#160; </xsl:otherwise>
                    </xsl:choose>
                </p>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template
            match="dri:field[@id='aspect.administrative.collection.SetupCollectionHarvestingForm.field.oai-set-comp' and @type='composite']"
            mode="formComposite" priority="2">
        <xsl:for-each select="dri:field[@type='radio']">
            <div class="ds-form-content">
                <xsl:for-each select="dri:option">
                    <input type="radio">
                        <xsl:attribute name="id">
                            <xsl:value-of select="@returnValue"/>
                        </xsl:attribute>
                        <xsl:attribute name="name">
                            <xsl:value-of select="../@n"/>
                        </xsl:attribute>
                        <xsl:attribute name="value">
                            <xsl:value-of select="@returnValue"/>
                        </xsl:attribute>
                        <xsl:if test="../dri:value[@type='option'][@option = current()/@returnValue]">
                            <xsl:attribute name="checked">checked</xsl:attribute>
                        </xsl:if>
                    </input>
                    <label>
                        <xsl:attribute name="for">
                            <xsl:value-of select="@returnValue"/>
                        </xsl:attribute>
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


    <xsl:template match="dri:field" mode="normalField">
        <xsl:variable name="confidenceIndicatorID"
                      select="concat(translate(@id,'.','_'),'_confidence_indicator')"/>
        <xsl:choose>
            <!-- TODO: this has changed drammatically (see form3.xml) -->
            <xsl:when test="@type= 'select'">
                <select>
                    <xsl:call-template name="fieldAttributes"/>
                    <xsl:apply-templates/>
                    <xsl:if test="not(dri:option)">
                        <option value=""/>
                    </xsl:if>
                </select>
            </xsl:when>
            <xsl:when test="@type= 'textarea'">
                <textarea>
                    <xsl:call-template name="fieldAttributes"/>
                    <xsl:if
                            test="ancestor::dri:div[@id='aspect.submission.StepTransformer.div.submit-describe-publication' or @id= 'aspect.submission.StepTransformer.div.submit-describe-dataset']">
                        <xsl:if test="dri:help">
                            <xsl:attribute name="title">
                                <xsl:value-of select="dri:help"/>
                            </xsl:attribute>
                        </xsl:if>
                        <xsl:if test="../dri:help">
                            <xsl:attribute name="title">
                                <xsl:value-of select="../dri:help"/>
                            </xsl:attribute>
                        </xsl:if>
                    </xsl:if>
                    <!--
                       if the cols and rows attributes are not defined we need to call
                       the tempaltes for them since they are required attributes in strict xhtml
                    -->
                    <xsl:choose>
                        <xsl:when test="not(./dri:params[@cols])">
                           <!-- <xsl:call-template name="textAreaCols"/>  -->  
                           <!-- override to Dryad specific value - maybe this can be refined in if makes things ugly-->
                           <xsl:attribute name="cols">60</xsl:attribute>
                        </xsl:when>
                    </xsl:choose>
                    <xsl:choose>
                        <xsl:when test="not(./dri:params[@rows])">
                            <xsl:call-template name="textAreaRows"/>
                        </xsl:when>
                    </xsl:choose>

                    <xsl:apply-templates/>
                    <xsl:choose>
                        <xsl:when test="./dri:value[@type='raw']">
                            <xsl:copy-of select="./dri:value[@type='raw']/node()"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:copy-of select="./dri:value[@type='default']/node()"/>
                        </xsl:otherwise>
                    </xsl:choose>
                    <xsl:if test="string-length(./dri:value) &lt; 1">
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
                        <xsl:with-param name="unlockButton"
                                        select="dri:value[@type='authority']/dri:field[@rend='ds-authority-lock']/@n"/>
                        <xsl:with-param name="unlockHelp"
                                        select="dri:value[@type='authority']/dri:field[@rend='ds-authority-lock']/dri:help"/>
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

            <!-- This is changing drammatically -->
            <xsl:when test="@type= 'checkbox' or @type= 'radio'">
                <fieldset>
                    <xsl:call-template name="standardAttributes">
                        <xsl:with-param name="class">
                            <xsl:text>ds-</xsl:text>
                            <xsl:value-of select="@type"/>
                            <xsl:text>-field </xsl:text>
                            <xsl:if test="dri:error">
                                <xsl:text>error </xsl:text>
                            </xsl:if>
                        </xsl:with-param>
                    </xsl:call-template>
                    <xsl:attribute name="id">
                        <xsl:value-of select="generate-id()"/>
                    </xsl:attribute>
                    <xsl:if test="dri:label">
                        <legend>
                            <xsl:apply-templates select="dri:label" mode="compositeComponent"/>
                        </legend>
                    </xsl:if>
                    <xsl:apply-templates/>
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
                    <xsl:if
                            test="ancestor::dri:div[@id='aspect.submission.StepTransformer.div.submit-describe-publication' or @id= 'aspect.submission.StepTransformer.div.submit-describe-dataset']">
                        <xsl:if test="dri:help">
                            <xsl:attribute name="title">
                                <xsl:value-of select="dri:help"/>
                            </xsl:attribute>
                        </xsl:if>
                        <xsl:if test="../dri:help">
                            <xsl:attribute name="title">
                                <xsl:value-of select="../dri:help"/>
                            </xsl:attribute>
                        </xsl:if>
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
                    <xsl:apply-templates/>
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

    <xsl:template
            match="dri:list[@id='aspect.submission.StepTransformer.list.journal-select-sublist']">
        <ul>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class"></xsl:with-param>
            </xsl:call-template>
            <xsl:for-each select="dri:item">
                <li>
                    <xsl:call-template name="standardAttributes">
                        <xsl:with-param name="class"></xsl:with-param>
                    </xsl:call-template>
                    <xsl:call-template name="pick-label"/>
                    <div class="ds-form-content">
                        <xsl:apply-templates/>
                        <!-- special name used in submission UI review page -->
                        <xsl:if test="@n = 'submit-review-field-with-authority'">
                            <xsl:call-template name="authorityConfidenceIcon">
                                <xsl:with-param name="confidence" select="substring-after(./@rend, 'cf-')"/>
                            </xsl:call-template>
                        </xsl:if>
                    </div>
                </li>
            </xsl:for-each>
        </ul>
    </xsl:template>


    <xsl:template
            match="dri:item[starts-with(@id, 'aspect.submission.StepTransformer.item.submission-file-') or (@id = 'aspect.submission.StepTransformer.item.bitstream-item') or (@id = 'aspect.submission.StepTransformer.item.external-item')]">
        <li>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">
                    <xsl:text>ds-form-item </xsl:text>
                    <xsl:choose>
                        <!-- Makes sure that the dark always falls on the last item -->
                        <xsl:when test="count(../dri:item) mod 2 = 0">
                            <xsl:if test="count(../dri:item) > 3">
                                <xsl:if test="(count(preceding-sibling::dri:item) mod 2 = 0)">even</xsl:if>
                                <xsl:if test="(count(preceding-sibling::dri:item) mod 2 = 1)">odd</xsl:if>
                            </xsl:if>
                        </xsl:when>
                        <xsl:when test="count(../dri:item) mod 2 = 1">
                            <xsl:if test="count(../dri:item) > 3">
                                <xsl:if test="(count(preceding-sibling::dri:item) mod 2 = 1)">even</xsl:if>
                                <xsl:if test="(count(preceding-sibling::dri:item) mod 2 = 0)">odd</xsl:if>
                            </xsl:if>
                        </xsl:when>
                    </xsl:choose>
                    <!-- The row is also tagged specially if it contains another "form" list -->
                    <xsl:if test="dri:list[@type='form']">sublist</xsl:if>
                </xsl:with-param>
            </xsl:call-template>

            <xsl:call-template name="pick-label"/>

            <div class="ds-form-content">
                <xsl:if test="dri:hi[@rend='head']">
                    <table class="submittable">
                        <tr>
                            <xsl:for-each select="dri:hi[@rend='head']">
                                <th>
                                    <xsl:apply-templates/>
                                </th>
                            </xsl:for-each>
                        </tr>
                        <tr>
                            <xsl:for-each select="dri:hi[@rend='content']">
                                <td>
                                    <xsl:apply-templates/>
                                </td>
                            </xsl:for-each>
                        </tr>
                    </table>
                </xsl:if>
                <!--Add all optional hidden fields-->
                <xsl:if test="@id = 'aspect.submission.StepTransformer.item.external-item'">
                    <xsl:apply-templates select="dri:xref"/>
                    <xsl:apply-templates select="dri:hi"/>
                </xsl:if>
                <xsl:apply-templates select="dri:field"/>
            </div>

        </li>

    </xsl:template>

    <xsl:template match="dri:field[@id='aspect.discovery.SiteViewer.field.submit']">
        <input>
            <xsl:call-template name="fieldAttributes"/>
            <xsl:if test="@type='button'">
                <xsl:attribute name="type">submit</xsl:attribute>
            </xsl:if>
            <xsl:attribute name="value">xmlui.discovery.SiteViewer.go</xsl:attribute>
            <xsl:if test="dri:value/i18n:text">
                <xsl:attribute name="i18n:attr">value</xsl:attribute>
            </xsl:if>
            <xsl:apply-templates/>
        </input>
    </xsl:template>

    <xsl:template match="dri:field[@id='aspect.discovery.SimpleSearch.field.query']">
        <xsl:variable name="query">
            <xsl:call-template name="parse-query-param">
                <xsl:with-param name="param-name">query</xsl:with-param>
            </xsl:call-template>
        </xsl:variable>
        <span>
            <xsl:value-of select="$query"/>
        </span>
        <input
                id="aspect_discovery_SimpleSearch_field_query"
                class="ds-text-field" name="query"
                type="hidden" value="{$query}"/>
    </xsl:template>

    <xsl:template match="dri:field[@id='aspect.discovery.SimpleSearch.field.submit']">
        <!-- Would fit better in Java, but trying to avoid touching discovery code -->
        <xsl:variable name="location">
            <xsl:call-template name="parse-query-param">
                <xsl:with-param name="param-name">location</xsl:with-param>
            </xsl:call-template>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$location != ''">
                <xsl:element name="input">
                    <xsl:attribute name="type">hidden</xsl:attribute>
                    <xsl:attribute name="name">location</xsl:attribute>
                    <xsl:attribute name="value">
                        <xsl:value-of select="$location"/>
                    </xsl:attribute>
                </xsl:element>
            </xsl:when>
            <xsl:otherwise>
                <input name="location" type="hidden" value="l2"/>
            </xsl:otherwise>
        </xsl:choose>
        <!-- end of what was added -->
        <input>
            <xsl:call-template name="fieldAttributes"/>
            <xsl:if test="@type='button'">
                <xsl:attribute name="type">submit</xsl:attribute>
            </xsl:if>
            <xsl:attribute name="value">xmlui.discovery.SimpleSearch.go</xsl:attribute>
            <xsl:if test="dri:value/i18n:text">
                <xsl:attribute name="i18n:attr">value</xsl:attribute>
            </xsl:if>
            <xsl:apply-templates/>
        </input>
        <a href="/searching" alt="How searching works in Dryad">
            <img src="/themes/Dryad/images/help.png" alt="How searching works in Dryad"/>
        </a>
    </xsl:template>


    <xsl:template match="dri:help" mode="help">
        <xsl:if
                test="not(ancestor::dri:div[@id='aspect.submission.StepTransformer.div.submit-describe-publication' or @id= 'aspect.submission.StepTransformer.div.submit-describe-dataset'])">
            <!--Only create the <span> if there is content in the <dri:help> node-->
            <xsl:if test="./text() or ./node()">
                <span>
                    <xsl:attribute name="class">
                        <xsl:text>field-help</xsl:text>
                    </xsl:attribute>
                    <xsl:apply-templates/>
                </span>
            </xsl:if>
        </xsl:if>
    </xsl:template>

    <xsl:template match="dri:help" mode="compositeComponent">
        <xsl:if
                test="not(ancestor::dri:div[@id='aspect.submission.StepTransformer.div.submit-describe-publication' or @id= 'aspect.submission.StepTransformer.div.submit-describe-dataset'])">
            <span class="composite-help">
                <xsl:if
                        test="ancestor::dri:div[@id='aspect.submission.StepTransformer.div.submit-describe-publication' or @id= 'aspect.submission.StepTransformer.div.submit-describe-dataset']">
                    <xsl:variable name="translatedParentId">
                        <xsl:value-of select="translate(../@id, '.', '_')"/>
                    </xsl:variable>
                    <xsl:attribute name="connectId">
                        <xsl:value-of select="$translatedParentId"/>
                    </xsl:attribute>
                    <xsl:attribute name="id"><xsl:value-of select="$translatedParentId"
                            />_tooltip
                    </xsl:attribute>
                </xsl:if>

                <xsl:apply-templates/>
            </span>
        </xsl:if>
    </xsl:template>

    <xsl:template match="dri:item[@id='aspect.submission.StepTransformer.item.data-upload-details']">
        <div class="ds-form-content">
            <i18n:text>
                <xsl:value-of select="."/>
            </i18n:text>
        </div>
    </xsl:template>

    <xsl:template match="dri:list[@id='aspect.submission.StepTransformer.list.submit-upload-file']">
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

            <xsl:apply-templates select="dri:item[@id='aspect.submission.StepTransformer.item.data-upload-details']"/>

            <table class="datafiletable">
                <tr>
                    <td>
                        <xsl:apply-templates
                                select="dri:item[@id='aspect.submission.StepTransformer.item.dataset-item']/dri:field[@type='radio']"
                                />
                    </td>
                    <td>
                        <xsl:apply-templates
                                select="dri:item[@id='aspect.submission.StepTransformer.item.dataset-item']/*[not(@type='radio')]"
                                />
                    </td>
                </tr>
                <tr>
                    <td>
                        <xsl:apply-templates
                                select="dri:item[@id='aspect.submission.StepTransformer.item.dataset-identifier']/dri:field[@type='radio']"
                                />
                    </td>
                    <td>
                        <xsl:apply-templates
                                select="dri:item[@id='aspect.submission.StepTransformer.item.dataset-identifier']/*[not(@type='radio')]"
                                />
                    </td>
                </tr>
            </table>
        </fieldset>
    </xsl:template>

    <!-- Build a single row in the bitsreams table of the item view page -->
    <xsl:template match="mets:file">
        <xsl:param name="context" select="."/>
        <tr>
            <xsl:attribute name="class">
                <xsl:text>ds-table-row </xsl:text>
                <xsl:if test="(position() mod 2 = 0)">even</xsl:if>
                <xsl:if test="(position() mod 2 = 1)">odd</xsl:if>
            </xsl:attribute>
            <td>
                <xsl:choose>
                    <xsl:when test="mets:FLocat[@LOCTYPE='URL']">
                        <a>
                            <xsl:attribute name="href">
                                <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                            </xsl:attribute>
                            <xsl:attribute name="title">
                                <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>
                            </xsl:attribute>
                            <xsl:choose>
                                <xsl:when test="string-length(mets:FLocat[@LOCTYPE='URL']/@xlink:title) > 50">
                                    <xsl:variable name="title_length"
                                                  select="string-length(mets:FLocat[@LOCTYPE='URL']/@xlink:title)"/>
                                    <xsl:value-of select="substring(mets:FLocat[@LOCTYPE='URL']/@xlink:title,1,15)"/>
                                    <xsl:text> ... </xsl:text>
                                    <xsl:value-of
                                            select="substring(mets:FLocat[@LOCTYPE='URL']/@xlink:title,$title_length - 25,$title_length)"
                                            />
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </a>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="mets:FLocat[@LOCTYPE='TXT']/@xlink:text"/>
                    </xsl:otherwise>
                </xsl:choose>
            </td>
            <!-- File size always comes in bytes and thus needs conversion -->
            <td>
                <xsl:choose>
                    <xsl:when test="@SIZE &lt; 1000">
                        <xsl:value-of select="@SIZE"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-bytes</i18n:text>
                    </xsl:when>
                    <xsl:when test="@SIZE &lt; 1000000">
                        <xsl:value-of select="substring(string(@SIZE div 1000),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-kilobytes</i18n:text>
                    </xsl:when>
                    <xsl:when test="@SIZE &lt; 1000000000">
                        <xsl:value-of select="substring(string(@SIZE div 1000000),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-megabytes</i18n:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="substring(string(@SIZE div 1000000000),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-gigabytes</i18n:text>
                    </xsl:otherwise>
                </xsl:choose>
            </td>
            <!-- Lookup File Type description in local messages.xml based on MIME Type.
     In the original DSpace, this would get resolved to an application via
     the Bitstream Registry, but we are constrained by the capabilities of METS
     and can't really pass that info through. -->
            <td>
                <xsl:call-template name="getFileTypeDesc">
                    <xsl:with-param name="mimetype">
                        <xsl:value-of select="substring-before(@MIMETYPE,'/')"/>
                        <xsl:text>/</xsl:text>
                        <xsl:value-of select="substring-after(@MIMETYPE,'/')"/>
                    </xsl:with-param>
                </xsl:call-template>
            </td>
            <td>
                <xsl:choose>
                    <xsl:when
                            test="$context/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/
                        mets:file[@GROUPID=current()/@GROUPID]">
                        <a class="image-link">
                            <xsl:attribute name="href">
                                <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                            </xsl:attribute>
                            <img alt="Thumbnail">
                                <xsl:attribute name="src">
                                    <xsl:value-of
                                            select="$context/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/
                                        mets:file[@GROUPID=current()/@GROUPID]/mets:FLocat[@LOCTYPE='URL']/@xlink:href"
                                            />
                                </xsl:attribute>
                            </img>
                        </a>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:choose>
                            <xsl:when test="mets:FLocat[@LOCTYPE='URL']">
                                <a>
                                    <xsl:attribute name="href">
                                        <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                                    </xsl:attribute>
                                    <i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-viewOpen</i18n:text>
                                </a>
                            </xsl:when>
                        </xsl:choose>
                    </xsl:otherwise>
                </xsl:choose>
            </td>
            <!-- Display the contents of 'Description' as long as at least one bitstream contains a description -->
            <xsl:if
                    test="$context/mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file/mets:FLocat/@xlink:label != ''">
                <td>
                    <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:label"/>
                </td>
            </xsl:if>

        </tr>
    </xsl:template>


    <!-- Adding button for GenBank. -->
    <xsl:template match="dri:field[@id='aspect.submission.submit.OverviewStep.field.submit_genbank']">
        <xsl:variable name="url"
                      select="//dri:field[@id='aspect.submission.submit.OverviewStep.field.genbank_url']/dri:value"/>

        <xsl:element name="input">
            <xsl:attribute name="type">button</xsl:attribute>
            <xsl:attribute name="id">aspect_submission_submit_OverviewStep_field_submit_genbank</xsl:attribute>
            <xsl:attribute name="value">Send to Gen Bank</xsl:attribute>
            <xsl:attribute name="name">submit_adddataset</xsl:attribute>
            <xsl:attribute name="onclick">javascript:return openCenterPopup('<xsl:value-of select="$url"/>', 'GenBank',
                '1000', '800');
            </xsl:attribute>
        </xsl:element>
    </xsl:template>


    <!-- RestrictItem: template used for dispalying the right page based upon the item state stored in the metadatapage dryad.itemState -->
    <xsl:template match="dri:div[@n='workflow_pendingPublicationStep']/dri:head" priority="100">
        <xsl:variable name="head_count" select="count(ancestor::dri:div)"/>
        <xsl:element name="h{$head_count}">
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-div-head</xsl:with-param>
            </xsl:call-template>
            <i18n:text>xmlui.aspect.artifactbrowser.RestrictedItem.div.pendingPublicationStep-resource.head</i18n:text>
        </xsl:element>
    </xsl:template>

    <xsl:template match="dri:div[@n='workflow_pendingPublicationStep']/dri:p[1]" priority="100">
        <p>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-paragraph</xsl:with-param>
            </xsl:call-template>
            <i18n:text>xmlui.aspect.artifactbrowser.RestrictedItem.div.pendingPublicationStep-resource.body</i18n:text>
        </p>
    </xsl:template>

    <xsl:template match="dri:div[@n='workflow_pendingPublicationStep']/dri:p[@n='item_status']" priority="100">
        <p>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-paragraph</xsl:with-param>
            </xsl:call-template>
            <i18n:text>xmlui.aspect.artifactbrowser.RestrictedItem.div.pendingPublicationStep-resource.status
            </i18n:text>
        </p>
        <p>
            <i18n:text>xmlui.aspect.artifactbrowser.RestrictedItem.div.pendingPublicationStep-resource.help</i18n:text>

        </p>

    </xsl:template>


    <xsl:template
            match="dri:body/dri:div[@id='aspect.submission.StepTransformer.div.submit-describe-dataset']/dri:list[@id='aspect.submission.StepTransformer.list.submit-describe-dataset']/dri:item/dri:field[@n='submit_cancel']">


        <input id="aspect_submission_StepTransformer_field_submit_cancel" class="ds-button-field" name="submit_cancel"
               type="submit" value="Cancel &amp; Delete">
            <xsl:attribute name="onclick">
		    <xsl:text>var x = confirm ("Data File and all its content will be deleted. Do you want to continue?");
			      if(x) return true;
		              return false;
		    </xsl:text>
            </xsl:attribute>
        </input>

    </xsl:template>


    <xsl:template
            match="dri:body/dri:div[@id='aspect.submission.StepTransformer.div.submit-describe-dataset']/dri:list[@n='submit-overview-file']/dri:item[@n='bitstream-item']/dri:hi/dri:field[@n='submit_remove_dataset']">


        <input id="aspect_submission_StepTransformer_field_submit_remove_dataset" class="ds-button-field"
               name="submit_remove_dataset" type="submit" value="Remove">
            <xsl:attribute name="onclick">
		<xsl:text>var x = confirm ("The selected file will be removed. Do you want to continue?");
			  if(x) return true;
			  return false;
		</xsl:text>
            </xsl:attribute>
        </input>

    </xsl:template>





      <!-- First submission form: added and rewrote some templates to manage the form using jquery, to lead the user through the submission -->

      <!-- First submission form: Article Status Radios -->
      <xsl:template match="dri:body/dri:div/dri:list/dri:item[@n='article_status']/dri:field[@n='article_status']">

          <br/>
          <span style="margin-left:-195px;color:gray">
              <i18n:text>
                  <xsl:value-of select="dri:help"/>
              </i18n:text>
          </span>
          <br/>
          <br/>
          <div class="radios" style="margin-left:-195px;">
              <xsl:for-each select="dri:option">
                  <input type="radio">
                      <xsl:attribute name="id">
                          <xsl:value-of select="."/>
                      </xsl:attribute>
                      <xsl:attribute name="name">
                          <xsl:value-of select="../@n"/>
                      </xsl:attribute>
                      <xsl:attribute name="value">
                          <xsl:value-of select="@returnValue"/>
                      </xsl:attribute>
                      <xsl:if test="../dri:value[@type='option'][@option = current()/@returnValue]">
                          <xsl:attribute name="checked">checked</xsl:attribute>
                      </xsl:if>
                  </input>
                  <i18n:text>
                      <xsl:value-of select="."/>
                  </i18n:text>
                  <br/>
              </xsl:for-each>
          </div>
      </xsl:template>



      <!-- First submission form: STATUS: PUBLISHED - journalID Select + Manuscript Number Edit Box -->
      <xsl:template match="dri:list[@n='doi']">
          <li id="aspect_submission_StepTransformer_list_doi">
          <table style="border: 2px solid gray; float:right; display:inline-block; margin-top:-65px; margin-right:200px; padding:10px; width:500px">
              <tr>
		<xsl:for-each select="dri:item/dri:field">
		    <xsl:variable name="currentId"><xsl:value-of select="@id"/></xsl:variable>
		    <xsl:variable name="currentName"><xsl:value-of select="@n"/></xsl:variable>
		    <xsl:attribute name="id"><xsl:value-of select="$currentName"/></xsl:attribute>
      
		    <xsl:if test="$currentName!='unknown_doi'">
			<td style='width:35%'>
			  <label class="ds-form-label-select-publication">
			      <xsl:attribute name="for">
			      <xsl:value-of select="translate($currentId,'.','_')"/>
			      </xsl:attribute>
			      <i18n:text>
			      <xsl:value-of select="dri:label"/>
			      </i18n:text>
			      <xsl:text>: </xsl:text>
			  </label>
	
			  <xsl:apply-templates select="../dri:field[@id=$currentId]"/>
			  <xsl:apply-templates select="../dri:field[@id=$currentId]/dri:error"/>
			</td>
		    </xsl:if>
		    
		    <xsl:if test="$currentName='unknown_doi'">
			<td style='font-weight:bold; border-left: 1px solid gray; padding:0px;width:5%'>&#160;&#160;&#160;&#160;&#160;&#160;OR</td>
			
			<td style='font-weight:bold; border-right: 1px solid gray;'>
			  <span style=''></span>
			  </td>
			  <td>
			  <xsl:apply-templates select="../dri:field[@id=$currentId]"/>
			  <xsl:apply-templates select="../dri:field[@id=$currentId]/dri:error"/>
			</td>
		    </xsl:if>
  
		</xsl:for-each>
	    </tr>
	  </table>
          </li>
      </xsl:template>

      <!-- First submission form: STATUS: ACCEPTED/IN REVIEW/NOT_YET_SUBMITTED -->
      <xsl:template match="dri:list/dri:item[@n='select_publication_new' or @n='select_publication_exist']">
          <li>
          <table id="status_other_than_published" style="border: 2px solid gray; float:right; display:inline-block; margin-top:-65px; margin-right:200px; padding:10px; width:500px;">
          <tr><td>
              <!--xsl:call-template name="standardAttributes">
              <xsl:with-param name="class">
                  <xsl:text>ds-form-item </xsl:text>
                  <xsl:choose>
                  <xsl:when test="position() mod 2 = 0 and not(@rend = 'odd')">even</xsl:when>
                  <xsl:otherwise>odd</xsl:otherwise>
                  </xsl:choose>
              </xsl:with-param>
              </xsl:call-template>

              <div class="ds-form-content">

              <xsl:if test="dri:field[@type='radio']">
                  <xsl:apply-templates select="dri:field[@type='radio']"/>
                  <br/>
              </xsl:if-->

              <!-- RENDER:
                  - JournalID_status_not_yet_submitted
                  - journalID_status_in_review
                  - journalID
                  - MANUSCRIPT NUMBER

              -->
              <xsl:for-each select="dri:field[@type='composite']/dri:field">
                  <tr class="selectPubSubmitTable"><td>

                  <xsl:variable name="currentId"><xsl:value-of select="@id"/></xsl:variable>
                  <xsl:variable name="currentName"><xsl:value-of select="@n"/></xsl:variable>
                  <xsl:attribute name="id"><xsl:value-of select="$currentName"/></xsl:attribute>


                      <label class="ds-form-label-select-publication">
                          <xsl:attribute name="for"><xsl:value-of select="translate($currentId,'.','_')"/></xsl:attribute>
                          <i18n:text><xsl:value-of select="dri:label"/></i18n:text>
                          <xsl:text>: </xsl:text>
                      </label>



                      <xsl:apply-templates select="../dri:field[@id=$currentId]"/>
                      <xsl:apply-templates select="../dri:field[@id=$currentId]/dri:error"/>


                  </td></tr>
              </xsl:for-each>

              <xsl:for-each select="dri:field[@type!='composite']">
                  <xsl:variable name="currentId"><xsl:value-of select="@id"/></xsl:variable>
                  <xsl:variable name="currentName"><xsl:value-of select="@n"/></xsl:variable>

                  <!-- MANUSCRIPT NUMBER STATUS ACCEPTED -->
                  <xsl:if test="$currentName!='manu_accepted-cb'">
                  <tr id="aspect_submission_StepTransformer_item_manu-number-status-accepted">
                      <td>
                      <label class="ds-form-label-select-publication">
                          <xsl:attribute name="for">
                          <xsl:value-of select="translate($currentId,'.','_')"/>
                          </xsl:attribute>
                          <i18n:text>
                          <xsl:value-of select="dri:label"/>
                          </i18n:text>
                          <xsl:text>: </xsl:text>
                      </label>
                      <xsl:apply-templates select="../dri:field[@id=$currentId]"/>
                      <xsl:apply-templates select="../dri:field[@id=$currentId]/dri:error"/>
                      </td>
                  </tr>
                  </xsl:if>

                  <!-- CHECKBOX ACCEPTEANCE STATUS ACCEPTED -->
                  <xsl:if test="$currentName='manu_accepted-cb'">
                  <tr id="aspect_submission_StepTransformer_item_manu_accepted-cb">
                      <td>
                      <xsl:apply-templates select="../dri:field[@id=$currentId]"/>
                      <xsl:apply-templates select="../dri:field[@id=$currentId]/dri:error"/>
                      </td>
                  </tr>
                  </xsl:if>



              </xsl:for-each>

              <!--/div-->
          </td></tr>
          </table>
          </li>
      </xsl:template>
     <!-- END First submission form: added and rewrote some templates to manage the form using jquery, to lead the user through the submission -->


    <!-- Quick patch to remove empty lists from options -->
    <xsl:template match="dri:options//dri:list[count(child::*)=0]" priority="5" mode="nested"></xsl:template>
    <xsl:template match="dri:options//dri:list[count(child::*)=0]" priority="5"></xsl:template>


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

        <!-- CALLING THE FUNCTION WITHOUT OVERRIDING THE OnLoad() method -->
        <script type="text/javascript">

            <xsl:text>jQuery(document).ready(function() {</xsl:text>
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
            <xsl:value-of
                    select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
            <xsl:text>'});</xsl:text>
            <xsl:text>});</xsl:text>
        </script>



    </xsl:template>




    <xsl:template match="dri:referenceSet[@type = 'related-item-detail']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <table>
            <xsl:apply-templates select="*[not(name()='head')]" mode="related-item-summary"/>
        </table>
    </xsl:template>

    <xsl:template match="dri:referenceSet[@type = 'related-item-summary']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <table>
            <xsl:apply-templates select="*[not(name()='head')]" mode="related-item-summary"/>
        </table>
    </xsl:template>
    <xsl:template match="//dri:reference" mode="related-item-summary">

        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
        </xsl:variable>
        <tr>

            <xsl:apply-templates select = "document($externalMetadataURL)" mode="related-item-summary"/>
        </tr>
    </xsl:template>

    <xsl:template match="mets:METS" mode="related-item-summary">
        <td>
            <a>
                <xsl:attribute name="href">
                    <xsl:choose>
                        <xsl:when test=".//dim:field[@element='identifier'][@mdschema='dc'][not(@qualifier)]">
                            <xsl:text>/resource/</xsl:text>
                            <xsl:copy-of select=".//dim:field[@element='identifier'][@mdschema='dc'][not(@qualifier)]"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="@OBJID"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:attribute>
                <xsl:value-of select="//dim:field[@element='title']"/>
            </a>
        </td>
    </xsl:template>


    <!-- TEST !!!!!!!! -->
    <!-- Submission file upload step. Adding jquery progres bar-->
    <!--<xsl:template match="dri:body/dri:div/dri:list/dri:item[@n='dataset-item']/dri:field[@n='dataset-file']">-->
         <!--<xsl:apply-imports/>-->

        <!--<div class="row">-->
            <!--<label for="aspect_submission_StepTransformer_field_dataset-file">Select a File to Upload</label><br />-->
                <!--<input type="file" name="dataset-file" id="aspect_submission_StepTransformer_field_dataset-file" onchange="fileSelected();"/>-->
            <!--</div>-->
            <!--<div id="fileName"></div>-->
            <!--<div id="fileSize"></div>-->
            <!--<div id="fileType"></div>-->
            <!--<div class="row">-->
            <!--<input type="button" onclick="uploadFile()" value="Upload" />-->
        <!--</div>-->
        <!--<div id="progressNumber"></div>-->
     <!--</xsl:template>-->
    <!-- END TEST !!!!!!!! -->

</xsl:stylesheet>
