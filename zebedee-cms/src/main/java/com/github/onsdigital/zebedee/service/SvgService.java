package com.github.onsdigital.zebedee.service;

import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Handles conversion of SVG.
 */
public class SvgService {

    /**
     * Convert the SVG input stream to PNG on the output stream.
     *
     * @param inputStream
     * @param outputStream
     * @throws TranscoderException
     */
    public static void convertSvgToPng(InputStream inputStream, OutputStream outputStream) throws TranscoderException {
        Transcoder transcoder = new PNGTranscoder();

        transcoder.addTranscodingHint(ImageTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER, new Float(0.08f));

        TranscoderInput transcoderInput = new TranscoderInput(inputStream);
        TranscoderOutput transcoderOutput = new TranscoderOutput(outputStream);
        transcoder.transcode(transcoderInput, transcoderOutput);
    }
}