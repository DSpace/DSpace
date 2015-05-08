
<!--
    Rendering of the description of the featured artwork 

    Author: carolyn.zinn at kzoo.edu
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
                xmlns:confman="org.dspace.core.ConfigurationManager"
                exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc confman">

    <xsl:output method="xml" encoding="UTF-8" indent="yes"/>
<xsl:template name="featuredArtwork">

	<div id="photobanner">
        <div class="transparent"><a href="https://cache.kzoo.edu/handle/10920/10710">Kalamazoo College Art Collection</a></div>
	</div>
    <div class="hero-unit">
    <!-- BEGIN DESCRIPTION. DO NOT MODIFY ABOVE THIS LINE!!-->
    
            <h1>Harvest Near Louviers; Oil on canvas</h1>
            <h3>Nozal, Alexandre,1852-1929</h3>
            <p>
            Alexandre Nozal, landscape painter, was born at Neuilly-sur-Seine on 7th August 1852.
            He studied under Luminais and made his debut at the Paris Salon of 1876.
            He was awarded medals at the Salons of 1882 and 1883 and at the Exposition Universelles of 1889 and 1900, 
            and in 1895 he was elected a Chevalier de la Legion d’honneur.
            Examples of his works are in many museums including the Luxembourg in Paris and the municipal art galleries 
            of Bourges, Chalons-sur-Marne, Gray, Montpellier, Nantes, Rouen, and Melbourne, Australia.
            </p>
   
   
    <!-- END DESCRIPTION. DO NOT MODIFY BELOW THIS LINE!! -->
    <p>Visit the <a href="https://cache.kzoo.edu/handle/10920/10710">Kalamazoo College Art Collection</a></p> 
    </div>
</xsl:template>
</xsl:stylesheet>