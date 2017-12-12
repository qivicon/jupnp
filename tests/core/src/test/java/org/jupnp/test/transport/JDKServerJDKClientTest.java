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

package org.jupnp.test.transport;

import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.test.transport.StreamServerClientTest;
import org.jupnp.transport.impl.StreamClientConfigurationImpl;
import org.jupnp.transport.impl.StreamClientImpl;
import org.jupnp.transport.impl.StreamServerConfigurationImpl;
import org.jupnp.transport.impl.StreamServerImpl;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamServer;

/**
 * @author Christian Bauer
 */
public class JDKServerJDKClientTest extends StreamServerClientTest {

    @Override
    public StreamServer createStreamServer(int port) {
        return new StreamServerImpl(
            new StreamServerConfigurationImpl(port)
        );
    }

    @Override
    public StreamClient createStreamClient(UpnpServiceConfiguration configuration) {
        return new StreamClientImpl(
            new StreamClientConfigurationImpl(
                configuration.getSyncProtocolExecutorService(),
                3
            )
        );
    }

    // DISABLED, NOT SUPPORTED

    @Override
    public void cancelled() throws Exception {
    }

    @Override
    public void checkAlive() throws Exception {
    }

    @Override
    public void checkAliveExpired() throws Exception {
    }

    @Override
    public void checkAliveCancelled() throws Exception {
    }
}
