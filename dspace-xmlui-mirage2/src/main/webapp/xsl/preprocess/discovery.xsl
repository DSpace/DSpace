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

    <xsl:template match="dri:list[@id='aspect.discovery.SimpleSearch.list.primary-search']">
        <list>
            <xsl:call-template name="copy-attributes"/>
            <item>
                <xsl:copy-of select="dri:item/dri:field[@id='aspect.discovery.SimpleSearch.field.scope']"/>
                <xsl:copy-of select="dri:item/dri:field[@id='aspect.discovery.SimpleSearch.field.query']"/>
                <xsl:copy-of select="dri:item[@id='aspect.discovery.SimpleSearch.item.did-you-mean']"/>
                <xsl:copy-of select="dri:item/dri:field[@id='aspect.discovery.SimpleSearch.field.submit']"/>
            </item>
        </list>
    </xsl:template>

    <xsl:template match="dri:row[@id='aspect.discovery.SimpleSearch.row.filter-controls']/dri:cell">
        <cell>
            <xsl:call-template name="copy-attributes"/>
            <field id="aspect.discovery.SimpleSearch.field.submit_reset_filter" rend="discovery-reset-filter-button" n="submit_reset_filter" type="button">
                <params/>
                <value type="raw">
                    <i18n:text>xmlui.mirage2.discovery.reset</i18n:text>
                </value>
            </field>
            <field rend="discovery-add-filter-button visible-xs " n="submit_add_filter" type="button">
                <params/>
                <value type="raw">
                    <i18n:text>xmlui.mirage2.discovery.newFilter</i18n:text>
                </value>
            </field>
            <xsl:apply-templates/>
        </cell>
    </xsl:template>

    <xsl:template match="dri:div[@id='aspect.discovery.SimpleSearch.div.search-filters']">
        <div>
            <xsl:call-template name="copy-attributes"/>
            <xsl:attribute name="rend">
                <xsl:call-template name="string-replace-all">
                    <xsl:with-param name="text" select="@rend"/>
                    <xsl:with-param name="replace" select="' hidden'"/>
                    <xsl:with-param name="by" select="''"/>
                </xsl:call-template>
            </xsl:attribute>
            <div rend="clearfix">
                <p rend="pull-right">
                    <xref target="#" rend="show-advanced-filters">
                        <i18n:text>xmlui.mirage2.discovery.showAdvancedFilters</i18n:text>
                    </xref>
                    <xref target="#" rend="hide-advanced-filters hidden">
                        <i18n:text>xmlui.mirage2.discovery.hideAdvancedFilters</i18n:text>
                    </xref>
                </p>
            </div>

            <xsl:apply-templates/>
        </div>
    </xsl:template>

    <xsl:template match="dri:div[@id='aspect.discovery.SimpleSearch.div.discovery-filters-wrapper']">
        <div>
            <xsl:call-template name="copy-attributes"/>
            <xsl:attribute name="rend">
                <xsl:value-of select="@rend"/>
                <xsl:text> hidden</xsl:text>
            </xsl:attribute>
            <xsl:apply-templates/>
        </div>
    </xsl:template>

    <xsl:template match="dri:div[@rend='controls-gear-wrapper' and @n='search-controls-gear']//dri:item[contains(@rend, 'gear-head')]">
        <item>
            <xsl:call-template name="copy-attributes"/>
            <xsl:attribute name="rend">
                <xsl:value-of select="@rend"/>
                <xsl:text> dropdown-header</xsl:text>
            </xsl:attribute>
            <xsl:apply-templates/>
        </item>
    </xsl:template>

    <xsl:template match="dri:div[@rend='controls-gear-wrapper' and @n='search-controls-gear']/dri:list//dri:list">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="dri:table[@id='aspect.discovery.SimpleSearch.table.discovery-filters']/dri:row[@role='header']"/>
    <xsl:template match="dri:table[@id='aspect.discovery.SimpleSearch.table.discovery-filters']/dri:row[@n='filler-row']"/>

    <xsl:template match="dri:field[@id='aspect.discovery.SimpleSearch.field.scope']/dri:label"/>

    <!--remove the shared discovery stylesheet, this theme has its own-->
    <xsl:template match="dri:meta/dri:pageMeta/dri:metadata[@element='stylesheet'][@qualifier='screen'][@lang='discovery']"/>

    <xsl:template match="dri:meta/dri:pageMeta/dri:metadata[@element='javascript'][@qualifier='static'][text()='static/js/discovery/search-controls.js']"/>

    <xsl:template match="dri:div[@id='aspect.discovery.SimpleSearch.div.search-results']/dri:head"/>


    <!--<xsl:template match="dri:hi[@rend='highlight']">-->
        <!--<hi>-->
            <!--<xsl:call-template name="copy-attributes"/>-->
            <!--<xsl:attribute name="rend">                -->
               <!--<xsl:text>highlight </xsl:text>-->
            <!--</xsl:attribute>-->
            <!--<xsl:apply-templates/>-->
        <!--</hi>-->
    <!--</xsl:template>-->

</xsl:stylesheet>
