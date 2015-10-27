<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml" xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:confman="org.dspace.core.ConfigurationManager"
	xmlns="http://www.w3.org/1999/xhtml" exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc confman">

	<!-- The template to handle the dri:body element. It simply creates the 
		ds-body div and applies templates of the body's child elements (which consists 
		entirely of dri:div tags). -->
	<xsl:template match="dri:body">
		<div id="cic-body" class="row">
			<xsl:call-template name="buildTrail" />
			<!-- <div> -->
			<xsl:if
				test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='alert'][@qualifier='message']">
				<div id="ds-system-wide-alert">
					<p>
						<xsl:copy-of
							select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='alert'][@qualifier='message']/node()" />
					</p>
				</div>
			</xsl:if>


			<!-- Check for the custom pages -->
			<xsl:choose>
				<xsl:when test="not(string($request-uri)) and ($is-error-page = 'false')">
					<xsl:call-template name="buildHome" />
				</xsl:when>
				<!-- Handler for Static pages -->

				<xsl:when test="starts-with($request-uri, 'page/')">
					<div class="static-page">
						<xsl:copy-of select="document(concat('../../../',$request-uri,'.xhtml') )" />
					</div>
				</xsl:when>
				<!-- Si tenemos datos de discovery para mostrar, lo hacemos en un sidebar -->
				<xsl:when test="/dri:document/dri:options/dri:list[@n='discovery']/child::node()">
					<div class="row">
						<div class="col-md-9">
							<xsl:apply-templates />
						</div>
						<div class="col-md-3" id="cic-sidebar">
							<h3>
								<xsl:copy-of select="/dri:document/dri:options/dri:list[@n='discovery']/dri:head" />
							</h3>
							<xsl:for-each select="/dri:document/dri:options/dri:list[@n='discovery']/dri:list">
								<xsl:call-template name="buildPanelFromList" />
							</xsl:for-each>
						</div>
					</div>
				</xsl:when>
				<!-- Otherwise use default handling of body -->
				<xsl:otherwise>
					<div class="container-fluid">
						<xsl:apply-templates />
					</div>
				</xsl:otherwise>
			</xsl:choose>

			<!-- </div> -->
		</div>
	</xsl:template>


	<xsl:template name="buildHome">

		<div class="row">
			<div class="col-md-7 hidden-xs" id="welcome-panel">
				<div class="bs-callout bs-callout-info">
					<xsl:for-each select="dri:div[@n='news']">
						<!-- <h1><xsl:copy-of select="dri:head" /></h1> -->
						<xsl:copy-of select="dri:p" />
					</xsl:for-each>
				</div>
			</div>
			<div class="col-md-5 hidden-xs hidden-sm">
				<xsl:call-template name="build-img">
					<xsl:with-param name="img.src">images/decorativa.png</xsl:with-param>
				</xsl:call-template>
			</div>
		</div>
		<div id="home-highlight" class="row">
			<!-- <div id="home-highlight-img"> -->
			<!-- <xsl:text> </xsl:text> -->
			<!-- </div> -->
			<div id="home-highlight-content" class="col-md-7">
				<form id="home-search-form" class="form-inline" role="form">
					<xsl:attribute name="action"><xsl:value-of select="$search-url" /></xsl:attribute>
				   <label for="q">
				   		<i18n:text>xmlui.cicdigital.home.explore</i18n:text>
				   </label>
				    <div class="container">
				    	<div class="col-xs-10 col-sm-11">
					    	<input type="text" name="query" class="form-control" size="30" placeholder="Ingrese su búsqueda ..."/>
					    </div>
					    <div class="col-xs-2 col-sm-1" id="lupa-button">
						    <button type="submit" name="lr" class="btn btn-link"><span class="glyphicon glyphicon-search" aria-hidden="true"></span></button>
						</div>
					</div>
				</form>
				<div id="home-browse-bar">
					
					<div class="btn-group btn-group-justified" role="group"  aria-label="...">
						<a class="btn" role="button">
							<xsl:attribute name="href">
								<xsl:call-template name="print-path">
									<xsl:with-param name="path">/browse?type=author</xsl:with-param>
								</xsl:call-template>
							</xsl:attribute>
							<span class="hidden-xs"><i18n:text>xmlui.ArtifactBrowser.Navigation.head_browse</i18n:text></span><xsl:text> </xsl:text><i18n:text>xmlui.ArtifactBrowser.CollectionViewer.browse_authors</i18n:text>
						</a>
						<a class="btn" role="button">
							<xsl:attribute name="href">
								<xsl:call-template name="print-path">
									<xsl:with-param name="path">/handle/123456789/3</xsl:with-param>
								</xsl:call-template>
							</xsl:attribute>
							<span class="hidden-xs"><i18n:text>xmlui.ArtifactBrowser.Navigation.head_browse</i18n:text></span><xsl:text> </xsl:text><i18n:text>xmlui.cicdigital.home.centros</i18n:text>
						</a>
						<a class="btn" role="button">
							<xsl:attribute name="href">
								<xsl:call-template name="print-path">
									<xsl:with-param name="path">/discover</xsl:with-param>
								</xsl:call-template>
							</xsl:attribute>
							<i18n:text>xmlui.ArtifactBrowser.Navigation.head_all_of_dspace</i18n:text>
						</a>
					</div>
				</div>
			</div><!--
    		--><div id="home-autoarchivo" class="col-md-5">
<!-- 				<h3> -->
					<xsl:call-template name="build-anchor">
						<xsl:with-param name="a.href">/submissions</xsl:with-param>
						<xsl:with-param name="a.value">
							 <xsl:text> </xsl:text><i18n:text>xmlui.cicdigital.home.subir-material</i18n:text>
						</xsl:with-param>
						<xsl:with-param name="img.src">images/flecha_subir.png</xsl:with-param>
					</xsl:call-template>
<!-- 				</h3> -->
				<p>
					<i18n:text>xmlui.cicdigital.home.subir-material-descripcion</i18n:text>
				</p>
			</div>
		</div>
		<div class="row">
			<div class="col-md-7">
			<xsl:for-each select="dri:div[@n='site-home']/dri:div[@n='site-recent-submission']">
					<h3>
						<xsl:copy-of select="dri:head" />
					</h3>
					<xsl:apply-templates select="dri:referenceSet" />
					<xsl:call-template name="build-anchor">
						<xsl:with-param name="a.href"
							select="dri:p[@n='recent-submission-view-more']/dri:xref/@target" />
						<xsl:with-param name="a.value">
							<xsl:copy-of
								select="dri:p[@n='recent-submission-view-more']/dri:xref/node()" />
						</xsl:with-param>
					</xsl:call-template>

				</xsl:for-each>
			</div>
			
		</div>

	</xsl:template>

	<!-- The header (distinct from the HTML head element) contains the title, 
		subtitle, login box and various placeholders for header images -->
	<xsl:template name="buildTrail">

		<div class="row" id="cic-trail">
			<xsl:choose>
				<!-- Dynamic pages trail -->
				<xsl:when test="count(/dri:document/dri:meta/dri:pageMeta/dri:trail/@target) > 0">
					<ol class="breadcrumb">
						<xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:trail">
							<!-- Solo se imprimen los elementos con un target y se omite el primer 
								trail correspondiente al inicio -->
							<xsl:if test="@target and . != /dri:document/dri:meta/dri:pageMeta/dri:trail[1]">
								<li>
									<a>
										<xsl:attribute name="href"><xsl:value-of
											select="@target" /></xsl:attribute>
										<xsl:copy-of select="." />
									</a>
								</li>
							</xsl:if>
						</xsl:for-each>
					</ol>
				</xsl:when>
				<xsl:otherwise>
					<div id="invisible">a</div>
				</xsl:otherwise>
			</xsl:choose>
		</div>
	</xsl:template>
</xsl:stylesheet>
