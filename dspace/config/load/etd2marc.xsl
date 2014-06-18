<xsl:stylesheet version="1.0"
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns="http://www.loc.gov/MARC21/slim"
>

   <xsl:output method="xml"
               encoding="UTF-8"
               omit-xml-declaration="no"
               indent="yes"
   />

   <xsl:param name="files" select="'Unknown'"/>

   <xsl:param name="handle" select="'Unknown'"/>

   <!-- Top Level -->
   <xsl:template match="/DISS_submission">

     <!-- Accept date -->
     <xsl:variable name="acceptdate">
       <xsl:value-of select="substring(DISS_description/DISS_dates/DISS_accept_date,7,4)"/>
     </xsl:variable>

     <collection>
       <record>

         <leader>00000nam  22     ka 4500</leader>

         <controlfield tag="007">cr  n         </controlfield>

         <controlfield tag="008">
           <xsl:text>      s</xsl:text>
           <xsl:value-of select="$acceptdate"/>
           <xsl:text>    mdu     sbm         eng d</xsl:text>
         </controlfield>

         <datafield tag="040" ind1=" " ind2=" ">
           <subfield code="a">UMC</subfield>
           <subfield code="c">UMC</subfield>
         </datafield>

         <datafield tag="049" ind1=" " ind2=" ">
           <subfield code="a">UMCP</subfield>
         </datafield>

         <xsl:for-each select="DISS_authorship/DISS_author">
           <datafield tag="100" ind1="1" ind2=" ">
             <subfield code="a">
               <xsl:apply-templates select="DISS_name"/>
               <xsl:text>.</xsl:text>
             </subfield>
           </datafield>
         </xsl:for-each>

         <datafield tag="245" ind1="1" ind2=" ">
           <subfield code="a"><xsl:value-of select="DISS_description/DISS_title"/><xsl:value-of select="' '"/></subfield>
           <subfield code="h">[electronic resource] / </subfield>
           <subfield code="c">
             <xsl:for-each select="DISS_authorship/DISS_author/DISS_name">
               <xsl:call-template name="fname_first"/>
             </xsl:for-each>
             <xsl:text>.</xsl:text>
           </subfield>
         </datafield>

         <datafield tag="260" ind1=" " ind2=" ">
           <subfield code="a">College Park, Md.: </subfield>
           <subfield code="b">University of Maryland, </subfield>
           <subfield code="c">
             <xsl:value-of select="$acceptdate"/>
             <xsl:text>.</xsl:text>
           </subfield>
         </datafield>

         <xsl:for-each select="DISS_description/DISS_institution/DISS_inst_contact">
           <datafield tag="500" ind1=" " ind2=" ">
             <subfield code="a">
               <xsl:text>Thesis research directed by: </xsl:text>
               <xsl:value-of select="."/>
               <xsl:text>.</xsl:text>
             </subfield>
           </datafield>
         </xsl:for-each>

         <datafield tag="500" ind1=" " ind2=" ">
           <subfield code="a">Title from t.p. of PDF.</subfield>
         </datafield>

         <xsl:for-each select="DISS_description/DISS_degree">
           <datafield tag="502" ind1=" " ind2=" ">
             <subfield code="a">
               <xsl:text>Thesis (</xsl:text>
               <xsl:value-of select="."/>
               <xsl:text>) -- University of Maryland, College Park, </xsl:text>
               <xsl:value-of select="$acceptdate"/>
               <xsl:text>.</xsl:text>
             </subfield>
           </datafield>
         </xsl:for-each>

         <datafield tag="504" ind1=" " ind2=" ">
           <subfield code="a">Includes bibliographical references.</subfield>
         </datafield>

         <datafield tag="516" ind1=" " ind2=" ">
           <subfield code="a"><xsl:value-of select="$files"/></subfield>
         </datafield>

         <datafield tag="530" ind1=" " ind2=" ">
           <subfield code="a">Also available in paper.</subfield>
           <subfield code="b">Published by UMI Dissertation Services, Ann Arbor, Mich.</subfield>
         </datafield>

<!--
         <xsl:for-each select="DISS_content/DISS_abstract">
           <datafield tag="520" ind1="3" ind2=" ">
             <subfield code="a">
               <xsl:for-each select="DISS_para[normalize-space(.) != '']">
                 <xsl:value-of select="normalize-space(.)"/>
       <xsl:text> </xsl:text>
               </xsl:for-each>
             </subfield>
           </datafield>
         </xsl:for-each>
-->

         <datafield tag="856" ind1="4" ind2="0">
           <subfield code="u"><xsl:value-of select="$handle"/></subfield>
         </datafield>

         <datafield tag="852" ind1="0" ind2=" ">
           <subfield code="8">1</subfield>
           <subfield code="b">CPSPE</subfield>
           <subfield code="c">MDTHS</subfield>
           <subfield code="h">LD3231.M70d</subfield>
           <subfield code="i">
             <xsl:value-of select="$acceptdate"/>
        <xsl:value-of select="' '"/>
        <xsl:apply-templates select="DISS_authorship/DISS_author/DISS_name"/>
           </subfield>
         </datafield>

         <datafield tag="876" ind1=" " ind2=" ">
           <subfield code="8">1.1</subfield>
           <subfield code="j">03</subfield>
           <subfield code="p"></subfield>
           <subfield code="x">MT:BOOK</subfield>
         </datafield>

         <datafield tag="852" ind1="0" ind2=" ">
           <subfield code="8">2</subfield>
           <subfield code="b">CPNET</subfield>
           <subfield code="c">WWW</subfield>
           <subfield code="h">LD3231.M70d</subfield>
           <subfield code="i">
             <xsl:value-of select="$acceptdate"/>
        <xsl:value-of select="' '"/>
        <xsl:apply-templates select="DISS_authorship/DISS_author/DISS_name"/>
           </subfield>
         </datafield>

         <datafield tag="876" ind1=" " ind2=" ">
           <subfield code="8">2.1</subfield>
           <subfield code="j">04</subfield>
           <subfield code="p"></subfield>
           <subfield code="x">MT:WWW</subfield>
         </datafield>

       </record>
     </collection>
   </xsl:template>

   <!-- Common Templates -->
   <xsl:template match="DISS_name">
     <xsl:value-of select="DISS_surname"/>
     <xsl:text>, </xsl:text>
     <xsl:value-of select="DISS_fname"/>
     <xsl:if test="DISS_middle">
        <xsl:text> </xsl:text>
        <xsl:value-of select="DISS_middle"/>
     </xsl:if>
   </xsl:template>
 
   <xsl:template name="fname_first">
     <xsl:value-of select="DISS_fname"/>

     <xsl:for-each select="DISS_middle">
       <xsl:text> </xsl:text>
       <xsl:value-of select="."/>
     </xsl:for-each>

     <xsl:text> </xsl:text>
     <xsl:value-of select="DISS_surname"/>
   </xsl:template>


</xsl:stylesheet>



