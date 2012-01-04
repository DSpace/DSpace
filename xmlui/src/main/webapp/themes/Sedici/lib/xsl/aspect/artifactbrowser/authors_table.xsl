<xsl:stylesheet
    xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
    xmlns:dri="http://di.tamu.edu/DRI/1.0/"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

    <xsl:output indent="yes"/>

   <xsl:template match="dri:div[@id='aspect.artifactbrowser.ConfigurableBrowse.div.browse-by-author-results']">
     <xsl:variable name="cantItemsXCol">
          20
     </xsl:variable> 
     
     <xsl:apply-templates mode="tabla_autores">    
        <xsl:with-param name="itemsTotal"><xsl:value-of select="@itemsTotal"/></xsl:with-param>
        <xsl:with-param name="cantItemsXCol"><xsl:value-of select="$cantItemsXCol"/></xsl:with-param>
     </xsl:apply-templates>
     
   </xsl:template>
   
   <xsl:template match="dri:table" mode='tabla_autores'>
	 <xsl:param name="itemsTotal"/>
     <xsl:param name="cantItemsXCol"/>
     
     <!-- Si hay mas de un elemento debo mostrar por lo menos el primer contenedor de autores -->
     <xsl:if test="$itemsTotal>1">
	     <xsl:call-template name="div_contenedor_autores">
	     	<xsl:with-param name="contador">0</xsl:with-param>
	     	 <xsl:with-param name="cantItemsXCol"><xsl:value-of select="$cantItemsXCol"/></xsl:with-param>
	     </xsl:call-template>
	 </xsl:if>

     <!-- Si hay mas de la cantidad permitida por columna, genero el segundo contenedor -->
     <xsl:if test="$itemsTotal > $cantItemsXCol">
	     <xsl:call-template name="div_contenedor_autores">
	     	<xsl:with-param name="contador">1</xsl:with-param>
	     	 <xsl:with-param name="cantItemsXCol"><xsl:value-of select="$cantItemsXCol"/></xsl:with-param>
	     </xsl:call-template>
      </xsl:if>
	 
	 <!-- Si hay mas del doble de la cantidad permitida por columna, genero el tercer contenedor -->
	   <xsl:if test="$itemsTotal > ($cantItemsXCol*2)">
	     <xsl:call-template name="div_contenedor_autores">
	     	<xsl:with-param name="contador">2</xsl:with-param>
	     	 <xsl:with-param name="cantItemsXCol"><xsl:value-of select="$cantItemsXCol"/></xsl:with-param>
	     </xsl:call-template>
      </xsl:if>


	</xsl:template>
	
	    <!-- No hago nada porque lo controlo desde el otro lado -->
    <xsl:template name="div_contenedor_autores">
         <xsl:param name="contador"/>
         <xsl:param name="cantItemsXCol"/>
         
         <div class="contenedor_autores">
		  <table>
		     <xsl:call-template name="header_autores">
		     	<xsl:with-param name="header"><xsl:value-of select="dri:row[@role='header']"/></xsl:with-param>
		     </xsl:call-template>
		     
	         <xsl:apply-templates select="dri:row[floor(position() div ($cantItemsXCol+2)) = $contador]"  mode="tabla_autores"/>
	
		  </table>
	     </div>
         
         
         
    </xsl:template>
	
    <!-- Imprime el header de la tabla -->
    <xsl:template name="header_autores">
	    <xsl:param name="header"/>
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
    </xsl:template>
    
    <!-- No hago nada porque lo controlo desde el otro lado -->
    <xsl:template match="dri:row[@role='header']" mode="tabla_autores">
       
    </xsl:template>
    
    <xsl:template match="dri:row" mode="tabla_autores">
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