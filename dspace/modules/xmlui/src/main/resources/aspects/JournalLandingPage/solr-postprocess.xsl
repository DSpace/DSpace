<?xml version="1.0" encoding="UTF-8"?>
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
                exclude-result-prefixes="confman dc dim dri i18n mets mods xhtml xlink xsl">
    
    <xsl:output method="xml"/>
    
    <xsl:template match="/">
        <xsl:apply-templates/>
    </xsl:template>
    
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
    
    <!-- the sitemap's ordering of transformers is meant to run the solr queries as
         quickly as possible; this re-orders for final page layout    
    -->
    <xsl:template match="//dri:body/@*"/>
    <xsl:template match="//dri:body">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates select="dri:div[@id='aspect.journal.landing.JournalStats.div.journal-landing-banner-outer']"/>
            <xsl:apply-templates select="dri:div[@id='aspect.journal.landing.JournalStats.div.journal-landing-search']"/>
            <xsl:apply-templates select="dri:div[@id='aspect.journal.landing.JournalStats.div.journal-landing-stats']"/>
        </xsl:copy>
    </xsl:template>

    <!-- Suppress any stats lists panels that do not have items. 
    -->
    <xsl:template match="/dri:document/dri:body/dri:div[@id='aspect.journal.landing.JournalStats.div.journal-landing-stats']/dri:div">
        <xsl:choose>
            <xsl:when test="   count(./descendant::dri:referenceSet/processing-instruction('reference')) &gt; 0
                            or count(./descendant::dri:referenceSet/dri:reference) &gt; 0
            ">
                <xsl:copy>
                    <xsl:copy-of select="@*"/>
                    <xsl:apply-templates/>
                </xsl:copy>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <!-- Suppress statistics tablist items if the corresponding reference list has no items.
         Tab list items become the tab buttons on the landing page.
    -->
    <xsl:template match="//dri:list[@id='aspect.journal.landing.JournalStats.list.tablist']/dri:item">
        <xsl:variable name="index" select="count(preceding-sibling::dri:item) + 1"/>
        <xsl:variable name="stats-wrapper" select="ancestor::dri:div[@id='aspect.journal.landing.JournalStats.div.journal-landing-stats']"/>
        <xsl:variable name="reflist" select="$stats-wrapper/dri:div[$index]//dri:referenceSet"/>
        <xsl:choose>
            <xsl:when test="   count($reflist/processing-instruction('reference')) &gt; 0
                            or count($reflist/dri:reference) &gt; 0
            ">
                <xsl:copy>
                    <xsl:copy-of select="@*"/>
                    <xsl:apply-templates/>                    
                </xsl:copy>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <!-- preserve these pi's for serialization -->
    <xsl:template match="processing-instruction('reference')">
        <xsl:processing-instruction name="reference">
            <xsl:value-of select="."/>
        </xsl:processing-instruction>
    </xsl:template>

</xsl:stylesheet>
