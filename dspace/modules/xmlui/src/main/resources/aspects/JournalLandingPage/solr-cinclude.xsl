<?xml version="1.0" encoding="UTF-8"?>
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

    <!--  -->
    <xsl:template match="//dri:body/@*"/>
    
    <!-- for stats section, hard code order of tabs -->
    <xsl:template match="dri:div[@id='aspect.journal.landing.JournalStats.div.journal-landing-stats']">
        <xsl:copy>
            <xsl:apply-templates select="./@*|./node()"/>

            <xsl:variable name="baseUrl" select="string(//dri:metadata[@element='cinclude'][@qualifier='solr-base-url'])"/>

            <!-- month tab -->
            <xsl:call-template name="cinclude">
                <xsl:with-param name="baseUrl" select="$baseUrl"/>
                <xsl:with-param name="query"   select="string(//dri:metadata[@element='cinclude'][@qualifier='journal-landing-stats-month'])"/>
            </xsl:call-template>

            <!-- year tab -->
            <xsl:call-template name="cinclude">
                <xsl:with-param name="baseUrl" select="$baseUrl"/>
                <xsl:with-param name="query"   select="string(//dri:metadata[@element='cinclude'][@qualifier='journal-landing-stats-year'])"/>
            </xsl:call-template>

            <!-- alltime tab -->
            <xsl:call-template name="cinclude">
                <xsl:with-param name="baseUrl" select="$baseUrl"/>
                <xsl:with-param name="query"   select="string(//dri:metadata[@element='cinclude'][@qualifier='journal-landing-stats-alltime'])"/>
            </xsl:call-template>

        </xsl:copy>
    </xsl:template>

    <xsl:template name="cinclude">
        <xsl:param name="baseUrl"/>
        <xsl:param name="query"/>

        <xsl:element name="include" namespace="http://apache.org/cocoon/include/1.0">
           <xsl:attribute name="src">
                <xsl:value-of select="concat($baseUrl, $query)"/>
            </xsl:attribute>
        </xsl:element>

    </xsl:template>

    <!-- no need to send these down the pipeline -->
    <xsl:template match="dri:metadata[@element='cinclude']"/>

</xsl:stylesheet>
