<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    xmlns:ddw="http://datadryad.org/api/v1/widgets/display"
    xmlns:csv="http://apache.org/cocoon/csv/1.0"
    xmlns:math="http://exslt.org/math"
    exclude-result-prefixes="xhtml ddw math csv"
    version="1.0">

    <!-- Pass through DataOne-MN bitstream url for
         PNG and JPEG images.
         TODO: handle scaling, positioning better
    --> 
    <xsl:template name="image-native">
        <html>
            <head>
                <style>
                    .container {
                    margin: 10px;
                    max-width: 100%;
                    max-height: 100%;
                    text-align: center;
                    }
                    .fit {
                    max-height:100%;
                    max-width:100%;
                    vertical-aign:middle;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <img class="fit" src="{$source}" alt="Image for {$doi}"></img>
                </div>
            </body>
        </html>
    </xsl:template>

</xsl:stylesheet>