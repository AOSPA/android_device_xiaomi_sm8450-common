// From frameworks/base/nfc/java/android/nfc/INfcAdapter.aidl
package android.nfc;

interface INfcAdapter {
    int getState();
    void pausePolling(int timeoutInMs);
    void resumePolling();
}
