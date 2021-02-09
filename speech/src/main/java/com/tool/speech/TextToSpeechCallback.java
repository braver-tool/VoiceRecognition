package com.tool.speech;

public interface TextToSpeechCallback {
    void onStart();
    void onCompleted();
    void onError();
}
