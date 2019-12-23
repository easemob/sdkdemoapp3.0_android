package com.hyphenate.chatuidemo.conference;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chatuidemo.utils.PreferenceManager;
import java.nio.ByteBuffer;

public class ExternalAudioInputRecord {
    private static final String TAG = "ExternalAudioInputExt";

    // Default audio data format is PCM 16 bit per sample.
    // Guaranteed to be supported by all devices.
    private static final int BITS_PER_SAMPLE = 16;

    // Requested size of each recorded buffer provided to the client.
    private static final int CALLBACK_BUFFER_SIZE_MS = 10;

    // Average number of callbacks per second.
    private static final int BUFFERS_PER_SECOND = 1000 / CALLBACK_BUFFER_SIZE_MS;

    // We ask for a native buffer size of BUFFER_SIZE_FACTOR * (minimum required
    // buffer size). The extra space is allocated to guard against glitches under
    // high load.
    private static final int BUFFER_SIZE_FACTOR = 2;

    // The AudioRecordJavaThread is allowed to wait for successful call to join()
    // but the wait times out afther this amount of time.
    private static final long AUDIO_RECORD_THREAD_JOIN_TIMEOUT_MS = 2000;

    public static final int audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION;

    private ByteBuffer byteBuffer;

    private AudioRecord audioRecord;

    private AudioInputRecordThread audioInputThread;

    private byte[] emptyBytes;

    boolean audioInitFlag = false;

    private static ExternalAudioInputRecord instance;

    public static synchronized ExternalAudioInputRecord getInstance(){
        if(instance==null){
            instance=new ExternalAudioInputRecord();
        }
        return instance;
    }

    private ExternalAudioInputRecord(){
    }

    public  boolean getAudioInitFlag(){
        return audioInitFlag;
    }

    private class AudioInputRecordThread extends Thread {
        private volatile boolean keepAlive = true;
        public AudioInputRecordThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
            Log.d(TAG, "AudioRecordThread" + "  name=" + Thread.currentThread().getName() + ", " +
                            "id=" + Thread.currentThread().getId());
            assertTrue(audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING);

            while (keepAlive) {
                byteBuffer.clear();
                byteBuffer.put(emptyBytes);
                int bytesRead = audioRecord.read(byteBuffer, byteBuffer.capacity());
                if (bytesRead == byteBuffer.capacity()) {
                    if (keepAlive) {
                        //外部音频数据写入缓冲区
                        int ret = EMClient.getInstance().conferenceManager().inputExternalAudioData(byteBuffer.array(), byteBuffer.capacity());
                        if(ret != 0) {
                            if (ret == -1) {
                                Log.d(TAG, "Buffer is not Full, add data fail ,dataSize:" + byteBuffer.capacity());
                            } else if (ret == -2) {
                                Log.d(TAG, "Buffer is Full , dataSize:" + byteBuffer.capacity());
                            }
                        }
                    }
                } else {
                    String errorMessage = "AudioRecord.read failed: " + bytesRead;
                    Log.e(TAG, errorMessage);
                    if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                        keepAlive = false;
                    }
                }
            }
            try {
                if (audioRecord != null) {
                    audioRecord.stop();
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "AudioRecord.stop failed: " + e.getMessage());
            }
        }
        public void stopThread() {
            Log.d(TAG, "stopThread");
            keepAlive = false;
        }
    }

    public int initRecording() {
        int channels = 1; //外部输入音频目前只支持单声道
        int sampleRate = PreferenceManager.getInstance().getCallAudioSampleRate();
        if(sampleRate == -1){ //默认采样率为16000
            sampleRate = 16000;
        }
        final int bytesPerFrame = channels * (BITS_PER_SAMPLE / 8);
        final int framesPerBuffer = sampleRate / BUFFERS_PER_SECOND;
        byteBuffer = ByteBuffer.allocateDirect(bytesPerFrame * framesPerBuffer);
        if (!(byteBuffer.hasArray())) {
            Log.e(TAG, "ByteBuffer does not have backing array.");
            return -1;
        }
        Log.d(TAG, "byteBuffer.capacity: " + byteBuffer.capacity());
        emptyBytes = new byte[byteBuffer.capacity()];

        int minBufferSize =
                AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "AudioRecord.getMinBufferSize failed: " + minBufferSize);
            return -1;
        }
        Log.d(TAG, "AudioRecord.getMinBufferSize: " + minBufferSize);

        // Use a larger buffer size than the minimum required when creating the
        // AudioRecord instance to ensure smooth recording under load. It has been
        // verified that it does not increase the actual recording latency.
        int bufferSizeInBytes = Math.max(BUFFER_SIZE_FACTOR * minBufferSize, byteBuffer.capacity());
        Log.d(TAG, "bufferSizeInBytes: " + bufferSizeInBytes);
        try {
            audioRecord = new AudioRecord(audioSource, sampleRate, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "AudioRecord ctor error: " + e.getMessage());
            releaseAudioResources();
            return -1;
        }
        if (audioRecord == null || audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.d(TAG, "Failed to create a new AudioRecord instance");
            releaseAudioResources();
            return -1;
        }
        Log.d(TAG, "open mic success");
        return framesPerBuffer;
    }

    public boolean startRecording() {
        if(!audioInitFlag){
            if(initRecording() <= 0){
                Log.d(TAG, "InitRecording Failed");
                return  false;
            }
            audioInitFlag = true;
        }

        Log.d(TAG, "startRecording");
        assertTrue(audioRecord != null);
        try {
            audioRecord.startRecording();
        } catch (IllegalStateException e) {
            Log.d(TAG, "AudioRecord.startRecording failed: " + e.getMessage());
            return false;
        }
        if(audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
            Log.d(TAG, "AudioRecord.startRecording failed - incorrect state :" + audioRecord.getRecordingState());
            return false;
        }

        audioInputThread = new AudioInputRecordThread("ExternalAudioInputRecordThread");
        Log.d(TAG, "do startRecording");
        audioInputThread.start();
        return true;
    }

    public boolean stopRecording() {
        Log.d(TAG, "stopRecording");
        if(audioInitFlag){
           assertTrue(audioInputThread != null);
           audioInputThread.stopThread();
           if (!joinUninterruptibly(audioInputThread, AUDIO_RECORD_THREAD_JOIN_TIMEOUT_MS)) {
            Log.e(TAG, "Join of AudioRecordJavaThread timed out");
          }
          audioInputThread = null;
          releaseAudioResources();
          audioInitFlag = false;
        }
        return true;
    }

    // Releases the native AudioRecord resources.
    private void releaseAudioResources() {
        Log.d(TAG, "releaseAudioResources");
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
    }

    private  void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("Expected condition to be true");
        }
    }

    private boolean joinUninterruptibly(Thread thread, long timeoutMs) {
        long startTimeMs = SystemClock.elapsedRealtime();
        long timeRemainingMs = timeoutMs;
        boolean wasInterrupted = false;

        while(timeRemainingMs > 0L) {
            try {
                thread.join(timeRemainingMs);
                break;
            } catch (InterruptedException var11) {
                wasInterrupted = true;
                long elapsedTimeMs = SystemClock.elapsedRealtime() - startTimeMs;
                timeRemainingMs = timeoutMs - elapsedTimeMs;
            }
        }
        if (wasInterrupted) {
            Thread.currentThread().interrupt();
        }
        return !thread.isAlive();
    }
}
