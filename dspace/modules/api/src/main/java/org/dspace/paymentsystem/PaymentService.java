/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.paymentsystem;

import org.apache.cocoon.environment.Request;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.content.DSpaceObject;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.workflow.WorkflowItem;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilderFactory;
import java.sql.SQLException;
import java.util.Date;

/**
 *  This is the API interface for the Shopping Cart to
 *  interact with the external Paypal Payment service.
 *
 * @author Mark Diggory, mdiggory at atmire.com
 * @author Fabio Bolognesi, fabio at atmire.com
 * @author Lantian Gai, lantian at atmire.com
 */
public interface PaymentService
{
    public String getSecureTokenId();

    public String generateSecureToken(ShoppingCart shoppingCart, String secureTokenId, String knotId,Context context);

    public boolean submitReferenceTransaction(Context c, WorkflowItem wfi, HttpServletRequest request);

    public void generateUserForm(Context context, Division mainDiv, String actionURL, String knotId, String type, Request request, Item item, DSpaceObject dso) throws WingException, SQLException;

    public boolean chargeCard(Context c, WorkflowItem wfi, HttpServletRequest request, ShoppingCart shoppingCart);

    public void generatePaypalForm(Division mandiv, ShoppingCart shoppingCart, String actionURL, String type,Context context) throws WingException, SQLException;

    public void generateNoCostForm(Division actionsDiv, ShoppingCart transaction, org.dspace.content.Item item, PaymentSystemConfigurationManager manager, PaymentSystemService paymentSystemService) throws WingException, SQLException;

    public void showSkipPaymentButton(Division mainDiv, String message) throws WingException;

    public void addButtons(Division mainDiv) throws WingException;
}
