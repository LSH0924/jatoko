package com.jatoko.service;

@FunctionalInterface
public interface ProgressCallback {
    void onProgress(String message, int percentage);
}
