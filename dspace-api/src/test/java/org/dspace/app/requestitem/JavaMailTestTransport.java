/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.URLName;

/**
 * A dummy load for SMTP transport, which saves the last message "sent" for
 * later inspection.  See the {@link getMessage()} and {@link getAddresses()}
 * methods for access to the message.  Sending a new message through an instance
 * of this Transport discards the previous message.
 *
 * <p><strong>This class is not thread-safe</strong>.
 *
 * @author mwood
 */
public class JavaMailTestTransport
        extends Transport {
    private static Message msg;
    private static Address[] adrss;

    public JavaMailTestTransport(Session session, URLName urlname) {
        super(session, urlname);
    }

    @Override
    public void sendMessage(Message aMsg, Address[] aAdrss)
            throws MessagingException {
        msg = aMsg;
        adrss = aAdrss;
    }

    @Override
    public void connect(String host, int port, String user, String password) { }

    /* *** Implementation-specific methods. *** */

    /**
     * Access the most recent saved message.
     *
     * @return saved message.
     */
    public static Message getMessage() {
        return msg;
    }

    /**
     * Access the most recent saved addresses.
     *
     * @return saved addresses.
     */
    public static Address[] getAddresses() {
        return adrss;
    }
}
