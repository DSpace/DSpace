<!--

	Template extraido de tema mirage con el mismo nombre.
	Solo redefino el template necesario. Para modificar otros dirigirse al template original y copiar los necesarios.
-->

<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/"
	xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns="http://www.w3.org/1999/xhtml"
	exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc">

    <xsl:output indent="yes"/>
    

    <!-- Fieldset (instanced) field stuff, in the case of non-composites -->
    <xsl:template match="dri:field[dri:field/dri:instance | dri:params/@operations]" priority="2">
        <xsl:apply-templates select="dri:help" mode="help"/>

        <xsl:if test="dri:instance">
            <div class="ds-previous-values">
                <!-- Iterate over the dri:instance elements contained in this field. The instances contain
                    stored values as either "interpreted", "raw", or "default" values. -->
                <xsl:call-template name="especialSimpleFieldIterator">
                    <xsl:with-param name="position">1</xsl:with-param>
                </xsl:call-template>

                <!-- Behind the scenes, add hidden fields for every instance set. This is to make sure that
                    the form still submits the information in those instances, even though they are no
                    longer encoded as HTML fields. The DRI Reference should contain the exact attributes
                    the hidden fields should have in order for this to work properly. -->
				<!-- <xsl:apply-templates select="dri:instance" mode="hiddenInterpreter"/>                  -->    
              </div>
        </xsl:if>
        
        <!-- Create the first field normally -->
        <xsl:apply-templates select="." mode="normalField"/>
        <!-- Follow it up with an ADD button if the add operation is specified. This allows
            entering more than one value for this field. -->
        <xsl:if test="contains(dri:params/@operations,'add')">
            <!-- Add buttons should be named "submit_[field]_add" so that we can ignore errors from required fields when simply adding new values-->
            <input type="submit"  value="xmlui.forms.add" i18n:attr="value" id="{concat('submit_',@n,'_add')}" name="{concat('submit_',@n,'_add')}" class="ds-button-field ds-add-button">
              <!-- Make invisible if we have choice-lookup popup that provides its own Add. -->
              <xsl:if test="dri:params/@choicesPresentation = 'lookup'">
                <xsl:attribute name="style">
                  <xsl:text>display:none;</xsl:text>
                </xsl:attribute>
	        </xsl:if>
           </input>
        </xsl:if>
        <br/>
        <xsl:apply-templates select="dri:error" mode="error"/>
    </xsl:template>
    
   
<!--   <input type="hidden" value="Aacn Clinical Issues" name="dc_title_alternative_1">
<input class="ds-authority-value " type="text" onchange="javascript: return DSpaceAuthorityOnChange(this, '_confidence','');" value="1079-0713" name="dc_title_alternative_authority_1" readonly="readonly">
<input class="ds-authority-confidence-input" type="hidden" value="failed" name="dc_title_alternative_confidence_1">
-->   

    <!-- Template especiales para la muestra de los valores en las subscripciones del perfil -->     
    <xsl:template match="dri:field[@n='subscriptions']" priority="2">
        <xsl:apply-templates select="dri:help" mode="help"/>
        <!-- Create the first field normally -->
        <xsl:apply-templates select="." mode="normalField"/>
        <!-- Follow it up with an ADD button if the add operation is specified. This allows
            entering more than one value for this field. -->
        <xsl:if test="contains(dri:params/@operations,'add')">
            <!-- Add buttons should be named "submit_[field]_add" so that we can ignore errors from required fields when simply adding new values-->
            <input type="submit" value="xmlui.profile.subscriptions.add" id="{concat('submit_',@n,'_add')}" name="{concat('submit_',@n,'_add')}" class="ds-button-field ds-add-button" i18n:attr="value">
              <!-- Make invisible if we have choice-lookup popup that provides its own Add. -->
              <xsl:if test="dri:params/@choicesPresentation = 'lookup'">
                <xsl:attribute name="style">
                  <xsl:text>display:none;</xsl:text>
                </xsl:attribute>
        	  </xsl:if>
           </input>
        </xsl:if>
        <br/>
        <xsl:apply-templates select="dri:error" mode="error"/>
        <xsl:if test="dri:instance">
            <div class="ds-previous-values">
                <!-- Iterate over the dri:instance elements contained in this field. The instances contain
                    stored values as either "interpreted", "raw", or "default" values. -->
                <xsl:call-template name="subscriptionsSimpleFieldIterator">
                    <xsl:with-param name="position">1</xsl:with-param>
                </xsl:call-template>
                <!-- Conclude with a DELETE button if the delete operation is specified. This allows
                    removing one or more values stored for this field. -->
                <xsl:if test="contains(dri:params/@operations,'delete') and dri:instance">
                    <!-- Delete buttons should be named "submit_[field]_delete" so that we can ignore errors from required fields when simply removing values-->
                    <input type="submit" value="xmlui.profile.subscriptions.remove" name="{concat('submit_',@n,'_delete')}" class="ds-button-field ds-delete-button" i18n:attr="value"/>
                </xsl:if>
                <!-- Behind the scenes, add hidden fields for every instance set. This is to make sure that
                    the form still submits the information in those instances, even though they are no
                    longer encoded as HTML fields. The DRI Reference should contain the exact attributes
                    the hidden fields should have in order for this to work properly. -->
				<xsl:apply-templates select="dri:instance" mode="hiddenInterpreter"/>                    
              </div>
        </xsl:if>

    </xsl:template>
    
    <!-- Utilizo un field iterator especial pues los instances del subscription no tienen el valor, y lo tengo que sacar del list -->
    <xsl:template name="subscriptionsSimpleFieldIterator">
        <xsl:param name="position"/>
        
        <xsl:if test="dri:instance[position()=$position]">
            <xsl:variable name="authValue" select="substring-before(dri:instance[position()=$position]/dri:value[@type='authority'], '#')"/>            
            <xsl:variable name="authLabel" select="substring-after(dri:instance[position()=$position]/dri:value[@type='authority'], '#')"/>
            <xsl:variable name="valor"><xsl:value-of select="dri:instance[position()=$position]/dri:value/@option"/></xsl:variable>
            <input type="checkbox" value="{concat(@n,'_',$position)}" name="{concat(@n,'_selected')}">
              <xsl:copy-of select="dri:option[@returnValue=$valor]"/>
            </input>
             <!-- look for authority value in instance. -->
            <xsl:if test="dri:instance[position()=$position]/dri:value[@type='authority']">             	
              <xsl:call-template name="multipleAuthorityInputFields">
                <xsl:with-param name="name" select="@n"/>
      			<xsl:with-param name="position" select="$position"/>
     			<xsl:with-param name="value" select="dri:instance[position()=$position]/dri:value"/>
      			<xsl:with-param name="authValue" select="$authValue"/>
      			<xsl:with-param name="authLabel" select="$authLabel"/>
      			<xsl:with-param name="confValue" select="dri:instance[position()=$position]/dri:value[@type='authority']/@confidence"/>
              </xsl:call-template>
            </xsl:if>
            <br/>
            <xsl:call-template name="subscriptionsSimpleFieldIterator">
                <xsl:with-param name="position"><xsl:value-of select="$position + 1"/></xsl:with-param>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>
    
    <!-- Este template recibirá en la propiedad authority formateado de la siguiente manera
	     authorityKey#authorityLabel -->
    <xsl:template name="especialSimpleFieldIterator">
        <xsl:param name="position"/>
        
        <xsl:if test="dri:instance[position()=$position]">
            <xsl:variable name="authValue" select="substring-before(dri:instance[position()=$position]/dri:value[@type='authority'], '#')"/>
            <xsl:variable name="authLabel" select="substring-after(dri:instance[position()=$position]/dri:value[@type='authority'], '#')"/>
            <xsl:variable name="checkboxId" select="concat(@n,'_checkbox_',$position)"/>
            <input type="checkbox" id="{$checkboxId}"  value="{concat(@n,'_',$position)}" name="{concat(@n,'_selected')}" style="display:none;"/> 

            <xsl:apply-templates select="dri:instance[position()=$position]" mode='inputChange'>
               <xsl:with-param name="position" select="$position"/>
            </xsl:apply-templates>

             <!-- look for authority value in instance. -->
            <xsl:if test="dri:instance[position()=$position]/dri:value[@type='authority']">             	
              <xsl:call-template name="multipleAuthorityInputFields">
                <xsl:with-param name="name" select="@n"/>
      			<xsl:with-param name="position" select="$position"/>
     			<xsl:with-param name="value" select="dri:instance[position()=$position]/dri:value"/>
      			<xsl:with-param name="authValue" select="$authValue"/>
      			<xsl:with-param name="authLabel" select="$authLabel"/>
      			<xsl:with-param name="confValue" select="dri:instance[position()=$position]/dri:value[@type='authority']/@confidence"/>
              </xsl:call-template>
            </xsl:if>
            
             <!-- Conclude with a DELETE button if the delete operation is specified. This allows
                removing one value stored for this field. -->
            <xsl:if test="contains(dri:params/@operations,'delete') and dri:instance">
                <!-- Delete buttons should be named "submit_[field]_delete" so that we can ignore errors from required fields when simply removing values-->
                <input type="submit" value="xmlui.forms.remove" i18n:attr="value" name="{concat('submit_',@n,'_delete')}" class="ds-button-field ds-delete-button" onClick="eliminarMetadato('{$checkboxId}');"/>
            </xsl:if>
            <br/>
            <xsl:call-template name="especialSimpleFieldIterator">
                <xsl:with-param name="position"><xsl:value-of select="$position + 1"/></xsl:with-param>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>
     
    <!-- TEMPLATE PARA GENERAR LOS INPUt PARA MODIFICACIONES -->
    
    <xsl:template match="dri:instance" mode="inputChange">
        <xsl:param name="position" select="1"/>
        <xsl:choose>
             <xsl:when test="../@type= 'textarea'">
             <textarea>
                    <xsl:call-template name="fieldAttributes"/>
                    <xsl:attribute name="class">ds-textarea-field submit-textarea</xsl:attribute>
                    <xsl:attribute name="name"><xsl:value-of select="concat(../@n,'_',$position)"/></xsl:attribute>
                    <xsl:choose>
                       <xsl:when test="not(../dri:params[@cols])">
                           <xsl:call-template name="textAreaCols"/>
                        </xsl:when>
                    </xsl:choose>
                    <xsl:choose>
                       <xsl:when test="not(../dri:params[@rows])">
                           <xsl:call-template name="textAreaRows"/>
                       </xsl:when>
                    </xsl:choose>
			        <xsl:value-of select="dri:value" disable-output-escaping="yes"/>

                </textarea> 
             </xsl:when>
             <xsl:when test="../@type= 'text' and not(../dri:params/@authorityControlled='yes')">
			       <input type="text">
			           <xsl:attribute name="class">ds-text-field submit-text</xsl:attribute>
			           <xsl:attribute name="name"><xsl:value-of select="concat(../@n,'_',$position)"/></xsl:attribute>
			           <xsl:attribute name="value">
			               <xsl:value-of select="dri:value[@type='raw']"/>
			           </xsl:attribute>
			       </input>
             </xsl:when>
             <xsl:when test="not(../dri:params/@authorityControlled='yes') ">
                    <xsl:value-of select="dri:value[@type='raw']"/>                    
             </xsl:when>
             <xsl:when test="not(../dri:params/@choicesPresentation='suggest') ">
                    <xsl:value-of select="dri:value[@type='raw']"/>                    
             </xsl:when>
        </xsl:choose>
        

    </xsl:template>
    
    <xsl:template match="/dri:document/dri:body/dri:div[@id='aspect.xmlworkflow.WorkflowTransformer.div.perform-task']/dri:head">
        <xsl:variable name="handle"><xsl:value-of select="substring-after(substring-before(../dri:referenceSet/dri:reference/@url,'/mets.xml'), 'metadata/')"/></xsl:variable>
        <xsl:variable name="head_count" select="count(ancestor::dri:*[dri:head])"/>
        <xsl:variable name="class">ds-div-head</xsl:variable>
        <xsl:element name="h{$head_count}">
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class" select="$class"/>
            </xsl:call-template>
            <xsl:apply-templates/>
            <xsl:choose>
            	<xsl:when test="contains($handle,'internal')">
            		(<i18n:text>xmlui.forms.claimedAction.non_installed</i18n:text>)
            	</xsl:when>
            	<xsl:otherwise>
	            	(<a>
		        	  <xsl:attribute name="href">
		        	  <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath']"/>/<xsl:value-of select="$handle"/>
                      </xsl:attribute>		        	  
		        	  <xsl:value-of select="substring-after($handle, 'handle/')"/>
		            </a>)
            	</xsl:otherwise>
            </xsl:choose>
        </xsl:element>

    </xsl:template>
    


</xsl:stylesheet>
