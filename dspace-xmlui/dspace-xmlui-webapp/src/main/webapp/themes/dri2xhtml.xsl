<?xml version="1.0" encoding="UTF-8"?>

<!--
    dri2xhtml.xsl
    
    Version: $Revision: 1.22 $
    
    Date: $Date: 2006/07/27 22:54:52 $
    
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
