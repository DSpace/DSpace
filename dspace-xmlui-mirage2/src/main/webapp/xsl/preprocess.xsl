<?xml version="1.0" encoding="UTF-8"?>
<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->

<!--
    Author: Art Lowel (art at atmire dot com)

    The purpose of this file is to transform the DRI for some parts of
    DSpace into a format more suited for the theme xsls. This way the
    theme xsl files can stay cleaner, without having to change Java
    code and interfere with other themes

    e.g. this file can be used to add a class to a form field, without
    having to duplicate the entire form field template in the theme xsl
    Simply add it here to the rend attribute and let the default form
    field template handle the rest.
-->

<xsl:stylesheet
                xmlns="http://di.tamu.edu/DRI/1.0/"
                xmlns:dri="http://di.tamu.edu/DRI/1.0/"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
                exclude-result-prefixes="xsl dri i18n">

    <xsl:import href="preprocess/general.xsl"/>
    <xsl:import href="preprocess/admin.xsl"/>
    <xsl:import href="preprocess/discovery.xsl"/>
    <xsl:import href="preprocess/browse.xsl"/>
    <xsl:import href="preprocess/communitylist.xsl"/>
    <xsl:import href="preprocess/itemview.xsl"/>
    <xsl:import href="preprocess/navigation.xsl"/>
    <xsl:output indent="yes"/>

</xsl:stylesheet>
