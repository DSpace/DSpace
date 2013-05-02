<?xml version="1.0" encoding="UTF-8"?>
<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ex="http://apache.org/cocoon/exception/1.0"
                xmlns:i18n="http://apache.org/cocoon/i18n/2.1">

  <xsl:param name="realPath"/>
  <xsl:param name="errorKind">internalError</xsl:param>
  <xsl:param name="contextPath">/</xsl:param>
  <xsl:param name="printDebug">false</xsl:param>
  <xsl:param name="requestQueryString"></xsl:param>
  
<xsl:template match="/">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
	<link rel="shortcut icon">
		<xsl:attribute name="href"><xsl:value-of select="concat($contextPath,'/themes/Sedici/images/favicon.ico')"/></xsl:attribute>
	</link>
	<link media="screen" rel="stylesheet" type="text/css" >
		<xsl:attribute name="href"><xsl:value-of select="concat($contextPath,'/themes/Sedici/lib/css/base.css')"/></xsl:attribute>
	</link>
	<link media="screen" rel="stylesheet" type="text/css">
		<xsl:attribute name="href"><xsl:value-of select="concat($contextPath,'/themes/Sedici/lib/css/reset.css')"/></xsl:attribute>
	</link>
	<link media="screen" rel="stylesheet" type="text/css">
		<xsl:attribute name="href"><xsl:value-of select="concat($contextPath,'/themes/Sedici/lib/css/style.css')"/></xsl:attribute>
	</link>
	<title><i18n:text>sedici.errorTitle.<xsl:value-of select="$errorKind"/></i18n:text></title>
        <style>
          p.home { padding: 10px 30px 10px 15px; margin-left: 15px; font-size: 100%;}
          p.message { padding: 10px 30px 10px 15px; margin-left: 15px; font-weight: bold; font-size: 100%;  border-left: 1px #336699 dashed;}
          p.description { padding: 10px 30px 20px 30px; border-width: 0px 0px 1px 0px; border-style: solid; border-color: #336699;}
          p.topped { padding-top: 10px; border-width: 1px 0px 0px 0px; border-style: solid; border-color: #336699; }
          span.switch { cursor: pointer; margin-left: 5px; text-decoration: underline; }
          span.description { color: #336699; font-weight: bold; }
          .row-1 { background-color: #F0F0F0;}
          table { border-collapse: collapse; margin-top: 0.3em; }
          td { padding: 0.1em; }
        </style>
</head>
<body>
    <xsl:if test="$printDebug = 'true'">
        <xsl:attribute name="onload">
          <xsl:if test="ex:exception-report/ex:cocoon-stacktrace">toggle('locations');</xsl:if>
          <xsl:if test="ex:exception-report/ex:stacktrace">toggle('stacktrace');</xsl:if>
          <xsl:if test="ex:exception-report/ex:full-stacktrace">toggle('full-stacktrace');</xsl:if>
        </xsl:attribute>
	</xsl:if>
		
	<div id="ds-main" xmlns="http://di.tamu.edu/DRI/1.0/"
		xmlns:i18n="http://apache.org/cocoon/i18n/2.1">
		<div id="ds-header-wrapper">
			<div id="ds-header" class="clearfix">
				 <a id="ds-header-logo-link"><xsl:attribute name="href">
                                     <xsl:value-of select="concat($contextPath,'/')"/>
                                     </xsl:attribute>
					<span id="ds-header-logo">&#160;</span>
				</a>
			</div>
		</div>
<!-- 		<div id="topNav" xmlns:i18n="http://apache.org/cocoon/i18n/2.1"> -->
<!-- 		</div> -->

		<div id="ds-content-wrapper">
			<div id="ds-content" class="clearfix">

				<!--  BODYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY-->
				<div id="ds-body">
					<h1 class="errorTitle">
						<i18n:text>sedici.errorTitle.<xsl:value-of select="$errorKind"/></i18n:text>
				<!-- 		<xsl:value-of select="$requestQueryString"/> -->
					</h1>
					<p class="errorDescription"><i18n:text>sedici.errorDescription.<xsl:value-of select="$errorKind"/></i18n:text></p>
					<p class="generalNote"><i18n:text>sedici.errorDescription.generalNote</i18n:text></p>
					
					
					<xsl:if test="$printDebug = 'true'">
				  		<xsl:apply-templates select="ex:exception-report" />
					</xsl:if>
				</div>
				<!--  BODYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY-->
				
				<div id="ds-options-wrapper">
					<div id="ds-options">
<!-- 						<h1 class="ds-option-set-head"><i18n:text>xmlui.general.go</i18n:text></h1> -->
						<div class="ds-option-set"
							id="aspect_discovery_Navigation_list_discovery"
							xmlns="http://di.tamu.edu/DRI/1.0/"
							xmlns:i18n="http://apache.org/cocoon/i18n/2.1">
							
							<ul class="ds-options-list">
								<li>
									<h2 class="ds-sublist-head">
    								  <a>
    								  <xsl:attribute name="href">
    								  		<xsl:value-of select="$contextPath"/>/
    								  </xsl:attribute>
    								  <i18n:text>xmlui.general.go_home</i18n:text>
    								  </a>
    								</h2>
								</li>
							</ul>
						</div>
					</div>
				</div><!-- ds-options-wrapper -->

			</div>
		</div><!-- ds-content-wrapper -->

		<div id="footer" xmlns="http://di.tamu.edu/DRI/1.0/" xmlns:i18n="http://apache.org/cocoon/i18n/2.1">
			<div id="footercol1">
				<div class="datos_unlp">
					<strong>2003-2012 &#169; <a href="http://prebi.unlp.edu.ar/" target="_blank">PrEBi</a></strong> 
					<br/> 
					<a href="http://www.unlp.edu.ar" target="_blank">Universidad Nacional de La Plata</a> 
					<br/> Todos los derechos reservados conforme a la ley 11.723
				</div>
			</div>
			<div id="footercol2">
				<div class="datos_sedici">
					<strong>SeDiCI - Servicio de Difusión de la Creación
						Intelectual</strong> <br/> Calle 49 y 115 s/n 1er piso - Edificio ex
					Liceo <br/> 1900 La Plata, Buenos Aires - Tel 0221 423
					6696/6677 (int. 141)
				</div>
			</div>
		</div> <!-- del footer -->
		
		
	</div><!-- ds-main -->
	

</body>
</html>
</xsl:template>

  		<xsl:template match="ex:exception-report">
    	
	        <script type="text/javascript">
	          function toggle(id) {
	            var element = document.getElementById(id);
	            with (element.style) {
	              if ( display == "none" ) {
	                display = ""
	              } else {
	                display = "none"
	              }
	            }
	          
	            var text = document.getElementById(id + "-switch").firstChild;
	            if (text.nodeValue == "[show]") {
	              text.nodeValue = "[hide]";
	            } else {
	              text.nodeValue = "[show]";
	            }
	          }
	        </script>
	        <p class="message">
	          <xsl:value-of select="@class"/>:
	          <xsl:apply-templates select="ex:message" mode="breakLines"/>
	          <xsl:if test="ex:location">
	             <br/><span style="font-weight: normal"><xsl:apply-templates select="ex:location"/></span>
	          </xsl:if>
	        </p>
	
	        <p><span class="description">Cocoon stacktrace</span>
	           <span class="switch" id="locations-switch" onclick="toggle('locations')">[hide]</span>
	        </p>
	        <div id="locations">
	          <xsl:for-each select="ex:cocoon-stacktrace/ex:exception">
	            <xsl:sort select="position()" order="descending"/>
	            <strong>
	               <xsl:apply-templates select="ex:message" mode="breakLines"/>
	            </strong>
	            <table>
	               <xsl:for-each select="ex:locations/*[string(.) != '[cause location]']">
	                 <!-- [cause location] indicates location of a cause, which 
	                      the exception generator outputs separately -->
	                <tr class="row-{position() mod 2}">
	                   <td><xsl:call-template name="print-location"/></td>
	                   <td><em><xsl:value-of select="."/></em></td>
	                </tr>
	              </xsl:for-each>
	            </table>
	            <br/>
	           </xsl:for-each>
	        </div>
	
	        <xsl:apply-templates select="ex:stacktrace"/>
	        <xsl:apply-templates select="ex:full-stacktrace"/>
  
		</xsl:template>


  <xsl:template match="ex:stacktrace|ex:full-stacktrace">
      <p class="stacktrace">
       <span class="description">Java <xsl:value-of select="translate(local-name(), '-', ' ')"/></span>
       <span class="switch" id="{local-name()}-switch" onclick="toggle('{local-name()}')">[hide]</span>
       <pre id="{local-name()}">
         <xsl:value-of select="translate(.,'&#13;','')"/>
       </pre>
      </p>
  </xsl:template>
  
  <xsl:template match="ex:location">
   <xsl:if test="string-length(.) > 0">
     <em><xsl:value-of select="."/></em>
     <xsl:text> - </xsl:text>
   </xsl:if>
   <xsl:call-template name="print-location"/>
  </xsl:template>
  
  <xsl:template name="print-location">
     <xsl:choose>
       <xsl:when test="contains(@uri, $realPath)">
         <xsl:text>context:/</xsl:text>
         <xsl:value-of select="substring-after(@uri, $realPath)"/>
       </xsl:when>
       <xsl:otherwise>
         <xsl:value-of select="@uri"/>
       </xsl:otherwise>
      </xsl:choose>
      <xsl:text> - </xsl:text>
      <xsl:value-of select="@line"/>:<xsl:value-of select="@column"/>
  </xsl:template>
  
  <!-- output a text by splitting it with <br>s on newlines
       can be uses either by an explicit call or with <apply-templates mode="breakLines"/> -->
  <xsl:template match="node()"  mode="breakLines" name="breakLines">
     <xsl:param name="text" select="string(.)"/>
     <xsl:choose>
        <xsl:when test="contains($text, '&#10;')">
           <xsl:value-of select="substring-before($text, '&#10;')"/>
           <br/>
           <xsl:call-template name="breakLines">
              <xsl:with-param name="text" select="substring-after($text, '&#10;')"/>
           </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
           <xsl:value-of select="$text"/>
        </xsl:otherwise>
     </xsl:choose>
  </xsl:template>


</xsl:stylesheet>
