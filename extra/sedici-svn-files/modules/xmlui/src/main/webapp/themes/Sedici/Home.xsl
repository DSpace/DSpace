<?xml version="1.0" encoding="UTF-8"?>
<!--

    Copyright (C) 2011 SeDiCI <info@sedici.unlp.edu.ar>

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

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


    <xsl:import href="Sedici.xsl"/>
    <xsl:include href="slideshow/slideshow.xsl"/>
    
	<xsl:variable name="icon-row-size" select="3"/>
    
    <xsl:template match="dri:body">
    
       <div id="ds-body">
         <div id='home_slideshow'>
            <xsl:call-template name="slideshow"/>
         </div>
         
         <div id='home_search'>
	         <xsl:apply-templates select="dri:div[@n='front-page-search']" mode="home"/>
	     </div>
	     
	     <div id='home_info'>
	         <xsl:apply-templates select="dri:div[@n='news']/dri:p"/>
	     </div>
	     
	     <div id='home_main_communities'>
	     	 <h1 class="communities_header"><i18n:text>sedici.comunidades.header</i18n:text></h1>
	     	 <xsl:call-template name="render-community-section">
	     	 	<xsl:with-param name="elements" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='home-link']"/>
	     	 	<xsl:with-param name="elements-count" select="count(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='home-link'])"/>
	     	 </xsl:call-template>
	     </div>
	     
	     <div id='home_feed'>
	         <xsl:apply-templates select="dri:div[@id='ar.edu.unlp.sedici.aspect.news.ShowNews.div.feed']"/>
	     </div>
	     
	     <div id='home_envios_recientes'>
	         <h1><i18n:text><xsl:value-of select="dri:div[@n='site-home']/dri:div/dri:head"/></i18n:text></h1>
	         <ul class="ul_envios_recientes">
		       <xsl:for-each select="dri:div[@n='site-home']/dri:div/dri:referenceSet/dri:reference">
		                <li class='li_envios_recientes'>
		                   <xsl:apply-templates select='.' mode="home"/>
		                 </li>
		       </xsl:for-each>
		     </ul>
	         
	     </div>
	  </div>
         
   </xsl:template>
    
    
   <!-- mostramos los links del home basados en la propiedad de configuracion xmlui.community-list.home-links -->
   <xsl:template name="render-community-section">
   		<xsl:param name="elements"/>
   		<xsl:param name="elements-count"/>
   		<xsl:param name="element-index" select="1"/>
   		<xsl:param name="row-index" select="1"/>
   		
   		<!-- Chequea si corresponde crear una nueva fila -->
   		<xsl:if test="($row-index - 1) &lt; ($elements-count div $icon-row-size)">
	     	<div class="community_icon_row">
	     		<xsl:call-template name="render-community-icon-container">
	     			<xsl:with-param name="elements" select="$elements"/>
	     			<xsl:with-param name="elements-count" select="$elements-count"/>
	     			<xsl:with-param name="element-index" select="$element-index"/>
	     		</xsl:call-template>
	     	</div>
   		
	   		<xsl:call-template name="render-community-section">
	   			<xsl:with-param name="elements" select="$elements"/>
	   			<xsl:with-param name="elements-count" select="$elements-count"/>
	   			<xsl:with-param name="element-index" select="$element-index + $icon-row-size"/>
	   			<xsl:with-param name="row-index" select="$row-index + 1"/>
	   		</xsl:call-template>
		</xsl:if>
   </xsl:template>

	<xsl:template name="render-community-icon-container">
   		<xsl:param name="elements"/>
   		<xsl:param name="elements-count"/>
   		<xsl:param name="element-index"/>
   		<xsl:param name="local-index" select="1"/>
   		
   		<xsl:if test="(($local-index - 1) &lt; $icon-row-size) and (($element-index - 1) &lt; $elements-count)">
         	<xsl:variable name="slug" select="$elements[$element-index]/@qualifier"/>
         	<xsl:variable name="link" select="$elements[$element-index]"/>

	         <div class="community_icon_container">
	         	<xsl:attribute name="id">icono_<xsl:value-of select="$slug"/></xsl:attribute>
		 		<a>
		 		    <xsl:attribute name="href"><xsl:value-of select="$link"/></xsl:attribute>
			 		<h2><i18n:text>sedici.comunidades.<xsl:value-of select="$slug"/>.nombre</i18n:text></h2>
			 		<img class="community_icon">
			 			<xsl:attribute name="id">community_icon_<xsl:value-of select="$slug"/></xsl:attribute>
			            <xsl:attribute name="src">
			                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
			                <xsl:text>/themes/</xsl:text>
			                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
			                <xsl:text>/images/icono_</xsl:text><xsl:value-of select="$slug"/><xsl:text>.png</xsl:text>
			            </xsl:attribute>&#160;
			 		</img>
			 		<p><i18n:text>sedici.comunidades.<xsl:value-of select="$slug"/>.info</i18n:text></p>
		 		</a>
		 	 </div>
		 	 
	  		<xsl:call-template name="render-community-icon-container">
	  			<xsl:with-param name="elements" select="$elements"/>
	  			<xsl:with-param name="elements-count" select="$elements-count"/>
	  			<xsl:with-param name="element-index" select="$element-index + 1"/>
	  			<xsl:with-param name="local-index" select="$local-index + 1"/>
	  		</xsl:call-template>
		</xsl:if>
	</xsl:template>
    
   <xsl:template match="dri:div[@n='front-page-search']" mode="home">

        <form>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-interactive-div</xsl:with-param>
            </xsl:call-template>
            <xsl:attribute name="action"><xsl:value-of select="@action"/></xsl:attribute>
            <xsl:attribute name="method"><xsl:value-of select="@method"/></xsl:attribute>
            <xsl:if test="@method='multipart'">
                <xsl:attribute name="method">post</xsl:attribute>
                <xsl:attribute name="enctype">multipart/form-data</xsl:attribute>
            </xsl:if>
            <xsl:attribute name="onsubmit">javascript:tSubmit(this);</xsl:attribute>
                        <!--For Item Submission process, disable ability to submit a form by pressing 'Enter'-->
                        <xsl:if test="starts-with(@n,'submit')">
                                <xsl:attribute name="onkeydown">javascript:return disableEnterKey(event);</xsl:attribute>
            </xsl:if>
            <xsl:apply-templates select="dri:p[2]">
	                     <xsl:with-param name="muestra">true</xsl:with-param>
	         </xsl:apply-templates>          
          
        </form>
            
    </xsl:template>
    
    
   <xsl:template match='dri:reference' mode='home'>
     <xsl:call-template name="envio_reciente">
            <xsl:with-param name="url">
               <xsl:value-of select="@url"/>
            </xsl:with-param>
     </xsl:call-template>
   </xsl:template>
     
   <xsl:template name="envio_reciente">
       <xsl:param name="url"/>
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="$url"/>
            <!-- No options selected, render the full METS document -->
        </xsl:variable>
        <xsl:comment> External Metadata URL: <xsl:value-of select="$externalMetadataURL"/> </xsl:comment>
        <xsl:apply-templates select="document($externalMetadataURL)" mode="home"/>


   </xsl:template>


    
    <xsl:template match='mets:METS' mode='home'>
       <a>
          <xsl:attribute name="href">
          	<xsl:value-of select="@OBJID"/>
          </xsl:attribute>
               <xsl:value-of select="mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='title']" disable-output-escaping="yes"/>
       </a> 
    </xsl:template>

    <xsl:output indent="yes"/>
    

</xsl:stylesheet>