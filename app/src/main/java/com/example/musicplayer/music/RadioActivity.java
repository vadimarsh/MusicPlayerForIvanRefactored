package com.example.musicplayer.music;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.musicplayer.App;
import com.example.musicplayer.Player;
import com.example.musicplayer.R;
import com.example.musicplayer.Services.OnClearFromRecentService;
import com.example.musicplayer.adapter.TrackAdapter;
import com.example.musicplayer.database.Track;
import com.example.musicplayer.notification.CreateNotification;
import com.example.musicplayer.notification.Playable;

import java.net.MalformedURLException;
import java.net.URL;

public class RadioActivity extends AppCompatActivity implements Playable {
    Player player;
    Button setRadioButton, backButton;
    EditText radioUrl, radioTitle;
    ListView radioList;
    TrackAdapter adapter;
    Button play, prev, next;
    TextView title;
    NotificationManager notificationManager;
    boolean running = false;
    //AppDatabase db = App.getDb();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radio);
        player = App.getApp().getPlayer();
        if (!running) {
            running = true;
            startTitleThread();
        }

        init();

        //LOGS-------------------------
//        db.playlistDao().insert(new Playlist(App.getPlaylistIndex(), "playlist_name_1"));
//        App.incPlaylistIndex();
//        db.playlistDao().insert(new Playlist(App.getPlaylistIndex(), "playlist_name_2"));
//        App.incPlaylistIndex();

//        db.trackPlaylistDao().insert(new TrackPlaylist(5, 0));
//        db.trackPlaylistDao().insert(new TrackPlaylist(10, 1));
//        db.trackPlaylistDao().insert(new TrackPlaylist(6, 0));
//        db.trackPlaylistDao().insert(new TrackPlaylist(15, 1));

//        List<Track> list = db.trackDao().getAll();
//        for (Track track : list) {
//            Log.d("testing", track.getId() + " " + track.getName() + " " + track.getPath());
//        }
//
//        List<Playlist> list1 = db.playlistDao().getAll();
//        for (Playlist playlist : list1) {
//            Log.d("testing", playlist.getId() + " " + playlist.getName());
//        }
//
//        List<TrackPlaylist> list2 = db.trackPlaylistDao().getAll();
//        for (TrackPlaylist trackPlaylist : list2) {
//            Log.d("testing", trackPlaylist.getTrackId() + " " + trackPlaylist.getPlaylistId());
//        }
//        //LOGS-----------------------

        if (player.isPlaying()) {
            play.setBackgroundResource(R.drawable.ic_pause);
        } else {
            play.setBackgroundResource(R.drawable.ic_play);
        }

        if (!player.getSource().equals(".") && player.getCurrentRadio() != -1) {
            title.setText(player.getCurrentRadioTrack().getName());
        }
        else if (player.getSource().equals(".") && player.getCurrentSong() != -1) {
            title.setText(player.getCurrentTitle());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
            registerReceiver(broadcastReceiver, new IntentFilter("TRACKS_TRACKS"));
            startService(new Intent(getBaseContext(), OnClearFromRecentService.class));
        }
    }

    void init() {
        setRadioButton = findViewById(R.id.setRadioButton);
        radioUrl = findViewById(R.id.radioUrl);
        radioList = findViewById(R.id.radioList);
        backButton = findViewById(R.id.backButton);
        radioTitle = findViewById(R.id.radioTitle);
        play = findViewById(R.id.play);
        title = findViewById(R.id.songName);
        prev = findViewById(R.id.previous);
        next = findViewById(R.id.next);
        title.setSelected(true);

        adapter = new TrackAdapter();
        adapter.setData(player.getRadioList());
        radioList.setAdapter(adapter);
        registerForContextMenu(radioList);

        setRadioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    URL url = new URL(radioUrl.getText().toString());
                    if (!url.toString().startsWith("http://") && url.toString().startsWith("http")
                            || !url.toString().startsWith("https://") && url.toString().startsWith("https"))
                        throw new MalformedURLException("Неверный формат URL");
                    stopService(App.getApp().getPlayerService());
                    player.setSource(url.toString());;
                    player.addRadio(new Track(radioTitle.getText().toString(), radioUrl.getText().toString()));
                    player.setCurrentRadio(player.getRadioListSize() - 1);
                    adapter.setData(player.getRadioList());
                    adapter.notifyDataSetChanged();
                    updateTitle();
                    player.setIsPlaying(true);
                    player.setIsAnotherSong(true);
                    play.setBackgroundResource(R.drawable.ic_pause);
                    startService(App.getApp().getPlayerService());
                } catch (MalformedURLException e) {
                    Toast.makeText(RadioActivity.this, "Неверный формат URL", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        radioList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (player.getCurrentRadio() == position) return;
                player.setCurrentRadio(position);
                stopService(App.getApp().getPlayerService());
                player.setIsPlaying(true);
                player.setIsAnotherSong(true);
                player.setSource(player.getCurrentRadioTrack().getPath());
                play.setBackgroundResource(R.drawable.ic_pause);
                updateTitle();
                startService(App.getApp().getPlayerService());
                createRadioNotification(R.drawable.ic_pause);
            }
        });
        radioList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                player.setCurrentRadio(position);
                return false;
            }
        });
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player.isPlaying()) {
                    if (player.getSource().equals(".")) {
                        createTrackNotification(R.drawable.ic_play);
                    }
                    else {
                        createRadioNotification(R.drawable.ic_play);
                    }

                    player.setIsPlaying(false);
                    play.setBackgroundResource(R.drawable.ic_play);
                    stopService(App.getApp().getPlayerService());
                } else {
                    if (player.getMediaPlayer() == null) player.setCurrentSong(0);
                    if (player.getSource().equals(".")) {
                        createTrackNotification(R.drawable.ic_pause);
                    }
                    else {
                        createRadioNotification(R.drawable.ic_pause);
                    }

                    player.setIsPlaying(true);
                    play.setBackgroundResource(R.drawable.ic_pause);
                    startService(App.getApp().getPlayerService());
                }
                player.setIsAnotherSong(false);
            }
        });
        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player.getMediaPlayer() == null) return;
                if (player.getSource().equals(".") && player.getCurrentSong() - 1 >= 0) {
                    moveTrack(-1);
                    createTrackNotification(R.drawable.ic_pause);
                }
                else if (!player.getSource().equals(".") && player.getCurrentRadio() - 1 >= 0) {
                    moveRadio(-1);
                    createRadioNotification(R.drawable.ic_pause);
                }
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player.getMediaPlayer() == null) return;
                if (player.getSource().equals(".") && player.getCurrentSong() + 1 < player.getQueueSize()) {
                    moveTrack(1);
                    createTrackNotification(R.drawable.ic_pause);
                }
                else if (!player.getSource().equals(".") && player.getCurrentRadio() +1 < player.getRadioListSize()) {
                    moveRadio(1);
                    createRadioNotification(R.drawable.ic_pause);
                }
            }
        });
        title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!player.getSource().equals(".")) return;
                if (player.getCurrentPath().equals("")) return;
                if (player.isPlaying()) player.setMediaPlayerCurrentPosition(player.getCurrentPosition());
                Intent intent = new Intent(getApplicationContext(), SongActivity.class);
                startActivity(intent);
            }
        });
    }

    void moveTrack(int direction) {
        player.setWasSongSwitched(true);
        player.setCurrentSong(player.getCurrentSong() + direction);
        stopService(App.getApp().getPlayerService());
        player.setIsAnotherSong(true);
        updateTitle();
        startService(App.getApp().getPlayerService());
    }

    void moveRadio(int direction) {
        stopService(App.getApp().getPlayerService());
        player.setCurrentRadio(player.getCurrentRadio() + direction);
        player.setSource(player.getCurrentRadioTrack().getPath());
        player.setIsAnotherSong(true);
        player.setWasSongSwitched(true);
        updateTitle();
        startService(App.getApp().getPlayerService());
    }

    void updateTitle() {
        if (player.getSource().equals(".") && !title.getText().equals(player.getCurrentTitle())) {
            title.setText(player.getCurrentTitle());
        }
        else if (!player.getSource().equals(".") && !title.getText().equals(player.getCurrentRadioTrack().getName())) title.setText(player.getCurrentRadioTrack().getName());
    }

    void createTrackNotification(int index) {
        CreateNotification.createNotification(getApplicationContext(),
                player.getCurrentTrack(),
                index,
                player.getCurrentSong(),
                player.getQueueSize()-1);
    }

    void createRadioNotification(int index) {
        CreateNotification.createNotification(getApplicationContext(),
                player.getCurrentRadioTrack(),
                index,
                player.getCurrentRadio(),
                player.getRadioListSize() - 1);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CreateNotification.CHANNEL_ID,
                    "KOD DEV", NotificationManager.IMPORTANCE_LOW);

            notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (item.getTitle().equals("Delete")) {
            player.removeRadio(player.getCurrentRadio());
            adapter.setData(player.getRadioList());
            adapter.notifyDataSetChanged();
        }
        return true;
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getExtras().getString("actionName");

            switch (action) {
                case CreateNotification.ACTION_PREVIOUS:
                    onTrackPrevious();
                    break;
                case CreateNotification.ACTION_PLAY:
                    if (!player.isPlaying()) onTrackPause();
                    else onTrackPlay();
                    break;
                case CreateNotification.ACTION_NEXT:
                    onTrackNext();
                    break;
            }
        }
    };

    @Override
    public void onTrackPrevious() {
        title.setText(player.getCurrentRadioTrack().getName());
        play.setBackgroundResource(R.drawable.ic_pause);
    }

    @Override
    public void onTrackPlay() {
        play.setBackgroundResource(R.drawable.ic_pause);
    }

    @Override
    public void onTrackPause() {
        play.setBackgroundResource(R.drawable.ic_play);
    }

    @Override
    public void onTrackNext() {
        title.setText(player.getCurrentRadioTrack().getName());
        play.setBackgroundResource(R.drawable.ic_pause);
    }

    private void startTitleThread() {
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            public void run() {
                while (running) {
                    try {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    handler.post(new Runnable(){
                        public void run() {
                            if (player.getMediaPlayer() == null) return;
                            updateTitle();
                        }
                    });
                }
            }
        };
        new Thread(runnable).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        running = false;
    }
}