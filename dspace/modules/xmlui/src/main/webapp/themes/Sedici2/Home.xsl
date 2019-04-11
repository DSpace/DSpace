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

    <xsl:import href="Sedici2.xsl"/>

    <xsl:output indent="yes"/>

    <xsl:include href="slideshow/slideshow.xsl"/>
    
	<xsl:variable name="icon-row-size" select="3"/>
    
    <!-- Columna central -->
    <xsl:template match="dri:body">
       <div id="ds-body" class="home">

		<!-- Agregamos el slideshow -->
<!--          <div id='home_slideshow'> -->
<!--             <xsl:call-template name="slideshow"/> -->
<!--          </div> -->

		<!-- Por el momento mostramos una imagen estatica que simula ser el slideshow -->
	     <div id='home_info'>
	         <xsl:apply-templates select="dri:div[@n='news']/dri:p"/>
	     </div>

		<div id="home_slideshow" class="html_content">
			<div class="html_slide">
				<div id="slide_recursos" class="html_slide_item">
					<h2><i18n:text>sedici.home.slide_resources.title</i18n:text></h2>
					<p><i18n:text>sedici.home.slide_resources.info</i18n:text></p>
				</div>
				<div id="slide_visibilidad" class="html_slide_item">
					<h2><i18n:text>sedici.home.slide_work_visibility.title</i18n:text></h2>
					<p><i18n:text>sedici.home.slide_work_visibility.info</i18n:text></p>
				</div>
				<div id="slide_preservacion" class="html_slide_item">
					<h2><i18n:text>sedici.home.slide_digital_preservation.title</i18n:text></h2>
					<p><i18n:text>sedici.home.slide_digital_preservation.info</i18n:text></p>
				</div>
				<div id="slide_contacto" class="html_slide_item">
					<h2><i18n:text>sedici.home.slide_pairs_contact.title</i18n:text></h2>
					<p><i18n:text>sedici.home.slide_pairs_contact.info</i18n:text></p>
				</div>
			</div>
		</div>
     
        <div id="home_obligacion_deposito_button">
            <a title="Acceder a las políticas de obligación de depósito.">
                <xsl:attribute name="href">
                    <xsl:value-of select="//dri:pageMeta/dri:metadata[@element='contextPath']"/>
                    <xsl:text>handle/10915/74049</xsl:text>
                </xsl:attribute>
                <img>
                    <xsl:attribute name="src">
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                        <xsl:text>/themes/</xsl:text>
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                        <xsl:text>/images/home_obligacion_deposito_button.png</xsl:text>
                    </xsl:attribute>&#160;
                </img>
            </a>
        </div>

        <div id="home_pagina_estadisticas_button">
            <a title="Acceder a las estadísticas de contenidos en el repositorio.">
                <xsl:attribute name="href">
                    <xsl:value-of select="//dri:pageMeta/dri:metadata[@element='contextPath']"/>
                    <xsl:text>/pages/estadisticasContenidoRepositorio</xsl:text>
                </xsl:attribute>
                <img>
                    <xsl:attribute name="src">
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                        <xsl:text>/themes/</xsl:text>
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                        <xsl:text>/images/home_statistics_button.png</xsl:text>
                    </xsl:attribute>&#160;
                </img>
            </a>
        </div>

	     <div id='home_feed'>
	         <xsl:apply-templates select="dri:div[@id='ar.edu.unlp.sedici.aspect.news.ShowNews.div.feed']"/>
	     </div>
	     
	     <div id='home_envios_recientes'>
	         <h1><i18n:text><xsl:value-of select="dri:div[@n='site-home']/dri:div/dri:head"/></i18n:text></h1>
	         <xsl:call-template name="recent-submissions">
	         	<xsl:with-param name="references" select="dri:div[@n='site-home']/dri:div/dri:referenceSet/dri:reference"/>
	         </xsl:call-template>
	     </div>
	  </div>
   </xsl:template>
    
   <!-- Desde el home no mostramos options directamente -->
   <xsl:template match="dri:options">
   </xsl:template>

   <!-- Columna izquierda con accesos principales -->
   <xsl:template name="buildLeftSection">
       <div id="ds-left-section">

			<xsl:call-template name="buildHomeSearch"/>
			
			<xsl:call-template name="buildHomeAutoarchivo"/>
				
       </div>
   </xsl:template>

   <!-- Columna derecha con info de usuario y accesos a comunidades principales -->
   <xsl:template name="buildRightSection">
   		<div id="ds-right-section">
			<xsl:call-template name="buildUserBox"/>
			<xsl:call-template name="buildCommunitiesBox"/>
		</div>
   </xsl:template>
    
   <xsl:template name="buildCommunitiesBox">
	   <div id='home_main_communities'>
	   	 <h1 class="communities_header"><i18n:text>sedici.comunidades.header</i18n:text></h1>
	   	 <xsl:call-template name="render-community-section">
	   	 	<xsl:with-param name="elements" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='home-link']"/>
	   	 </xsl:call-template>
	   </div>
   </xsl:template>
    
   <!-- mostramos los links del home basados en la propiedad de configuracion xmlui.community-list.home-links -->
   <xsl:template name="render-community-section">
   		<xsl:param name="elements"/>

		<xsl:for-each select="$elements">
         	<xsl:variable name="slug" select="@qualifier"/>
         	<xsl:variable name="link" select="."/>
		
	         <div class="community_icon_container">
	         	<xsl:attribute name="id">icono_<xsl:value-of select="$slug"/></xsl:attribute>
		 		<a>
		 		    <xsl:attribute name="href"><xsl:value-of select="$link"/></xsl:attribute>
			 		<h2><i18n:text>sedici.comunidades.<xsl:value-of select="$slug"/>.nombre</i18n:text></h2>
			 		<p><i18n:text>sedici.comunidades.<xsl:value-of select="$slug"/>.info</i18n:text></p>
		 		</a>
		 	 </div>
   		</xsl:for-each>
   
   </xsl:template>

</xsl:stylesheet>