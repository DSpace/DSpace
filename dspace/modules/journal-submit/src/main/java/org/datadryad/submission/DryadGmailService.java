package org.datadryad.submission;
import org.dspace.core.*;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.auth.oauth2.DataStoreCredentialRefreshListener;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;

import java.lang.RuntimeException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.io.*;
import javax.mail.internet.MimeMessage;
import javax.mail.Session;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class DryadGmailService {
    private static final Logger LOGGER = Logger.getLogger(DryadGmailService.class);
    private static final String SCOPE = "https://www.googleapis.com/auth/gmail.modify";
    private static final String APP_NAME = "Dryad Email Authorization";

    private String myUserID;
    private GoogleClientSecrets myClientSecrets;
    private FileDataStoreFactory myDataStoreFactory;
    private HttpTransport myHttpTransport;
    private JsonFactory myJsonFactory;
    private Credential myCredential;
    private Gmail myGmailService;
    private GoogleAuthorizationCodeFlow myAuthCodeFlow;

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
                    .addRefreshListener(new DataStoreCredentialRefreshListener(myUserID,myDataStoreFactory))
                    .build();
            myCredential = getMyCredential();
        } catch (IOException e) {
            throw new RuntimeException(e.toString());
        }
    }

    // Default constructor builds a DryadGmailService for the dryad-journal-submit@gmail.com account
    public DryadGmailService() {
        this(new File(ConfigurationManager.getProperty("submit.journal.credential.dir")), ConfigurationManager.getProperty("submit.journal.clientsecrets").toString(), SCOPE, ConfigurationManager.getProperty("submit.journal.email"));
    }

    public void authorize (String code) {
        GoogleAuthorizationCodeTokenRequest codeTokenRequest = myAuthCodeFlow.newTokenRequest(code);

        try {
            GoogleTokenResponse response = codeTokenRequest.setRedirectUri(GoogleOAuthConstants.OOB_REDIRECT_URI).execute();
            myCredential = myAuthCodeFlow.createAndStoreCredential(response, myUserID);
        } catch (IOException e) {
            throw new RuntimeException("couldn't authorize: code was " + code + "\n" + e.toString());
        }
        return;
    }

    public String getAuthorizationURLString() {
        return myAuthCodeFlow.newAuthorizationUrl().setRedirectUri(GoogleOAuthConstants.OOB_REDIRECT_URI).build();
    }

    public Credential getMyCredential() {
        try {
            return myAuthCodeFlow.loadCredential(myUserID);
        } catch (IOException e) {
            throw new RuntimeException("couldn't load credential: " + e.getMessage());
        }
    }

    public Gmail getMyGmailService() {
        if (myGmailService == null) {
            // Create a new authorized Gmail API client
            myGmailService = new Gmail.Builder(myHttpTransport, myJsonFactory, getMyCredential()).build();
        }
        return myGmailService;
    }

    public String getMyUserID() {
        return myUserID;
    }

    public static String testMethod() throws IOException {
        DryadGmailService dryadGmailService = new DryadGmailService();

        ArrayList<String> labels = new ArrayList<String>();
        labels.add(ConfigurationManager.getProperty("submit.journal.email.testlabel"));

        String result = "";

        List<Message> messages = dryadGmailService.retrieveMessagesWithLabels(labels);
        // Print ID of each Thread.
        if (messages != null) {
            ArrayList<String> processedMessageIDs = new ArrayList<String>();
            result = result + ("got " + messages.size() + " test messages");
            for (Message message : messages) {
                result = result + ("Message: " + message.getId() + ", " + message.getSnippet());
            }
        }
        return result;
    }

    public List<Message> retrieveMessagesWithLabels (List<String> labels) throws IOException {
        List<Message> messages = listMessagesWithLabels(labels);

        ArrayList<Message> resultMessages = new ArrayList<Message>();
        // Print ID of each Thread.
        if (messages != null) {
            for (Message m : messages) {
                Message message = getMyGmailService().users().messages().get(getMyUserID(), m.getId()).setFormat("raw").execute();
                resultMessages.add(message);
            }
        }
        return resultMessages;
    }

    public static ArrayList<MimeMessage> processJournalEmails () throws IOException {
        DryadGmailService dryadGmailService = new DryadGmailService();
        ArrayList<String> labels = new ArrayList<String>();
        ArrayList<MimeMessage> mimeMessages = new ArrayList<MimeMessage>();

        labels.add(ConfigurationManager.getProperty("submit.journal.email.label"));
        List<Message> messages = dryadGmailService.retrieveMessagesWithLabels(labels);
        // Print ID of each Thread.
        if (messages != null) {
            ArrayList<String> processedMessageIDs = new ArrayList<String>();
            for (Message message : messages) {
                ByteArrayInputStream postBody = new java.io.ByteArrayInputStream(Base64.decodeBase64(message.getRaw()));
                Session session = Session.getInstance(new Properties());
                try {
                    MimeMessage mimeMessage = new MimeMessage(session, postBody);
                    mimeMessages.add(mimeMessage);
                } catch (javax.mail.MessagingException e) {
                    throw new RuntimeException("MessagingException: " + e.getMessage());
                }
                dryadGmailService.modifyMessage(message.getId(), new ArrayList<String>(), labels);
            }
        }
        return mimeMessages;
    }

    /**
     * List all Messages of the user's mailbox with labelIds applied.
     *
     * @param labels Only return Messages with these labels applied.
     * @throws IOException
     */
    private List<Message> listMessagesWithLabels(List<String> labels) throws IOException {
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

        List<Message> messages = new ArrayList<Message>();
        if (labelIDs.isEmpty()) {
            return messages;
        }

        ListMessagesResponse response = myGmailService.users().messages().list(myUserID)
                .setLabelIds(labelIDs).execute();

        while (response.getMessages() != null) {
            messages.addAll(response.getMessages());
            if (response.getNextPageToken() != null) {
                String pageToken = response.getNextPageToken();
                response = myGmailService.users().messages().list(myUserID).setLabelIds(labelIDs)
                        .setPageToken(pageToken).execute();
            } else {
                break;
            }
        }

        return messages;
    }

    /**
     * Modify the labels a message is associated with.
     *
     * @param messageId ID of Message to Modify.
     * @param labelsToAdd List of label ids to add.
     * @param labelsToRemove List of label ids to remove.
     * @throws IOException
     */
    public void modifyMessage(String messageId, List<String> labelsToAdd, List<String> labelsToRemove) throws IOException {
        ArrayList<String> labelIDsToAdd = new ArrayList<String>();
        ArrayList<String> labelIDsToRemove = new ArrayList<String>();

        ListLabelsResponse labelsResponse = myGmailService.users().labels().list(myUserID).execute();
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
        Message message = myGmailService.users().messages().modify(myUserID, messageId, mods).execute();
    }

}

