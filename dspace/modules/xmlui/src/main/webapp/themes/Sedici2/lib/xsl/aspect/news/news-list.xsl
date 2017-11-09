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
    Rendering of a list of items (e.g. in a search or
    browse results page)

    Author: art.lowel at atmire.com
    Author: lieven.droogmans at atmire.com
    Author: ben at atmire.com
    Author: Alexey Maslov

-->

<xsl:stylesheet
    xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
    xmlns:dri="http://di.tamu.edu/DRI/1.0/"
    xmlns:mets="http://www.loc.gov/METS/"
    xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
    xmlns:xlink="http://www.w3.org/TR/xlink/"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:xalan="http://xml.apache.org/xalan"
    exclude-result-prefixes="xalan i18n dri mets dim  xlink xsl">

    <xsl:output indent="yes"/>

    <!--these templates are modfied to support the 2 different item list views that
    can be configured with the property 'xmlui.theme.mirage.item-list.emphasis' in dspace.cfg-->


    <!--handles the rendering of a single item in a list in file mode-->
    <xsl:template match="dri:div[@id='ar.edu.unlp.sedici.aspect.news.ShowNews.div.feed']">
      	<h1><xsl:copy-of select='dri:head'/></h1>
		
		<xsl:for-each select="dri:list[@id='ar.edu.unlp.sedici.aspect.news.ShowNews.list.news']">
		    <xsl:call-template name="noticias"/>
		</xsl:for-each>
		
		<a class="showall" href="http://sedici.unlp.edu.ar/blog/" target="_blank">
			<i18n:text>sedici.noticias.verTodas</i18n:text>
		</a>
    </xsl:template>
    
    <xsl:template name="noticias">
       <!-- recorro la lista de noticias -->
       <ul class="ul_noticias">
	       <xsl:for-each select='dri:list'>
               <li class='li_noticia_individual'>
                  <xsl:call-template name="noticia_individual"/> 
                </li>
	       </xsl:for-each>
       </ul>

    </xsl:template>
    
    <xsl:template name='noticia_individual'>
          <span class="news_date"><xsl:value-of select="dri:item[@n='fecha']"/></span>
    	  <a target="_blank">
             <xsl:attribute name="href">
                 <xsl:value-of select="dri:item[@n='link']/dri:xref/@target"/>
             </xsl:attribute>
             <xsl:value-of select="dri:item[@n='titulo']"/>
          </a>
         <!-- <p><xsl:value-of select="dri:item[@n='descripcion']" disable-output-escaping="yes"/></p> --> 
    </xsl:template>
    
</xsl:stylesheet>
