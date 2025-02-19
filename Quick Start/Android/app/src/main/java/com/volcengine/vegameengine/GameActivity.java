/**
 * Copyright (c) 2022 Volcengine
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.volcengine.vegameengine;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;

import static com.volcengine.vegameengine.util.Feature.FEATURE_AUDIO;
import static com.volcengine.vegameengine.util.Feature.FEATURE_CAMERA;
import static com.volcengine.vegameengine.util.Feature.FEATURE_CLIPBOARD;
import static com.volcengine.vegameengine.util.Feature.FEATURE_FILE_CHANNEL;
import static com.volcengine.vegameengine.util.Feature.FEATURE_LOCAL_INPUT;
import static com.volcengine.vegameengine.util.Feature.FEATURE_LOCATION;
import static com.volcengine.vegameengine.util.Feature.FEATURE_MESSAGE_CHANNEL;
import static com.volcengine.vegameengine.util.Feature.FEATURE_PAD_CONSOLE;
import static com.volcengine.vegameengine.util.Feature.FEATURE_POD_CONTROL;
import static com.volcengine.vegameengine.util.Feature.FEATURE_PROBE_NETWORK;
import static com.volcengine.vegameengine.util.Feature.FEATURE_SENSOR;
import static com.volcengine.vegameengine.util.Feature.FEATURE_UNCLASSIFIED;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.volcengine.cloudcore.common.mode.CameraId;
import com.volcengine.cloudcore.common.mode.LocalVideoStreamError;
import com.volcengine.cloudcore.common.mode.LocalVideoStreamState;
import com.volcengine.cloudphone.apiservice.IClipBoardListener;
import com.volcengine.cloudphone.apiservice.IMessageChannel;
import com.volcengine.cloudphone.apiservice.IProbeNetworkListener;
import com.volcengine.cloudphone.apiservice.ProbeStats;
import com.volcengine.cloudphone.apiservice.StreamProfileChangeCallBack;
import com.volcengine.cloudphone.apiservice.outinterface.CameraManagerListener;
import com.volcengine.cloudphone.apiservice.outinterface.RemoteCameraRequestListener;
import com.volcengine.vegameengine.feature.AudioServiceView;
import com.volcengine.vegameengine.feature.CamaraManagerView;
import com.volcengine.vegameengine.feature.ClarityServiceView;
import com.volcengine.vegameengine.feature.ClipBoardServiceManagerView;
import com.volcengine.vegameengine.feature.FileChannelView;
import com.volcengine.vegameengine.feature.GroundManagerView;
import com.volcengine.vegameengine.feature.LocalInputManagerView;
import com.volcengine.vegameengine.feature.LocationServiceView;
import com.volcengine.vegameengine.feature.MessageChannelView;
import com.volcengine.vegameengine.feature.PadConsoleManagerView;
import com.volcengine.vegameengine.feature.PodControlServiceView;
import com.volcengine.vegameengine.feature.ProbeNetworkView;
import com.volcengine.vegameengine.feature.SensorView;
import com.volcengine.vegameengine.feature.UnclassifiedView;
import com.volcengine.vegameengine.util.DialogUtils;
import com.volcengine.vegameengine.util.Feature;
import com.volcengine.vegameengine.util.ScreenUtil;
import com.volcengine.androidcloud.common.log.AcLog;
import com.volcengine.androidcloud.common.model.StreamStats;
import com.volcengine.cloudcore.common.mode.LocalStreamStats;
import com.volcengine.cloudgame.GamePlayConfig;
import com.volcengine.cloudgame.VeGameEngine;
import com.volcengine.cloudphone.apiservice.outinterface.IGamePlayerListener;
import com.volcengine.cloudphone.apiservice.outinterface.IStreamListener;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GameActivity extends AppCompatActivity
        implements IGamePlayerListener, IStreamListener {

    private final String TAG = getClass().getSimpleName();
    private ViewGroup mContainer;
    public static final String KEY_PARAM_GAME_ID = "gameId";
    public static final String KEY_ROUND_ID = "roundId";
    public static final String KEY_ClARITY_ID = "clarityId";
    public static final String KEY_FEATURE_ID = "featureId";
    private ConstraintLayout mContainers;

    private boolean mIsHideButtons = false;
    public VeGameEngine veGameEngine = VeGameEngine.getInstance();
    DialogUtils.DialogWrapper mDialogWrapper;
    FileChannelView mFileChannelView;
    private GamePlayConfig mGamePlayConfig;

    private Button btnAudio, btnCamera, btnClarity, btnClipBoard, btnFileChannel, btnGround, btnLocation;
    private Button btnMessageChannel, btnPodControl, btnRotation, btnSensor, btnUnclassified;
    private Button btnProbeNetwork, btnLocalInput, btnPadConsole;
    private TextView tvInfo;
    private boolean isLand = false;
    private boolean isShowInfo = false;
    private long lastBackPress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenUtil.adaptHolePhone(this);
        setContentView(R.layout.activity_play);
        mContainer = findViewById(R.id.container);
        initView();
        initConfig();
    }

    private void initConfig() {
        GamePlayConfig.Builder builder = new GamePlayConfig.Builder();
        String userId = "userid" + System.currentTimeMillis();
        AcLog.d(TAG, "userId: " + userId);
        Intent intent = getIntent();
        String ak = "your_ak";
        String sk = "your_sk";
        String token = "your_token";

        // ak, sk, token: 请通过火山引擎申请ak获得，详情见https://www.volcengine.com/docs/6512/75577
        builder.userId(userId) // 用户userid
                .ak(ak) // 必填 ACEP ak
                .sk(sk)  // 必填 ACEP sk
                .token(token) // 必填 ACEP session
                .container(mContainer)//必填参数，用来承载画面的 Container, 参数说明: layout 需要是FrameLayout或者FrameLayout的子类
                .roundId(intent.getStringExtra(KEY_ROUND_ID))//必填参数，自定义roundId
                .videoStreamProfileId(intent.getIntExtra(KEY_ClARITY_ID, 1)) // 选填参数，清晰度ID
                .gameId(intent.getStringExtra(KEY_PARAM_GAME_ID)) //必填, gameId
                .enableAcceleratorSensor(true)
                .enableGravitySensor(true)
                .enableGyroscopeSensor(true)
                .enableMagneticSensor(true)
                .enableOrientationSensor(true)
                .enableVibrator(true)
                .enableLocationService(true)
                .enableLocalKeyboard(true)
                .enableFileChannel(true)
                .streamListener(GameActivity.this);

        mGamePlayConfig = builder.build();
        // 初始化成功才可以调用
        veGameEngine.start(mGamePlayConfig, GameActivity.this);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString("key_uid", "user_id");
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        veGameEngine.resume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        super.onPause();
        veGameEngine.pause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onDestroy() {
        veGameEngine.stop();
        if (mDialogWrapper != null) {
            mDialogWrapper.release();
            mDialogWrapper = null;
        }
        if (mFileChannelView != null) {
            mFileChannelView = null;
        }
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mFileChannelView != null) {
            mFileChannelView.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onPlaySuccess(String roundId, int clarityId, Map<String, String> extraMap, String gameId,
                              String reservedId) {
        AcLog.d(TAG, "roundId " + roundId + " clarityId " + clarityId + "extra:" + extraMap +
                "gameId : " + gameId + " reservedId" + reservedId);
        VeGameEngine.getInstance().getCameraManager().setRemoteRequestListener(new RemoteCameraRequestListener() {
            @Override
            public void onVideoStreamStartRequested(CameraId cameraId) {
                AcLog.d(TAG, "onVideoStreamStartRequested, cameraId :" + cameraId);
                VeGameEngine.getInstance().getCameraManager().startVideoStream(cameraId);
            }

            @Override
            public void onVideoStreamStopRequested() {
                AcLog.d(TAG, "onVideoStreamStopRequested ");
                VeGameEngine.getInstance().getCameraManager().stopVideoStream();
            }
        });
        VeGameEngine.getInstance().getCameraManager().setCameraManagerListener(new CameraManagerListener() {
            @Override
            public void onLocalVideoStateChanged(LocalVideoStreamState localVideoStreamState, LocalVideoStreamError errorCode) {
                AcLog.d(TAG, "LocalVideoStreamState" + localVideoStreamState.toString() + ",LocalVideoStreamError" + errorCode);
            }

            @Override
            public void onFirstCapture() {
                AcLog.d(TAG, "onFirstCapture");
            }
        });

        veGameEngine.getClarityService().setStreamProfileChangeListener(new StreamProfileChangeCallBack() {
            @Override
            public void onVideoStreamProfileChange(boolean isSuccess, int from, int to) {
                AcLog.d(TAG, "VideoStreamProfileChange  isSuccess " + isSuccess + "from " + from + "to " + to);
            }

            @Override
            public void onError(int i, String s) {
                AcLog.d(TAG, "onError - " + s);
            }
        });

        veGameEngine.getClipBoardServiceManager().setBoardSyncClipListener(new IClipBoardListener() {
            @Override
            public void onClipBoardMessageReceived(ClipData clipData) {
                AcLog.d(TAG, "clipBoard : " + clipData.toString());
            }
        });
//        tvInfo.setText("roundId:" + roundId + "\n" + "streamProfile:" + clarityId);
        tvInfo.setText(String.format(
                "roundId: %s\nstreamProfile: %s\nextraMap: %s\ngameId: %s\nreservedId: %s\n",
                roundId,
                clarityId,
                extraMap,
                gameId,
                reservedId
        ));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tvInfo.setZ(Long.MAX_VALUE);
        }
    }

    private void initView() {
        mContainer = findViewById(R.id.container);
        mContainers = findViewById(R.id.cl_container);
        tvInfo = findViewById(R.id.tv_info);

        btnAudio = findViewById(R.id.btn_audio);
        btnCamera = findViewById(R.id.btn_camera);
        btnClarity = findViewById(R.id.btn_clarity);
        btnClipBoard = findViewById(R.id.btn_clipboard);
        btnFileChannel = findViewById(R.id.btn_file_channel);
        btnGround = findViewById(R.id.btn_ground);
        btnLocation = findViewById(R.id.btn_location);
        btnMessageChannel = findViewById(R.id.btn_message_channel);
        btnPodControl = findViewById(R.id.btn_pod_control);
        btnPadConsole = findViewById(R.id.btn_pad_console);
        btnRotation = findViewById(R.id.btn_orientation);
        btnSensor = findViewById(R.id.btn_sensor);
        btnUnclassified = findViewById(R.id.btn_unclassified);
        btnProbeNetwork = findViewById(R.id.btn_probe_network);
        btnLocalInput = findViewById(R.id.btn_local_input);

        findViewById(R.id.btn_show_info).setOnClickListener(v -> {
            isShowInfo = !isShowInfo;
            tvInfo.setVisibility(isShowInfo ? View.VISIBLE : View.GONE);
        });

        findViewById(R.id.btn_show_or_hide).setOnClickListener(v -> {
            mIsHideButtons = !mIsHideButtons;
            mContainers.setVisibility(mIsHideButtons ? View.GONE : View.VISIBLE);
        });

        if (veGameEngine.getClarityService() != null) {
            new ClarityServiceView(this, veGameEngine.getClarityService(), btnClarity);
        } else {
            AcLog.d(TAG, "ClarityService is null!");
        }

        btnRotation.setOnClickListener(view -> {
            if (isLand) {
                setRotation(270);
            } else {
                setRotation(0);
            }
            isLand = !isLand;
        });

        switch (getIntent().getIntExtra(KEY_FEATURE_ID, -1)) {
            case FEATURE_AUDIO:
                btnAudio.setVisibility(View.VISIBLE);
                btnAudio.setOnClickListener(view -> {
                    if (veGameEngine.getAudioService() != null) {
                        mDialogWrapper = DialogUtils.wrapper(
                                new AudioServiceView(this, veGameEngine.getAudioService()));
                        mDialogWrapper.show();
                    } else {
                        AcLog.d(TAG, "AudioService is null!");
                    }
                });
                break;
            case FEATURE_CAMERA:
                if (veGameEngine.getCameraManager() != null) {
                    new CamaraManagerView(this, veGameEngine.getCameraManager(), btnCamera);
                } else {
                    AcLog.d(TAG, "CameraManager is null!");
                }
                break;
            case FEATURE_CLIPBOARD:
                if (veGameEngine.getClipBoardServiceManager() != null) {
                    new ClipBoardServiceManagerView(this, veGameEngine.getClipBoardServiceManager(), btnClipBoard);
                } else {
                    AcLog.d(TAG, "ClipBoardServiceManager is null!");
                }
                break;
            case FEATURE_FILE_CHANNEL:
                btnFileChannel.setVisibility(View.VISIBLE);
                btnFileChannel.setOnClickListener(view -> {
                    if (veGameEngine.getFileChannel() != null) {
                        mFileChannelView = new FileChannelView(this, veGameEngine.getFileChannel());
                        mDialogWrapper = DialogUtils.wrapper(mFileChannelView);
                        mDialogWrapper.show();
                    } else {
                        AcLog.d(TAG, "FileChannel is null!");
                    }
                });
                break;
            case FEATURE_LOCAL_INPUT:
                if (veGameEngine.getLocalInputManager() != null) {
                    new LocalInputManagerView(this, veGameEngine.getLocalInputManager(), btnLocalInput);
                } else {
                    AcLog.d(TAG, "LocalInputManager is null!");
                }
                break;
            case FEATURE_LOCATION:
                if (veGameEngine.getLocationService() != null) {
                    new LocationServiceView(this, veGameEngine.getLocationService(), btnLocation);
                } else {
                    AcLog.d(TAG, "LocationService is null!");
                }
                break;
            case FEATURE_MESSAGE_CHANNEL:
                if (veGameEngine.getMessageChannel() != null) {
                    new MessageChannelView(this, veGameEngine.getMessageChannel(), btnMessageChannel);
                } else {
                    AcLog.d(TAG, "MessageChannel is null!");
                }
                break;
            case FEATURE_PAD_CONSOLE:
                if (veGameEngine.getGamePadService() != null) {
                    new PadConsoleManagerView(this, veGameEngine.getGamePadService(), btnPadConsole);
                } else {
                    AcLog.d(TAG, "GamePadService is null!");
                }
                break;
            case FEATURE_POD_CONTROL:
                if (veGameEngine.getPodControlService() != null) {
                    new PodControlServiceView(this, veGameEngine.getPodControlService(), btnPodControl);
                } else {
                    AcLog.d(TAG, "PodControlService is null!");
                }
                break;
            case FEATURE_PROBE_NETWORK:
                btnProbeNetwork.setVisibility(View.VISIBLE);
                btnProbeNetwork.setOnClickListener(view -> {
                    final ProbeNetworkView dialog = new ProbeNetworkView(this, v -> veGameEngine.probeInterrupt());
                    dialog.showProbeNetworkDialogForGame(mGamePlayConfig);
                });
                break;
            case FEATURE_SENSOR:
                new SensorView(this, btnSensor);
                break;
            case FEATURE_UNCLASSIFIED:
                new UnclassifiedView(this, btnUnclassified);
                break;
            default:
                break;
        }
    }

    private void setRotation(int rotation) {
        switch (rotation) {
            case 0:
            case 180:
                setRequestedOrientation(SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                break;
            case 90:
            case 270:
                setRequestedOrientation(SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                break;
        }
    }

    @Override
    public void onError(int i, String s) {
        String msg = "onError:" + i + ", " + s;
        Toast.makeText(this, "code" + i + "msg" + s, Toast.LENGTH_SHORT).show();
        Log.e(TAG, msg);
    }

    @Override
    public void onWarning(int i, String s) {
        Log.d(TAG, "warn: code " + i + ", msg" + s);
    }

    @Override
    public void onNetworkChanged(int i) {
        Log.d(TAG, String.format("%d", i));
    }


    public static void startGame(
            String gameId,
            String roundId,
            int clarityId,
            Activity activity,
            int featureId) {
        Intent intent = new Intent(activity, GameActivity.class);
        intent.putExtra(GameActivity.KEY_PARAM_GAME_ID, gameId);
        if (roundId.isEmpty() || roundId.equals("")) roundId = "123";
        intent.putExtra(GameActivity.KEY_ROUND_ID, roundId);
        intent.putExtra(GameActivity.KEY_ClARITY_ID, clarityId);
        intent.putExtra(GameActivity.KEY_FEATURE_ID, featureId);
        activity.startActivity(intent);
    }

    @Override
    public void onFirstAudioFrame(String s) {
        Log.d(TAG, "onFirstAudioFrame " + s);
    }

    @Override
    public void onFirstRemoteVideoFrame(String s) {
        Log.d(TAG, "onFirstRemoteVideoFrame " + s);
    }

    @Override
    public void onStreamStarted() {
        Log.d(TAG, "onStreamStarted ");
    }

    @Override
    public void onStreamPaused() {
        Log.d(TAG, "onStreamPaused ");
    }

    @Override
    public void onStreamResumed() {
        Log.d(TAG, "onStreamResumed ");
    }

    @Override
    public void onStreamStats(StreamStats streamStats) {
        Log.d(TAG, " " + streamStats.getDecoderOutputFrameRate() + " " +
                streamStats.getStallCount() + " " +
                streamStats.getReceivedResolutionHeight() + " " +
                streamStats.getReceivedResolutionWidth() + " " +
                streamStats.getRendererOutputFrameRate() + " " +
                streamStats.getDecoderOutputFrameRate() + " " +
                streamStats.getReceivedAudioBitRate() + " " +
                streamStats.getReceivedVideoBitRate());
    }

    @Override
    public void onLocalStreamStats(LocalStreamStats localStreamStats) {
        AcLog.d(TAG, "LocalStreamStats" + localStreamStats);
    }

    @Override
    public void onStreamConnectionStateChanged(int i) {
        Log.d(TAG, "onStreamConnectionStateChanged " + i);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        AcLog.d(TAG, "onConfigurationChanged newConfig " + newConfig.orientation);
        VeGameEngine.getInstance().rotate(newConfig.orientation);
    }

    @Override
    public void onDetectDelay(long l) {
        Log.d(TAG, "delay " + l);
    }

    @Override
    public void onRotation(int i) {
        Log.d(TAG, "rotation" + i);
        setRotation(i);
    }

    @Override
    public void onPodExit(int i, String s) {
        Log.d(TAG, "onPodExit" + i + " ,msg:" + s);
    }

    @Override
    public void onBackPressed() {
        long current = System.currentTimeMillis();
        if (current - lastBackPress < 1000L) {
            super.onBackPressed();
        }
        else {
            Toast.makeText(this, getString(R.string.back_again_to_exit), Toast.LENGTH_SHORT).show();
            lastBackPress = current;
        }
    }
}
