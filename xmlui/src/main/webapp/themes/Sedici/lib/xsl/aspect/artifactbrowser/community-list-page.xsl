<!--
	Muestra la community-list para el request /community-list.
	Difiere de cualquier otra que tiene que tener un menu para desplegarse.
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
    xmlns:util="org.dspace.app.xmlui.utils.XSLUtils"
    xmlns:confman="org.dspace.core.ConfigurationManager"
    exclude-result-prefixes="xalan encoder i18n dri mets dim xlink xsl util confman">

    <xsl:output indent="yes"/>
    
    <!-- creo la variable con las comunidades que pueden ser desplegables -->
    <xsl:variable name="communities-desplegables">
        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='dropdown-communities']"></xsl:value-of>
    </xsl:variable>
   
    <!-- $autoarchiveId es el id de la coleccion de autoarchivo, encerrado entre '|' -->
    <xsl:variable name="autoarchiveId">
        <xsl:value-of select="concat('|', substring-after(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='autoArchive'][@qualifier='handle'], '/'), '|')"></xsl:value-of>
    </xsl:variable> 
    
    <xsl:template match="dri:div[@id='aspect.artifactbrowser.CommunityBrowser.div.comunity-browser']">
		 <xsl:apply-templates mode='community-list-page'>
		   <!-- Mando este parametro para definir si es comunidad base o no -->
		   <xsl:with-param name="inicio">true</xsl:with-param>
		 </xsl:apply-templates>
    </xsl:template>
    
    <xsl:template match="dri:head" mode='community-list-page'>
       <h1 class="ds-div-head"><i18n:text><xsl:value-of select="."/></i18n:text></h1>     
    </xsl:template>
    
    <xsl:template match="dri:p" mode='community-list-page'>
       <p class="ds-paragraph"><i18n:text><xsl:value-of select="."/></i18n:text></p>       
    </xsl:template>
    
    
    <!-- Matching con los elementos <referenceSet> -->
    <xsl:template match="dri:referenceSet[@type = 'summaryList']" priority="2" mode='community-list-page'>
        <xsl:param name="inicio">false</xsl:param>
        
        <xsl:apply-templates select="dri:head"/>

		<!-- Decide si tiene que armar los tabs o una lista -->        
        <xsl:choose>
            <xsl:when test="$inicio = 'true'">
		    	<div id="community-tabs">
	                <ul>
	                    <xsl:call-template name="community-list-page-tabs">
	                    	<xsl:with-param name="data" select="*[not(name()='head')]"/>
	                    </xsl:call-template>
	                </ul>
					<xsl:call-template name="community-list-page-content">
						<xsl:with-param name="data" select="*[not(name()='head')]"/>
					</xsl:call-template>
                </div>
            </xsl:when>
            <xsl:otherwise>
                <ul>
                    <xsl:apply-templates select="*[not(name()='head')]" mode="community-list-page"/>
                </ul>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <!-- creacion de los tabs a partir de la configuracion de home-links: en el mismo orden -->
    <xsl:template name="community-list-page-tabs">
    	<xsl:param name="data"/>
    	
    	<xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='home-link']">
    		<xsl:variable name="community-selector">/metadata/handle/<xsl:value-of select="."/>/mets.xml</xsl:variable>
    		<xsl:call-template name="render-tab-header">
    			<xsl:with-param name="configData" select="."/>
    			<xsl:with-param name="element" select="$data[@url=$community-selector]"/>
    		</xsl:call-template>
    	</xsl:for-each>
    </xsl:template>
    
	<xsl:template name="render-tab-header">
		<xsl:param name="configData"/>
		<xsl:param name="element"/>
	
        <xsl:variable name="id" select="translate((translate(substring-after($element/@url,'/metadata/handle/'),'/','-')),'-mets.xml','')"/>
        
        <li id="tab-{$id}">
        	<xsl:attribute name="class"> ds-artifact-item tab 
        		<xsl:value-of select="$configData/@qualifier"></xsl:value-of>
        	</xsl:attribute>
			<a>
				<xsl:attribute name="href">#content-<xsl:value-of select="$id"/></xsl:attribute>
				<i18n:text>sedici.comunidades.<xsl:value-of select="$configData/@qualifier"/>.nombre</i18n:text>
			</a>
        </li>
	</xsl:template>
    
    <!-- matching con los elementos <reference> para la creacion de los divs con el contenido para los tabs -->
    <xsl:template name="community-list-page-content">
    	<xsl:param name="data"/>
    	
    	<xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='home-link']">
	        <xsl:variable name="id" select="translate(.,'/','')"/>
    		<xsl:variable name="community-selector">/metadata/handle/<xsl:value-of select="."/>/mets.xml</xsl:variable>

	        <div id="content-{$id}">
	        	<xsl:attribute name="class"><xsl:value-of select="@qualifier"></xsl:value-of></xsl:attribute>
	        
	        	<div class="tab-info">
	        		<i18n:text>sedici.comunidades.<xsl:value-of select="@qualifier"/>.info</i18n:text>
	        	</div>
	           	<xsl:apply-templates select="$data[@url=$community-selector]/*" mode='community-list-page'/>
	        </div>

    	</xsl:for-each>
    </xsl:template>
    
    
    <!-- Then we resolve the reference tag to an external mets object -->
    <xsl:template match="dri:reference" mode="community-list-page">
        <xsl:variable name="id" select="translate((translate(substring-after(@url,'/metadata/handle/'),'/','-')),'-mets.xml','')"/>
        
        <!-- $communityId es el id de la comunidad actual, encerrado entre '|' -->
        <xsl:variable name="communityId" select="concat('|',substring-after(substring-before(substring-after(@url,'/metadata/handle/'),'/mets.xml'),'/'), '|')"/>
     
        <li id="li-{$id}">
            <xsl:attribute name="class">
                <xsl:text>ds-artifact-item </xsl:text>
                <xsl:choose>
                    <xsl:when test="position() mod 2 = 0">even</xsl:when>
                    <xsl:otherwise>odd</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            
            <!-- Si es una comunidad que se debe desplegar, muestro el boton para desplegar -->
            <xsl:if test="@type='DSpace Community'">               
               <xsl:if test="contains($communities-desplegables, $communityId)">
		           <div id='div-{$id}' class='div-boton-menu-desplegable'>
	        	   		<a href="javascript:llamar_alerta('div-{$id}','boton-{$id}','ocultar', 'ver');" type="button" class='ocultador' id='boton-{$id}' value="ver">+</a>
	        	   </div>
        	   </xsl:if>
            </xsl:if>
            
            <!-- Si la referencia es de la coleccion de autoarchivo no se debe mostrar -->
           <xsl:if test="not($communityId = $autoarchiveId)">   
           		<xsl:call-template name="render-item-name">
           			<xsl:with-param name="url" select="@url"/>
           		</xsl:call-template>
           </xsl:if>
            
           <xsl:if test="contains($communities-desplegables, $communityId)">
           		<xsl:apply-templates mode='community-list-page'/>
           </xsl:if>
        </li>
    </xsl:template>
    
    
    <!-- entra al mets y genera el toda la estructura de divs, spans y anchor de DSpace para la comunidad/coleccion -->
    <xsl:template name="render-item-name">
		<xsl:param name="url"/>
		
		<xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="$url"/>
            <!-- Since this is a summary only grab the descriptive metadata, and the thumbnails -->
            <xsl:text>?sections=dmdSec,fileSec&amp;fileGrpTypes=THUMBNAIL</xsl:text>
        </xsl:variable>
		
		<xsl:apply-templates select="document($externalMetadataURL)" mode="community-list-page"/>
		
    </xsl:template>
    
    <!-- entra al mets y genera solo el nombre de la comunidad/coleccion -->
    <xsl:template name="render-item-name-only">
		<xsl:param name="url"/>
		
		<xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="$url"/>
            <!-- Since this is a summary only grab the descriptive metadata, and the thumbnails -->
            <xsl:text>?sections=dmdSec,fileSec&amp;fileGrpTypes=THUMBNAIL</xsl:text>
        </xsl:variable>
		
		<xsl:apply-templates select="document($externalMetadataURL)" mode="community-list-page-name-only"/>
		
    </xsl:template>

    
    
   <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']]" mode="community-list-page">
   		<xsl:variable name="item-type">
	        <xsl:choose>
	            <xsl:when test="@LABEL='DSpace Collection'">collection</xsl:when>
	            <xsl:otherwise>community</xsl:otherwise>                
	        </xsl:choose>
        </xsl:variable>

        <div>
        	<xsl:attribute name="class">artifact-description-<xsl:value-of select="$item-type"/></xsl:attribute>
            <div class="artifact-title">
                <a href="{@OBJID}">
                	<xsl:apply-templates select="." mode="community-list-page-name-only"/>
                </a>
            </div>
        </div>
    </xsl:template>
    
   <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']]" mode="community-list-page-name-only">
        <xsl:variable name="data" select="./mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim"/>
        <span class="Z3988">                    
            <xsl:choose>
                <xsl:when test="string-length($data/dim:field[@element='title'][1]) &gt; 0">
                    <xsl:value-of select="$data/dim:field[@element='title'][1]"/>
                </xsl:when>
                <xsl:otherwise>
                    <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                </xsl:otherwise>
            </xsl:choose>
        </span>
    </xsl:template>

</xsl:stylesheet>