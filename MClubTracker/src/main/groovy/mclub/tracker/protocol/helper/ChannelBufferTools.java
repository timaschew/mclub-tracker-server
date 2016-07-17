/*
 * Copyright 2012 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mclub.tracker.protocol.helper;

import org.jboss.netty.buffer.ChannelBuffer;

import java.util.Formatter;

public final class ChannelBufferTools {

    private ChannelBufferTools() {
    }

    public static Integer find(ChannelBuffer buf, int start, String subString) {
        return find(buf, start, buf.readerIndex() + buf.readableBytes(), subString);
    }

    /**
     * Find string in network buffer
     */
    public static Integer find(
            ChannelBuffer buf,
            Integer start,
            Integer finish,
            String subString) {

        int index = start;
        boolean match;

        for (; index < finish; index++) {
            match = true;

            for (int i = 0; i < subString.length(); i++) {
                char c = (char) buf.getByte(index + i);
                if (c != subString.charAt(i)) {
                    match = false;
                    break;
                }
            }

            if (match) {
                return index;
            }
        }

        return null;
    }

    /**
     * Convert hex to integer (length in hex digits)
     */
    public static int readHexInteger(ChannelBuffer buf, int length) {

        int result = 0;

        for (int i = 0; i < length / 2; i++) {
            int b = buf.readUnsignedByte();
            result *= 10;
            result += b >>> 4;
            result *= 10;
            result += b & 0x0f;
        }

        if (length % 2 != 0) {
            int b = buf.getUnsignedByte(buf.readerIndex());
            result *= 10;
            result += b >>> 4;
        }

        return result;
    }

    /**
     * Return hex string
     */
    public static String readHexString(ChannelBuffer buf, int length) {

        StringBuilder result = new StringBuilder();
        Formatter formatter = new Formatter(result);

        for (int i = 0; i < length / 2; i++) {
            formatter.format("%02x", buf.readByte());
        }

        if (length % 2 != 0) {
            int b = buf.getUnsignedByte(buf.readerIndex());
            formatter.format("%01x", b >>> 4);
        }

        return result.toString();
    }

    /**
     * Read BCD coded coordinate (first byte has sign bit)
     */
    public static double readCoordinate(ChannelBuffer buf) {
        int b1 = buf.readUnsignedByte();
        int b2 = buf.readUnsignedByte();
        int b3 = buf.readUnsignedByte();
        int b4 = buf.readUnsignedByte();

        double value = (b2 & 0xf) * 10 + (b3 >> 4);
        value += (((b3 & 0xf) * 10 + (b4 >> 4)) * 10 + (b4 & 0xf)) / 1000.0;
        value /= 60;
        value += ((b1 >> 4 & 0x7) * 10 + (b1 & 0xf)) * 10 + (b2 >> 4);

        if ((b1 & 0x80) != 0) {
            value = -value;
        }

        return value;
    }

    public static byte[] convertHexString(String in) {
        int count = in.length() / 2;
        byte[] out = new byte[count];
        for (int i = 0; i < count; i++) {
            out[i] = (byte) Integer.parseInt(in.substring(i * 2, (i + 1) * 2), 16);
        }
        return out;
    }

    public static String convertByteArray(byte[] in) {
        StringBuilder out = new StringBuilder();
        Formatter formatter = new Formatter(out);
        for (byte b : in) {
            formatter.format("%02x", b);
        }
        return out.toString();
    }

}
