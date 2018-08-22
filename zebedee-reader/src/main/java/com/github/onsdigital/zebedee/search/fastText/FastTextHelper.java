package com.github.onsdigital.zebedee.search.fastText;

import cc.fasttext.FastText;
import cc.fasttext.Vector;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.Base64;
import java.util.List;

import static com.github.onsdigital.zebedee.util.VariableUtils.getVariableValue;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

public class FastTextHelper {

    private final FastText fastText;

    private static FastTextHelper INSTANCE;

    public static final String PREFIX = "__label__";

    public static FastTextHelper getInstance() throws IOException {
        if (INSTANCE == null) {
            synchronized (FastTextHelper.class) {
                INSTANCE = new FastTextHelper();
            }
        }
        return INSTANCE;
    }

    private FastTextHelper() throws IOException {
        fastText = FastText.load(Configuration.getFastTextModelFilename());
    }

    public FastText getFastText() {
        return fastText;
    }

    public int getDimensions() {
        return getFastText().getArgs().dim();
    }

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

    public static double[] toDoubleArray(Vector vector) {
        List<Float> floatList = vector.getData();
        double[] vec = new double[floatList.size()];

        for (int i = 0; i < vec.length; i++) {
            vec[i] = (double) floatList.get(i);
        }

        return vec;
    }

    public static class Configuration {
        public static boolean INDEX_EMBEDDING_VECTORS = Boolean.parseBoolean(getVariableValue("INDEX_EMBEDDING_VECTORS"));
        private static String fastTextModelDirectory = defaultIfBlank(getVariableValue("SUPERVISED_MODELS_DIRECTORY"), "./supervised_models");
        private static String fastTextModelName = defaultIfBlank(getVariableValue("SUPERVISED_MODEL_NAME"), "ons_supervised.bin");

        public static String getFastTextModelFilename() {
            return String.format("%s/%s", fastTextModelDirectory, fastTextModelName);
        }
    }
}
