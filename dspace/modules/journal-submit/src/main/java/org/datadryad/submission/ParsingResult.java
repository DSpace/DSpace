package org.datadryad.submission;

/**
 * A ParsingResult object stores metadata extracted from a submission e-mail 
 * message from a journal
 * 
 * @author Akio Sone for the Dryad project
 */
public class ParsingResult {


    /** The submission id. */
    private String submissionId;
    
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
    private boolean flawedId = false;
    /**
     * @return the flawedId
     */
    public boolean hasFlawedId() {
        return flawedId;
    }

    /**
     * @param flawedId the flawedId to set
     */
    public void setHasFlawedId(boolean flawedId) {
        this.flawedId = flawedId;
    }



    /** The submission data as a String Builder instance. */
    private StringBuilder submissionData;
    
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
    private String senderEmailAddress;
    
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
    
    /** The journal code. */
    private String journalCode;
    
    /**
     * @return the journalCode
     */
    public String getJournalCode() {
        return journalCode;
    }

    /**
     * @param journalCode the journalCode to set
     */
    public void setJournalCode(String journalCode) {
        this.journalCode = journalCode;
    }

    
    /** records a error message */
    private String status = null;
    
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
            "flawedId="+ flawedId + ",\n"+
            "submissionData=" + submissionData.toString()
            + "\n********** ParsingResult: contents: end:\n";
    }
}