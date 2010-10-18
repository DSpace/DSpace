<?xml version="1.0" encoding="UTF-8"?>

<!--
  dri2xhtml.xsl

  Version: $Revision: 3705 $
 
  Date: $Date: 2009-04-11 17:02:24 +0000 (Sat, 11 Apr 2009) $
 
  Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
  Institute of Technology.  All rights reserved.
 
  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are
  met:
 
  - Redistributions of source code must retain the above copyright
  notice, this list of conditions and the following disclaimer.
 
  - Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.
 
  - Neither the name of the Hewlett-Packard Company nor the name of the
  Massachusetts Institute of Technology nor the names of their
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.
 
  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
  OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
  USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
  DAMAGE.
-->

<!--
    Starting point of the dri2xhtml transformation.
    This xsl references all dependencies.

    Author: art.lowel at atmire.com
    Author: lieven.droogmans at atmire.com
    Author: ben at atmire.com
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
    
    <xsl:import href="core/global-variables.xsl"/>
    <xsl:import href="core/page-structure.xsl"/>
    <xsl:import href="core/elements.xsl"/>
    <xsl:import href="core/navigation.xsl"/>
    <xsl:import href="core/forms.xsl"/>
    <xsl:import href="core/attribute-handlers.xsl"/>
    <xsl:import href="core/utils.xsl"/>
    <xsl:import href="aspect/general/choice-authority-control.xsl"/>
    <xsl:import href="aspect/administrative/harvesting.xsl"/>
    <xsl:import href="aspect/artifactbrowser/artifactbrowser.xsl"/>
    <xsl:output indent="yes"/>
    
    <!--Duplicate changes from the respective xsl to adjust them-->

</xsl:stylesheet>
