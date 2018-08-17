<?xml version="1.0" encoding="UTF-8"?>
<!-- 

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

	Developed by DSpace @ Lyncode <dspace@lyncode.com> 
	Following OpenAIRE Guidelines 1.1:
		- http://www.openaire.eu/component/content/article/207

 -->
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:doc="http://www.lyncode.com/xoai" xmlns:dc="http://purl.org/dc/doc:elements/1.1/" >
	<xsl:output indent="yes" method="xml" omit-xml-declaration="yes" />

	<xsl:include href="driver-commons.xsl"/>
	
	<xsl:template match="/doc:metadata">
	  <doc:metadata>
	  		
				  	
	  	<xsl:variable name="context" select="'snrd'"/>
		
		<xsl:call-template name="sedici-identifier">
			<xsl:with-param name="handle" select="doc:element[@name='others']/doc:field[@name='handle']/text()"/>
			<xsl:with-param name="context-name" select="$context"/>
		</xsl:call-template>
			
	  		<xsl:variable name="subtype" select="doc:element[@name='sedici']/doc:element[@name='subtype']/doc:element/doc:field/text()"/>
			<xsl:call-template name="driver-type">
				<xsl:with-param name="subtype" select="$subtype"/>
			</xsl:call-template>
			
			<xsl:call-template name="snrd-type">
				<xsl:with-param name="subtype" select="$subtype"/>
			</xsl:call-template>
			
			<xsl:call-template name="driver-version">
				<xsl:with-param name="subtype" select="$subtype"/>
			</xsl:call-template>
			
			<!-- Aclaración: a partir de la versión 2015, la fecha de embargo debe mostrarse dentro de un <dc:date> como
			segunda instancia. En el 'driver-commons.xsl' sólo se muestra alguna de las siguientes 2 fechas: dc.date.issued ó
			sedici.date.exposure (si existe). Por lo tanto, el embargoDate siempre se mostrará segundo... -->
			<xsl:call-template name="accessRightsAndEmbargo">
				<xsl:with-param name="liftDate" select="doc:element[@name='sedici']/doc:element[@name='embargo']/doc:element[@name='liftDate']/doc:element/doc:field/text()"/>
				<xsl:with-param name="context-name" select="$context"/>
			</xsl:call-template>
			
			<xsl:if test="not(doc:element[@name='sedici']/doc:element[@name='rights']/doc:element[@name='uri']/doc:element/doc:field)">	
				<xsl:call-template name="addSEDICIRepositoryLicense"/>
			</xsl:if>

			<!-- Ver "Idenficadores alternativos" en SNRD v.2015 -->
			<!--  sedici.identifier.doi, sedici.identifier.handle, sedici.identifier.isbn -->
			<xsl:for-each select="doc:element[@name='sedici']/doc:element[@name='identifier']/doc:element[@name='doi' or @name='handle' or @name='isbn']/doc:element/doc:field">
				<xsl:call-template name="printAlternativeIdentifier">
					<xsl:with-param name="type"><xsl:value-of select="../../@name"/></xsl:with-param>
					<xsl:with-param name="value"><xsl:value-of select="./text()"/></xsl:with-param>
				</xsl:call-template>
			</xsl:for-each>
			
			<xsl:apply-templates select="@*|node()" />

	  </doc:metadata>
	</xsl:template>

	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" />
		</xsl:copy>
	</xsl:template>


	<!-- Genero el nuevo type en base a nuestro subtype -->
	<xsl:template name="snrd-type" >
		<xsl:param name="subtype" />
		
		<xsl:variable name="snrd_type_snrd">
		<xsl:choose>
			<xsl:when test="$subtype='Documento de trabajo'">
				documento de trabajo
			</xsl:when>
			<!-- No se exporta
			<xsl:when test="$subtype='Preprint'">
				artículo
			</xsl:when> -->
			<xsl:when test="$subtype='Articulo'">
				artículo
			</xsl:when>
			<xsl:when test="$subtype='Resumen'">
				documento de conferencia
			</xsl:when>
			<!-- No se exporta
			<xsl:when test="$subtype='Comunicacion'">
				artículo
			</xsl:when> -->
			<xsl:when test="$subtype='Revision'">
				reseña artículo
			</xsl:when>
			<!-- No se exporta
			<xsl:when test="$subtype='Contribucion a revista'">
				artículo
			</xsl:when> -->
			<xsl:when test="$subtype='Reporte'">
				informe técnico
			</xsl:when>
			<xsl:when test="$subtype='Patente'">
				patente
			</xsl:when>
			<xsl:when test="$subtype='Tesis de doctorado'">
				tesis doctoral
			</xsl:when>
			<xsl:when test="$subtype='Tesis de maestria'">
				tesis de maestría
			</xsl:when>
			<xsl:when test="$subtype='Trabajo de especializacion'">
				tesis de maestría
			</xsl:when>
			<xsl:when test="$subtype='Tesis de grado'">
				tesis de grado
			</xsl:when>
			<xsl:when test="$subtype='Trabajo final de grado'">
				trabajo final de grado
			</xsl:when>
			<xsl:when test="$subtype='Libro'">
				libro
			</xsl:when>
			<xsl:when test="$subtype='Capitulo de libro'">
				parte de libro
			</xsl:when>
			<xsl:when test="$subtype='Objeto de conferencia'">
				documento de conferencia
			</xsl:when>
			<xsl:when test="$subtype='Conjunto de datos'">
				conjunto de datos
			</xsl:when>
			<!-- No se exportan
			<xsl:when test="$subtype='Documento institucional'">
				otros
			</xsl:when>
			<xsl:when test="$subtype='Resolucion'">
				otros
			</xsl:when>
			<xsl:when test="$subtype='Imagen fija'">
				fotografía
			</xsl:when> 
 			<xsl:when test="$subtype='Imagen en movimiento'">
				videograbación
			</xsl:when>
 			<xsl:when test="$subtype='Audio'">
				otros
			</xsl:when> -->
			<xsl:otherwise>
				otros
			</xsl:otherwise>
		</xsl:choose>
		</xsl:variable>
		<!-- Prefijo añadido desde las Directrices SNRD v.2015 -->
		<xsl:variable name="snrd_type_prefix">info:ar-repo/semantics/</xsl:variable>

		<doc:element name='snrd'>
			<doc:element name='type'>
				<doc:element name='es'>
						<doc:field name="value"><xsl:value-of select="concat($snrd_type_prefix, normalize-space($snrd_type_snrd))"/></doc:field>
				</doc:element>
			</doc:element>
		</doc:element>
		
	</xsl:template>
	
	<xsl:template name="addSEDICIRepositoryLicense">
		<doc:element name="sedici">
			<doc:element name="rights">
	            <doc:element name="license">
	            	<doc:element name="es">
						<doc:field name="value">
							<xsl:value-of select="'Licencia de distribución no exclusiva SEDICI'"/>
						</doc:field>
					</doc:element>
	            </doc:element>
			</doc:element>
		</doc:element>
	</xsl:template>
	
	<!-- Ver "Idenficadores alternativos" en SNRD v.2015 -->
	<!--  Silenciamos sedici.identifier.doi, sedici.identifier.handle, sedici.identifier.isbn -->
	<xsl:template match="doc:metadata/doc:element[@name='sedici']/doc:element[@name='identifier']/doc:element[@name='doi' or @name='handle' or @name='isbn']"/>
	
	<!-- AUXILIARY TEMPLATES -->

	<!-- Éste template modifica el texto referido al lenguaje del item-->
	<xsl:template match="doc:metadata/doc:element[@name='dc']/doc:element[@name='language']">
		
		<xsl:variable name="language" select="./doc:element/doc:field[@name='value']/text()"/>
		
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
		
		<doc:element name="language">
            <doc:element name="es">
				<doc:field name="value">
					<xsl:value-of select="normalize-space($valueLanguage)"/>
				</doc:field>
			</doc:element>
		</doc:element>	
	</xsl:template>
	
	<xsl:template name="printAlternativeIdentifier">
		<xsl:param name="type"/>
		<xsl:param name="value"/>
		
			<doc:element name="snrd">
				<doc:element name="alternativeIdentifier">
					<doc:field name="value">
						<xsl:value-of select="concat('info:eu-repo/semantics/altIdentifier/',$type,'/', $value)"/>
					</doc:field>
				</doc:element>
			</doc:element>
		
	</xsl:template>
	
</xsl:stylesheet>