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

package org.jupnp.transport.impl;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Locale;

import org.jupnp.http.Headers;
import org.jupnp.model.UnsupportedDataException;
import org.jupnp.model.message.IncomingDatagramMessage;
import org.jupnp.model.message.OutgoingDatagramMessage;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.UpnpOperation;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.transport.spi.DatagramProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation.
 * 
 * @author Christian Bauer
 */
public class DatagramProcessorImpl implements DatagramProcessor {

    private Logger log = LoggerFactory.getLogger(DatagramProcessor.class);

    public IncomingDatagramMessage read(InetAddress receivedOnAddress, DatagramPacket datagram) throws UnsupportedDataException {

        try {

            if (log.isTraceEnabled()) {
                log.trace("===================================== DATAGRAM BEGIN ============================================");
                log.trace(new String(datagram.getData()));
                log.trace("-===================================== DATAGRAM END =============================================");
            }

            ByteArrayInputStream is = new ByteArrayInputStream(datagram.getData());

            String[] startLine = Headers.readLine(is).split(" ");
            if (startLine[0].startsWith("HTTP/1.")) {
                return readResponseMessage(receivedOnAddress, datagram, is, Integer.valueOf(startLine[1]), startLine[2], startLine[0]);
            } else {
                return readRequestMessage(receivedOnAddress, datagram, is, startLine[0], startLine[2]);
            }

        } catch (Exception ex) {
            throw new UnsupportedDataException("Could not parse headers: " + ex, ex, datagram.getData());
        }
    }

    public DatagramPacket write(OutgoingDatagramMessage message) throws UnsupportedDataException {

        StringBuilder statusLine = new StringBuilder();

        UpnpOperation operation = message.getOperation();

        if (operation instanceof UpnpRequest) {

            UpnpRequest requestOperation = (UpnpRequest) operation;
            statusLine.append(requestOperation.getHttpMethodName()).append(" * ");
            statusLine.append("HTTP/1.").append(operation.getHttpMinorVersion()).append("\r\n");

        } else if (operation instanceof UpnpResponse) {
            UpnpResponse responseOperation = (UpnpResponse) operation;
            statusLine.append("HTTP/1.").append(operation.getHttpMinorVersion()).append(" ");
            statusLine.append(responseOperation.getStatusCode()).append(" ").append(responseOperation.getStatusMessage());
            statusLine.append("\r\n");
        } else {
            throw new UnsupportedDataException(
                    "Message operation is not request or response, don't know how to process: " + message
            );
        }

        // UDA 1.0, 1.1.2: No body but message must have a blank line after header
        StringBuilder messageData = new StringBuilder();
        messageData.append(statusLine);

        messageData.append(message.getHeaders().toString()).append("\r\n");

        if (log.isTraceEnabled()) {
            log.trace("Writing message data for: " + message);
            log.trace("---------------------------------------------------------------------------------");
            log.trace(messageData.toString().substring(0, messageData.length() - 2)); // Don't print the blank lines
            log.trace("---------------------------------------------------------------------------------");
        }

        try {
            // According to HTTP 1.0 RFC, headers and their values are US-ASCII
            // TODO: Probably should look into escaping rules, too
            byte[] data = messageData.toString().getBytes("US-ASCII");

            log.trace("Writing new datagram packet with " + data.length + " bytes for: " + message);
            return new DatagramPacket(data, data.length, message.getDestinationAddress(), message.getDestinationPort());

        } catch (UnsupportedEncodingException ex) {
            throw new UnsupportedDataException(
                "Can't convert message content to US-ASCII: " + ex.getMessage(), ex, messageData
            );
        }
    }

    protected IncomingDatagramMessage readRequestMessage(InetAddress receivedOnAddress,
                                                         DatagramPacket datagram,
                                                         ByteArrayInputStream is,
                                                         String requestMethod,
                                                         String httpProtocol) throws Exception {

        // Headers
        UpnpHeaders headers = new UpnpHeaders(is);

        // Assemble message
        IncomingDatagramMessage requestMessage;
        UpnpRequest upnpRequest = new UpnpRequest(UpnpRequest.Method.getByHttpName(requestMethod));
        upnpRequest.setHttpMinorVersion(httpProtocol.toUpperCase(Locale.ENGLISH).equals("HTTP/1.1") ? 1 : 0);
        requestMessage = new IncomingDatagramMessage(upnpRequest, datagram.getAddress(), datagram.getPort(), receivedOnAddress);

        requestMessage.setHeaders(headers);

        return requestMessage;
    }

    protected IncomingDatagramMessage readResponseMessage(InetAddress receivedOnAddress,
                                                          DatagramPacket datagram,
                                                          ByteArrayInputStream is,
                                                          int statusCode,
                                                          String statusMessage,
                                                          String httpProtocol) throws Exception {

        // Headers
        UpnpHeaders headers = new UpnpHeaders(is);

        // Assemble the message
        IncomingDatagramMessage responseMessage;
        UpnpResponse upnpResponse = new UpnpResponse(statusCode, statusMessage);
        upnpResponse.setHttpMinorVersion(httpProtocol.toUpperCase(Locale.ENGLISH).equals("HTTP/1.1") ? 1 : 0);
        responseMessage = new IncomingDatagramMessage(upnpResponse, datagram.getAddress(), datagram.getPort(), receivedOnAddress);

        responseMessage.setHeaders(headers);

        return responseMessage;
    }


}