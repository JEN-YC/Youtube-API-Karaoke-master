package com.miller.p2p;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.*;
import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;

public class MainActivity extends YouTubeBaseActivity implements YouTubePlayer.OnInitializedListener {
    public static ServerClass serverClass = null;
    public static ClientClass clientClass = null;
    WifiManager wifiManager;
    AudioManager audioManager;
    public static WifiP2pManager mManager;
    public static WifiP2pManager.Channel mChannel;
    YouTubePlayerView playerView;
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    TextView status, result_text, history_text;
    EditText search;
    Button search_btn;
    VideoPojo init_viedo = new VideoPojo();
    List<VideoPojo> searchResults;
    List<VideoPojo> songList = new ArrayList<>();
    Handler handler, msg_handler;
    YoutubeAdapter searchAdapter, historyAdapter;
    YouTubePlayer player;
    YoutubeAPI youtubeAPI;
    public static Toast toast;
    List<WifiP2pDevice> peers = new ArrayList<>();
    String[] device_names;
    Boolean isFullScreen = false;
    ListView history_listview, result_listview;

    static final int MESSAGE_READ = 1;
    final int PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialWork();
        processViews();
        processControllers();
    }

    public static void makeTextAndShow(final Context context, final String text, final int duration) {
        if (toast == null) {
            //如果還沒有用過makeText方法，才使用
            toast = android.widget.Toast.makeText(context, text, duration);
        } else {
            toast.setText(text);
            toast.setDuration(duration);
        }
        toast.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reason) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if(getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//        }
        registerReceiver(mReceiver, mIntentFilter);
        processControllers();
    }

    private void processViews() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        youtubeAPI = new YoutubeAPI(MainActivity.this);
        result_listview = findViewById(R.id.result_list);
        history_listview = findViewById(R.id.history_list);
        history_text = findViewById(R.id.history_textView);
        result_text = findViewById(R.id.search_result);
        handler = new Handler();
        search_btn = findViewById(R.id.search_btn);
        search = findViewById(R.id.search);
        playerView = findViewById(R.id.video);
        playerView.initialize(YoutubeAPI.KEY, this);
        status = findViewById(R.id.status);
        // AddFirstVideo();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                popular_songs = youtubeAPI.getPopular();
//                handler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (popular_songs.size() > 0) {
//                            playlistAdapter = new PlaylistAdapter(MainActivity.this, popular_songs);
//                            popular_listview.setAdapter(playlistAdapter);
//
//                        } else {
//                            popular_listview.setVisibility(View.GONE);
//                            popular_text.setVisibility(View.GONE);
//                        }
//                    }
//                });
//            }
//        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    private void initialWork() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
            }
        } else { }
        msg_handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_READ:
                        byte[] readBuffer = (byte[]) msg.obj;
                        String tempMsg = new String(readBuffer, 0, msg.arg1);
                        try {
                            JSONObject json = new JSONObject(tempMsg);
                            int command = Integer.parseInt((String) json.get("command"));
                            switch (command) {
                                case 1:
                                    // ORDER
                                    String video_id = (String) json.get("ID");
                                    String URL = (String) json.get("URL");
                                    String Title = (String) json.get("Title");
                                    String description = (String) json.get("description");
                                    try {
                                        if (songList.size() == 0)
                                            player.loadVideo(video_id);
                                    }catch (IllegalStateException e){
                                        e.printStackTrace();
                                    }
                                    VideoPojo pojo = new VideoPojo();
                                    pojo.setId(video_id);
                                    pojo.setDescription(description);
                                    pojo.setThumbnailURL(URL);
                                    pojo.setTitle(Title);
                                    songList.add(pojo);
                                    if(historyAdapter == null){
                                        historyAdapter = new YoutubeAdapter(MainActivity.this, songList);
                                        history_listview.setAdapter(historyAdapter);
                                        break;
                                    }
                                    refresh();
                                    break;
                                case 2:
                                    // CHANGE VOLUME
                                    int value = Integer.parseInt((String)json.get("value"));
                                    int max_volume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                                    int adjust = abs(max_volume * value / 100);
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, adjust, 0);
//                                    audioManager.adjustVolume(value, 0);
                                    break;
//                                case 3:
//                                    // DOWN
//                                    audioManager.adjustVolume(AudioManager.ADJUST_LOWER, 0);
//                                    break;
                                case 4:
                                    // REPLAY
                                    player.seekToMillis(0);
                                    break;
                                case 5:
                                    // STOP
                                    player.pause();
                                    break;
                                case 6:
                                    // NEXT
                                    if (songList.size() > 1) {
                                        VideoPojo next_video = songList.get(1);
                                        songList.remove(0);
                                        history_listview.invalidateViews();
                                        player.loadVideo(next_video.getId());
                                    }
                                    else
                                        player.seekToMillis(player.getDurationMillis()-1);
                                    break;
                                case 7:
                                    //PLAY
                                    player.play();
                                    break;
                                case 8:
                                    //FULL SCREEN
                                    if(isFullScreen) {
                                        player.setFullscreen(false);
                                        isFullScreen = false;
                                    }
                                    else{
                                        player.setFullscreen(true);
                                        isFullScreen = true;
                                    }
                                    break;
//                                case 9:
//                                    // MUTE
//                                    audioManager.adjustVolume(AudioManager.ADJUST_MUTE, 0);
//                                    break;
                            }
                        } catch (Exception e) {
                            makeTextAndShow(getApplicationContext(), "請重啟app", Toast.LENGTH_LONG);
                        }
                }
                return true;
            }
        });
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                status.setText("等待被連接");
            }

            @Override
            public void onFailure(int reason) {
                status.setText("開啟搜尋失敗");
            }
        });
    }


    private void processControllers() {
        result_listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (songList.size() == 0)
                    player.loadVideo(searchResults.get(position).getId());

                songList.add(searchResults.get(position));
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (songList.size() > 0) {
                            historyAdapter = new YoutubeAdapter(MainActivity.this, songList);
                            history_listview.setAdapter(historyAdapter);
                            result_text.setVisibility(View.INVISIBLE);
                            result_listview.setVisibility(View.INVISIBLE);
                            history_listview.setVisibility(View.VISIBLE);
                            history_text.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        });
        search_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (search.getText().toString().equals("")) {
                } else {
                    new Thread() {
                        public void run() {
                            searchResults = youtubeAPI.search(search.getText().toString());
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    search.setText("");
                                    if (searchResults.size() > 0) {
                                        searchAdapter = new YoutubeAdapter(MainActivity.this, searchResults);
                                        result_listview.setAdapter(searchAdapter);
                                        result_listview.setVisibility(View.VISIBLE);
                                        result_text.setVisibility(View.VISIBLE);
                                        history_text.setVisibility(View.INVISIBLE);
                                        history_listview.setVisibility(View.INVISIBLE);
                                    }
                                }
                            });
                        }
                    }.start();
                }
            }
        });
    }

    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            final InetAddress groupOwnerAddress = info.groupOwnerAddress;
            Log.e("owner", String.valueOf(info.isGroupOwner));
            if (info.groupFormed && info.isGroupOwner) {
                makeTextAndShow(MainActivity.this, "已與遙控器連線", Toast.LENGTH_SHORT);
                status.setText(R.string.connect_user);
                serverClass = new ServerClass();
                serverClass.start();
            }
            else if (info.groupFormed){
                makeTextAndShow(MainActivity.this, "已與遙控器連線", Toast.LENGTH_SHORT);
                status.setText(R.string.connect_user);
                clientClass = new ClientClass(groupOwnerAddress);
                clientClass.start();
                Log.e("111", "good");
            }
        }
    };
    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            if(!peerList.getDeviceList().equals(peers)){
                peers.clear();
                peers.addAll(peerList.getDeviceList());
                device_names = new String[peerList.getDeviceList().size()];

                int i = 0;
                for(WifiP2pDevice wifiP2pDevice : peerList.getDeviceList()){
                    device_names[i] = wifiP2pDevice.deviceName;
                    i++;
                }

            }

            if(peers.size() == 0){
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        status.setText("可以開始被連接");
                    }

                    @Override
                    public void onFailure(int reason) {
                        status.setText("開啟搜尋失敗");
                    }
                });
            }
        }
    };

    public class ClientClass extends Thread {
        Socket socket;
        String hostAddr;

        public ClientClass(InetAddress hostAddress) {
            hostAddr = hostAddress.getHostAddress();
            socket = new Socket();
        }

        @Override
        public void run() {
            try {
                socket.connect(new InetSocketAddress(hostAddr, 8889), 500);
                ReceiveClass receiveClass = new ReceiveClass(socket);
                receiveClass.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class ServerClass extends Thread {
        Socket socket;
        ServerSocket serverSocket;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(8889));
                while(true) {
                    socket = serverSocket.accept();
                    ReceiveClass receiveClass = new ReceiveClass(socket);
                    receiveClass.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ReceiveClass extends Thread {
        private Socket socket;
        private DataInputStream inputStream;

        public ReceiveClass(Socket socket) {
            this.socket = socket;
            try {
                inputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            byte[] buffer = new byte[16384];
            int bytes;

            while (socket != null) {
                try {
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        msg_handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, final YouTubePlayer youTubePlayer, boolean b) {
        if (youTubePlayer == null) return;
        player = youTubePlayer;
        // youTubePlayer.loadVideo(FIRST_VIDEO_ID);
        youTubePlayer.setPlayerStyle(YouTubePlayer.PlayerStyle.CHROMELESS);
        youTubePlayer.setPlayerStateChangeListener(new YouTubePlayer.PlayerStateChangeListener() {
            @Override
            public void onLoading() {

            }

            @Override
            public void onLoaded(String s) {

            }

            @Override
            public void onAdStarted() {

            }

            @Override
            public void onVideoStarted() {

            }

            @Override
            public void onVideoEnded() {
                if (songList.size() > 1) {
                    VideoPojo next_video = songList.get(1);
                    songList.remove(0);
                    history_listview.invalidateViews();
                    youTubePlayer.loadVideo(next_video.getId());
                }
            }

            @Override
            public void onError(YouTubePlayer.ErrorReason errorReason) {
            }
        });
    }

    private void AddFirstVideo(){
        init_viedo.setTitle("蔡依林 Jolin Tsai《玫瑰少年 Womxnly》Official Dance Video");
        init_viedo.setId("feOq6MWeUXA");
        init_viedo.setThumbnailURL("https://i.ytimg.com/vi/feOq6MWeUXA/default.jpg");
        init_viedo.setDescription("「Love your body」尊重身體的直覺反應♬ 數位收聽: https://jolin.lnk.to/UglyBeauty 訂閱蔡依林頻道: https://sonymusic.pse.is/jolin Jolin從｢玫瑰少年」音樂創...");
        songList.add(init_viedo);
        historyAdapter = new YoutubeAdapter(MainActivity.this, songList);
        history_listview.setAdapter(historyAdapter);
    }
    private void refresh(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                historyAdapter.notifyDataSetChanged();
            }
        });
    }
    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
        makeTextAndShow(this, "Youtube 撥放器載入失敗，請重啟app", Toast.LENGTH_LONG);

    }
}
