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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the Mozilla Public License v2.0
 * along with this program. If not, see <http://mozilla.org/MPL/2.0/>.
 */
package games.gmatrix.rcon4j;

import games.gmatrix.rcon4j.exception.MalformedPacketException;

import java.io.*;
import java.net.SocketException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Packet {
    public static final Integer EXEC_CMD = 2, AUTH = 3;
    private Integer requestId, type;
    private byte[] payload;

    private Packet(int requestId, int type, byte[] payload) {
        this.requestId = requestId;
        this.type = type;
        this.payload = payload;
    }

    protected static Packet sendPacket(Rcon rcon, int type, byte[] payload) throws IOException {
        try {
            write(rcon.getSocket().getOutputStream(), rcon.getRequestId(), type, payload);
        } catch (SocketException e) {
            rcon.getSocket().close();
            throw e;
        }

        return read(rcon.getSocket().getInputStream());
    }

    protected static Packet sendAuth(Rcon rcon, byte[] password) throws IOException {
        try {
            write(rcon.getSocket().getOutputStream(), rcon.getRequestId(), AUTH, password);
        } catch (SocketException e) {
            rcon.getSocket().close();
            throw e;
        }

        read(rcon.getSocket().getInputStream());
        return read(rcon.getSocket().getInputStream());
    }


    private static void write(OutputStream out, int requestId, int type, byte[] payload) throws IOException {
        int bodyLength = getBodyLength(payload.length);
        int packetLength = getPacketLength(bodyLength);

        ByteBuffer buffer = ByteBuffer.allocate(packetLength);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.putInt(bodyLength);
        buffer.putInt(requestId);
        buffer.putInt(type);
        buffer.put(payload);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        out.write(buffer.array());
        out.flush();
    }

    private static Packet read(InputStream in) throws IOException {
        byte[] header = new byte[4 * 3];
        in.read(header);

        try {
            ByteBuffer buffer = ByteBuffer.wrap(header);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            int length = buffer.getInt();
            int requestId = buffer.getInt();
            int type = buffer.getInt();

            byte[] payload = new byte[length - 4 - 4 - 2];
            DataInputStream dis = new DataInputStream(in);
            dis.readFully(payload);
            dis.read(new byte[2]);

            return new Packet(requestId, type, payload);
        } catch (BufferUnderflowException | EOFException e) {
            throw new MalformedPacketException("Cannot read the whole packet");
        }
    }

    private static int getPacketLength(int bodyLength) {
        return 4 + bodyLength;
    }

    private static int getBodyLength(int payloadLength) {
        return 4 + 4 + payloadLength + 2;
    }

    public int getRequestId() {
        return requestId;
    }

    public int getType() {
        return type;
    }

    public byte[] getPayload() {
        return payload;
    }
}