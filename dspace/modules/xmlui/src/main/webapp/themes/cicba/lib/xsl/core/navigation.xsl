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

	<!-- The template to handle dri:options. Since it contains only dri:list 
		tags (which carry the actual information), the only things than need to be 
		done is creating the ds-options div and applying the templates inside it. 
		In fact, the only bit of real work this template does is add the search box, 
		which has to be handled specially in that it is not actually included in 
		the options div, and is instead built from metadata available under pageMeta. -->
	<xsl:template match="dri:options">
		<div class="col-md-3">
			<xsl:for-each select="dri:list[@n!='account' and @n!='browse'] ">
				<xsl:if test="dri:item or dri:list">
					<div class="panel panel-primary">
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
												<xsl:for-each select="dri:item/dri:xref">
													<li class="list-group-item">
														<a>
															<xsl:attribute name="href"><xsl:value-of
																select="@target" /></xsl:attribute>
															<xsl:copy-of select="." />
														</a>
													</li>
												</xsl:for-each>
											</ul>
										</div>
									</xsl:if>

								</xsl:for-each>
							</div>
						</xsl:if>

						<!-- List group -->
						<ul class="list-group">
							<xsl:for-each select="dri:item/dri:xref">
								<li class="list-group-item">
									<a>
										<xsl:attribute name="href"><xsl:value-of
											select="@target" /></xsl:attribute>
										<xsl:copy-of select="." />
									</a>
								</li>
							</xsl:for-each>
						</ul>
					</div>
				</xsl:if>
			</xsl:for-each>
		</div>

	</xsl:template>

	<!-- This template controls the display of the Discovery FACET'S box. Avoids 
		to show the box in the "Homepage" -->
	<xsl:template match="dri:list[@n='discovery']">
		<xsl:if
			test="(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='URI']/text())!= ''">
			<xsl:apply-templates select="dri:head" />
			<div id="aspect_discovery_Navigation_list_discovery" class="ds-option-set">
				<ul class="ds-options-list">
					<xsl:for-each select="dri:list">
						<li>
							<xsl:apply-templates select="." />
						</li>
					</xsl:for-each>
				</ul>
			</div>
		</xsl:if>
	</xsl:template>


	<!--give nested navigation list the class sublist -->
	<xsl:template match="dri:options/dri:list/dri:list"
		priority="3" mode="nested">
		<li>
			<xsl:apply-templates select="dri:head" mode="nested" />
			<ul class="ds-simple-list sublist">
				<xsl:apply-templates select="dri:item" mode="nested" />
			</ul>
		</li>
	</xsl:template>

	<!-- Quick patch to remove empty lists from options -->
	<xsl:template match="dri:options//dri:list[count(child::*)=0]"
		priority="5" mode="nested">
	</xsl:template>
	<xsl:template match="dri:options//dri:list[count(child::*)=0]"
		priority="5">
	</xsl:template>

</xsl:stylesheet>
