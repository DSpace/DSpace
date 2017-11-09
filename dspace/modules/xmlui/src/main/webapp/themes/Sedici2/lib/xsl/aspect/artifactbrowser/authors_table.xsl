<xsl:stylesheet
    xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
    xmlns:dri="http://di.tamu.edu/DRI/1.0/"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

    <xsl:output indent="yes"/>

	<!-- Capturamos las tablas que queremos presentar en columnas -->

	<xsl:template match="dri:table[@id='aspect.discovery.SearchFacetFilter.table.browse-by-subject-results']">
		<xsl:apply-templates select="." mode="multiple_column_browse"/>
	</xsl:template>

	<xsl:template match="dri:table[@id='aspect.discovery.SearchFacetFilter.table.browse-by-keywords-results']">
		<xsl:apply-templates select="." mode="multiple_column_browse"/>
	</xsl:template>

	<xsl:template match="dri:table[@id='aspect.discovery.SearchFacetFilter.table.browse-by-type-results']">
		<xsl:apply-templates select="." mode="multiple_column_browse"/>
	</xsl:template>

	<xsl:template match="dri:table[@id='aspect.discovery.SearchFacetFilter.table.browse-by-author-results']">
		<xsl:apply-templates select="." mode="multiple_column_browse"/>
	</xsl:template>

	<xsl:template match="dri:table[@id='aspect.artifactbrowser.ConfigurableBrowse.table.browse-by-subject-results']">
		<xsl:apply-templates select="." mode="multiple_column_browse"/>
	</xsl:template>

	<xsl:template match="dri:table[@id='aspect.artifactbrowser.ConfigurableBrowse.table.browse-by-author-results']">
		<xsl:apply-templates select="." mode="multiple_column_browse"/>
	</xsl:template>


   <!-- Generamos las columnas -->
   <xsl:template match="dri:table" mode="multiple_column_browse">
	 <!-- Recupero la cantidad de filas a mostrar en la pagina -->
     <xsl:variable name="itemsTotal">
		<xsl:value-of select="count(dri:row[not(@role)])"/>
     </xsl:variable>
	
     <!-- Calculo la cantidad de columnas y filas por columna -->
	 <xsl:variable name="cantItemsXCol">
          <xsl:choose>
	          <xsl:when test="$itemsTotal>60">
	              <xsl:value-of select="ceiling($itemsTotal div 3)"/>
	          </xsl:when>
	          <xsl:otherwise>
	                20
	          </xsl:otherwise>
          </xsl:choose>
     </xsl:variable>
     
     <!-- Si hay mas de un elemento debo mostrar por lo menos el primer contenedor -->
     <xsl:if test="$itemsTotal>=1">
	     <xsl:call-template name="div_column_browse">
	     	<xsl:with-param name="contador">0</xsl:with-param>
	     	<xsl:with-param name="cantItemsXCol" select="$cantItemsXCol"/>
	     </xsl:call-template>
	 </xsl:if>

     <!-- Si hay mas de la cantidad permitida por columna, genero el segundo contenedor -->
     <xsl:if test="$itemsTotal > $cantItemsXCol">
	     <xsl:call-template name="div_column_browse">
	     	<xsl:with-param name="contador">1</xsl:with-param>
	     	<xsl:with-param name="cantItemsXCol" select="$cantItemsXCol"/>
	     </xsl:call-template>
      </xsl:if>
	 
	 <!-- Si hay mas del doble de la cantidad permitida por columna, genero el tercer contenedor -->
	   <xsl:if test="$itemsTotal > ($cantItemsXCol*2)">
	     <xsl:call-template name="div_column_browse">
	     	<xsl:with-param name="contador">2</xsl:with-param>
	     	<xsl:with-param name="cantItemsXCol" select="$cantItemsXCol"/>
	     </xsl:call-template>
      </xsl:if>

	</xsl:template>
	
	<!-- No hago nada porque lo controlo desde el otro lado -->
    <xsl:template name="div_column_browse">
         <xsl:param name="contador"/>
         <xsl:param name="cantItemsXCol"/>
         
         <div class="browse_column">
		  <table>
		  		<!-- El header no se genera por pedido del diseñador -->
<!-- 		     <xsl:call-template name="header_browse_columns"> -->
<!-- 		     	<xsl:with-param name="header"><xsl:value-of select="dri:row[@role='header']"/></xsl:with-param> -->
<!-- 		     </xsl:call-template> -->
	         <xsl:apply-templates select="dri:row[floor((position()-1) div ($cantItemsXCol)) = $contador]"  mode="table_browse_columns"/>
		  </table>
	     </div>
    </xsl:template>
	
    <!-- Imprime el header de la tabla -->
    <xsl:template name="header_browse_columns">
	    <xsl:param name="header"/>
	    <xsl:if test="$header">
		    <tr>
		          <xsl:call-template name="standardAttributes">
		              <xsl:with-param name="class">ds-table-header-row</xsl:with-param>
		          </xsl:call-template>
		          <th>
		           <xsl:call-template name="standardAttributes">
		               <xsl:with-param name="class">ds-table-header-cell
		                   <xsl:if test="(position() mod 2 = 0)">even</xsl:if>
		                   <xsl:if test="(position() mod 2 = 1)">odd</xsl:if>
		               </xsl:with-param>
		           </xsl:call-template>
		          <i18n:text><xsl:value-of select="$header"/></i18n:text>
		          </th>
		      </tr>    
	     </xsl:if>
    </xsl:template>
    
    <!-- No hago nada porque lo controlo desde el otro lado -->
    <xsl:template match="dri:row[@role='header']" mode="table_browse_columns">
       
    </xsl:template>
    
    <!-- Es el mismo que el di:row normal, solo que necesito por el MODE -->
    <xsl:template match="dri:row" mode="table_browse_columns">
        <tr>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-table-row
                    <xsl:if test="(position() mod 2 = 0)">even</xsl:if>
                    <xsl:if test="(position() mod 2 = 1)">odd</xsl:if>
                </xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </tr>
    </xsl:template>

    

</xsl:stylesheet>