<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" exclude-result-prefixes="xs" version="1.0">
    <xsl:output method="text" omit-xml-declaration="yes"/>
    <xsl:template match="/">
        <xsl:text>Title: </xsl:text>
        <xsl:value-of select="metadata/value[@element='title']"/>
	<xsl:text>&#10;Access: Institution</xsl:text>
        <xsl:text>&#10;Storage-Option: Glacier-Deep-OR</xsl:text>
    </xsl:template>
</xsl:stylesheet>
