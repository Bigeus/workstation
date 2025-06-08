package com.example.workstation;

import android.app.Dialog;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class TaskMenu extends AppCompatActivity implements TaskAdapter.OnTaskClickListener {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
    private TextView currentVideoText;
    private ImageView playPauseButton;
    private SeekBar volumeSeekBar;
    private boolean isPlaying = false;
    private int currentStationIndex = -1;

    // MediaPlayer for radio streaming
    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler();
    private boolean isBuffering = false;

    private static final String CHANNEL_ID = "radio_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final String ACTION_PLAY_PAUSE = "com.example.workstation.PLAY_PAUSE";
    private static final String ACTION_STOP = "com.example.workstation.STOP";

    private NotificationManager notificationManager;
    private BroadcastReceiver radioReceiver;

    // Lofi radio streams
    private final String[] lofiRadioStreams = {
            "https://stream.laut.fm/lofi",                    // Lofi Radio
            "http://listen.chillhop.com/listen",              // ChillHop Radio
            "https://streams.ilovemusic.de/iloveradio17.mp3", // ILoveRadio Chill
            "https://stream.rcs.revma.com/audioassets/CHILL", // Chill Radio
            "http://hyades.shoutca.st:8043/stream"            // Lofi Hip Hop Radio
    };

    private final String[] lofiStationNames = {
            "Lofi Radio",
            "ChillHop Lofi",
            "ILoveRadio Chill",
            "Chill Beats",
            "Lofi Hip Hop"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_taskmenu);

        initializeViews();
        initializeFirebase();
        setupRecyclerView();
        setupMediaPlayer();
        loadTasks();
        setupLofiButtons();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewTasks);
        currentVideoText = findViewById(R.id.currentVideoText);
        playPauseButton = findViewById(R.id.playPauseButton);

        playPauseButton.setOnClickListener(v -> togglePlayPause());



        currentVideoText.setText("Selecione uma rádio lofi");
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Rádio Lofi",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Controles da rádio lofi");
            channel.setShowBadge(false);

            notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void setupRecyclerView() {
        taskList = new ArrayList<>();
        taskAdapter = new TaskAdapter(this, taskList);
        taskAdapter.setOnTaskClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(taskAdapter);
    }

    private void setupMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e("RADIO_PLAYER", "Erro ao liberar MediaPlayer anterior", e);
            }
        }

        mediaPlayer = new MediaPlayer();

        // Configurações críticas para streaming estável
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        mediaPlayer.setAudioAttributes(audioAttributes);

        // IMPORTANTE: Configurar wake lock para manter streaming
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        // Buffer maior para streaming mais estável
        try {
            // Usar reflexão para acessar métodos internos (se disponível)
            mediaPlayer.getClass().getMethod("setBufferingParams", int.class, int.class)
                    .invoke(mediaPlayer, 15000, 50000); // 15s inicial, 50s máximo
        } catch (Exception e) {
            Log.d("RADIO_PLAYER", "BufferingParams não disponível nesta versão do Android");
        }

        mediaPlayer.setOnPreparedListener(mp -> {
            isBuffering = false;
            runOnUiThread(() -> {
                playPauseButton.setImageResource(R.drawable.ic_pause);
                isPlaying = true;
                currentVideoText.setText("🎵 " + lofiStationNames[currentStationIndex]);
                Toast.makeText(this, "Rádio conectada!", Toast.LENGTH_SHORT).show();
            });

            try {
                mp.start();

                // Aplicar volume
                if (volumeSeekBar != null) {
                    float volume = volumeSeekBar.getProgress() / 100f;
                    mp.setVolume(volume, volume);
                }

                Log.d("RADIO_PLAYER", "Stream iniciado com sucesso");

            } catch (Exception e) {
                Log.e("RADIO_PLAYER", "Erro ao iniciar playback", e);
            }
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            String errorMsg = getErrorMessage(what, extra);
            Log.e("RADIO_PLAYER", "Erro no MediaPlayer: " + errorMsg + " (what=" + what + ", extra=" + extra + ")");

            runOnUiThread(() -> {
                currentVideoText.setText("⚠️ Reconectando...");
                playPauseButton.setImageResource(R.drawable.ic_play);
                isPlaying = false;
                isBuffering = false;
            });

            // Tentar reconectar automaticamente após erro
            handler.postDelayed(() -> {
                if (currentStationIndex != -1) {
                    Log.d("RADIO_PLAYER", "Tentando reconectar após erro");
                    setupMediaPlayer(); // Recriar MediaPlayer
                    prepareRadioStream(currentStationIndex);
                }
            }, 2000);

            return true; // Indica que tratamos o erro
        });

        mediaPlayer.setOnInfoListener((mp, what, extra) -> {
            Log.d("RADIO_PLAYER", "Info: what=" + what + ", extra=" + extra);

            switch (what) {
                case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                    isBuffering = true;
                    runOnUiThread(() -> {
                        currentVideoText.setText("🔄 Buffering...");
                    });
                    Log.d("RADIO_PLAYER", "Buffering iniciado");
                    break;

                case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                    isBuffering = false;
                    runOnUiThread(() -> {
                        if (isPlaying) {
                            currentVideoText.setText("🎵 " + lofiStationNames[currentStationIndex]);
                        }
                    });
                    Log.d("RADIO_PLAYER", "Buffering finalizado");
                    break;

                case MediaPlayer.MEDIA_INFO_AUDIO_NOT_PLAYING:
                    Log.w("RADIO_PLAYER", "Áudio não está tocando - possível problema de buffer");
                    // Tentar reconectar se persistir
                    handler.postDelayed(() -> {
                        if (isPlaying && !mediaPlayer.isPlaying()) {
                            Log.w("RADIO_PLAYER", "Forçando reconexão por áudio parado");
                            prepareRadioStream(currentStationIndex);
                        }
                    }, 5000);
                    break;

                case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                    Log.d("RADIO_PLAYER", "Metadata atualizada");
                    break;
            }
            return false;
        });

        // Listener para detectar fim inesperado do stream
        mediaPlayer.setOnCompletionListener(mp -> {
            Log.w("RADIO_PLAYER", "Stream completou inesperadamente - reconectando");
            isPlaying = false;
            runOnUiThread(() -> {
                playPauseButton.setImageResource(R.drawable.ic_play);
                currentVideoText.setText("🔄 Reconectando...");
            });

            // Reconectar após um breve delay
            handler.postDelayed(() -> {
                if (currentStationIndex != -1) {
                    prepareRadioStream(currentStationIndex);
                }
            }, 1000);
        });
    }

    private void setupLofiButtons() {
        findViewById(R.id.lofi1Button).setOnClickListener(v -> selectLofiStation(0));
        findViewById(R.id.lofi2Button).setOnClickListener(v -> selectLofiStation(1));
        findViewById(R.id.lofi3Button).setOnClickListener(v -> selectLofiStation(2));
    }

    @SuppressWarnings("deprecation")
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.net.Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;

            android.net.NetworkCapabilities capabilities =
                    connectivityManager.getNetworkCapabilities(network);
            return capabilities != null &&
                    capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } else {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }

    private void selectLofiStation(int index) {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "📶 Sem conexão com a internet", Toast.LENGTH_SHORT).show();
            return;
        }

        if (index < 0 || index >= lofiRadioStreams.length) {
            Log.e("RADIO_PLAYER", "Índice inválido: " + index);
            return;
        }

        // Se a mesma estação está tocando, apenas pause/play
        if (currentStationIndex == index && !isBuffering) {
            togglePlayPause();
            return;
        }

        // Parar estação atual se estiver tocando
        if (isPlaying || isBuffering) {
            stopRadio();
        }

        // Definir nova estação e preparar
        currentStationIndex = index;
        Log.d("RADIO_PLAYER", "Selecionada estação: " + lofiStationNames[index]);
        prepareRadioStream(index);
    }

    private void tryNextStationOrReconnect() {
        if (currentStationIndex == -1) return;

        // Tentar próxima estação
        int nextStation = (currentStationIndex + 1) % lofiRadioStreams.length;

        if (nextStation != currentStationIndex) {
            Log.d("RADIO_PLAYER", "Tentando próxima estação: " + nextStation);
            currentStationIndex = nextStation;
            prepareRadioStream(nextStation);
        } else {
            // Se todas as estações falharam, tentar reconectar a atual
            Log.d("RADIO_PLAYER", "Reconectando estação atual");
            prepareRadioStream(currentStationIndex);
        }
    }

    private void prepareRadioStream(int index) {
        if (index < 0 || index >= lofiRadioStreams.length) {
            Log.e("RADIO_PLAYER", "Índice inválido: " + index);
            return;
        }

        // Executar em thread separada para não bloquear UI
        new Thread(() -> {
            String streamUrl = null;
            try {
                if (mediaPlayer != null) {
                    // Reset completo do player
                    try {
                        if (mediaPlayer.isPlaying()) {
                            mediaPlayer.stop();
                        }
                        mediaPlayer.reset();
                    } catch (Exception e) {
                        Log.e("RADIO_PLAYER", "Erro ao resetar MediaPlayer", e);
                        // Se falhar, recriar completamente
                        runOnUiThread(() -> setupMediaPlayer());
                        return;
                    }

                    streamUrl = lofiRadioStreams[index];
                    Log.d("RADIO_PLAYER", "Preparando stream: " + streamUrl);

                    runOnUiThread(() -> {
                        isBuffering = true;
                        isPlaying = false;
                        playPauseButton.setImageResource(R.drawable.ic_play);
                        currentVideoText.setText("🔄 Conectando: " + lofiStationNames[index]);
                    });

                    // Configurar headers para melhor compatibilidade
                    Map<String, String> headers = new HashMap<>();
                    headers.put("User-Agent", "AndroidRadioPlayer/1.0 (compatible; Android " + android.os.Build.VERSION.RELEASE + ")");
                    headers.put("Accept", "*/*");
                    headers.put("Connection", "keep-alive");
                    headers.put("Cache-Control", "no-cache");

                    // Definir fonte de dados com timeout
                    mediaPlayer.setDataSource(getApplicationContext(), Uri.parse(streamUrl), headers);

                    // Preparar de forma assíncrona
                    mediaPlayer.prepareAsync();

                } else {
                    Log.e("RADIO_PLAYER", "MediaPlayer é null - recriando");
                    runOnUiThread(() -> {
                        setupMediaPlayer();
                        prepareRadioStream(index);
                    });
                }
            } catch (IOException e) {
                Log.e("RADIO_PLAYER", "Erro de IO ao preparar stream: " + streamUrl, e);

                runOnUiThread(() -> {
                    currentVideoText.setText("❌ Erro de conexão");
                    isBuffering = false;
                    Toast.makeText(this, "Falha na conexão - tentando novamente", Toast.LENGTH_SHORT).show();
                });

                // Tentar próxima estação ou reconectar
                handler.postDelayed(() -> tryNextStationOrReconnect(), 3000);

            } catch (Exception e) {
                Log.e("RADIO_PLAYER", "Erro inesperado ao preparar stream", e);
                runOnUiThread(() -> {
                    currentVideoText.setText("❌ Erro inesperado");
                    isBuffering = false;
                    // Recriar MediaPlayer em caso de erro crítico
                    setupMediaPlayer();
                });
            }
        }).start();
    }

    private void togglePlayPause() {
        if (currentStationIndex == -1) {
            selectLofiStation(0);
            return;
        }

        if (isBuffering) {
            Toast.makeText(this, "⏳ Aguarde, carregando...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mediaPlayer == null) {
            Log.w("RADIO_PLAYER", "MediaPlayer é null - recriando");
            setupMediaPlayer();
            prepareRadioStream(currentStationIndex);
            return;
        }

        try {
            if (isPlaying) {
                // Pausar
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    playPauseButton.setImageResource(R.drawable.ic_play);
                    isPlaying = false;
                    currentVideoText.setText("⏸️ " + lofiStationNames[currentStationIndex]);
                    Log.d("RADIO_PLAYER", "Rádio pausada");
                }
            } else {
                // Retomar ou iniciar
                if (mediaPlayer.isPlaying()) {
                    // Já está tocando mas estado inconsistente
                    isPlaying = true;
                    playPauseButton.setImageResource(R.drawable.ic_pause);
                    currentVideoText.setText("🎵 " + lofiStationNames[currentStationIndex]);
                } else {
                    try {
                        mediaPlayer.start();
                        playPauseButton.setImageResource(R.drawable.ic_pause);
                        isPlaying = true;
                        currentVideoText.setText("🎵 " + lofiStationNames[currentStationIndex]);

                        // Reaplicar volume
                        if (volumeSeekBar != null) {
                            float volume = volumeSeekBar.getProgress() / 100f;
                            mediaPlayer.setVolume(volume, volume);
                        }

                        Log.d("RADIO_PLAYER", "Rádio retomada");
                    } catch (IllegalStateException e) {
                        Log.w("RADIO_PLAYER", "MediaPlayer em estado inválido - reconectando", e);
                        prepareRadioStream(currentStationIndex);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("RADIO_PLAYER", "Erro no toggle play/pause", e);
            // Em caso de erro, reconectar
            setupMediaPlayer();
            prepareRadioStream(currentStationIndex);
        }
    }

    private String getErrorMessage(int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                return "Erro desconhecido";
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                return "Servidor indisponível";
            case -1004: // MEDIA_ERROR_IO
                return "Erro de conexão";
            case -1007: // MEDIA_ERROR_MALFORMED
                return "Stream inválido";
            case -110: // MEDIA_ERROR_TIMED_OUT
                return "Tempo esgotado";
            default:
                return "Erro " + what + "/" + extra;
        }
    }

    private void resumeRadio() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.start();
                playPauseButton.setImageResource(R.drawable.ic_pause);
                isPlaying = true;
                currentVideoText.setText("Tocando: " + lofiStationNames[currentStationIndex]);

                // Apply current volume
                float volume = volumeSeekBar.getProgress() / 100f;
                mediaPlayer.setVolume(volume, volume);
            }
        } catch (Exception e) {
            Log.e("RADIO_PLAYER", "Error resuming radio", e);
            prepareRadioStream(currentStationIndex);
        }
    }

    private void pauseRadio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            playPauseButton.setImageResource(R.drawable.ic_play);
            isPlaying = false;
            currentVideoText.setText("Pausado: " + lofiStationNames[currentStationIndex]);
        }
    }

    private void stopRadio() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            playPauseButton.setImageResource(R.drawable.ic_play);
            isPlaying = false;
            isBuffering = false;
        }
    }

    // Task management methods remain the same as in your original code
    public void showAddTaskModal(View view) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.modal_add_task);

        EditText taskNameEdit = dialog.findViewById(R.id.taskNameEdit);
        RadioGroup priorityGroup = dialog.findViewById(R.id.priorityGroup);
        Button addButton = dialog.findViewById(R.id.addTaskButton);
        Button cancelButton = dialog.findViewById(R.id.cancelButton);

        addButton.setOnClickListener(v -> {
            String taskName = taskNameEdit.getText().toString().trim();
            if (taskName.isEmpty()) {
                Toast.makeText(this, "Digite o nome da task", Toast.LENGTH_SHORT).show();
                return;
            }

            int selectedId = priorityGroup.getCheckedRadioButtonId();
            int priority = 1; // default

            if (selectedId == R.id.priority1) priority = 1;
            else if (selectedId == R.id.priority2) priority = 2;
            else if (selectedId == R.id.priority3) priority = 3;

            addTaskToFirebase(taskName, priority);
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void addTaskToFirebase(String name, int priority) {
        String userId = mAuth.getCurrentUser().getUid();

        Map<String, Object> task = new HashMap<>();
        task.put("name", name);
        task.put("priority", priority);
        task.put("isFinished", false);

        db.collection("users").document(userId).collection("tasks")
                .add(task)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Task adicionada com sucesso", Toast.LENGTH_SHORT).show();
                    loadTasks();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao adicionar task", Toast.LENGTH_SHORT).show();
                    Log.e("FIREBASE", "Erro ao adicionar task", e);
                });
    }

    private void loadTasks() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId).collection("tasks")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        taskList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Task taskObj = new Task(
                                    document.getId(),
                                    document.getString("name"),
                                    document.getLong("priority").intValue(),
                                    document.getBoolean("isFinished")
                            );
                            taskList.add(taskObj);
                        }

                        Collections.sort(taskList, (t1, t2) -> {
                            if (t1.isFinished() != t2.isFinished()) {
                                return Boolean.compare(t1.isFinished(), t2.isFinished());
                            }
                            return Integer.compare(t1.getPriority(), t2.getPriority());
                        });

                        taskAdapter.notifyDataSetChanged();
                    } else {
                        Log.e("FIREBASE", "Erro ao carregar tasks", task.getException());
                    }
                });
    }

    @Override
    public void onTaskClick(Task task, int position) {
        task.setFinished(!task.isFinished());
        updateTaskInFirebase(task);
    }

    @Override
    public void onDeleteClick(Task task, int position) {
        deleteTaskFromFirebase(task.getId(), position);
    }

    private void updateTaskInFirebase(Task task) {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId).collection("tasks").document(task.getId())
                .update("isFinished", task.isFinished())
                .addOnSuccessListener(aVoid -> taskAdapter.notifyDataSetChanged())
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE", "Erro ao atualizar task", e);
                    task.setFinished(!task.isFinished());
                });
    }

    private void deleteTaskFromFirebase(String taskId, int position) {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId).collection("tasks").document(taskId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    taskList.remove(position);
                    taskAdapter.notifyItemRemoved(position);
                    Toast.makeText(this, "Task removida", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao remover task", Toast.LENGTH_SHORT).show();
                    Log.e("FIREBASE", "Erro ao remover task", e);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("RADIO_PLAYER", "onDestroy - limpando recursos");

        // Parar todas as verificações
        handler.removeCallbacks(radioHealthCheck);
        handler.removeCallbacksAndMessages(null);

        // Parar e liberar MediaPlayer
        stopRadio();
        if (mediaPlayer != null) {
            try {
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e("RADIO_PLAYER", "Erro ao liberar MediaPlayer", e);
            }
            mediaPlayer = null;
        }
    }

    private Runnable radioHealthCheck = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && isPlaying && currentStationIndex != -1) {
                try {
                    if (!mediaPlayer.isPlaying()) {
                        Log.w("RADIO_PLAYER", "Rádio deveria estar tocando mas não está - reconectando");
                        runOnUiThread(() -> {
                            currentVideoText.setText("🔄 Reconectando...");
                            isPlaying = false;
                            playPauseButton.setImageResource(R.drawable.ic_play);
                        });

                        // Reconectar
                        handler.post(() -> prepareRadioStream(currentStationIndex));
                    } else {
                        // Tudo OK, verificar novamente em 30 segundos
                        handler.postDelayed(this, 30000);
                    }
                } catch (Exception e) {
                    Log.e("RADIO_PLAYER", "Erro na verificação de saúde da rádio", e);
                    // Reiniciar MediaPlayer se houver erro crítico
                    handler.post(() -> {
                        setupMediaPlayer();
                        prepareRadioStream(currentStationIndex);
                    });
                }
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("RADIO_PLAYER", "onPause - mantendo rádio tocando");
        // NÃO pausar a rádio aqui - deixar tocando em background

        handler.removeCallbacks(radioHealthCheck);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("RADIO_PLAYER", "onResume");

        // Verificar se a rádio ainda está tocando
        if (mediaPlayer != null && isPlaying) {
            try {
                if (!mediaPlayer.isPlaying()) {
                    Log.w("RADIO_PLAYER", "Rádio parou durante pause - reconectando");
                    prepareRadioStream(currentStationIndex);
                } else {
                    // Reiniciar verificação de saúde
                    handler.postDelayed(radioHealthCheck, 10000);
                }
            } catch (Exception e) {
                Log.e("RADIO_PLAYER", "Erro ao verificar estado no onResume", e);
            }
        }
    }
}