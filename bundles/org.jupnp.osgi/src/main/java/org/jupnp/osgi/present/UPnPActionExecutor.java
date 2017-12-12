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

package org.jupnp.osgi.present;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.jupnp.model.action.ActionArgumentValue;
import org.jupnp.model.action.ActionExecutor;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.meta.ActionArgument;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.types.InvalidValueException;
import org.jupnp.osgi.util.OSGiDataConverter;
import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPStateVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bruce Green
 */
class UPnPActionExecutor implements ActionExecutor {

    private final Logger log = LoggerFactory.getLogger(UPnPActionExecutor.class);

    private UPnPAction action;

    public UPnPActionExecutor(UPnPAction action) {
        this.action = action;
    }

    @Override
    public void execute(ActionInvocation<LocalService> actionInvocation) {
		log.trace("ENTRY {}.{}: {}", this.getClass().getName(), "execute", actionInvocation);

        ActionArgumentValue<LocalService>[] inputs = actionInvocation.getInput();

        Dictionary<String, Object> args = new Hashtable<String, Object>();
        for (ActionArgumentValue<LocalService> input : inputs) {
            ActionArgument<?> argument = input.getArgument();

            args.put(argument.getName(), OSGiDataConverter.toOSGiValue(input.getDatatype(), input.getValue()));
        }

        try {
            Dictionary out = action.invoke(args);

            if (out != null) {
                for (String key : (List<String>) Collections.list(out.keys())) {

                    Object value = out.get(key);
                    if (value != null) {
                        UPnPStateVariable variable = action.getStateVariable(key);
                        value = OSGiDataConverter.tojUPnPValue(variable.getUPnPDataType(), value);

                        try {
                            //System.out.printf("*** key: %s  value: %s [%s]\n", key, value, value);
                            actionInvocation.setOutput(key, value);
                        } catch (InvalidValueException e) {
                            log.error("Error executing action {} variable {}.", action.getName(), key);
                            log.error(e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error executing action ({}).", action.getName());
            log.error(e.getMessage());
        }
    }
}
