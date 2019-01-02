<?xml version="1.0" encoding="UTF-8"?>
<!-- 
	Éste documento XSL contiene algunas transformaciones en común que son impuestas por 
	DRIVER Guideleness v.2, las cuales son implementadas tanto en el contexto "driver","snrd" y "openaire".
	
	Para información, info-eu-repo: https://wiki.surfnet.nl/display/standards/info-eu-repo#info-eu-repo-AccessRights
 -->
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:doc="http://www.lyncode.com/xoai" xmlns:dc="http://purl.org/dc/doc:elements/1.1/" >
	<xsl:output indent="yes" method="xml" omit-xml-declaration="yes" />

<!-- Cada referencia al contexto se mantiene en los parámetros {context-name}. -->

<!-- dc:identifier -->
<!-- Remuevo sedici.identifier.other, no se exporta -->
	<xsl:template match="/doc:metadata/doc:element[@name='sedici']/doc:element[@name='identifier']/doc:element[@name='other']" />
<!--  Genera el campo {$context-name}.identifier.handle -->
	<xsl:template name="sedici-identifier" >
		<xsl:param name="handle" />
		<xsl:param name="context-name"/>
		
		<doc:element name='{$context-name}'>
			<doc:element name='identifier'>
				<doc:element name='handle'>
					<doc:element>
						<doc:field name="value">http://sedici.unlp.edu.ar/handle/<xsl:value-of select="$handle"/></doc:field>
					</doc:element>
				</doc:element>
			</doc:element>
		</doc:element>		
	</xsl:template>
	
<!-- dc:rights - info-eu-repo AccessRights compliant-->
<!-- Chequeo si es un doc embargado e imprimo la fecha de fin de embargo sedici.embargo.liftDate -->
	<xsl:template name="accessRightsAndEmbargo" >
		<xsl:param name="context-name"/>

		<!--sedici.rights.* = rights -->
		<xsl:variable name="bitstreams" select="/doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle'][./doc:field/text()='ORIGINAL']/doc:element[@name='bitstreams']/doc:element[@name='bitstream']"/>
		<xsl:variable name="embargoed" select="$bitstreams/doc:field[@name='embargo']"/>

		<xsl:variable name="date-prefix">
			<xsl:if test="($context-name = 'driver') or ($context-name = 'openaire') or ($context-name = 'snrd')">
				<xsl:text>info:eu-repo/date/embargoEnd/</xsl:text>
			</xsl:if>
		</xsl:variable>

		<xsl:choose>
			<xsl:when test="count($bitstreams) = 0 or count($embargoed[text() = 'forever']) = count($bitstreams) ">
				<doc:element name='driver'>
					<doc:element name='rights'>
						<doc:element name='accessRights'>
							<doc:element name='es'>
								<doc:field name="value">info:eu-repo/semantics/closedAccess</doc:field>
							</doc:element>
						</doc:element>
					</doc:element>
				</doc:element>
			</xsl:when>
			<xsl:when test="count($embargoed) = 0">
				<doc:element name='driver'>
					<doc:element name='rights'>
						<doc:element name='accessRights'>
							<doc:element name='es'>
								<doc:field name="value">info:eu-repo/semantics/openAccess</doc:field>
							</doc:element>
						</doc:element>
					</doc:element>
				</doc:element>
			</xsl:when>
			<xsl:when test="count($embargoed[text() = 'forever']) &gt; 0">
				<doc:element name='driver'>
					<doc:element name='rights'>
						<doc:element name='accessRights'>
							<doc:element name='es'>
								<doc:field name="value">info:eu-repo/semantics/restrictedAccess</doc:field>
							</doc:element>
						</doc:element>
					</doc:element>
				</doc:element>
			</xsl:when>
			<xsl:otherwise>
<!-- 			es un embargoedAccess si o si -->
			<xsl:for-each select="$embargoed">
				<xsl:sort select="text()" />
				<xsl:if test="position() = 1">
					<xsl:variable name="liftDate">
						<xsl:value-of select="text()" />
					</xsl:variable>
					<doc:element name='driver'>
						<doc:element name='rights'>
							<doc:element name='accessRights'>
								<doc:element name='es'>
									<doc:field name="value">info:eu-repo/semantics/embargoedAccess</doc:field>
								</doc:element>
							</doc:element>
						</doc:element>
					</doc:element>
					<doc:element name='{$context-name}'>
						<doc:element name='rights'>
							<doc:element name='embargoEndDate'>
								<doc:element name='es'>
									<doc:field name="value">
										<xsl:call-template name="formatdate">
											<xsl:with-param name="datestr">
												<xsl:value-of select="$liftDate" />
											</xsl:with-param>
											<xsl:with-param name="prefix" select="$date-prefix"/>
										</xsl:call-template>
									</doc:field>
								</doc:element>
							</doc:element>
						</doc:element>
					</doc:element>
					</xsl:if>
				</xsl:for-each>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
<!-- dc:date - ISO 8601 compliant-->
<!-- Removing other dc.date.* - Para reducir ambigüedad, se dejan sólo las fechas de publicación y de exposición (sólo Tesis)-->
	<xsl:template match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name!='issued']" />
	
 	<!-- Formatting sedici.date.exposure -->
	<xsl:template match="/doc:metadata/doc:element[@name='sedici']/doc:element[@name='date']/doc:element[@name='exposure']/doc:element/doc:field/text()">
		<xsl:call-template name="formatdate">
			<xsl:with-param name="datestr" select="." />
		</xsl:call-template>
	</xsl:template>
 	
 	<!-- Formatting dc.date.issued -->
 	<!-- Sólo procesamos y mostramos el dc.date.issued si es que NO EXISTE el sedici.date.exposure. -->
	<xsl:template match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']">
		<xsl:if test="not(../../../doc:element[@name='sedici']/doc:element[@name='date']/doc:element[@name='exposure'])">
			<doc:element name="issued">	
				<doc:element name="es">		
					<doc:field name="value">
						<xsl:call-template name="formatdate">
							<xsl:with-param name="datestr" select="doc:element/doc:field/text()	" />
						</xsl:call-template>
					</doc:field>
				</doc:element>
			</doc:element>
		</xsl:if>
	</xsl:template>

<!-- dc:type - info-eu-repo Publication types - Version type compliant -->
<!-- Silencio el dc.type  -->
	<xsl:template match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element"/>
<!-- Genero el nuevo type en base a nuestro subtype -->
	<xsl:template name="driver-type" >
		<xsl:param name="subtype" />
		
		<xsl:variable name="driver_type_driver">
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
			<xsl:when test="$subtype='Resumen'">
				info:eu-repo/semantics/conferenceObject
			</xsl:when>
			<xsl:when test="$subtype='Revision'">
				info:eu-repo/semantics/review
			</xsl:when>
			<xsl:when test="$subtype='Contribucion a revista'">
				info:eu-repo/semantics/contributionToPeriodical
			</xsl:when>
			<xsl:when test="$subtype='Reporte'">
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
			<xsl:when test="$subtype='Convenio'">
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
		<doc:element name='driver'>
			<doc:element name='type'>
				<doc:element name='es'>
						<doc:field name="value"><xsl:value-of select="normalize-space($driver_type_driver)"/></doc:field>
					</doc:element>
				</doc:element>
		</doc:element>
		
	</xsl:template>
<!-- Genero el nuevo type en base a nuestro subtype -->
	<xsl:template name="driver-version" >
		<xsl:param name="subtype" />
		
		<xsl:variable name="driver_type_version">
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
			<xsl:when test="$subtype='Convenio'">
				info:eu-repo/semantics/publishedVersion
			</xsl:when>
			<xsl:when test="$subtype='Revision'">
				info:eu-repo/semantics/publishedVersion
			</xsl:when>
			<xsl:when test="$subtype='Contribucion a revista'">
				info:eu-repo/semantics/publishedVersion
			</xsl:when>
			<xsl:when test="$subtype='Reporte'">
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
		<doc:element name='driver'>
			<doc:element name='type'>
				<doc:element name='version'>
					<doc:element name='es'>
						<doc:field name="value"><xsl:value-of select="normalize-space($driver_type_version)"/></doc:field>
					</doc:element>
				</doc:element>
			</doc:element>
		</doc:element>
	</xsl:template>

<!-- AUXILIARY TEMPLATES -->
	<!-- Date format -->
	<xsl:template name="formatdate">
		<xsl:param name="datestr" />
		<xsl:param name="prefix" />
		<xsl:variable name="sub">
			<xsl:value-of select="substring($datestr,1,10)" />
		</xsl:variable>
		<xsl:value-of select="concat($prefix,$sub)" />
	</xsl:template>


</xsl:stylesheet>