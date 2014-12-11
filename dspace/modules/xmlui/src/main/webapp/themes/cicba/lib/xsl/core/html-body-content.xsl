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
		<div id="cic-body">
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
				<xsl:when test="not(string($request-uri))">
					<xsl:call-template name="buildHome" />
				</xsl:when>
				<!-- Handler for Static pages -->
				
				<xsl:when test="starts-with($request-uri, 'page/')">
					<div class="row static-page">
						<xsl:copy-of select="document(concat('../../../',$request-uri,'.xhtml') )" />
					</div>
				</xsl:when>
				<!-- Otherwise use default handling of body -->
				<xsl:otherwise>
				<div class="row">
					<div class="col-md-9">
						<xsl:apply-templates />
					</div>
					<div class="col-md-3">
						<xsl:for-each select="/dri:document/dri:options/dri:list[@n='discovery']/dri:list">
							<xsl:call-template name="buildPanelFromList" />
						</xsl:for-each>
					</div>
				</div>
				</xsl:otherwise>
			</xsl:choose>

		</div>
	</xsl:template>
	
	
	<xsl:template name="buildHome">

		<div class="row">
			<div class="col-md-8" id="welcome-panel">
				<xsl:for-each select="dri:div[@n='news']">
					<h1>
						<xsl:copy-of select="dri:head" />
					</h1>
					<xsl:copy-of select="dri:p" />
				</xsl:for-each>
			</div>
			<div class="col-md-4">
				<xsl:call-template name="build-img">
					<xsl:with-param name="img.src">images/provincia.png</xsl:with-param>
				</xsl:call-template>
			</div>
		</div>
		<div id="home-highlight" class="row">
			<div id="home-highlight-img">
				<xsl:text> </xsl:text>
			</div>
			<div id="home-highlight-content">
				<form class="form-inline" role="form">
				   <label for="q">
				   		<i18n:text>xmlui.cicdigital.home.explore</i18n:text>
				   </label>
				    <div><input type="text" class="form-control" id="q" autofocus="true" size="30" placeholder="Ingrese su búsqueda ..."/>
				    <button type="submit" class="btn btn-link"><span class="glyphicon glyphicon-search" aria-hidden="true"></span></button>
				  </div>	
					<span class="badge">Ciencias Agrícolas</span>
					<span class="badge">Ciencias Médicas y de la Salud</span>
					<span class="badge">Ciencias Naturales y Exactas</span>
					<span class="badge">Ciencias Sociales</span>
					<span class="badge">Humanidades</span>
					<span class="badge">Ingenierías y Tecnologías</span>
				</form>
			</div>
		</div>
		<div class="row">
			<div class="col-md-1 col-md-offset-1" >
				<xsl:call-template name="build-img">
					<xsl:with-param name="img.src">images/flecha_subir.png</xsl:with-param>
				</xsl:call-template>
			</div>
			<div class="col-md-4" style="font-size:23pt" >
				<i18n:text>xmlui.cicdigital.home.subir-material</i18n:text>				
			</div>
			<div class="col-md-6" >
				<i18n:text>xmlui.cicdigital.home.subir-material-descripcion</i18n:text>
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
					
<!-- 					<xsl:apply-templates /> -->
			</xsl:for-each>
			</div>
			<div class="col-md-5">
				<xsl:call-template name="build-img">
					<xsl:with-param name="img.src">images/generica_72.png</xsl:with-param>
				</xsl:call-template>
			</div>
		</div>
		
	</xsl:template>
</xsl:stylesheet>