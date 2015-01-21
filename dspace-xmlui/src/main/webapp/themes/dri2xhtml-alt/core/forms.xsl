<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<!--
    Templates to cover the forms and forms fields.

    Author: art.lowel at atmire.com
    Author: lieven.droogmans at atmire.com
    Author: ben at atmire.com
    Author: Alexey Maslov

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

    <!-- Special treatment of a list type "form", which is used to encode simple forms and give them structure.
        This is done partly to ensure that the resulting HTML form follows accessibility guidelines. -->

    <xsl:template match="dri:list[@type='form']" priority="3">
    <xsl:choose>
       <xsl:when test="ancestor::dri:list[@type='form']">

            <li>
            <fieldset>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">
                    <!-- Provision for the sub list -->
                    <xsl:text>ds-form-</xsl:text>
                    <xsl:if test="ancestor::dri:list[@type='form']">
                        <xsl:text>sub</xsl:text>
                    </xsl:if>
                    <xsl:text>list </xsl:text>
                    <xsl:if test="count(dri:item) > 3">
                        <xsl:text>thick </xsl:text>
                    </xsl:if>
                </xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="dri:head"/>

            <ol>
                <xsl:apply-templates select="*[not(name()='label' or name()='head')]" />
            </ol>
            </fieldset>
            </li>
            </xsl:when>
            <xsl:otherwise>
            <fieldset>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">
                    <!-- Provision for the sub list -->
                    <xsl:text>ds-form-</xsl:text>
                    <xsl:if test="ancestor::dri:list[@type='form']">
                        <xsl:text>sub</xsl:text>
                    </xsl:if>
                    <xsl:text>list </xsl:text>
                    <xsl:if test="count(dri:item) > 3">
                        <xsl:text>thick </xsl:text>
                    </xsl:if>
                </xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="dri:head"/>

            <ol>
                <xsl:apply-templates select="*[not(name()='label' or name()='head')]" />
            </ol>
            </fieldset>
            </xsl:otherwise>
            </xsl:choose>
    </xsl:template>

    <!-- TODO: Account for the dri:hi/dri:field kind of nesting here and everywhere else... -->
    <xsl:template match="dri:list[@type='form']/dri:item" priority="3">
        <li>
                <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">
                    <xsl:text>ds-form-item </xsl:text>
                <xsl:choose>
                    <!-- Makes sure that the dark always falls on the last item -->
                    <xsl:when test="count(../dri:item) mod 2 = 0">
                        <xsl:if test="count(../dri:item) > 3">
                            <xsl:if test="(count(preceding-sibling::dri:item) mod 2 = 0)">even </xsl:if>
                            <xsl:if test="(count(preceding-sibling::dri:item) mod 2 = 1)">odd </xsl:if>
                        </xsl:if>
                    </xsl:when>
                    <xsl:when test="count(../dri:item) mod 2 = 1">
                        <xsl:if test="count(../dri:item) > 3">
                            <xsl:if test="(count(preceding-sibling::dri:item) mod 2 = 1)">even </xsl:if>
                            <xsl:if test="(count(preceding-sibling::dri:item) mod 2 = 0)">odd </xsl:if>
                        </xsl:if>
                    </xsl:when>
                </xsl:choose>
                <!-- The last row is special if it contains only buttons -->
                <xsl:if test="position()=last() and dri:field[@type='button'] and not(dri:field[not(@type='button')])">last </xsl:if>
                <!-- The row is also tagged specially if it contains another "form" list -->
                <xsl:if test="dri:list[@type='form']">sublist </xsl:if>
                </xsl:with-param>
            </xsl:call-template>

            <xsl:choose>
                <xsl:when test="dri:field[@type='composite']">
                    <xsl:call-template name="pick-label"/>
                    <xsl:apply-templates mode="formComposite"/>
                </xsl:when>
                <xsl:when test="dri:list[@type='form']">
                    <xsl:apply-templates />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template name="pick-label"/>
                    <div class="ds-form-content">
                        <xsl:apply-templates />
                        <!-- special name used in submission UI review page -->
                        <xsl:if test="@n = 'submit-review-field-with-authority'">
                          <xsl:call-template name="authorityConfidenceIcon">
                            <xsl:with-param name="confidence" select="substring-after(./@rend, 'cf-')"/>
                          </xsl:call-template>
                        </xsl:if>
                    </div>
                </xsl:otherwise>
            </xsl:choose>
        </li>
    </xsl:template>

    <!-- An item in a nested "form" list -->
    <xsl:template match="dri:list[@type='form']//dri:list[@type='form']/dri:item" priority="3">
        <li>
                <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">
                    <xsl:text>ds-form-item </xsl:text>

                <!-- Row counting voodoo, meant to impart consistent row alternation colors to the form lists.
                    Should probably be chnaged to a system that is more straigtforward. -->
                <xsl:choose>
                    <xsl:when test="(count(../../..//dri:item) - count(../../..//dri:list[@type='form'])) mod 2 = 0">
                        <!--<xsl:if test="count(../dri:item) > 3">-->
                            <xsl:if test="(count(preceding-sibling::dri:item | ../../preceding-sibling::dri:item/dri:list[@type='form']/dri:item) mod 2 = 0)">even </xsl:if>
                            <xsl:if test="(count(preceding-sibling::dri:item | ../../preceding-sibling::dri:item/dri:list[@type='form']/dri:item) mod 2 = 1)">odd </xsl:if>

                    </xsl:when>
                    <xsl:when test="(count(../../..//dri:item) - count(../../..//dri:list[@type='form'])) mod 2 = 1">
                        <!--<xsl:if test="count(../dri:item) > 3">-->
                            <xsl:if test="(count(preceding-sibling::dri:item | ../../preceding-sibling::dri:item/dri:list[@type='form']/dri:item) mod 2 = 1)">even </xsl:if>
                            <xsl:if test="(count(preceding-sibling::dri:item | ../../preceding-sibling::dri:item/dri:list[@type='form']/dri:item) mod 2 = 0)">odd </xsl:if>

                    </xsl:when>
                </xsl:choose>
                <!--
                <xsl:if test="position()=last() and dri:field[@type='button'] and not(dri:field[not(@type='button')])">last</xsl:if>
                    -->
               </xsl:with-param>
            </xsl:call-template>

            <xsl:call-template name="pick-label"/>

            <xsl:choose>
                <xsl:when test="dri:field[@type='composite']">
                    <xsl:apply-templates mode="formComposite"/>
                </xsl:when>
                <xsl:otherwise>
                    <div class="ds-form-content">
                        <xsl:apply-templates />
                        <!-- special name used in submission UI review page -->
                        <xsl:if test="@n = 'submit-review-field-with-authority'">
                          <xsl:call-template name="authorityConfidenceIcon">
                            <xsl:with-param name="confidence" select="substring-after(./@rend, 'cf-')"/>
                          </xsl:call-template>
                        </xsl:if>
                    </div>
                </xsl:otherwise>
            </xsl:choose>
        </li>
    </xsl:template>

    <xsl:template name="pick-label">
        <xsl:choose>
            <xsl:when test="dri:field/dri:label">
                <label class="ds-form-label">
                        <xsl:choose>
                                <xsl:when test="./dri:field/@id">
                                        <xsl:attribute name="for">
                                                <xsl:value-of select="translate(./dri:field/@id,'.','_')"/>
                                        </xsl:attribute>
                                </xsl:when>
                                <xsl:otherwise></xsl:otherwise>
                        </xsl:choose>
                    <xsl:apply-templates select="dri:field/dri:label" mode="formComposite"/>
                    <xsl:text>:</xsl:text>
                </label>
            </xsl:when>
            <xsl:when test="string-length(string(preceding-sibling::*[1][local-name()='label'])) > 0">
                <xsl:choose>
                        <xsl:when test="./dri:field/@id">
                                <label>
                                        <xsl:apply-templates select="preceding-sibling::*[1][local-name()='label']"/>
                                    <xsl:text>:</xsl:text>
                                </label>
                        </xsl:when>
                        <xsl:otherwise>
                                <span>
                                        <xsl:apply-templates select="preceding-sibling::*[1][local-name()='label']"/>
                                    <xsl:text>:</xsl:text>
                                </span>
                        </xsl:otherwise>
                </xsl:choose>

            </xsl:when>
            <xsl:when test="dri:field">
                <xsl:choose>
                        <xsl:when test="preceding-sibling::*[1][local-name()='label']">
                                <xsl:if test="string-length(string(preceding-sibling::*[1][local-name()='label'])) > 0">
                                    <label class="ds-form-label">
                                        <xsl:choose>
                                            <xsl:when test="./dri:field/@id">
                                                    <xsl:attribute name="for">
                                                            <xsl:value-of select="translate(./dri:field/@id,'.','_')"/>
                                                    </xsl:attribute>
                                            </xsl:when>
                                            <xsl:otherwise></xsl:otherwise>
                                        </xsl:choose>
                                        <xsl:apply-templates select="preceding-sibling::*[1][local-name()='label']"/>
                                    </label>
                                </xsl:if>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:apply-templates select="preceding-sibling::*[1][local-name()='label']"/>
                            </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <!-- If the label is empty and the item contains no field, omit the label. This is to
                    make the text inside the item (since what else but text can be there?) stretch across
                    both columns of the list. -->
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="dri:list[@type='form']/dri:label" priority="3">
                <xsl:attribute name="class">
                <xsl:text>ds-form-label</xsl:text>
               <xsl:if test="@rend">
                     <xsl:text> </xsl:text>
                     <xsl:value-of select="@rend"/>
                 </xsl:if>
        </xsl:attribute>
        <xsl:choose>
                <xsl:when test="following-sibling::dri:item[1]/dri:field/@id">
                        <xsl:attribute name="for">
                                <xsl:value-of select="translate(following-sibling::dri:item[1]/dri:field/@id,'.','_')" />
                        </xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                </xsl:otherwise>
        </xsl:choose>
        <xsl:apply-templates />
    </xsl:template>


    <xsl:template match="dri:field/dri:label" mode="formComposite">
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="dri:list[@type='form']/dri:head" priority="5">
        <legend>
                <xsl:apply-templates />
        </legend>
    </xsl:template>

    <!-- NON-instance composite fields (i.e. not repeatable) -->
    <xsl:template match="dri:field[@type='composite']" mode="formComposite">
        <div class="ds-form-content">
            <xsl:variable name="confidenceIndicatorID" select="concat(translate(@id,'.','_'),'_confidence_indicator')"/>
            <xsl:apply-templates select="dri:field" mode="compositeComponent"/>
            <xsl:choose>
              <xsl:when test="dri:params/@choicesPresentation = 'suggest'">
                <xsl:message terminate="yes">
                  <xsl:text>ERROR: Input field with "suggest" (autocomplete) choice behavior is not implemented for Composite (e.g. "name") fields.</xsl:text>
                </xsl:message>
              </xsl:when>
              <!-- lookup popup includes its own Add button if necessary. -->
              <xsl:when test="dri:params/@choicesPresentation = 'lookup'">
                <xsl:call-template name="addLookupButton">
                  <xsl:with-param name="isName" select="'true'"/>
                  <xsl:with-param name="confIndicator" select="$confidenceIndicatorID"/>
                </xsl:call-template>
              </xsl:when>
                <xsl:when test="dri:params/@choicesPresentation = 'authorLookup'">
                    <xsl:call-template name="addLookupButtonAuthor">
                        <xsl:with-param name="isName" select="'true'"/>
                        <xsl:with-param name="confIndicator" select="$confidenceIndicatorID"/>
                    </xsl:call-template>
                </xsl:when>
            </xsl:choose>
            <xsl:if test="dri:params/@authorityControlled">
              <xsl:variable name="confValue" select="dri:field/dri:value[@type='authority'][1]/@confidence"/>
              <xsl:call-template name="authorityConfidenceIcon">
                <xsl:with-param name="confidence" select="$confValue"/>
                <xsl:with-param name="id" select="$confidenceIndicatorID"/>
              </xsl:call-template>
              <xsl:call-template name="authorityInputFields">
                <xsl:with-param name="name" select="@n"/>
                <xsl:with-param name="authValue" select="dri:field/dri:value[@type='authority'][1]/text()"/>
                <xsl:with-param name="confValue" select="$confValue"/>
              </xsl:call-template>
            </xsl:if>
            <div class="spacer">&#160;</div>
            <xsl:apply-templates select="dri:field/dri:error" mode="compositeComponent"/>
            <xsl:apply-templates select="dri:error" mode="compositeComponent"/>
            <xsl:apply-templates select="dri:help" mode="compositeComponent"/>
        </div>
    </xsl:template>

    <!-- The handling of the special case of instanced composite fields under "form" lists -->
    <xsl:template match="dri:field[@type='composite'][dri:field/dri:instance | dri:params/@operations]" mode="formComposite" priority="2">
        <xsl:variable name="confidenceIndicatorID" select="concat(translate(@id,'.','_'),'_confidence_indicator')"/>
        <div class="ds-form-content">
            <xsl:apply-templates select="dri:field" mode="compositeComponent"/>
            <xsl:if test="contains(dri:params/@operations,'add')">
                <!-- Add buttons should be named "submit_[field]_add" so that we can ignore errors from required fields when simply adding new values-->
               <input type="submit" value="Add" name="{concat('submit_',@n,'_add')}" class="ds-button-field ds-add-button">
                  <!-- Make invisible if we have choice-lookup operation that provides its own Add. -->
                  <xsl:if test="dri:params/@choicesPresentation = 'lookup'">
                    <xsl:attribute name="style">
                      <xsl:text>display:none;</xsl:text>
                    </xsl:attribute>
            </xsl:if>
                   <xsl:if test="dri:params/@choicesPresentation = 'authorLookup'">
                       <xsl:attribute name="style">
                           <xsl:text>display:none;</xsl:text>
                       </xsl:attribute>
                   </xsl:if>
               </input>
            </xsl:if>
            <!-- insert choice mechansim and/or Add button here -->
            <xsl:choose>
              <xsl:when test="dri:params/@choicesPresentation = 'suggest'">
                <xsl:message terminate="yes">
                  <xsl:text>ERROR: Input field with "suggest" (autocomplete) choice behavior is not implemented for Composite (e.g. "name") fields.</xsl:text>
                </xsl:message>
              </xsl:when>
              <!-- lookup popup includes its own Add button if necessary. -->
              <xsl:when test="dri:params/@choicesPresentation = 'lookup'">
                <xsl:call-template name="addLookupButton">
                  <xsl:with-param name="isName" select="'true'"/>
                  <xsl:with-param name="confIndicator" select="$confidenceIndicatorID"/>
                </xsl:call-template>
              </xsl:when>
                <xsl:when test="dri:params/@choicesPresentation = 'authorLookup'">
                    <xsl:call-template name="addLookupButtonAuthor">
                        <xsl:with-param name="isName" select="'true'"/>
                        <xsl:with-param name="confIndicator" select="$confidenceIndicatorID"/>
                    </xsl:call-template>
                </xsl:when>
            </xsl:choose>
            <!-- place to store authority value -->
            <xsl:if test="dri:params/@authorityControlled">
              <xsl:call-template name="authorityConfidenceIcon">
                <xsl:with-param name="confidence" select="dri:value[@type='authority']/@confidence"/>
                <xsl:with-param name="id" select="$confidenceIndicatorID"/>
              </xsl:call-template>
              <xsl:call-template name="authorityInputFields">
                <xsl:with-param name="name" select="@n"/>
                <xsl:with-param name="authValue" select="dri:value[@type='authority']/text()"/>
                <xsl:with-param name="confValue" select="dri:value[@type='authority']/@confidence"/>
              </xsl:call-template>
            </xsl:if>
            <div class="spacer">&#160;</div>
            <xsl:apply-templates select="dri:field/dri:error" mode="compositeComponent"/>
            <xsl:apply-templates select="dri:error" mode="compositeComponent"/>
            <xsl:apply-templates select="dri:help" mode="compositeComponent"/>
            <xsl:if test="dri:instance or dri:field/dri:instance">
                <div class="ds-previous-values">
                    <xsl:call-template name="fieldIterator">
                        <xsl:with-param name="position">1</xsl:with-param>
                    </xsl:call-template>
                    <xsl:if test="contains(dri:params/@operations,'delete') and (dri:instance or dri:field/dri:instance)">
                        <!-- Delete buttons should be named "submit_[field]_delete" so that we can ignore errors from required fields when simply removing values-->
                        <input type="submit" value="Remove selected" name="{concat('submit_',@n,'_delete')}" class="ds-button-field ds-delete-button" />
                    </xsl:if>
                    <xsl:for-each select="dri:field">
                        <xsl:apply-templates select="dri:instance" mode="hiddenInterpreter"/>
                    </xsl:for-each>
                </div>
            </xsl:if>
        </div>
    </xsl:template>




    <!-- TODO: The field section works but needs a lot of scrubbing. I would say a third of the
        templates involved are either bloated or superfluous. -->


    <!-- Things I know:
        1. I can tell a field is multivalued if it has instances in it
        2. I can't really do that for composites, although I can check its
            component fields for condition 1 above.
        3. Fields can also be inside "form" lists, which is its own unique condition
    -->

    <!-- Fieldset (instanced) field stuff, in the case of non-composites -->
    <xsl:template match="dri:field[dri:field/dri:instance | dri:params/@operations]" priority="2">
        <!-- Create the first field normally -->
        <xsl:apply-templates select="." mode="normalField"/>
        <!-- Follow it up with an ADD button if the add operation is specified. This allows
            entering more than one value for this field. -->
        <xsl:if test="contains(dri:params/@operations,'add')">
            <!-- Add buttons should be named "submit_[field]_add" so that we can ignore errors from required fields when simply adding new values-->
            <input type="submit" value="Add" name="{concat('submit_',@n,'_add')}" class="ds-button-field ds-add-button">
              <!-- Make invisible if we have choice-lookup popup that provides its own Add. -->
              <xsl:if test="dri:params/@choicesPresentation = 'lookup'">
                <xsl:attribute name="style">
                  <xsl:text>display:none;</xsl:text>
                </xsl:attribute>
        </xsl:if>
                <xsl:if test="dri:params/@choicesPresentation = 'authorLookup'">
                    <xsl:attribute name="style">
                        <xsl:text>display:none;</xsl:text>
                    </xsl:attribute>
                </xsl:if>
           </input>
        </xsl:if>
        <br/>
        <xsl:apply-templates select="dri:help" mode="help"/>
        <xsl:apply-templates select="dri:error" mode="error"/>
        <xsl:if test="dri:instance">
            <div class="ds-previous-values">
                <!-- Iterate over the dri:instance elements contained in this field. The instances contain
                    stored values as either "interpreted", "raw", or "default" values. -->
                <xsl:call-template name="simpleFieldIterator">
                    <xsl:with-param name="position">1</xsl:with-param>
                </xsl:call-template>
                <!-- Conclude with a DELETE button if the delete operation is specified. This allows
                    removing one or more values stored for this field. -->
                <xsl:if test="contains(dri:params/@operations,'delete') and dri:instance">
                    <!-- Delete buttons should be named "submit_[field]_delete" so that we can ignore errors from required fields when simply removing values-->
                    <input type="submit" value="Remove selected" name="{concat('submit_',@n,'_delete')}" class="ds-button-field ds-delete-button" />
                </xsl:if>
                <!-- Behind the scenes, add hidden fields for every instance set. This is to make sure that
                    the form still submits the information in those instances, even though they are no
                    longer encoded as HTML fields. The DRI Reference should contain the exact attributes
                    the hidden fields should have in order for this to work properly. -->
                <xsl:apply-templates select="dri:instance" mode="hiddenInterpreter"/>
            </div>
        </xsl:if>
    </xsl:template>

    <!-- The iterator is a recursive function that creates a checkbox (to be used in deletion) for
        each value instance and interprets the value inside. It also creates a hidden field from the
        raw value contained in the instance. -->
    <xsl:template name="simpleFieldIterator">
        <xsl:param name="position"/>
        <xsl:if test="dri:instance[position()=$position]">
            <input type="checkbox" value="{concat(@n,'_',$position)}" name="{concat(@n,'_selected')}"/>
            <xsl:apply-templates select="dri:instance[position()=$position]" mode="interpreted"/>

            <!-- look for authority value in instance. -->
            <xsl:if test="dri:instance[position()=$position]/dri:value[@type='authority']">
              <xsl:call-template name="authorityConfidenceIcon">
                <xsl:with-param name="confidence" select="dri:instance[position()=$position]/dri:value[@type='authority']/@confidence"/>
              </xsl:call-template>
            </xsl:if>
            <br/>
            <xsl:call-template name="simpleFieldIterator">
                <xsl:with-param name="position"><xsl:value-of select="$position + 1"/></xsl:with-param>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <!-- Authority: added fields for auth values as well. -->
    <!-- Common case: use the raw value of the instance to create the hidden field -->
    <xsl:template match="dri:instance" mode="hiddenInterpreter">
        <input type="hidden">
            <xsl:attribute name="name"><xsl:value-of select="concat(../@n,'_',position())"/></xsl:attribute>
            <xsl:attribute name="value">
                <xsl:value-of select="dri:value[@type='raw']"/>
            </xsl:attribute>
        </input>
        <!-- XXX do we want confidence icon here?? -->
        <xsl:if test="dri:value[@type='authority']">
          <xsl:call-template name="authorityInputFields">
            <xsl:with-param name="name" select="../@n"/>
            <xsl:with-param name="position" select="position()"/>
            <xsl:with-param name="authValue" select="dri:value[@type='authority']/text()"/>
            <xsl:with-param name="confValue" select="dri:value[@type='authority']/@confidence"/>
          </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <!-- Select box case: use the selected options contained in the instance to create the hidden fields -->
    <xsl:template match="dri:field[@type='select']/dri:instance" mode="hiddenInterpreter">
        <xsl:variable name="position" select="position()"/>
        <xsl:for-each select="dri:value[@type='option']">
            <input type="hidden">
                <xsl:attribute name="name">
                    <xsl:value-of select="concat(../../@n,'_',$position)"/>
                </xsl:attribute>
                <!-- Since the dri:option and dri:values inside a select field are related by the return
                    value, encoded in @returnValue and @option attributes respectively, the option
                    attribute can be used directly instead of being resolved to the the correct option -->
                <xsl:attribute name="value">
                    <!--<xsl:value-of select="../../dri:option[@returnValue = current()/@option]"/>-->
                    <xsl:value-of select="@option"/>
                </xsl:attribute>
            </input>
        </xsl:for-each>
    </xsl:template>



    <!-- Composite instanced field stuff -->
    <!-- It is also the one that receives the special error and help handling -->
    <xsl:template match="dri:field[@type='composite'][dri:field/dri:instance | dri:params/@operations]" priority="3">
        <!-- First is special, so first we grab all the values from the child fields.
            We do this by applying normal templates to the field, which should ignore instances. -->
        <span class="ds-composite-field">
            <xsl:apply-templates select="dri:field" mode="compositeComponent"/>
        </span>
        <xsl:apply-templates select="dri:field/dri:error" mode="compositeComponent"/>
        <xsl:apply-templates select="dri:error" mode="compositeComponent"/>
        <xsl:apply-templates select="dri:help" mode="compositeComponent"/>
        <!-- Insert choice mechanism here.
             Follow it up with an ADD button if the add operation is specified. This allows
            entering more than one value for this field. -->

        <xsl:if test="contains(dri:params/@operations,'add')">
            <!-- Add buttons should be named "submit_[field]_add" so that we can ignore errors from required fields when simply adding new values-->
           <input type="submit" value="Add" name="{concat('submit_',@n,'_add')}" class="ds-button-field ds-add-button">
              <!-- Make invisible if we have choice-lookup popup that provides its own Add. -->
              <xsl:if test="dri:params/@choicesPresentation = 'lookup'">
                <xsl:attribute name="style">
                  <xsl:text>display:none;</xsl:text>
                </xsl:attribute>
        </xsl:if>
               <xsl:if test="dri:params/@choicesPresentation = 'authorLookup'">
                   <xsl:attribute name="style">
                       <xsl:text>display:none;</xsl:text>
                   </xsl:attribute>
               </xsl:if>
           </input>
        </xsl:if>

        <xsl:variable name="confidenceIndicatorID" select="concat(translate(@id,'.','_'),'_confidence_indicator')"/>
        <xsl:if test="dri:params/@authorityControlled">
          <!-- XXX note that this is wrong and won't get any authority values, but
             - for instanced inputs the entry box starts out empty anyway.
            -->
          <xsl:call-template name="authorityConfidenceIcon">
            <xsl:with-param name="confidence" select="dri:value[@type='authority']/@confidence"/>
            <xsl:with-param name="id" select="$confidenceIndicatorID"/>
          </xsl:call-template>
          <xsl:call-template name="authorityInputFields">
            <xsl:with-param name="name" select="@n"/>
            <xsl:with-param name="id" select="@id"/>
            <xsl:with-param name="authValue" select="dri:value[@type='authority']/text()"/>
            <xsl:with-param name="confValue" select="dri:value[@type='authority']/@confidence"/>
          </xsl:call-template>
        </xsl:if>
        <xsl:choose>
          <xsl:when test="dri:params/@choicesPresentation = 'suggest'">
            <xsl:call-template name="addAuthorityAutocomplete">
              <xsl:with-param name="confidenceIndicatorID" select="$confidenceIndicatorID"/>
            </xsl:call-template>
          </xsl:when>
          <!-- lookup popup includes its own Add button if necessary. -->
          <!-- XXX does this need a Confidence Icon? -->
          <xsl:when test="dri:params/@choicesPresentation = 'lookup'">
            <xsl:call-template name="addLookupButton">
              <xsl:with-param name="isName" select="'true'"/>
              <xsl:with-param name="confIndicator" select="$confidenceIndicatorID"/>
            </xsl:call-template>
          </xsl:when>
            <xsl:when test="dri:params/@choicesPresentation = 'authorLookup'">
                <xsl:call-template name="addLookupButtonAuthor">
                    <xsl:with-param name="isName" select="'true'"/>
                    <xsl:with-param name="confIndicator" select="$confidenceIndicatorID"/>
                </xsl:call-template>
            </xsl:when>
        </xsl:choose>
        <br/>
        <xsl:if test="dri:instance or dri:field/dri:instance">
            <div class="ds-previous-values">
                <xsl:call-template name="fieldIterator">
                    <xsl:with-param name="position">1</xsl:with-param>
                </xsl:call-template>
                <!-- Conclude with a DELETE button if the delete operation is specified. This allows
                    removing one or more values stored for this field. -->
                <xsl:if test="contains(dri:params/@operations,'delete') and (dri:instance or dri:field/dri:instance)">
                    <!-- Delete buttons should be named "submit_[field]_delete" so that we can ignore errors from required fields when simply removing values-->
                    <input type="submit" value="Remove selected" name="{concat('submit_',@n,'_delete')}" class="ds-button-field ds-delete-button" />
                </xsl:if>
                <xsl:for-each select="dri:field">
                    <xsl:apply-templates select="dri:instance" mode="hiddenInterpreter"/>
                </xsl:for-each>
            </div>
        </xsl:if>
    </xsl:template>

    <!-- The iterator is a recursive function that creates a checkbox (to be used in deletion) for
        each value instance and interprets the value inside. It also creates a hidden field from the
        raw value contained in the instance.

         What makes it different from the simpleFieldIterator is that it works with a composite field's
        components rather than a single field, which requires it to consider several sets of instances. -->
    <xsl:template name="fieldIterator">
        <xsl:param name="position"/>
        <!-- add authority value for this instance -->
        <xsl:if test="dri:instance[position()=$position]/dri:value[@type='authority']">
          <xsl:call-template name="authorityInputFields">
            <xsl:with-param name="name" select="@n"/>
            <xsl:with-param name="position" select="$position"/>
            <xsl:with-param name="authValue" select="dri:instance[position()=$position]/dri:value[@type='authority']/text()"/>
            <xsl:with-param name="confValue" select="dri:instance[position()=$position]/dri:value[@type='authority']/@confidence"/>
          </xsl:call-template>
        </xsl:if>
        <xsl:choose>
            <!-- First check to see if the composite itself has a non-empty instance value in that
                position. In that case there is no need to go into the individual fields. -->
            <xsl:when test="count(dri:instance[position()=$position]/dri:value[@type != 'authority'])">
                <input type="checkbox" value="{concat(@n,'_',$position)}" name="{concat(@n,'_selected')}"/>
                <xsl:apply-templates select="dri:instance[position()=$position]" mode="interpreted"/>
                <xsl:call-template name="authorityConfidenceIcon">
                  <xsl:with-param name="confidence" select="dri:instance[position()=$position]/dri:value[@type='authority']/@confidence"/>
                </xsl:call-template>
                <br/>
                <xsl:call-template name="fieldIterator">
                    <xsl:with-param name="position"><xsl:value-of select="$position + 1"/></xsl:with-param>
                </xsl:call-template>
            </xsl:when>
            <!-- Otherwise, build the string from the component fields -->
            <xsl:when test="dri:field/dri:instance[position()=$position]">
                <input type="checkbox" value="{concat(@n,'_',$position)}" name="{concat(@n,'_selected')}"/>
                <xsl:apply-templates select="dri:field" mode="compositeField">
                    <xsl:with-param name="position" select="$position"/>
                </xsl:apply-templates>
                <br/>
                <xsl:call-template name="fieldIterator">
                    <xsl:with-param name="position"><xsl:value-of select="$position + 1"/></xsl:with-param>
                </xsl:call-template>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="dri:field[@type='text' or @type='textarea']" mode="compositeField">
        <xsl:param name="position">1</xsl:param>
        <xsl:if test="not(position()=1)">
            <xsl:text>, </xsl:text>
        </xsl:if>
        <!--<input type="hidden" name="{concat(@n,'_',$position)}" value="{dri:instance[position()=$position]/dri:value[@type='raw']}"/>-->
        <xsl:choose>
            <xsl:when test="dri:instance[position()=$position]/dri:value[@type='interpreted']">
                <span class="ds-interpreted-field"><xsl:apply-templates select="dri:instance[position()=$position]/dri:value[@type='interpreted']" mode="interpreted"/></span>
            </xsl:when>
            <xsl:when test="dri:instance[position()=$position]/dri:value[@type='raw']">
                <span class="ds-interpreted-field"><xsl:apply-templates select="dri:instance[position()=$position]/dri:value[@type='raw']" mode="interpreted"/></span>
            </xsl:when>
            <xsl:when test="dri:instance[position()=$position]/dri:value[@type='default']">
                <span class="ds-interpreted-field"><xsl:apply-templates select="dri:instance[position()=$position]/dri:value[@type='default']" mode="interpreted"/></span>
            </xsl:when>
            <xsl:otherwise>
                <span class="ds-interpreted-field">No value submitted.</span>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <xsl:template match="dri:field[@type='select']" mode="compositeField">
        <xsl:param name="position">1</xsl:param>
        <xsl:if test="not(position()=1)">
            <xsl:text>, </xsl:text>
        </xsl:if>
        <xsl:for-each select="dri:instance[position()=$position]/dri:value[@type='option']">
            <!--<input type="hidden" name="{concat(@n,'_',$position)}" value="{../../dri:option[@returnValue = current()/@option]}"/>-->
        </xsl:for-each>
        <xsl:choose>
            <xsl:when test="dri:instance[position()=$position]/dri:value[@type='interpreted']">
                <span class="ds-interpreted-field"><xsl:apply-templates select="dri:instance[position()=$position]/dri:value[@type='interpreted']" mode="interpreted"/></span>
            </xsl:when>
            <xsl:when test="dri:instance[position()=$position]/dri:value[@type='option']">
                <span class="ds-interpreted-field">
                    <xsl:for-each select="dri:instance[position()=$position]/dri:value[@type='option']">
                        <xsl:if test="position()=1">
                            <xsl:text>(</xsl:text>
                        </xsl:if>
                        <xsl:value-of select="../../dri:option[@returnValue = current()/@option]"/>
                        <xsl:if test="position()=last()">
                            <xsl:text>)</xsl:text>
                        </xsl:if>
                        <xsl:if test="not(position()=last())">
                            <xsl:text>, </xsl:text>
                        </xsl:if>
                    </xsl:for-each>
                </span>
            </xsl:when>
            <xsl:otherwise>
                <span class="ds-interpreted-field">No value submitted.</span>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- TODO: make this work? Maybe checkboxes and radio buttons should not be instanced... -->
    <xsl:template match="dri:field[@type='checkbox' or @type='radio']" mode="compositeField">
        <xsl:param name="position">1</xsl:param>
        <xsl:if test="not(position()=1)">
            <xsl:text>, </xsl:text>
        </xsl:if>
        <span class="ds-interpreted-field">Checkbox</span>
    </xsl:template>










    <xsl:template match="dri:field[@type='select']/dri:instance" mode="interpreted">
        <span class="ds-interpreted-field">
            <xsl:for-each select="dri:value[@type='option']">
                <!--<input type="hidden" name="{concat(../@n,'_',position())}" value="{../../dri:option[@returnValue = current()/@option]}"/>-->
                <xsl:if test="position()=1">
                    <xsl:text>(</xsl:text>
                </xsl:if>
                <xsl:value-of select="../../dri:option[@returnValue = current()/@option]"/>
                <xsl:if test="position()=last()">
                    <xsl:text>)</xsl:text>
                </xsl:if>
                <xsl:if test="not(position()=last())">
                    <xsl:text>, </xsl:text>
                </xsl:if>
            </xsl:for-each>
        </span>
    </xsl:template>


    <xsl:template match="dri:instance" mode="interpreted">
        <!--<input type="hidden" name="{concat(../@n,'_',position())}" value="dri:value[@type='raw']"/>-->
        <xsl:choose>
            <xsl:when test="dri:value[@type='interpreted']">
                <span class="ds-interpreted-field"><xsl:apply-templates select="dri:value[@type='interpreted']" mode="interpreted"/></span>
            </xsl:when>
            <xsl:when test="dri:value[@type='raw']">
                <span class="ds-interpreted-field"><xsl:apply-templates select="dri:value[@type='raw']" mode="interpreted"/></span>
            </xsl:when>
            <xsl:when test="dri:value[@type='default']">
                <span class="ds-interpreted-field"><xsl:apply-templates select="dri:value[@type='default']" mode="interpreted"/></span>
            </xsl:when>
            <xsl:otherwise>
                <span class="ds-interpreted-field">No value submitted.</span>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>





    <xsl:template match="dri:value" mode="interpreted">
        <xsl:apply-templates />
    </xsl:template>

    <!--
    <xsl:template match="dri:field">

        Possible child elements:
        params(one), help(zero or one), error(zero or one), value(any), field(one or more – only with the composite type)

        Possible attributes:
        @n, @id, @rend
        @disabled
        @required
        @type =
            button: A button input control that when activated by the user will submit the form, including all the fields, back to the server for processing.
            checkbox: A boolean input control which may be toggled by the user. A checkbox may have several fields which share the same name and each of those fields may be toggled independently. This is distinct from a radio button where only one field may be toggled.
            file: An input control that allows the user to select files to be submitted with the form. Note that a form which uses a file field must use the multipart method.
            hidden: An input control that is not rendered on the screen and hidden from the user.
            password: A single-line text input control where the input text is rendered in such a way as to hide the characters from the user.
            radio:  A boolean input control which may be toggled by the user. Multiple radio button fields may share the same name. When this occurs only one field may be selected to be true. This is distinct from a checkbox where multiple fields may be toggled.
            select: A menu input control which allows the user to select from a list of available options.
            text: A single-line text input control.
            textarea: A multi-line text input control.
            composite: A composite input control combines several input controls into a single field. The only fields that may be combined together are: checkbox, password, select, text, and textarea. When fields are combined together they can possess multiple combined values.
    </xsl:template>
        -->



    <!-- The handling of component fields, that is fields that are part of a composite field type -->
    <xsl:template match="dri:field" mode="compositeComponent">
        <xsl:choose>
                <xsl:when test="@type = 'checkbox'  or @type='radio'">
                    <xsl:apply-templates select="." mode="normalField"/>
                    <xsl:if test="dri:label">
                        <br/>
                        <xsl:apply-templates select="dri:label" mode="compositeComponent"/>
                    </xsl:if>
                </xsl:when>
                <xsl:otherwise>
                        <label class="ds-composite-component">
                            <xsl:if test="position()=last()">
                                <xsl:attribute name="class">ds-composite-component last</xsl:attribute>
                            </xsl:if>
                            <xsl:apply-templates select="." mode="normalField"/>
                            <xsl:if test="dri:label">
                                <br/>
                                <xsl:apply-templates select="dri:label" mode="compositeComponent"/>
                            </xsl:if>
                        </label>
                </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="dri:error" mode="compositeComponent">
        <xsl:apply-templates select="." mode="error"/>
    </xsl:template>

    <xsl:template match="dri:help" mode="compositeComponent">
        <span class="composite-help"><xsl:apply-templates /></span>
    </xsl:template>



    <!-- The handling of the field element is more complex. At the moment, the handling of input fields in the
        DRI schema is very similar to HTML, utilizing the same controlled vocabulary in most cases. This makes
        converting DRI fields to HTML inputs a straightforward, if a bit verbose, task. We are currently
        looking at other ways of encoding forms, so this may change in the future. -->
    <!-- The simple field case... not part of a complex field and does not contain instance values -->
    <xsl:template match="dri:field">
        <xsl:apply-templates select="." mode="normalField"/>
        <xsl:if test="not(@type='composite') and ancestor::dri:list[@type='form']">
            <!--
            <xsl:if test="not(@type='checkbox') and not(@type='radio') and not(@type='button')">
                <br/>
            </xsl:if>
            -->
            <xsl:apply-templates select="dri:help" mode="help"/>
            <xsl:apply-templates select="dri:error" mode="error"/>
        </xsl:if>
    </xsl:template>

    <xsl:template match="dri:field" mode="normalField">
        <xsl:variable name="confidenceIndicatorID" select="concat(translate(@id,'.','_'),'_confidence_indicator')"/>
        <xsl:choose>
            <!-- TODO: this has changed dramatically (see form3.xml) -->
                        <xsl:when test="@type= 'select'">
                                <select>
                                    <xsl:call-template name="fieldAttributes"/>
                                    <xsl:apply-templates/>
                                </select>
                        </xsl:when>
            <xsl:when test="@type= 'textarea'">
                                <textarea>
                                    <xsl:call-template name="fieldAttributes"/>
                                    <xsl:attribute name="onkeydown">event.cancelBubble=true;</xsl:attribute>

                                    <!--
                                        if the cols and rows attributes are not defined we need to call
                                        the templates for them since they are required attributes in strict xhtml
                                     -->
                                    <xsl:choose>
                                        <xsl:when test="not(./dri:params[@cols])">
                                                        <xsl:call-template name="textAreaCols"/>
                                        </xsl:when>
                                    </xsl:choose>
                                    <xsl:choose>
                                        <xsl:when test="not(./dri:params[@rows])">
                                                <xsl:call-template name="textAreaRows"/>
                                        </xsl:when>
                                    </xsl:choose>

                                    <xsl:apply-templates />
                                    <xsl:choose>
                                        <xsl:when test="./dri:value[@type='raw']">
                                            <xsl:copy-of select="./dri:value[@type='raw']/node()"/>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:copy-of select="./dri:value[@type='default']/node()"/>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                    <xsl:if  test="string-length(./dri:value) &lt; 1">
                                       <i18n:text>xmlui.dri2xhtml.default.textarea.value</i18n:text>
                                    </xsl:if>
                                </textarea>


              <!-- add place to store authority value -->
              <xsl:if test="dri:params/@authorityControlled">
                <xsl:variable name="confidence">
                  <xsl:if test="./dri:value[@type='authority']">
                   <xsl:value-of select="./dri:value[@type='authority']/@confidence"/>
                  </xsl:if>
                </xsl:variable>
                <!-- add authority confidence widget -->
                <xsl:call-template name="authorityConfidenceIcon">
                  <xsl:with-param name="confidence" select="$confidence"/>
                  <xsl:with-param name="id" select="$confidenceIndicatorID"/>
                </xsl:call-template>
                <xsl:call-template name="authorityInputFields">
                  <xsl:with-param name="name" select="@n"/>
                  <xsl:with-param name="id" select="@id"/>
                  <xsl:with-param name="authValue" select="dri:value[@type='authority']/text()"/>
                  <xsl:with-param name="confValue" select="dri:value[@type='authority']/@confidence"/>
                  <xsl:with-param name="confIndicatorID" select="$confidenceIndicatorID"/>
                  <xsl:with-param name="unlockButton" select="dri:value[@type='authority']/dri:field[@rend='ds-authority-lock']/@n"/>
                  <xsl:with-param name="unlockHelp" select="dri:value[@type='authority']/dri:field[@rend='ds-authority-lock']/dri:help"/>
                </xsl:call-template>
              </xsl:if>
              <!-- add choice mechanisms -->
              <xsl:choose>
                <xsl:when test="dri:params/@choicesPresentation = 'suggest'">
                  <xsl:call-template name="addAuthorityAutocomplete">
                    <xsl:with-param name="confidenceIndicatorID" select="$confidenceIndicatorID"/>
                    <xsl:with-param name="confidenceName">
                      <xsl:value-of select="concat(@n,'_confidence')"/>
                    </xsl:with-param>
                  </xsl:call-template>
            </xsl:when>
                <xsl:when test="dri:params/@choicesPresentation = 'lookup'">
                  <xsl:call-template name="addLookupButton">
                    <xsl:with-param name="isName" select="'false'"/>
                    <xsl:with-param name="confIndicator" select="$confidenceIndicatorID"/>
                  </xsl:call-template>
                </xsl:when>
                  <xsl:when test="dri:params/@choicesPresentation = 'authorLookup'">
                      <xsl:call-template name="addLookupButtonAuthor">
                          <xsl:with-param name="isName" select="'false'"/>
                          <xsl:with-param name="confIndicator" select="$confidenceIndicatorID"/>
                      </xsl:call-template>
                  </xsl:when>
              </xsl:choose>
            </xsl:when>

            <!-- This is changing dramatically -->
            <xsl:when test="@type= 'checkbox' or @type= 'radio'">
                <fieldset>
                    <xsl:call-template name="standardAttributes">
                                    <xsl:with-param name="class">
                                        <xsl:text>ds-</xsl:text><xsl:value-of select="@type"/><xsl:text>-field </xsl:text>
                                        <xsl:if test="dri:error">
                                            <xsl:text>error </xsl:text>
                                        </xsl:if>
                                    </xsl:with-param>
                                </xsl:call-template>
                                <xsl:attribute name="id"><xsl:value-of select="generate-id()"/></xsl:attribute>
                    <xsl:if test="dri:label">
                        <legend><xsl:apply-templates select="dri:label" mode="compositeComponent" /></legend>
                    </xsl:if>
                    <xsl:apply-templates />
                </fieldset>
            </xsl:when>
            <!--
                <input>
                            <xsl:call-template name="fieldAttributes"/>
                    <xsl:if test="dri:value[@checked='yes']">
                                <xsl:attribute name="checked">checked</xsl:attribute>
                    </xsl:if>
                    <xsl:apply-templates/>
                </input>
                -->
            <xsl:when test="@type= 'composite'">
                <!-- TODO: add error and help stuff on top of the composite -->
                <span class="ds-composite-field">
                    <xsl:apply-templates select="dri:field" mode="compositeComponent"/>
                </span>
                <xsl:apply-templates select="dri:field/dri:error" mode="compositeComponent"/>
                <xsl:apply-templates select="dri:error" mode="compositeComponent"/>
                <xsl:apply-templates select="dri:field/dri:help" mode="compositeComponent"/>
                <!--<xsl:apply-templates select="dri:help" mode="compositeComponent"/>-->
            </xsl:when>
                    <!-- text, password, file, and hidden types are handled the same.
                        Buttons: added the xsl:if check which will override the type attribute button
                            with the value 'submit'. No reset buttons for now...
                    -->
                    <xsl:otherwise>
                        <input>
                            <xsl:call-template name="fieldAttributes"/>
                            <xsl:if test="@type='button'">
                                <xsl:attribute name="type">submit</xsl:attribute>
                            </xsl:if>
                            <xsl:attribute name="value">
                                <xsl:choose>
                                    <xsl:when test="./dri:value[@type='raw']">
                                        <xsl:value-of select="./dri:value[@type='raw']"/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of select="./dri:value[@type='default']"/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:attribute>
                            <xsl:if test="dri:value/i18n:text">
                                <xsl:attribute name="i18n:attr">value</xsl:attribute>
                            </xsl:if>
                            <xsl:apply-templates />
                        </input>

                        <xsl:variable name="confIndicatorID" select="concat(@id,'_confidence_indicator')"/>
                        <xsl:if test="dri:params/@authorityControlled">
                          <xsl:variable name="confidence">
                            <xsl:if test="./dri:value[@type='authority']">
                              <xsl:value-of select="./dri:value[@type='authority']/@confidence"/>
                            </xsl:if>
                          </xsl:variable>
                          <!-- add authority confidence widget -->
                          <xsl:call-template name="authorityConfidenceIcon">
                            <xsl:with-param name="confidence" select="$confidence"/>
                            <xsl:with-param name="id" select="$confidenceIndicatorID"/>
                          </xsl:call-template>
                          <xsl:call-template name="authorityInputFields">
                            <xsl:with-param name="name" select="@n"/>
                            <xsl:with-param name="id" select="@id"/>
                            <xsl:with-param name="authValue" select="dri:value[@type='authority']/text()"/>
                            <xsl:with-param name="confValue" select="dri:value[@type='authority']/@confidence"/>
                          </xsl:call-template>
                        </xsl:if>
                        <xsl:choose>
                          <xsl:when test="dri:params/@choicesPresentation = 'suggest'">
                            <xsl:call-template name="addAuthorityAutocomplete">
                              <xsl:with-param name="confidenceIndicatorID" select="$confidenceIndicatorID"/>
                              <xsl:with-param name="confidenceName">
                                <xsl:value-of select="concat(@n,'_confidence')"/>
                              </xsl:with-param>
                            </xsl:call-template>
                          </xsl:when>
                          <xsl:when test="dri:params/@choicesPresentation = 'lookup'">
                            <xsl:call-template name="addLookupButton">
                              <xsl:with-param name="isName" select="'false'"/>
                              <xsl:with-param name="confIndicator" select="$confidenceIndicatorID"/>
                            </xsl:call-template>
                          </xsl:when>
                            <xsl:when test="dri:params/@choicesPresentation = 'authorLookup'">
                                <xsl:call-template name="addLookupButtonAuthor">
                                    <xsl:with-param name="isName" select="'false'"/>
                                    <xsl:with-param name="confIndicator" select="$confidenceIndicatorID"/>
                                </xsl:call-template>
                            </xsl:when>
                        </xsl:choose>
                    </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- A set of standard attributes common to all fields -->
    <xsl:template name="fieldAttributes">
        <xsl:call-template name="standardAttributes">
            <xsl:with-param name="class">
                <xsl:text>ds-</xsl:text><xsl:value-of select="@type"/><xsl:text>-field </xsl:text>
                <xsl:if test="dri:error">
                    <xsl:text>error </xsl:text>
                </xsl:if>
            </xsl:with-param>
        </xsl:call-template>
        <xsl:if test="@disabled='yes'">
            <xsl:attribute name="disabled">disabled</xsl:attribute>
        </xsl:if>
        <xsl:if test="@type != 'checkbox' and @type != 'radio' ">
                <xsl:attribute name="name"><xsl:value-of select="@n"/></xsl:attribute>
        </xsl:if>
        <xsl:if test="@type != 'select' and @type != 'textarea' and @type != 'checkbox' and @type != 'radio' ">
                <xsl:attribute name="type"><xsl:value-of select="@type"/></xsl:attribute>
        </xsl:if>
        <xsl:if test="@type= 'textarea'">
                <xsl:attribute name="onfocus">javascript:tFocus(this);</xsl:attribute>
        </xsl:if>
    </xsl:template>

    <!-- Since the field element contains only the type attribute, all other attributes commonly associated
        with input fields are stored on the params element. Rather than parse the attributes directly, this
        template generates a call to attribute templates, something that is not done in XSL by default. The
        templates for the attributes can be found further down. -->
    <xsl:template match="dri:params">
        <xsl:apply-templates select="@*"/>
    </xsl:template>


    <xsl:template match="dri:field[@type='select']/dri:option">
        <option>
            <xsl:attribute name="value"><xsl:value-of select="@returnValue"/></xsl:attribute>
            <xsl:if test="../dri:value[@type='option'][@option = current()/@returnValue]">
                <xsl:attribute name="selected">selected</xsl:attribute>
            </xsl:if>
            <xsl:apply-templates />
        </option>
    </xsl:template>

    <xsl:template match="dri:field[@type='checkbox' or @type='radio']/dri:option">
        <label>
            <input>
                <xsl:attribute name="name"><xsl:value-of select="../@n"/></xsl:attribute>
                <xsl:attribute name="type"><xsl:value-of select="../@type"/></xsl:attribute>
                <xsl:attribute name="value"><xsl:value-of select="@returnValue"/></xsl:attribute>
                <xsl:if test="../dri:value[@type='option'][@option = current()/@returnValue]">
                    <xsl:attribute name="checked">checked</xsl:attribute>
                </xsl:if>
                <xsl:if test="../@disabled='yes'">
                    <xsl:attribute name="disabled">disabled</xsl:attribute>
                </xsl:if>
            </input>
            <xsl:apply-templates />
        </label>
    </xsl:template>



    <!-- A special case for the value element under field of type 'select'. Instead of being used to create
        the value attribute of an HTML input tag, these are used to create selection options.
    <xsl:template match="dri:field[@type='select']/dri:value" priority="2">
        <option>
            <xsl:attribute name="value"><xsl:value-of select="@optionValue"/></xsl:attribute>
            <xsl:if test="@optionSelected = 'yes'">
                <xsl:attribute name="selected">selected</xsl:attribute>
            </xsl:if>
            <xsl:apply-templates />
        </option>
    </xsl:template>-->

    <!-- In general cases the value of this element is used directly, so the template does nothing. -->
    <xsl:template match="dri:value" priority="1">
    </xsl:template>

    <!-- The field label is usually invoked directly by a higher level tag, so this template does nothing. -->
    <xsl:template match="dri:field/dri:label" priority="2">
    </xsl:template>

    <xsl:template match="dri:field/dri:label" mode="compositeComponent">
        <xsl:apply-templates />
    </xsl:template>

    <!-- The error field handling -->
    <xsl:template match="dri:error">
        <xsl:attribute name="title"><xsl:value-of select="."/></xsl:attribute>
        <xsl:if test="i18n:text">
            <xsl:attribute name="i18n:attr">title</xsl:attribute>
        </xsl:if>
    </xsl:template>

    <xsl:template match="dri:error" mode="error">
        <span class="error">* <xsl:apply-templates/></span>
    </xsl:template>



    <!-- Help elements are turning into tooltips. There might be a better way to do this -->
    <xsl:template match="dri:help">
        <xsl:attribute name="title"><xsl:value-of select="."/></xsl:attribute>
        <xsl:if test="i18n:text">
            <xsl:attribute name="i18n:attr">title</xsl:attribute>
        </xsl:if>
    </xsl:template>

    <xsl:template match="dri:help" mode="help">
        <!--Only create the <span> if there is content in the <dri:help> node-->
        <xsl:if test="./text() or ./node()">
            <span class="field-help">
                <xsl:apply-templates />
            </span>
        </xsl:if>
    </xsl:template>
    
</xsl:stylesheet>
