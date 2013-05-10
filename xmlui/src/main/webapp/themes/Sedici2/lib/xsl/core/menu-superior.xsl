
<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/"
	xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:java="http://xml.apache.org/xalan/java"
    xmlns:confman="org.dspace.core.ConfigurationManager"
	xmlns="http://www.w3.org/1999/xhtml"
	exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc confman java">

    <xsl:output indent="yes"/>


   <xsl:template name="menuSuperior">
    <div id="topNav">
	    <ul id="ds-menu-superior">
			<li class="main">
				<a href="{$context-path}/"><i18n:text>sedici.menuSuperior.home</i18n:text></a>
			</li>
			<li class="main">
				<a href="{$context-path}/discover"><i18n:text>sedici.menuSuperior.buscar</i18n:text></a>
				<ul>
					<li><a href="{$context-path}/community-list"><i18n:text>sedici.menuSuperior.exploracion.colecciones</i18n:text></a></li>
					<li><a href="{$context-path}/browse?type=author&amp;rpp=60"><i18n:text>sedici.menuSuperior.exploracion.autor</i18n:text></a></li>
	        		<li><a href="{$context-path}/browse?type=subject"><i18n:text>sedici.menuSuperior.exploracion.tema</i18n:text></a></li>
			    </ul>
			</li>
			<li class="main">
				<xsl:choose>
					<xsl:when test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='autoArchive' and @qualifier='submit']='true'">
					<a>
						<xsl:attribute name="href">
							<xsl:value-of select="$context-path"/>
							<xsl:text>/handle/</xsl:text>
							<xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='autoArchive'][@qualifier='handle']"/>
							<xsl:text>/submit</xsl:text>
						</xsl:attribute>
						<i18n:text>sedici.menuSuperior.subirMaterial</i18n:text>
					</a>	
				</xsl:when>
				<xsl:otherwise>
  					<a href="{$context-path}/submit"><i18n:text>sedici.menuSuperior.subirMaterial</i18n:text></a>
  				</xsl:otherwise>
			  </xsl:choose> 
			</li>
<!-- 	        <li class="main"> -->
<!-- 	        	<a href="#"><i18n:text>sedici.menuSuperior.exploracion</i18n:text></a> -->
<!-- 				<ul> -->
<!-- 					<li><a href="{$context-path}/community-list"><i18n:text>sedici.menuSuperior.exploracion.colecciones</i18n:text></a></li> -->
<!-- 					<li><a href="{$context-path}/browse?type=dateissued"><i18n:text>sedici.menuSuperior.exploracion.fechaPublicacion</i18n:text></a></li> -->
<!-- 					<li><a href="{$context-path}/browse?type=author&amp;rpp=60"><i18n:text>sedici.menuSuperior.exploracion.autor</i18n:text></a></li> -->
<!-- 	        		<li><a href="{$context-path}/browse?type=title"><i18n:text>sedici.menuSuperior.exploracion.titulo</i18n:text></a></li> -->
<!-- 	        		<li><a href="{$context-path}/browse?type=subject"><i18n:text>sedici.menuSuperior.exploracion.tema</i18n:text></a></li> -->
<!-- 			    </ul> -->
<!-- 			</li>             -->
	        <li class="main">
	        	<a href="#"><i18n:text>sedici.menuSuperior.institucional</i18n:text></a>
				<ul>
					<li><a href="{$context-path}/pages/queEsSedici"><i18n:text>sedici.menuSuperior.institucional.queEsSedici</i18n:text></a></li>
					<li><a href="{$context-path}/pages/politicas"><i18n:text>sedici.menuSuperior.institucional.politica</i18n:text></a></li>
					<li><a href="{$context-path}/pages/links"><i18n:text>sedici.menuSuperior.institucional.links</i18n:text></a></li>
	        		<li><a href="{$context-path}/pages/staff"><i18n:text>sedici.menuSuperior.institucional.staff</i18n:text></a></li>
	        		<li><a href="{$context-path}/pages/comoLlegar"><i18n:text>sedici.menuSuperior.institucional.comoLlegar</i18n:text></a></li>
			    </ul>
			</li>
			<li class="main">
				<a href="#"><i18n:text>sedici.menuSuperior.preguntas</i18n:text></a>
				<ul>
				  <li><a href="{$context-path}/pages/comoAgregarTrabajos"><i18n:text>sedici.menuSuperior.informacion.agregacion</i18n:text></a></li>
				  <li><a href="{$context-path}/pages/informacionTesistas"><i18n:text>sedici.menuSuperior.informacion.infoTesistas</i18n:text></a></li>
				  <li><a href="{$context-path}/pages/revistasAccesoAbierto"> <i18n:text>sedici.menuSuperior.informacion.revistasAccesoAbierto</i18n:text></a></li>
				  <li><a href="{$context-path}/pages/FAQ"> <i18n:text>sedici.menuSuperior.informacion.faq</i18n:text></a></li>
				</ul>
			</li>
			<li class="main">
				<a href="#" title="Contáctese" data-uv-lightbox="classic_widget" data-uv-mode="full" data-uv-primary-color="#cc6d00" data-uv-link-color="#007dbf" data-uv-default-mode="support" data-uv-forum-id="150127"><i18n:text>sedici.menuSuperior.contacto</i18n:text></a>
			</li>

			<!-- Genero la seccion administrativa -->
			<xsl:if test="/dri:document/dri:options/dri:list[@id='aspect.viewArtifacts.Navigation.list.administrative']/*">
				<li class="main admin">
					<a href="#"><i18n:text>sedici.menuSuperior.administracion</i18n:text></a>
					<ul>
						<div id="admin-menu-main-options">
							<!-- Se muestran las opciones que pertenecen a un grupo -->
							<xsl:apply-templates select="/dri:document/dri:options/dri:list[@id='aspect.viewArtifacts.Navigation.list.administrative']/dri:list" mode="admin-menu"/>
	
			                <!-- Se muestran las opciones de estadísticas -->            
			                <xsl:if test="not(java:ar.edu.unlp.sedici.xmlui.xsl.XslExtensions.matches(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='URI'], 'handle/\d+/\d+/submit(.*)'))">
								<xsl:apply-templates select='/dri:document/dri:options/dri:list[@id="aspect.statistics.Navigation.list.statistics"]' mode="admin-menu"/> 
			                </xsl:if>
            			</div>
            			<!-- Se muestran las opciones que no pertenecen a ningun grupo -->
            			<div id="admin-menu-other-options">
							<li class="submenu"><i18n:text>sedici.menuSuperior.otrasOpciones</i18n:text></li>
							<xsl:apply-templates select="/dri:document/dri:options/dri:list[@id='aspect.viewArtifacts.Navigation.list.administrative']/dri:item" mode="admin-menu"/>
						</div>
					</ul>
				</li>
            </xsl:if>

			
	     </ul>
     </div>
    </xsl:template>
    
	<xsl:template match="dri:list" mode="admin-menu">
		<li class="submenu">
			<xsl:copy-of select="dri:head"/>
		</li>
		<xsl:apply-templates select="dri:item" mode="admin-menu"/>
	</xsl:template>
	
	<xsl:template match="dri:item" mode="admin-menu">
		<li>
			<xsl:apply-templates select="dri:xref"/>
		</li>
	</xsl:template>



</xsl:stylesheet>
