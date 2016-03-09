<?xml version='1.0' encoding='UTF-8'?>
<xsl:stylesheet
    version='1.0'
    xmlns:xsl='http://www.w3.org/1999/XSL/Transform'
    xmlns:dri="http://di.tamu.edu/DRI/1.0/"
    xmlns:il8n="http://apache.org/cocoon/i18n/2.1"
    >

    <xsl:output method="xml" indent="yes"/>
    <xsl:preserve-space elements="*"/>

    <xsl:variable name="time-month-q"   select="'time:[NOW-1MONTH TO NOW]'"     />
    <xsl:variable name="time-year-q"    select="'time:[NOW-1YEAR TO NOW]'"      />
    <xsl:variable name="time-alltime-q" select="'time:[* TO NOW]'"              />

    <xsl:variable name="time-month"     select="'journallanding-stats-month'"  />
    <xsl:variable name="time-year"      select="'journallanding-stats-year'"   />
    <xsl:variable name="time-alltime"   select="'journallanding-stats-alltime'"/>

    <xsl:variable name="q" select="/response/lst[@name='responseHeader']/lst[@name='params']/str[@name='q']"/>
    <xsl:variable name="n">
        <xsl:choose>
            <xsl:when test="contains($q, $time-month-q)"><xsl:value-of select="$time-month"/></xsl:when>
            <xsl:when test="contains($q, $time-year-q)"><xsl:value-of select="$time-year"/></xsl:when>
            <xsl:when test="contains($q, $time-alltime-q)"><xsl:value-of select="$time-alltime"/></xsl:when>
        </xsl:choose>
    </xsl:variable>

    <xsl:template match="/">

        <div xmlns="http://di.tamu.edu/DRI/1.0/"
            id="{concat('aspect.journallanding.JournalStats.div.',$n)}"
            rend="{$n}"
            n="{$n}"
        >
            <div id="aspect.journallanding.JournalStats.div.items" n="items">
                <referenceSet id="aspect.journallanding.JournalStats.referenceSet.{$n}" n="{$n}" type="summaryList">
                    <head/>
                    <xsl:apply-templates select="//lst[@name='owningItem']/int" mode="reference"/>
                </referenceSet>
            </div>
            <div id="aspect.journallanding.JournalStats.div.vals" n="vals">
                <list id="aspect.journallanding.JournalStats.list.{$n}" rend="{$n}" n="{$n}" type="simple">
                    <head><il8n:text>xmlui.JournalLandingPage.JournalStats.val_head</il8n:text></head>
                    <xsl:apply-templates select="//lst[@name='owningItem']/int" mode="item"/>
                </list>
            </div>
        </div>
    </xsl:template>

    <xsl:template match="*"/>
    <xsl:template match="text()"/>

    <xsl:template match="//int[parent::lst[@name='owningItem']]" mode="reference">
        <xsl:processing-instruction name="reference">
            <xsl:value-of select="@name"/>
        </xsl:processing-instruction>
    </xsl:template>

    <xsl:template match="//int[parent::lst[@name='owningItem']]" mode="item">
        <item xmlns="http://di.tamu.edu/DRI/1.0/">
            <xsl:value-of select="."/>
        </item>
    </xsl:template>

</xsl:stylesheet>
