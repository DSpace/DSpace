<?xml version="1.0" encoding="UTF-8"?>
<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<!--
    TODO: Describe this XSL file
    Author: Alexey Maslov
    
-->

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
        exclude-result-prefixes="mets xlink xsl dim xhtml mods dc">
    
    <xsl:output indent="yes"/>

    <!-- Finally, the following templates match list types not mentioned above. They work for lists of type
        'simple' as well as any unknown list types. -->
    <xsl:template match="dri:list[@n='memberships']">
        <xsl:if test="count(dri:item)>1">
	        <xsl:apply-templates select="dri:head" mode="memberships"/>
	        <ul>
	            <xsl:call-template name="standardAttributes">
	                <xsl:with-param name="class">ds-simple-list</xsl:with-param>
	            </xsl:call-template>
	            <xsl:apply-templates select="*[not(name()='head')]" mode="memberships"/>
	        </ul>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="dri:head" mode="memberships">
        <h2>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-head-visible</xsl:with-param>
            </xsl:call-template>
            <i18n:text><xsl:value-of select="."/></i18n:text>
        </h2>
    </xsl:template>

    
    <!-- Generic item handling for cases where nothing special needs to be done -->
    <xsl:template match="dri:item" mode="memberships">
    <xsl:choose>
    <xsl:when test="node()!='Anonymous'">
   	    <li>
            <xsl:apply-templates />
        </li>
    </xsl:when>
    </xsl:choose>

    </xsl:template>
   

</xsl:stylesheet>
