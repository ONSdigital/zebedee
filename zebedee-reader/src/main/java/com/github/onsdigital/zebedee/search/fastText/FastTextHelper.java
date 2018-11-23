package com.github.onsdigital.zebedee.search.fastText;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.Base64;

import static com.github.onsdigital.zebedee.util.VariableUtils.getVariableValue;

public class FastTextHelper {

    private static final int DOUBLE_SIZE_IN_BYTES = 8;

    public static final String convertArrayToBase64(double[] array) {
        final int capacity = DOUBLE_SIZE_IN_BYTES * array.length;
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

    /**
     * TODO - Fix me
     */
    public static class Configuration {
        private static final String INDEX_EMBEDDING_VECTORS_KEY = "INDEX_EMBEDDING_VECTORS";

        public static boolean INDEX_EMBEDDING_VECTORS = Boolean.parseBoolean(getVariableValue(INDEX_EMBEDDING_VECTORS_KEY));
    }
}
