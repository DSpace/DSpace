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
        exclude-result-prefixes="xsl dri i18n">

    <xsl:output indent="yes"/>

    <xsl:template match="dri:field[@id='aspect.artifactbrowser.ConfigurableBrowse.field.starts_with'
    or @id='aspect.discovery.SearchFacetFilter.field.starts_with'
    or @id='aspect.administrative.WithdrawnItems.field.starts_with' or @id='aspect.administrative.PrivateItems.field.starts_with']">
        <field>
            <xsl:call-template name="copy-attributes"/>
            <xsl:attribute name="placeholder">
                <xsl:value-of select="preceding-sibling::i18n:text[1]/text()"/>
            </xsl:attribute>
            <xsl:apply-templates/>
        </field>
    </xsl:template>

    <xsl:template match="dri:div[@id='aspect.artifactbrowser.ConfigurableBrowse.div.browse-navigation'
    or @id='aspect.administrative.PrivateItems.div.browse-navigation'
    or @id='aspect.administrative.WithdrawnItems.div.browse-navigation' or @id='aspect.discovery.SearchFacetFilter.div.filter-navigation']">
        <div rend="browse-navigation-wrapper hidden-print">
            <div>
                <xsl:call-template name="copy-attributes"/>
                <xsl:choose>
                    <xsl:when test="dri:list[@rend='alphabet']">
                        <div rend="row">
                            <div rend="col-xs-4 col-sm-12">
                                <xsl:apply-templates select="dri:list[@rend='alphabet']"/>
                            </div>
                            <div rend="col-xs-8 col-sm-12">
                                <xsl:apply-templates select="*[not(self::dri:list[@rend='alphabet'])]"/>
                            </div>
                        </div>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:apply-templates/>
                    </xsl:otherwise>
                </xsl:choose>
            </div>
        </div>
    </xsl:template>

    <xsl:template match="dri:list[@rend='alphabet']">
        <xsl:variable name="current-value">
            <xsl:value-of select="$page-meta/dri:metadata[@element='request'][@qualifier='URI']"/>
            <xsl:text>?</xsl:text>
            <xsl:value-of select="$page-meta/dri:metadata[@element='request'][@qualifier='queryString']"/>
        </xsl:variable>
        <field type="select" rend="alphabet-select visible-xs">
        <xsl:for-each select="dri:item/dri:xref">
                <option returnValue="{@target}">
                    <xsl:value-of select="."/>
                </option>
            </xsl:for-each>
            <value type="option" option="{$current-value}"/>
        </field>
        <list>
            <xsl:call-template name="copy-attributes"/>
            <xsl:attribute name="rend">
                <xsl:text>alphabet list-inline hidden-xs</xsl:text>
            </xsl:attribute>
            <xsl:apply-templates/>
        </list>

    </xsl:template>

    <xsl:template match="dri:div[dri:div[@id = 'aspect.discovery.SearchFacetFilter.div.filter-navigation']]">
        <div>
            <xsl:call-template name="copy-attributes"/>
            <xsl:variable name="results-id" select="concat(@id, '-results')"/>
            <xsl:copy-of select="//dri:div[@id = $results-id]/dri:head"/>
            <xsl:apply-templates/>
        </div>
    </xsl:template>

    <xsl:template match="dri:div[@id = 'aspect.artifactbrowser.ConfigurableBrowse.div.browse-controls'
    or @id='aspect.administrative.WithdrawnItems.div.browse-controls'
    or @id='aspect.administrative.PrivateItems.div.browse-controls'
    or @id='aspect.discovery.SearchFacetFilter.div.browse-controls']">
        <div>
            <xsl:call-template name="copy-attributes"/>
                <xsl:attribute name="rend">
                    <xsl:value-of select="@rend"/>
                    <xsl:text> hidden</xsl:text>
                </xsl:attribute>
            <xsl:apply-templates/>
        </div>
    </xsl:template>

    <xsl:template match="dri:div[starts-with(@id, 'aspect.discovery.SearchFacetFilter.div.browse-by-')][@pagination = 'simple']/dri:head"/>


    <xsl:template match="dri:list[@id='aspect.browseArtifacts.CommunityBrowse.list.community-browse' or @id='aspect.browseArtifacts.CollectionBrowse.list.collection-browse'][dri:head]">
        <div>
            <xsl:call-template name="copy-attributes"/>

            <xsl:apply-templates select="dri:head"/>

            <p>
                <xsl:attribute name="rend">
                    <xsl:text> btn-group</xsl:text>
                </xsl:attribute>
                <xsl:apply-templates select="*[not(name()='head')]"/>

            </p>

        </div>
    </xsl:template>

    <xsl:template
            match="dri:list[@id='aspect.browseArtifacts.CommunityBrowse.list.community-browse' or @id='aspect.browseArtifacts.CollectionBrowse.list.collection-browse']/dri:item[dri:xref]">

        <xref>
            <xsl:call-template name="copy-attributes"/>
            <xsl:attribute name="rend">
                <xsl:value-of select="dri:xref/@rend"/>
                <xsl:text> btn btn-default </xsl:text>
            </xsl:attribute>
            <xsl:attribute name="target">
                <xsl:value-of select="dri:xref/@target"/>
            </xsl:attribute>
            <xsl:apply-templates select="dri:xref/*"/>
        </xref>
    </xsl:template>

    <xsl:template
            match="dri:field[@id='aspect.administrative.WithdrawnItems.field.rpp' or @id='aspect.administrative.PrivateItems.field.rpp']/dri:option">

       <xsl:if test="@returnValue=5 or @returnValue=10 or @returnValue=20 or @returnValue=40 or @returnValue=60
                                    or @returnValue=80 or @returnValue=100">
            <option>
                <xsl:call-template name="copy-attributes"/>
                <xsl:apply-templates />
            </option>
       </xsl:if>
    </xsl:template>



</xsl:stylesheet>
