<?xml version="1.0" encoding="UTF-8"?>
<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->

<!--
    TODO: Describe this XSL file
    Author: Alexey Maslov

-->

<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/"
	xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns="http://www.w3.org/1999/xhtml"
	exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc">

    <!--<xsl:import href="../dri2xhtml-alt/dri2xhtml.xsl"/>-->
    <xsl:import href="aspect/artifactbrowser/artifactbrowser.xsl"/>
    <xsl:import href="core/global-variables.xsl"/>
    <xsl:import href="core/elements.xsl"/>
    <xsl:import href="core/forms.xsl"/>
    <xsl:import href="core/page-structure.xsl"/>
    <xsl:import href="core/navigation.xsl"/>
    <xsl:import href="core/attribute-handlers.xsl"/>
    <xsl:import href="core/utils.xsl"/>
    <xsl:import href="aspect/general/choice-authority-control.xsl"/>
    <xsl:import href="aspect/general/vocabulary-support.xsl"/>
    <!--<xsl:import href="xsl/aspect/administrative/administrative.xsl"/>-->
    <xsl:import href="aspect/artifactbrowser/common.xsl"/>
    <xsl:import href="aspect/artifactbrowser/item-list.xsl"/>
    <xsl:import href="aspect/artifactbrowser/item-view.xsl"/>
    <xsl:import href="aspect/artifactbrowser/community-list.xsl"/>
    <xsl:import href="aspect/artifactbrowser/collection-list.xsl"/>
    <xsl:import href="aspect/artifactbrowser/browse.xsl"/>
    <xsl:import href="aspect/discovery/discovery.xsl"/>
    <xsl:import href="aspect/artifactbrowser/one-offs.xsl"/>
    <xsl:import href="aspect/submission/submission.xsl"/>
    <xsl:output indent="yes"/>


</xsl:stylesheet>
