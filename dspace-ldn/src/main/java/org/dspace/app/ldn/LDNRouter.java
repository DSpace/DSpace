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

@Aspect
public class LDNRouter {

    private static final Logger log = LogManager.getLogger(LDNRouter.class);

    private Map<Set<String>, LDNProcessor> processors = new HashMap<>();

    @Around("execution(* org.dspace.app.ldn.LDNController.inbox(..)) && args(notification, ..)")
    public Object logAroundGetEmployee(ProceedingJoinPoint joinPoint, Notification notification) throws Throwable {
        log.info("Processors {}", processors);

        log.info("Process notification {} {}", notification.getType(), notification.getId());

        LDNProcessor processor = processors.get(notification.getType());

        Object response = joinPoint.proceed(new Object[] { notification, processor });

        for (LDNAction action : processor.getActions()) {
            action.execute(notification);
        }

        return response;
    }

    public Map<Set<String>, LDNProcessor> getProcessors() {
        return processors;
    }

    public void setProcessors(Map<Set<String>, LDNProcessor> processors) {
        this.processors = processors;
    }

}