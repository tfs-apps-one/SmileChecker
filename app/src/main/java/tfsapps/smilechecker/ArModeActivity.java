package tfsapps.smilechecker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.Image;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.DisplayMetrics;
import android.view.Display;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import com.google.android.gms.ads.AdSize;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

public class ArModeActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 1001;

    private PreviewView previewView;
    private OverlayView overlayView;
    private TextView faceResult;
    private ImageView star_1;
    private ImageView star_2;
    private ImageView star_3;

    private ExecutorService cameraExecutor;
    private FaceDetector faceDetector;
    private AdView mAdView;
    private boolean isEffectPlaying = false;

    // Settings
    private boolean isFaceFrame = true;
    private boolean isFaceMark = true;
    private boolean isFaceHighMark = false;
    private boolean isFaceValue = false;
    private String _language;

    // Current detection status
    private int human_num = 0;
    private int smile_num = 0;
    private int angry_num = 0;
    private int sad_num = 0;
    private int curious_num = 0;

    // UIのチラつき防止用のホールド変数
    private long lastSmileTime = 0;
    private static final long HOLD_DURATION_MS = 1500; // 1.5秒間ホールド
    // private static final long HOLD_DURATION_MS = 1000; // 1秒間ホールド
    private int hold_human_num = 0;
    private int hold_smile_num = 0;
    private int hold_angry_num = 0;
    private int hold_sad_num = 0;
    private int hold_curious_num = 0;

    // バナー広告
    // 本番ID
    private String AD_UNIT_ID_BANNER = "ca-app-pub-4924620089567925/5858003358";
    // テストID
    //private String AD_UNIT_ID_BANNER = "ca-app-pub-3940256099942544/6300978111";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar_mode);

        previewView = findViewById(R.id.previewView);
        overlayView = findViewById(R.id.overlayView);
        faceResult = findViewById(R.id.faceResult);
        star_1 = findViewById(R.id.result_1);
        star_2 = findViewById(R.id.result_2);
        star_3 = findViewById(R.id.result_3);

        Intent intent = getIntent();
        if (intent != null) {
            isFaceFrame = intent.getBooleanExtra("isFaceFrame", true);
            isFaceMark = intent.getBooleanExtra("isFaceMark", true);
            isFaceHighMark = intent.getBooleanExtra("isFaceHighMark", false);
            isFaceValue = intent.getBooleanExtra("isFaceValue", false);
        }

        Locale local = Locale.getDefault();
        _language = local.getLanguage();

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();
        faceDetector = FaceDetection.getClient(options);

        cameraExecutor = Executors.newSingleThreadExecutor();

        // Initialize AdMob and load banner ad
        MobileAds.initialize(this);
        loadBanner();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, new String[] { Manifest.permission.CAMERA }, REQUEST_CAMERA_PERMISSION);
        }
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::processImageProxy);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                boolean isFrontFacing = cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                // Ignore
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void processImageProxy(ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            boolean isFrontFacing = false; // Using DEFAULT_BACK_CAMERA
            int width = imageProxy.getHeight();
            int height = imageProxy.getWidth();
            int rotation = imageProxy.getImageInfo().getRotationDegrees();
            if (rotation == 0 || rotation == 180) {
                width = imageProxy.getWidth();
                height = imageProxy.getHeight();
            }

            final int fWidth = width;
            final int fHeight = height;

            faceDetector.process(image)
                    .addOnSuccessListener(faces -> {
                        human_num = faces.size();
                        smile_num = 0;
                        angry_num = 0;
                        sad_num = 0;
                        curious_num = 0;

                        List<OverlayView.FaceDrawInfo> faceInfos = new ArrayList<>();
                        for (Face face : faces) {
                            Rect bounds = face.getBoundingBox();
                            String emotionMessage = "";
                            String emotionValue = "";

                            if (face.getSmilingProbability() != null) {
                                float smileProb = face.getSmilingProbability();

                                // 99%以上で特別演出を発動
                                if (smileProb >= 0.99f) {
                                    showPerfectSmileEffect();
                                }

                                if (isFaceHighMark) {
                                    emotionMessage = getEmotion(face);
                                } else {
                                    emotionMessage = getEmotionBasice(face);
                                }
                                emotionValue = String.format("%.0f%%", smileProb * 100);
                            }

                            faceInfos.add(new OverlayView.FaceDrawInfo(
                                    new RectF(bounds), emotionMessage, emotionValue,
                                    isFaceFrame, isFaceMark, isFaceValue));
                        }

                        overlayView.setCameraInfo(fWidth, fHeight, isFrontFacing);
                        overlayView.setFaces(faceInfos);

                        // --- ホールド処理（チラつき防止） ---
                        long currentTime = System.currentTimeMillis();
                        if (smile_num > 0) {
                            // スマイルが検出されている場合は即座に結果を更新し、時刻を記録
                            hold_human_num = human_num;
                            hold_smile_num = smile_num;
                            hold_sad_num = sad_num;
                            hold_angry_num = angry_num;
                            hold_curious_num = curious_num;
                            lastSmileTime = currentTime;
                        } else {
                            // スマイルが検出されなくなった場合でも、一定時間は前のスマイル結果を保持する
                            if (currentTime - lastSmileTime > HOLD_DURATION_MS) {
                                // ホールド期間が過ぎたら、現在の（スマイルなしの）結果を反映
                                hold_human_num = human_num;
                                hold_smile_num = smile_num;
                                hold_sad_num = sad_num;
                                hold_angry_num = angry_num;
                                hold_curious_num = curious_num;
                            }
                        }

                        updateResultUI();
                    })
                    .addOnCompleteListener(task -> imageProxy.close());
        }
    }

    private String getEmotionBasice(Face face) {
        String emotionMessage = "Neutral";
        if (face.getSmilingProbability() != null) {
            float smileProb = face.getSmilingProbability();
            if (smileProb > 0.5) {
                emotionMessage = "Smile";
                smile_num++;
                return emotionMessage;
            }
        }
        return emotionMessage;
    }

    private String getEmotion(Face face) {
        String emotionMessage = "Neutral";
        if (face.getSmilingProbability() != null) {
            float smileProb = face.getSmilingProbability();
            if (smileProb >= 0.9) {
                emotionMessage = "Perfect Smile";
            } else if (smileProb > 0.75) {
                emotionMessage = "Smile";
            } else if (smileProb > 0.5) {
                emotionMessage = "Soft Smile";
            }
            if (smileProb > 0.5) {
                smile_num++;
                return emotionMessage;
            }
        }

        if (face.getLeftEyeOpenProbability() != null && face.getRightEyeOpenProbability() != null) {
            float leftEyeProb = face.getLeftEyeOpenProbability();
            float rightEyeProb = face.getRightEyeOpenProbability();
            float avgEyeOpen = (leftEyeProb + rightEyeProb) / 2;
            if (avgEyeOpen < 0.3) {
                emotionMessage = "Angry";
                angry_num++;
            }
        }

        if (face.getHeadEulerAngleX() > 20) {
            emotionMessage = "Sad";
            sad_num++;
        } else if (face.getHeadEulerAngleY() > 20 || face.getHeadEulerAngleY() < -20) {
            emotionMessage = "Curious";
            curious_num++;
        }

        return emotionMessage;
    }

    private void updateResultUI() {
        String message = "";
        float _percent = 0;

        String _mess1 = "";
        String _mess2 = "";
        String _mess3 = "";
        String _mess4 = "";
        String _mess5 = "";

        if (_language.equals("ja")) {
            _mess1 = "満点の【スマイル】です";
            _mess2 = "多くの【スマイル】があります";
            _mess3 = "少しの【スマイル】があります";
            _mess4 = "【顔を検出できません】";
            _mess5 = "【スマイル】がないかな？";
        } else {
            _mess1 = "Full [smile]";
            _mess2 = "Many [smile]";
            _mess3 = "A little [smile]";
            _mess4 = "Face cannot be detected !!";
            _mess5 = "Is there no [smile] !?";
        }

        if (hold_human_num > 0 && hold_smile_num > 0) {
            _percent = ((float) hold_smile_num / (float) hold_human_num);

            if (hold_human_num == hold_smile_num || _percent >= 1.0) {
                message = _mess1;
                star_1.setImageResource(R.drawable.star_ok);
                star_2.setImageResource(R.drawable.star_ok);
                star_3.setImageResource(R.drawable.star_ok);
            } else if (_percent >= 0.5) {
                message = _mess2;
                star_1.setImageResource(R.drawable.star_ok);
                star_2.setImageResource(R.drawable.star_ok);
                star_3.setImageResource(R.drawable.star_ng2);
            } else {
                message = _mess3;
                star_1.setImageResource(R.drawable.star_ok);
                star_2.setImageResource(R.drawable.star_ng2);
                star_3.setImageResource(R.drawable.star_ng2);
            }
        } else {
            if (hold_human_num == 0) {
                message = _mess4;
            } else {
                message = _mess5;
            }
            star_1.setImageResource(R.drawable.star_ng2);
            star_2.setImageResource(R.drawable.star_ng2);
            star_3.setImageResource(R.drawable.star_ng2);
        }

        if (_language.equals("ja")) {
            message += "\n" +
                    "\n　顔の検出　：" + hold_human_num +
                    "\n　スマイル　：" + hold_smile_num +
                    "\n　悲しい　　：" + hold_sad_num +
                    "\n　怒り・立腹：" + hold_angry_num +
                    "\n　興味・困惑：" + hold_curious_num +
                    "\n";
        } else {
            message += "\n" +
                    "\n Face-----> " + hold_human_num +
                    "\n Smile----> " + hold_smile_num +
                    "\n Sad------> " + hold_sad_num +
                    "\n Anger----> " + hold_angry_num +
                    "\n Curious--> " + hold_curious_num +
                    "\n";
        }

        final String finalMsg = message;
        runOnUiThread(() -> faceResult.setText(finalMsg));
    }

    public void onBackClick(View view) {
        finish();
    }

    public void onCameraAppClick(View view) {
        Intent takePictureIntent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
        try {
            startActivity(takePictureIntent);
        } catch (android.content.ActivityNotFoundException e) {
            takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            try {
                startActivity(takePictureIntent);
            } catch (android.content.ActivityNotFoundException e2) {
                Toast.makeText(this, "Camera app not found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // バナー広告（アダプティブ）のロード
    private void loadBanner() {
        mAdView = new AdView(this);
        // テスト用バナーID
        mAdView.setAdUnitId(AD_UNIT_ID_BANNER);

        FrameLayout adContainerView = findViewById(R.id.adContainerViewArMode);
        if (adContainerView != null) {
            adContainerView.removeAllViews();
            adContainerView.addView(mAdView);

            AdSize adSize = getAdSize();
            mAdView.setAdSize(adSize);

            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }
    }

    private AdSize getAdSize() {
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;

        int adWidth = (int) (widthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
    }

    // --- Perfect Smile 演出メソッド ---
    private void showPerfectSmileEffect() {
        if (isEffectPlaying)
            return;
        isEffectPlaying = true;

        runOnUiThread(() -> {
            TextView effectText = findViewById(R.id.perfectSmileEffectText);
            if (effectText == null) {
                isEffectPlaying = false;
                return;
            }

            effectText.setVisibility(View.VISIBLE);
            effectText.setAlpha(0f);
            effectText.setScaleX(0.5f);
            effectText.setScaleY(0.5f);

            ObjectAnimator scaleX = ObjectAnimator.ofFloat(effectText, "scaleX", 0.5f, 1.2f, 1.0f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(effectText, "scaleY", 0.5f, 1.2f, 1.0f);
            ObjectAnimator alphaIn = ObjectAnimator.ofFloat(effectText, "alpha", 0f, 1f);

            AnimatorSet bounceIn = new AnimatorSet();
            bounceIn.playTogether(scaleX, scaleY, alphaIn);
            bounceIn.setDuration(600);
            bounceIn.setInterpolator(new android.view.animation.OvershootInterpolator());

            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(effectText, "alpha", 1f, 0f);
            fadeOut.setStartDelay(2000); // 2秒間表示
            fadeOut.setDuration(500);

            AnimatorSet effectSet = new AnimatorSet();
            effectSet.playSequentially(bounceIn, fadeOut);
            effectSet.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    effectText.setVisibility(View.GONE);
                    isEffectPlaying = false;
                }
            });
            effectSet.start();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    protected void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
        cameraExecutor.shutdown();
        if (faceDetector != null) {
            faceDetector.close();
        }
    }
}
