package com.example.smartnoisemonitor;

import android.content.Context;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.AudioFormat;

import org.tensorflow.lite.support.audio.TensorAudio;
import org.tensorflow.lite.task.audio.classifier.AudioClassifier;
import org.tensorflow.lite.task.audio.classifier.Classifications;

import java.io.IOException;
import java.util.List;

public class SoundClassifier {

    private AudioClassifier classifier;
    private TensorAudio audioTensor;
    private AudioRecord audioRecord;

    public SoundClassifier(Context context) throws IOException {
        classifier = AudioClassifier.createFromFile(context, "yamnet.tflite");
        audioTensor = classifier.createInputTensorAudio();
        audioRecord = classifier.createAudioRecord();
    }

    public void startListening(SoundResultCallback callback) {
        audioRecord.startRecording();

        new Thread(() -> {
            while (!Thread.interrupted()) {
                audioTensor.load(audioRecord);
                List<Classifications> results = classifier.classify(audioTensor);

                // Top result
                String label = results.get(0).getCategories().get(0).getLabel();
                float score = results.get(0).getCategories().get(0).getScore();

                if (score > 0.5) { // Only strong predictions
                    callback.onResult(label, score);
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    public void stopListening() {
        if (audioRecord != null) audioRecord.stop();
    }

    public interface SoundResultCallback {
        void onResult(String label, float confidence);
    }
}
