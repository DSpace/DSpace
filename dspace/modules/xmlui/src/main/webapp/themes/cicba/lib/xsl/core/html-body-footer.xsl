<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml" xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:confman="org.dspace.core.ConfigurationManager"
	xmlns="http://www.w3.org/1999/xhtml" exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc confman">


	<!-- Like the header, the footer contains various miscellaneous text, links, 
		and image placeholders -->
	<xsl:template name="buildFooter">
		<div class="row">
			<div id="ds-footer">
				<div id="ds-footer-left">
					<a href="http://www.dspace.org/" target="_blank">DSpace software</a>
					copyright&#160;&#169;&#160;2002-2012&#160;
					<a href="http://www.duraspace.org/" target="_blank">Duraspace</a>
				</div>
				<div id="ds-footer-right">
					<span class="theme-by">Theme by&#160;</span>
					<a title="@mire NV" target="_blank" href="http://atmire.com" id="ds-footer-logo-link">
						<span id="ds-footer-logo">&#160;</span>
					</a>
				</div>
				<div id="ds-footer-links">
					<a>
						<xsl:attribute name="href">
                            <xsl:value-of
							select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]" />
                            <xsl:text>/contact</xsl:text>
                        </xsl:attribute>
						<i18n:text>xmlui.dri2xhtml.structural.contact-link</i18n:text>
					</a>
					<xsl:text> | </xsl:text>
					<a>
						<xsl:attribute name="href">
                            <xsl:value-of
							select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]" />
                            <xsl:text>/feedback</xsl:text>
                        </xsl:attribute>
						<i18n:text>xmlui.dri2xhtml.structural.feedback-link</i18n:text>
					</a>
				</div>
				<!--Invisible link to HTML sitemap (for search engines) -->
				<a class="hidden">
					<xsl:attribute name="href">
                        <xsl:value-of
						select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]" />
                        <xsl:text>/htmlmap</xsl:text>
                    </xsl:attribute>
					<xsl:text>&#160;</xsl:text>
				</a>
			</div>
		</div>
	</xsl:template>
</xsl:stylesheet>