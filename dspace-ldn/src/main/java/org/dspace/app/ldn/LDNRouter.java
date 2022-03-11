/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.dspace.app.ldn.model.Notification;
import org.dspace.app.ldn.processor.LDNProcessor;

/**
 * Linked Data Notification router aspect with point cut around inbox endpoint.
 * Provides routing notification to appropriate processor and injexting
 * processor as argument to inbox.
 */
@Aspect
public class LDNRouter {

    private static final Logger log = LogManager.getLogger(LDNRouter.class);

    private Map<Set<String>, LDNProcessor> processors = new HashMap<>();

    /**
     * Around pointcut to route notificaiton and inject argument to inbox endpoint
     * handler method.
     *
     * @param joinPoint    proceeding join point, extract arguments and proceed to
     *                     method
     * @param notification received notification being passed into inbox endpoint
     *                     handler method
     * @return Object result of the inbox endpoint handler method
     * @throws Throwable failed to pointcut
     */
    @Around("execution(* org.dspace.app.ldn.LDNInboxController.inbox(..)) && args(notification, ..)")
    public Object routeNotification(ProceedingJoinPoint joinPoint, Notification notification) throws Throwable {
        LDNProcessor processor = processors.get(notification.getType());

        log.info("Routed notification {} {} to {}",
                notification.getId(),
                notification.getType(),
                processor.getClass().getSimpleName());

        return joinPoint.proceed(new Object[] { notification, processor });
    }

    /**
     * @return Map<Set<String>, LDNProcessor>
     */
    public Map<Set<String>, LDNProcessor> getProcessors() {
        return processors;
    }

    /**
     * @param processors
     */
    public void setProcessors(Map<Set<String>, LDNProcessor> processors) {
        this.processors = processors;
    }

}
