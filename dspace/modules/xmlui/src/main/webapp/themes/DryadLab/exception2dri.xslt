<?xml version="1.0" encoding="UTF-8"?>
<!--
  Copyright 1999-2004 The Apache Software Foundation

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

<!--

This stylsheet to handle exception display is a modified version of the
base apache cocoon stylesheet, this is still under the Apache license.

The original author is unknown.
Scott Phillips adapted it for Manakin's need.

-->

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ex="http://apache.org/cocoon/exception/1.0"
                xmlns:i18n="http://apache.org/cocoon/i18n/2.1">

    <xsl:param name="realPath"/>

    <!-- let sitemap override default page title -->
    <xsl:param name="pageTitle">An error has occurred</xsl:param>

    <!-- let sitemap override default context path -->
    <xsl:param name="contextPath">/</xsl:param>

    <xsl:template match="ex:exception-report">
        <document xmlns="http://di.tamu.edu/DRI/1.0/" xmlns:i18n="http://apache.org/cocoon/i18n/2.1" version="1.1" xmlns:xi="http://www.w3.org/2003/XInclude">
            <body>

                <div id="error-occured" rend="primary" n="error-occured">
                    <head>An error has occured</head>
                    <p><xref target="/">Go to Dryad home</xref></p>


                </div>
                <div id="full-stacktrace">
                    <xsl:apply-templates select="ex:stacktrace"/>
                    <xsl:apply-templates select="ex:full-stacktrace"/>
                </div>
            </body>

            <meta>
                <userMeta/>
                <pageMeta>
                    <metadata element="title">Error Page</metadata>
                    <trail target="/">Dryad Digital Repository</trail>
                </pageMeta>
                <repositoryMeta/>
            </meta>
        </document>

    </xsl:template>

    <xsl:template match="ex:stacktrace|ex:full-stacktrace">
        <p>
            <!--xsl:if test="contains(@class, 'ResourceNotFoundException')"-->
            <!--/xsl:if-->
            <xsl:value-of select="translate(.,'&#13;','')"/>

            <!--span class="description">Java <xsl:value-of select="translate(local-name(), '-', ' ')"/></span>
            <span class="switch" id="{local-name()}-switch" onclick="toggle('{local-name()}')">[hide]</span>
            <pre id="{local-name()}">
                <xsl:value-of select="translate(.,'&#13;','')"/>
            </pre-->
        </p>
    </xsl:template>

</xsl:stylesheet>
