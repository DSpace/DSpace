<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<xsl:stylesheet
    xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
    xmlns:dri="http://di.tamu.edu/DRI/1.0/"
    xmlns:mets="http://www.loc.gov/METS/"
    xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
    xmlns:xlink="http://www.w3.org/TR/xlink/"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:xalan="http://xml.apache.org/xalan"
    xmlns:encoder="xalan://java.net.URLEncoder"
    exclude-result-prefixes="xalan encoder i18n dri mets dim  xlink xsl">

    <xsl:output indent="yes"/>

<!--
    These templates are devoted to rendering the search results for Discovery.
    Since Discovery uses hit highlighting separate templates are required !
-->


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
                    <xsl:when test="dri:list[@n=(concat($handle, ':dc.title')) and descendant::text()]">
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
                    <xsl:when test="dri:list[@n=(concat($handle, ':dc.title')) and descendant::text()]">
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


        <!--Generates thumbnails (if present)-->
        <xsl:apply-templates select="$metsDoc/mets:METS/mets:fileSec" mode="artifact-preview"><xsl:with-param name="href" select="concat($context-path, '/handle/', $handle)"/></xsl:apply-templates>

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
                        <xsl:when test="dri:list[@n=(concat($handle, ':dc.title')) and descendant::text()]">
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
    </xsl:template>

</xsl:stylesheet>
