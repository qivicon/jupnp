/**
 * Copyright (C) 2014 4th Line GmbH, Switzerland and others
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package org.jupnp.util;

import java.util.Dictionary;
import java.util.Map;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * Implements a managed service to enable or disable the {@link SpecificationViolationReporter}. 
 * 
 * @author Andre Fuechsel 
 */
@SuppressWarnings("rawtypes")
public class SpecificationViolationReporterConfig implements ManagedService {

    private static final String SPECIFICATION_VIOLATION_REPORTER_ENABLED = "specificationViolationReporterEnabled";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private volatile boolean specificationViolationReportingEnabled = true;

    public void activate(ComponentContext ctx) {
        // get default configuration
        configure(ctx.getProperties());
        configureReporter();
    }

    public void modified(Map<String, Object> config) {
        configureReporter();
    }

    @Override
    public void updated(Dictionary properties) throws ConfigurationException {
        // get updated configuration
        configure(properties);
    }

    private void configure(Dictionary config) {
        Object enabledObj = config.get(SPECIFICATION_VIOLATION_REPORTER_ENABLED);
        if (enabledObj != null && enabledObj instanceof Boolean) {
            specificationViolationReportingEnabled = (Boolean) enabledObj;
        }
    }

    private void configureReporter() {
        if (specificationViolationReportingEnabled) {
            logger.info("Enabling jUPnP specification violation reporter");
            SpecificationViolationReporter.enableReporting();
        } else {
            logger.info("Disabling jUPnP specification violation reporter");
            SpecificationViolationReporter.disableReporting();
        }
    }
}
