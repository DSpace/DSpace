/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.paymentsystem;

import org.apache.log4j.Logger;
import org.dspace.content.Bundle;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.utils.DSpace;
import org.dspace.workflow.DryadWorkflowUtils;

/**
 *  THis Consumer updates the Shopping cart total int he event that
 *  DataFiles and/or DataPackages are changed during submission and
 *  workflow
 *
 * @author Mark Diggory, mdiggory at atmire.com
 * @author Fabio Bolognesi, fabio at atmire.com
 * @author Lantian Gai, lantian at atmire.com
 */
public class ShoppingCartConsumer implements Consumer {
    private static Logger log = Logger.getLogger(ShoppingCartConsumer.class);

    @Override
    public void initialize() throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void consume(Context ctx, Event event) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
        //todo:when user change journal,need to update the total price
        PaymentSystemService paymentSystemService = new DSpace().getSingletonService(PaymentSystemService.class);

        int st = event.getSubjectType();
        if (st != Constants.BUNDLE) {
            log.warn("Shopping cart consumer should not have been given this kind of Subject in an event, skipping: "+ event.toString());
            return;
        }
        DSpaceObject subject = event.getSubject(ctx);

        DSpaceObject object = event.getObject(ctx);

        int et = event.getEventType();
        if(subject.getType()==Constants.BUNDLE )
        {

            Bundle bundle = (Bundle) subject;
            // Bundle.getItem.....
            Item[] item = bundle.getItems();
            if(item!=null&&item.length>0){
            Item publication =  DryadWorkflowUtils.getDataPackage(ctx, item[0]);

            if(publication==null) publication=item[0];

            DCValue[] journal = publication.getMetadata("prism.publicationName");
            ShoppingCart shoppingCart = paymentSystemService.getShoppingCartByItemId(ctx,publication.getID());
            if(shoppingCart!=null)
            {
               Double oldPrice = shoppingCart.getTotal();
               //recaculate based on the current rate
               Double newPrice = paymentSystemService.calculateShoppingCartTotal(ctx,shoppingCart);

               Double oversized = paymentSystemService.getSurchargeLargeFileFee(ctx,shoppingCart);

               if(!oldPrice.equals(newPrice))
               {
                   if(oldPrice>newPrice)
                   {  //update the new price
                       shoppingCart.setTotal(newPrice);
                   }
                   else
                   {
                       //oldprice < newprice ,adding new files and oversized
                       if(oversized>0)
                       {
                           //file is oversized
                           shoppingCart.setTotal(newPrice);
                       }
                   }
               }
                else
               {
                   //do nothing
               }
                shoppingCart.update();
            }
            }
        }

    }

    @Override
    public void end(Context ctx) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void finish(Context ctx) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
