
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
    
 
  <xsl:template match="dri:div[@n='search-filters']">
        <xsl:apply-templates select="dri:head"/>
        <xsl:apply-templates select="@pagination">
            <xsl:with-param name="position">top</xsl:with-param>
        </xsl:apply-templates>
        <script type="text/javascript">
             var filtros_activos=
              <xsl:choose>
            		<xsl:when test="dri:list/dri:item[@n='used-filters']">
            		   true
            		</xsl:when>
            		<xsl:otherwise>
            			false
            		</xsl:otherwise>
              </xsl:choose>;
             var ocultar_filtros_text='<i18n:text>xmlui.discovery.ocultar_filtros</i18n:text>';
             var ver_filtros_text='<i18n:text>xmlui.discovery.ver_filtros</i18n:text>(<xsl:value-of select="count(dri:list/dri:item[@n='used-filters']/dri:field)"/>)';
        </script>
		<xsl:variable name="filtro_texto">
	        	<xsl:choose>
	            		<xsl:when test="dri:list/dri:item[@n='used-filters']">	
							xmlui.discovery.ocultar_filtros
	            		</xsl:when>
	            		<xsl:otherwise>
	            		xmlui.discovery.ver_filtros(<xsl:value-of select="count(dri:list/dri:item[@n='used-filters']/dri:field)"/>)
	            		</xsl:otherwise>
	            </xsl:choose>
		</xsl:variable>
        <button id="input_mostrar_filtros" class="ds-button-field" onclick="javascript:mostrarFiltros()">
	            <xsl:choose>
	            		<xsl:when test="dri:list/dri:item[@n='used-filters']">	
							<i18n:text>xmlui.discovery.ocultar_filtros</i18n:text>
	            		</xsl:when>
	            		<xsl:otherwise>
	            		<i18n:text>xmlui.discovery.ver_filtros</i18n:text> (<xsl:value-of select="count(dri:list/dri:item[@n='used-filters']/dri:field)"/>)
	            		</xsl:otherwise>
	            </xsl:choose>
	     </button>

        <script type="text/javascript">
	        function mostrarFiltros(){
	            if (filtros_activos){
	            	$("#div-search-filters").hide('slow');
	            	filtros_activos=false;
	            		$('#input_mostrar_filtros').text(ver_filtros_text);
	            	
	            } else {            
	            	$("#div-search-filters").show('slow');
	            	filtros_activos=true;
	            	$('#input_mostrar_filtros').text(ocultar_filtros_text);
	            }
	        }
        </script>


        <div id="div-search-filters">
            <xsl:attribute name="style">
            	<xsl:choose>
            		<xsl:when test="dri:list/dri:item[@n='used-filters']">
            		   display:block
            		</xsl:when>
            		<xsl:otherwise>
            			display:none
            		</xsl:otherwise>
            	</xsl:choose>
            </xsl:attribute>
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
	                        <xsl:apply-templates select="*[not(name()='head')]"/>
	          
	        </form>
        </div>
        <!-- JS to scroll form to DIV parent of "Add" button if jump-to -->
        <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='page'][@qualifier='jumpTo']">
          <script type="text/javascript">
            <xsl:text>var button = document.getElementById('</xsl:text>
            <xsl:value-of select="translate(@id,'.','_')"/>
            <xsl:text>').elements['</xsl:text>
            <xsl:value-of select="concat('submit_',/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='page'][@qualifier='jumpTo'],'_add')"/>
            <xsl:text>'];</xsl:text>
            <xsl:text>
                      if (button != null) {
                        var n = button.parentNode;
                        for (; n != null; n = n.parentNode) {
                            if (n.tagName == 'DIV') {
                              n.scrollIntoView(false);
                              break;
                           }
                        }
                      }
            </xsl:text>
          </script>
        </xsl:if>
        <xsl:apply-templates select="@pagination">
            <xsl:with-param name="position">bottom</xsl:with-param>
        </xsl:apply-templates>
    </xsl:template>
    
</xsl:stylesheet>