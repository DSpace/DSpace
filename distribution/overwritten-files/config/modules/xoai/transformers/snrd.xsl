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
	
	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" />
		</xsl:copy>
	</xsl:template>

	<!-- Remuevo sedici.identifier.other -->
	<xsl:template match="/doc:metadata/doc:element[@name='sedici']/doc:element[@name='identifier']/doc:element[@name='other']" />
	
	<!-- Removing other dc.date.* -->
	<xsl:template match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name!='issued']" />
	
	<!-- Chequeo si es un doc embargado e imprimo la fecha de fin de embargo sedici.embargo.liftDate -->
	<xsl:template match="/doc:metadata/doc:element[@name='sedici']/doc:element[@name='embargo']/doc:element[@name='liftDate']/doc:element/doc:field">
		<dc:rights><xsl:text>info:eu-repo/semantics/embargoedAccess</xsl:text></dc:rights>
		<dc:rights><xsl:value-of select="." /></dc:rights>
	</xsl:template>
			
 	<!-- Formatting dc.date.issued -->
	<xsl:template match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field/text()">
		<xsl:call-template name="formatdate">
			<xsl:with-param name="datestr" select="." />
		</xsl:call-template>
	</xsl:template>
	
	
	<!-- Silencio el dc.type  -->
	<xsl:template match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element"/>
	
	<!-- Genero el nuevo type en base a nuestro subtype -->
	<xsl:template match="/doc:metadata/doc:element[@name='sedici']/doc:element[@name='subtype']/doc:element/doc:field/text()" >
		<xsl:variable name="subtype" select="."/>
		
		<xsl:choose>
			<xsl:when test="$subtype='Documento de trabajo'">
				<dc:type>info:eu-repo/semantics/workingPaper</dc:type>
				<dc:type>documento de trabajo</dc:type>
			</xsl:when>
			<xsl:when test="$subtype='Preprint'">
				<dc:type>info:eu-repo/semantics/preprint</dc:type>
				<dc:type>artículo</dc:type>
			</xsl:when>
			<xsl:when test="$subtype='Articulo'">
				<dc:type>info:eu-repo/semantics/article</dc:type>
				<dc:type>artículo</dc:type>
			</xsl:when>
			<xsl:when test="$subtype='Comunicacion'">
				<dc:type>info:eu-repo/semantics/article</dc:type>
				<dc:type>artículo</dc:type>
			</xsl:when>
			<xsl:when test="$subtype='Revision'">
				<dc:type>info:eu-repo/semantics/review</dc:type>
				<dc:type>reseña artículo</dc:type>
			</xsl:when>
			<xsl:when test="$subtype='Contribucion a revista'">
				<dc:type>info:eu-repo/semantics/contributionToPeriodical</dc:type>
				<dc:type>artículo</dc:type>
			</xsl:when>
			<xsl:when test="$subtype='Informe tecnico'">
				<dc:type>info:eu-repo/semantics/report</dc:type>
				<dc:type>informe técnico</dc:type>
			</xsl:when>
			<xsl:when test="$subtype='patente'">
				<dc:type>info:eu-repo/semantics/patent</dc:type>
				<dc:type>patente</dc:type>
			</xsl:when>
			<xsl:when test="$subtype='Tesis de doctorado'">
				<dc:type>info:eu-repo/semantics/doctoralThesis</dc:type>
				<dc:type>tesis doctoral</dc:type>
			</xsl:when>
			<xsl:when test="$subtype='Tesis de maestria'">
				<dc:type>info:eu-repo/semantics/masterThesis</dc:type>
				<dc:type>tesis de maestría</dc:type>
			</xsl:when>
			<xsl:when test="$subtype='Trabajo de especializacion'">
				<dc:type>info:eu-repo/semantics/masterThesis</dc:type>
				<dc:type>otros</dc:type>
			</xsl:when>
			<xsl:when test="$subtype='Tesis de grado'">
				<dc:type>info:eu-repo/semantics/bachelorThesis</dc:type>
				<dc:type>tesis de grado</dc:type>
			</xsl:when>
			<xsl:when test="$subtype='Trabajo final de grado'">
				<dc:type>info:eu-repo/semantics/bachelorThesis</dc:type>
				<dc:type>trabajo final de grado</dc:type>
			</xsl:when>
			<xsl:when test="$subtype='Libro'">
				<dc:type>info:eu-repo/semantics/book</dc:type>
				<dc:type>libro</dc:type>
			</xsl:when>
			<xsl:when test="$subtype='Capitulo de libro'">
				<dc:type>info:eu-repo/semantics/bookPart</dc:type>
				<dc:type>parte de libro</dc:type>
			</xsl:when>
			<xsl:when test="$subtype='Objeto de conferencia'">
				<dc:type>info:eu-repo/semantics/conferenceObject</dc:type>
				<dc:type>documento de conferencia</dc:type>
			</xsl:when>
			<xsl:when test="$subtype='Documento institucional'">
				<dc:type>info:eu-repo/semantics/annotation</dc:type>
				<dc:type>otros</dc:type>
			</xsl:when>
			<xsl:when test="$subtype='Resolucion'">
				<dc:type>info:eu-repo/semantics/annotation</dc:type>
				<dc:type>otros</dc:type>
			</xsl:when>
			<xsl:when test="$subtype='Imagen fija'">
				<dc:type>info:eu-repo/semantics/other</dc:type>
				<dc:type>fotografía</dc:type>
 			</xsl:when> 
 			<xsl:when test="$subtype='Imagen en movimiento'">
				<dc:type>info:eu-repo/semantics/other</dc:type>
				<dc:type>videograbación</dc:type>
			</xsl:when>
 			<xsl:when test="$subtype='Audio'">
				<dc:type>info:eu-repo/semantics/other</dc:type>
				<dc:type>otros</dc:type>
			</xsl:when>
			<xsl:otherwise>
				<dc:type>info:eu-repo/semantics/other</dc:type>
				<dc:type>otros</dc:type>
			</xsl:otherwise>
		</xsl:choose>
			
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
