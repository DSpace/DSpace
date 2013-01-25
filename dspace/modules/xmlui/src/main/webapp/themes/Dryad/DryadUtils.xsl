<?xml version="1.0" encoding="UTF-8"?>

<!--  This stylesheet provides some generic use templates that are used from various other XSLTs. -->

<xsl:stylesheet xmlns="http://www.w3.org/1999/xhtml" xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
                xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:mets="http://www.loc.gov/METS/"
                xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
                xmlns:mods="http://www.loc.gov/mods/v3" xmlns:xlink="http://www.w3.org/TR/xlink/"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:datetime="http://exslt.org/dates-and-times" xmlns:encoder="xalan://java.net.URLEncoder"
                xmlns:strings="http://exslt.org/strings"
                xmlns:confman="org.dspace.core.ConfigurationManager"
                exclude-result-prefixes="xalan strings encoder datetime" version="1.0">

    <xsl:output method="xml" version="1.0" encoding="utf-8" indent="yes"/>

    <xsl:variable name="ascii">!"#$%&amp;'()*+,-./0123456789:;&lt;=&gt;?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\]^_`abcdefghijklmnopqrstuvwxyz{|}~</xsl:variable>
    <xsl:variable name="latin1">&#160;&#161;&#162;&#163;&#164;&#165;&#166;&#167;&#168;&#169;&#170;&#171;&#172;&#173;&#174;&#175;&#176;&#177;&#178;&#179;&#180;&#181;&#182;&#183;&#184;&#185;&#186;&#187;&#188;&#189;&#190;&#191;&#192;&#193;&#194;&#195;&#196;&#197;&#198;&#199;&#200;&#201;&#202;&#203;&#204;&#205;&#206;&#207;&#208;&#209;&#210;&#211;&#212;&#213;&#214;&#215;&#216;&#217;&#218;&#219;&#220;&#221;&#222;&#223;&#224;&#225;&#226;&#227;&#228;&#229;&#230;&#231;&#232;&#233;&#234;&#235;&#236;&#237;&#238;&#239;&#240;&#241;&#242;&#243;&#244;&#245;&#246;&#247;&#248;&#249;&#250;&#251;&#252;&#253;&#254;&#255;</xsl:variable>

    <!-- Characters that usually don't need to be escaped -->
    <xsl:variable name="safe">!'()*-.0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz~</xsl:variable>

    <xsl:variable name="hex">0123456789ABCDEF</xsl:variable>

    <!--  This template can parse names and normalize the output; it works
     on names in direct order -->
    <xsl:template name="name-parse">
        <xsl:param name="name"/>
        <xsl:variable name="names"
                      select="strings:split(normalize-space(translate($name, '.', ' ')))"/>
        <xsl:variable name="count" select="count($names)"/>
        <xsl:if test="$count &gt; 0">
            <!-- handle the last name, first -->
            <xsl:value-of select="$names[$count]"/>
            <xsl:if test="$count &gt; 1">
                <xsl:text> </xsl:text>
                <!-- now the first and middle names -->
                <xsl:for-each select="$names">
                    <xsl:choose>
                        <xsl:when test="position() = $count"/>
                        <xsl:otherwise>
                            <xsl:value-of select="substring(., 1, 1)"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:for-each>
            </xsl:if>
        </xsl:if>
    </xsl:template>

    <!--  This template can parse names and normalize the output; it works
on names with last name first (comma delimited). -->
    <xsl:template name="name-parse-reverse">
        <xsl:param name="name"/>
        <xsl:variable name="names" select="strings:split(translate($name, '.', ' '), ',')"/>
        <xsl:value-of select="normalize-space($names[1])"/>
        <xsl:text> </xsl:text>
        <xsl:for-each select="strings:split(normalize-space($names[2]), ' ')">
            <xsl:value-of select="substring(., 1, 1)"/>
        </xsl:for-each>
    </xsl:template>

    <!--  Used by the template below, datetime:difference -->
    <datetime:month-lengths xmlns:date="http://exslt.org/dates-and-times">
        <datetime:month>31</datetime:month>
        <datetime:month>28</datetime:month>
        <datetime:month>31</datetime:month>
        <datetime:month>30</datetime:month>
        <datetime:month>31</datetime:month>
        <datetime:month>30</datetime:month>
        <datetime:month>31</datetime:month>
        <datetime:month>31</datetime:month>
        <datetime:month>30</datetime:month>
        <datetime:month>31</datetime:month>
        <datetime:month>30</datetime:month>
        <datetime:month>31</datetime:month>
    </datetime:month-lengths>

    <!--  This template can return differences between supplied start and end
   datetimes -->
    <xsl:template name="datetime:difference">
        <xsl:param name="start"/>
        <xsl:param name="end"/>
        <xsl:variable name="start-neg" select="starts-with($start, '-')"/>
        <xsl:variable name="start-no-neg">
            <xsl:choose>
                <xsl:when test="$start-neg or starts-with($start, '+')">
                    <xsl:value-of select="substring($start, 2)"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$start"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="start-no-neg-length" select="string-length($start-no-neg)"/>
        <xsl:variable name="start-timezone">
            <xsl:choose>
                <xsl:when test="substring($start-no-neg, $start-no-neg-length) = 'Z'">Z</xsl:when>
                <xsl:otherwise>
                    <xsl:variable name="tz"
                                  select="substring($start-no-neg, $start-no-neg-length - 5)"/>
                    <xsl:if
                            test="(substring($tz, 1, 1) = '-' or substring($tz, 1, 1) = '+') and substring($tz, 4, 1) = ':'">
                        <xsl:value-of select="$tz"/>
                    </xsl:if>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="end-neg" select="starts-with($end, '-')"/>
        <xsl:variable name="end-no-neg">
            <xsl:choose>
                <xsl:when test="$end-neg or starts-with($end, '+')">
                    <xsl:value-of select="substring($end, 2)"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$end"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="end-no-neg-length" select="string-length($end-no-neg)"/>
        <xsl:variable name="end-timezone">
            <xsl:choose>
                <xsl:when test="substring($end-no-neg, $end-no-neg-length) = 'Z'">Z</xsl:when>
                <xsl:otherwise>
                    <xsl:variable name="tz" select="substring($end-no-neg, $end-no-neg-length - 5)"/>
                    <xsl:if
                            test="(substring($tz, 1, 1) = '-' or substring($tz, 1, 1) = '+') and substring($tz, 4, 1) = ':'">
                        <xsl:value-of select="$tz"/>
                    </xsl:if>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="difference">
            <xsl:if
                    test="(not(string($start-timezone)) or $start-timezone = 'Z' or (substring($start-timezone, 2, 2) &lt;= 23 and substring($start-timezone, 5, 2) &lt;= 59)) and (not(string($end-timezone)) or $end-timezone = 'Z' or (substring($end-timezone, 2, 2) &lt;= 23 and substring($end-timezone, 5, 2) &lt;= 59))">
                <xsl:variable name="start-dt"
                              select="substring($start-no-neg, 1, $start-no-neg-length - string-length($start-timezone))"/>
                <xsl:variable name="start-dt-length" select="string-length($start-dt)"/>
                <xsl:variable name="end-dt"
                              select="substring($end-no-neg, 1, $end-no-neg-length - string-length($end-timezone))"/>
                <xsl:variable name="end-dt-length" select="string-length($end-dt)"/>
                <xsl:variable name="start-year"
                              select="substring($start-dt, 1, 4) * (($start-neg * -2) + 1)"/>
                <xsl:variable name="end-year"
                              select="substring($end-dt, 1, 4) * (($end-neg * -2) + 1)"/>
                <xsl:variable name="diff-year" select="$end-year - $start-year"/>
                <xsl:choose>
                    <xsl:when test="not(number($start-year) and number($end-year))"/>
                    <xsl:when test="$start-dt-length = 4 or $end-dt-length = 4">
                        <xsl:choose>
                            <xsl:when test="$diff-year &lt; 0">-P
                                <xsl:value-of
                                        select="$diff-year * -1"/> Y
                            </xsl:when>
                            <xsl:otherwise>P
                                <xsl:value-of select="$diff-year"/> Y
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:when>
                    <xsl:when
                            test="substring($start-dt, 5, 1) = '-' and substring($end-dt, 5, 1) = '-'">
                        <xsl:variable name="start-month" select="substring($start-dt, 6, 2)"/>
                        <xsl:variable name="end-month" select="substring($end-dt, 6, 2)"/>
                        <xsl:variable name="diff-month" select="$end-month - $start-month"/>
                        <xsl:choose>
                            <xsl:when test="not($start-month &lt;= 12 and $end-month &lt;= 12)"/>
                            <xsl:when test="$start-dt-length = 7 or $end-dt-length = 7">
                                <xsl:variable name="months" select="$diff-month + ($diff-year * 12)"/>
                                <xsl:variable name="abs-months"
                                              select="$months * ((($months >= 0) * 2) - 1)"/>
                                <xsl:variable name="y" select="floor($abs-months div 12)"/>
                                <xsl:variable name="m" select="$abs-months mod 12"/>
                                <xsl:if test="$months &lt; 0">-</xsl:if>
                                <xsl:text>P</xsl:text>
                                <xsl:if test="$y">
                                    <xsl:value-of select="$y"/> Y
                                </xsl:if>
                                <xsl:if test="$m">
                                    <xsl:value-of select="$m"/> M
                                </xsl:if>
                            </xsl:when>
                            <xsl:when
                                    test="substring($start-dt, 8, 1) = '-' and substring($end-dt, 8, 1) = '-'">
                                <xsl:variable name="start-day" select="substring($start-dt, 9, 2)"/>
                                <xsl:variable name="end-day" select="substring($end-dt, 9, 2)"/>
                                <xsl:if test="$start-day &lt;= 31 and $end-day &lt;= 31">
                                    <xsl:variable name="month-lengths"
                                                  select="document('')/*/datetime:month-lengths/datetime:month"/>
                                    <xsl:variable name="start-y-1" select="$start-year - 1"/>
                                    <xsl:variable name="start-leaps"
                                                  select="floor($start-y-1 div 4) - floor($start-y-1 div 100) + floor($start-y-1 div 400)"/>
                                    <xsl:variable name="start-leap"
                                                  select="(not($start-year mod 4) and $start-year mod 100) or not($start-year mod 400)"/>
                                    <xsl:variable name="start-month-days"
                                                  select="sum($month-lengths[position() &lt; $start-month])"/>
                                    <xsl:variable name="start-days">
                                        <xsl:variable name="days"
                                                      select="($start-year * 365) + $start-leaps + $start-month-days + $start-day"/>
                                        <xsl:choose>
                                            <xsl:when test="$start-leap and $start-month > 2">
                                                <xsl:value-of select="$days + 1"/>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:value-of select="$days"/>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:variable>
                                    <xsl:variable name="end-y-1" select="$end-year - 1"/>
                                    <xsl:variable name="end-leaps"
                                                  select="floor($end-y-1 div 4) - floor($end-y-1 div 100) + floor($end-y-1 div 400)"/>
                                    <xsl:variable name="end-leap"
                                                  select="(not($end-year mod 4) and $end-year mod 100) or not($end-year mod 400)"/>
                                    <xsl:variable name="end-month-days"
                                                  select="sum($month-lengths[position() &lt; $end-month])"/>
                                    <xsl:variable name="end-days">
                                        <xsl:variable name="days"
                                                      select="($end-year * 365) + $end-leaps + $end-month-days + $end-day"/>
                                        <xsl:choose>
                                            <xsl:when test="$end-leap and $end-month > 2">
                                                <xsl:value-of select="$days + 1"/>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:value-of select="$days"/>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:variable>
                                    <xsl:variable name="diff-days" select="$end-days - $start-days"/>
                                    <xsl:choose>
                                        <xsl:when
                                                test="$start-dt-length = 10 or $end-dt-length = 10">
                                            <xsl:choose>
                                                <xsl:when test="$diff-days &lt; 0">-P
                                                    <xsl:value-of
                                                            select="$diff-days * -1"/> D
                                                </xsl:when>
                                                <xsl:otherwise>P
                                                    <xsl:value-of select="$diff-days"/>
                                                    D
                                                </xsl:otherwise>
                                            </xsl:choose>
                                        </xsl:when>
                                        <xsl:when
                                                test="substring($start-dt, 11, 1) = 'T' and substring($end-dt, 11, 1) = 'T' and substring($start-dt, 14, 1) = ':' and substring($start-dt, 17, 1) = ':' and substring($end-dt, 14, 1) = ':' and substring($end-dt, 17, 1) = ':'">
                                            <xsl:variable name="start-hour"
                                                          select="substring($start-dt, 12, 2)"/>
                                            <xsl:variable name="start-min"
                                                          select="substring($start-dt, 15, 2)"/>
                                            <xsl:variable name="start-sec"
                                                          select="substring($start-dt, 18)"/>
                                            <xsl:variable name="end-hour"
                                                          select="substring($end-dt, 12, 2)"/>
                                            <xsl:variable name="end-min"
                                                          select="substring($end-dt, 15, 2)"/>
                                            <xsl:variable name="end-sec"
                                                          select="substring($end-dt, 18)"/>
                                            <xsl:if
                                                    test="$start-hour &lt;= 23 and $end-hour &lt;= 23 and $start-min &lt;= 59 and $end-min &lt;= 59 and $start-sec &lt;= 60 and $end-sec &lt;= 60">
                                                <xsl:variable name="min-s" select="60"/>
                                                <xsl:variable name="hour-s" select="60 * 60"/>
                                                <xsl:variable name="day-s" select="60 * 60 * 24"/>
                                                <xsl:variable name="start-tz-adj">
                                                    <xsl:variable name="tz"
                                                                  select="(substring($start-timezone, 2, 2) * $hour-s) + (substring($start-timezone, 5, 2) * $min-s)"/>
                                                    <xsl:choose>
                                                        <xsl:when test="starts-with($start-timezone, '-')">
                                                            <xsl:value-of select="$tz"/>
                                                        </xsl:when>
                                                        <xsl:when test="starts-with($start-timezone, '+')">
                                                            <xsl:value-of select="$tz * -1"/>
                                                        </xsl:when>
                                                        <xsl:otherwise>0</xsl:otherwise>
                                                    </xsl:choose>
                                                </xsl:variable>
                                                <xsl:variable name="end-tz-adj">
                                                    <xsl:variable name="tz"
                                                                  select="(substring($end-timezone, 2, 2) * $hour-s) + (substring($end-timezone, 5, 2) * $min-s)"/>
                                                    <xsl:choose>
                                                        <xsl:when test="starts-with($end-timezone, '-')">
                                                            <xsl:value-of select="$tz"/>
                                                        </xsl:when>
                                                        <xsl:when test="starts-with($end-timezone, '+')">
                                                            <xsl:value-of select="$tz * -1"/>
                                                        </xsl:when>
                                                        <xsl:otherwise>0</xsl:otherwise>
                                                    </xsl:choose>
                                                </xsl:variable>
                                                <xsl:variable name="start-secs"
                                                              select="$start-sec + ($start-min * $min-s) + ($start-hour * $hour-s) + ($start-days * $day-s) + $start-tz-adj"/>
                                                <xsl:variable name="end-secs"
                                                              select="$end-sec + ($end-min * $min-s) + ($end-hour * $hour-s) + ($end-days * $day-s) + $end-tz-adj"/>
                                                <xsl:variable name="diff-secs"
                                                              select="$end-secs - $start-secs"/>
                                                <xsl:variable name="s"
                                                              select="$diff-secs * ((($diff-secs &lt; 0) * -2) + 1)"/>
                                                <xsl:variable name="days"
                                                              select="floor($s div $day-s)"/>
                                                <xsl:variable name="hours"
                                                              select="floor(($s - ($days * $day-s)) div $hour-s)"/>
                                                <xsl:variable name="mins"
                                                              select="floor(($s - ($days * $day-s) - ($hours * $hour-s)) div $min-s)"/>
                                                <xsl:variable name="secs"
                                                              select="$s - ($days * $day-s) - ($hours * $hour-s) - ($mins * $min-s)"/>
                                                <xsl:if test="$diff-secs &lt; 0">-</xsl:if>
                                                <xsl:text>P</xsl:text>
                                                <xsl:if test="$days">
                                                    <xsl:value-of select="$days"/>
                                                    <xsl:text>D</xsl:text>
                                                </xsl:if>
                                                <xsl:if test="$hours or $mins or $secs">T</xsl:if>
                                                <xsl:if test="$hours">
                                                    <xsl:value-of select="$hours"/>
                                                    <xsl:text>H</xsl:text>
                                                </xsl:if>
                                                <xsl:if test="$mins">
                                                    <xsl:value-of select="$mins"/>
                                                    <xsl:text>M</xsl:text>
                                                </xsl:if>
                                                <xsl:if test="$secs">
                                                    <xsl:value-of select="$secs"/>
                                                    <xsl:text>S</xsl:text>
                                                </xsl:if>
                                            </xsl:if>
                                        </xsl:when>
                                    </xsl:choose>
                                </xsl:if>
                            </xsl:when>
                        </xsl:choose>
                    </xsl:when>
                </xsl:choose>
            </xsl:if>
        </xsl:variable>
        <xsl:value-of select="$difference"/>
    </xsl:template>


    <!-- Ago 2011: changed logic, the servelt /doi doesn't exist anymore. Change also /handle for /resource -->
    <xsl:param name="url"/>
    <xsl:template name="checkURL">
        <xsl:param name="doiIdentifier"
                   select=".//dim:field[@element='identifier'][@mdschema='dc'][not(@qualifier)][1]"/>

        <xsl:choose>
            <xsl:when
                    test="/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@mdschema='workflow'][@element='step'][@qualifier='reviewerKey'] or $meta[@element='identifier'][@qualifier='reviewerKey']">
                <xsl:value-of select="$meta[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/review?token=</xsl:text>
                <xsl:choose>
                    <xsl:when test="$meta[@element='identifier'][@qualifier='reviewerKey']">
                        <xsl:value-of select="$meta[@element='identifier'][@qualifier='reviewerKey']"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of
                                select="/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@mdschema='workflow'][@element='step'][@qualifier='reviewerKey']"/>
                    </xsl:otherwise>
                </xsl:choose>


                <xsl:variable name="this" select="."/>

                <xsl:text>&amp;doi=</xsl:text>
                <xsl:choose>
                    <xsl:when test="starts-with($this, 'doi:10.5061/')">
                        <xsl:value-of select="$this"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$doiIdentifier"/>
                    </xsl:otherwise>
                </xsl:choose>

            </xsl:when>

            <xsl:otherwise>
                <xsl:text>/resource/</xsl:text>
                <xsl:variable name="this" select="."/>

                <xsl:choose>
                    <xsl:when test="starts-with($this, 'doi:10.5061/')">
                        <xsl:value-of select="$this"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$doiIdentifier"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <xsl:template name="renderCOinS">
        <xsl:text>ctx_ver=Z39.88-2004&amp;rft_val_fmt=info%3Aofi%2Ffmt%3Akev%3Amtx%3Adc&amp;</xsl:text>
        <xsl:for-each select=".//dim:field[@element = 'identifier']">
            <xsl:text>rft_id=</xsl:text>
            <xsl:value-of select="encoder:encode(string(.))"/>
            <xsl:text>&amp;</xsl:text>
        </xsl:for-each>
        <xsl:text>rfr_id=info%3Asid%2Fdatadryad.org%3Arepo&amp;</xsl:text>
        <xsl:for-each select=".//dim:field">
            <xsl:value-of select="concat('rft.', @element,'=',encoder:encode(string(.))) "/>
            <xsl:if test="position()!=last()">
                <xsl:text>&amp;</xsl:text>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>

</xsl:stylesheet>
