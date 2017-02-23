/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;

/**
 * @author kstamatis
 *
 */
public class TagCloudConfiguration {

	String cloudCase;
	String width;
	
	String fontFrom;
	String fontTo;
	
	String cuttingLevel;
	int totalTags;
	boolean randomColors;
	
	String ordering;
	
	boolean displayScore;
	boolean shouldCenter;
	
	String locale;
	
	/**
	 * 
	 */
	public TagCloudConfiguration() {
		//Default values;
		
		width = "100%";
		
		cloudCase = "Case.PRESERVE_CASE";
		
		fontFrom = "1.1";//"15";
		fontTo = "3.2";//"40";
		
		cuttingLevel = "0";
		totalTags = -1;
		randomColors = true;
		
		ordering = "Tag.NameComparatorAsc";
		
		displayScore = false;
		shouldCenter = true;
		
		locale = "el";
	}

	/**
	 * @return the cloudCase
	 */
	public String getCloudCase() {
		return cloudCase;
	}

	/**
	 * @param cloudCase the cloudCase to set
	 */
	public void setCloudCase(String cloudCase) {
		this.cloudCase = cloudCase;
	}

	/**
	 * @return the width
	 */
	public String getWidth() {
		return width;
	}

	/**
	 * @param width the width to set
	 */
	public void setWidth(String width) {
		this.width = width;
	}

	/**
	 * @return the fontFrom
	 */
	public String getFontFrom() {
		return fontFrom;
	}

	/**
	 * @param fontFrom the fontFrom to set
	 */
	public void setFontFrom(String fontFrom) {
		this.fontFrom = fontFrom;
	}

	/**
	 * @return the fontTo
	 */
	public String getFontTo() {
		return fontTo;
	}

	/**
	 * @param fontTo the fontTo to set
	 */
	public void setFontTo(String fontTo) {
		this.fontTo = fontTo;
	}

	/**
	 * @return the cuttingLevel
	 */
	public String getCuttingLevel() {
		return cuttingLevel;
	}

	/**
	 * @param cuttingLevel the cuttingLevel to set
	 */
	public void setCuttingLevel(String cuttingLevel) {
		this.cuttingLevel = cuttingLevel;
	}

	/**
	 * @return the totalTags
	 */
	public int getTotalTags() {
		return totalTags;
	}

	/**
	 * @param totalTags the totalTags to set
	 */
	public void setTotalTags(int totalTags) {
		this.totalTags = totalTags;
	}

	/**
	 * @return the randomColors
	 */
	public boolean isRandomColors() {
		return randomColors;
	}

	/**
	 * @param randomColors the randomColors to set
	 */
	public void setRandomColors(boolean randomColors) {
		this.randomColors = randomColors;
	}

	/**
	 * @return the ordering
	 */
	public String getOrdering() {
		return ordering;
	}

	/**
	 * @param ordering the ordering to set
	 */
	public void setOrdering(String ordering) {
		this.ordering = ordering;
	}

	/**
	 * @return the displayScore
	 */
	public boolean isDisplayScore() {
		return displayScore;
	}

	/**
	 * @param displayScore the displayScore to set
	 */
	public void setDisplayScore(boolean displayScore) {
		this.displayScore = displayScore;
	}

	/**
	 * @return the shouldCenter
	 */
	public boolean isShouldCenter() {
		return shouldCenter;
	}

	/**
	 * @param shouldCenter the shouldCenter to set
	 */
	public void setShouldCenter(boolean shouldCenter) {
		this.shouldCenter = shouldCenter;
	}

	/**
	 * @return the locale
	 */
	public String getLocale() {
		return locale;
	}

	/**
	 * @param locale the locale to set
	 */
	public void setLocale(String locale) {
		this.locale = locale;
	}
}
