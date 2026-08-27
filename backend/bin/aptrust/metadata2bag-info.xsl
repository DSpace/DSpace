<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" exclude-result-prefixes="xs" version="2.0">
    <xsl:output method="text" omit-xml-declaration="yes"/>
    <xsl:template match="/">
        <xsl:variable name="date" select="format-date(current-date(),'[Y0001]-[M01]-[D01]')"/>
        <xsl:text>Source-Organization: University of Michigan</xsl:text>
        <xsl:text>&#10;Bagging-Date: </xsl:text>
        <xsl:value-of select="$date"/>
        <xsl:text>&#10;Bag-Count: 1 of 1</xsl:text>
	<xsl:text>&#10;Internal-Sender-Identifier: </xsl:text>
        <xsl:value-of select="metadata/value[@element='identifier'][@qualifier='uri'][@schema='dc']/text()[contains(.,'2027.42')]"/>
    </xsl:template>
</xsl:stylesheet>
