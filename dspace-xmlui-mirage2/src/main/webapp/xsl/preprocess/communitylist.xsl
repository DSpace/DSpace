<?xml version="1.0" encoding="UTF-8"?>
<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->

<!--
    Author: Art Lowel (art at atmire dot com)

    The purpose of this file is to transform the DRI for some parts of
    DSpace into a format more suited for the theme xsls. This way the
    theme xsl files can stay cleaner, without having to change Java
    code and interfere with other themes

    e.g. this file can be used to add a class to a form field, without
    having to duplicate the entire form field template in the theme xsl
    Simply add it here to the rend attribute and let the default form
    field template handle the rest.
-->

<xsl:stylesheet
        xmlns="http://di.tamu.edu/DRI/1.0/"
        xmlns:dri="http://di.tamu.edu/DRI/1.0/"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
        xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
        xmlns:mets="http://www.loc.gov/METS/"
        xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
        exclude-result-prefixes="xsl dri dim mets i18n">

    <xsl:output indent="yes"/>

    <xsl:template match="dri:referenceSet[@id='aspect.artifactbrowser.CommunityBrowser.referenceSet.community-browser'] |
                         dri:list[@id='aspect.artifactbrowser.CommunityBrowser.list.comunity-browser']">
        <div id="{@id}" rend="community-browser-wrapper">
            <xsl:apply-templates mode="community-browser"/>
        </div>
    </xsl:template>

    <xsl:template match="dri:reference" mode="community-browser">
        <xsl:variable name="handle">
            <xsl:call-template name="get-handle-class-from-url">
                <xsl:with-param name="url" select="@url"/>
            </xsl:call-template>
        </xsl:variable>
        <div>
            <xsl:attribute name="rend">
                <xsl:text>row community-browser-row</xsl:text>
                <xsl:if test="ancestor::dri:referenceSet[1][@id='aspect.artifactbrowser.CommunityBrowser.referenceSet.community-browser'] and position() mod 2 = 0">
                    <xsl:text> odd-community-browser-row</xsl:text>
                </xsl:if>
            </xsl:attribute>
            <xsl:variable name="externalMetadataURL">
                <xsl:text>cocoon://</xsl:text>
                <xsl:value-of select="@url"/>
                <xsl:text>?sections=dmdSec</xsl:text>
            </xsl:variable>
            <xsl:variable name="depth" select="count(ancestor::dri:referenceSet)"/>
            <xsl:variable name="nephews" select="count(following-sibling::dri:reference/dri:referenceSet) + count(preceding-sibling::dri:reference/dri:referenceSet)"/>
            <xsl:variable name="second_cousins" select="count(parent::dri:referenceSet/following-sibling::dri:referenceSet/dri:reference/dri:referenceSet) + count(parent::dri:referenceSet/preceding-sibling::dri:referenceSet/dri:reference/dri:referenceSet)"/>
            <xsl:variable name="needs_one_less_indent"
                          select="$depth > 0 and $nephews = 0 and $second_cousins = 0 and not(dri:referenceSet)"/>
            <xsl:variable name="left-width">
                <xsl:choose>
                    <xsl:when test="$depth = 1 and $needs_one_less_indent">
                        <xsl:value-of select="$depth - 1"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$depth"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:choose>
                <xsl:when test="node()">
                    <div>
                        <xsl:attribute name="rend">
                            <xsl:text>col-xs-2 col-sm-1</xsl:text>
                            <xsl:if test="$left-width > 1">
                                <xsl:text> col-sm-offset-</xsl:text>
                                <xsl:value-of select="$left-width - 1"/>
                            </xsl:if>
                        </xsl:attribute>
                        <field rend="community-browser-toggle-button" value="#collapse-{$handle}"/>
                    </div>
                    <div rend="col-xs-10 col-sm-{12 - $left-width}">
                        <xsl:apply-templates select="document($externalMetadataURL)" mode="community-browser"/>
                    </div>
                </xsl:when>
                <xsl:otherwise>
                    <div rend="col-xs-10 col-sm-{12 - $left-width} col-xs-offset-2 col-sm-offset-{$left-width}">
                        <xsl:attribute name="rend">
                            <xsl:text>col-xs-10 col-sm-</xsl:text><xsl:value-of select="12 - $left-width"/>
                            <xsl:text> col-sm-offset-</xsl:text><xsl:value-of select="$left-width"/>
                            <xsl:choose>
                                <xsl:when test="$depth = 1 and $needs_one_less_indent">
                                    <xsl:text> list-mode</xsl:text>
                                </xsl:when>
                                <!--<xsl:when test="$depth > 1 and $needs_one_less_indent">-->
                                    <!--<xsl:text> col-xs-offset-2  half-indented</xsl:text>-->
                                <!--</xsl:when>-->
                                <xsl:otherwise>
                                    <xsl:text> col-xs-offset-2</xsl:text>
                                </xsl:otherwise>
                            </xsl:choose>

                        </xsl:attribute>
                        <xsl:apply-templates select="document($externalMetadataURL)" mode="community-browser"/>
                    </div>
                </xsl:otherwise>
            </xsl:choose>
        </div>
        <xsl:if test="dri:referenceSet/dri:reference">
            <div id="collapse-{$handle}" rend="sub-tree-wrapper hidden">
                <xsl:apply-templates select="dri:referenceSet[dri:reference[@type = 'DSpace Community']]/dri:reference" mode="community-browser"/>
                <xsl:apply-templates select="dri:referenceSet[dri:reference[@type = 'DSpace Collection']]/dri:reference" mode="community-browser"/>
            </div>
        </xsl:if>
    </xsl:template>

    <xsl:template match="mets:METS" mode="community-browser">
        <xsl:variable name="dim" select="mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim"/>
        <xref target="{@OBJID}" n="community-browser-link">
            <xsl:value-of select="$dim/dim:field[@element='title']"/>
        </xref>
        <!--Display community strengths (item counts) if they exist-->
        <xsl:if test="string-length($dim/dim:field[@element='format'][@qualifier='extent'][1]) &gt; 0">
            <span>
                <xsl:text> [</xsl:text>
                <xsl:value-of
                    select="$dim/dim:field[@element='format'][@qualifier='extent'][1]"/>
                <xsl:text>]</xsl:text>
            </span>
        </xsl:if>

        <xsl:variable name="description" select="$dim/dim:field[@element='description'][@qualifier='abstract']"/>
        <xsl:if test="string-length($description/text()) > 0">
            <p rend="hidden-xs">
                <xsl:value-of select="$description"/>
            </p>
        </xsl:if>
    </xsl:template>

    <xsl:template match="dri:list " mode="community-browser">
        <xsl:variable name="list-position">
            <xsl:value-of select="position()"/>
        </xsl:variable>
        <xsl:for-each select="dri:item">
            <xsl:variable name="handle">
                <xsl:choose>
                    <xsl:when test="dri:hi/dri:xref/@target">
                        <xsl:value-of select="dri:hi/dri:xref/@target"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="dri:xref/@target"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:variable name="title">
                <xsl:choose>
                    <xsl:when test="dri:hi/dri:xref">
                        <xsl:value-of select="dri:hi/dri:xref"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="dri:xref"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:variable name="count">
                <xsl:choose>
                    <xsl:when test="dri:hi/dri:xref/@rend">
                        <xsl:value-of select="dri:hi/dri:xref/@rend"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="dri:xref/@rend"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:variable name="desc">
                <xsl:choose>
                    <xsl:when test="dri:hi/dri:xref/@n">
                        <xsl:value-of select="dri:hi/dri:xref/@n"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="dri:xref/@n"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>

            <xsl:variable name="handle-class">
                <xsl:call-template name="get-handle-class-from-handle">
                    <xsl:with-param name="url">
                        <xsl:value-of select="$handle"/>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:variable>
            <div>
                <xsl:attribute name="rend">
                    <xsl:text>row community-browser-row</xsl:text>
                    <xsl:if test="parent::dri:list/ancestor::dri:list[1][@id='aspect.artifactbrowser.CommunityBrowser.list.comunity-browser'] and $list-position mod 2 = 0">
                        <xsl:text> odd-community-browser-row</xsl:text>
                    </xsl:if>
                </xsl:attribute>
                <xsl:variable name="depth" select="count(parent::dri:list/ancestor::dri:list)"/>
                <xsl:variable name="nephews" select="count(parent::dri:list/following-sibling::dri:list/dri:item[following-sibling::dri:list]) + count(parent::dri:list/preceding-sibling::dri:list/dri:item[following-sibling::dri:list])"/>
                <xsl:variable name="second_cousins" select="count(parent::dri:list/parent::dri:list/following-sibling::dri:list/dri:item/dri:list) + count(parent::dri:list/parent::dri:list/preceding-sibling::dri:list/dri:item/dri:list)"/>
                <xsl:variable name="needs_one_less_indent"
                              select="$depth > 0 and $nephews = 0 and $second_cousins = 0 and not(parent::dri:list/dri:list)"/>
                <xsl:variable name="left-width">
                    <xsl:choose>
                        <xsl:when test="$depth = 1 and $needs_one_less_indent">
                            <xsl:value-of select="$depth - 1"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="$depth"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <xsl:choose>
                    <xsl:when test="following-sibling::dri:list">
                        <div>
                            <xsl:attribute name="rend">
                                <xsl:text>col-xs-2 col-sm-1</xsl:text>
                                <xsl:if test="$left-width > 1">
                                    <xsl:text> col-sm-offset-</xsl:text>
                                    <xsl:value-of select="$left-width - 1"/>
                                </xsl:if>
                            </xsl:attribute>
                            <field rend="community-browser-toggle-button" value="#collapse-{$handle-class}"/>
                        </div>
                        <div rend="col-xs-10 col-sm-{12 - $left-width}">
                            <xref n="community-browser-link">
                                <xsl:attribute name="target">
                                    <xsl:value-of select="$handle"/>
                                </xsl:attribute>
                                <xsl:value-of select="$title"/>
                            </xref>
                            <!--Display community strengths (item counts) if they exist-->
                            <xsl:if test="$count != ''">
                                <span>
                                    <xsl:text> [</xsl:text>
                                    <xsl:value-of
                                            select="$count"/>
                                    <xsl:text>]</xsl:text>
                                </span>
                            </xsl:if>
                            <xsl:if test="$desc != ''">
                                <p rend="hidden-xs">
                                    <xsl:value-of select="$desc"/>
                                </p>
                            </xsl:if>
                        </div>
                    </xsl:when>
                    <xsl:otherwise>
                        <div rend="col-xs-10 col-sm-{12 - $left-width} col-xs-offset-2 col-sm-offset-{$left-width}">
                            <xsl:attribute name="rend">
                                <xsl:text>col-xs-10 col-sm-</xsl:text><xsl:value-of select="12 - $left-width"/>
                                <xsl:text> col-sm-offset-</xsl:text><xsl:value-of select="$left-width"/>
                                <xsl:choose>
                                    <xsl:when test="$depth = 1 and $needs_one_less_indent">
                                        <xsl:text> list-mode</xsl:text>
                                    </xsl:when>
                                    <!--<xsl:when test="$depth > 1 and $needs_one_less_indent">-->
                                    <!--<xsl:text> col-xs-offset-2  half-indented</xsl:text>-->
                                    <!--</xsl:when>-->
                                    <xsl:otherwise>
                                        <xsl:text> col-xs-offset-2</xsl:text>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:attribute>
                            <xref n="community-browser-link">
                                <xsl:attribute name="target">
                                    <xsl:value-of select="$handle"/>
                                </xsl:attribute>
                                <xsl:value-of select="$title"/>
                            </xref>
                            <!--Display community strengths (item counts) if they exist-->
                            <xsl:if test="$count != ''">
                                <span>
                                    <xsl:text> [</xsl:text>
                                    <xsl:value-of
                                            select="$count"/>
                                    <xsl:text>]</xsl:text>
                                </span>
                            </xsl:if>
                            <xsl:if test="$desc != ''">
                                <p rend="hidden-xs">
                                    <xsl:value-of select="$desc"/>
                                </p>
                            </xsl:if>
                        </div>
                    </xsl:otherwise>
                </xsl:choose>
            </div>
            <xsl:if test="following-sibling::dri:list/dri:item">
                <div id="collapse-{$handle-class}" rend="sub-tree-wrapper hidden">
                    <xsl:apply-templates select="parent::dri:list/dri:list" mode="community-browser"/>
                </div>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>
    
    <xsl:template name="get-handle-class-from-url">
        <xsl:param name="url"/>
        <xsl:variable name="handle_pre">
            <xsl:call-template name="string-replace-all">
                <xsl:with-param name="text" select="$url"/>
                <xsl:with-param name="replace" select="'/metadata/handle/'"/>
                <xsl:with-param name="by" select="''"/>
            </xsl:call-template>
        </xsl:variable>

        <xsl:variable name="handle_pre1">
            <xsl:call-template name="string-replace-all">
                <xsl:with-param name="text" select="$handle_pre"/>
                <xsl:with-param name="replace" select="'/mets.xml'"/>
                <xsl:with-param name="by" select="''"/>
            </xsl:call-template>
        </xsl:variable>

        <xsl:variable name="handle">
            <xsl:call-template name="string-replace-all">
                <xsl:with-param name="text" select="$handle_pre1"/>
                <xsl:with-param name="replace" select="'/'"/>
                <xsl:with-param name="by" select="'_'"/>
            </xsl:call-template>
        </xsl:variable>

        <xsl:call-template name="string-replace-all">
            <xsl:with-param name="text" select="$handle"/>
            <xsl:with-param name="replace" select="'.'"/>
            <xsl:with-param name="by" select="'_'"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="get-handle-class-from-handle">
        <xsl:param name="url"/>
        <xsl:variable name="handle_pre">
            <xsl:call-template name="string-replace-all">
                <xsl:with-param name="text" select="$url"/>
                <xsl:with-param name="replace" select="'/handle/'"/>
                <xsl:with-param name="by" select="''"/>
            </xsl:call-template>
        </xsl:variable>

        <xsl:call-template name="string-replace-all">
            <xsl:with-param name="text" select="$handle_pre"/>
            <xsl:with-param name="replace" select="'/'"/>
            <xsl:with-param name="by" select="'_'"/>
        </xsl:call-template>
    </xsl:template>

</xsl:stylesheet>
