<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml" xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:confman="org.dspace.core.ConfigurationManager"
	xmlns="http://www.w3.org/1999/xhtml" exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc confman">

	<xsl:import href="html-body-content-home.xsl"/>
	<!-- The template to handle the dri:body element. It simply creates the 
		ds-body div and applies templates of the body's child elements (which consists 
		entirely of dri:div tags). -->
	<xsl:template match="dri:body">
		<div class="col-md-9">
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
<!-- 				<xsl:when test="starts-with($request-uri, 'page/about')"> -->
<!-- 					<div> -->
<!-- 						<h1>About This Repository</h1> -->
<!-- 						<p>To add your own content to this page, edit -->
<!-- 							webapps/xmlui/themes/Mirage/lib/xsl/core/page-structure.xsl and -->
<!-- 							add your own content to the title, trail, and body. If you wish -->
<!-- 							to add additional pages, you -->
<!-- 							will need to create an additional -->
<!-- 							xsl:when block and match the -->
<!-- 							request-uri to whatever page -->
<!-- 							you are -->
<!-- 							adding. Currently, static pages created through altering XSL -->
<!-- 							are -->
<!-- 							only available -->
<!-- 							under the URI prefix of page/. -->
<!-- 						</p> -->
<!-- 					</div> -->
<!-- 				</xsl:when> -->
				<!-- Handler for Static pages -->
				<xsl:when test="starts-with($request-uri, 'page/')">
					<div class="static-page">
						<xsl:copy-of select="document(concat('../../../',$request-uri,'.xhtml') )" />
					</div>
				</xsl:when>
				<!-- Otherwise use default handling of body -->
				<xsl:otherwise>
					<xsl:apply-templates />
				</xsl:otherwise>
			</xsl:choose>

		</div>
	</xsl:template>
</xsl:stylesheet>