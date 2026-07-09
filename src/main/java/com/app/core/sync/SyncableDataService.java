package com.app.core.sync;

public interface SyncableDataService {
    
    String getSyncType();

    default int getOrder() {
        return 0;
    }

    void syncToDatabase();
}
