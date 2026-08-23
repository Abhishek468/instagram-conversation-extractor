package com.instagram.extractor.storage;

import java.io.IOException;

public interface MessageIndex {

    boolean contains(String messageId);

    void add(String messageId);

    int size();

    void save() throws IOException;
}