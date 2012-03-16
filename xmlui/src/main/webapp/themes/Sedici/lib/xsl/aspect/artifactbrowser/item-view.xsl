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

    <xsl:output indent="yes"/>

    <xsl:template name="itemSummaryView-DIM">
        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
        mode="itemSummaryView-DIM"/>

        <!-- Generate the bitstream information from the file section -->
        <div id="item-file-section">
			<h2><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-head</i18n:text></h2>
	        <xsl:choose>
	            <xsl:when test="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']">
	                <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']">
	                    <xsl:with-param name="context" select="."/>
	                    <xsl:with-param name="primaryBitstream" select="./mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"/>
	                </xsl:apply-templates>
	            </xsl:when>
	            
	            <!-- Special case for handling ORE resource maps stored as DSpace bitstreams -->
	            <xsl:when test="./mets:fileSec/mets:fileGrp[@USE='ORE']">
	                <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='ORE']"/>
	            </xsl:when>
	            
	            <xsl:otherwise>
					<p><i18n:text>xmlui.dri2xhtml.METS-1.0.item-no-files</i18n:text></p>
	            </xsl:otherwise>
	        </xsl:choose>
		</div>
		
        <!-- Generate the Creative Commons license information from the file section (DSpace deposit license hidden by default)-->
        <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CC-LICENSE']"/>

    </xsl:template>

<!-- Aca va la muestra sencilla de un item -->
    <xsl:template match="dim:dim" mode="itemSummaryView-DIM">
        <div class="item-summary-view-metadata">
            <xsl:call-template name="itemSummaryView-DIM-fields"/>
        </div>
    </xsl:template>

    <xsl:template name="itemSummaryView-DIM-fields">
	
		<!-- date.available row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'date-available'" />
			<xsl:with-param name="elements" select="dim:field[@element='date' and @qualifier='available']" />
			<xsl:with-param name="type" select="'date'"/>
		</xsl:call-template>

		<!-- Title row -->
		<xsl:choose>
			<xsl:when test="count(dim:field[@element='title'][not(@qualifier)]) &gt; 0">
				<!-- display first title as h1 -->
				<h1>
					<xsl:value-of select="dim:field[@element='title'][not(@qualifier)][1]/node()" disable-output-escaping="yes" />
				</h1>
				<xsl:if test="dim:field[@element='title'][not(@qualifier)][2]">
					<div class="simple-item-view-title">
						<xsl:for-each select="dim:field[@element='title'][not(@qualifier)][position() &gt; 1]">
							<span class="metadata-value">
								<xsl:value-of select="./node()" disable-output-escaping="yes" />
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
			<xsl:with-param name="name" select="'title-subtitle'" />
			<xsl:with-param name="elements" select="dim:field[@element='title' and @qualifier='subtitle']" />
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
	
							<xsl:copy-of select="node()" />
	
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
	
							<xsl:copy-of select="node()" />
	
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
				<h2><i18n:text>xmlui.dri2xhtml.METS-1.0.embargoed-document</i18n:text></h2>
				<p><i18n:text>xmlui.dri2xhtml.METS-1.0.embargoed-document-description</i18n:text></p>
				<xsl:call-template name="render-normal-field">
					<xsl:with-param name="name" select="'embargo-liftDate'" />
					<xsl:with-param name="elements" select="dim:field[@element='embargo' and @qualifier='liftDate']" />
				</xsl:call-template>
			</div>
		</xsl:if>

		<!-- date.issued row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'date-issued'" />
			<xsl:with-param name="elements" select="dim:field[@element='date' and @qualifier='issued']" />
			<xsl:with-param name="type" select="'date'" />
		</xsl:call-template>

		<!-- Para el Tipo de Documento mostramos el sedici.subtype porque es mas especifico -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'subtype'" />
			<xsl:with-param name="elements" select="dim:field[@element='subtype']" />
			<xsl:with-param name="type" select="'i18n-code'"/>
		</xsl:call-template>

		<!-- title.alternative row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'title-alternative'" />
			<xsl:with-param name="elements" select="dim:field[@element='title' and @qualifier='alternative']" />
			<xsl:with-param name="type" select="'date'" />
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

		<!-- Si tiene director asumimos que es una tesis -->
		<xsl:if test="dim:field[@element='contributor' and @qualifier='director']">
			<h2><i18n:text>xmlui.dri2xhtml.METS-1.0.tesis-info</i18n:text></h2>
			<!-- contributor.director row -->
			<xsl:call-template name="render-normal-field">
				<xsl:with-param name="name" select="'contributor-director'" />
				<xsl:with-param name="elements" select="dim:field[@element='contributor' and @qualifier='director']" />
			</xsl:call-template>
	
			<!-- contributor.codirector row -->
			<xsl:call-template name="render-normal-field">
				<xsl:with-param name="name" select="'contributor-codirector'" />
				<xsl:with-param name="elements" select="dim:field[@element='contributor' and @qualifier='codirector']" />
			</xsl:call-template>
	
			<!-- date.exposure row -->
			<xsl:call-template name="render-normal-field">
				<xsl:with-param name="name" select="'date-exposure'" />
				<xsl:with-param name="elements" select="dim:field[@element='date' and @qualifier='exposure']" />
				<xsl:with-param name="type" select="'date'" />
			</xsl:call-template>

			<!-- affiliatedInstitution row -->
			<xsl:call-template name="render-normal-field">
				<xsl:with-param name="name" select="'affiliated-institution'" />
				<xsl:with-param name="elements" select="dim:field[@element='AffiliatedInstitution']" />
			</xsl:call-template>
	
			<!-- degree.name row -->
			<xsl:call-template name="render-normal-field">
				<xsl:with-param name="name" select="'degree-name'" />
				<xsl:with-param name="elements" select="dim:field[@element='degree' and @qualifier='name']" />
			</xsl:call-template>
	
			<!-- degree.grantor row -->
			<xsl:call-template name="render-normal-field">
				<xsl:with-param name="name" select="'degree-grantor'" />
				<xsl:with-param name="elements" select="dim:field[@element='degree' and @qualifier='grantor']" />
			</xsl:call-template>
		</xsl:if>

		
		<h2><i18n:text>xmlui.dri2xhtml.METS-1.0.general-info</i18n:text></h2>
		<xsl:if test="not(dim:field[@element='contributor' and @qualifier='director'])">
			<!-- date.exposure row -->
			<xsl:call-template name="render-normal-field">
				<xsl:with-param name="name" select="'date-exposure'" />
				<xsl:with-param name="elements" select="dim:field[@element='date' and @qualifier='exposure']" />
				<xsl:with-param name="type" select="'date'" />
			</xsl:call-template>
		</xsl:if>

		<xsl:if test="(dim:field[@element='contributor' and (@qualifier='editor' or @qualifier='translator' or @qualifier='compiler' or @qualifier='juror' or @qualifier='colaborator')])">
			<div id="contributors">
				<!-- contributor.editor row -->
				<xsl:call-template name="render-normal-field">
					<xsl:with-param name="name" select="'contributor-editor'" />
					<xsl:with-param name="elements" select="dim:field[@element='contributor' and @qualifier='editor']" />
					<xsl:with-param name="separator" select="' | '"/>
				</xsl:call-template>
	
				<!-- contributor.translator row -->
				<xsl:call-template name="render-normal-field">
					<xsl:with-param name="name" select="'contributor-translator'" />
					<xsl:with-param name="elements" select="dim:field[@element='contributor' and @qualifier='translator']" />
					<xsl:with-param name="separator" select="' | '"/>
				</xsl:call-template>
	
				<!-- contributor.compiler row -->
				<xsl:if test="dim:field[@element='creator']">
					<xsl:call-template name="render-normal-field">
						<xsl:with-param name="name" select="'contributor-compiler'" />
						<xsl:with-param name="elements" select="dim:field[@element='contributor' and @qualifier='compiler']" />
						<xsl:with-param name="separator" select="' | '"/>
					</xsl:call-template>
				</xsl:if>
	
				<!-- contributor.juror row -->
				<xsl:call-template name="render-normal-field">
					<xsl:with-param name="name" select="'contributor-juror'" />
					<xsl:with-param name="elements" select="dim:field[@element='contributor' and @qualifier='juror']" />
					<xsl:with-param name="separator" select="' | '"/>
				</xsl:call-template>
	
				<!-- contributor.colaborator row -->
				<xsl:call-template name="render-normal-field">
					<xsl:with-param name="name" select="'contributor-colaborator'" />
					<xsl:with-param name="elements" select="dim:field[@element='contributor' and @qualifier='colaborator']" />
					<xsl:with-param name="separator" select="' | '"/>
				</xsl:call-template>
				
			</div>
		</xsl:if>

		<!-- language row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'language'" />
			<xsl:with-param name="elements" select="dim:field[@element='language' and not(@qualifier)]" />
			<xsl:with-param name="type" select="'i18n-code'"/>
		</xsl:call-template>

		<!-- publisher row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'publisher'" />
			<xsl:with-param name="elements" select="dim:field[@element='publisher']" />
		</xsl:call-template>

		<!-- relation.isPartOf row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'relation-isPartOf'" />
			<xsl:with-param name="elements" select="dim:field[@element='relation' and @qualifier='isPartOf']" />
		</xsl:call-template>

		<!-- relation.dossier row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'relation-dossier'" />
			<xsl:with-param name="elements" select="dim:field[@element='relation' and @qualifier='dossier']" />
		</xsl:call-template>

		<!-- originInfo row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'originInfo'" />
			<xsl:with-param name="elements" select="dim:field[@element='originInfo']" />
		</xsl:call-template>

		<!-- identifier rows -->
		<div class="identifiers">
			<xsl:call-template name="render-normal-field">
				<xsl:with-param name="name" select="'identifier-uri'" />
				<xsl:with-param name="elements" select="dim:field[@element='identifier' and @qualifier='uri' and @mdschema='sedici']" />
				<xsl:with-param name="separator" select="' | '"/>
				<xsl:with-param name="type" select="'url'"/>
			</xsl:call-template>
		
			<xsl:call-template name="render-normal-field">
				<xsl:with-param name="name" select="'identifier-doi'" />
				<xsl:with-param name="elements" select="dim:field[@element='identifier' and @qualifier='doi']" />
				<xsl:with-param name="separator" select="' | '"/>
				<xsl:with-param name="type" select="'url'"/>
			</xsl:call-template>

			<xsl:call-template name="render-normal-field">
				<xsl:with-param name="name" select="'identifier-handle'" />
				<xsl:with-param name="elements" select="dim:field[@element='identifier' and @qualifier='handle']" />
				<xsl:with-param name="separator" select="' | '"/>
				<xsl:with-param name="type" select="'url'"/>
			</xsl:call-template>
			
			<xsl:call-template name="render-normal-field">
				<xsl:with-param name="name" select="'identifier-other'" />
				<xsl:with-param name="elements" select="dim:field[@element='identifier' and @qualifier='other']" />
				<xsl:with-param name="separator" select="' | '"/>
				<xsl:with-param name="type" select="'url'"/>
			</xsl:call-template>
		</div>

		<!-- identifier.issn row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'identifier-issn'" />
			<xsl:with-param name="elements" select="dim:field[@element='identifier' and @qualifier='issn']" />
		</xsl:call-template>
		
		<!-- identifier.isbn row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'identifier-isbn'" />
			<xsl:with-param name="elements" select="dim:field[@element='identifier' and @qualifier='isbn']" />
		</xsl:call-template>
		
		<!-- location row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'location'" />
			<xsl:with-param name="elements" select="dim:field[@element='location']" />
		</xsl:call-template>

		<!-- coverage.spatial row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'coverage-spatial'" />
			<xsl:with-param name="elements" select="dim:field[@element='coverage' and @qualifier='spatial']" />
		</xsl:call-template>
		
		<!-- coverage.temporal row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'coverage-temporal'" />
			<xsl:with-param name="elements" select="dim:field[@element='coverage' and @qualifier='temporal']" />
		</xsl:call-template>
		
		<!-- format.extent row -->
		<xsl:call-template name="render-normal-field">
			<xsl:with-param name="name" select="'format-extent'" />
			<xsl:with-param name="elements" select="dim:field[@element='format' and @qualifier='extent']" />
		</xsl:call-template>
		
		<!-- subjects row -->
		<div id="subjects">
			<!-- subject.materias row -->
			<xsl:call-template name="render-normal-field">
				<xsl:with-param name="name" select="'subject-materias'" />
				<xsl:with-param name="elements" select="dim:field[@element='subject' and @qualifier='materias']" />
			</xsl:call-template>

			<!-- subject.decs row -->
			<xsl:call-template name="render-normal-field">
				<xsl:with-param name="name" select="'subject-decs'" />
				<xsl:with-param name="elements" select="dim:field[@element='subject' and @qualifier='decs']" />
			</xsl:call-template>

			<!-- subject.eurovoc row -->
			<xsl:call-template name="render-normal-field">
				<xsl:with-param name="name" select="'subject-eurovoc'" />
				<xsl:with-param name="elements" select="dim:field[@element='subject' and @qualifier='eurovoc']" />
			</xsl:call-template>

			<!-- subject.descriptores row -->
			<xsl:call-template name="render-normal-field">
				<xsl:with-param name="name" select="'subject-descriptores'" />
				<xsl:with-param name="elements" select="dim:field[@element='subject' and @qualifier='descriptores']" />
			</xsl:call-template>

			<!-- subject.other row -->
			<xsl:call-template name="render-normal-field">
				<xsl:with-param name="name" select="'subject-other'" />
				<xsl:with-param name="elements" select="dim:field[@element='subject' and @qualifier='other']" />
			</xsl:call-template>

			<!-- subject.keyword row --> 
			<xsl:call-template name="render-normal-field">
				<xsl:with-param name="name" select="'subject-keyword'" />
				<xsl:with-param name="elements" select="dim:field[@element='subject' and @qualifier='keyword']" />
			</xsl:call-template>
		</div>

		<!-- status row -->
		<xsl:if test="(dim:field[@element='status']='si' or dim:field[@element='fulltext']='si')">
			<div id="other_attributes">
				<h2><i18n:text>xmlui.dri2xhtml.METS-1.0.other-attributes</i18n:text></h2>
				<ul>
				<xsl:if test="(dim:field[@element='status']='si')">
					<li class="metadata peer-review">
						<i18n:text>xmlui.dri2xhtml.METS-1.0.item-is-peer-review</i18n:text>
					</li>
				</xsl:if>
		
				<!-- fulltext row -->
				<xsl:if test="(dim:field[@element='fulltext']='si')">
					<li class="metadata fulltext">
						<i18n:text>xmlui.dri2xhtml.METS-1.0.item-is-fulltext</i18n:text>
					</li>
				</xsl:if>
				</ul>
			</div>
		</xsl:if>

		<!-- Link para la vista full -->
		<xsl:if test="$ds_item_view_toggle_url != ''">
			<div id="view-item-metadata">
				<a>
					<xsl:attribute name="href"><xsl:value-of select="$ds_item_view_toggle_url" /></xsl:attribute>
					<i18n:text>xmlui.ArtifactBrowser.ItemViewer.show_full</i18n:text>
				</a>
			</div>
		</xsl:if>
			
		<!-- Generate the Creative Commons license information from the file section (DSpace deposit license hidden by default) -->
		<xsl:apply-templates select="mets:fileSec/mets:fileGrp[@USE='CC-LICENSE']"/>
    </xsl:template>

	<!-- Renderiza un campo con su correspondiente label (solo si hay elementos para mostrar) -->
	<xsl:template name="render-normal-field">
		<xsl:param name="name"/>
		<xsl:param name="elements"/>
		<xsl:param name="separator" select="'; '"/>
		<xsl:param name="type" select="'text'"/>
		
		<!-- Generamos salida solo si hay algun elemento para mostrar -->
		<xsl:if test="count($elements) &gt; 0">
			<div>
				<xsl:attribute name="class">
					<xsl:text>metadata simple-item-view-other </xsl:text>
					<xsl:value-of select="$name"/>
				</xsl:attribute>
	
				<span class="metadata-label"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-<xsl:value-of select="$name"/></i18n:text>:</span>
		
				<xsl:call-template name="render-field-value">
					<xsl:with-param name="elements" select="$elements"/>
					<xsl:with-param name="index" select="1"/>
					<xsl:with-param name="separator" select="$separator"/>
					<xsl:with-param name="type" select="$type"/>
				</xsl:call-template>
	
			</div>
		</xsl:if>
	</xsl:template>

	<xsl:template name="render-field-value">
		<xsl:param name="elements"/>
		<xsl:param name="index"/>
		<xsl:param name="separator"/>
		<xsl:param name="type"/>
		
		<span class="metadata-value">
			<xsl:choose>
				<xsl:when test="$type='url'">
					<a target="_blank">
						<xsl:attribute name="href">
							<xsl:value-of select="$elements[$index]"/>
						</xsl:attribute>
						<xsl:value-of select="$elements[$index]" disable-output-escaping="yes"/>
					</a>
				</xsl:when>
				
				<xsl:when test="$type='date'">
					<!-- Se espera el formato YYYY-MM-DD[THH:mm:ssZ] -->
					<xsl:variable name="dateString" select="$elements[$index]"/>
					
					<xsl:choose>
						<xsl:when test="string-length($dateString) &lt; 10">
							<xsl:value-of select="$elements[$index]" disable-output-escaping="yes"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:variable name="dateParser" select="java:java.text.SimpleDateFormat.new('yyyy-MM-dd')"/>
							<xsl:variable name="date" select="java:parse($dateParser, $dateString)"/>
							
							<xsl:variable name="locale" select="java:java.util.Locale.new('es')"/>
							<xsl:variable name="formatter" select="java:java.text.DateFormat.getDateInstance(1, $locale)"/>
		
							<xsl:value-of select="java:format($formatter, $date)"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:when>
				<xsl:when test="$type='i18n-code'">
					<i18n:text>xmlui.dri2xhtml.METS-1.0.code-value-<xsl:value-of select="$elements[$index]"/></i18n:text>
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
			</xsl:call-template>
		</xsl:if>
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
              <xsl:copy-of select="./node()"/>
              <xsl:if test="./@authority and ./@confidence">
                <xsl:call-template name="authorityConfidenceIcon">
                  <xsl:with-param name="confidence" select="./@confidence"/>
                </xsl:call-template>
              </xsl:if>
            </td>
                <td><xsl:value-of select="./@language"/></td>
            </tr>
    </xsl:template>

    <!--dont render the item-view-toggle automatically in the summary view, only when it get's called-->
    <xsl:template match="dri:p[contains(@rend , 'item-view-toggle') and
        (preceding-sibling::dri:referenceSet[@type = 'summaryView'] or following-sibling::dri:referenceSet[@type = 'summaryView'])]">
    </xsl:template>

    <!-- dont render the head on the item view page -->
    <xsl:template match="dri:div[@n='item-view']/dri:head" priority="5">
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
                     	<!--Do not sort any more bitstream order can be changed-->
                        <!--<xsl:sort data-type="number" select="boolean(./@ID=$primaryBitstream)" order="descending" />-->
                        <!--<xsl:sort select="mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>-->
                        <xsl:with-param name="context" select="$context"/>
                    </xsl:apply-templates>
                </xsl:otherwise>
            </xsl:choose>
        </div>
    </xsl:template>

    <xsl:template match="mets:file">
        <xsl:param name="context" select="."/>
        <div class="file-wrapper clearfix">
            <div class="thumbnail-wrapper">
                <a class="image-link">
                    <xsl:attribute name="href">
                        <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
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
<!--                         	style="height: {$thumbnail.maxheight}px;" -->
                            <img alt="Icon" src="{concat($theme-path, '/images/',$img_path)}"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </a>
            </div>
<!--             style="height: {$thumbnail.maxheight}px;" -->
            <div class="file-metadata">
                <!-- Display the contents of 'Description' only if bitstream contains a description -->
                <xsl:if test="mets:FLocat[@LOCTYPE='URL']/@xlink:label != ''">
                    <div>
		                <a class="image-link">
		                    <xsl:attribute name="href">
		                        <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
		                    </xsl:attribute>
	                        <span>
	                            <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:label"/>
	                        </span>
		                 </a>
                    </div>
                </xsl:if>
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

</xsl:stylesheet>
