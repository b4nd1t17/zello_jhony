package com.example.zello_jhony;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class MainActivity extends AppCompatActivity {

    private Button connectButton;
    private Button selectOpusButton;
    private Button sendAudioButton;
    private Uri selectedOpusFileUri;
    private EditText usernameEditText;
    private EditText passwordEditText;
    private EditText channelEditText;

    private static final String TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJXa002Uld3Z1JHVnNJRU5oYldsdWJ6bzQuUUp6ZVp4QVFMR0Q2LTZaeFJ3bVdmN3ZCbTFfaHdPSWtON1Bzb2swczBLayIsImV4cCI6MTY5NzMwNzY2MSwiYXpwIjoiZGV2In0.fX28ILuUYX6cTRghRWN1jv_cv_3YKV1AjeZbRkaKOEx14R3aNveNGN-eNJLLL5yAE4auCva6wEdVWHqpQKMlDdikzzVWTHq_5i05N8GpxJZE1_S4ZFR9DJEjvtliNHFnYwM5L4yRKTP5wFapw1-bxPWWLPqLnPOOY9EMv2reXRFD-LR17bNeOwq13nc5MjqQG7-ysL8L5Vm5iIpJ7I69DRvWy1XZ9b_1JuNMTqTheBwz3k7J2r90l1z0H8N5fBGIvr8dH1MDjs9jT2nW4C_y4zBRbqGSFx7mQXjrZH3mdnHMpqyRl5WB_C8q0ZSpyeDbIsgICZ9X5_lCAYSPDh8S2w";

    private static final String TAG = "MainActivity";
    private static final int FRAGMENT_SIZE = 320;
    private int sequenceNumber = 1;
    private volatile int streamId = -1;
    private WebSocketClient webSocketClient;
    private String currentChannel;
    private int packetId = 0;
    private List<Integer> confirmedPackets = new ArrayList<>();

    // Handler para manejar las confirmaciones
    private Handler confirmationHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            int packetId = msg.arg1;
            boolean confirmed = msg.what == 1;

            if (confirmed) {
                confirmedPackets.add(packetId);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectButton = findViewById(R.id.connectButton);
        selectOpusButton = findViewById(R.id.selectOpusButton);
        sendAudioButton = findViewById(R.id.sendAudioButton);
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        channelEditText = findViewById(R.id.channelEditText);

        connectButton.setOnClickListener(view -> connectToWebSocket());
        selectOpusButton.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, 1);
        });
        sendAudioButton.setOnClickListener(this::onSendAudioClick);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            selectedOpusFileUri = data.getData();
            Log.i(TAG, "Archivo seleccionado: " + selectedOpusFileUri.toString());
        }
    }

    private void connectToWebSocket() {
        final String username = usernameEditText.getText().toString();
        final String password = passwordEditText.getText().toString();
        final String channel = channelEditText.getText().toString();

        if (username.isEmpty() || password.isEmpty() || channel.isEmpty()) {
            Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        currentChannel = channel;

        try {
            URI uri = new URI("wss://zello.io/ws");
            webSocketClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    Log.i(TAG, "Conexión abierta");
                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("command", "logon");
                        jsonObject.put("seq", sequenceNumber++);
                        jsonObject.put("auth_token", TOKEN);
                        jsonObject.put("username", username);
                        jsonObject.put("password", password);
                        jsonObject.put("channel", channel);

                        webSocketClient.send(jsonObject.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error al crear mensaje JSON: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                }

                @Override
                public void onMessage(String message) {
                    Log.i(TAG, "Mensaje recibido: " + message);
                    try {
                        JSONObject jsonObject = new JSONObject(message);
                        if (jsonObject.has("stream_id")) {
                            streamId = jsonObject.getInt("stream_id");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onClose(int i, String s, boolean b) {
                    Log.i(TAG, "Conexión cerrada: " + s);
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Conexión cerrada: " + s, Toast.LENGTH_LONG).show());
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error: " + e.getMessage(), e);
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            };
            webSocketClient.connect();
        } catch (URISyntaxException e) {
            Log.e(TAG, "Error de sintaxis de URI: ", e);
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error al conectar: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private byte[] readOpusFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                byte[] buffer = new byte[inputStream.available()];
                inputStream.read(buffer);
                inputStream.close();
                return buffer;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al leer el archivo OPUS: ", e);
        }
        return null;
    }

    private String buildCodecHeader(int sampleRateHz, int framesPerPacket, int frameSizeMs) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putShort((short) sampleRateHz);
        buffer.put((byte) framesPerPacket);
        buffer.put((byte) frameSizeMs);

        byte[] packetBytes = buffer.array();
        return Base64.encodeToString(packetBytes, Base64.DEFAULT);
    }

    private void startVoiceStream(String channelName) {
        try {
            String codecHeader = buildCodecHeader(16000, 1, 60);

            JSONObject startStreamCommand = new JSONObject();
            startStreamCommand.put("command", "start_stream");
            startStreamCommand.put("seq", sequenceNumber++);
            startStreamCommand.put("channel", channelName);
            startStreamCommand.put("type", "audio");
            startStreamCommand.put("codec", "opus");
            startStreamCommand.put("codec_header", codecHeader);
            startStreamCommand.put("packet_duration", 60); // 60 ms

            webSocketClient.send(startStreamCommand.toString());
            Log.i(TAG, "Stream de voz iniciado");
        } catch (JSONException e) {
            Log.e(TAG, "Error al crear mensaje JSON para iniciar el flujo de voz: ", e);
        }
    }

    private void stopVoiceStream(String channelName) {
        try {
            JSONObject stopStreamCommand = new JSONObject();
            stopStreamCommand.put("command", "stop_stream");
            stopStreamCommand.put("seq", sequenceNumber++);
            stopStreamCommand.put("stream_id", streamId);
            stopStreamCommand.put("channel", channelName);

            webSocketClient.send(stopStreamCommand.toString());
            Log.i(TAG, "Stream de voz detenido");
        } catch (JSONException e) {
            Log.e(TAG, "Error al crear mensaje JSON para detener el flujo de voz: ", e);
        }
    }

    private void sendAudioPacket(String channelName, int streamId, byte[] packetData) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + 4 + packetData.length);
            buffer.put((byte) 0x01); // Tipo de paquete (audio)
            buffer.putInt(streamId); // ID del stream
            buffer.putInt(packetId); // ID del paquete
            buffer.put(packetData); // Datos de audio

            webSocketClient.send(buffer.array());
            packetId++;
        } catch (Exception e) {
            Log.e(TAG, "Error al enviar paquete de audio: ", e);
        }
    }

    private void sendAudioToZelloChannel(byte[] opusData, String channelName) {
        try {
            startVoiceStream(channelName);

            CountDownLatch latch = new CountDownLatch(1);
            new Thread(() -> {
                try {
                    while (streamId == -1) {
                        Thread.sleep(100);
                    }
                    latch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restaura la interrupción en caso de que se interrumpa el hilo.
                    Log.e(TAG, "El hilo fue interrumpido mientras esperaba: " + e.getMessage(), e);
                }
            }).start();

            latch.await();

            int totalPackets = (int) Math.ceil((double) opusData.length / FRAGMENT_SIZE);

            for (int packetIndex = 0; packetIndex < totalPackets; packetIndex++) {
                int start = packetIndex * FRAGMENT_SIZE;
                int end = Math.min((packetIndex + 1) * FRAGMENT_SIZE, opusData.length);
                byte[] packetData = Arrays.copyOfRange(opusData, start, end);

                sendAudioPacket(channelName, streamId, packetData);

                if (packetIndex < totalPackets - 1) {
                    Thread.sleep(100);
                }
            }

            stopVoiceStream(channelName);
            streamId = -1;
            packetId = 0; // Reset packet ID for the next session
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restaura la interrupción en caso de que se interrumpa el hilo.
            Log.e(TAG, "El hilo fue interrumpido mientras enviaba fragmentos de audio: " + e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, "Error al enviar fragmentos de audio: ", e);
        }
    }

    private void onSendAudioClick(View view) {
        if (selectedOpusFileUri != null) {
            byte[] opusData = readOpusFile(selectedOpusFileUri);
            if (opusData != null) {
                sendAudioToZelloChannel(opusData, currentChannel);
            }
        } else {
            Toast.makeText(this, "Seleccione un archivo OPUS primero", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webSocketClient != null) {
            webSocketClient.close();
        }
    }
}
