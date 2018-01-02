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

import org.jupnp.model.state.StateVariableAccessor;
import org.osgi.service.upnp.UPnPStateVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bruce Green
 */
class UPnPStateVariableAccessor extends StateVariableAccessor {

    private final Logger log = LoggerFactory.getLogger(UPnPStateVariableAccessor.class);

    private UPnPStateVariable variable;

    public UPnPStateVariableAccessor(UPnPStateVariable variable) {
        this.variable = variable;
    }

    @Override
    public Class<?> getReturnType() {
    	log.trace("ENTRY {}.{}: ", this.getClass().getName(), "getReturnType"); 
        return variable.getJavaDataType();
    }

    @Override
    public Object read(Object serviceImpl) throws Exception {
		log.trace("ENTRY {}.{}: {}", this.getClass().getName(), "read", serviceImpl);
        return null;
    }
}
