/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion;

/**
 * This DTO class is returned by an {@link org.dspace.app.suggestion.openaire.EvidenceScorer} to model the concept of
 * an evidence / fact that has been used to evaluate the precision of a suggestion increasing or decreasing the score
 * of the suggestion.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class SuggestionEvidence {

    /** name of the evidence */
    private String name;

    /** positive or negative value to influence the score of the suggestion */
    private double score;

    /** additional notes */
    private String notes;

    public SuggestionEvidence() {
    }

    public SuggestionEvidence(String name, double score, String notes) {
        this.name = name;
        this.score = score;
        this.notes = notes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

}