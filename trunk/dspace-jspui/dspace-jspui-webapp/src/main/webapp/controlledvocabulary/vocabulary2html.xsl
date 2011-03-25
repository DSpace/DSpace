<?xml version="1.0" encoding="UTF-8"?>
<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<!--
This stylesheet converts taxonomies from their XML representation to
an HTML tree. Its basically a preety-printer.
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<!-- ************************************ -->
	<xsl:output method="xml" version="1.0" omit-xml-declaration="yes" indent="yes" encoding="utf-8"/>
	<!-- ************************************ -->
	<xsl:param name="allowMultipleSelection"/>
	<xsl:param name="contextPath"/>
	
<!--<xsl:variable name="allowMultipleSelection">no</xsl:variable> -->
	<!-- ************************************ -->
	<xsl:template match="/">
		<ul class="controlledvocabulary">
			<xsl:apply-templates/>
			<li/>
		</ul>
	</xsl:template>
	<!-- ************************************ -->
	<xsl:template match="isComposedBy">
		<xsl:apply-templates select="node"/>
	</xsl:template>
	<!-- ************************************ -->
	<xsl:template match="node">
		<li>
			<!--** place icon ** -->
			<xsl:choose>
				<xsl:when test="./isComposedBy/node">
					<img class="controlledvocabulary">
						<xsl:attribute name="src"><xsl:value-of select="$contextPath"/>/image/controlledvocabulary/p.gif</xsl:attribute>
						<xsl:attribute name="onclick">ec(this, '<xsl:value-of select="$contextPath"/>');</xsl:attribute>
						<xsl:attribute name="alt">expand search term category</xsl:attribute>
					</img>
				</xsl:when>
				<xsl:otherwise>
					<img class="dummyclass">
						<xsl:attribute name="src"><xsl:value-of select="$contextPath"/>/image/controlledvocabulary/f.gif</xsl:attribute>
						<xsl:attribute name="alt">search term</xsl:attribute>
					</img>
				</xsl:otherwise>
			</xsl:choose>
			<!--** place check box if necessary** -->
			<xsl:choose>
				<xsl:when test="$allowMultipleSelection='yes'">
					<xsl:variable name="nodePath">
						<xsl:call-template name="getNodePath"/>
					</xsl:variable>
					<xsl:variable name="checkBoxName">cb_<xsl:value-of select="generate-id()"/>
					</xsl:variable>
					<input class="controlledvocabulary" type="checkbox" name="{$checkBoxName}" value="{$nodePath}"/>
					<xsl:value-of select="@label"/>
				</xsl:when>
				<xsl:otherwise>
					<a class="value" onclick="javascript: i(this);" href="javascript:void(null);">
						<xsl:value-of select="@label"/>
					</a>
				</xsl:otherwise>
			</xsl:choose>
			<!--** render children ** -->
			<xsl:if test="./isComposedBy/node">
				<ul class="controlledvocabulary">
					<xsl:apply-templates select="isComposedBy"/>
				</ul>
			</xsl:if>
		</li>
	</xsl:template>
	<!-- ************************************ -->
	<xsl:template match="text"/>
	<!-- ************************************ -->
	<xsl:template match="text()" priority="-1"/>
	<!-- ************************************ -->
	<xsl:template name="getNodePath">
		<xsl:for-each select="ancestor::node"><xsl:value-of select="@label"/>::</xsl:for-each><xsl:value-of select="@label"/>
	</xsl:template>
	<!-- ************************************ -->
</xsl:stylesheet>
