<xsl:stylesheet version="1.0"
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>
   <xsl:output method="xml"
               encoding="utf8"
               omit-xml-declaration="no"
               indent="yes"
   />

   <!-- Top Level -->
   <xsl:template match="/DISS_submission">
     <dublin_core>
       <xsl:apply-templates select="*"/>
       <dcvalue element="contributor" qualifier="publisher">Digital Repository at the University of Maryland</dcvalue>
       <dcvalue element="contributor" qualifier="publisher">University of Maryland (College Park, Md.)</dcvalue>
     </dublin_core>
   </xsl:template>

   <!-- Authorship -->
   <xsl:template match="DISS_authorship">
     <xsl:apply-templates select="*"/>
   </xsl:template>

   <xsl:template match="DISS_author">
     <dcvalue element="contributor" qualifier="author">
       <xsl:apply-templates select="DISS_name"/>
     </dcvalue>
   </xsl:template>

   <!-- Description -->
   <xsl:template match="DISS_description">
     <xsl:apply-templates select="*"/>
   </xsl:template>

   <xsl:template match="DISS_title">
     <dcvalue element="title" qualifier="none">   
       <xsl:value-of select="."/>
     </dcvalue>
   </xsl:template>

   <xsl:template match="DISS_degree">
     <xsl:choose>
       <xsl:when test="starts-with(.,'--')">
         <dcvalue element="type" qualifier="none">Unknown</dcvalue>
       </xsl:when>
       <xsl:when test="contains(.,'D')">
         <dcvalue element="type" qualifier="none">Dissertation</dcvalue>
       </xsl:when>
       <xsl:otherwise>
         <dcvalue element="type" qualifier="none">Thesis</dcvalue>
       </xsl:otherwise>
     </xsl:choose>
   </xsl:template>

   <xsl:template match="DISS_advisor">
     <dcvalue element="contributor" qualifier="advisor">
       <xsl:apply-templates select="DISS_name"/>
     </dcvalue>
   </xsl:template>

   <xsl:template match="DISS_categorization">
     <xsl:apply-templates select="*"/>
   </xsl:template>

   <xsl:template match="DISS_institution">
     <xsl:apply-templates select="*"/>
   </xsl:template>

   <xsl:template match="DISS_category[not(contains(text(), 'Select One'))]">
     <dcvalue element="subject" qualifier="pqcontrolled">
       <xsl:value-of select="DISS_cat_desc"/>
     </dcvalue>
   </xsl:template>

   <xsl:template match="DISS_keyword[text()]">
     <xsl:call-template name="Parse_DISS_keyword">
       <xsl:with-param name="text" select="."/>
     </xsl:call-template>
   </xsl:template>

   <!-- Parse DISS_keyword, delimiter is comma followed by space -->
   <xsl:template name="Parse_DISS_keyword">
     <xsl:param name="text"/>
     <xsl:choose>
       <!-- Skip leading space or comma -->
       <xsl:when test="substring($text,1,1) = ' ' or substring($text,1,1) = ','">
         <xsl:call-template name="Parse_DISS_keyword">
           <xsl:with-param name="text" select="substring($text,2)"/>
         </xsl:call-template>
       </xsl:when> 

       <!-- Add keyword and continue parsing -->
       <xsl:when test="contains($text,',')">
         <dcvalue element="subject" qualifier="pquncontrolled">
           <xsl:value-of select="substring-before($text,',')"/>
         </dcvalue>
         <xsl:call-template name="Parse_DISS_keyword">
           <xsl:with-param name="text" select="substring-after($text,',')"/>
         </xsl:call-template>
       </xsl:when>

       <!-- Add final keyword -->
       <xsl:otherwise>
         <dcvalue element="subject" qualifier="pquncontrolled">
           <xsl:value-of select="$text"/>
         </dcvalue>
       </xsl:otherwise>
     </xsl:choose>
   </xsl:template>

   <xsl:template match="DISS_language">
     <dcvalue element="language" qualifier="iso">
       <xsl:value-of select="."/>
     </dcvalue>
   </xsl:template>
   
   <xsl:template match="DISS_dates">
     <xsl:apply-templates select="*"/>
   </xsl:template>

   <xsl:template match="DISS_accept_date">
     <dcvalue element="date" qualifier="issued">
       <xsl:call-template name="DISS_date">
         <xsl:with-param name="yearonly" select="'true'"/>
       </xsl:call-template>
     </dcvalue>
   </xsl:template>

   <xsl:template match="DISS_inst_contact[not(contains(text(), 'Pick One'))]">
     <dcvalue element="contributor" qualifier="department">
       <xsl:value-of select="."/>
     </dcvalue>
   </xsl:template>

   <!-- Content -->
   <xsl:template match="DISS_content">
     <xsl:apply-templates select="*"/>
   </xsl:template>

   <xsl:template match="DISS_abstract[text() != '']">
     <dcvalue element="description" qualifier="abstract">
       <xsl:apply-templates select="DISS_para"/>
     </dcvalue>
   </xsl:template>

   <xsl:template match="DISS_para">
     <xsl:value-of select="."/>
     <xsl:text>&#10;&#10;</xsl:text>
   </xsl:template>
 
   <!-- Common Templates -->
   <xsl:template match="DISS_name">
     <xsl:value-of select="DISS_surname"/>
     <xsl:text>, </xsl:text>
     <xsl:value-of select="DISS_fname"/>
     <xsl:text> </xsl:text>
     <xsl:value-of select="DISS_middle"/>
   </xsl:template>
 
   <xsl:template name="DISS_date">
     <xsl:param name="yearonly" select="'false'"/>

     <xsl:value-of select="substring(.,7,4)"/>

     <xsl:if test="$yearonly != 'true'">
       <xsl:text>-</xsl:text>
       <xsl:value-of select="substring(.,1,2)"/>
       <xsl:text>-</xsl:text>
       <xsl:value-of select="substring(.,4,2)"/>
     </xsl:if>
   </xsl:template>
 
   <xsl:template match="*"/>

</xsl:stylesheet>



