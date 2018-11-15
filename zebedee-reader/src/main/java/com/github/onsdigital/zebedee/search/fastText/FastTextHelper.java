package com.github.onsdigital.zebedee.search.fastText;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.Base64;

import static com.github.onsdigital.zebedee.util.VariableUtils.getVariableValue;

public class FastTextHelper {

    public static final String convertArrayToBase64(double[] array) {
        final int capacity = 8 * array.length;
        final ByteBuffer bb = ByteBuffer.allocate(capacity);
        for (int i = 0; i < array.length; i++) {
            bb.putDouble(array[i]);
        }
        bb.rewind();
        final ByteBuffer encodedBB = Base64.getEncoder().encode(bb);
        return new String(encodedBB.array());
    }

    public static double[] convertBase64ToArray(String base64Str) {
        final byte[] decode = Base64.getDecoder().decode(base64Str.getBytes());
        final DoubleBuffer doubleBuffer = ByteBuffer.wrap(decode).asDoubleBuffer();

        final double[] dims = new double[doubleBuffer.capacity()];
        doubleBuffer.get(dims);
        return dims;
    }

    public static class Configuration {
        public static boolean INDEX_EMBEDDING_VECTORS = Boolean.parseBoolean(getVariableValue("INDEX_EMBEDDING_VECTORS"));
    }
}
