
<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/"
	xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:confman="org.dspace.core.ConfigurationManager"
	xmlns="http://www.w3.org/1999/xhtml"
	exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc confman">

    <xsl:output indent="yes"/>

   <xsl:template name="menuSuperior">
    <div id="topNav">
	    <ul id="ds-menu-superior">
			<li class="main">
				<a href="{$context-path}/"><i18n:text>sedici.menuSuperior.home</i18n:text></a>
			</li>
			<li class="main">
				<a href="{$context-path}/discover"><i18n:text>sedici.menuSuperior.buscar</i18n:text></a>
			</li>
	        <li class="main">
	        	<a href="#"><i18n:text>sedici.menuSuperior.exploracion</i18n:text></a>
				<ul>
					<li><a href="{$context-path}/community-list"><i18n:text>sedici.menuSuperior.exploracion.colecciones</i18n:text></a></li>
					<li><a href="{$context-path}/browse?type=dateissued"><i18n:text>sedici.menuSuperior.exploracion.fechaPublicacion</i18n:text></a></li>
					<li><a href="{$context-path}/browse?type=author&amp;rpp=60"><i18n:text>sedici.menuSuperior.exploracion.autor</i18n:text></a></li>
	        		<li><a href="{$context-path}/browse?type=title"><i18n:text>sedici.menuSuperior.exploracion.titulo</i18n:text></a></li>
	        		<li><a href="{$context-path}/browse?type=subject"><i18n:text>sedici.menuSuperior.exploracion.tema</i18n:text></a></li>
			    </ul>
			</li>            
	        <li class="main">
	        	<a href="#"><i18n:text>sedici.menuSuperior.institucional</i18n:text></a>
				<ul>
					<li><a href="{$context-path}/pages/queEsSedici"><i18n:text>sedici.menuSuperior.institucional.queEsSedici</i18n:text></a></li>
					<li><a href="{$context-path}/pages/politica"><i18n:text>sedici.menuSuperior.institucional.politica</i18n:text></a></li>
					<li><a href="{$context-path}/pages/links"><i18n:text>sedici.menuSuperior.institucional.links</i18n:text></a></li>
	        		<li><a href="{$context-path}/pages/staff"><i18n:text>sedici.menuSuperior.institucional.staff</i18n:text></a></li>
	        		<li><a href="{$context-path}/pages/comoLlegar"><i18n:text>sedici.menuSuperior.institucional.comoLlegar</i18n:text></a></li>
			    </ul>
			</li>
			<li class="main">
				<a href="#"><i18n:text>sedici.menuSuperior.informacion</i18n:text></a>
				<ul>
				  <li><a href="{$context-path}/pages/comoAgregarTrabajos"><i18n:text>sedici.menuSuperior.informacion.agregacion</i18n:text></a></li>
				  <li><a href="{$context-path}/pages/informacionTesistas"><i18n:text>sedici.menuSuperior.informacion.infoTesistas</i18n:text></a></li>
				  <li><a href="{$context-path}/pages/FAQ"> <i18n:text>sedici.menuSuperior.informacion.faq</i18n:text></a></li>
				</ul>
			
			</li>
			<li class="main">
				<a href="{$context-path}/feedback"><i18n:text>sedici.menuSuperior.contacto</i18n:text></a>
			</li>
	     </ul>
     </div>

    </xsl:template>
    
   
</xsl:stylesheet>