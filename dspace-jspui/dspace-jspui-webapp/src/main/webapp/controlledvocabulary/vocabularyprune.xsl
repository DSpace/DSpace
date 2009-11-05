<?xml version="1.0" encoding="UTF-8"?>
<!-- 
This stylesheet prunnes out all the branches of
the taxonomy that don't instanciate with the param "filter", i.e. tha term
provided as parameter does not exist in any of its children.
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<!-- ************************************ -->
	<xsl:output method="xml" version="1.0" indent="yes" encoding="utf-8"/>
	<!-- ************************************ -->
	<xsl:param name="filter"/>
	<!--	<xsl:variable name="filter">programming</xsl:variable> -->
	<xsl:variable name="lcletters">abcdefghijklmnopqrstuvwxyz</xsl:variable>
	<xsl:variable name="ucletters">ABCDEFGHIJKLMNOPQRSTUVWXYZ</xsl:variable>
	<!-- ************************************ -->
	<xsl:template match="/">
		<xsl:apply-templates/>
	</xsl:template>
	<!-- ************************************ -->
	<xsl:template match="node">
		<xsl:choose>
			<xsl:when test="descendant-or-self::node[contains(translate(@label,$lcletters,$ucletters), translate($filter,$lcletters,$ucletters))]">
				<node>
					<xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute>
					<xsl:attribute name="label"><xsl:value-of select="normalize-space(@label)"/></xsl:attribute>
					<xsl:apply-templates/>
				</node>
			</xsl:when>
		</xsl:choose>
	</xsl:template>
	<!-- ************************************ -->
	<xsl:template match="isComposedBy">
		<xsl:choose>
			<xsl:when test="descendant-or-self::node[contains(translate(@label,$lcletters,$ucletters), translate($filter,$lcletters,$ucletters))]">
				<isComposedBy>
					<xsl:apply-templates/>
				</isComposedBy>
			</xsl:when>
		</xsl:choose>
	</xsl:template>
	<!-- ************************************ -->
	<xsl:template match="text()" priority="-1"/>
	<!-- ************************************ -->
</xsl:stylesheet>
