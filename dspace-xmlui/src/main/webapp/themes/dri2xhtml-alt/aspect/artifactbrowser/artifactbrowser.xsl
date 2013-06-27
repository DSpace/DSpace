<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<!--
    Starting point of the artifactbrowser transformation.
    This xsl references all artifactbrowser related dependencies.

    Author: art.lowel at atmire.com
    Author: lieven.droogmans at atmire.com
    Author: ben at atmire.com
    Author: Alexey Maslov

-->

<xsl:stylesheet
    xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
    xmlns:dri="http://di.tamu.edu/DRI/1.0/"
    xmlns:mets="http://www.loc.gov/METS/"
    xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
    xmlns:xlink="http://www.w3.org/TR/xlink/"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:atom="http://www.w3.org/2005/Atom"
    xmlns:ore="http://www.openarchives.org/ore/terms/"
    xmlns:oreatom="http://www.openarchives.org/ore/atom/"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:xalan="http://xml.apache.org/xalan"
    xmlns:encoder="xalan://java.net.URLEncoder"
    exclude-result-prefixes="xalan encoder i18n dri mets dim  xlink xsl">

    <xsl:import href="common.xsl"/>
    <xsl:import href="item-list.xsl"/>
    <xsl:import href="collection-list.xsl"/>
    <xsl:import href="community-list.xsl"/>
    <xsl:import href="item-view.xsl"/>
    <xsl:import href="collection-view.xsl"/>
    <xsl:import href="community-view.xsl"/>
    <xsl:import href="ORE.xsl"/>
    <xsl:import href="COinS.xsl"/>
    <xsl:import href="discovery.xsl"/>

    <xsl:output indent="yes"/>


</xsl:stylesheet>
