/*
 * RCON4j
 * Copyright (C) 2019 Garrett Koleda
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Mozilla Public License as published by
 * the Mozilla Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Mozilla Public License for more details.
 *
 * You should have received a copy of the Mozilla Public License v2.0
 * along with this program. If not, see <http://mozilla.org/MPL/2.0/>.
 */
package games.gmatrix.rcon4j;

import games.gmatrix.rcon4j.exception.AuthException;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

import static games.gmatrix.rcon4j.Packet.EXEC_CMD;

public class Rcon {
    private Integer requestId;
    private Socket socket;
    private Charset charset;
    private final Object sync = new Object();
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    public Rcon(String host, int port, byte[] password) throws IOException, AuthException {
        charset = StandardCharsets.UTF_8;
        connect(host, port, password);
    }

    public void connect(String host, Integer port, byte[] password) throws IOException, AuthException {
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("Host cannot be null");
        }

        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Selected port is out of range");
        }

        Packet response;
        synchronized (sync) {
            requestId = random.nextInt();
            socket = new Socket(host, port);
            response = Packet.sendAuth(this, password);
        }

        if (response.getRequestId() != requestId) {
            if (response.getRequestId() == -1) {
                throw new AuthException("Password rejected by server");
            } else {
                throw new AuthException("Authentication responseId doesn't match the original id");
            }
        }
    }

    public void disconnect() throws IOException {
        synchronized (sync) {
            socket.close();
        }
    }

    public String command(String payload) throws IOException {
        if (payload == null || payload.trim().isEmpty()) {
            throw new IllegalArgumentException("Payload cannot be null");
        }

        Packet response = sendPacket(EXEC_CMD, payload.getBytes());
        return new String(response.getPayload(), getCharset());
    }

    private Packet sendPacket(int type, byte[] payload) throws IOException {
        synchronized (sync) {
            return Packet.sendPacket(this, type, payload);
        }
    }

    public int getRequestId() {
        return requestId;
    }

    public Socket getSocket() {
        return socket;
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }
}