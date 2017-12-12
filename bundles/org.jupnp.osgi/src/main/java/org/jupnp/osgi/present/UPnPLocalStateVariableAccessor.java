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

import java.text.SimpleDateFormat;

import org.jupnp.model.state.StateVariableAccessor;
import org.jupnp.model.types.InvalidValueException;
import org.jupnp.osgi.util.OSGiDataConverter;
import org.osgi.service.upnp.UPnPLocalStateVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class UPnPLocalStateVariableAccessor extends StateVariableAccessor {
    private final Logger logger = LoggerFactory.getLogger(UPnPLocalStateVariableAccessor.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private static final SimpleDateFormat dateTimeTZFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat timeTZFormat = new SimpleDateFormat("HH:mm:ssZ");
	private UPnPLocalStateVariable variable;
	
	public UPnPLocalStateVariableAccessor(UPnPLocalStateVariable variable) {
		this.variable = variable;
	}
	
	@Override
	public Class<?> getReturnType() {
		logger.trace("ENTRY {}.{}: ", this.getClass().getName(), "getReturnType");
		return variable.getJavaDataType();
	}

	@Override
	public Object read(Object serviceImpl) throws Exception {
		logger.trace("ENTRY {}.{}: {}", this.getClass().getName(), "read", serviceImpl);

		Object value = variable.getCurrentValue();
		if (value != null) {
			value = OSGiDataConverter.tojUPnPValue(variable.getUPnPDataType(), value);
			
			try {
			} catch (InvalidValueException e) {
				logger.error("Error accessing variable {}.", variable.getName());
				logger.error(e.getMessage());
			}
		}
		
		return value;
	}
}
