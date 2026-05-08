package com.coredeux.export.writer;

import java.io.IOException;
import java.util.List;

public interface ExportWriteSession extends AutoCloseable {

    void writeHeader(List<String> values) throws IOException;

    void writeRow(List<String> values) throws IOException;

    void finish() throws IOException;

    @Override
    void close() throws IOException;
}
