
<!--
    Rendering of a list of items (e.g. in a search or
    browse results page)

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
    xmlns:confman="org.dspace.core.ConfigurationManager"
    exclude-result-prefixes="xalan encoder i18n dri mets dim xlink xsl util confman">

    <xsl:output indent="yes"/>

    <!--these templates are modfied to support the 2 different item list views that
    can be configured with the property 'xmlui.theme.mirage.item-list.emphasis' in dspace.cfg-->

    <xsl:template name="itemSummaryList-DIM">
        <xsl:variable name="itemWithdrawn" select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/@withdrawn" />

        <xsl:variable name="href">
            <xsl:choose>
                <xsl:when test="$itemWithdrawn">
                    <xsl:value-of select="@OBJEDIT"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="@OBJID"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <!-- confman:getProperty('xmlui.theme.mirage.item-list.emphasis') -->
        <xsl:variable name="emphasis" select="'file'"/>
        <xsl:choose>
            <xsl:when test="'file' = $emphasis">


                <div class="item-wrapper row">
                    <div class="col-sm-3 hidden-xs">
                        <xsl:apply-templates select="./mets:fileSec" mode="artifact-preview">
                            <xsl:with-param name="href" select="$href"/>
                        </xsl:apply-templates>
                    </div>

                    <div class="col-sm-9">
                        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
                                             mode="itemSummaryList-DIM-metadata">
                            <xsl:with-param name="href" select="$href"/>
                        </xsl:apply-templates>
                    </div>

                </div>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
                                     mode="itemSummaryList-DIM-metadata"><xsl:with-param name="href" select="$href"/></xsl:apply-templates>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--handles the rendering of a single item in a list in file mode-->
    <!--handles the rendering of a single item in a list in metadata mode-->
    <xsl:template match="dim:dim" mode="itemSummaryList-DIM-metadata">
        <xsl:param name="href"/>
        <div class="artifact-description">
            <h4 class="artifact-title">
                <xsl:element name="a">
                    <xsl:attribute name="href">
                        <xsl:value-of select="$href"/>
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
                <span class="Z3988">
                    <xsl:attribute name="title">
                        <xsl:call-template name="renderCOinS"/>
                    </xsl:attribute>
                    &#xFEFF; <!-- non-breaking space to force separating the end tag -->
                </span>
            </h4>
            <div class="artifact-info">
                <span class="author h4">
                    <small>
                    <xsl:choose>
                        <xsl:when test="dim:field[@element='contributor'][@qualifier='author']">
                            <xsl:for-each select="dim:field[@element='contributor'][@qualifier='author']">
                                <span>
                                  <xsl:if test="@authority">
                                    <xsl:attribute name="class"><xsl:text>ds-dc_contributor_author-authority</xsl:text></xsl:attribute>
                                  </xsl:if>
                                  <xsl:copy-of select="node()"/>
                                </span>
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
                    </small>
                </span>
                <xsl:text> </xsl:text>
                <xsl:if test="dim:field[@element='date' and @qualifier='issued']">
	                <span class="publisher-date h4">  <small>
	                    <xsl:text>(</xsl:text>
	                    <xsl:if test="dim:field[@element='publisher']">
	                        <span class="publisher">
	                            <xsl:copy-of select="dim:field[@element='publisher']/node()"/>
	                        </span>
	                        <xsl:text>, </xsl:text>
	                    </xsl:if>
	                    <span class="date">
	                        <xsl:value-of select="substring(dim:field[@element='date' and @qualifier='issued']/node(),1,10)"/>
	                    </span>
	                    <xsl:text>)</xsl:text>
                        </small></span>
                </xsl:if>
            </div>
            <xsl:if test="dim:field[@element = 'description' and @qualifier='abstract']">
                <xsl:variable name="abstract" select="dim:field[@element = 'description' and @qualifier='abstract']/node()"/>
                <div class="artifact-abstract">
                    <xsl:value-of select="util:shortenString($abstract, 220, 10)"/>
                </div>
            </xsl:if>
        </div>
    </xsl:template>

    <xsl:template name="itemDetailList-DIM">
        <xsl:call-template name="itemSummaryList-DIM"/>
    </xsl:template>


    <xsl:template match="mets:fileSec" mode="artifact-preview">
        <xsl:param name="href"/>
        <div class="thumbnail artifact-preview">
            <a class="image-link" href="{$href}">
                <!--<img data-src="holder.js/100%x180" alt="100%x180" style="max-width: 178px; width: 100%; display: block; margin:auto;" src="data:image/png;base64,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"/>-->

                <xsl:choose>
                    <xsl:when test="mets:fileGrp[@USE='THUMBNAIL']">
                        <img alt="Thumbnail" class="img-responsive">
                            <xsl:attribute name="src">
                                <xsl:value-of
                                        select="mets:fileGrp[@USE='THUMBNAIL']/mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                            </xsl:attribute>
                        </img>
                    </xsl:when>
                    <xsl:otherwise>
                        <!--<i class="glyphicon glyphicon-file" aria-hidden="true"/>-->
                        <img alt="Thumbnail">
                            <xsl:attribute name="data-src">
                                <xsl:text>holder.js/100%x</xsl:text>
                                <xsl:value-of select="$thumbnail.maxheight"/>
                                <xsl:text>/text:No Thumbnail</xsl:text>
                            </xsl:attribute>
                        </img>
                    </xsl:otherwise>
                </xsl:choose>
            </a>
        </div>
    </xsl:template>


</xsl:stylesheet>
