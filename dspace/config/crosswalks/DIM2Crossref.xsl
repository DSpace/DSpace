<?xml version="1.0" encoding="UTF-8"?>

<!-- Document : DIM2Crossref.xsl Description: Converts metadata from DSpace
	Intermediat Format (DIM) into metadata following the Crossref Schema -->
<xsl:stylesheet  version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:dspace="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:java="http://xml.apache.org/xalan/java" >

	<xsl:output method="xml" indent="yes" encoding="utf-8" />

	<xsl:template match="@* | text()" />

	<xsl:template match="/dspace:dim[@dspaceType='ITEM']">
		<!-- org.dspace.identifier.doi.CrossrefConnector uses this XSLT to transform
			metadata for the Crossref metadata store. -->

		<doi_batch version="4.4.2"
			xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			xmlns="http://www.crossref.org/schema/4.4.2"
			xsi:schemaLocation="http://www.crossref.org/schema/4.4.2 https://www.crossref.org/schemas/crossref4.4.2.xsd">

			<head>
				<doi_batch_id>
					<xsl:value-of select="java:java.time.LocalDateTime.now()" />
				</doi_batch_id>

				<timestamp>
					<xsl:value-of select="java:java.lang.System.currentTimeMillis()" />
				</timestamp>

				<depositor>
					<depositor_name>Universidad Nacional de La Plata</depositor_name>
					<email_address>admin@sedici.unlp.edu.ar</email_address>
				</depositor>

				<registrant>Universidad Nacional de La Plata</registrant>
			</head>

			<body>
				<xsl:variable name="type" select="//dspace:field[@mdschema='dc' and @element='type']/text()"/>
				<xsl:choose>
					<xsl:when test="$type='Tesis'">
						<xsl:call-template name="setDissertation" />
					</xsl:when>
					<xsl:when test="$type='Articulo'">
						<xsl:call-template name="setJournal" />
					</xsl:when>
					<xsl:when test="$type='Artículo'">
						<xsl:call-template name="setJournal" />
					</xsl:when>

					<!-- <xsl:when test="$type='Libro'">
						<xsl:call-template name="setBook" />
					</xsl:when> -->

					<!-- <xsl:when test="string(text())='Objeto de conferencia'">
						<xsl:call-template name="setObjetoDeConferencia" />
					</xsl:when> -->

					<!-- <xsl:when test="string(text())='Conjunto de datos'">
						<xsl:call-template name="setConjuntoDeDatos" />
					</xsl:when> -->

					<!-- No se mapean
					<xsl:when test="$subtype='Objeto Fisico'"></xsl:when>
					<xsl:when test="$subtype='Objeto de aprendizaje'"></xsl:when>
					<xsl:when test="$subtype='Imagen fija'"></xsl:when>
					<xsl:when test="$subtype='Documento institucional'"></xsl:when>
					<xsl:when test="$subtype='Audio'"></xsl:when>
					<xsl:when test="$subtype='Publicacion seriada'"></xsl:when>
					<xsl:when test="$subtype='Imagen en movimiento'"></xsl:when> -->
				</xsl:choose>
			</body>
		</doi_batch>

	</xsl:template>

	<xsl:template name="setDissertation">
		<dissertation publication_type="full_text"
			reference_distribution_opts="none"
			xmlns="http://www.crossref.org/schema/4.4.2" >
			<xsl:call-template name="setDocLanguageAttr" />
			<xsl:call-template name="setPublicationTypeAttr" />

			<!-- contributors -->
			<xsl:call-template name="setContributors" />

			<!-- titles -->
			<xsl:call-template name="setTitles" />

			<!-- jats:abstract -->
			<xsl:call-template name="setAbstract" />

			<!-- approval_date -->
			<xsl:call-template name="setApprovalDate" />

			<!-- institution -->
			<xsl:call-template name="setInstitution" />

			<!-- degree -->
			<degree>
				<xsl:value-of
					select="//dspace:field[@mdschema='thesis' and @element='degree' and @qualifier='name']" />
			</degree>

			<!-- isbn -->
			<xsl:call-template name="setISBN" />

			<!-- No se mapea, el indentifier ya lo mapeamos en doi_data
			<publisher_item></publisher_item>  -->

			<!-- <crossmark></crossmark> -->

			<!-- No se mapea, no tenemos información de fundRef
			<fr:program name="fundref" xmlns:fr="http://www.crossref.org/fundref.xsd">
			</fr:program> -->

			<!-- ai:program -->
			<xsl:call-template name="setAIProgram" />

			<!-- rel:program -->
			<xsl:call-template name="setRelationsProgram" />

			<!-- No se mapea porque no usamos ninguna red de preservación
			<archive_locations></archive_locations> -->

			<!-- No se mapea porque no utilizamos Scholarly Sharing Network (SCN)
				policies
			<scn_policies></scn_policies> -->

			<!-- doi_data -->
			<xsl:call-template name="setDOIData" />

			<!-- No se mapea, no tenemos esa información
			<citation_list></citation_list> -->

			<!-- No se mapea, no aplica a ningún metadato que tengamos
			<component_list></component_list> -->
		</dissertation>
	</xsl:template>

	<xsl:template name="setJournal">
		<journal xmlns="http://www.crossref.org/schema/4.4.2">

			<journal_metadata reference_distribution_opts="none">

				<!-- full_title -->
				<xsl:call-template name="setFullTitle" />

				<!-- issn -->
				<xsl:call-template name="setISSN" />

				<!-- No mapea, no tenemos metadatos de coden
				<coden></coden> -->

				<!-- No se mapea porque no usamos ninguna red de preservación
				<archive_locations></archive_locations> -->

				<!-- doi_data no aplica, no tenemos doi para journals -->
				<!-- <xsl:call-template name="setDOIData" /> -->
			</journal_metadata>

			<!-- No tenemos suficiente información de una issue de un journal
			<journal_issue>
			</journal_issue> -->

			<journal_article publication_type="full_text"
				reference_distribution_opts="none">
				<xsl:call-template name="setDocLanguageAttr" />
				<xsl:call-template name="setPublicationTypeAttr" />

				<!-- titles -->
				<xsl:call-template name="setTitles" />

				<!-- contributors -->
				<xsl:call-template name="setContributors" />

				<!-- jats:abstract -->
				<xsl:call-template name="setAbstract" />

				<!-- publication_date -->
				<xsl:call-template name="setPublicationDate" />

				<!-- pages -->
				<xsl:call-template name="setPages" />

				<!-- No se mapea, el indentifier ya lo mapeamos en doi_data
				<publisher_item></publisher_item>  -->

				<!-- <crossmark></crossmark> -->

				<!-- No se mapea, no tenemos información de fundRef
				<fr:program name="fundref" xmlns:fr="http://www.crossref.org/fundref.xsd">
				</fr:program> -->

				<!-- ai:program -->
				<xsl:call-template name="setAIProgram" />

				<!-- rel:program -->
				<xsl:call-template name="setRelationsProgram" />

				<!-- No se mapea porque no usamos ninguna red de preservación
				<archive_locations></archive_locations> -->

				<!-- No se mapea porque no utilizamos Scholarly Sharing Network (SCN)
					policies
				<scn_policies></scn_policies> -->

				<!-- doi_data -->
				<xsl:call-template name="setDOIData" />

				<!-- No se mapea, no tenemos esa información
				<citation_list></citation_list> -->

				<!-- No se mapea, no aplica a ningún metadato que tengamos
				<component_list></component_list> -->
			</journal_article>
		</journal>
	</xsl:template>

	<xsl:template name="setBook">
		<!-- TO DO -->
		<book book_type="other" xmlns="http://www.crossref.org/schema/4.4.2">

			<book_metadata reference_distribution_opts="none">
			</book_metadata>

			<book_series_metadata reference_distribution_opts="none">
			</book_series_metadata>

			<book_set_metadata reference_distribution_opts="none">
			</book_set_metadata>

			<xsl:if
					test="//dspace:field[@mdschema='dc' and @element='type']/text() = 'Capitulo de libro'">
				<content_item component_type="chapter"
					level_sequence_number="1" publication_type="full_text"
					reference_distribution_opts="none">
				</content_item>
			</xsl:if>
		</book>
	</xsl:template>

	<xsl:template name="setFullTitle">
		<full_title xmlns="http://www.crossref.org/schema/4.4.2">
			<xsl:value-of
			select="//dspace:field[@mdschema='sedici' and @element='relation' and @qualifier='journalTitle']" />
		</full_title>
	</xsl:template>

	<xsl:template name="setTitles">
		<titles xmlns="http://www.crossref.org/schema/4.4.2">
			<title>
				<xsl:value-of
					select="//dspace:field[@mdschema='dc' and @element='title' and not(@qualifier='alternative')]" />
			</title>
			<xsl:if
				test="//dspace:field[@mdschema='sedici' and @element='title' and not(@qualifier='subtitlo')]">
				<subtitle>
					<xsl:value-of
						select="//dspace:field[@mdschema='sedici' and @element='title' and not(@qualifier='subtitlo')]" />
				</subtitle>
			</xsl:if>
		</titles>
	</xsl:template>

	<xsl:template name="setContributors">
		<contributors xmlns="http://www.crossref.org/schema/4.4.2">

			<!-- <person_name role=author> -->
			<xsl:call-template name="setPersonName">
				<xsl:with-param name="person"
					select="//dspace:field[@mdschema='sedici' and @element='creator' and @qualifier='person']" />
				<xsl:with-param name="role">
					<xsl:text>author</xsl:text>
				</xsl:with-param>
				<xsl:with-param name="sequence">
					<xsl:text>first</xsl:text>
				</xsl:with-param>
			</xsl:call-template>

			<!-- <organization> -->
			<xsl:for-each
				select="//dspace:field[@mdschema='sedici' and @element='contributor' and @qualifier='corporate]']">
				<organization contributor_role="author"
					sequence="first">
					<xsl:value-of select="." />
				</organization>
			</xsl:for-each>

			<!-- <person_name role=editor> -->
			<xsl:for-each
				select="//dspace:field[@mdschema='sedici' and @element='contributor' and @qualifier='editor']" >
				<xsl:call-template name="setPersonName">
					<xsl:with-param name="person" select="." />
					<xsl:with-param name="role">
						<xsl:text>editor</xsl:text>
					</xsl:with-param>
					<xsl:with-param name="sequence">
						<xsl:text>first</xsl:text>
					</xsl:with-param>
				</xsl:call-template>
			</xsl:for-each>
		</contributors>
	</xsl:template>

	<xsl:template name="setPersonName">
		<xsl:param name="person" />
		<xsl:param name="role" />
		<xsl:param name="sequence" />
		<person_name xmlns="http://www.crossref.org/schema/4.4.2">
			<xsl:attribute name="contributor_role">
				<xsl:value-of select="$role" />
			</xsl:attribute>
			<xsl:attribute name="sequence">
				<xsl:value-of select="$sequence" />
			</xsl:attribute>
			<xsl:choose>
				<xsl:when test="contains($person,',')">
					<given_name>
						<xsl:value-of
							select="substring(substring-after($person,','),1,60)" />
					</given_name>
					<surname>
						<xsl:value-of
							select="substring(substring-before($person,','),1,60)" />
					</surname>
				</xsl:when>
				<xsl:otherwise>
					<surname>
						<xsl:value-of select="substring($person,1,60)" />
					</surname>
				</xsl:otherwise>

			</xsl:choose>
		</person_name>
	</xsl:template>

	<xsl:template name="setAbstract">
		<xsl:for-each
			select="//dspace:field[@mdschema='dc' and @element='description' and @qualifier='abstract']">
			<jats:abstract
				xmlns:jats="http://www.ncbi.nlm.nih.gov/JATS1">
				<xsl:attribute name="xml:lang">
					<xsl:value-of select="./@lang" />
				</xsl:attribute>
				<jats:p>
					<xsl:value-of select="." />
				</jats:p>
			</jats:abstract>
		</xsl:for-each>
	</xsl:template>

	<xsl:template name="setPublicationDate">
		<xsl:for-each
			select="//dspace:field[@mdschema='dc' and @element='date' and @qualifier='issued']">
			<publication_date xmlns="http://www.crossref.org/schema/4.4.2">
				<xsl:if test="string-length(./text()) &gt; 5">
					<month>
						<xsl:value-of select="substring(./text(),6,2)" />
					</month>
				</xsl:if>
				<xsl:if test="string-length(./text()) &gt; 8">
					<day>
						<xsl:value-of select="substring(./text(),9)" />
					</day>
				</xsl:if>
				<year>
					<xsl:value-of select="substring(./text(),1,4)" />
				</year>
			</publication_date>
		</xsl:for-each>
	</xsl:template>

	<xsl:template name="setApprovalDate">
		<xsl:for-each
			select="//dspace:field[@mdschema='sedici' and @element='date' and @qualifier='exposure']">
			<approval_date xmlns="http://www.crossref.org/schema/4.4.2">
				<xsl:if test="string-length(./text()) &gt; 5">
					<month>
						<xsl:value-of select="substring(./text(),6,2)" />
					</month>
				</xsl:if>
				<xsl:if test="string-length(./text()) &gt; 8">
					<day>
						<xsl:value-of select="substring(./text(),9)" />
					</day>
				</xsl:if>
				<year>
					<xsl:value-of select="substring(./text(),1,4)" />
				</year>
			</approval_date>
		</xsl:for-each>
	</xsl:template>

	<xsl:template name="setInstitution">
		<xsl:if
			test="//dspace:field[@mdschema='thesis' and @element='degree' and @qualifier='grantor']">
			<institution xmlns="http://www.crossref.org/schema/4.4.2">
				<institution_name>
					<xsl:value-of
						select="//dspace:field[@mdschema='thesis' and @element='degree' and @qualifier='grantor']" />
				</institution_name>
			</institution>
		</xsl:if>
	</xsl:template>

	<xsl:template name="setISBN">
		<xsl:for-each
			select="//dspace:field[@mdschema='sedici' and @element='identifier' and @qualifier='isbn']">
			<xsl:if test="string-length(.) &gt; 9">
				<isbn xmlns="http://www.crossref.org/schema/4.4.2">
					<xsl:value-of select="substring(.,1,17)" />
				</isbn>
			</xsl:if>
		</xsl:for-each>
	</xsl:template>

	<xsl:template name="setAIProgram">
		<xsl:if
			test="//dspace:field[@mdschema='sedici' and @element='rights' and @qualifier='uri']">
			<ai:program
				xmlns:ai="http://www.crossref.org/AccessIndicators.xsd"
				name="AccessIndicators">
				<ai:license_ref>
					<xsl:value-of
						select="//dspace:field[@mdschema='sedici' and @element='rights' and @qualifier='uri']" />
				</ai:license_ref>
			</ai:program>
		</xsl:if>
	</xsl:template>

	<xsl:template name="setRelationsProgram">
		<rel:program
			xmlns:rel="http://www.crossref.org/relations.xsd" name="relations">

			<!-- sedici.relation.journalVolumeAndIssue -->
			<xsl:if
				test="//dspace:field[@mdschema='sedici' and @element='relation' and @qualifier='journalVolumeAndIssue']">
				<rel:related_item>
					<rel:description>Journal issue or volume which the item is part of</rel:description>
					<rel:inter_work_relation
						identifier-type="other" relationship-type="isPartOf">
						<xsl:value-of
							select="//dspace:field[@mdschema='sedici' and @element='relation' and @qualifier='journalVolumeAndIssue']" />
					</rel:inter_work_relation>
				</rel:related_item>
			</xsl:if>

			<!-- sedici.relation.event -->
			<xsl:if
				test="//dspace:field[@mdschema='sedici' and @element='relation' and @qualifier='event']">
				<rel:related_item>
					<rel:description>Event name the item is part of</rel:description>
					<rel:inter_work_relation
						identifier-type="other" relationship-type="isPartOf">
						<xsl:value-of
							select="//dspace:field[@mdschema='sedici' and @element='relation' and @qualifier='event']" />
					</rel:inter_work_relation>
				</rel:related_item>
			</xsl:if>

			<!-- sedici.relation.isRelatedWith -->
			<xsl:if
				test="//dspace:field[@mdschema='sedici' and @element='relation' and @qualifier='isRelatedWith']">
				<rel:related_item>
					<rel:inter_work_relation
						identifier-type="uri" relationship-type="isRelatedMaterial">
						<xsl:value-of
							select="//dspace:field[@mdschema='sedici' and @element='relation' and @qualifier='isRelatedWith']" />
					</rel:inter_work_relation>
				</rel:related_item>
			</xsl:if>

			<!-- sedici.relation.ciclo -->
			<xsl:if
				test="//dspace:field[@mdschema='sedici' and @element='relation' and @qualifier='ciclo']">
				<rel:related_item>
					<rel:description>Program name which the item is part of</rel:description>
					<rel:inter_work_relation
						identifier-type="other" relationship-type="isPartOf">
						<xsl:value-of
							select="//dspace:field[@mdschema='sedici' and @element='relation' and @qualifier='ciclo']" />
					</rel:inter_work_relation>
				</rel:related_item>
			</xsl:if>

			<!-- sedici.relation.isPartOfSeries -->
			<xsl:if
				test="//dspace:field[@mdschema='sedici' and @element='relation' and @qualifier='isPartOfSeries']">
				<rel:related_item>
					<rel:description>Series which the item is part of</rel:description>
					<rel:inter_work_relation
						identifier-type="other" relationship-type="isPartOf">
						<xsl:value-of
							select="//dspace:field[@mdschema='sedici' and @element='relation' and @qualifier='isPartOfSeries']" />
					</rel:inter_work_relation>
				</rel:related_item>
			</xsl:if>

			<!-- sedici.relation.bookTitle -->
			<xsl:if
				test="//dspace:field[@mdschema='sedici' and @element='relation' and @qualifier='bookTitle']">
				<rel:related_item>
					<rel:description>Book title which the item is part of</rel:description>
					<rel:inter_work_relation
						identifier-type="other" relationship-type="isPartOf">
						<xsl:value-of
							select="//dspace:field[@mdschema='sedici' and @element='relation' and @qualifier='bookTitle']" />
					</rel:inter_work_relation>
				</rel:related_item>
			</xsl:if>

			<!-- sedici.relation.isReviewOf -->
			<xsl:if
				test="//dspace:field[@mdschema='sedici' and @element='relation' and @qualifier='isReviewOf']">
				<rel:related_item>
					<rel:inter_work_relation
						identifier-type="uri" relationship-type="isReviewOf">
						<xsl:value-of
							select="//dspace:field[@mdschema='sedici' and @element='relation' and @qualifier='isReviewOf']" />
					</rel:inter_work_relation>
				</rel:related_item>
			</xsl:if>

			<!-- sedici.relation.isReviewedBy -->
			<xsl:if
				test="//dspace:field[@mdschema='sedici' and @element='relation' and @qualifier='isReviewedBy']">
				<rel:related_item>
					<rel:inter_work_relation
						identifier-type="uri" relationship-type="hasReview">
						<xsl:value-of
							select="//dspace:field[@mdschema='sedici' and @element='relation' and @qualifier='isReviewedBy']" />
					</rel:inter_work_relation>
				</rel:related_item>
			</xsl:if>
		</rel:program>
	</xsl:template>

	<xsl:template name="setISSN">
		<xsl:if
			test="string-length(//dspace:field[@mdschema='sedici' and @element='identifier' and @qualifier='issn']) &gt; 7">
			<issn xmlns="http://www.crossref.org/schema/4.4.2">
				<xsl:value-of
					select="substring(//dspace:field[@mdschema='sedici' and @element='identifier' and @qualifier='issn'],1,9)" />
			</issn>
		</xsl:if>
	</xsl:template>

	<xsl:template name="setPages">
		<xsl:for-each
			select="//dspace:field[@mdschema='dc' and @element='format' and @qualifier='extent']">
			<xsl:if
				test="contains(./text(),'p.') or contains(./text(),'P.')">
				<xsl:variable name="pages"
					select="translate(./text(),'Pp. ','')" />
				<xsl:choose>
					<xsl:when test="contains($pages,'+')">
						<xsl:variable name="parsedPages"
							select="substring-before($pages,'+')" />
						<xsl:if test="contains($parsedPages,'-')">
							<pages xmlns="http://www.crossref.org/schema/4.4.2">
								<first_page>
									<xsl:value-of
										select="substring-before($parsedPages,'-')" />
								</first_page>
								<last_page>
									<xsl:value-of
										select="substring-after($parsedPages,'-')" />
								</last_page>
							</pages>
						</xsl:if>
					</xsl:when>
					<xsl:otherwise>
						<xsl:if test="contains($pages,'-')">
							<pages xmlns="http://www.crossref.org/schema/4.4.2">
								<first_page>
									<xsl:value-of
										select="substring-before($pages,'-')" />
								</first_page>
								<last_page>
									<xsl:value-of select="substring-after($pages,'-')" />
								</last_page>
							</pages>
						</xsl:if>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:if>
		</xsl:for-each>
	</xsl:template>

	<xsl:template name="setDOIData">

		<doi_data xmlns="http://www.crossref.org/schema/4.4.2">

			<doi>
				<!-- Solo seteo el doi si ya existe alguno en sedici.identifier.doi, sino se setea uno nuevo despúes, por afuera del xsl -->
				<xsl:if
				test="//dspace:field[@mdschema='sedici' and @element='identifier' and @qualifier='doi' and contains(., '10.')]">
					<xsl:variable name="doi"
					select="//dspace:field[@mdschema='sedici' and @element='identifier' and @qualifier='doi']" />
					<xsl:variable name="doiStartIndex"
					select="string-length(substring-before($doi,'10.'))+1" />
					<xsl:value-of select="substring($doi,$doiStartIndex)" />
				</xsl:if>
			</doi>

			<timestamp>
				<xsl:value-of
					select="java:java.lang.System.currentTimeMillis()" />
			</timestamp>

			<!-- dc.identifier.uri -->
			<resource>
				<xsl:value-of
					select="//dspace:field[@mdschema='dc' and @element='identifier' and @qualifier='uri']" />
			</resource>

			<!-- No mapeamos un doi a varios recursos
			<collection multi-resolution="" property=""></collection> -->
		</doi_data>

	</xsl:template>

	<xsl:template name="setDocLanguageAttr">
		<xsl:if
			test="//dspace:field[@mdschema='dc' and @element='language']">
			<xsl:attribute name="language">
				<xsl:value-of
					select="//dspace:field[@mdschema='dc' and @element='language']" />
			</xsl:attribute>
		</xsl:if>
	</xsl:template>

	<xsl:template name="setPublicationTypeAttr">
		<xsl:if
			test="//dspace:field[@mdschema='sedici' and @element='description' and @qualifier='fulltext']">
				<xsl:attribute name="publication_type">
					<xsl:choose>
						<xsl:when test="//dspace:field[@mdschema='sedici' and @element='description' and @qualifier='fulltext'] = 'true'">
							<xsl:text>full_text</xsl:text>
						</xsl:when>
						<xsl:otherwise>
							<xsl:text>bibliographic_record</xsl:text>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:attribute>
		</xsl:if>
	</xsl:template>

</xsl:stylesheet>
