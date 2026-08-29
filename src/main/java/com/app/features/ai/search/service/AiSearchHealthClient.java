package com.app.features.ai.search.service;

public interface AiSearchHealthClient {

    boolean isReady();

    String getModelVersion();

    String getIndexDirectory();

    int getDocumentCount();

    String getStatusDetail();
}
