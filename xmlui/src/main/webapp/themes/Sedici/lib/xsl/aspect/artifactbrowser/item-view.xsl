<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<!--
    Rendering specific to the item display page.

    Author: art.lowel at atmire.com
    Author: lieven.droogmans at atmire.com
    Author: ben at atmire.com
    Author: Alexey Maslov

-->

<xsl:stylesheet
    xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
    xmlns:dri="http://di.tamu.edu/DRI/1.0/"
    xmlns:mets="http://www.loc.gov/METS/"
    xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
    xmlns:xlink="http://www.w3.org/TR/xlink/"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:atom="http://www.w3.org/2005/Atom"
    xmlns:ore="http://www.openarchives.org/ore/terms/"
    xmlns:oreatom="http://www.openarchives.org/ore/atom/"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:xalan="http://xml.apache.org/xalan"
    xmlns:java="http://xml.apache.org/xalan/java"
    xmlns:encoder="xalan://java.net.URLEncoder"
    xmlns:util="org.dspace.app.xmlui.utils.XSLUtils"
    xmlns:fn="http://www.w3.org/2005/xpath-functions"
    exclude-result-prefixes="xalan java encoder i18n dri mets dim xlink xsl util">

    <xsl:output indent="yes" method="xhtml" />
    
    <xsl:variable name="linkFilter"><xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'] "/>/discover</xsl:variable>
    
    <xsl:template name="itemSummaryView-DIM">
        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim" mode="itemSummaryView-DIM"/>

		<xsl:if test="$ds_item_view_toggle_url != ''">
			<div id="view-item-metadata">
				<a>
					<xsl:attribute name="href"><xsl:value-of select="$ds_item_view_toggle_url" /></xsl:attribute>
					<i18n:text>xmlui.ArtifactBrowser.ItemViewer.show_full</i18n:text>
				</a>
			</div>
		</xsl:if>
		
        <!-- Generate the Creative Commons license information from the file section (DSpace deposit license hidden by default)-->
        <!-- <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CC-LICENSE'] "/> -->

    </xsl:template>

<!-- Aca va la muestra sencilla de un item -->
    <xsl:template match="dim:dim" mode="itemSummaryView-DIM">
        <div class="item-summary-view-metadata">
            <xsl:call-template name="itemSummaryView-DIM-fields"/>
        </div>
    </xsl:template>

	<xsl:template name="itemSummaryView-DIM-fields">

		<!-- Title row -->
		<xsl:choose>
			<xsl:when test="count(dim:field[@element='title'][not(@qualifier)]) &gt; 0">
				<!-- display first title as h1 -->
				<h1>
					<xsl:value-of select="dim:field[@element='title'][not(@qualifier)][1]/node()" disable-output-escaping="yes"/>
				</h1>
				<xsl:if test="dim:field[@element='title'][not(@qualifier)][2]">
					<div class="simple-item-view-title">
						<xsl:for-each select="dim:field[@element='title'][not(@qualifier)][position() &gt; 1]">
							<span class="metadata-value">
								<xsl:value-of select="./node()" disable-output-escaping="yes"/>
								<xsl:if test="count(following-sibling::dim:field[@element='title'][not(@qualifier)]) != 0">
									<xsl:text>; </xsl:text>
								</xsl:if>
							</span>
						</xsl:for-each>
					</div>
				</xsl:if>
			</xsl:when>
			<xsl:otherwise>
				<h1>
					<i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
				</h1>
			</xsl:otherwise>
		</xsl:choose>
		
		<!-- title.subtitle row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'title-subtitle'"/>
			<xsl:with-param name="elements" select="dim:field[@element='title' and @qualifier='subtitle'] "/>
		</xsl:call-template>
		
		<!-- book-title row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'relation-book-title'"/>
			<xsl:with-param name="elements" select="dim:field[@element='relation' and @qualifier='bookTitle'] "/>
		</xsl:call-template>

		<!-- relation.isPartOfSeries row -->
		
        <xsl:call-template name="render-normal-field">
    		<xsl:with-param name="name" select="'relation-isPartOfSeries'"/>
    		<xsl:with-param name="elements"    select="dim:field[@element='relation' and @qualifier='isPartOfSeries'] "/>
		</xsl:call-template>
		<!-- Author(s) row -->
		<div class="simple-item-view-authors">
			<xsl:choose>
				<xsl:when test="dim:field[@element='creator']">
					<span class="metadata-label">
						<xsl:choose>
							<xsl:when test="count(dim:field[@element='creator']) &gt; 1">
								<i18n:text>xmlui.dri2xhtml.METS-1.0.item-authors</i18n:text>:
							</xsl:when>
							<xsl:otherwise>
								<i18n:text>xmlui.dri2xhtml.METS-1.0.item-author</i18n:text>:
							</xsl:otherwise>
						</xsl:choose>
					</span>
					<xsl:for-each select="dim:field[@element='creator']">
						<span class="metadata-value">
							<xsl:if test="@authority">
								<xsl:attribute name="class"><xsl:text>ds-dc_contributor_author-authority</xsl:text></xsl:attribute>
							</xsl:if>	
								
							<a>
								<xsl:attribute name="href"><xsl:value-of select="$linkFilter"/>?filtertype=author&amp;filter="<xsl:copy-of select="translate(node(),'áéíóú','aeiou')"/>"</xsl:attribute>
								<xsl:copy-of select="node()" />
							</a>							
	
							<xsl:if test="count(following-sibling::dim:field[@element='creator']) != 0">
								<xsl:text> | </xsl:text>
							</xsl:if>
						</span>
					</xsl:for-each>
				</xsl:when>
				<xsl:when test="dim:field[@element='contributor' and @qualifier='compiler']">
					<span class="metadata-label">
						<xsl:choose>
							<xsl:when test="count(dim:field[@element='contributor' and @qualifier='compiler']) &gt; 1">
								<i18n:text>xmlui.dri2xhtml.METS-1.0.item-contributor-compilers</i18n:text>:
							</xsl:when>
							<xsl:otherwise>
								<i18n:text>xmlui.dri2xhtml.METS-1.0.item-contributor-compiler</i18n:text>:
							</xsl:otherwise>
						</xsl:choose>
					</span>
					<xsl:for-each select="dim:field[@element='contributor' and @qualifier='compiler']">
						<span class="metadata-value">
							<xsl:if test="@authority">
								<xsl:attribute name="class"><xsl:text>ds-dc_contributor_author-authority</xsl:text></xsl:attribute>
							</xsl:if>
	
							<a>
								<xsl:attribute name="href"><xsl:value-of select="$linkFilter"/>?filtertype=persona&amp;filter="<xsl:copy-of select="translate(node(),'áéíóú','aeiou')"/>"</xsl:attribute>
								<xsl:copy-of select="node()" />
							</a>							
	
							<xsl:if test="count(following-sibling::dim:field[@element='contributor' and @qualifier='compiler']) != 0">
								<xsl:text> | </xsl:text>
							</xsl:if>
						</span>
					</xsl:for-each>
				</xsl:when>
				<xsl:otherwise>
					<i18n:text>xmlui.dri2xhtml.METS-1.0.no-author</i18n:text>
				</xsl:otherwise>
				
			</xsl:choose>
		</div>

		<!-- embargo rows -->
		<xsl:if test="(dim:field[@element='embargo' and @qualifier='liftDate'])">
			<div id="embargo-info">
				<span class="embargo_msg"><i18n:text>xmlui.dri2xhtml.METS-1.0.embargoed-document-description</i18n:text></span>
				<span class="embargo_date">
					<xsl:call-template name="render-date">
						<xsl:with-param name="dateString" select="dim:field[@element='embargo' and @qualifier='liftDate'] "/>
					</xsl:call-template>
				</span>
			</div>
		</xsl:if>

		<!-- date.issued row -->
		<xsl:if test="dim:field[@element='date' and @qualifier='issued'] != substring-before(dim:field[@element='date' and @qualifier='accessioned'], 'T')">
			<xsl:call-template name="render-normal-field">
				<xsl:with-param name="name" select="'date-issued'"/>
				<xsl:with-param name="elements" select="dim:field[@element='date' and @qualifier='issued'] "/>
				<xsl:with-param name="type" select="'date'"/>
			</xsl:call-template>
        </xsl:if>
        
		<!-- Para el Tipo de Documento mostramos el sedici.subtype porque es mas especifico -->
		<!-- Si no hay subtype, mostramos el dc.type -->
		<xsl:choose>
			<xsl:when test="dim:field[@element='subtype']">
				<xsl:call-template name="render-normal-field">
					<xsl:with-param name="name" select="'subtype'"/>
					<xsl:with-param name="elements" select="dim:field[@element='subtype'] "/>
					<xsl:with-param name="filter">subtipo</xsl:with-param>
				</xsl:call-template>
			</xsl:when>
			<xsl:when test="dim:field[@element='type']">
				<xsl:call-template name="render-normal-field">
					<xsl:with-param name="name" select="'subtype'"/>
					<xsl:with-param name="elements" select="dim:field[@element='type'] "/>
					<xsl:with-param name="filter">subtipo</xsl:with-param>
				</xsl:call-template>
			</xsl:when>
			<!-- No hay otherwise -->
		</xsl:choose>

		<!-- title.alternative row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'title-alternative'"/>
			<xsl:with-param name="elements" select="dim:field[@element='title' and @qualifier='alternative'] "/>
		</xsl:call-template>

		<!-- Abstract row -->
		<xsl:if test="(dim:field[@element='description' and @qualifier='abstract'])">
			<div class="simple-item-view-description">
				<h2><i18n:text>xmlui.dri2xhtml.METS-1.0.item-abstract</i18n:text></h2>
				<div>
					<xsl:variable name="show_language_indicator">
						<xsl:choose>
							<xsl:when test="count(dim:field[@element='description' and @qualifier='abstract']) &gt; 1">1</xsl:when>
							<xsl:otherwise>0</xsl:otherwise>
						</xsl:choose>
					</xsl:variable>

					<xsl:for-each select="dim:field[@element='description' and @qualifier='abstract']">
						<!-- Indicador del idioma (solo si hay multiples abstracts) -->
						<xsl:if test="$show_language_indicator = 1">
							<span class="metadata-lang"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-lang-<xsl:value-of select="@language"/></i18n:text></span>
						</xsl:if>
						<p>
							<xsl:value-of select="node()" disable-output-escaping="yes"/>
						</p>
					</xsl:for-each>
				</div>
			</div>
		</xsl:if>
		<!-- note row -->
		<xsl:if test="(dim:field[@element='note'])">
			<div class="simple-item-view-description">
				<h2><i18n:text>xmlui.dri2xhtml.METS-1.0.item-note</i18n:text></h2>
				<p>
					<xsl:value-of select="dim:field[@element='note']" disable-output-escaping="yes"/>
				</p>
			</div>
		</xsl:if>

		<!-- Solo para el tipo tesis -->
		<xsl:if test="dim:field[@element='type'] = 'Tesis'">
			<h2><i18n:text>xmlui.dri2xhtml.METS-1.0.tesis-info</i18n:text></h2>
			<!-- contributor.director row -->
			<xsl:call-template name="render-normal-field">
				<xsl:with-param name="name" select="'contributor-director'"/>
				<xsl:with-param name="elements" select="dim:field[@element='contributor' and @qualifier='director'] "/>
				<xsl:with-param name="filter">persona</xsl:with-param>
			</xsl:call-template>

			<!-- contributor.codirector row -->
			<xsl:call-template name="render-normal-field">
				<xsl:with-param name="name" select="'contributor-codirector'"/>
				<xsl:with-param name="elements" select="dim:field[@element='contributor' and @qualifier='codirector'] "/>
				<xsl:with-param name="filter">persona</xsl:with-param>
			</xsl:call-template>

			<!-- date.exposure row -->
			<xsl:call-template name="render-normal-field">
				<xsl:with-param name="name" select="'date-exposure'"/>
				<xsl:with-param name="elements" select="dim:field[@element='date' and @qualifier='exposure'] "/>
				<xsl:with-param name="type" select="'date'"/>
			</xsl:call-template>

			<!-- affiliatedInstitution row -->
			<xsl:call-template name="render-normal-field">
				<xsl:with-param name="name" select="'institucion-desarrollo'"/>
				<xsl:with-param name="elements" select="dim:field[@element='institucionDesarrollo'] "/>
				<xsl:with-param name="filter">institucion</xsl:with-param>
			</xsl:call-template>

			<!-- degree.name row -->
			<xsl:call-template name="render-normal-field">
				<xsl:with-param name="name" select="'degree-name'"/>
				<xsl:with-param name="elements" select="dim:field[@element='degree' and @qualifier='name'] "/>
			</xsl:call-template>

			<!-- degree.grantor row -->
			<xsl:call-template name="render-normal-field">
				<xsl:with-param name="name" select="'degree-grantor'"/>
				<xsl:with-param name="elements" select="dim:field[@element='degree' and @qualifier='grantor'] "/>
			</xsl:call-template>
		</xsl:if>


		<h2><i18n:text>xmlui.dri2xhtml.METS-1.0.general-info</i18n:text></h2>
		<xsl:if test="not(dim:field[@element='contributor' and @qualifier='director'])">
			<!-- date.exposure row -->
			<xsl:call-template name="render-normal-field">
				<xsl:with-param name="name" select="'date-exposure'"/>
				<xsl:with-param name="elements" select="dim:field[@element='date' and @qualifier='exposure'] "/>
				<xsl:with-param name="type" select="'date'"/>
				<xsl:with-param name="filter">persona</xsl:with-param>
			</xsl:call-template>
		</xsl:if>

		<!-- identifier.expediente row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'identifier-expediente'"/>
			<xsl:with-param name="elements"	select="dim:field[@element='identifier' and @qualifier='expediente'] "/>
		</xsl:call-template>

		<!-- contributor.inscriber row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'contributor-inscriber'"/>
			<xsl:with-param name="elements" select="dim:field[@element='contributor' and @qualifier='inscriber'] "/>
			<xsl:with-param name="filter">persona</xsl:with-param>
		</xsl:call-template>

		<xsl:if	test="(dim:field[@element='contributor' and (@qualifier='editor' or @qualifier='translator' or @qualifier='compiler' or @qualifier='juror' or @qualifier='colaborator')])">

			<!-- contributor.editor row -->
				<xsl:call-template name="render-normal-field">
					<xsl:with-param name="name" select="'contributor-editor'" />
					<xsl:with-param name="elements" select="dim:field[@element='contributor' and @qualifier='editor']" />
					<xsl:with-param name="separator" select="' | '"/>
					<xsl:with-param name="filter">persona</xsl:with-param>
				</xsl:call-template>
	
				<!-- contributor.translator row -->
				<xsl:call-template name="render-normal-field">
					<xsl:with-param name="name" select="'contributor-translator'" />
					<xsl:with-param name="elements" select="dim:field[@element='contributor' and @qualifier='translator']" />
					<xsl:with-param name="separator" select="' | '"/>
					<xsl:with-param name="filter">persona</xsl:with-param>
				</xsl:call-template>

			<!-- contributor.compiler row -->
				<xsl:if test="dim:field[@element='creator']">
					<xsl:call-template name="render-normal-field">
						<xsl:with-param name="name" select="'contributor-compiler'" />
						<xsl:with-param name="elements" select="dim:field[@element='contributor' and @qualifier='compiler']" />
						<xsl:with-param name="separator" select="' | '"/>
						<xsl:with-param name="filter">persona</xsl:with-param>
					</xsl:call-template>
				</xsl:if>

			<!-- contributor.juror row -->
			<xsl:call-template name="render-normal-field">
				<xsl:with-param name="name" select="'contributor-juror'"/>
				<xsl:with-param name="elements" select="dim:field[@element='contributor' and @qualifier='juror'] "/>
				<xsl:with-param name="separator" select="' | '"/>
				<xsl:with-param name="filter">persona</xsl:with-param>
			</xsl:call-template>

			<!-- contributor.colaborator row -->
			<xsl:call-template name="render-normal-field">
				<xsl:with-param name="name" select="'contributor-colaborator'"/>
				<xsl:with-param name="elements" select="dim:field[@element='contributor' and @qualifier='colaborator'] "/>
				<xsl:with-param name="separator" select="' | '"/>
				<xsl:with-param name="filter">persona </xsl:with-param>
			</xsl:call-template>


		</xsl:if>

		<!-- language row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'language'"/>
			<xsl:with-param name="elements" select="dim:field[@element='language' and not(@qualifier)] "/>
			<xsl:with-param name="type" select="'i18n-code'"/>
		</xsl:call-template>

		<!-- publisher row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'publisher'"/>
			<xsl:with-param name="elements" select="dim:field[@element='publisher'] "/>
		</xsl:call-template>

		<!-- Si hay informacion de la revista, mostramos el metadato -->
		<xsl:if test="dim:field[@element='relation' and @qualifier='journalTitle']">
			<div class="metadata simple-item-view-other relation-journal">
				<span class="metadata-label"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-relation-journal</i18n:text>:</span>
				<span class="metadata-value">
					<xsl:value-of select="dim:field[@element='relation' and @qualifier='journalTitle']" disable-output-escaping="yes"/>
					<xsl:if test="dim:field[@element='relation' and @qualifier='journalVolumeAndIssue']">
						<xsl:text>; </xsl:text>
						<xsl:value-of select="dim:field[@element='relation' and @qualifier='journalVolumeAndIssue']" disable-output-escaping="yes"/>
					</xsl:if>
				</span>
			</div>
		</xsl:if>

		<!-- sedici.relation.event row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'relation-event'"/>
			<xsl:with-param name="elements" select="dim:field[@element='relation' and @qualifier='event'] "/>
		</xsl:call-template>

		<!-- relation.dossier row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'relation-dossier'"/>
			<xsl:with-param name="elements" select="dim:field[@element='relation' and @qualifier='dossier'] "/>
		</xsl:call-template>

		<!-- originInfo row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'originInfo'"/>
			<xsl:with-param name="elements" select="dim:field[@element=' '] "/>
			<xsl:with-param name="filter">institucion</xsl:with-param>
		</xsl:call-template>

		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'identifier-doi'"/>
			<xsl:with-param name="elements" select="dim:field[@element='identifier' and @qualifier='doi'] "/>
			<xsl:with-param name="separator" select="' | '"/>
			<xsl:with-param name="type" select="'url'"/>
		</xsl:call-template>

		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'identifier-handle'"/>
			<xsl:with-param name="elements" select="dim:field[@element='identifier' and @qualifier='handle'] "/>
			<xsl:with-param name="separator" select="' | '"/>
			<xsl:with-param name="type" select="'url'"/>
		</xsl:call-template>

		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'identifier-other'"/>
			<xsl:with-param name="elements" select="dim:field[@element='identifier' and @qualifier='other'] "/>
			<xsl:with-param name="separator" select="' | '"/>
			<xsl:with-param name="type" select="'url'"/>
		</xsl:call-template>

		<!-- identifier.issn row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'identifier-issn'"/>
			<xsl:with-param name="elements" select="dim:field[@element='identifier' and @qualifier='issn'] "/>
		</xsl:call-template>

		<!-- identifier.isbn row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'identifier-isbn'"/>
			<xsl:with-param name="elements" select="dim:field[@element='identifier' and @qualifier='isbn'] "/>
		</xsl:call-template>

		<!-- location row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'location'"/>
			<xsl:with-param name="elements" select="dim:field[@element='location'] "/>
			<xsl:with-param name="type" select="'url'"/>
			<xsl:with-param name="acotar" select="'true'"/>
		</xsl:call-template>

		<!-- coverage.spatial row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'coverage-spatial'"/>
			<xsl:with-param name="elements" select="dim:field[@element='coverage' and @qualifier='spatial'] "/>
		</xsl:call-template>

		<!-- coverage.temporal row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'coverage-temporal'"/>
			<xsl:with-param name="elements" select="dim:field[@element='coverage' and @qualifier='temporal'] "/>
		</xsl:call-template>

		<!-- format.extent row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'format-extent'"/>
			<xsl:with-param name="elements" select="dim:field[@element='format' and @qualifier='extent'] "/>
		</xsl:call-template>

		<!-- subjects row -->
		<xsl:if test="dim:field[@element='subject']">
			<div id="subjects">
				<!-- subject.materias row -->
				<xsl:call-template name="render-normal-field">
					<xsl:with-param name="name" select="'subject-materias'"/>
					<xsl:with-param name="elements" select="dim:field[@element='subject' and @qualifier='materias'] "/>
					<xsl:with-param name="filter">subject</xsl:with-param>
				</xsl:call-template>

				<!-- todos los descriptores (terminos de tesuaro) -->
				<xsl:call-template name="render-normal-field">
					<xsl:with-param name="name" select="'subject-descriptores'"/>
					<xsl:with-param name="elements" select="dim:field[(@element='subject' and @qualifier='descriptores') or (@element='subject' and @qualifier='decs') or (@element='subject' and @qualifier='eurovoc') or (@element='subject' and @qualifier='acmcss98') or (@element='subject' and @qualifier='other')] "/>
					<xsl:with-param name="filter">descriptor</xsl:with-param>
				</xsl:call-template>

				<!-- subject.keyword row --> 
				<xsl:call-template name="render-normal-field">
					<xsl:with-param name="name" select="'subject-keyword'" />
					<xsl:with-param name="elements" select="dim:field[@element='subject' and @qualifier='keyword']" />
					<xsl:with-param name="filter">descriptor</xsl:with-param>
				</xsl:call-template>
			</div>
		</xsl:if>

		<!-- relation-review-of row (es probable que sea uno solo) -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'relation-review-of'"/>
			<xsl:with-param name="elements" select="dim:field[@element='relation' and @qualifier='isReviewOf'] "/>
			<xsl:with-param name="type">url</xsl:with-param>
		</xsl:call-template>

		<!-- relation-reviewed-by row (es probable que sea uno solo) -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'relation-reviewed-by'"/>
			<xsl:with-param name="elements" select="dim:field[@element='relation' and @qualifier='isReviewedBy'] "/>
			<xsl:with-param name="type">url</xsl:with-param>
		</xsl:call-template>

		<!-- Mostramos los documentos relacionados (es probable que sean muchos) -->
		<xsl:if test="dim:field[@element='relation' and @qualifier='isRelatedWith']">
			<div class="metadata simple-item-view-other relation-related-with">
				<h2><i18n:text>xmlui.dri2xhtml.METS-1.0.item-relation-related-with</i18n:text></h2>
				<ul>
					<xsl:for-each select="dim:field[@element='relation' and @qualifier='isRelatedWith']">
						<li>
							<a target="_blank">
								<xsl:attribute name="href"><xsl:value-of select="."/></xsl:attribute>
								<xsl:value-of select="."/>
							</a>
						</li>
					</xsl:for-each>
				</ul>
			</div>
		</xsl:if>

		<!-- date.available row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'date-available'" />
			<xsl:with-param name="elements" select="dim:field[@element='date' and @qualifier='available']" />
			<xsl:with-param name="type" select="'date'"/>
		</xsl:call-template>

		<xsl:apply-templates select="/mets:METS" mode="generate-bitstream"/>

		<!-- status row -->
		<xsl:if test="dim:field[@element='status'] or dim:field[@element='fulltext']">
	        <div id="other_attributes">
				<h2><i18n:text>xmlui.dri2xhtml.METS-1.0.other-attributes</i18n:text></h2>
				<ul>
					<xsl:if test="dim:field[@element='status']">
						<li class="metadata peer-review">
							<i18n:text>xmlui.dri2xhtml.METS-1.0.item-is-<xsl:value-of select="dim:field[@element='status']"/></i18n:text>
						</li>
					</xsl:if>
			
					<!-- fulltext row -->
					<xsl:if test="dim:field[@element='fulltext']">
						<li class="metadata fulltext">
							<i18n:text>xmlui.dri2xhtml.METS-1.0.item-<xsl:value-of select="dim:field[@element='fulltext']"/>-fulltext</i18n:text>
						</li>
					</xsl:if>
				</ul>
			</div>
		</xsl:if>
		
		<!-- mods.recordInfo.recordContentSource row -->
		<xsl:if test="dim:field[@element='recordInfo' and @qualifier='recordContentSource']">
			<div class="metadata simple-item-view-other record-source">
				<span class="metadata-value">
					<i18n:text>xmlui.dri2xhtml.METS-1.0.item-record-source</i18n:text>
					<xsl:value-of select="dim:field[@element='recordInfo' and @qualifier='recordContentSource']" disable-output-escaping="yes"/>
				</span>
			</div>
		</xsl:if>
		
		<!-- Generate the Creative Commons license information from the file section (DSpace deposit license hidden by default) -->
		<xsl:apply-templates select="mets:fileSec/mets:fileGrp[@USE='CC-LICENSE'] "/>



	</xsl:template>



	<!-- Renderiza un campo con su correspondiente label (solo si hay elementos para mostrar) -->
	<xsl:template name="render-normal-field">
		<xsl:param name="name"/>
		<xsl:param name="elements"/>
		<xsl:param name="separator" select="'; '"/>
		<xsl:param name="type" select="'text'"/>
		<xsl:param name="acotar"/>
		<xsl:param name="filter" select="''"/>

		<!-- Generamos salida solo si hay algun elemento para mostrar -->
		<xsl:if test="count($elements) &gt; 0">
			<div>
				<xsl:attribute name="class">
					<xsl:text>metadata simple-item-view-other </xsl:text>
					<xsl:value-of select="$name"/>
				</xsl:attribute>

			<xsl:variable name="externalMetadataURL">
			            <xsl:text>cocoon:/</xsl:text>
			            <xsl:value-of select="/dri:document/dri:meta/dri:repositoryMeta/dri:repository/@url"/>
			           
		        </xsl:variable>
				<span class="metadata-label">
						<i18n:translate>
   							<i18n:text>xmlui.dri2xhtml.METS-1.0.item-<xsl:value-of select="$name"/></i18n:text>
   							<i18n:param>
   								<xsl:apply-templates select="document($externalMetadataURL)" mode="nameRepo"/>
   								dddd
   								<xsl:value-of select="$externalMetadataURL"/>
   								</i18n:param>
   						</i18n:translate>
				:</span>
		
		
				<xsl:call-template name="render-field-value">
					<xsl:with-param name="elements" select="$elements"/>
					<xsl:with-param name="index" select="1"/>
					<xsl:with-param name="separator" select="$separator"/>
					<xsl:with-param name="type" select="$type"/>
					<xsl:with-param name="acotar" select="$acotar"/>
					<xsl:with-param name="filter" select="$filter"/>
				</xsl:call-template>

			</div>
		</xsl:if>
	</xsl:template>
<xsl:template match="mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim" mode="nameRepo">
			  		<xsl:value-of select="dim:field[@element='name']"/>
   	 </xsl:template>
	<xsl:template match="dri:list[@id='aspect.submission.StepTransformer.list.submit-describe']">
		<i18n:text>sedici.common.camposObligatorios</i18n:text><br/><br/>
		<xsl:apply-templates />
		</xsl:template>
	
    <!-- Solo las urls se acotan en caso de que este explicitado -->
	<xsl:template name="render-field-value">
		<xsl:param name="elements"/>
		<xsl:param name="index"/>
		<xsl:param name="separator"/>
		<xsl:param name="type"/>
		<xsl:param name="acotar"/>
		<xsl:param name="filter"/>

		<span class="metadata-value">

			<xsl:choose>
				<xsl:when test="$type='url'">
				<!-- Si $type =url pero no es una url bien formada no se muestra como link. Si $acotar = true, pero es un handle, se muestra completo, de caso contrario solo se muestra el host -->
				  <xsl:choose>					      
			         <xsl:when test="java:ar.edu.unlp.sedici.xmlui.xsl.XslExtensions.isUrl($elements[$index])">
			            <a target="_blank">
								<xsl:attribute name="href">
								<xsl:value-of select="$elements[$index] "/>
							</xsl:attribute>
								<xsl:choose>
									<xsl:when test="contains($elements[$index],'handle.net')">
										<xsl:value-of select="$elements[$index]" disable-output-escaping="yes"/>
							   </xsl:when>
									<xsl:when test="$acotar = 'true'">
																	        <xsl:value-of select="java:ar.edu.unlp.sedici.xmlui.xsl.XslExtensions.getBaseUrl($elements[$index])" disable-output-escaping="yes"/>/...

									</xsl:when>
									<xsl:otherwise>
			             <xsl:value-of select="$elements[$index]" disable-output-escaping="yes"/>
			         </xsl:otherwise>
								</xsl:choose>
							</a>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="$elements[$index]" disable-output-escaping="yes"/>
						</xsl:otherwise>
					</xsl:choose>

				</xsl:when>

				<xsl:when test="$type='date'">
					<xsl:call-template name="render-date">
						<xsl:with-param name="dateString" select="$elements[$index] "/>
					</xsl:call-template>
				</xsl:when>

				<xsl:when test="$type='i18n-code'">
					<i18n:text>xmlui.dri2xhtml.METS-1.0.code-value-<xsl:value-of select="$elements[$index]"/></i18n:text>
				</xsl:when>

				<xsl:when test="$filter!=''">
					<a>
						<xsl:attribute name="href"><xsl:value-of select="$linkFilter"/>?filtertype=<xsl:value-of select="$filter"/>&amp;filter="<xsl:value-of select="translate($elements[$index],'áéíóú','aeiou')"/>"</xsl:attribute>
						<xsl:value-of select="$elements[$index]" disable-output-escaping="yes"/>
					</a>
				</xsl:when>

				<xsl:otherwise>
					<xsl:value-of select="$elements[$index]" disable-output-escaping="yes"/>
				</xsl:otherwise>
			</xsl:choose>

		</span>

		<xsl:if test="($index &lt; count($elements))">
			<xsl:if test="($separator != '')"><xsl:value-of select="$separator"/></xsl:if>
			<xsl:call-template name="render-field-value">
				<xsl:with-param name="elements" select="$elements"/>
				<xsl:with-param name="index" select="($index + 1)"/>
				<xsl:with-param name="separator" select="$separator"/>
				<xsl:with-param name="type" select="$type"/>
				<xsl:with-param name="acotar" select="$acotar"/>
				<xsl:with-param name="filter" select="$filter"/>
			</xsl:call-template>
		</xsl:if>
	</xsl:template>


	<xsl:template name="render-date">
		<xsl:param name="dateString"/>
		<!-- TODO: se debería obtener el locale del usuario -->
		<xsl:variable name="locale" select="java:java.util.Locale.new('es')"/>

		<!-- Se espera el formato YYYY-MM-DD[THH:mm:ssZ] -->

		<xsl:value-of select="java:ar.edu.unlp.sedici.xmlui.xsl.XslExtensions.formatearFecha($dateString, $locale)"/>

	</xsl:template>

	<xsl:template match="dim:dim" mode="itemDetailView-DIM">
		<table class="ds-includeSet-table detailtable">
			<xsl:apply-templates mode="itemDetailView-DIM"/>
		</table>
		<span class="Z3988">
			<xsl:attribute name="title">
                 <xsl:call-template name="renderCOinS"/>
            </xsl:attribute>
			&#xFEFF; <!-- non-breaking space to force separating the end tag -->
		</span>
	</xsl:template>

	<!-- Aca van los metadatos detallados -->
	<xsl:template match="dim:field" mode="itemDetailView-DIM">
		<tr>
			<xsl:attribute name="class">
                    <xsl:text>ds-table-row </xsl:text>
                    <xsl:if test="(position() div 2 mod 2 = 0)">even </xsl:if>
                    <xsl:if test="(position() div 2 mod 2 = 1)">odd </xsl:if>
                </xsl:attribute>
			<td class="label-cell">
				<xsl:value-of select="./@mdschema"/>
				<xsl:text>.</xsl:text>
				<xsl:value-of select="./@element"/>
				<xsl:if test="./@qualifier">
					<xsl:text>.</xsl:text>
					<xsl:value-of select="./@qualifier"/>
				</xsl:if>
			</td>
			<td>
				<xsl:value-of select="./node()" disable-output-escaping="yes"/>
				<xsl:if test="./@authority and ./@confidence">
					<xsl:call-template name="authorityConfidenceIcon">
						<xsl:with-param name="confidence" select="./@confidence"/>
					</xsl:call-template>
				</xsl:if>
			</td>
                <td><xsl:value-of select="./@language"/></td>
            </tr>
	</xsl:template>

	<!--dont render the item-view-toggle automatically in the summary view, only when it get's called -->
	<xsl:template match="dri:p[contains(@rend , 'item-view-toggle') and
        (preceding-sibling::dri:referenceSet[@type = 'summaryView'] or following-sibling::dri:referenceSet[@type = 'summaryView'])]">
	</xsl:template>

	<!-- dont render the head on the item view page -->
	<xsl:template match="dri:div[@n='item-view']/dri:head" priority="5">
	</xsl:template>

	<xsl:template match="mets:METS" mode="generate-bitstream">

		<!-- Generate the bitstream information from the file section -->
		<div id="item-file-section">
			<h2><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-head</i18n:text></h2>
			<xsl:choose>
				<xsl:when test="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL' or @USE='ORE'] or ./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@element='identifier' and @qualifier='uri' and @mdschema='sedici']">
					<xsl:if test="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']">
						<xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']">
							<xsl:with-param name="context" select="."/>
							<xsl:with-param name="primaryBitstream" select="./mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"/>
						</xsl:apply-templates>
					</xsl:if>

					<!-- Special case for handling ORE resource maps stored as DSpace bitstreams -->
					<xsl:if test="./mets:fileSec/mets:fileGrp[@USE='ORE']">
						<xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='ORE'] "/>
					</xsl:if>

					<!-- Localizacion Electronica -->
					<xsl:if test="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@element='identifier' and @qualifier='uri' and @mdschema='sedici']">
						<xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@element='identifier' and @qualifier='uri' and @mdschema='sedici'] "/>
					</xsl:if>
				</xsl:when>
				<xsl:otherwise>
					<p><i18n:text>xmlui.dri2xhtml.METS-1.0.item-no-files</i18n:text></p>
				</xsl:otherwise>
			</xsl:choose>
		</div>
	</xsl:template>

	<xsl:template match="mets:fileGrp[@USE='CONTENT']">
		<xsl:param name="context"/>
		<xsl:param name="primaryBitstream" select="-1"/>

		<div class="file-list">
			<xsl:choose>
				<!-- If one exists and it's of text/html MIME type, only display the primary bitstream -->
				<xsl:when test="mets:file[@ID=$primaryBitstream]/@MIMETYPE='text/html'">
					<xsl:apply-templates select="mets:file[@ID=$primaryBitstream]">
						<xsl:with-param name="context" select="$context"/>
					</xsl:apply-templates>
				</xsl:when>
				<!-- Otherwise, iterate over and display all of them -->
				<xsl:otherwise>
					<xsl:apply-templates select="mets:file">
						<!--Do not sort any more bitstream order can be changed -->
						<!--<xsl:sort data-type="number" select="boolean(./@ID=$primaryBitstream)" order="descending"/> -->
						<!--<xsl:sort select="mets:FLocat[@LOCTYPE='URL']/@xlink:title"/> -->
						<xsl:with-param name="context" select="$context"/>
					</xsl:apply-templates>
				</xsl:otherwise>
			</xsl:choose>
		</div>
	</xsl:template>

	<xsl:template match="mets:file">
		<xsl:param name="context" select="."/>

		<!-- nuevo nombre para el documento a descargar -->
		<xsl:variable name="documentTitle">
			<xsl:choose>
				<xsl:when test="mets:FLocat[@LOCTYPE='URL']/@xlink:label">
					<xsl:value-of select="java:ar.edu.unlp.sedici.xmlui.xsl.XslExtensions.codificarURL(string(mets:FLocat[@LOCTYPE='URL']/@xlink:label))"/>
				</xsl:when>
				<xsl:when test="$context/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='title'][@mdschema='dc']">
					<xsl:value-of select="java:ar.edu.unlp.sedici.xmlui.xsl.XslExtensions.codificarURL(string($context/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='title'][@mdschema='dc']))"/>
				</xsl:when>
				<xsl:otherwise>
					<i18n:text>xmlui.bitstream.downloadName</i18n:text>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<xsl:variable name="extension" select="substring-after(mets:FLocat[@LOCTYPE='URL']/@xlink:title, '.')"/>
		<xsl:variable name="sequence" select="substring-after(mets:FLocat[@LOCTYPE='URL']/@xlink:href, '?')"/>

		<xsl:variable name="link"> 
           <xsl:value-of select="substring-before(mets:FLocat[@LOCTYPE='URL']/@xlink:href, substring-after($context/@ID, ':'))"/><xsl:value-of select="substring-after($context/@ID, ':')"/>/<xsl:value-of select="$documentTitle"/>.<xsl:value-of select="$extension"/>?<xsl:value-of select="$sequence"/>
        </xsl:variable>

		<div class="file-wrapper clearfix">
			<div class="thumbnail-wrapper">
				<xsl:choose>
             	<xsl:when test="$context/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='embargo' and @qualifier='liftDate']">
	              <i18n:text>sedici.comunidades.tesis.embargo</i18n:text>
						<br />
						<span>
							<xsl:choose>
	                        <xsl:when test="$context/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/mets:file[@GROUPID=current()/@GROUPID]">
	                            <img alt="Thumbnail">
										<xsl:attribute name="src">
	                                    <xsl:value-of select="$context/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/mets:file[@GROUPID=current()/@GROUPID]/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
	                                </xsl:attribute>
									</img>
								</xsl:when>
								<xsl:otherwise>
									<xsl:variable name="file_type" select="substring-before(@MIMETYPE, '/')"/>
									<xsl:variable name="file_subtype" select="substring-after(@MIMETYPE, '/')"/>
	                        	<xsl:variable name="img_path">
										<xsl:choose>
		                        		<xsl:when test="$file_type = 'image'">mime_img.png</xsl:when>
		                        		<xsl:when test="$file_subtype = 'pdf'">mime_pdf.png</xsl:when>
		                        		<xsl:when test="$file_subtype = 'msword'">mime_msword.png</xsl:when>
		                        		<xsl:otherwise>mime.png</xsl:otherwise>
		                        	</xsl:choose>
									</xsl:variable>
									<img alt="Icon" src="{concat($theme-path, '/images/',$img_path)}"/>
								</xsl:otherwise>
							</xsl:choose>
						</span>

					</xsl:when>
					<xsl:otherwise>
						<a class="image-link" target="_blank">
							<xsl:attribute name="href">
	                        <xsl:value-of select="$link"/>                        
	                    </xsl:attribute>

						 <xsl:choose>
	                        <xsl:when test="$context/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/mets:file[@GROUPID=current()/@GROUPID]">
	                            <img alt="Thumbnail">
	                                <xsl:attribute name="src">
	                                    <xsl:value-of select="$context/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/mets:file[@GROUPID=current()/@GROUPID]/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
	                                </xsl:attribute>
	                            </img>
	                        </xsl:when>
	                        <xsl:otherwise>
	                        	<xsl:variable name="file_type" select="substring-before(@MIMETYPE, '/')"/>
	                        	<xsl:variable name="file_subtype" select="substring-after(@MIMETYPE, '/')"/>
	                        	<xsl:variable name="img_path">
		                        	<xsl:choose>
		                        		<xsl:when test="$file_type = 'image'">mime_img.png</xsl:when>
		                        		<xsl:when test="$file_subtype = 'pdf'">mime_pdf.png</xsl:when>
		                        		<xsl:when test="$file_subtype = 'msword'">mime_msword.png</xsl:when>
		                        		<xsl:otherwise>mime.png</xsl:otherwise>
		                        	</xsl:choose>
	                        	</xsl:variable>
	                            <img alt="Icon" src="{concat($theme-path, '/images/',$img_path)}"/>
	                         </xsl:otherwise>
	                    </xsl:choose>
                	</a>
                </xsl:otherwise>
             </xsl:choose>
            </div>

			<div class="file-metadata">
				<xsl:choose>
					<xsl:when test="$context/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='embargo' and @qualifier='liftDate']">
						<xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:label" disable-output-escaping="yes"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:if test="mets:FLocat[@LOCTYPE='URL']/@xlink:label != ''">
							<div>
								<a class="image-link" target="_blank">
									<xsl:attribute name="href">
				                        <xsl:value-of select="$link"/>
				                    </xsl:attribute>
									<span><xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:label" disable-output-escaping="yes"/></span>
								</a>
							</div>
						</xsl:if>
					</xsl:otherwise>
				</xsl:choose>
				<div>
					<span>
						<xsl:choose>
							<xsl:when test="@SIZE &lt; 1024">
								<xsl:value-of select="@SIZE"/>
								<i18n:text>xmlui.dri2xhtml.METS-1.0.size-bytes</i18n:text>
							</xsl:when>
							<xsl:when test="@SIZE &lt; 1024 * 1024">
								<xsl:value-of select="substring(string(@SIZE div 1024),1,5)"/>
								<i18n:text>xmlui.dri2xhtml.METS-1.0.size-kilobytes</i18n:text>
							</xsl:when>
							<xsl:when test="@SIZE &lt; 1024 * 1024 * 1024">
								<xsl:value-of select="substring(string(@SIZE div (1024 * 1024)),1,5)"/>
								<i18n:text>xmlui.dri2xhtml.METS-1.0.size-megabytes</i18n:text>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="substring(string(@SIZE div (1024 * 1024 * 1024)),1,5)"/>
								<i18n:text>xmlui.dri2xhtml.METS-1.0.size-gigabytes</i18n:text>
							</xsl:otherwise>
						</xsl:choose>
					</span>
					<xsl:text> - </xsl:text>
	                <!-- Lookup File Type description in local messages.xml based on MIME Type.
			         In the original DSpace, this would get resolved to an application via
			         the Bitstream Registry, but we are constrained by the capabilities of METS
			         and can't really pass that info through. -->
                    <span>
						<xsl:call-template name="getFileTypeDesc">
							<xsl:with-param name="mimetype">
								<xsl:value-of select="substring-before(@MIMETYPE,'/')"/>
								<xsl:text>/</xsl:text>
								<xsl:value-of select="substring-after(@MIMETYPE,'/')"/>
							</xsl:with-param>
						</xsl:call-template>
					</span>
				</div>
			</div>

		</div>


	</xsl:template>


	<xsl:template match="dim:field[@element='identifier' and @qualifier='uri' and @mdschema='sedici']">
		<div class="file-wrapper clearfix">
			<div class="thumbnail-wrapper">
				<a class="image-link" target="_blank">
					<xsl:attribute name="href">
						<xsl:value-of select="."/>
					</xsl:attribute>
					<img alt="Icon" src="{concat($theme-path, '/images/mime_link.png')}"/>
				</a>
			</div>
			<div class="file-metadata">
				<div>
					<i18n:text>xmlui.dri2xhtml.METS-1.0.item-identifier-uri</i18n:text>
				</div>
				<div>
					<a class="image-link" target="_blank">
						<xsl:attribute name="href">
							<xsl:value-of select="." />
						</xsl:attribute>
						<xsl:value-of select="java:ar.edu.unlp.sedici.xmlui.xsl.XslExtensions.getBaseUrl(.)" disable-output-escaping="yes"/>
						<xsl:text>/...</xsl:text>
					</a>
				</div>
			</div>
		</div>
	</xsl:template>

	<!-- Listado de los items recientemente subidos -->
	<xsl:template match="dim:dim" mode="itemSummaryList-DIM-metadata">
		<xsl:param name="href"/>
			<div  class="artifact-title">
				<span class="title">
				<xsl:element name="a">
					<xsl:attribute name="href">
                        <xsl:value-of select="$href"/>
                    </xsl:attribute>
					<xsl:choose>
						<xsl:when test="dim:field[@element='title']">
							<xsl:value-of select="dim:field[@element='title'][1]/node()" disable-output-escaping="yes"/>
						</xsl:when>
						<xsl:otherwise>
							<i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:element>
				</span>				
				<span class="publisher-date">
					<!-- date.issued : extraemos el año solamente -->
					<xsl:if test="dim:field[@element='date' and @qualifier='issued'] ">
						<xsl:value-of select="substring(dim:field[@element='date' and @qualifier='issued'],1,4)"></xsl:value-of>
					</xsl:if>
				</span>
			</div>
		<div class="artifact-description">			
				<div class="author">
					<xsl:choose>
						<xsl:when test="dim:field[@element='creator']">
							<xsl:for-each select="dim:field[@element='creator']">
								<xsl:copy-of select="node()"/>
								<xsl:if test="count(following-sibling::dim:field[@element='creator']) != 0">
									<xsl:text>; </xsl:text>
								</xsl:if>
							</xsl:for-each>
						</xsl:when>
						<xsl:when test="dim:field[@element='contributor' and @qualifier='compiler']">
							<xsl:for-each select="dim:field[@element='contributor' and @qualifier='compiler']">
								<xsl:copy-of select="node()"/>
								<xsl:if test="count(following-sibling::dim:field[@element='contributor' and @qualifier='compiler']) != 0">
									<xsl:text>; </xsl:text>
								</xsl:if>
							</xsl:for-each>
						</xsl:when>
						<xsl:otherwise>
							<i18n:text>xmlui.dri2xhtml.METS-1.0.no-author</i18n:text>
						</xsl:otherwise>
					</xsl:choose>
				</div>

				<xsl:variable name="originInfoContent">
					<xsl:choose>
						
						<!-- Solo para el tipo tesis: grado alanzado e institución otorgante -->
						<xsl:when test="dim:field[@element='type'] = 'Tesis'">
							<xsl:value-of select="dim:field[@element='degree' and @qualifier='name']"/>
							<xsl:text>; </xsl:text>
							<xsl:value-of select="dim:field[@element='degree' and @qualifier='grantor']"/>
						</xsl:when>

						<!-- Solo para el tipo Objeto de coferencia: evento -->
						<xsl:when test="dim:field[@element='type'] = 'Objeto de conferencia'">
							<xsl:value-of select="dim:field[@element='relation' and @qualifier='event']"/>
						</xsl:when>
						
						<!-- Si tiene journalTitle -->
						<xsl:when test="dim:field[@element = 'relation' and @qualifier='journalTitle']">
							<xsl:value-of select="dim:field[@element='relation' and @qualifier='journalTitle']"/>
							<xsl:if test="dim:field[@element='relation' and @qualifier='journalVolumeAndIssue']">
								<xsl:text>; </xsl:text>
								<xsl:value-of select="dim:field[@element='relation' and @qualifier='journalVolumeAndIssue']"/>
							</xsl:if>

							<!-- Si además tiene evento, lo muestro -->
							<xsl:if test="dim:field[@element = 'relation' and @qualifier='event']">
								<xsl:text> | </xsl:text>
								<xsl:value-of select="dim:field[@element='relation' and @qualifier='event']"/>
							</xsl:if>
						</xsl:when>
					</xsl:choose>			
				</xsl:variable>
				
				<div class="originInfo">
					<xsl:choose>					
						<xsl:when test="$originInfoContent != ''">
							<xsl:value-of select="$originInfoContent"/>
						</xsl:when>
						<xsl:otherwise>
							<!-- si nada aplica, mostramos el originInfo -->
							<xsl:value-of select="dim:field[@element='originInfo' and @qualifier='place']"/>
						</xsl:otherwise>
					</xsl:choose>			
				</div>
				<div class="type">
					<xsl:choose>
						<xsl:when test="dim:field[@element='subtype']"> 
								<xsl:copy-of select="dim:field[@element='subtype']/node()"/> 
						</xsl:when>
						<xsl:when test="dim:field[@element='type']"> 
								<xsl:copy-of select="dim:field[@element='type']/node()"/> 
						</xsl:when>
						<!-- No hay otherwise -->
					</xsl:choose>
				</div>	
				<div i18n:attr="title">
					<xsl:choose>
						 	<xsl:when test="../../../../mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']"> 
								    <xsl:attribute name="class">availability local</xsl:attribute>
								    <xsl:attribute name="title">xmlui.dri2xhtml.METS-1.0.item-availability-local-help</xsl:attribute>
								    <i18n:text>xmlui.dri2xhtml.METS-1.0.item-availability-local</i18n:text>
					            </xsl:when>					
							<!-- Localizacion Electronica -->
							<xsl:when test="dim:field[@element='identifier'  and @qualifier='uri' and @mdschema='sedici']"> 
								    <xsl:attribute name="class">availability linked</xsl:attribute>
								    <xsl:attribute name="title">xmlui.dri2xhtml.METS-1.0.item-availability-linked-help</xsl:attribute>
					                <i18n:text>xmlui.dri2xhtml.METS-1.0.item-availability-linked</i18n:text>
							</xsl:when>
					</xsl:choose>  
				</div>
        </div>
	</xsl:template>

	<!-- An item rendered in the detailView pattern, the "full item record"  view of a DSpace item in Manakin. -->
	<xsl:template name="itemDetailView-DIM">

		<!-- Output all of the metadata about the item from the metadata section -->
		<xsl:apply-templates select="mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim" mode="itemDetailView-DIM"/>

		<xsl:apply-templates select="." mode="generate-bitstream"/>

	  <!-- Generate the Creative Commons license information from the file section (DSpace deposit license hidden by default) -->
        <!-- <xsl:apply-templates select="mets:fileSec/mets:fileGrp[@USE='CC-LICENSE' or @USE='LICENSE']"/>-->
      

	</xsl:template>

	<xsl:template match="/dri:document/dri:body/dri:div/dri:p[@rend='item-view-toggle item-view-toggle-bottom']">
	</xsl:template>

	<xsl:template match="/dri:document/dri:body/dri:div/dri:referenceSet[@id='aspect.artifactbrowser.ItemViewer.referenceSet.collection-viewer']/dri:reference/dri:referenceSet[@rend='hierarchy']">
		<xsl:apply-templates select="dri:head"/>
		<ul id="ds-trail-items">
			<xsl:apply-templates select="/dri:document/dri:body/dri:div[@id='aspect.artifactbrowser.AddItemCollections.div.item-view-sedici']/dri:list[@id='aspect.artifactbrowser.AddItemCollections.list.item-view-sedici-colecciones']/dri:list" mode='collectionsWithCommunities'/>
        </ul>
	</xsl:template>

	<xsl:template match="dri:list[@id='aspect.artifactbrowser.AddItemCollections.list.item-view-sedici-colecciones']/dri:list" mode="collectionsWithCommunities">
    	<li class="ds-trail-link">
			<a>
				<xsl:attribute name="href">
		    	<xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'] "/>/handle/<xsl:value-of select="dri:item[1]/dri:xref/@target"/>
	    	</xsl:attribute>
				<xsl:value-of select="dri:item[1]/dri:xref"/>
			</a>
		
		  <span>→</span>
			<a>
				<xsl:attribute name="href">
    	    		<xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'] "/>/handle/<xsl:value-of select="dri:item[2]/dri:xref/@target"/>
    	 		</xsl:attribute>
				<xsl:value-of select="dri:item[2]/dri:xref"/>
			</a>
		</li>
		
	</xsl:template>

	<xsl:template match="dri:div[@id='aspect.artifactbrowser.RestrictedItem.div.withdrawn']">
		<xsl:apply-templates select="dri:head"/>
		<xsl:apply-templates select="dri:p[@id='aspect.artifactbrowser.RestrictedItem.p.item_status'] "/>
		<p class="ds-paragraph">
			<i18n:text>xmlui.ArtifactBrowser.RestrictedItem.para_item_withdrawn_contact</i18n:text>
			<a>
			<xsl:attribute name="href">mailto: info@sedici.unlp.edu.ar</xsl:attribute>
				info@sedici.unlp.edu.ar
			</a>
		</p>
	</xsl:template>


</xsl:stylesheet>