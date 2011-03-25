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

<xsl:stylesheet 
    xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
    xmlns:dri="http://di.tamu.edu/DRI/1.0/"
    xmlns:mets="http://www.loc.gov/METS/"
    xmlns:mods="http://www.loc.gov/mods/v3"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:dim="http://www.dspace.org/xmlns/dspace/dim" 
    xmlns:xlink="http://www.w3.org/TR/xlink/"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    
    <xsl:import href="dri2xhtml/structural.xsl"/>
    <xsl:import href="dri2xhtml/DIM-Handler.xsl"/>
    <!--<xsl:import href="dri2xhtml/QDC-Handler.xsl"/>
        <xsl:import href="dri2xhtml/MODS-Handler.xsl"/>-->
    
    <xsl:import href="dri2xhtml/General-Handler.xsl"/>
    <xsl:output indent="yes"/>       
    
</xsl:stylesheet>
