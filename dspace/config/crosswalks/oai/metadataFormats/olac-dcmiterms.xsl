<?xml version="1.0" encoding="UTF-8" ?>

<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:doc="http://www.lyncode.com/xoai"
    xmlns:xalan="http://xml.apache.org/xslt"
    xmlns:olac="http://experimental.loc/olac"
    xmlns:fn="http://custom.crosswalk.functions"
    xmlns:fnx="http://www.w3.org/2005/xpath-functions"
    exclude-result-prefixes="doc xalan fn fnx"

    version="1.0">
    <xsl:output omit-xml-declaration="yes" method="xml" indent="yes" />
    
    <xsl:template match="/">
        <xsl:call-template name="OLAC_DCMI"/>
    </xsl:template>
    
    <xsl:template name="OLAC_DCMI">
        <olac:OLAC-DcmiTerms>
            <!-- abstract -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='abstract']/doc:element/doc:field[@name='value'])">
                <olac:abstract><xsl:value-of select="."/></olac:abstract>
            </xsl:for-each>
            <!-- alternative -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element[@name='alternative']/doc:element/doc:field[@name='value'])">
                <olac:alternative><xsl:value-of select="."/></olac:alternative>
            </xsl:for-each>
            <!-- available -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='available']/doc:element/doc:field[@name='value'])">
                <olac:available><xsl:value-of select="."/></olac:available>
            </xsl:for-each>
            <!-- citation -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='citation']/doc:element/doc:field[@name='value'])">
                <olac:bibliographicCitation><xsl:value-of select="."/></olac:bibliographicCitation>
            </xsl:for-each>
            <!-- contributors -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name!='author']/doc:element/doc:field[@name='value']|doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element/doc:field[@name='value'])">
                <olac:contributor><xsl:value-of select="."/></olac:contributor>
            </xsl:for-each>
            <!-- created -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='copyright']/doc:element/doc:field[@name='value'])">
                <olac:created><xsl:value-of select="."/></olac:created>
            </xsl:for-each>
            <!-- creator -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value']|doc:metadata/doc:element[@name='dc']/doc:element[@name='creator']/doc:element/doc:field[@name='value'])">
                <olac:creator><xsl:value-of select="."/></olac:creator>
            </xsl:for-each>
            <!-- date -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='accessioned' or @name='updated']/doc:element/doc:field[@name='value']|doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element/doc:field[@name='value'])">
                <olac:date><xsl:value-of select="."/></olac:date>
            </xsl:for-each>
            <!-- copyright -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='copyright']/doc:element/doc:field[@name='value'])">
                <olac:dateCopyrighted><xsl:value-of select="."/></olac:dateCopyrighted>
            </xsl:for-each>
            <!-- submitted -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='submitted']/doc:element/doc:field[@name='value'])">
                <olac:dateSubmitted><xsl:value-of select="."/></olac:dateSubmitted>
            </xsl:for-each>
            <!-- description -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='sponsorship' or @name='statementofresponsibility' or @name='uri' or @name='version']/doc:element/doc:field[@name='value']|doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element/doc:field[@name='value'])">
                <olac:description><xsl:value-of select="."/></olac:description>
            </xsl:for-each>
            <!-- extent -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='format']/doc:element[@name='extent']/doc:element/doc:field[@name='value'])">
                <olac:extent><xsl:value-of select="."/></olac:extent>
            </xsl:for-each>
            <!-- format -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='format']/doc:element[@name='mimetype']/doc:element/doc:field[@name='value']|doc:metadata/doc:element[@name='dc']/doc:element[@name='format']/doc:element/doc:field[@name='value'])">
                <olac:format><xsl:value-of select="."/></olac:format>
            </xsl:for-each>
            <!-- haspart -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='haspart']/doc:element/doc:field[@name='value'])">
                <olac:hasPart><xsl:value-of select="."/></olac:hasPart>
            </xsl:for-each>
            <!-- hasversion -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='hasversion']/doc:element/doc:field[@name='value'])">
                <olac:hasVersion><xsl:value-of select="."/></olac:hasVersion>
            </xsl:for-each>
            <!-- identifier -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name!='citation']/doc:element/doc:field[@name='value']|doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element/doc:field[@name='value'])">
                <olac:identifier><xsl:value-of select="."/></olac:identifier>
            </xsl:for-each>
            <!-- isformatof -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='isformatof']/doc:element/doc:field[@name='value'])">
                <olac:isFormatOf><xsl:value-of select="."/></olac:isFormatOf>
            </xsl:for-each>
            <!-- ispartof -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='ispartof']/doc:element/doc:field[@name='value'])">
                <olac:isPartOf><xsl:value-of select="."/></olac:isPartOf>
            </xsl:for-each>
            <!-- isreferencedby -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='isreferencedby']/doc:element/doc:field[@name='value'])">
                <olac:isReferencedBy><xsl:value-of select="."/></olac:isReferencedBy>
            </xsl:for-each>
            <!-- isreplacedby -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='isreplacedby']/doc:element/doc:field[@name='value'])">
                <olac:isReplacedBy><xsl:value-of select="."/></olac:isReplacedBy>
            </xsl:for-each>
            <!-- issued -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value'])">
                <olac:issued><xsl:value-of select="."/></olac:issued>
            </xsl:for-each>
            <!-- isversionof -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='isversionof']/doc:element/doc:field[@name='value'])">
                <olac:isVersionOf><xsl:value-of select="."/></olac:isVersionOf>
            </xsl:for-each>
            <!-- language -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='language']//doc:field[@name='value'])">
                <olac:language><xsl:attribute name="olac-language"><xsl:value-of select="."/></xsl:attribute><xsl:value-of select="."/></olac:language>
            </xsl:for-each>
            <!-- medium -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='format']/doc:element[@name='medium']/doc:element/doc:field[@name='value'])">
                <olac:medium><xsl:value-of select="."/></olac:medium>
            </xsl:for-each>
            <!-- publisher -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='publisher']/doc:element/doc:field[@name='value'])">
                <olac:publisher><xsl:value-of select="."/></olac:publisher>
            </xsl:for-each>
            <!-- relation -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='ispartofseries' or @name='isbasedon' or @name='uri']/doc:element/doc:field[@name='value']|doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element/doc:field[@name='value'])">
                <olac:relation><xsl:value-of select="."/></olac:relation>
            </xsl:for-each>
            <!-- replaces -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='replaces']/doc:element/doc:field[@name='value'])">
                <olac:replaces><xsl:value-of select="."/></olac:replaces>
            </xsl:for-each>
            <!-- requires -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='requires']/doc:element/doc:field[@name='value'])">
                <olac:requires><xsl:value-of select="."/></olac:requires>
            </xsl:for-each>
            <!-- rights -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element[@name='uri']/doc:element/doc:field[@name='value']|doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element/doc:field[@name='value'])">
                <olac:rights><xsl:value-of select="."/></olac:rights>
            </xsl:for-each>
            <!-- source -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='source']/doc:element[@name='uri']/doc:element/doc:field[@name='value']|doc:metadata/doc:element[@name='dc']/doc:element[@name='source']/doc:element/doc:field[@name='value'])">
                <olac:source><xsl:value-of select="."/></olac:source>
            </xsl:for-each>
            <!-- spatial -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='coverage']/doc:element[@name='spatial']/doc:element/doc:field[@name='value'])">
                <olac:spatial><xsl:value-of select="."/></olac:spatial>
            </xsl:for-each>
            <!-- subject -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']//doc:field[@name='value'])">
                <olac:subject><xsl:value-of select="."/></olac:subject>
            </xsl:for-each>
            <!-- toc -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='tableofcontents']/doc:element/doc:field[@name='value'])">
                <olac:tableOfContents><xsl:value-of select="."/></olac:tableOfContents>
            </xsl:for-each>
            <!-- temporal -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='coverage']/doc:element[@name='temporal']/doc:element/doc:field[@name='value'])">
                <olac:temporal><xsl:value-of select="."/></olac:temporal>
            </xsl:for-each>
            <!-- title -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value'])">
                <olac:title><xsl:value-of select="."/></olac:title>
            </xsl:for-each>
            <!-- type -->
            <xsl:for-each select="fnx:distinct-values(doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value'])">
                <olac:type><xsl:value-of select="."/></olac:type>
            </xsl:for-each>
        </olac:OLAC-DcmiTerms>
    </xsl:template>
</xsl:stylesheet>
