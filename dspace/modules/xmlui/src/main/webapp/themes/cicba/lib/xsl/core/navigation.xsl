<!-- The contents of this file are subject to the license and copyright detailed 
	in the LICENSE and NOTICE files at the root of the source tree and available 
	online at http://www.dspace.org/license/ -->
<!-- Rendering specific to the navigation (options) Author: art.lowel at 
	atmire.com Author: lieven.droogmans at atmire.com Author: ben at atmire.com 
	Author: Alexey Maslov -->

<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml" xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns="http://www.w3.org/1999/xhtml"
	exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc">

	<xsl:output indent="yes" />

	<xsl:template name="buildPanelFromList">
		<!-- Solo imprimo el panel si tiene algun dato -->
		
			<div class="panel panel-info">
				<xsl:if test="dri:head">
					<div class="panel-heading">
						<h3 class="panel-title">
							<xsl:copy-of select="dri:head" />
						</h3>
					</div>
				</xsl:if>
				<xsl:if test="dri:list">
					<div class="panel-body">
						<xsl:for-each select="dri:list">
							<xsl:if test="dri:item or dri:list">
								<div class="panel panel-info">
									<div class="panel-heading">
										<h3 class="panel-title">
											<xsl:copy-of select="dri:head" />
										</h3>
									</div>

									<ul class="list-group">
										<xsl:for-each select="dri:item">
											<li class="list-group-item">
												<xsl:for-each select="dri:xref">
												<a>
													<xsl:attribute name="href"><xsl:value-of
														select="@target" /></xsl:attribute>
													<xsl:copy-of select="text()" />
												</a>
												</xsl:for-each>
												<xsl:value-of select="text()" />
											</li>
										</xsl:for-each>
									</ul>
								</div>
							</xsl:if>

						</xsl:for-each>
					</div>
				</xsl:if>

				<xsl:if test="count(dri:item) &gt; 0">
					<ul class="list-group">
						<xsl:for-each select="dri:item">
							<li class="list-group-item">
								<xsl:for-each select="dri:xref">
									<a>
										<xsl:attribute name="href"><xsl:value-of
											select="@target" /></xsl:attribute>
										<!-- <xsl:copy-of select="text()" /> -->
										<xsl:copy-of select="node()" />
									</a>
								</xsl:for-each>
								<xsl:value-of select="text()" />
							</li>
						</xsl:for-each>
					</ul>
				</xsl:if>
			</div>
	</xsl:template>

	<xsl:template match="dri:options/dri:list" priority="1">

	</xsl:template>

</xsl:stylesheet>
