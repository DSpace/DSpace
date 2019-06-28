<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:doc="http://www.lyncode.com/xoai">
	<xsl:output indent="yes" method="xml" omit-xml-declaration="yes" />
	<xsl:include href="driver-commons.xsl"/>
	
	<xsl:variable name="context" select="'doaj'"/>
	
	<xsl:template match="/doc:metadata">
		<doc:metadata>
			<xsl:call-template name="sedici-identifier">
				<xsl:with-param name="handle" select="doc:element[@name='others']/doc:field[@name='handle']/text()"/>
				<xsl:with-param name="context-name" select="$context"/>
			</xsl:call-template>
			<xsl:apply-templates select="@*|node()"/>
		</doc:metadata>
	</xsl:template>	
	
	<!-- This template transforms the language's value of an attribute in a "first-level" <doc:element> tag pass as a parameter.
	It applies to the following XML structure:
	<doc:element name="(es|en|pt|it|fr|etc...)"> a_language_code_accord_to_ISO_639_1B
		<doc:field>SOME_ARBITRARY_VALUE</doc:field>
	</doc:element>
	-->
	<xsl:template match="doc:element" mode="transformLanguageInAttribute">
		<doc:element>
			<xsl:attribute name="name">
				<xsl:call-template name="languageTransformer">
					<xsl:with-param name="language" select="./@name"/>
				</xsl:call-template>
			</xsl:attribute>
			<xsl:copy-of select=".//*"/>
		</doc:element>
	</xsl:template>
	
	<!-- Keywords accord ISO 639-2B:dc.subject -->
	<xsl:template match="doc:element[@name='dc']/doc:element[@name='subject']">
		<xsl:apply-templates select="." mode="transformLanguageInAttribute"/>
	</xsl:template>

	<!-- Keywords accord ISO 639-2B:sedici.subject.(materias) -->
	<xsl:template match="doc:element[@name='sedici']/doc:element[@name='subject']/doc:element[@name='materias']//doc:element">
		<xsl:apply-templates select="." mode="transformLanguageInAttribute"/>
	</xsl:template>
	<!-- dc.abstract accord ISO 639-2B -->
	<xsl:template match="doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='abstract']//doc:element">
		<xsl:apply-templates select="." mode="transformLanguageInAttribute"/>
	</xsl:template>
	
	<!-- dc.title accord ISO 639-2B -->
	<xsl:template match="doc:element[@name='dc']/doc:element[@name='title']/doc:element[@name != 'alternative']">
		<xsl:apply-templates select="." mode="transformLanguageInAttribute"/>
	</xsl:template>
	
	<!-- dc.title.alternative accord ISO 639-2B -->
	<xsl:template match="doc:element[@name='dc']/doc:element[@name='title']/doc:element[@name='alternative']/doc:element">
		<xsl:apply-templates select="." mode="transformLanguageInAttribute"/>
	</xsl:template>
	
	<xsl:template match="doc:element[@name='dc']/doc:element[@name='language']">
		<doc:element name="language">
			<doc:element name="es">
				<doc:field name="value">
					<xsl:call-template name="languageTransformer">
						<xsl:with-param name="language" select="./doc:element/doc:field[@name='value']/text()"/>
					</xsl:call-template>
				</doc:field>
			</doc:element>
		</doc:element>
	</xsl:template>
	
	<!-- Language transformer - ISO 639-2-b-->
	<xsl:template name="languageTransformer">
		<xsl:param name="language"/>
		
		<xsl:variable name="valueLanguage">
			<xsl:choose>
				<xsl:when test="$language='es'">
					spa
				</xsl:when>
				<xsl:when test="$language='en'">
					eng
				</xsl:when>
				<xsl:when test="$language='pt'">
					por
				</xsl:when>
				<xsl:when test="$language='fr'">
					fra
				</xsl:when>
				<xsl:when test="$language='it'">
					ita
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="$language"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		
		<xsl:value-of select="normalize-space($valueLanguage)"/>
			
	</xsl:template>
	
	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" />
		</xsl:copy>
	</xsl:template>
	
</xsl:stylesheet>