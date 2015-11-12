package org.datadryad.submission;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.DataStoreCredentialRefreshListener;
import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import org.apache.log4j.Logger;
import org.dspace.core.*;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.File;
import java.io.StringReader;
import java.util.*;

public class DryadGmailService {
    private static final Logger LOGGER = Logger.getLogger(DryadGmailService.class);
    private static final String SCOPE = "https://www.googleapis.com/auth/gmail.modify";

    private static String myUserID;
    private GoogleClientSecrets myClientSecrets;
    private FileDataStoreFactory myDataStoreFactory;
    private static HttpTransport myHttpTransport;
    private static JsonFactory myJsonFactory;
    private static Credential myCredential;
    private static Gmail myGmailService;
    private static GoogleAuthorizationCodeFlow myAuthCodeFlow;

    private static LinkedHashMap<String,Message> currentMessages = null;

    /**
     * Instantiate a DryadGmailService object.
     *
     * @param credentialPath Path to the location of the clientSecretsFile and the credential DataStore.
     * @param scope authorization scope
     * @param user account to be authorized
     * @throws IOException
     */
    public DryadGmailService(File credentialPath, String clientsecretsFile, String scope, String user) {
        myUserID = user;
        try {
            myDataStoreFactory = new FileDataStoreFactory(credentialPath);
            myHttpTransport = new NetHttpTransport();
            myJsonFactory = new JacksonFactory();
            myClientSecrets = GoogleClientSecrets.load(myJsonFactory, new StringReader (clientsecretsFile));

        } catch (IOException e) {
            throw new RuntimeException("Client secret loading failed; please check the file at "+ clientsecretsFile.toString());
        }
        try {
            myAuthCodeFlow = new GoogleAuthorizationCodeFlow.Builder(myHttpTransport, myJsonFactory, myClientSecrets, Arrays.asList(scope))
                    .setDataStoreFactory(myDataStoreFactory)
                    .setAccessType("offline")
                    .setApprovalPrompt("force")
                    .addRefreshListener(new DataStoreCredentialRefreshListener(getMyUserID(),myDataStoreFactory))
                    .build();
            myCredential = getMyCredential();
        } catch (IOException e) {
            throw new RuntimeException("Something is wrong with the stored credential file; please remove it and reauthorize.");
        }
    }

    // Default constructor builds a DryadGmailService for the dryad-journal-submit@gmail.com account
    public DryadGmailService() {
        this(new File(ConfigurationManager.getProperty("submit.journal.credential.dir")), ConfigurationManager.getProperty("submit.journal.clientsecrets").toString(), SCOPE, ConfigurationManager.getProperty("submit.journal.email"));
    }

    public void authorize (String code) {
        try {
        myAuthCodeFlow = new GoogleAuthorizationCodeFlow.Builder(myHttpTransport, myJsonFactory, myClientSecrets, Arrays.asList(SCOPE))
                .setDataStoreFactory(myDataStoreFactory)
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .addRefreshListener(new DataStoreCredentialRefreshListener(getMyUserID(),myDataStoreFactory))
                .build();
        GoogleAuthorizationCodeTokenRequest codeTokenRequest = myAuthCodeFlow.newTokenRequest(code);

            GoogleTokenResponse response = codeTokenRequest.setRedirectUri(GoogleOAuthConstants.OOB_REDIRECT_URI).execute();
            myCredential = myAuthCodeFlow.createAndStoreCredential(response, getMyUserID());
        } catch (IOException e) {
            throw new RuntimeException("couldn't authorize: code was " + code + "\n" + e.toString());
        }
        return;
    }

    public String getAuthorizationURLString() {
        return myAuthCodeFlow.newAuthorizationUrl().setRedirectUri(GoogleOAuthConstants.OOB_REDIRECT_URI).build();
    }

    public static Credential getMyCredential() {
        try {
            return myAuthCodeFlow.loadCredential(getMyUserID());
        } catch (IOException e) {
            throw new RuntimeException("couldn't load credential: " + e.getMessage());
        }
    }

    public static Gmail getMyGmailService() {
        if (myGmailService == null) {
            // Create a new authorized Gmail API client
            myGmailService = new Gmail.Builder(myHttpTransport, myJsonFactory, getMyCredential()).build();
        }
        return myGmailService;
    }

    public static String getMyUserID() {
        return myUserID;
    }

    public static String testMethod() throws IOException {
        DryadGmailService dryadGmailService = new DryadGmailService();

        ArrayList<String> labels = new ArrayList<String>();
        labels.add(ConfigurationManager.getProperty("submit.journal.email.testlabel"));

        String result = "";

        List<Message> messages = retrieveMessagesWithLabels(labels);
        if (messages != null) {
            ArrayList<String> processedMessageIDs = new ArrayList<String>();
            result = result + ("got " + messages.size() + " test messages");
            for (Message message : messages) {
                result = result + ("Message: " + message.getId() + ", " + message.getSnippet());
            }
        }
        return result;
    }

    public static List<Message> retrieveMessagesWithLabels (List<String> labels) throws IOException {
        List<Message> labeledMessages = new ArrayList<Message>();

        // Look through the possible labels from Gmail and add the ones that match the requested labels.
        ArrayList<String> labelIDs = new ArrayList<String>();
        ListLabelsResponse labelsResponse = getMyGmailService().users().labels().list(getMyUserID()).execute();
        List<Label> labelList = labelsResponse.getLabels();
        for (Label label : labelList) {
            for (String labelName : labels) {
                if (label.getName().equals(labelName)) {
                    labelIDs.add(label.getId());
                }
            }
        }

        // If we have Gmail labels, then start looking for all pages of emails that match the label.
        if (!labelIDs.isEmpty()) {
            ListMessagesResponse response = getMyGmailService().users().messages().list(getMyUserID())
                    .setLabelIds(labelIDs).execute();

            while (response.getMessages() != null) {
                labeledMessages.addAll(response.getMessages());
                if (response.getNextPageToken() != null) {
                    String pageToken = response.getNextPageToken();
                    response = getMyGmailService().users().messages().list(getMyUserID()).setLabelIds(labelIDs)
                            .setPageToken(pageToken).execute();
                } else {
                    break;
                }
            }
        }

        // process the retrieved emails and save the raw text to the resultMessages
        ArrayList<Message> resultMessages = new ArrayList<Message>();
        if (labeledMessages != null) {
            for (Message m : labeledMessages) {
                Message message = getMyGmailService().users().messages().get(getMyUserID(), m.getId()).setFormat("raw").execute();
                resultMessages.add(message);
            }
        }
        return resultMessages;
    }

    // queries the Gmail API and returns a list of Gmail message IDs. This list is valid until completeJournalProcessing is called.
    public static List<String> getJournalMessageIds() throws IOException {
        List<String> result = new LinkedList<String>();
        if (currentMessages == null) {

            ArrayList<String> labels = new ArrayList<String>();
            labels.add(ConfigurationManager.getProperty("submit.journal.email.label"));
            List<Message> messages = retrieveMessagesWithLabels(labels);

            currentMessages = new LinkedHashMap<String, Message>();
            for (Message m : messages) {
                currentMessages.put(m.getId(), m);
            }
            result.addAll(currentMessages.keySet());
        } else {
            throw new RuntimeException("Journal emails are still processing: " + currentMessages.size() + " messages remaining.");
        }
        return result;
    }

    public static void completeJournalProcessing() {
        currentMessages = null;
    }

    public static MimeMessage getMessageForId(String mID) {
        Message message = currentMessages.get(mID);
        ByteArrayInputStream postBody = new java.io.ByteArrayInputStream(Base64.decodeBase64(message.getRaw()));
        Session session = Session.getInstance(new Properties());
        try {
            return new MimeMessage(session, postBody);
        } catch (javax.mail.MessagingException e) {
            throw new RuntimeException("MessagingException: " + e.getMessage());
        }
    }

    // convenience methods for adding/removing previously-defined labels

    public static void addJournalLabelForMessageWithId (String mID) {
        ArrayList<String> labels = new ArrayList<String>();
        labels.add(ConfigurationManager.getProperty("submit.journal.email.label"));
        modifyMessage(mID, labels, new ArrayList<String>());
    }

    public static void removeJournalLabelForMessageWithId (String mID) {
        ArrayList<String> labels = new ArrayList<String>();
        labels.add(ConfigurationManager.getProperty("submit.journal.email.label"));
        modifyMessage(mID, new ArrayList<String>(), labels);
    }

    public static void addErrorLabelForMessageWithId (String mID) {
        ArrayList<String> labels = new ArrayList<String>();
        labels.add(ConfigurationManager.getProperty("submit.journal.email.error.label"));
        modifyMessage(mID, labels, new ArrayList<String>());
    }

    /**
     * Modify the labels a message is associated with.
     *
     * @param messageId ID of Message to Modify.
     * @param labelsToAdd List of label ids to add.
     * @param labelsToRemove List of label ids to remove.
     * @throws IOException
     */
    public static void modifyMessage(String messageId, List<String> labelsToAdd, List<String> labelsToRemove) {
        ArrayList<String> labelIDsToAdd = new ArrayList<String>();
        ArrayList<String> labelIDsToRemove = new ArrayList<String>();

        try {
            ListLabelsResponse labelsResponse = getMyGmailService().users().labels().list(getMyUserID()).execute();
            List<Label> labelList = labelsResponse.getLabels();
            for (Label label : labelList) {
                for (String labelName : labelsToAdd) {
                    if (label.getName().equals(labelName)) {
                        labelIDsToAdd.add(label.getId());
                    }
                }
                for (String labelName : labelsToRemove) {
                    if (label.getName().equals(labelName)) {
                        labelIDsToRemove.add(label.getId());
                    }
                }
            }
            ModifyMessageRequest mods = new ModifyMessageRequest().setAddLabelIds(labelIDsToAdd)
                    .setRemoveLabelIds(labelIDsToRemove);
            Message message = getMyGmailService().users().messages().modify(getMyUserID(), messageId, mods).execute();
        } catch (IOException e) {
            throw new RuntimeException (" Couldn't modify message " + messageId, e);
        }
    }

}

