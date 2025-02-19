package tfsapps.smilechecker;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView imageView;
    private TextView txtResult;
    private Uri imageUri;
    private Bitmap _bitmap = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        txtResult = findViewById(R.id.txtResult);
        Button btnChooseImage = findViewById(R.id.btnChooseImage);

        btnChooseImage.setOnClickListener(v -> openGallery());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            imageView.setImageURI(imageUri);
            showProgressDialog(this);
            analyzeImage();
            detectFaces();
        }
    }

    private void analyzeImage() {
        if (imageUri == null) return;

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            _bitmap = bitmap;
            InputImage image = InputImage.fromBitmap(bitmap, 0);

            ImageLabeler labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);
            labeler.process(image)
                    .addOnSuccessListener(labels -> displayResults(labels))
                    .addOnFailureListener(e -> txtResult.setText("判別失敗: " + e.getMessage()));

        } catch (IOException e) {
            txtResult.setText("画像の読み込みに失敗しました");
        }
    }

    private void displayResults(List<ImageLabel> labels) {
        StringBuilder resultText = new StringBuilder();
        for (ImageLabel label : labels) {
            String labelText = label.getText(); // 昆虫の名前
            float confidence = label.getConfidence(); // 信頼度 (0.0 - 1.0)

            if (confidence > 0.7) { // 信頼度が70%以上のものだけ表示
                resultText.append(labelText).append(" (").append(String.format("%.1f", confidence * 100)).append("%)\n");
            }
        }

        if (resultText.length() > 0) {
            txtResult.setText(resultText.toString());
        } else {
            txtResult.setText("昆虫が識別できませんでした");
        }
    }

    private void detectFaces() {
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
//                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST) // 軽量・高速化
//                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL) // 目・鼻・口の位置を検出（横顔対応）
//                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
//                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE) // 顔の有無のみ判定
//                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE) // 精度重視  検出優先
//                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
//                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE) // 処理速度を優先   感情も
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL) // 顔の特徴点全てを検出
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // 表情を分類
                        .build();

        FaceDetector detector = FaceDetection.getClient(options);

        InputImage image = InputImage.fromBitmap(_bitmap, 0);
        detector.process(image)
                .addOnSuccessListener(faces -> {

                    // Bitmapのコピーを作成（元画像を変更せずに描画するため）
                    Bitmap mutableBitmap = _bitmap.copy(Bitmap.Config.ARGB_8888, true);
                    Canvas canvas = new Canvas(mutableBitmap);
                    Paint paint = new Paint();
                    paint.setColor(Color.GREEN);  // 緑色の枠
                    paint.setStyle(Paint.Style.STROKE);  // 枠線のみ描画
                    paint.setStrokeWidth(8);  // 枠線の太さを8pxに設定

                    // テキスト用のペイントオブジェクト
                    Paint textPaint = new Paint();
                    textPaint.setColor(Color.GREEN);
                    textPaint.setTextSize(110);
                    textPaint.setStyle(Paint.Style.FILL);

                    // テキスト用のペイントオブジェクト
                    Paint textValuePaint = new Paint();
                    textValuePaint.setColor(Color.GREEN);
                    textValuePaint.setTextSize(110);
                    textValuePaint.setStyle(Paint.Style.FILL);

                    for (Face face : faces) {
                        // 顔の矩形領域を取得
                        RectF bounds = new RectF(face.getBoundingBox());

                        // 顔を囲む緑枠を描画
                        canvas.drawRect(bounds, paint);

                        // 表情の判定
                        String emotionMessage = "";
                        String emotionValue = "";
                        if (face.getSmilingProbability() != null) {

                            // 笑顔の確率を取得
                            emotionMessage = getEmotion(face);
                            float smileProb = face.getSmilingProbability();
                            emotionValue =  String.format("%.0f%%", smileProb * 100);
//                            float smileProb = face.getSmilingProbability();
//                            if (smileProb > 0.5) {
//                                emotionMessage = "Smile!";
//                            } else {
//                                emotionMessage = "Neutral";
//                            }
                        }

                        // メッセージを顔の下に表示
                        if (!emotionMessage.isEmpty()) {
                            canvas.drawText(emotionMessage, bounds.left, bounds.bottom + 110, textPaint);
                        }
                        if (!emotionValue.isEmpty() && emotionMessage.toLowerCase().contains("smile")) {
                            canvas.drawText(emotionValue, bounds.left, bounds.top - 110, textValuePaint);
                        }
                    }

                    // ImageViewに変更された画像を表示
                    ImageView imageView = findViewById(R.id.imageView);
                    imageView.setImageBitmap(mutableBitmap); // 枠付き画像をセット

                    int faceCount = faces.size(); // 検出した顔の数
                    Toast.makeText(this, "検出した人数: " + faceCount + "人", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "顔を検出できませんでした", Toast.LENGTH_LONG).show();
                });
    }


    // 表情を判定するメソッド
    private String getEmotion(Face face) {
        String emotionMessage = "Neutral";  // 初期値は「中立」

        // 笑顔の判定
        if (face.getSmilingProbability() != null) {
            float smileProb = face.getSmilingProbability();
            if (smileProb >= 0.9) {
                emotionMessage = "Perfect Smile";
            } else if (smileProb > 0.75) {
                emotionMessage = "Smile";
            } else if (smileProb > 0.5) {
                emotionMessage = "Soft Smile";
            } else if (smileProb < 0.2) {
                //emotionMessage = "Sad"; // ほぼ笑っていない場合は悲しみ
            }
        }

        // 目の開き具合で感情を判定
        if (face.getLeftEyeOpenProbability() != null && face.getRightEyeOpenProbability() != null) {
            float leftEyeProb = face.getLeftEyeOpenProbability();
            float rightEyeProb = face.getRightEyeOpenProbability();
            float avgEyeOpen = (leftEyeProb + rightEyeProb) / 2;

            if (avgEyeOpen < 0.3) {
                emotionMessage = "Angry"; // 目を細めている → 怒り
            } else if (avgEyeOpen > 0.9) {
                //emotionMessage = "Surprised"; // 目を大きく見開いている → 驚き
            }
        }

        // 頭の傾きで追加判定
        if (face.getHeadEulerAngleX() > 20) {
            emotionMessage = "Sad"; // 下を向いている → 悲しみ
        } else if (face.getHeadEulerAngleX() < -20) {
            //emotionMessage = "Confused"; // 上を向いている → 困惑
        } else if (face.getHeadEulerAngleY() > 20 || face.getHeadEulerAngleY() < -20) {
            emotionMessage = "Curious"; // 横に傾けている → 疑問・興味
        }

        return emotionMessage;
    }

    //進捗ダイアログ
    public static void showProgressDialog(Context context) {
//        public static void showProgressDialog(Context context, String title, String message) {
        // カスタムレイアウトを作成
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog, null);

        String title = "【AI】";
        String message = "\n\n\n画像解析中．．．\n\n\n";

        // ダイアログを作成
        AlertDialog progressDialog = new AlertDialog.Builder(context)
                .setTitle(title)         // タイトルを追加
                .setMessage(message)     // メッセージを追加
                .setView(view)           // ProgressBar をセット
                .setCancelable(false)    // 手動で閉じられないようにする
                .create();

        // ダイアログを表示
        progressDialog.show();

        // 0.5秒後にダイアログを閉じる
        new Handler().postDelayed(progressDialog::dismiss, 1500);
    }


    /*
    private String getEmotion(Face face) {
        String emotionMessage = "Neutral";  // 初期値は「中立」

        if (face.getSmilingProbability() != null) {
            float smileProb = face.getSmilingProbability();
            if (smileProb >= 1.0){
                emotionMessage = "Perfect!!";
            }
            else if (smileProb > 0.75) {
                emotionMessage = "Smile";
            } else if (smileProb > 0.5) {
                emotionMessage = "Soft Smile";
            }
        }

        // 他の表情の分類
        if (face.getHeadEulerAngleX() > 15 || face.getHeadEulerAngleX() < -15) {
            emotionMessage = "Serious"; // 頭を強く傾けている場合、真剣な表情
        } else if (face.getHeadEulerAngleY() > 15 || face.getHeadEulerAngleY() < -15) {
            emotionMessage = "Surprised"; // 頭を横に傾けることで驚きの表情
        }

        return emotionMessage;
    }
     */
}
