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
		<div class="row" id="cic-footer">
			<div class="col-md-2">
				<xsl:call-template name="build-anchor">
					<xsl:with-param name="a.href">
						http://www.gba.gob.ar
					</xsl:with-param>
					<xsl:with-param name="img.src">
						images/marca_para_footer.png
					</xsl:with-param>
					<xsl:with-param name="img.alt">
						BA
					</xsl:with-param>
				</xsl:call-template>
			</div>
			<div class="col-md-2 ">
				<ul>
					<li>
						<xsl:call-template name="build-anchor">
							<xsl:with-param name="a.href">/</xsl:with-param>
							<xsl:with-param name="a.value">
								<i18n:text>xmlui.general.dspace_home</i18n:text>
							</xsl:with-param>
						</xsl:call-template>
					</li>
					<li>
						<xsl:call-template name="build-anchor">
							<xsl:with-param name="a.href">/community-list</xsl:with-param>
							<xsl:with-param name="a.value">
								<i18n:text catalogue="default">xmlui.ArtifactBrowser.Navigation.head_browse</i18n:text>
							</xsl:with-param>
						</xsl:call-template>
					</li>
					<li>
						<xsl:call-template name="build-anchor">
							<xsl:with-param name="a.href">/feedback</xsl:with-param>
							<xsl:with-param name="a.value">
								<i18n:text>xmlui.dri2xhtml.structural.contact-link</i18n:text>
							</xsl:with-param>
						</xsl:call-template>
					</li>
				</ul>
			</div>
			<ul class="col-md-2 cic-banners">
				
				<li><xsl:call-template name="build-anchor">
					<xsl:with-param name="a.href">
						<xsl:text>http://www.cic.gba.gob.ar</xsl:text>
					</xsl:with-param>
					<xsl:with-param name="img.src">
						images/logo_cic_footer.png
					</xsl:with-param>
					<xsl:with-param name="img.alt">
						CIC
					</xsl:with-param>
				</xsl:call-template></li>
				
				<li><xsl:call-template name="build-anchor">
					<xsl:with-param name="a.href">
						<xsl:text>http://sedici.unlp.edu.ar</xsl:text>
					</xsl:with-param>
					<xsl:with-param name="img.src">
						images/logo_sedici_footer.png
					</xsl:with-param>
					<xsl:with-param name="img.alt">
						SEDICI
					</xsl:with-param>
				</xsl:call-template></li>
				
				<li><xsl:call-template name="build-anchor">
					<xsl:with-param name="a.href">
						<xsl:text>http://www.dspace.org</xsl:text>
					</xsl:with-param>
					<xsl:with-param name="img.src">
						images/logo_dspace_footer.png
					</xsl:with-param>
					<xsl:with-param name="img.alt">
						DSpace
					</xsl:with-param>
				</xsl:call-template></li>
			</ul>
			<div class="col-md-5 col-md-offset-1">
				<address>
					<strong>Comisión de Investigaciones Científicas</strong>
					<br />
					Calle 526 entre 10 y 11
					<br />
					CP: 1900 - La Plata - Buenos Aires - Argentina
					<br />
					<a href="mailto:#">cic-digital@sedici.unlp.edu.ar</a>
					<br />
					<abbr title="Phone">Tel:</abbr>
					+54 (0221) 423 6696/6677 (int. 141) (CIC-DIGITAL)
					<br />
					<abbr title="Phone">Tel:</abbr>
					+54 (0221) 421-7374 / 482-3795 (CIC-Central)
				</address>
			</div>
		</div>
<!-- 		<div class="row" id="cic-footer"> -->
<!-- 			<div> -->
<!-- 				Este repositorio esta soportado por el software -->
<!-- 				<a href="http://www.dspace.org/" target="_blank">DSpace</a> -->
<!-- 			</div> -->
<!-- 		</div> -->
	</xsl:template>
</xsl:stylesheet>