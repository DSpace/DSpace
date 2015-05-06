<?xml version="1.0" encoding="UTF-8"?>
<!-- The contents of this file are subject to the license and copyright detailed 
	in the LICENSE and NOTICE files at the root of the source tree and available 
	online at http://www.dspace.org/license/ -->
<!-- TODO: Describe this XSL file Author: Alexey Maslov
	modified for LINDAT/CLARIN
 -->

<xsl:stylesheet version="1.0" 
	xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/"
	xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns="http://www.w3.org/1999/xhtml"
	exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc">

	<xsl:import href="../dri2xhtml-alt/dri2xhtml.xsl" />
	<xsl:import href="lib/xsl/aspect/artifactbrowser/common.xsl" />	
	<xsl:import href="lib/xsl/core/global-variables.xsl" />
	<xsl:import href="lib/xsl/core/page-structure.xsl" />
	<xsl:import href="lib/xsl/core/navigation.xsl" />
	<xsl:import href="lib/xsl/core/elements.xsl" />
	<xsl:import href="lib/xsl/core/forms.xsl" />
	<xsl:import href="lib/xsl/core/common.xsl" />	
	<xsl:import href="lib/xsl/core/attribute-handlers.xsl" />	
	<xsl:import href="lib/xsl/core/utils.xsl" />	
	<xsl:import href="lib/xsl/aspect/general/choice-authority-control.xsl" />
	<xsl:import href="lib/xsl/aspect/administrative/administrative.xsl" />	
	<xsl:import href="lib/xsl/aspect/artifactbrowser/item-list.xsl" />
	<xsl:import href="lib/xsl/aspect/artifactbrowser/item-view.xsl" />	
	<xsl:import href="lib/xsl/aspect/artifactbrowser/community-list.xsl" />
	<xsl:import href="lib/xsl/aspect/artifactbrowser/community-view.xsl" />	
	<xsl:import href="lib/xsl/aspect/artifactbrowser/ORE.xsl"/>
    <xsl:import href="lib/xsl/aspect/artifactbrowser/COinS.xsl"/>
	<xsl:import href="lib/xsl/lindat/header.xsl" />
	<xsl:import href="lib/xsl/lindat/footer.xsl" />
	<xsl:output indent="yes" />

	<xsl:template match='//dri:list[@id="aspect.discovery.Navigation.list.discovery"]' />

	<xsl:template match="dri:div[@n='news']" />
	<xsl:template match="dri:options//dri:list[count(child::*)=0]" priority="5" />
	<xsl:template match="dri:options//dri:list[@n='discover']" />

	<xsl:template match="/dri:document/dri:options//dri:head" priority="10">
		<i18n:text>
			<xsl:value-of select="." />
		</i18n:text>
	</xsl:template>

	<xsl:template
		match="dri:div[@n='control-panel']//dri:list[not(@n='options' or @n='files')]"
		priority="5">
		<div class="well well-light">
			<xsl:apply-imports />
		</div>
	</xsl:template>
	
	<xsl:template match="dri:div[@n='masked-page-control']" priority="10" />
	
	<xsl:template match="dri:div[@n='search-controls']" priority="10">
		<div>
			<xsl:apply-templates />
		</div>
	</xsl:template>

	<xsl:template match="dri:list[@n='sort-options']" priority="10" >
		<ul class="dropdown-menu">
			<xsl:for-each select="dri:item">
				<xsl:if test="position()!=1">
					<li class="divider">&#160;</li>
				</xsl:if>
				<li>
					<h6 style="padding: 0 0 0 10px; margin: 0">
						<xsl:apply-templates select="./node()" />
					</h6>
				</li>
				<xsl:if test="following-sibling::*[1][self::dri:list]">
					<xsl:for-each select="following-sibling::*[1]/dri:item">
						<li>
							<xsl:if test="dri:xref[@rend='gear-option gear-option-selected'] or self::dri:item[@rend='gear-option gear-option-selected']">
								<xsl:attribute name="class">
									<xsl:text>disabled</xsl:text>
								</xsl:attribute>
							</xsl:if>
							<a>
								<xsl:if test="dri:xref[@rend='gear-option gear-option-selected'] or self::dri:item[@rend='gear-option gear-option-selected']">
									<i class="fa fa-check">&#160;</i>
								</xsl:if>
								<xsl:attribute name="href">
									<xsl:value-of select="dri:xref/@target" />
								</xsl:attribute>
								<xsl:apply-templates select="dri:xref/node()" />
							</a>
						</li>
					</xsl:for-each>
				</xsl:if>
			</xsl:for-each>
		</ul>
	</xsl:template>	

    <xsl:template match="dri:div[@id='cz.cuni.mff.ufal.LicensePage.div.licenses']" priority="10">

        <div class="well">
                        <xsl:apply-templates select="dri:head" />
                        <xsl:for-each select="dri:list">
                                <div class="well well-sm well-white">
                                        <h4>
                                        <a>
                                                <xsl:attribute name="href">
                                                        <xsl:value-of select="dri:item[@n='definition']/node()" />
                                                </xsl:attribute>
                                                <xsl:value-of select="dri:item[@n='name']/node()" />
                                        </a></h4>
                                        <xsl:apply-templates select="dri:list[@n='license_details']" />
                                </div>
                        </xsl:for-each>
        </div>

    </xsl:template>
</xsl:stylesheet>

