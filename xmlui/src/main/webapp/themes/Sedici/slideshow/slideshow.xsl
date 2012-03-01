<xsl:stylesheet version="2.0" 
     xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
     xmlns="http://www.w3.org/1999/xhtml"
     xmlns:dri="http://di.tamu.edu/DRI/1.0/"
     xmlns:mets="http://www.loc.gov/METS/"
     xmlns:dim="http://www.dspace.org/xmlns/dspace/dim">
<!-- Ver http://www.queness.com/post/1450/jquery-photo-slide-show-with-slick-caption-tutorial-revisited -->
<xsl:template name='slideshow'>

	<!-- Este es el codigo que deberan tocar para modificar el slideshow -->
	<ul class="slideshow">
	    <li>
	    	<a>
    			<xsl:attribute name="href">
    				<xsl:value-of select="//dri:pageMeta/dri:metadata[@element='contextPath']"/>/pages/queEsSedici
    			</xsl:attribute>
	    		<img title="¿Qué es SeDiCI?" alt="SeDiCI (Servicio de Difusión de la Creación Intelectual) es el repositorio institucional central de la Universidad Nacional de La Plata.">
	    			<xsl:attribute name="src">
	    				<xsl:value-of select="//dri:pageMeta/dri:metadata[@element='contextPath']"/>/themes/Sedici/images/img_slideshow_1.jpg
	    			</xsl:attribute>
	    		</img>
	    	</a>
	    </li>
	    <li>
	    	<a href="#">
	    		<img title="Migración a DSpace" alt="Desde marzo de 2012, SeDiCI se encuentra funcionando sobre plataforma de software completamente nueva. Bienvenidos a SeDiCI-DSpace.">
	    			<xsl:attribute name="src">
	    				<xsl:value-of select="//dri:pageMeta/dri:metadata[@element='contextPath']"/>/themes/Sedici/images/img_slideshow_2.jpg
	    			</xsl:attribute>
	    		</img>
	    	</a>
	    </li>
	    <li>
	    	<a href="#">
	    		<img title="Libro electrónico supera las 2500 descargas" alt="El libro electrónico Cirugía. Bases clínicas y terapéuticas editado el año pasado por SeDiCI ya superó las 2500 descargas.">
	    			<xsl:attribute name="src">
	    				<xsl:value-of select="//dri:pageMeta/dri:metadata[@element='contextPath']"/>/themes/Sedici/images/img_slideshow_3.jpg
	    			</xsl:attribute>
	    		</img>
	    	</a>
	    </li>
	    <li>
	    	<a>
    			<xsl:attribute name="href">
    				<xsl:value-of select="//dri:pageMeta/dri:metadata[@element='contextPath']"/>/pages/comoAgregarTrabajosTesistas
    			</xsl:attribute>
	    		<img title="Depósito de tesis de posgrado" alt="La resolución 78/11 de la UNLP dictamina que todos los alumnos de posgrado deben depositar una copia digital de sus tesis en SeDiCI.">
	    			<xsl:attribute name="src">
	    				<xsl:value-of select="//dri:pageMeta/dri:metadata[@element='contextPath']"/>/themes/Sedici/images/img_slideshow_4.jpg
	    			</xsl:attribute>
	    		</img>
	    	</a>
	    </li>
	</ul>

</xsl:template>

</xsl:stylesheet>
