<?xml version="1.0" encoding="UTF-8" ?>
<!-- 
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:doc="http://www.lyncode.com/xoai" 
    xmlns:bib="http://lindat.mff.cuni.cz/ns/experimental/bibtex"
    xmlns:fn="http://custom.crosswalk.functions"
    xmlns:confman="org.dspace.core.ConfigurationManager"
    exclude-result-prefixes="doc fn confman"
    version="1.0">
    
    <!-- repository name -->
    <xsl:variable name="dspace.name" select="fn:getProperty('dspace.name')"/>

    <xsl:output omit-xml-declaration="yes" method="xml" indent="yes" cdata-section-elements="bib:bibtex"/>
    
    <xsl:template match="/">
        <bib:bibtex xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://lindat.mff.cuni.cz/ns/experimental/bibtex http://lindat.mff.cuni.cz/schemas/experimental/bibtex.xsd">
        @misc{<xsl:value-of select="doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text()"/>,
            <xsl:variable name="title"><xsl:call-template name="title"/></xsl:variable>
            <xsl:variable name="author"><xsl:call-template name="author"/></xsl:variable>
            <xsl:variable name="url"><xsl:call-template name="url"/></xsl:variable>
            <xsl:variable name="institution"><xsl:call-template name="institution"/></xsl:variable>
            <xsl:variable name="keywords"><xsl:call-template name="keywords"/></xsl:variable>
            <xsl:variable name="copyright"><xsl:call-template name="copyright"/></xsl:variable>
            <xsl:variable name="year"><xsl:call-template name="year"/></xsl:variable>
            <xsl:if test="$title != ''">
                    <xsl:value-of select="fn:format($title)"/>
            </xsl:if>
            <xsl:if test="$author != ''">
                    <xsl:value-of select="fn:format($author)"/>
            </xsl:if>
            <xsl:if test="$url != ''">
                    <xsl:value-of select="fn:format($url)"/>
            </xsl:if>
            <xsl:if test="$institution != ''">
                    <xsl:value-of select="fn:format($institution)"/>
            </xsl:if>
            <xsl:if test="$copyright != ''">
            <!-- See the keywords template comment
            <xsl:value-of select="util:format($keywords)"/>
            -->
                    <xsl:value-of select="fn:format($copyright)"/>
            </xsl:if>
            <xsl:if test="$year != ''">
                    <xsl:value-of select="fn:format($year)"/>}
            </xsl:if>
        </bib:bibtex>
    </xsl:template>
    
    <xsl:template name="title">
        <xsl:if test="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']">
                title = {<xsl:value-of select="fn:bibtexify(doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value'])"/>},
        </xsl:if>
    </xsl:template>
    
    <xsl:template name="author">
            <xsl:choose>
                    <xsl:when test="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value']">
                        author = {<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value']">
                                                    <xsl:value-of select="fn:bibtexify(.)"/>
                                                    <xsl:if test="position() != last()"> and  </xsl:if>
                                  </xsl:for-each>},
                    </xsl:when>
                    <xsl:when test="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='other']/doc:element/doc:field[@name='value']">
                        author = {<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='other']/doc:element/doc:field[@name='value']">
                                                    <xsl:value-of select="fn:bibtexify(.)"/>
                                                    <xsl:if test="position() != last()"> and  </xsl:if>
                                  </xsl:for-each>},
                    </xsl:when>
            </xsl:choose>
    </xsl:template>
    
    <!-- Do not escape -->
    <xsl:template name="url">
        <xsl:if test="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field[@name='value']">
            url = {<xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field[@name='value']"/>},
        </xsl:if>
    </xsl:template>
    
    <xsl:template name="institution">
	note = {<xsl:value-of select="fn:bibtexify($dspace.name)"/>},
    </xsl:template>
    
    <!-- was mapped to dc.keywords but that does not exist -->
    <xsl:template name="keywords">
    </xsl:template>
    
    <xsl:template name="copyright">
        <xsl:choose>
                <xsl:when test="doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element/doc:field[@name='value']">
                        copyright = {<xsl:value-of select="fn:bibtexify(doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element/doc:field[@name='value'])"/>},
                </xsl:when>
                <xsl:when test="doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#DistributionInfo#LicenseInfo']/doc:element[@name='license']/doc:element/doc:field[@name='value']">
                        copyright = {<xsl:value-of select="fn:bibtexify(doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#DistributionInfo#LicenseInfo']/doc:element[@name='license']/doc:element/doc:field[@name='value'])"/>},
                </xsl:when>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template name="year">
        <xsl:if test="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']">
                year = {<xsl:value-of select="substring(doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value'],1,4)"/>}
        </xsl:if>
    </xsl:template>
</xsl:stylesheet>
