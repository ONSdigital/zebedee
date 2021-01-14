package com.github.onsdigital.zebedee.encryption;

import java.nio.file.Path;

public interface CollectionKeyReadWriter {

    CollectionKey get(Path filePath);
}
