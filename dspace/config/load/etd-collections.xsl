<xsl:stylesheet version="1.0"
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>
   <xsl:output method="xml"/>

   <!-- Top Level -->
   <xsl:template match="/DISS_submission">
     <collections>
       <xsl:for-each select="DISS_description/DISS_institution/DISS_inst_contact">
       	 <xsl:variable name="contact"><xsl:value-of select="normalize-space(.)"/></xsl:variable>
       	 
       	 <xsl:for-each select="document('etd-collections.xml')/maps/map[normalize-space(from/text()) = $contact]/to">
           <collection><xsl:value-of select="."/></collection>
       	 </xsl:for-each>
       </xsl:for-each>
     </collections>
   </xsl:template>

</xsl:stylesheet>



