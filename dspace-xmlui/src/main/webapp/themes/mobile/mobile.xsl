<?xml version="1.0" encoding="UTF-8"?>
<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<!--

    mobile.xsl
    Mobile theme 1.1 for DSpace 3.0
    Last update by Elias Tzoc <tzoce@muohio.edu>
    September 30, 2013
-->
 

<xsl:stylesheet 
    xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
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
	exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc confman">    
    <xsl:import href="../dri2xhtml.xsl"/>
    <xsl:output indent="yes"/>

    <!-- mobile variable -->
    <xsl:variable name="mobile-url" select="confman:getProperty('dspace.mobileUrl')"/>
    <xsl:variable name="dspace-url" select="confman:getProperty('dspace.url')"/>
    <xsl:variable name="page-url" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='URI']"/>

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
            <body>
                <!-- first HTML5 line -->
                <div data-role="page" data-fetch="always"> 
                    <!--
                <div id="ds-main" style="background:white">
                        The header div, complete with title, subtitle, trail and other junk. The trail is
                        built by applying a template over pageMeta's trail children. -->
                    <xsl:call-template name="buildHeader"/>
                    <!--
                    <xsl:call-template name="buildHeader"/>
                    -->

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

                </div><!-- data-role-->
            </body>
        </html>
    </xsl:template>

 <!-- The HTML head element contains references to CSS as well as embedded JavaScript code. Most of this
        information is either user-provided bits of post-processing (as in the case of the JavaScript), or
        references to stylesheets pulled directly from the pageMeta element. -->
    <xsl:template name="buildHead"><head>
	    <meta name="viewport" content="width=device-width, initial-scale=1" /> 
	    <!-- CSS generated at http://jquerymobile.com/themeroller/ -->
        <!-- If you get ocassional non-styled pages, try using absolute paths for the CSS -->
                <link rel="stylesheet">
                    <xsl:attribute name="href">
                        <xsl:value-of select="$mobile-url"/>
                        <xsl:text>/themes/mobile/lib/sc-mobile.min.css</xsl:text>
                    </xsl:attribute>
                </link>
                
        <link rel="stylesheet">
                    <xsl:attribute name="href">
                        <xsl:text>http://code.jquery.com/mobile/1.1.1/jquery.mobile.structure-1.1.1.min.css</xsl:text>
                    </xsl:attribute>
                </link>

		<!-- CSS tweaks -->
        <!-- If you get ocassional non-styled pages, try using absolute paths for the CSS -->
                <link rel="stylesheet">
                    <xsl:attribute name="href">
                        <xsl:value-of select="$mobile-url"/>
                        <xsl:text>/themes/mobile/lib/m-tweaks.css</xsl:text>
                    </xsl:attribute>
                </link>

        <script>
        <xsl:attribute name="src">
        <xsl:text>http://code.jquery.com/jquery-1.7.1.min.js</xsl:text>
        </xsl:attribute>&#160;</script>

        <script>
        <xsl:attribute name="src">
        <xsl:text>http://code.jquery.com/mobile/1.1.1/jquery.mobile-1.1.1.min.js</xsl:text>
        </xsl:attribute>&#160;</script>

        <script>
        <xsl:attribute name="src">
        <xsl:value-of select="$mobile-url"/>
        <xsl:text>/themes/mobile/lib/cookies.js</xsl:text>
        </xsl:attribute>&#160;</script>

            <!-- Add the title in -->
            <xsl:variable name="page_title" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='title']" />
            <title>
                <xsl:choose>
                        <xsl:when test="not($page_title)">
                                <xsl:text>  </xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                                <xsl:copy-of select="$page_title/node()" />
                        </xsl:otherwise>
                </xsl:choose>
            </title>
        </head>
    </xsl:template>
         

     <!-- The header (distinct from the HTML head element) contains the title, subtitle, login box and various
        placeholders for header images -->
    <xsl:template name="buildHeader">
	    <!-- new header with a data-icon linking to the homepage -->
        <div data-role="header"> 
            <h2> 
                <i18n:text>xmlui.mobile.home_mobile</i18n:text>
   	    </h2>
            <a>
                <xsl:attribute name="href">
                <xsl:value-of select="$mobile-url"/>
                </xsl:attribute>
                <xsl:attribute name="data-icon">
                <xsl:text>home</xsl:text>
                </xsl:attribute>
                <xsl:attribute name="data-iconpos">
                <xsl:text>notext</xsl:text>
                </xsl:attribute>
                <xsl:attribute name="data-direction">
                <xsl:text>reverse</xsl:text>
                </xsl:attribute>
                <xsl:text>Home</xsl:text>
	    </a>

            <!-- link to full website page -->
            <a>
                <xsl:attribute name="href">
                <xsl:value-of select="$dspace-url"/>
                <xsl:text>/</xsl:text>
                <xsl:value-of select="$page-url"/>
                </xsl:attribute>
                <xsl:attribute name="data-icon">
                <xsl:text>forward</xsl:text>
                </xsl:attribute>
                <xsl:attribute name="data-iconpos">
                <xsl:text>notext</xsl:text>
                </xsl:attribute>
                <xsl:attribute name="data-direction">
                <xsl:text>reverse</xsl:text>
                </xsl:attribute>
                <xsl:text>non-mobile view</xsl:text>
	    </a>
       </div><!-- header -->

    </xsl:template>


<!-- BEGIN front page customization  -->
    <xsl:template match="dri:body">
<div data-role="content">
        <div id="ds-body">
<xsl:apply-templates />
	<xsl:choose>
    	<xsl:when test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='title']='xmlui.general.dspace_home'">
           <h3>
               <i18n:text>xmlui.mobile.search_all</i18n:text>
           </h3>
           <form id="search" class="ds-interactive-div primary" action="{/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search' and @qualifier='advancedURL']}" method="get">
               <fieldset>
               <input id="search-basic" name="query" type="search" value="" />
               <input id="search" name="submit" type="submit" value="Go" />
               </fieldset>
           </form>

           <!-- browse ALL code 'borrowed' from ds-options -->
           <h3>
                <i18n:text>xmlui.mobile.browse_all</i18n:text>
           </h3>

           <div id="browse-front-page" data-role="controlgroup">
            <a>
                <xsl:attribute name="href">
                <xsl:value-of select="$mobile-url"/>
                <xsl:text>/browse?type=dateissued</xsl:text>
                </xsl:attribute>
                <xsl:attribute name="data-role">
                <xsl:text>button</xsl:text>
                </xsl:attribute>
                <xsl:attribute name="data-icon">
                <xsl:text>arrow-r</xsl:text>
                </xsl:attribute>
                <xsl:attribute name="data-iconpos">
                <xsl:text>right</xsl:text>
                </xsl:attribute>
                <i18n:text>xmlui.mobile.browse_date</i18n:text>
            </a>

            <a>
                <xsl:attribute name="href">
                <xsl:value-of select="$mobile-url"/>
                <xsl:text>/browse?type=author</xsl:text>
                </xsl:attribute>
                <xsl:attribute name="data-role">
                <xsl:text>button</xsl:text>
                </xsl:attribute>
                <xsl:attribute name="data-icon">
                <xsl:text>arrow-r</xsl:text>
                </xsl:attribute>
                <xsl:attribute name="data-iconpos">
                <xsl:text>right</xsl:text>
                </xsl:attribute>
                <i18n:text>xmlui.mobile.browse_author</i18n:text>
            </a>

            <a>
                <xsl:attribute name="href">
                <xsl:value-of select="$mobile-url"/>
                <xsl:text>/browse?type=title</xsl:text>
                </xsl:attribute>
                <xsl:attribute name="data-role">
                <xsl:text>button</xsl:text>
                </xsl:attribute>
                <xsl:attribute name="data-icon">
                <xsl:text>arrow-r</xsl:text>
                </xsl:attribute>
                <xsl:attribute name="data-iconpos">
                <xsl:text>right</xsl:text>
                </xsl:attribute>
				<i18n:text>xmlui.mobile.browse_title</i18n:text>
            </a>

            <a>
                <xsl:attribute name="href">
                <xsl:value-of select="$mobile-url"/>
                <xsl:text>/browse?type=subject</xsl:text>
                </xsl:attribute>
                <xsl:attribute name="data-role">
                <xsl:text>button</xsl:text>
                </xsl:attribute>
                <xsl:attribute name="data-icon">
                <xsl:text>arrow-r</xsl:text>
                </xsl:attribute>
                <xsl:attribute name="data-iconpos">
                <xsl:text>right</xsl:text>
                </xsl:attribute>
				<i18n:text>xmlui.mobile.browse_subject</i18n:text>
            </a>
          </div><!-- browse-front-page -->
          <br />
		  
		  <!-- link to full website -->
            <a href="#" data-role="button" data-icon="forward" data-iconpos="left">
                <xsl:attribute name="onclick">createCookie('viewfull','true','','$dspace.hostname');window.location='<xsl:value-of select="$dspace-url"/>';</xsl:attribute>
                <xsl:text>View full website</xsl:text>
            </a>
	</xsl:when>

</xsl:choose>
        </div>
</div><!-- data-role: content -->

    </xsl:template>
<!-- END front page customization  -->

    <!-- new footer -->
    <xsl:template name="buildFooter">
        <div data-role="footer">
        <h4>Mobile theme for DSpace</h4>
	</div>
    </xsl:template>


    <!-- From here on out come the templates for supporting elements that are contained within structural
        ones. These include head (in all its myriad forms), rich text container elements (like hi and figure),
        as well as the field tag and its related elements. The head elements are done first. -->

    <!-- The first (and most complex) case of the header tag is the one used for divisions. Since divisions can
        nest freely, their headers should reflect that. Thus, the type of HTML h tag produced depends on how
        many divisions the header tag is nested inside of. -->

    <!-- Tweaking the font-size properties for H elements -->
    <xsl:template match="dri:div/dri:head" priority="3">
        <xsl:variable name="head_count" select="count(ancestor::dri:div)"/>
        <xsl:variable name="font-sizing" select="265 - $head_count * 80 - string-length(current())"></xsl:variable>
        <xsl:element name="h3">
        <xsl:apply-templates />
        </xsl:element>
    </xsl:template>

    <xsl:template match="dri:referenceSet[@type = 'summaryList']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <!-- Here we decide whether we have a hierarchical list or a flat one -->
        <xsl:choose>
            <xsl:when test="descendant-or-self::dri:referenceSet/@rend='hierarchy' or ancestor::dri:referenceSet/@rend='hierarchy'">
                <ul data-role="listview">
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

    <!-- summaryList -->
    <xsl:template match="dri:reference" mode="summaryList">
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
        <li>
            <xsl:apply-templates select="document($externalMetadataURL)" mode="summaryList"/>
            <xsl:apply-templates />
        </li>
    </xsl:template>

    <!-- A community rendered in the summaryList pattern ... on the front page. -->
    <xsl:template name="communitySummaryList-DIM">
        <xsl:variable name="data" select="./mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim"/>
            <a href="{@OBJID}">
	            <xsl:choose>
		            <xsl:when test="string-length($data/dim:field[@element='title'][1]) &gt; 0">
		                <xsl:value-of select="$data/dim:field[@element='title'][1]"/>
		            </xsl:when>
		            <xsl:otherwise>
		                <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
		            </xsl:otherwise>
	            </xsl:choose>
            </a>
    </xsl:template>


    <xsl:template name="itemSummaryList-DIM">
        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
            mode="itemSummaryList-DIM"/>
        <!-- Generate the thumbnail, if present, from the file section -->
        <!--
        <xsl:apply-templates select="./mets:fileSec" mode="artifact-preview"/>
        -->
    </xsl:template>

<!-- BEGIN short item record metadata -->
    <!-- Generate the info about the item from the metadata section -->
    <xsl:template match="dim:dim" mode="itemSummaryView-DIM">

<!-- AddThis Button BEGIN -->
<div id="addthis">
                <a>
                    <xsl:attribute name="href">
                        <xsl:text>http://www.addthis.com/bookmark.php?v=250&amp;username=xa-4d35e7801c9278b4</xsl:text>
                    </xsl:attribute>
                    <xsl:attribute name="class">
                        <xsl:text>addthis_button</xsl:text>
                    </xsl:attribute>
                    <img>
                        <xsl:attribute name="src">
                            <xsl:text>http://s7.addthis.com/static/btn/lg-share-en.gif</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="width">
                            <xsl:text>125</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="height">
                            <xsl:text>16</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="style">
                            <xsl:text>border:0</xsl:text>
                        </xsl:attribute>
                    </img>
                </a>
                <script type="text/javascript">
                    <xsl:attribute name="src">
                        <xsl:text>http://s7.addthis.com/js/250/addthis_widget.js#username=</xsl:text>
                    </xsl:attribute>
                    <xsl:text>username</xsl:text>
                </script>
            </div>
<!-- AddThis Button END -->

<div id="metadata-wrapper">
<div class="metadata-view">
                <span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-title</i18n:text>: </span>
                    <xsl:choose>
                        <xsl:when test="count(dim:field[@element='title'][not(@qualifier)]) &gt; 1">
                            <xsl:for-each select="dim:field[@element='title'][not(@qualifier)]">
                                <xsl:value-of select="./node()"/>
                                <xsl:if test="count(following-sibling::dim:field[@element='title'][not(@qualifier)]) != 0">
                                            <xsl:text>; </xsl:text><br/>
                                        </xsl:if>
                            </xsl:for-each>
                        </xsl:when>
                         <xsl:when test="count(dim:field[@element='title'][not(@qualifier)]) = 1">
                            <xsl:value-of select="dim:field[@element='title'][not(@qualifier)][1]/node()"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                        </xsl:otherwise>
                    </xsl:choose>
</div>

            <xsl:if test="dim:field[@element='contributor'][@qualifier='author'] or dim:field[@element='creator'] or dim:field[@element='contributor']">
<div class="metadata-view">
                        <span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-author</i18n:text>: </span>
                            <xsl:choose>
                                <xsl:when test="dim:field[@element='contributor'][@qualifier='author']">
                                    <xsl:for-each select="dim:field[@element='contributor'][@qualifier='author']">
                                        <xsl:copy-of select="node()"/>
                                        <xsl:if test="count(following-sibling::dim:field[@element='contributor'][@qualifier='author']) != 0">
                                            <xsl:text>; </xsl:text>
                                        </xsl:if>
                                    </xsl:for-each>
                                </xsl:when>
                                <xsl:when test="dim:field[@element='creator']">
                                    <xsl:for-each select="dim:field[@element='creator']">
                                        <xsl:copy-of select="node()"/>
                                        <xsl:if test="count(following-sibling::dim:field[@element='creator']) != 0">
                                            <xsl:text>; </xsl:text>
                                        </xsl:if>
                                    </xsl:for-each>
                                </xsl:when>
                                <xsl:when test="dim:field[@element='contributor']">
                                    <xsl:for-each select="dim:field[@element='contributor']">
                                        <xsl:copy-of select="node()"/>
                                        <xsl:if test="count(following-sibling::dim:field[@element='contributor']) != 0">
                                            <xsl:text>; </xsl:text>
                                        </xsl:if>
                                    </xsl:for-each>
                                </xsl:when>
                                <xsl:otherwise>
                                    <i18n:text>xmlui.dri2xhtml.METS-1.0.no-author</i18n:text>
                                </xsl:otherwise>
                            </xsl:choose>
</div>
            </xsl:if>
            <xsl:if test="dim:field[@element='description' and @qualifier='abstract']">
<div class="metadata-view">
                        <span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-abstract</i18n:text>: </span>
                        <xsl:if test="count(dim:field[@element='description' and @qualifier='abstract']) &gt; 1">
                                <hr class="metadata-seperator"/>
                        </xsl:if>
                        <xsl:for-each select="dim:field[@element='description' and @qualifier='abstract']">
                                <xsl:copy-of select="./node()"/>
                                <xsl:if test="count(following-sibling::dim:field[@element='description' and @qualifier='abstract']) != 0">
                                <hr class="metadata-seperator"/>
                            </xsl:if>
                        </xsl:for-each>
                        <xsl:if test="count(dim:field[@element='description' and @qualifier='abstract']) &gt; 1">
                                <hr class="metadata-seperator"/>
                        </xsl:if>
</div>
            </xsl:if>

            <xsl:if test="dim:field[@element='description' and not(@qualifier)]">
<div class="metadata-view">
                        <span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-description</i18n:text>: </span>
                        <xsl:if test="count(dim:field[@element='description' and not(@qualifier)]) &gt; 1 and not(count(dim:field[@element='description' and @qualifier='abstract']) &gt; 1)">
                                <hr class="metadata-seperator"/>
                        </xsl:if>
                        <xsl:for-each select="dim:field[@element='description' and not(@qualifier)]">
                                <xsl:copy-of select="./node()"/>
                                <xsl:if test="count(following-sibling::dim:field[@element='description' and not(@qualifier)]) != 0">
                                <hr class="metadata-seperator"/>
                            </xsl:if>
                        </xsl:for-each>
                        <xsl:if test="count(dim:field[@element='description' and not(@qualifier)]) &gt; 1">
                                <hr class="metadata-seperator"/>
                        </xsl:if>
</div>
            </xsl:if>

            <xsl:if test="dim:field[@element='identifier' and @qualifier='uri']">
<div class="metadata-view">
                        <span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-uri</i18n:text>: </span>
                                <xsl:for-each select="dim:field[@element='identifier' and @qualifier='uri']">
                                    <a>
                                        <xsl:attribute name="href">
                                            <xsl:copy-of select="./node()"/>
                                        </xsl:attribute>
                                        <xsl:copy-of select="./node()"/>
                                    </a>
                                    <xsl:if test="count(following-sibling::dim:field[@element='identifier' and @qualifier='uri']) != 0">
                                        <br/>
                                    </xsl:if>
                            </xsl:for-each>
</div>
            </xsl:if>
            <xsl:if test="dim:field[@element='date' and @qualifier='issued'] or dim:field[@element='published']">
<div class="metadata-view">
                        <span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-date</i18n:text>: </span>
                             <xsl:choose>
                             <xsl:when test="count(dim:field[@element='date' and @qualifier='published']) != 0">
                                <xsl:for-each select="dim:field[@element='date' and @qualifier='published']">
                                        <xsl:copy-of select="substring(./node(),1,10)"/>
                                         <xsl:if test="count(following-sibling::dim:field[@element='date' and @qualifier='published']) != 0">
                                        <br />
                                         </xsl:if>
                                </xsl:for-each>
                             </xsl:when>
                             <xsl:otherwise>
                                <xsl:for-each select="dim:field[@element='date' and @qualifier='issued']">
                                        <xsl:copy-of select="substring(./node(),1,10)"/>
                                         <xsl:if test="count(following-sibling::dim:field[@element='date' and @qualifier='issued']) != 0">
                                        <br />
                                         </xsl:if>
                                </xsl:for-each>
                             </xsl:otherwise>
                             </xsl:choose>
</div>
            </xsl:if>

            <xsl:if test="dim:field[@element='title'][not(@qualifier)]">
<div class="metadata-view">
                        <span class="bold"><i18n:text>xmlui.mobile.related_google_scholar</i18n:text></span>
                                <xsl:for-each select="dim:field[@element='title'][not(@qualifier)]">
                                <xsl:variable name="v1" select="./node()"/>
                                <xsl:variable name="v1" select="translate($v1,'-', ' ')"/>
                                <xsl:variable name="v1" select="translate($v1,'/', ' ')"/>
                                <xsl:variable name="v1" select="translate($v1,' ', '+')"/>
                                <a>
                                    <xsl:attribute name="href">
                                    <xsl:text>http://scholar.google.com/scholar?q=</xsl:text>
                                    <xsl:value-of select="$v1"/>
                                    </xsl:attribute>
                                    <xsl:attribute name="target">
                                    <xsl:text>_blank</xsl:text>
                                    </xsl:attribute>
                                    <i18n:text>xmlui.mobile.items_in_google_scholar</i18n:text>
                                </a>
                            </xsl:for-each>
</div>
            </xsl:if>

</div><!-- metadata wrapper -->

    </xsl:template>
<!-- END short item record metadata -->


<!-- modifying search box properties in community and collection pages -->
<xsl:template match="dri:field[@id='aspect.artifactbrowser.CommunityViewer.field.query']">
<input id="aspect_artifactbrowser_CommunityViewer_field_query" class="ds-text-field" name="query" type="search" value="" /> 
</xsl:template>

<xsl:template match="dri:field[@id='aspect.artifactbrowser.CollectionViewer.field.query']">
<input id="aspect_artifactbrowser_CollectionViewer_field_query" class="ds-text-field" name="query" type="search" value="" /> 
</xsl:template>

<!-- Forcing UL elements to display with listview properties -->
		<xsl:template match="dri:div[@id='aspect.artifactbrowser.CollectionViewer.div.collection-browse']">
        <ul data-role="listview">
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-simple-list</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="*[not(name()='head')]" mode="nested"/>
        </ul>
		</xsl:template>

		<xsl:template match="dri:div[@id='aspect.artifactbrowser.CommunityViewer.div.community-browse']">
        <ul data-role="listview">
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-simple-list</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="*[not(name()='head')]" mode="nested"/>
        </ul>
		</xsl:template>

        <!-- Generate the bitstream information from the file section -->
        <xsl:template match="mets:fileGrp[@USE='CONTENT']">
        <xsl:param name="context"/>
        <xsl:param name="primaryBitstream" select="-1"/>

        <h2><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-head</i18n:text></h2>
            <xsl:choose>
                <!-- If one exists and it's of text/html MIME type, only display the primary bitstream -->
                <xsl:when test="mets:file[@ID=$primaryBitstream]/@MIMETYPE='text/html'">
                    <xsl:apply-templates select="mets:file[@ID=$primaryBitstream]">
                        <xsl:with-param name="context" select="$context"/>
                    </xsl:apply-templates>
                </xsl:when>
                <!-- Otherwise, iterate over and display all of them -->
                <xsl:otherwise>
                    <xsl:apply-templates select="mets:file">
                        <!--Do not sort any more bitstream order can be changed-->
                        <!--<xsl:sort data-type="number" select="boolean(./@ID=$primaryBitstream)" order="descending" />-->
                        <!--<xsl:sort select="mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>-->
                        <xsl:with-param name="context" select="$context"/>
                    </xsl:apply-templates>
                </xsl:otherwise>
            </xsl:choose>
    </xsl:template>


    <!-- Build a single row in the bitstreams table of the item view page -->
    <xsl:template match="mets:file">
        <xsl:param name="context" select="."/>
    <div class="file-wrapper">
    <div class="thumbnail-wrapper">
                <xsl:choose>
                    <xsl:when test="$context/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/
                        mets:file[@GROUPID=current()/@GROUPID]">
                        <a class="image-link">
                            <xsl:attribute name="href">
                        <!-- redirect to root folder -->
                        <xsl:variable name="url-1" select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                        <xsl:variable name="url-1" select="substring-after($url-1,'')"/>
                        <xsl:value-of select="$url-1"/>
                        </xsl:attribute>
                        <xsl:attribute name="target">
                        <xsl:text>_blank</xsl:text>
                        </xsl:attribute>
                            <img alt="Thumbnail">
                                <xsl:attribute name="src">
                                    <xsl:value-of select="$context/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/
                                        mets:file[@GROUPID=current()/@GROUPID]/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                                </xsl:attribute>
                            </img>
                        </a>
                    </xsl:when>
                    <xsl:otherwise>
                        <a class="image-link">
                            <xsl:attribute name="href">
                            <!-- redirect to root folder -->
                           <xsl:variable name="url-1" select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                           <xsl:variable name="url-1" select="substring-after($url-1,'')"/>
                           <xsl:value-of select="$url-1"/>
                           </xsl:attribute>
                           <xsl:attribute name="target">
                           <xsl:text>_blank</xsl:text>
                           </xsl:attribute>
                            <img alt="Thumbnail">
                                <xsl:attribute name="src">
                                <xsl:variable name="request-uri" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath']"/>
                                <xsl:text>/themes/mobile/lib/images/default-thumbnail.png</xsl:text>
                                </xsl:attribute>
                            </img>
                        </a>
                    </xsl:otherwise>
                </xsl:choose>
		</div>          


<div class="file-metadata">
            <div>
                <a>
                    <xsl:attribute name="href">
                    <!-- redirect to root folder -->
                    <xsl:variable name="url-1" select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                    <xsl:variable name="url-1" select="substring-after($url-1,'')"/>
                    <xsl:value-of select="$url-1"/>
                    </xsl:attribute>
                    <xsl:attribute name="target">
                    <xsl:text>_blank</xsl:text>
                    </xsl:attribute>
                    <xsl:attribute name="title">
                        <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>
                    </xsl:attribute>
                        <i18n:text>xmlui.mobile.download</i18n:text>

                <xsl:text> (</xsl:text>
                <xsl:choose>
                    <xsl:when test="@SIZE &lt; 1024">
                        <xsl:value-of select="@SIZE"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-bytes</i18n:text>
                    </xsl:when>
                    <xsl:when test="@SIZE &lt; 1024 * 1024">
                        <xsl:value-of select="substring(string(@SIZE div 1024),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-kilobytes</i18n:text>
                    </xsl:when>
                    <xsl:when test="@SIZE &lt; 1024 * 1024 * 1024">
                        <xsl:value-of select="substring(string(@SIZE div (1024 * 1024)),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-megabytes</i18n:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="substring(string(@SIZE div (1024 * 1024 * 1024)),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-gigabytes</i18n:text>
                    </xsl:otherwise>
                </xsl:choose>
                <xsl:text>)</xsl:text>

                </a>
            </div>

            <div>
                    <xsl:choose>
                        <xsl:when test="string-length(mets:FLocat[@LOCTYPE='URL']/@xlink:title) > 25">
                            <xsl:variable name="title_length" select="string-length(mets:FLocat[@LOCTYPE='URL']/@xlink:title)"/>
                            <xsl:value-of select="substring(mets:FLocat[@LOCTYPE='URL']/@xlink:title,1,15)"/>
                            <xsl:text>...</xsl:text>
                            <xsl:value-of select="substring(mets:FLocat[@LOCTYPE='URL']/@xlink:title,$title_length - 7,$title_length)"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>
                        </xsl:otherwise>
                    </xsl:choose>

            </div>

            <!-- Lookup File Type description in local messages.xml based on MIME Type.
                In the original DSpace, this would get resolved to an application via
                the Bitstream Registry, but we are constrained by the capabilities of METS
                and can't really pass that info through. -->
            <div>
              <xsl:call-template name="getFileTypeDesc">
                <xsl:with-param name="mimetype">
                  <xsl:value-of select="substring-before(@MIMETYPE,'/')"/>
                  <xsl:text>/</xsl:text>
                  <xsl:value-of select="substring-after(@MIMETYPE,'/')"/>
                </xsl:with-param>
              </xsl:call-template>
            </div>

        </div>


</div>
    </xsl:template>

<!-- removing elements for a clean/basic mobile page -->
<!-- search box on front page -->
<xsl:template match="dri:div[@n='front-page-search']">&#160;
</xsl:template>

<!-- community recent submission -->
<xsl:template match="dri:div[@n='community-recent-submission']">&#160;
</xsl:template>

<!-- ds-options -->
<!-- the browse section is being implemented in the front page customization -->
<xsl:template match="dri:options">&#160;
</xsl:template>

</xsl:stylesheet>
