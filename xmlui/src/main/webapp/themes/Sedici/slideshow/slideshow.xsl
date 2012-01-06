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
	    <li class="show">
	    	<a href="#">
	    		<img title="SeDiCI" alt="Serivicio de Difusión de la Creación Intelectual de la Universidad Nacional de La Plata">
	    			<xsl:attribute name="src">
	    				<xsl:value-of select="//dri:pageMeta/dri:metadata[@element='contextPath']"/>/themes/Sedici/images/img_slideshow_1.jpg
	    			</xsl:attribute>
	    		</img>
	    	</a>
	    </li>
	    <li class="show">
	    	<a href="#">
	    		<img title="Nueva colección con libros de medicina" alt="Se ha agregado una nueva colección exclusiva para libros de medicina, con mas de 500 libros en formato digital, accesibles bajo las políticas de Acceso Abierto">
	    			<xsl:attribute name="src">
	    				<xsl:value-of select="//dri:pageMeta/dri:metadata[@element='contextPath']"/>/themes/Sedici/images/img_slideshow_2.jpg
	    			</xsl:attribute>
	    		</img>
	    	</a>
	    </li>
	    <li class="show">
	    	<a href="#">
	    		<img title="Reglamentación de Tesis de Posgrado" alt="La Universidad Nacional de La Plata publicó la resolución Nº 785/45 que obliga a los alumnos de posgrado a depositar sus tesis en SeDiCI como requisito para tramitar su título">
	    			<xsl:attribute name="src">
	    				<xsl:value-of select="//dri:pageMeta/dri:metadata[@element='contextPath']"/>/themes/Sedici/images/img_slideshow_3.jpg
	    			</xsl:attribute>
	    		</img>
	    	</a>
	    </li>
	</ul>

</xsl:template>

</xsl:stylesheet>