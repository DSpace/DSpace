<xsl:stylesheet version="1.0"
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"
>
   <xsl:output method="xml"
               encoding="utf8"
               omit-xml-declaration="no"
               indent="yes"
   />

   <!-- Top Level -->
   <xsl:template match="/ss:Workbook">
     <collection>
       <xsl:apply-templates select="ss:Worksheet/ss:Table/ss:Row[position() &gt; 1]"/>
     </collection>
   </xsl:template>

   <!-- Single record -->
   <xsl:template match="ss:Row">

     <!-- docID -->
     <xsl:variable name="docID">
       <xsl:call-template name="getColumn">
         <xsl:with-param name="colnum" select="'1'"/>
       </xsl:call-template>
     </xsl:variable>

     <!-- docTitle -->
     <xsl:variable name="docTitle">
       <xsl:call-template name="getColumn">
         <xsl:with-param name="colnum" select="'2'"/>
       </xsl:call-template>
     </xsl:variable>

     <!-- docDescription -->
     <xsl:variable name="docDescription">
       <xsl:call-template name="getColumn">
         <xsl:with-param name="colnum" select="'4'"/>
       </xsl:call-template>
     </xsl:variable>

     <!-- docNotes -->
     <xsl:variable name="docNotes">
       <xsl:call-template name="getColumn">
         <xsl:with-param name="colnum" select="'5'"/>
       </xsl:call-template>
     </xsl:variable>

     <!-- author -->
     <xsl:variable name="author">
       <xsl:call-template name="getColumn">
         <xsl:with-param name="colnum" select="'6'"/>
       </xsl:call-template>
     </xsl:variable>

     <!-- publishDate -->
     <xsl:variable name="publishDate">
       <xsl:call-template name="getColumn">
         <xsl:with-param name="colnum" select="'7'"/>
       </xsl:call-template>
     </xsl:variable>

     <!-- fileName -->
     <xsl:variable name="fileName">
       <xsl:call-template name="getColumn">
         <xsl:with-param name="colnum" select="'9'"/>
       </xsl:call-template>
     </xsl:variable>

     <!-- docType -->
     <xsl:variable name="docType">
       <xsl:call-template name="getColumn">
         <xsl:with-param name="colnum" select="'10'"/>
       </xsl:call-template>
     </xsl:variable>

     <!-- project -->
     <xsl:variable name="project">
       <xsl:call-template name="getColumn">
         <xsl:with-param name="colnum" select="'11'"/>
       </xsl:call-template>
     </xsl:variable>


     <xsl:if test="$fileName != ''">
       <record>

         <fileName><xsl:value-of select="$fileName"/></fileName>
         <url>http://cissm.umd.edu/papers/files/<xsl:value-of select="$fileName"/></url>

         <dublin_core>
         
           <!-- title -->
           <dcvalue element="title" qualifier="none">
             <xsl:value-of select="$docTitle"/>
           </dcvalue>
         
           <!-- contributor.author -->
           <dcvalue element="contributor" qualifier="author">
             <xsl:value-of select="$author"/>
           </dcvalue>
         
           <!-- date.issued -->
           <dcvalue element="date" qualifier="issued">
             <xsl:value-of select="substring($publishDate,1,10)"/>
           </dcvalue>
         
           <!-- relation.ispartofseries -->
           <dcvalue element="relation" qualifier="ispartofseries">
             <xsl:text>CISSM; </xsl:text>
             <xsl:value-of select="$docID"/>
           </dcvalue>
         
           <xsl:if test="$project != ''">
             <dcvalue element="relation" qualifier="ispartofseries">
               <xsl:value-of select="$project"/>
             </dcvalue>
           </xsl:if>
         
           <!-- description -->
           <xsl:if test="$docDescription != ''">
             <dcvalue element="description" qualifier="none">
               <xsl:value-of select="$docDescription"/>
             </dcvalue>
           </xsl:if>
         
           <!-- contributor.department -->
           <dcvalue element="contributor" qualifier="department">
             <xsl:text>CISSM</xsl:text>
           </dcvalue>
         
           <!-- description.abstract -->
           <xsl:if test="$docNotes != ''">
             <dcvalue element="description" qualifier="abstract">
               <xsl:value-of select="$docNotes"/>
             </dcvalue>
           </xsl:if>
         
           <!-- type -->
           <dcvalue element="type" qualifier="none">
             <xsl:value-of select="$docType"/>
           </dcvalue>
         
           <!-- language.iso -->
           <dcvalue element="language" qualifier="iso">
             <xsl:text>en_US</xsl:text>
           </dcvalue>
         
         </dublin_core>
       </record>
     </xsl:if>
   </xsl:template>


   <!-- getColumn -->
   <xsl:template name="getColumn">
     <xsl:param name="colnum"/>

     <xsl:call-template name="getColumnR">
       <xsl:with-param name="colnum" select="$colnum"/>
       <xsl:with-param name="i" select="'1'"/>
       <xsl:with-param name="cell" select="ss:Cell[1]"/>
     </xsl:call-template>
   </xsl:template>


   <!-- getColumnR -->
   <xsl:template name="getColumnR">
     <xsl:param name="colnum"/>
     <xsl:param name="i"/>
     <xsl:param name="cell"/>

     <xsl:variable name="modi">
       <xsl:choose>
         <xsl:when test="$cell/@ss:Index">
           <xsl:value-of select="$cell/@ss:Index"/>
         </xsl:when>
         <xsl:otherwise>
           <xsl:value-of select="$i"/>
         </xsl:otherwise>
       </xsl:choose>
     </xsl:variable>

<!--
     <xsl:message>
       <xsl:value-of select="$modi"/>
       <xsl:text>: </xsl:text>
       <xsl:value-of select="$colnum"/>
       <xsl:text>: </xsl:text>
       <xsl:value-of select="$cell/ss:Data"/>
     </xsl:message>
-->

     <xsl:choose>

       <!-- colnum is empty, not in the Row -->
       <xsl:when test="$modi &gt; $colnum">
       </xsl:when>

       <!-- this is the colnum we want -->
       <xsl:when test="$modi = $colnum">
         <xsl:value-of select="$cell/ss:Data"/>
       </xsl:when>

       <!-- try the next cell -->
       <xsl:otherwise>
         <xsl:if test="$cell/following-sibling::*[1]">
           <xsl:call-template name="getColumnR">
             <xsl:with-param name="colnum" select="$colnum"/>
             <xsl:with-param name="i" select="$modi + 1"/>
             <xsl:with-param name="cell" select="$cell/following-sibling::*[1]"/>
           </xsl:call-template>
         </xsl:if>
       </xsl:otherwise>

       
     </xsl:choose>

   </xsl:template>


</xsl:stylesheet>



