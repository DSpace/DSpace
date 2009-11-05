<?xml version="1.0"?>

<!--

This template is in the public domain available from http://www.exslt.org/. This 
was confirmed via email message from the site administrator(jeni@jenitennison.com) 
on Februray 8th, 2008 - the exact email message is included below:

>
> All the code that I've provided on the EXSLT is in the public domain and can be 
> used without restrictions. I'd ask that you include a reference to EXSLT and to 
> myself as a courtesy, but it's not a requirement.
> 
> Cheers,
> 
> Jeni
> - - 
> Jeni Tennison
> http://www.jenitennison.com
>

-->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:date="http://exslt.org/dates-and-times" xmlns:func="http://exslt.org/functions" extension-element-prefixes="date func">

<xsl:param name="date:date-time" select="'2000-01-01T00:00:00Z'"/>

<date:months>
   <date:month abbr="Jan">January</date:month>
   <date:month abbr="Feb">February</date:month>
   <date:month abbr="Mar">March</date:month>
   <date:month abbr="Apr">April</date:month>
   <date:month abbr="May">May</date:month>
   <date:month abbr="Jun">June</date:month>
   <date:month abbr="Jul">July</date:month>
   <date:month abbr="Aug">August</date:month>
   <date:month abbr="Sep">September</date:month>
   <date:month abbr="Oct">October</date:month>
   <date:month abbr="Nov">November</date:month>
   <date:month abbr="Dec">December</date:month>
</date:months>

<xsl:template name="month-name">
	<xsl:param name="date-time">
      <xsl:choose>
         <xsl:when test="function-available('date:date-time')">
            <xsl:value-of select="date:date-time()"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:value-of select="$date:date-time"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:param>
   <xsl:variable name="neg" select="starts-with($date-time, '-') and                                      not(starts-with($date-time, '--'))"/>
   <xsl:variable name="dt-no-neg">
      <xsl:choose>
         <xsl:when test="$neg or starts-with($date-time, '+')">
            <xsl:value-of select="substring($date-time, 2)"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:value-of select="$date-time"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:variable>
   <xsl:variable name="dt-no-neg-length" select="string-length($dt-no-neg)"/>
   <xsl:variable name="timezone">
      <xsl:choose>
         <xsl:when test="substring($dt-no-neg, $dt-no-neg-length) = 'Z'">Z</xsl:when>
         <xsl:otherwise>
            <xsl:variable name="tz" select="substring($dt-no-neg, $dt-no-neg-length - 5)"/>
            <xsl:if test="(substring($tz, 1, 1) = '-' or                             substring($tz, 1, 1) = '+') and                           substring($tz, 4, 1) = ':'">
               <xsl:value-of select="$tz"/>
            </xsl:if>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:variable>
   <xsl:variable name="month">
      <xsl:if test="not(string($timezone)) or                     $timezone = 'Z' or                      (substring($timezone, 2, 2) &lt;= 23 and                      substring($timezone, 5, 2) &lt;= 59)">
         <xsl:variable name="dt" select="substring($dt-no-neg, 1, $dt-no-neg-length - string-length($timezone))"/>
         <xsl:variable name="dt-length" select="string-length($dt)"/>
         <xsl:choose>
            <xsl:when test="substring($dt, 1, 2) = '--' and                             substring($dt, 3, 2) &lt;= 12 and                             substring($dt, 5, 1) = '-' and                             (substring($dt, 6) = '-' or                              ($dt-length = 7 and                               substring($dt, 6) &lt;= 31))">
               <xsl:value-of select="substring($dt, 3, 2)"/>
            </xsl:when>
            <xsl:when test="number(substring($dt, 1, 4)) and                             substring($dt, 5, 1) = '-' and                             substring($dt, 6, 2) &lt;= 12 and                             ($dt-length = 7 or                              (substring($dt, 8, 1) = '-' and                               substring($dt, 9, 2) &lt;= 31 and                               ($dt-length = 10 or                                (substring($dt, 11, 1) = 'T' and                                 substring($dt, 12, 2) &lt;= 23 and                                 substring($dt, 14, 1) = ':' and                                 substring($dt, 15, 2) &lt;= 59 and                                 substring($dt, 17, 1) = ':' and                                 substring($dt, 18) &lt;= 60))))">
               <xsl:value-of select="substring($dt, 6, 2)"/>
            </xsl:when>
         </xsl:choose>
      </xsl:if>
   </xsl:variable>
   <xsl:value-of select="document('')/*/date:months/date:month[number($month)]"/>
</xsl:template>

</xsl:stylesheet>