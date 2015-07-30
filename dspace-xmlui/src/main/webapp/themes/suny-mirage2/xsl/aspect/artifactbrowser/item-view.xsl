
<!--
    Rendering specific to the item display page.

    Author: art.lowel at atmire.com
    Author: lieven.droogmans at atmire.com
    Author: ben at atmire.com
    Author: Alexey Maslov

-->

<xsl:stylesheet
    xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
    xmlns:dri="http://di.tamu.edu/DRI/1.0/"
    xmlns:mets="http://www.loc.gov/METS/"
    xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
    xmlns:xlink="http://www.w3.org/TR/xlink/"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:atom="http://www.w3.org/2005/Atom"
    xmlns:ore="http://www.openarchives.org/ore/terms/"
    xmlns:oreatom="http://www.openarchives.org/ore/atom/"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:xalan="http://xml.apache.org/xalan"
    xmlns:encoder="xalan://java.net.URLEncoder"
    xmlns:util="org.dspace.app.xmlui.utils.XSLUtils"
    xmlns:jstring="java.lang.String"
    xmlns:rights="http://cosimo.stanford.edu/sdr/metsrights/"
    xmlns:confman="org.dspace.core.ConfigurationManager"
    xmlns:url="http://whatever/java/java.net.URLEncoder"
    exclude-result-prefixes="xalan encoder i18n dri mets dim xlink xsl util jstring rights confman">

    <xsl:output indent="yes"/>

    <!--
        baseurl including scheme, servername and port with no trailing slash.
    -->
    <xsl:variable name="baseurl">
        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='scheme']"/>
        <xsl:text>://</xsl:text>
        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='serverName']"/>
        <xsl:text>:</xsl:text>
        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='serverPort']"/>
    </xsl:variable>

    <xsl:template name="itemSummaryView-DIM">
        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
        mode="itemSummaryView-DIM"/>

        <xsl:copy-of select="$SFXLink" />

        <!-- Generate the Creative Commons license information from the file section (DSpace deposit license hidden by default)-->
        <xsl:if test="./mets:fileSec/mets:fileGrp[@USE='CC-LICENSE' or @USE='LICENSE']">
            <div class="license-info table">
                <p>
                    <i18n:text>xmlui.dri2xhtml.METS-1.0.license-text</i18n:text>
                </p>
                <ul class="list-unstyled">
                    <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CC-LICENSE' or @USE='LICENSE']" mode="simple"/>
                </ul>
            </div>
        </xsl:if>


    </xsl:template>

    <!-- An item rendered in the detailView pattern, the "full item record" view of a DSpace item in Manakin. -->
    <xsl:template name="itemDetailView-DIM">
        <!-- Output all of the metadata about the item from the metadata section -->
        <xsl:apply-templates select="mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
                             mode="itemDetailView-DIM"/>

        <!-- SUNY not show LICENSE / CC-LICENSE in file list -->
        <!-- Generate the bitstream information from the file section -->
        <xsl:choose>
            <xsl:when test="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL' or @USE='LICENSE']/mets:file">
                <h3><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-head</i18n:text></h3>
                <div class="file-list">
                    <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']">
                        <xsl:with-param name="context" select="."/>
                        <xsl:with-param name="primaryBitstream" select="./mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"/>
                    </xsl:apply-templates>
                </div>
            </xsl:when>
            <!-- Special case for handling ORE resource maps stored as DSpace bitstreams -->
            <xsl:when test="./mets:fileSec/mets:fileGrp[@USE='ORE']">
                <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='ORE']"/>
            </xsl:when>
            <xsl:otherwise>
                <h2><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-head</i18n:text></h2>
                <table class="ds-table file-list">
                    <tr class="ds-table-header-row">
                        <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-file</i18n:text></th>
                        <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-size</i18n:text></th>
                        <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-format</i18n:text></th>
                        <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-view</i18n:text></th>
                    </tr>
                    <tr>
                        <td colspan="4">
                            <p><i18n:text>xmlui.dri2xhtml.METS-1.0.item-no-files</i18n:text></p>
                        </td>
                    </tr>
                </table>
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>


    <xsl:template match="dim:dim" mode="itemSummaryView-DIM">
        <div class="item-summary-view-metadata">
        <xsl:call-template name="itemSummaryView-DIM-title"/>
            <xsl:choose>
                <xsl:when test="confman:getProperty('mirage2','snazy') = 'true'">
                    <div class="col-sm-12">
                        <!-- Add a snazy presentation section -->
                        <xsl:call-template name="itemSummaryView-DIM-file-section-snazy"/>

                        <div class="row">
                            <!-- Left Column -->
                            <div class="col-sm-4">
                                <xsl:call-template name="itemSummaryView-DIM-subject"/>
                                <xsl:call-template name="itemSummaryView-DIM-abstract"/>
                                <xsl:call-template name="itemSummaryView-DIM-description"/>
                                <xsl:call-template name="itemSummaryView-DIM-URI"/>
                                <xsl:call-template name="itemSummaryView-collections"/>
                                <xsl:call-template name="itemSummaryView-DIM-publisher"/>
                                <xsl:call-template name="itemSummaryView-DIM-first-line-of-text"/>
                                <xsl:call-template name="itemSummaryView-DIM-first-line-of-chorus"/>


                                <div class="row">
                                    <div class="col-xs-6 col-sm-12">
                                        <xsl:call-template name="itemSummaryView-DIM-thumbnail"/>
                                    </div>
                                    <div class="col-xs-6 col-sm-12">
                                        <xsl:call-template name="itemSummaryView-DIM-file-section"/>
                                    </div>
                                </div>
                                <xsl:call-template name="itemSummaryView-DIM-date"/>
                                <xsl:call-template name="itemSummaryView-DIM-authors"/>
                                <xsl:if test="$ds_item_view_toggle_url != ''">
                                    <xsl:call-template name="itemSummaryView-show-full"/>
                                </xsl:if>
                            </div>

                            <!-- Right Column -->
                            <div class="col-sm-8">
                                <xsl:apply-templates select="." mode="itemDetailView-DIM"/>
                            </div>
                        </div>
                    </div>
                </xsl:when>
                <xsl:otherwise>
                    <div class="row">
                        <div class="col-sm-4">
                            <div class="row">
                                <div class="col-xs-6 col-sm-12">
                                    <xsl:call-template name="itemSummaryView-DIM-thumbnail"/>
                                </div>
                                <div class="col-xs-6 col-sm-12">
                                    <xsl:call-template name="itemSummaryView-DIM-file-section"/>
                                </div>
                            </div>
                            <xsl:call-template name="itemSummaryView-DIM-date"/>
                            <xsl:call-template name="itemSummaryView-DIM-authors"/>
                            <xsl:if test="$ds_item_view_toggle_url != ''">
                                <xsl:call-template name="itemSummaryView-show-full"/>
                            </xsl:if>
                        </div>
                        <div class="col-sm-8">
                            <xsl:call-template name="itemSummaryView-DIM-subject"/>
                            <xsl:call-template name="itemSummaryView-DIM-abstract"/>
                            <xsl:call-template name="itemSummaryView-DIM-description"/>
                            <xsl:call-template name="itemSummaryView-DIM-URI"/>
                            <xsl:call-template name="itemSummaryView-collections"/>
                            <xsl:call-template name="itemSummaryView-DIM-publisher"/>
                            <xsl:call-template name="itemSummaryView-DIM-first-line-of-text"/>
                            <xsl:call-template name="itemSummaryView-DIM-first-line-of-chorus"/>
                        </div>
                    </div>

                    <!-- Show the files-in-this-item Thumbnails for SUNY... when multiple images -->
                    <!-- Generate the bitstream information from the file section -->
                    <xsl:choose>
                        <xsl:when test="(count(//mets:fileGrp[@USE='THUMBNAIL']/mets:file) &gt; 1) and //mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL' or @USE='LICENSE']/mets:file">
                            <div class="row">
                            <h3><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-head</i18n:text></h3>
                            <div class="file-list">
                                <xsl:apply-templates select="//mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']">
                                    <xsl:with-param name="context" select="//mets:METS"/>
                                    <xsl:with-param name="primaryBitstream" select="//mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"/>
                                </xsl:apply-templates>
                            </div>
                        </div>
                        </xsl:when>
                    </xsl:choose>
                    <!-- End show thumbnails for SUNY -->

                </xsl:otherwise>
            </xsl:choose>
        </div>
    </xsl:template>

    <xsl:template name="itemSummaryView-DIM-title">
        <xsl:choose>
            <xsl:when test="count(dim:field[@element='title'][not(@qualifier)]) &gt; 1">
                <h2 class="page-header first-page-header">
                    <xsl:value-of select="dim:field[@element='title'][not(@qualifier)][1]/node()"/>
                </h2>
                <div class="simple-item-view-other">
                    <p class="lead">
                        <xsl:for-each select="dim:field[@element='title'][not(@qualifier)]">
                            <xsl:if test="not(position() = 1)">
                                <xsl:value-of select="./node()"/>
                                <xsl:if test="count(following-sibling::dim:field[@element='title'][not(@qualifier)]) != 0">
                                    <xsl:text>; </xsl:text>
                                    <br/>
                                </xsl:if>
                            </xsl:if>

                        </xsl:for-each>
                    </p>
                </div>
            </xsl:when>
            <xsl:when test="count(dim:field[@element='title'][not(@qualifier)]) = 1">
                <h2 class="page-header first-page-header">
                    <xsl:value-of select="dim:field[@element='title'][not(@qualifier)][1]/node()"/>
                </h2>
            </xsl:when>
            <xsl:otherwise>
                <h2 class="page-header first-page-header">
                    <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                </h2>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="itemSummaryView-DIM-thumbnail">
        <div class="thumbnail">
            <xsl:choose>
                <xsl:when test="//mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']">
                    <xsl:variable name="src">
                        <xsl:choose>
                            <xsl:when test="/mets:METS/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/mets:file[@GROUPID=../../mets:fileGrp[@USE='CONTENT']/mets:file[@GROUPID=../../mets:fileGrp[@USE='THUMBNAIL']/mets:file/@GROUPID][1]/@GROUPID]">
                                <xsl:value-of
                                        select="/mets:METS/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/mets:file[@GROUPID=../../mets:fileGrp[@USE='CONTENT']/mets:file[@GROUPID=../../mets:fileGrp[@USE='THUMBNAIL']/mets:file/@GROUPID][1]/@GROUPID]/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of
                                        select="//mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:variable>
                    <img alt="Thumbnail">
                        <xsl:attribute name="src">
                            <xsl:value-of select="$src"/>
                        </xsl:attribute>
                    </img>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template name="getFileFormatIcon">
                        <xsl:with-param name="mimetype">
                            <xsl:value-of select="/mets:METS/mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file/@MIMETYPE"/>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:otherwise>
            </xsl:choose>
        </div>
    </xsl:template>

    <xsl:template name="itemSummaryView-DIM-abstract">
        <xsl:if test="dim:field[@element='description' and @qualifier='abstract']">
            <div class="simple-item-view-description item-page-field-wrapper table">
                <h5 class="visible-xs"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-abstract</i18n:text></h5>
                <div>
                    <xsl:for-each select="dim:field[@element='description' and @qualifier='abstract']">
                        <xsl:choose>
                            <xsl:when test="node()">
                                <xsl:copy-of select="node()"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:text>&#160;</xsl:text>
                            </xsl:otherwise>
                        </xsl:choose>
                        <xsl:if test="count(following-sibling::dim:field[@element='description' and @qualifier='abstract']) != 0">
                            <div class="spacer">&#160;</div>
                        </xsl:if>
                    </xsl:for-each>
                    <xsl:if test="count(dim:field[@element='description' and @qualifier='abstract']) &gt; 1">
                        <div class="spacer">&#160;</div>
                    </xsl:if>
                </div>
            </div>
        </xsl:if>
    </xsl:template>

    <xsl:template name="itemSummaryView-DIM-description">
        <xsl:if test="dim:field[@element='description' and not(@qualifier)]">
            <div class="simple-item-view-description item-page-field-wrapper table">
                <h5><i18n:text>xmlui.metadata.dc.description</i18n:text></h5>
                <div>
                    <xsl:for-each select="dim:field[@element='description' and not(@qualifier)]">
                        <xsl:choose>
                            <xsl:when test="node()">
                                <xsl:copy-of select="node()"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:text>&#160;</xsl:text>
                            </xsl:otherwise>
                        </xsl:choose>
                        <xsl:if test="count(following-sibling::dim:field[@element='description' and not(@qualifier)]) != 0">
                            <div class="spacer">&#160;</div>
                        </xsl:if>
                    </xsl:for-each>
                    <xsl:if test="count(dim:field[@element='description' and not(@qualifier)]) &gt; 1">
                        <div class="spacer">&#160;</div>
                    </xsl:if>
                </div>
            </div>
        </xsl:if>
    </xsl:template>

    <xsl:template name="itemSummaryView-DIM-authors">
        <xsl:if test="dim:field[@element='contributor'][@qualifier='author' and descendant::text()] or dim:field[@element='creator' and descendant::text()] or dim:field[@element='contributor' and descendant::text()]">
            <div class="simple-item-view-authors item-page-field-wrapper table">
                <h5><i18n:text>xmlui.dri2xhtml.METS-1.0.item-author</i18n:text></h5>
                <xsl:choose>
                    <!-- contributor.composer row (Fredonia) -->
                    <xsl:when test="dim:field[@element='contributor'][@qualifier='composer' and descendant::text()]">
                        <span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-composer</i18n:text>:</span>
                        <span>
                            <xsl:for-each select="dim:field[@element='contributor' and @qualifier='composer']">
                                <xsl:copy-of select="node()"/>
                                <xsl:if test="count(following-sibling::dim:field[@element='contributor' and @qualifier='composer']) != 0">
                                    <xsl:text>; </xsl:text>
                                </xsl:if>
                            </xsl:for-each>
                        </span>

                        <!-- Display the author as author-of-text -->
                        <xsl:if test="dim:field[@element='contributor'][@qualifier='author']">
                            <br/>
                            <span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-author-of-text</i18n:text>:</span>
                            <span>
                                <xsl:for-each select="dim:field[@element='contributor' and @qualifier='author']">
                                    <xsl:copy-of select="node()"/>
                                    <xsl:if test="count(following-sibling::dim:field[@element='contributor' and @qualifier='author']) != 0">
                                        <xsl:text>; </xsl:text>
                                    </xsl:if>
                                </xsl:for-each>
                            </span>
                        </xsl:if>

                        <!-- Display a Corporate Author -->
                        <xsl:if test="dim:field[@element='contributor'][@qualifier='corporate-author']">
                            <br/>
                            <span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-author-corporate</i18n:text>:</span>
                            <span>
                                <xsl:for-each select="dim:field[@element='contributor' and @qualifier='corporate-author']">
                                    <xsl:copy-of select="node()"/>
                                    <xsl:if test="count(following-sibling::dim:field[@element='contributor' and @qualifier='corporate-author']) != 0">
                                        <xsl:text>; </xsl:text>
                                    </xsl:if>
                                </xsl:for-each>
                            </span>
                        </xsl:if>
                    </xsl:when>

                    <xsl:when test="dim:field[@element='contributor'][@qualifier='author']">
                        <xsl:for-each select="dim:field[@element='contributor'][@qualifier='author']">
                            <div>
                                <xsl:if test="@authority">
                                    <xsl:attribute name="class"><xsl:text>ds-dc_contributor_author-authority</xsl:text></xsl:attribute>
                                </xsl:if>
                                <xsl:copy-of select="node()"/>
                            </div>
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
                            <div>
                                <xsl:copy-of select="node()"/>
                            </div>

                        </xsl:for-each>
                    </xsl:when>
                    <xsl:otherwise>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.no-author</i18n:text>
                    </xsl:otherwise>
                </xsl:choose>
            </div>
        </xsl:if>
    </xsl:template>

    <xsl:template name="itemSummaryView-DIM-URI">
        <xsl:if test="dim:field[@element='identifier' and @qualifier='uri' and descendant::text()]">
            <div class="simple-item-view-uri item-page-field-wrapper table">
                <h5><i18n:text>xmlui.dri2xhtml.METS-1.0.item-uri</i18n:text></h5>
                <span>
                    <xsl:for-each select="dim:field[@element='identifier' and @qualifier='uri']">
                        <a>
                            <xsl:attribute name="href">
                                <xsl:copy-of select="./node()"/>
                            </xsl:attribute>
                            <span class="glyphicon glyphicon-link"></span>
                            <xsl:text> </xsl:text>
                            <xsl:copy-of select="./node()"/>
                        </a>
                        <xsl:if test="count(following-sibling::dim:field[@element='identifier' and @qualifier='uri']) != 0">
                            <br/>
                        </xsl:if>
                    </xsl:for-each>
                </span>
            </div>
        </xsl:if>
    </xsl:template>

    <xsl:template name="itemSummaryView-DIM-date">
        <xsl:if test="dim:field[@element='date' and @qualifier='issued' and descendant::text()]">
            <div class="simple-item-view-date word-break item-page-field-wrapper table">
                <h5>
                    <i18n:text>xmlui.dri2xhtml.METS-1.0.item-date</i18n:text>
                </h5>
                <xsl:for-each select="dim:field[@element='date' and @qualifier='issued']">
                    <xsl:copy-of select="substring(./node(),1,10)"/>
                    <xsl:if test="count(following-sibling::dim:field[@element='date' and @qualifier='issued']) != 0">
                        <br/>
                    </xsl:if>
                </xsl:for-each>
            </div>
        </xsl:if>
    </xsl:template>

    <xsl:template name="itemSummaryView-DIM-subject">
        <xsl:if test="dim:field[@element='subject' and not(@qualifier) and descendant::text()]">
            <div class="simple-item-view-date word-break item-page-field-wrapper table">
                <h5>
                    <i18n:text>xmlui.dri2xhtml.METS-1.0.item-subject</i18n:text>
                </h5>
                <span>
                    <xsl:if test="count(dim:field[@element='subject' and not(@qualifier)]) &gt; 1 and not(count(dim:field[@element='subject' and @qualifier='abstract']) &gt; 1)">
                        <span class="spacer">&#160;</span>
                    </xsl:if>
                    <xsl:for-each select="dim:field[@element='subject' and not(@qualifier)]">
                        <a>
                            <xsl:attribute name="href">
                                <xsl:value-of select="$context-path"/>
                                <xsl:text>/browse?value=</xsl:text>
                                <xsl:value-of select="url:encode(./node())" />
                                <xsl:text>&amp;type=subject</xsl:text>
                            </xsl:attribute>
                            <xsl:copy-of select="./node()"/>
                        </a>
                        <xsl:if test="count(following-sibling::dim:field[@element='subject' and not(@qualifier)]) != 0">
                            <span class="spacer">; </span>
                        </xsl:if>
                    </xsl:for-each>
                    <xsl:if test="count(dim:field[@element='subject' and not(@qualifier)]) &gt; 1">
                        <span class="spacer">&#160;</span>
                    </xsl:if>
                </span>
            </div>
        </xsl:if>
    </xsl:template>

    <xsl:template name="itemSummaryView-DIM-publisher">
        <xsl:if test="dim:field[@element='publisher' and not(@qualifier) and descendant::text()]">
            <div class="simple-item-view-date word-break item-page-field-wrapper table">
                <h5>
                    <i18n:text>xmlui.metadata.dc.publisher</i18n:text>
                </h5>
                <span>
                    <xsl:for-each select="dim:field[@element='publisher']">
                        <xsl:copy-of select="node()"/>
                        <xsl:if test="count(following-sibling::dim:field[@element='publisher']) != 0">
                            <xsl:text>; </xsl:text>
                        </xsl:if>
                    </xsl:for-each>
                </span>
            </div>
        </xsl:if>
    </xsl:template>

    <xsl:template name="itemSummaryView-DIM-first-line-of-text">
        <xsl:if test="dim:field[@element='title' and @qualifier='first-line-of-text' and descendant::text()]">
            <div class="simple-item-view-date word-break item-page-field-wrapper table">
                <h5>
                    <i18n:text>xmlui.metadata.dc.title.first-line-of-text</i18n:text>
                </h5>
                <span>
                    <xsl:for-each select="dim:field[@element='title' and @qualifier='first-line-of-text']">
                        <xsl:copy-of select="node()"/>
                        <xsl:if test="count(following-sibling::dim:field[@element='title' and @qualifier='first-line-of-text']) != 0">
                            <xsl:text>; </xsl:text>
                        </xsl:if>
                    </xsl:for-each>
                </span>
            </div>
        </xsl:if>
    </xsl:template>

    <xsl:template name="itemSummaryView-DIM-first-line-of-chorus">
        <xsl:if test="dim:field[@element='title' and @qualifier='first-line-of-chorus' and descendant::text()]">
            <div class="simple-item-view-date word-break item-page-field-wrapper table">
                <h5>
                    <i18n:text>xmlui.metadata.dc.title.first-line-of-chorus</i18n:text>
                </h5>
                <span>
                    <xsl:for-each select="dim:field[@element='title' and @qualifier='first-line-of-chorus']">
                        <xsl:copy-of select="node()"/>
                        <xsl:if test="count(following-sibling::dim:field[@element='title' and @qualifier='first-line-of-chorus']) != 0">
                            <xsl:text>; </xsl:text>
                        </xsl:if>
                    </xsl:for-each>
                </span>
            </div>
        </xsl:if>
    </xsl:template>

<xsl:template name="itemSummaryView-show-full">
        <div class="simple-item-view-show-full item-page-field-wrapper table">
            <h5>Metadata</h5> <!-- TODO i18n -->
            <a>
                <xsl:attribute name="href"><xsl:value-of select="$ds_item_view_toggle_url"/></xsl:attribute>
                <span class="glyphicon glyphicon-list-alt"></span>
                <xsl:text> </xsl:text>
                <i18n:text>xmlui.ArtifactBrowser.ItemViewer.show_full</i18n:text>

            </a>
        </div>
    </xsl:template>

    <xsl:template name="itemSummaryView-collections">
        <xsl:if test="$document//dri:referenceSet[@id='aspect.artifactbrowser.ItemViewer.referenceSet.collection-viewer']">
            <div class="simple-item-view-collections item-page-field-wrapper table">
                <h5>
                    <xsl:text>Collections</xsl:text>    <!--TODO i18n-->
                </h5>
                <xsl:apply-templates select="$document//dri:referenceSet[@id='aspect.artifactbrowser.ItemViewer.referenceSet.collection-viewer']/dri:reference"/>
            </div>
        </xsl:if>
    </xsl:template>

    <xsl:template name="itemSummaryView-DIM-file-section">
        <xsl:if test="//mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL' or @USE='LICENSE']/mets:file">
            <xsl:choose>
                <xsl:when
                        test="count(//mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL' or @USE='LICENSE']/mets:file) > 1">
                    <a data-toggle="collapse" href="#collapseExample" aria-expanded="false" aria-controls="collapseExample">
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-viewOpen</i18n:text>
                    </a>
                </xsl:when>
                <xsl:otherwise>
                    <a data-toggle="collapse" href="#collapseExample" aria-expanded="true" aria-controls="collapseExample">
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-viewOpen</i18n:text>
                    </a>
                </xsl:otherwise>
            </xsl:choose>

            <div class="collapse" id="collapseExample">
                <!-- show files when only 1, collapsed when files > 1 -->
                <xsl:choose>
                    <xsl:when
                            test="count(//mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL' or @USE='LICENSE']/mets:file) > 1">
                        <xsl:attribute name="class">
                            <xsl:text>collapse</xsl:text>
                        </xsl:attribute>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:attribute name="class">
                            <xsl:text>collapse in</xsl:text>
                        </xsl:attribute>
                    </xsl:otherwise>
                </xsl:choose>

                <xsl:variable name="label-1">
                    <xsl:choose>
                        <xsl:when test="confman:getProperty('mirage2','item-view.bitstream.href.label.1')">
                            <xsl:value-of select="confman:getProperty('mirage2','item-view.bitstream.href.label.1')"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text>label</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>

                <xsl:variable name="label-2">
                    <xsl:choose>
                        <xsl:when test="confman:getProperty('mirage2','item-view.bitstream.href.label.2')">
                            <xsl:value-of select="confman:getProperty('mirage2','item-view.bitstream.href.label.2')"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text>title</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>

                <xsl:for-each select="//mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL' or @USE='LICENSE']/mets:file">
                    <div>
                        <a>
                            <xsl:attribute name="href">
                                <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                            </xsl:attribute>
                            <xsl:if test="mets:FLocat[@LOCTYPE='URL']/@xlink:label and not(mets:FLocat[@LOCTYPE='URL']/@xlink:label = '')">
                                <xsl:attribute name="title">
                                    <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:label"/>
                                </xsl:attribute>
                            </xsl:if>
                            <xsl:choose>
                                <xsl:when test="contains('image/jpeg', @MIMETYPE) and not(contains(mets:FLocat[@LOCTYPE='URL']/@xlink:href,'isAllowed=n'))">
                                    <xsl:attribute name="class">
                                        <xsl:text>imagebitstream</xsl:text>
                                    </xsl:attribute>
                                </xsl:when>
                            </xsl:choose>
                            <xsl:call-template name="getFileIcon">
                                <xsl:with-param name="mimetype">
                                    <xsl:value-of select="substring-before(@MIMETYPE,'/')"/>
                                    <xsl:text>/</xsl:text>
                                    <xsl:value-of select="substring-after(@MIMETYPE,'/')"/>
                                </xsl:with-param>
                            </xsl:call-template>
                            <xsl:choose>
                                <xsl:when test="contains($label-1, 'label') and mets:FLocat[@LOCTYPE='URL']/@xlink:label and not(mets:FLocat[@LOCTYPE='URL']/@xlink:label = '')">
                                    <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:label"/>
                                </xsl:when>
                                <xsl:when test="contains($label-1, 'title') and mets:FLocat[@LOCTYPE='URL']/@xlink:title and not(mets:FLocat[@LOCTYPE='URL']/@xlink:title = '')">
                                    <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>
                                </xsl:when>
                                <xsl:when test="contains($label-2, 'label') and mets:FLocat[@LOCTYPE='URL']/@xlink:label and not(mets:FLocat[@LOCTYPE='URL']/@xlink:label = '')">
                                    <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:label"/>
                                </xsl:when>
                                <xsl:when test="contains($label-2, 'title') and mets:FLocat[@LOCTYPE='URL']/@xlink:title and not(mets:FLocat[@LOCTYPE='URL']/@xlink:title = '')">
                                    <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:call-template name="getFileTypeDesc">
                                        <xsl:with-param name="mimetype">
                                            <xsl:value-of select="substring-before(@MIMETYPE,'/')"/>
                                            <xsl:text>/</xsl:text>
                                            <xsl:value-of select="substring-before(substring-after(@MIMETYPE,'/'),';')"/>
                                        </xsl:with-param>
                                    </xsl:call-template>
                                </xsl:otherwise>
                            </xsl:choose>
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
                </xsl:for-each>
            </div>


            <div class="collapse-group">
                <p><a class="btn" data-toggle="collapse" data-target="#viewdetails">

                </a></p>
                <p class="collapse" id="viewdetails" >

                </p>
            </div>
        </xsl:if>
    </xsl:template>

    <xsl:template match="dim:dim" mode="itemDetailView-DIM">
        <xsl:if test="confman:getProperty('mirage2','snazy') != 'true'">
            <xsl:call-template name="itemSummaryView-DIM-title"/>
        </xsl:if>
        <div class="ds-table-responsive">
            <table class="ds-includeSet-table detailtable table table-striped table-hover">
                <xsl:apply-templates mode="itemDetailView-DIM"/>
            </table>
        </div>

        <span class="Z3988">
            <xsl:attribute name="title">
                 <xsl:call-template name="renderCOinS"/>
            </xsl:attribute>
            &#xFEFF; <!-- non-breaking space to force separating the end tag -->
        </span>
        <xsl:copy-of select="$SFXLink" />
    </xsl:template>

    <xsl:template match="dim:field" mode="itemDetailView-DIM">
        <tr>
            <xsl:attribute name="class">
                <xsl:text>ds-table-row </xsl:text>
                <xsl:if test="(position() div 2 mod 2 = 0)">even </xsl:if>
                <xsl:if test="(position() div 2 mod 2 = 1)">odd </xsl:if>
            </xsl:attribute>
            <xsl:variable name="metadata-field">
                <xsl:value-of select="./@mdschema"/>
                <xsl:text>.</xsl:text>
                <xsl:value-of select="./@element"/>
                <xsl:if test="./@qualifier">
                    <xsl:text>.</xsl:text>
                    <xsl:value-of select="./@qualifier"/>
                </xsl:if>
            </xsl:variable>
            <td class="metadata-key label-cell">
                <!-- title for hover over -->
                <xsl:attribute name="title">
                    <xsl:value-of select="$metadata-field"/>
                    <xsl:if test="./@language and @language!=''">
                        <xsl:text>[</xsl:text>
                        <xsl:value-of select="./@language"/>
                        <xsl:text>]</xsl:text>
                    </xsl:if>
                </xsl:attribute>
                <!-- i18n for translating metadata key to human readable -->
                <i18n:text>
                    <xsl:text>xmlui.metadata.</xsl:text>
                    <xsl:value-of select="$metadata-field"/>
                </i18n:text>

            </td>
            <td class="metadata-field word-break">
                <!-- Linkify certain fields-->
                <xsl:choose>
                    <xsl:when test="@element='subject' and not(@qualifier)">
                        <a>
                            <xsl:attribute name="href">
                                <xsl:value-of select="$context-path"/>
                                <xsl:text>/browse?value=</xsl:text>
                                <xsl:value-of select="url:encode(./node())" />
                                <xsl:text>&amp;type=subject</xsl:text>
                            </xsl:attribute>
                            <xsl:copy-of select="./node()"/>
                        </a>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:copy-of select="./node()"/>
                    </xsl:otherwise>
                </xsl:choose>

                <!--<xsl:if test="./@authority and ./@confidence">-->
                <!--<xsl:call-template name="authorityConfidenceIcon">-->
                <!--<xsl:with-param name="confidence" select="./@confidence"/>-->
                <!--</xsl:call-template>-->
                <!--</xsl:if>-->
            </td>
            <!-- want to maybe be able to hide the language column -->
            <td class="metadata-language">
                <xsl:value-of select="./@language"/>
            </td>
        </tr>
    </xsl:template>

    <!-- don't render the item-view-toggle automatically in the summary view, only when it gets called -->
    <xsl:template match="dri:p[contains(@rend , 'item-view-toggle') and
        (preceding-sibling::dri:referenceSet[@type = 'summaryView'] or following-sibling::dri:referenceSet[@type = 'summaryView'])]">
    </xsl:template>

    <!-- don't render the head on the item view page -->
    <xsl:template match="dri:div[@n='item-view']/dri:head" priority="5">
    </xsl:template>

   <xsl:template match="mets:fileGrp[@USE='CONTENT']">
        <xsl:param name="context"/>
        <xsl:param name="primaryBitstream" select="-1"/>
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

   <xsl:template match="mets:fileGrp[@USE='LICENSE']">
        <xsl:param name="context"/>
        <xsl:param name="primaryBitstream" select="-1"/>
            <xsl:apply-templates select="mets:file">
                        <xsl:with-param name="context" select="$context"/>
            </xsl:apply-templates>
    </xsl:template>

    <xsl:template match="mets:file">
        <xsl:param name="context" select="."/>
        <div class="file-wrapper row">
            <div class="col-xs-6 col-sm-3">
                <div class="thumbnail">
                    <a class="image-link">
                        <xsl:attribute name="href">
                            <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                        </xsl:attribute>
                        <xsl:choose>
                            <xsl:when test="$context/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/
                        mets:file[@GROUPID=current()/@GROUPID]">
                                <img alt="Thumbnail">
                                    <xsl:attribute name="src">
                                        <xsl:value-of select="$context/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/
                                    mets:file[@GROUPID=current()/@GROUPID]/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                                    </xsl:attribute>
                                </img>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:call-template name="getFileFormatIcon">
                                    <xsl:with-param name="mimetype">
                                        <xsl:value-of select="$context/mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file[@GROUPID=current()/@GROUPID]/@MIMETYPE"/>
                                    </xsl:with-param>
                                </xsl:call-template>
                            </xsl:otherwise>
                        </xsl:choose>


                        <!--<img data-src="holder.js/100%x180" alt="100%x180" style="max-width: 178px; width: 100%; display: block; margin:auto;" src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAALIAAAC0CAYAAAAw03Z1AAAGuUlEQVR4Xu3Y10tkWRTF4V3mHFHxQUVFHxQxJ1T8280JFRMoGMEXU5kDxu59ZkqujuUUzLz08neh6W617nWv9dW551YsHo+/GQcJ/OEJxID8hzfIrx8SADIQJBIAskSNDAFkDEgkAGSJGhkCyBiQSADIEjUyBJAxIJEAkCVqZAggY0AiASBL1MgQQMaARAJAlqiRIYCMAYkEgCxRI0MAGQMSCQBZokaGADIGJBIAskSNDAFkDEgkAGSJGhkCyBiQSADIEjUyBJAxIJEAkCVqZAggY0AiASBL1MgQQMaARAJAlqiRIYCMAYkEgCxRI0MAGQMSCQBZokaGADIGJBIAskSNDAFkDEgkAGSJGhkCyBiQSADIEjUyBJAxIJEAkCVqZAggY0AiASBL1MgQQMaARAJAlqiRIYCMAYkEgCxRI0MAGQMSCQBZokaGADIGJBIAskSNDAFkDEgkAGSJGhkCyBiQSADIEjUyBJAxIJEAkCVqZAggY0AiASBL1MgQQMaARAJAlqiRIYCMAYkEgCxRI0MAGQMSCQBZokaGADIGJBIAskSNDAFkDEgkAGSJGhkCyBiQSADIEjUyBJAxIJEAkCVqZAggY0AiASBL1MgQQMaARAJAlqiRIYCMAYkEgCxRI0MAGQMSCQBZokaGADIGJBIAskSNDAFkDEgkAORIjXt7e3ZwcGADAwOWk5NjV1dXtrm5aRkZGe8/9fLyYtnZ2dbW1mbPz8+2sbFhFxcXlpaWZnV1deFPqsfn6/nrnp6ekp7z7e3N1tfX7fT01GKxmDU0NFhtbW2ql5P+OSD/rtchxuNx293dDWUPDQ1Zbm5u+NrS0tI/AKSnp9vo6KgtLCzY9fW1ZWZmBtQOrbW11aqrq79Fk+x6/qK5ubmk51xZWbGTk5MP1/M3VFVVlTTSVIYD8u+UxsfH7fHx8T2v4eHhsCL7kQDqWBcXF+38/Nyam5sD1snJSXPUIyMjYfV22JWVlWGlXF1dDd/r6uqym5ubsLL7St7R0RFe99X1fDVOds6Wlpbwe/rK79fzN5nDLikpsZ6enlS6lv4ZIP+N1W/Vs7Ozdn9/b1HIifaPj48DziicBOyampqA1ZEnVsjE98rKyuzh4cHu7u6sqakpbD38zZHsesnOWVpaGpD7Nsch+xbHYfv//fd14D/5AHKkfYfsID9D9i3D2NhYADg4OGj5+fnhVb7KHh4efvDT29trxcXFYcV1eK+vr+H7/jX/XvT46nrJzunXXl5etvLycuvs7Ay/i0POysoKWyF/Y/zkA8gpQD46OrK1tTWrqKiw9vb28ApfuaempsKK6NsHv9Vvb29bQUFBeFj0wx/mdnZ2wr/7+/utsLDwW8jfndO3JH493544XFbkj29bIKcAOXG7jz5YnZ2dhRWyqKjI+vr63lfIxK3eV/GJiYnwdT+ib4LEJT+vyN+d098cia2F3zH8zjE/P295eXnhLsGKHI+//eRbUnT2mZkZu729/bC18JXPQfrfDshXRD986+Bfd7C+R/aHvcvLy3fYvoL7Su4rtO+PfYvx+ROGz9f7t3NOT0+Hc/mDpn9a4pgbGxutvr7+x1fIihwh4Cucg4zukRN73a/2ovv7+2E7kTgcua/O/nDnn2D4KukPZg56a2vr/UHNP83w46vrJTunn9vfZP7xXHTf3d3d/eMf9DxLIP/Htcy3Dg7XPzXw2/z/cXx3Tkfsq7Ifvtpz/JUAkJEgkQCQJWpkCCBjQCIBIEvUyBBAxoBEAkCWqJEhgIwBiQSALFEjQwAZAxIJAFmiRoYAMgYkEgCyRI0MAWQMSCQAZIkaGQLIGJBIAMgSNTIEkDEgkQCQJWpkCCBjQCIBIEvUyBBAxoBEAkCWqJEhgIwBiQSALFEjQwAZAxIJAFmiRoYAMgYkEgCyRI0MAWQMSCQAZIkaGQLIGJBIAMgSNTIEkDEgkQCQJWpkCCBjQCIBIEvUyBBAxoBEAkCWqJEhgIwBiQSALFEjQwAZAxIJAFmiRoYAMgYkEgCyRI0MAWQMSCQAZIkaGQLIGJBIAMgSNTIEkDEgkQCQJWpkCCBjQCIBIEvUyBBAxoBEAkCWqJEhgIwBiQSALFEjQwAZAxIJAFmiRoYAMgYkEgCyRI0MAWQMSCQAZIkaGQLIGJBIAMgSNTIEkDEgkQCQJWpkCCBjQCIBIEvUyBBAxoBEAkCWqJEhgIwBiQSALFEjQwAZAxIJAFmiRoYAMgYkEgCyRI0MAWQMSCQAZIkaGQLIGJBIAMgSNTIEkDEgkQCQJWpkCCBjQCIBIEvUyBBAxoBEAr8AQizVRbhMmFcAAAAASUVORK5CYII="/>-->
                    </a>
                </div>
            </div>

            <div class="col-xs-6 col-sm-7">
                <dl class="file-metadata dl-horizontal">
                    <dt>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-name</i18n:text>
                        <xsl:text>:</xsl:text>
                    </dt>
                    <dd class="word-break">
                        <xsl:attribute name="title">
                            <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>
                        </xsl:attribute>
                        <xsl:value-of select="util:shortenString(mets:FLocat[@LOCTYPE='URL']/@xlink:title, 30, 5)"/>
                    </dd>
                <!-- File size always comes in bytes and thus needs conversion -->
                    <dt>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-size</i18n:text>
                        <xsl:text>:</xsl:text>
                    </dt>
                    <dd class="word-break">
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
                    </dd>
                <!-- Lookup File Type description in local messages.xml based on MIME Type.
         In the original DSpace, this would get resolved to an application via
         the Bitstream Registry, but we are constrained by the capabilities of METS
         and can't really pass that info through. -->
                    <dt>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-format</i18n:text>
                        <xsl:text>:</xsl:text>
                    </dt>
                    <dd class="word-break">
                        <xsl:call-template name="getFileTypeDesc">
                            <xsl:with-param name="mimetype">
                                <xsl:value-of select="substring-before(@MIMETYPE,'/')"/>
                                <xsl:text>/</xsl:text>
                                <xsl:value-of select="substring-before(substring-after(@MIMETYPE,'/'),';')"/>
                            </xsl:with-param>
                        </xsl:call-template>
                    </dd>
                <!---->
                <!-- Display the contents of 'Description' only if bitstream contains a description -->
                <xsl:if test="mets:FLocat[@LOCTYPE='URL']/@xlink:label != ''">
                        <dt>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-description</i18n:text>
                            <xsl:text>:</xsl:text>
                        </dt>
                        <dd class="word-break">
                            <xsl:attribute name="title">
                                <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:label"/>
                            </xsl:attribute>
                            <!--<xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:label"/>-->
                            <xsl:value-of select="util:shortenString(mets:FLocat[@LOCTYPE='URL']/@xlink:label, 30, 5)"/>
                        </dd>
                </xsl:if>
                </dl>
            </div>

            <div class="file-link col-xs-6 col-xs-offset-6 col-sm-2 col-sm-offset-0">
                <xsl:choose>
                    <xsl:when test="@ADMID">
                        <xsl:call-template name="display-rights"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:call-template name="view-open"/>
                    </xsl:otherwise>
                </xsl:choose>
            </div>
        </div>

</xsl:template>

    <xsl:template name="view-open">
        <a>
            <xsl:attribute name="href">
                <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
            </xsl:attribute>
            <i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-viewOpen</i18n:text>
        </a>
    </xsl:template>

    <xsl:template name="display-rights">
        <xsl:variable name="file_id" select="jstring:replaceAll(jstring:replaceAll(string(@ADMID), '_METSRIGHTS', ''), 'rightsMD_', '')"/>
        <xsl:variable name="rights_declaration" select="../../../mets:amdSec/mets:rightsMD[@ID = concat('rightsMD_', $file_id, '_METSRIGHTS')]/mets:mdWrap/mets:xmlData/rights:RightsDeclarationMD"/>
        <xsl:variable name="rights_context" select="$rights_declaration/rights:Context"/>
        <xsl:variable name="users">
            <xsl:for-each select="$rights_declaration/*">
                <xsl:value-of select="rights:UserName"/>
                <xsl:choose>
                    <xsl:when test="rights:UserName/@USERTYPE = 'GROUP'">
                       <xsl:text> (group)</xsl:text>
                    </xsl:when>
                    <xsl:when test="rights:UserName/@USERTYPE = 'INDIVIDUAL'">
                       <xsl:text> (individual)</xsl:text>
                    </xsl:when>
                </xsl:choose>
                <xsl:if test="position() != last()">, </xsl:if>
            </xsl:for-each>
        </xsl:variable>

        <xsl:choose>
            <xsl:when test="not ($rights_context/@CONTEXTCLASS = 'GENERAL PUBLIC') and ($rights_context/rights:Permissions/@DISPLAY = 'true')">
                <a href="{mets:FLocat[@LOCTYPE='URL']/@xlink:href}">
                    <img width="64" height="64" src="{concat($theme-path,'/images/Crystal_Clear_action_lock3_64px.png')}" title="Read access available for {$users}"/>
                    <!-- icon source: http://commons.wikimedia.org/wiki/File:Crystal_Clear_action_lock3.png -->
                </a>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="view-open"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Isn't about mimetype, but about open access or closed access bitstream -->
    <xsl:template name="getFileIcon">
        <xsl:param name="mimetype"/>
            <i aria-hidden="true">
                <xsl:attribute name="class">
                <xsl:text>glyphicon </xsl:text>
                <xsl:choose>
                    <xsl:when test="contains(mets:FLocat[@LOCTYPE='URL']/@xlink:href,'isAllowed=n')">
                        <xsl:text> glyphicon-lock</xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text> glyphicon-file</xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
                </xsl:attribute>
            </i>
        <xsl:text> </xsl:text>
    </xsl:template>

    <!-- Import from Snazy -->
    <xsl:template name="itemSummaryView-DIM-file-section-snazy">
        <xsl:param name="context" />
        <xsl:param name="primaryBitstream" />

        <!-- If there are images, maybe show the archive.org viewer first -->
        <span id="ds-firstpage-side">
            <xsl:choose>
                <xsl:when test="dim:field[@mdschema='ds' and @element='firstpage' and @qualifier='side']">
                    <xsl:attribute name="data-side">
                        <xsl:text>left</xsl:text>
                    </xsl:attribute>
                </xsl:when>
            </xsl:choose>
        </span>

        <!-- Show File Section:
            - Show a bookreader when there are more than one accessible images in the bitstreams
            - Otherwise show the snazy file list
        -->
        <xsl:choose>
            <xsl:when test="count(//mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL' or @USE='LICENSE']/mets:file[contains('image/jpeg', @MIMETYPE) and not(contains(mets:FLocat[@LOCTYPE='URL']/@xlink:href,'isAllowed=n'))]) > 2">
            <div id="BookReader"></div>
            </xsl:when>
            <xsl:otherwise>
                <ul id="file_list" class="snazy ds-file-list no-js">
                    <xsl:apply-templates select="//mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL' or @USE='LICENSE']/mets:file" mode="snazy">
                        <xsl:with-param name="context" select="$context"/>
                    </xsl:apply-templates>
                </ul>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="mets:file" mode="snazy">
        <xsl:variable name="videoplayer" select="'video/webm video/mp4 video/mpeg'" />
        <xsl:variable name="audioplayer" select="'audio/mpeg audio/x-mpeg audio/basic audio/x-wav'"/>
        <xsl:variable name="googleplayer" select="'azudio/mpeg azudio/basic azudio/x-wav'" />
        <xsl:variable name="html5video" select="'vzideo/webm'" />
        <xsl:variable name="flashvideo" select="'vzideo/mp4 vzideo/mpeg'" />
        <xsl:variable name="googledocsviewer" select="'application/jsjsjsj'" />
        <xsl:variable name="embedwithfallback" select="'application/x-pdf application/pdf'" />
        <xsl:variable name="image" select="'image/jpeg'"/>
        <xsl:variable name="mview">
            <xsl:choose>
                <xsl:when test="contains($googleplayer, @MIMETYPE)">
                    <xsl:text>googleplayer</xsl:text>
                </xsl:when>
                <xsl:when test="contains($html5video, @MIMETYPE)">
                    <xsl:text>html5video</xsl:text>
                </xsl:when>
                <xsl:when test="contains($flashvideo, @MIMETYPE)">
                    <xsl:text>flashvideo</xsl:text>
                </xsl:when>
                <xsl:when test="contains($googledocsviewer, @MIMETYPE)">
                    <xsl:text>googledocsviewer</xsl:text>
                </xsl:when>
                <xsl:when test="contains($embedwithfallback, @MIMETYPE)">
                    <xsl:text>embedwithfallback</xsl:text>
                </xsl:when>
                <xsl:when test="contains($videoplayer, @MIMETYPE)">
                    <xsl:text>videoplayer</xsl:text>
                </xsl:when>
                <xsl:when test="contains($audioplayer, @MIMETYPE)">
                    <xsl:text>audioplayer</xsl:text>
                </xsl:when>
                <xsl:when test="contains($image, @MIMETYPE)">
                    <xsl:text>image</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>default</xsl:text>
                </xsl:otherwise>

            </xsl:choose>
        </xsl:variable>

        <!-- CSS class names based on MIME type -->
        <xsl:variable name="mimetypeForCSS" select="translate(@MIMETYPE, '/', '-')" />
        <xsl:variable name="mimetypeType" select="substring-before(@MIMETYPE, '/')" />
        <xsl:variable name="mimetypeFormat" select="substring-after(@MIMETYPE, '/')" />
        <li>
            <xsl:attribute name="class">
                <xsl:text>file-entry </xsl:text>
                <xsl:value-of select="$mview" />
                <xsl:text> </xsl:text>
                <xsl:value-of select="$mimetypeType" />
                <xsl:text> </xsl:text>
                <xsl:value-of select="$mimetypeFormat" />
                <xsl:text> </xsl:text>
                <xsl:value-of select="$mimetypeForCSS" />
                <xsl:if test="(position() mod 2 = 0)"> even</xsl:if>
                <xsl:if test="(position() mod 2 = 1)"> odd</xsl:if>
            </xsl:attribute>
            <!--<xsl:text>Filename: </xsl:text>-->
            <div class="file-item file-link file-name">
                <span class="label">File:</span>
                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                    </xsl:attribute>
                    <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>
                </a>
                <span class="file-size">
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
                </span>
            </div>

            <div class="slide-arrow show">
                <div class="showhide" data-toggle="modal">
                    <!-- Button trigger modal -->
                    <xsl:attribute name="data-target">
                        <xsl:text>#myModal_</xsl:text>
                        <xsl:value-of select="@ID"/>
                    </xsl:attribute>
                    Show File
                </div>
            </div>


            <xsl:attribute name="id">
                <xsl:text>myModal_</xsl:text>
                <xsl:value-of select="@ID"/>
            </xsl:attribute>

            <div class="file-item file-mimetype last">
                <span class="label">MIME type:</span>
                <span class="value">
                    <xsl:value-of select="@MIMETYPE" />
                </span>
            </div>
            <!-- Display file based on MIME type -->
            <div class="file-view">
                <div class="file-view-container">
                    <xsl:choose>
                        <xsl:when test="$mview='googleplayer'">
                            <embed class="googleplayer" type="application/x-shockwave-flash" wmode="transparent" height="27" width="320">
                                <xsl:attribute name="src">
                                    <xsl:text>http://www.google.com/reader/ui/3523697345-audio-player.swf?audioUrl=</xsl:text>
                                    <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                                </xsl:attribute>
                                <xsl:attribute name="mime">
                                    <xsl:value-of select="@MIMETYPE" />
                                </xsl:attribute>
                            </embed>
                        </xsl:when>
                        <xsl:when test="$mview='html5video'">
                            <video class="html5video" preload="none" controls="controls">
                                <xsl:attribute name="src">
                                    <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href" />
                                </xsl:attribute>
                                <a>
                                    <xsl:attribute name="href">
                                        <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                                    </xsl:attribute>
                                    <i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-viewOpen</i18n:text>
                                </a>
                            </video>
                        </xsl:when>
                        <xsl:when test="$mview='videoplayer'">
                            <div id="myElement">Loading the player...</div>
                            <script type="text/javascript">
                                jwplayer("myElement").setup({
                                file: "<xsl:value-of select="$baseurl"/><xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>",
                                image: "",
                                width: "100%",
                                aspectratio: "16:9"
                                });
                            </script>
                        </xsl:when>
                        <xsl:when test="$mview='audioplayer'">
                            <div id="myElement">Loading the player...</div>
                            <script type="text/javascript">
                                jwplayer("myElement").setup({
                                file: "<xsl:value-of select="$baseurl"/><xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>",
                                width: "100%",
                                height: "30px"
                                });
                            </script>
                        </xsl:when>
                        <xsl:when test="$mview='googledocsviewer'">
                            <iframe class="googledocsviewer">
                                <xsl:attribute name="src">
                                    <xsl:text>http://docs.google.com/viewer?url=</xsl:text>
                                    <!--<xsl:text>http://labs.google.com/papers/bigtable-osdi06.pdf</xsl:text>-->
                                    <xsl:value-of select="$baseurl" />
                                    <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href" />
                                    <xsl:text>&#38;embedded=true</xsl:text>
                                </xsl:attribute>
                            </iframe>
                        </xsl:when>
                        <xsl:when test="$mview='embedwithfallback'">
                            <!-- Modal -->
                            <div class="modal modal-lg fade" tabindex="-1" role="dialog" aria-hidden="true">
                                <xsl:attribute name="id">
                                    <xsl:text>myModal_</xsl:text>
                                    <xsl:value-of select="@ID"/>
                                </xsl:attribute>
                                <xsl:attribute name="aria-labelledby">
                                    <xsl:text>myModalLabel_</xsl:text>
                                    <xsl:value-of select="@ID"/>
                                </xsl:attribute>


                                <div class="modal-dialog modal-dialog-lg">
                                    <div class="modal-content">
                                        <div class="modal-header">
                                            <button type="button" class="close" data-dismiss="modal"><span aria-hidden="true"><span class="glyphicon glyphicon-remove"></span></span><span class="sr-only">Close</span></button>
                                            <h4 class="modal-title">
                                                <xsl:attribute name="id">
                                                    <xsl:text>myModalLabel_</xsl:text>
                                                    <xsl:value-of select="@ID"/>
                                                </xsl:attribute>

                                                <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>
                                            </h4>
                                        </div>
                                        <div class="modal-body">
                                            <object class="embedwithfallback">
                                                <xsl:attribute name="data">
                                                    <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                                                </xsl:attribute>
                                                <xsl:attribute name="type">
                                                    <xsl:value-of select="@MIMETYPE" />
                                                </xsl:attribute>
                                                <a>
                                                    <xsl:attribute name="href">
                                                        <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                                                    </xsl:attribute>
                                                    <i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-viewOpen</i18n:text>
                                                </a>
                                            </object>
                                        </div>
                                        <div class="modal-footer">
                                            <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                                        </div>
                                    </div>
                                </div>
                            </div>

                        </xsl:when>
                        <xsl:when test="$mview='image'">
                            <img class="lazy smalldisplay" src="{concat($theme-path,'../mirage2/images/loading-lg.gif')}">
                                <xsl:attribute name="data-original">
                                    <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                                </xsl:attribute>
                            </img>
                        </xsl:when>
                        <xsl:otherwise>
                            <a>
                                <xsl:attribute name="href">
                                    <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                                </xsl:attribute>
                                <i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-viewOpen</i18n:text>
                            </a>
                        </xsl:otherwise>
                    </xsl:choose>
                </div>
            </div>
        </li>
    </xsl:template>

    <!-- Generate the license information from the file section -->
    <xsl:template match="mets:fileGrp[@USE='CC-LICENSE']" mode="simple">
        <li><a href="{mets:file/mets:FLocat[@xlink:title='license_text']/@xlink:href}"><i18n:text>xmlui.dri2xhtml.structural.link_cc</i18n:text></a></li>
    </xsl:template>

    <!-- Generate the license information from the file section -->
    <xsl:template match="mets:fileGrp[@USE='LICENSE']" mode="simple">
        <li><a href="{mets:file/mets:FLocat[@xlink:title='license.txt']/@xlink:href}"><i18n:text>xmlui.dri2xhtml.structural.link_original_license</i18n:text></a></li>
    </xsl:template>

</xsl:stylesheet>
