<?xml version="1.0" encoding="UTF-8" ?>
<!-- 

	The contents of this file are subject to the license and copyright detailed 
	in the LICENSE and NOTICE files at the root of the source tree and available 
	online at http://www.dspace.org/license/ 
	
	Developed by DSpace @ Lyncode <dspace@lyncode.com> 
	
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:doc="http://www.lyncode.com/xoai"
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim" version="1.0">
    <xsl:output omit-xml-declaration="yes" method="xml" indent="yes"/>

    <!-- An identity transformation to show the internal XOAI generated XML -->
    <xsl:template match="/">
        <dim:dim xmlns:dim="http://www.dspace.org/xmlns/dspace/dim" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://www.dspace.org/xmlns/dspace/dim http://www.dspace.org/schema/dim.xsd">
            <xsl:apply-templates select="//doc:field[@name='value']"/>
        </dim:dim>
    </xsl:template>

    <xsl:template match="/doc:metadata/doc:element/doc:element/doc:element/doc:field[@name='value']">
        <xsl:call-template name="dimfield">
            <xsl:with-param name="mdschema" select="../../../@name"/>
            <xsl:with-param name="element" select="../../@name"/>
            <xsl:with-param name="qualifier"/>
            <xsl:with-param name="language" select="../@name"/>
            <xsl:with-param name="authority" select="../doc:field[@name='authority']"/>
            <xsl:with-param name="confidence" select="../doc:field[@name='confidence']"/>
            <xsl:with-param name="value" select="text()"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="/doc:metadata/doc:element/doc:element/doc:element/doc:element/doc:field[@name='value']">
        <xsl:call-template name="dimfield">
            <xsl:with-param name="mdschema" select="../../../../@name"/>
            <xsl:with-param name="element" select="../../../@name"/>
            <xsl:with-param name="qualifier" select="../../@name"/>
            <xsl:with-param name="language" select="../@name" />
            <xsl:with-param name="authority" select="../doc:field[@name='authority']"/>
            <xsl:with-param name="confidence" select="../doc:field[@name='confidence']"/>
            <xsl:with-param name="value" select="text()"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="dimfield">
        <xsl:param name="mdschema"/>
        <xsl:param name="element"/>
        <xsl:param name="qualifier"/>
        <xsl:param name="language"/>
        <xsl:param name="authority"/>
        <xsl:param name="confidence"/>
        <xsl:param name="value"/>

        <dim:field>
            <xsl:attribute name="mdschema">
                <xsl:value-of select="$mdschema"/>
            </xsl:attribute>

            <xsl:attribute name="element">
                <xsl:value-of select="$element"/>
            </xsl:attribute>

            <xsl:if test="$qualifier">
                <xsl:attribute name="qualifier">
                    <xsl:value-of select="$qualifier"/>
                </xsl:attribute>
            </xsl:if>

            <xsl:choose>
                <xsl:when test="$language='none'"/>
                <xsl:otherwise>
                    <xsl:attribute name="lang">
                        <xsl:value-of select="$language"/>
                    </xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>

            <xsl:if test="$authority">
                <xsl:attribute name="authority">
                    <xsl:value-of select="$authority"/>
                </xsl:attribute>
            </xsl:if>

            <xsl:if test="$confidence">
                <xsl:attribute name="confidence">
                    <xsl:value-of select="$confidence"/>
                </xsl:attribute>
            </xsl:if>

            <xsl:value-of select="$value"/>
        </dim:field>
    </xsl:template>

</xsl:stylesheet>
