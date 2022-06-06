package com.github.onsdigital.zebedee.api;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.IOException;
import java.io.OutputStream;

public class StubServletOutputStream extends ServletOutputStream {
    private OutputStream target;

    public StubServletOutputStream(final OutputStream target) {
        this.target = target;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(int b) throws IOException {
        target.write(b);
    }

    @Override
    public void flush() throws IOException {
        super.flush();
        target.flush();
    }
}