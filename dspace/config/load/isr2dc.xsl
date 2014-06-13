<xsl:stylesheet version="1.0"
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>
   <xsl:output method="xml"
               encoding="utf8"
               omit-xml-declaration="no"
               indent="yes"
   />

   <!-- Top Level -->
   <xsl:template match="/record">
     <dublin_core>

       <dcvalue element="title" qualifier="none">
         <xsl:value-of select="title"/>
       </dcvalue>

       <xsl:for-each select="author">
         <dcvalue element="contributor" qualifier="author">
           <xsl:value-of select="."/>
         </dcvalue>
       </xsl:for-each>

       <dcvalue element="date" qualifier="issued">
         <xsl:value-of select="year"/>
       </dcvalue>

       <dcvalue element="relation" qualifier="ispartofseries">
	 <xsl:value-of select="concat('ISR; ',papertype,' ',year,'-',isrnum)"/>
       </dcvalue>

       <xsl:if test="center != 'ISR'">
         <dcvalue element="relation" qualifier="ispartofseries">
           <xsl:value-of select="concat(center,'; ',papertype,' ',year,'-',center/@centernum)"/>
         </dcvalue>
       </xsl:if>

       <xsl:for-each select="advisors[text() != '']">
         <dcvalue element="contributor" qualifier="advisor">
           <xsl:value-of select="."/>
         </dcvalue>
       </xsl:for-each>

       <xsl:if test="keywords">
         <dcvalue element="subject" qualifier="none">
           <xsl:value-of select="keywords"/>
         </dcvalue>
       </xsl:if>

       <dcvalue element="contributor" qualifier="department">
         <xsl:text>ISR</xsl:text>
       </dcvalue>

       <xsl:if test="center != 'ISR'">
         <dcvalue element="contributor" qualifier="department">
           <xsl:value-of select="center"/>
         </dcvalue>
       </xsl:if>

       <xsl:if test="abstract != 'ABSTRACT NOT AVAILABLE'">
         <dcvalue element="description" qualifier="abstract">
           <xsl:value-of select="abstract"/>
         </dcvalue>
       </xsl:if>

       <dcvalue element="type" qualifier="none">
         <xsl:choose>
	   <xsl:when test="papertype='TR'">
	     <xsl:text>Technical Report</xsl:text>
           </xsl:when>
	   <xsl:when test="papertype='MS'">
	     <xsl:text>Thesis</xsl:text>
           </xsl:when>
	   <xsl:when test="papertype='PhD'">
	     <xsl:text>Dissertation</xsl:text>
           </xsl:when>
	   <xsl:when test="papertype='UG'">
	     <xsl:text>Thesis</xsl:text>
           </xsl:when>
         </xsl:choose>
       </dcvalue>

       <dcvalue element="language" qualifier="iso">
         <xsl:text>en_US</xsl:text>
       </dcvalue>

     </dublin_core>
   </xsl:template>

</xsl:stylesheet>



