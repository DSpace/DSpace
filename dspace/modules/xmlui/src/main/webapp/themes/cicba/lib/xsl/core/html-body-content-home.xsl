<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml" xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:confman="org.dspace.core.ConfigurationManager"
	xmlns="http://www.w3.org/1999/xhtml" exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc confman">

	<xsl:template name="buildHome">

		<div class="row">
			<xsl:for-each select="dri:div[@n='news']">
				<div id="welcome-panel">
					<h3>
						<xsl:copy-of select="dri:head" />
					</h3>
					<xsl:value-of select="dri:p" />
				</div>
			</xsl:for-each>
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
				TUbo de ensayo
			</div>
		</div>
		
	</xsl:template>

</xsl:stylesheet>