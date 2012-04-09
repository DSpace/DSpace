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
	     	<h1 class="communities_header">Navegue por nuestras colecciones</h1>
	         <div id="icono_facultades" class="community_icon_container">
		 		<a>
		 		    <xsl:attribute name="href">
		 		       handle/<xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='home-link'][@qualifier='UnidadesAcademicas']"/>
			 		</xsl:attribute>
			 		<img id="community_icon_facultades" class="community_icon">
			            <xsl:attribute name="src">
			                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
			                <xsl:text>/themes/</xsl:text>
			                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
			                <xsl:text>/images/4_unidades.png</xsl:text>
			            </xsl:attribute>&#160;
			 		</img>
			 		<h2>Unidades académicas</h2>
		 		</a>
		 	 </div>

	         <div id="icono_revistas" class="community_icon_container">
		 		<a>
		 		    <xsl:attribute name="href">
		 		    handle/<xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='home-link'][@qualifier='Revistas']"/>
			 		</xsl:attribute>
			 		<img id="community_icon_revistas" class="community_icon">
			            <xsl:attribute name="src">
			                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
			                <xsl:text>/themes/</xsl:text>
			                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
			                <xsl:text>/images/1_revistas.png</xsl:text>
			            </xsl:attribute>&#160;
			 		</img>
			 		<h2>Revistas</h2>
		 		</a>
		 	 </div>

	         <div id="icono_eventos" class="community_icon_container">
		 		<a>
		 		    <xsl:attribute name="href">
		 		        handle/<xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='home-link'][@qualifier='Eventos']"/>
			 		</xsl:attribute>
			 		<img id="community_icon_eventos" class="community_icon">
			            <xsl:attribute name="src">
			                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
			                <xsl:text>/themes/</xsl:text>
			                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
			                <xsl:text>/images/3_eventos.png</xsl:text>
			            </xsl:attribute>&#160;
			 		</img>
			 		<h2>Eventos</h2>
		 		</a>
		 	 </div>

	         <div id="icono_libros" class="community_icon_container">
		 		<a>
			 		<xsl:attribute name="href">
			 		handle/<xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='home-link'][@qualifier='Libros']"/>
			 		</xsl:attribute>
			 		<img id="community_icon_libros" class="community_icon">
			            <xsl:attribute name="src">
			                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
			                <xsl:text>/themes/</xsl:text>
			                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
			                <xsl:text>/images/2_libros.png</xsl:text>
			            </xsl:attribute>&#160;
			 		</img>
			 		<h2>Libros</h2>
		 		</a>
		 	 </div>
	         
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
               <xsl:value-of select="mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='title']"/>
       </a> 
    </xsl:template>

    <xsl:output indent="yes"/>
    

</xsl:stylesheet>