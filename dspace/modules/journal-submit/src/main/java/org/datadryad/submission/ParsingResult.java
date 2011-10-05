package org.datadryad.submission;

/**
 * A ParsingResult object stores metadata extracted from a submission e-mail 
 * message from a journal
 * 
 * @author Akio Sone for the Dryad project
 */
public class ParsingResult {


    /** The submission id. */
    String submissionId;
    
    /**
     * @return the submissionId
     */
    public String getSubmissionId() {
        return submissionId;
    }
    
    /**
     * @param submissionId the submissionId to set
     */
    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }


    /** whether an parsed submission id is valid. */
    boolean hasFlawedId = false;
    /**
     * @return the hasFlawedId
     */
    public boolean isHasFlawedId() {
        return hasFlawedId;
    }

    /**
     * @param hasFlawedId the hasFlawedId to set
     */
    public void setHasFlawedId(boolean hasFlawedId) {
        this.hasFlawedId = hasFlawedId;
    }



    /** The submission data as a String Builder instance. */
    StringBuilder submissionData;
    
    /**
     * @return the submissionData
     */
    public StringBuilder getSubmissionData() {
        return submissionData;
    }

    /**
     * @param submissionData the submissionData to set
     */
    public void setSubmissionData(StringBuilder submissionData) {
        this.submissionData = submissionData;
    }


    /** The sender's email address. */
    String senderEmailAddress;
    
    /**
     * @return the senderEmailAddress
     */
    public String getSenderEmailAddress() {
        return senderEmailAddress;
    }

    /**
     * @param senderEmailAddress the senderEmailAddress to set
     */
    public void setSenderEmailAddress(String senderEmailAddress) {
        this.senderEmailAddress = senderEmailAddress;
    }


    /** The journal name. */
    String journalName;
    
    /**
     * @return the journalName
     */
    public String getJournalName() {
        return journalName;
    }

    /**
     * @param journalName the journalName to set
     */
    public void setJournalName(String journalName) {
        this.journalName = journalName;
    }
    
    
    /** records a error message */
    String status = null;
    
    /**
     * @return the status message
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }
    

    /**
     * pretty serialization for debugging
     */
    @Override
    public String toString() {
        return "********* ParsingResult: contents: start\n" + 
            "submissionId="+ submissionId + ",\n" +
            "senderEmailAddress=" + senderEmailAddress+ ",\n" +
            "hasFlawedId="+hasFlawedId + ",\n"+
            "submissionData=" + submissionData.toString()
            + "\n********** ParsingResult: contents: end:\n";
    }
}