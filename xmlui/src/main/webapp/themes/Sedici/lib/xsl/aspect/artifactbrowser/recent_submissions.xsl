<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<!--
    Rendering specific to the navigation (options)

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

    <xsl:output indent="yes"/>

    <!-- Generacion del titulo para los envios recientes en las comunidades -->
    <xsl:template match="dri:div[@id='aspect.artifactbrowser.CommunityRecentSubmissions.div.community-recent-submission']/dri:head">
      <h2 class="ds-div-head">
      	<i18n:text><xsl:value-of select="."/></i18n:text>&#160;<xsl:value-of select="/dri:document/dri:body/dri:div/dri:head"/>
      </h2>
    </xsl:template>
    
    <!-- Generacion del titulo para los envios recientes en las colecciones -->
    <xsl:template match="dri:div[@id='aspect.artifactbrowser.CollectionRecentSubmissions.div.collection-recent-submission']/dri:head">
      <h2 class="ds-div-head">
      	<i18n:text><xsl:value-of select="."/></i18n:text>&#160;<xsl:value-of select="/dri:document/dri:body/dri:div/dri:head"/>
      </h2>
    </xsl:template>
    
    <!-- 
    Esto es para no mostrar las entradas del DRI del aspecto artifact browser que vienen con los archivos recientes en la vista de la comunidad.
    Se debe mostrar en blanco ya que el discovery las muestra por su parte.
    En caso de desactivar el discovery se debrá cambiar la muestra.
    -->
    <xsl:template match="dri:referenceSet[@id='aspect.artifactbrowser.CommunityRecentSubmissions.referenceSet.collection-last-submitted']">
		
    </xsl:template>
    
    <!-- 
      Esto es para no mostrar las entradas del DRI del aspecto artifact browser que vienen con los archivos recientes en la vista de la collection.
      Se debe mostrar en blanco ya que el discovery las muestra por su parte.
      En caso de desactivar el discovery se debrá cambiar la muestra.
    -->
    <xsl:template match="dri:referenceSet[@id='aspect.artifactbrowser.CollectionRecentSubmissions.referenceSet.collection-last-submitted']">
		
    </xsl:template>
    
          
     <!-- Los siguientes son templates para el manejo de la carga de un nuevo item.
          En caso de ser un administrador mantengo la misma semantica que dspace trae por defecto.
          En caso de ser una eperson la redirecciono directamente a la carga de un item en la col. autoarchivo.
      -->        
     <xsl:template match="dri:div[@id='aspect.submission.Submissions.div.start-submision']/dri:p/dri:xref">
 		<xsl:call-template name="submitDesicion">
 			<xsl:with-param name="linkText"><xsl:value-of select="."/></xsl:with-param>
 		    <xsl:with-param name="link"><xsl:value-of select="@target"/></xsl:with-param>
 		</xsl:call-template>
     </xsl:template>
     
     <xsl:template match="dri:div[@id='aspect.submission.Submissions.div.unfinished-submisions']/dri:p/dri:hi/dri:xref">
 		<xsl:call-template name="submitDesicion">
 			<xsl:with-param name="linkText"><xsl:value-of select="."/></xsl:with-param>
 		    <xsl:with-param name="link"><xsl:value-of select="@target"/></xsl:with-param>
 		</xsl:call-template>
     </xsl:template>
     
     <xsl:template name="submitDesicion">
	  <xsl:param name="linkText"/>
       <xsl:param name="link"/>    
       <xsl:choose>
	     <xsl:when test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='autoArchive'][@qualifier='submit'] = 'true'">
	        <xsl:call-template name="newSubmissionEPerson">
	           <xsl:with-param name="linkText"><xsl:value-of select="$linkText"/></xsl:with-param>
	        </xsl:call-template>
	     </xsl:when>
	     <xsl:otherwise>
	        <xsl:call-template name="newSubmissionAdministrator">
	           <xsl:with-param name="link"><xsl:value-of select="$link"/></xsl:with-param>
	           <xsl:with-param name="linkText"><xsl:value-of select="$linkText"/></xsl:with-param>
	        </xsl:call-template> 
	     </xsl:otherwise>
     </xsl:choose>    
     </xsl:template>
     
     
     
     <xsl:template name="newSubmissionEPerson">
       <xsl:param name="linkText"/>
       <a>
	       <xsl:attribute name="href">
	       <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath']"/>/handle/<xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='autoArchive'][@qualifier='handle']"/>/submit</xsl:attribute>
	       <i18n:text><xsl:value-of select="$linkText"/></i18n:text>
       </a>
     
     </xsl:template>
     
     <xsl:template name="newSubmissionAdministrator">
       <xsl:param name="linkText"/>
       <xsl:param name="link"/>
       <a>
	       <xsl:attribute name="href">
	       <xsl:value-of select="$link"/>
	       </xsl:attribute>
	       <i18n:text><xsl:value-of select="$linkText"/></i18n:text>
       </a>
     
     </xsl:template>

</xsl:stylesheet>
