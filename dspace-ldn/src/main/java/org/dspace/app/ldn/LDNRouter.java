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

@Aspect
public class LDNRouter {

    private static final Logger log = LogManager.getLogger(LDNRouter.class);

    private Map<Set<String>, LDNProcessor> processors = new HashMap<>();

    @Around("execution(* org.dspace.app.ldn.LDNController.inbox(..)) && args(notification, ..)")
    public Object routeNotification(ProceedingJoinPoint joinPoint, Notification notification) throws Throwable {
        LDNProcessor processor = processors.get(notification.getType());

        log.info(
            "Routed notification {} {} to {}",
            notification.getId(),
            notification.getType(),
            processor.getClass().getSimpleName()
        );

        return joinPoint.proceed(new Object[] { notification, processor });
    }

    public Map<Set<String>, LDNProcessor> getProcessors() {
        return processors;
    }

    public void setProcessors(Map<Set<String>, LDNProcessor> processors) {
        this.processors = processors;
    }

}
