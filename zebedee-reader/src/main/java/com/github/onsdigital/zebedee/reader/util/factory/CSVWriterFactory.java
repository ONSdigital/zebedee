package com.github.onsdigital.zebedee.reader.util.factory;

import au.com.bytecode.opencsv.CSVWriter;

import java.io.Writer;

/**
 * Created by dave on 12/05/2017.
 */
@FunctionalInterface
public interface CSVWriterFactory {

    CSVWriter getCSVWriter(Writer writer, char separator);

}
