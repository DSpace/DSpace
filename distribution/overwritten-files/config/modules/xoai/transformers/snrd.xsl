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

	<xsl:template match="/doc:metadata">
	  <doc:metadata>
	  		
			<xsl:call-template name="snrd-identifier">
				<xsl:with-param name="handle" select="doc:element[@name='others']/doc:field[@name='handle']/text()"/>
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
			
			<xsl:call-template name="snrd-accessRightsAndEmbargo">
				<xsl:with-param name="liftDate" select="doc:element[@name='sedici']/doc:element[@name='embargo']/doc:element[@name='liftDate']/doc:element/doc:field/text()"/>
			</xsl:call-template>
			
			<xsl:apply-templates select="@*|node()" />

	  </doc:metadata>
	</xsl:template>

	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" />
		</xsl:copy>
	</xsl:template>


	<!-- Remuevo sedici.identifier.other, no se exporta -->
	<xsl:template match="/doc:metadata/doc:element[@name='sedici']/doc:element[@name='identifier']/doc:element[@name='other']" />
	
	<!--  Genera el campo snrd.identifier.handle -->
	<xsl:template name="snrd-identifier" >
		<xsl:param name="handle" />
		
		<doc:element name='snrd'>
			<doc:element name='identifier'>
				<doc:element name='handle'>
					<doc:element>
						<doc:field name="value">http://sedici.unlp.edu.ar/handle/<xsl:value-of select="$handle"/></doc:field>
					</doc:element>
				</doc:element>
			</doc:element>
		</doc:element>		
	</xsl:template>
	
	
	<!-- Chequeo si es un doc embargado e imprimo la fecha de fin de embargo sedici.embargo.liftDate -->
	<xsl:template name="snrd-accessRightsAndEmbargo" >
		<xsl:param name="liftDate" />
		<xsl:choose>
			<xsl:when test="$liftDate">
				<doc:element name='snrd'>
					<doc:element name='rights'>
						<doc:element name='accessRights'>
							<doc:element name='es'>
								<doc:field name="value">info:eu-repo/semantics/embargoedAccess</doc:field>
							</doc:element>
						</doc:element>
						<doc:element name='embargoEndDate'>
							<doc:element name='es'>
								<doc:field name="value"><xsl:value-of select="$liftDate" /></doc:field>
							</doc:element>
						</doc:element>
					</doc:element>
				</doc:element>
			</xsl:when>
			<xsl:otherwise>
				<doc:element name='snrd'>
					<doc:element name='rights'>
						<doc:element name='accessRights'>
							<doc:element name='es'>
								<doc:field name="value">info:eu-repo/semantics/openAccess</doc:field>
							</doc:element>
						</doc:element>
					</doc:element>
				</doc:element>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- Removing other dc.date.* -->
	<xsl:template match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name!='issued']" />
	
 	<!-- Formatting dc.date.issued -->
	<xsl:template match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field/text()">
		<xsl:call-template name="formatdate">
			<xsl:with-param name="datestr" select="." />
		</xsl:call-template>
	</xsl:template>
	
	<!-- Silencio el dc.type  -->
	<xsl:template match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element"/>
	
	<!-- Genero el nuevo type en base a nuestro subtype -->
	<xsl:template name="driver-type" >
		<xsl:param name="subtype" />
		
		<xsl:variable name="snrd_type_driver">
		<xsl:choose>
			<xsl:when test="$subtype='Documento de trabajo'">
				info:eu-repo/semantics/workingPaper
			</xsl:when>
			<xsl:when test="$subtype='Preprint'">
				info:eu-repo/semantics/preprint
			</xsl:when>
			<xsl:when test="$subtype='Articulo'">
				info:eu-repo/semantics/article
			</xsl:when>
			<xsl:when test="$subtype='Comunicacion'">
				info:eu-repo/semantics/article
			</xsl:when>
			<xsl:when test="$subtype='Revision'">
				info:eu-repo/semantics/review
			</xsl:when>
			<xsl:when test="$subtype='Contribucion a revista'">
				info:eu-repo/semantics/contributionToPeriodical
			</xsl:when>
			<xsl:when test="$subtype='Informe tecnico'">
				info:eu-repo/semantics/report
			</xsl:when>
			<xsl:when test="$subtype='patente'">
				info:eu-repo/semantics/patent
			</xsl:when>
			<xsl:when test="$subtype='Tesis de doctorado'">
				info:eu-repo/semantics/doctoralThesis
			</xsl:when>
			<xsl:when test="$subtype='Tesis de maestria'">
				info:eu-repo/semantics/masterThesis
			</xsl:when>
			<xsl:when test="$subtype='Trabajo de especializacion'">
				info:eu-repo/semantics/masterThesis
			</xsl:when>
			<xsl:when test="$subtype='Tesis de grado'">
				info:eu-repo/semantics/bachelorThesis
			</xsl:when>
			<xsl:when test="$subtype='Trabajo final de grado'">
				info:eu-repo/semantics/bachelorThesis
			</xsl:when>
			<xsl:when test="$subtype='Libro'">
				info:eu-repo/semantics/book
			</xsl:when>
			<xsl:when test="$subtype='Capitulo de libro'">
				info:eu-repo/semantics/bookPart
			</xsl:when>
			<xsl:when test="$subtype='Objeto de conferencia'">
				info:eu-repo/semantics/conferenceObject
			</xsl:when>
			<xsl:when test="$subtype='Documento institucional'">
				info:eu-repo/semantics/annotation
			</xsl:when>
			<xsl:when test="$subtype='Resolucion'">
				info:eu-repo/semantics/annotation
			</xsl:when>
			<xsl:when test="$subtype='Imagen fija'">
				info:eu-repo/semantics/other
			</xsl:when> 
 			<xsl:when test="$subtype='Imagen en movimiento'">
				info:eu-repo/semantics/other
			</xsl:when>
 			<xsl:when test="$subtype='Audio'">
				info:eu-repo/semantics/other
			</xsl:when>
			<xsl:otherwise>
				info:eu-repo/semantics/other
			</xsl:otherwise>
		</xsl:choose>
		</xsl:variable>
		<doc:element name='snrd'>
			<doc:element name='type'>
				<doc:element name='driver'>
					<doc:element name='es'>
						<doc:field name="value"><xsl:value-of select="normalize-space($snrd_type_driver)"/></doc:field>
					</doc:element>
				</doc:element>
			</doc:element>
		</doc:element>
		
	</xsl:template>
	
	<!-- Genero el nuevo type en base a nuestro subtype -->
	<xsl:template name="driver-version" >
		<xsl:param name="subtype" />
		
		<xsl:variable name="snrd_type_version">
		<xsl:choose>
			<xsl:when test="$subtype='Documento de trabajo'">
				info:eu-repo/semantics/submittedVersion
			</xsl:when>
			<xsl:when test="$subtype='Preprint'">
				info:eu-repo/semantics/acceptedVersion
			</xsl:when>
			<xsl:when test="$subtype='Articulo'">
				info:eu-repo/semantics/publishedVersion
			</xsl:when>
			<xsl:when test="$subtype='Comunicacion'">
				info:eu-repo/semantics/publishedVersion
			</xsl:when>
			<xsl:when test="$subtype='Revision'">
				info:eu-repo/semantics/publishedVersion
			</xsl:when>
			<xsl:when test="$subtype='Contribucion a revista'">
				info:eu-repo/semantics/publishedVersion
			</xsl:when>
			<xsl:when test="$subtype='Informe tecnico'">
				info:eu-repo/semantics/publishedVersion
			</xsl:when>
			<xsl:when test="$subtype='patente'">
				info:eu-repo/semantics/publishedVersion
			</xsl:when>
			<xsl:when test="$subtype='Tesis de doctorado'">
				info:eu-repo/semantics/acceptedVersion
			</xsl:when>
			<xsl:when test="$subtype='Tesis de maestria'">
				info:eu-repo/semantics/acceptedVersion
			</xsl:when>
			<xsl:when test="$subtype='Trabajo de especializacion'">
				info:eu-repo/semantics/acceptedVersion
			</xsl:when>
			<xsl:when test="$subtype='Tesis de grado'">
				info:eu-repo/semantics/acceptedVersion
			</xsl:when>
			<xsl:when test="$subtype='Trabajo final de grado'">
				info:eu-repo/semantics/acceptedVersion
			</xsl:when>
			<xsl:when test="$subtype='Libro'">
				info:eu-repo/semantics/publishedVersion
			</xsl:when>
			<xsl:when test="$subtype='Capitulo de libro'">
				info:eu-repo/semantics/publishedVersion
			</xsl:when>
			<xsl:when test="$subtype='Objeto de conferencia'">
				info:eu-repo/semantics/publishedVersion
			</xsl:when>
			<xsl:when test="$subtype='Documento institucional'">
				info:eu-repo/semantics/publishedVersion
			</xsl:when>
			<xsl:when test="$subtype='Resolucion'">
				info:eu-repo/semantics/publishedVersion
			</xsl:when>
			<xsl:when test="$subtype='Imagen fija'">
				info:eu-repo/semantics/publishedVersion
			</xsl:when> 
 			<xsl:when test="$subtype='Imagen en movimiento'">
				info:eu-repo/semantics/publishedVersion
			</xsl:when>
 			<xsl:when test="$subtype='Audio'">
				info:eu-repo/semantics/publishedVersion
			</xsl:when>
			<xsl:otherwise>
				info:eu-repo/semantics/publishedVersion
			</xsl:otherwise>
		</xsl:choose>
		</xsl:variable>
		<doc:element name='snrd'>
			<doc:element name='type'>
				<doc:element name='version'>
					<doc:element name='es'>
						<doc:field name="value"><xsl:value-of select="normalize-space($snrd_type_version)"/></doc:field>
					</doc:element>
				</doc:element>
			</doc:element>
		</doc:element>
		
	</xsl:template>
	
	<!-- Genero el nuevo type en base a nuestro subtype -->
	<xsl:template name="snrd-type" >
		<xsl:param name="subtype" />
		
		<xsl:variable name="snrd_type_snrd">
		<xsl:choose>
			<xsl:when test="$subtype='Documento de trabajo'">
				documento de trabajo
			</xsl:when>
			<xsl:when test="$subtype='Preprint'">
				artículo
			</xsl:when>
			<xsl:when test="$subtype='Articulo'">
				artículo
			</xsl:when>
			<xsl:when test="$subtype='Comunicacion'">
				artículo
			</xsl:when>
			<xsl:when test="$subtype='Revision'">
				reseña artículo
			</xsl:when>
			<xsl:when test="$subtype='Contribucion a revista'">
				artículo
			</xsl:when>
			<xsl:when test="$subtype='Informe tecnico'">
				informe técnico
			</xsl:when>
			<xsl:when test="$subtype='patente'">
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
			</xsl:when>
			<xsl:otherwise>
				otros
			</xsl:otherwise>
		</xsl:choose>
		</xsl:variable>

		<doc:element name='snrd'>
			<doc:element name='type'>
				<doc:element name='snrd'>
					<doc:element name='es'>
						<doc:field name="value"><xsl:value-of select="normalize-space($snrd_type_snrd)"/></doc:field>
					</doc:element>
				</doc:element>
			</doc:element>
		</doc:element>
		
	</xsl:template>
	
	<!-- AUXILIARY TEMPLATES -->
	
	
	<!-- Date format -->
	<xsl:template name="formatdate">
		<xsl:param name="datestr" />
		<xsl:variable name="sub">
			<xsl:value-of select="substring($datestr,1,10)" />
		</xsl:variable>
		<xsl:value-of select="$sub" />
	</xsl:template>
</xsl:stylesheet>
