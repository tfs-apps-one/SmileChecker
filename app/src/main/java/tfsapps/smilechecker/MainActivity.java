package tfsapps.smilechecker;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
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
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView imageView;
    private TextView txtResult;
    private Uri imageUri;
    private Bitmap _bitmap = null;

    private int human_num = 0;
    private int smile_num = 0;
    private int angry_num = 0;
    private int sad_num = 0;
    private int curious_num = 0;

    private Button btnChooseImage;
    private Button btnNextImage;
    private Switch SwFaceFrame;
    private Switch SwFaceMark;
    private Switch SwFaceHighMark;
    private Switch SwFaceValue;
    private Switch SwFaceHighSpeed;
    private Switch SwSelectionNum;

    private boolean isFaceFrame = true;
    private boolean isFaceMark = true;
    private boolean isFaceHighMark = false;
    private boolean isFaceValue = false;
    private boolean isAiHighSpeed = false;
    private boolean isSelectNum = false;

    private Bitmap mutableBitmap = null;
    private TextView face_result;
    private ImageView star_1;
    private ImageView star_2;
    private ImageView star_3;

    private static final int PICK_IMAGES_REQUEST = 1;
    private ArrayList<Uri> imageUris = new ArrayList<>();
    private int currentIndex = 0;
    private int MaxSelectNum = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_main);

//        imageView = findViewById(R.id.imageView);
//        txtResult = findViewById(R.id.txtResult);
//        btnChooseImage = findViewById(R.id.btnChooseImage);
//        btnNextImage = findViewById(R.id.btnNextImage);
//        face_result = findViewById(R.id.faceResult);
//        star_1 = findViewById(R.id.result_1);
//        star_2 = findViewById(R.id.result_2);
//        star_3 = findViewById(R.id.result_3);

        //btnChooseImage.setOnClickListener(v -> onOpenGallery());

        MainScreenDisplay();

        /***
        // 画面の高さを取得
        WindowManager windowManager = getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenHeight = size.y; // 画面の高さ（ピクセル）

        // ImageView の高さを設定（1/2サイズ）
        ViewGroup.LayoutParams params = imageView.getLayoutParams();
        params.height = screenHeight / 2;
        imageView.setLayoutParams(params);
        ***/
    }

    public void MainScreenDisplay(){

        imageView = findViewById(R.id.imageView);
        txtResult = findViewById(R.id.txtResult);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnNextImage = findViewById(R.id.btnNextImage);
        if(currentIndex > 0) {
            if (currentIndex >= imageUris.size()) {
                btnNextImage.setEnabled(false); // 最後の画像ならボタンを無効化
            }
        }
        face_result = findViewById(R.id.faceResult);
        star_1 = findViewById(R.id.result_1);
        star_2 = findViewById(R.id.result_2);
        star_3 = findViewById(R.id.result_3);

        ImageView imageView = findViewById(R.id.imageView);

        if (mutableBitmap == null){
            imageView.setImageResource(R.drawable.sample);
            face_result.setText("画像を選択して下さい\n結果はこちらに出力されます↓");
        }
        else{
            imageView.setImageBitmap(mutableBitmap); // 枠付き画像をセット
            ResultSmile();
        }
    }

    private void processNextImage() {
        if (currentIndex < imageUris.size()) {
            Uri imageUri = imageUris.get(currentIndex);
            imageView.setImageURI(imageUri); // 画像を表示

            int _time = 1700;
            if (isAiHighSpeed) _time = 1100;
            showProgressDialog(this, _time);
            ResetSmileCheckerNum();
            analyzeImage(imageUri);
            detectFaces();

            currentIndex++;
        }

        if (currentIndex >= imageUris.size()) {
            btnNextImage.setEnabled(false); // 最後の画像ならボタンを無効化
            Toast.makeText(this, "すべての画像を処理しました", Toast.LENGTH_SHORT).show();
        }
    }

    //ギャラリー
    /* １枚の処理
    public void onOpenGallery(View v) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }
     */
    public void onOpenGallery(View v) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // 複数選択を許可
        startActivityForResult(Intent.createChooser(intent, "画像を選択"), PICK_IMAGES_REQUEST);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /* イメージ１個の処理
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            imageView.setImageURI(imageUri);
            int _time = 1700;
            if (isAiHighSpeed) _time = 1100;
            showProgressDialog(this, _time);
            ResetSmileCheckerNum();
            analyzeImage();
            detectFaces();
        }*/
        if (isSelectNum){
            MaxSelectNum = 9;
        }
        else{
            MaxSelectNum = 3;
        }
        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                imageUris.clear();
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count && i < MaxSelectNum; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        imageUris.add(imageUri);
                    }
                } else if (data.getData() != null) {
                    imageUris.add(data.getData());
                }

                currentIndex = 0; // 最初の画像からスタート
                btnNextImage.setEnabled(true);
                processNextImage(); // 1枚目をすぐ表示
            }
        }
    }

    /* イメージ１枚の処理
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

     */
    private void analyzeImage(Uri imgUri) {
        if (imgUri == null) {
            return;
        }
        else {
            imageUri = imgUri;
        }
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

            //test_make  default = 0.7
            if (confidence > 0.5) { // 信頼度が70%以上のものだけ表示
               resultText.append(labelText).append(" (").append(String.format("%.1f", confidence * 100)).append("%)\n");
            }
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
                    mutableBitmap = _bitmap.copy(Bitmap.Config.ARGB_8888, true);
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
                        if (isFaceFrame){
                            canvas.drawRect(bounds, paint);
                        }

                        // 表情の判定
                        String emotionMessage = "";
                        String emotionValue = "";
                        if (face.getSmilingProbability() != null) {

                            // 笑顔の確率を取得
                            if (isFaceHighMark) {
                                emotionMessage = getEmotion(face);
                            }
                            else{
                                emotionMessage = getEmotionBasice(face);
                            }
                            float smileProb = face.getSmilingProbability();
                            emotionValue =  String.format("%.0f%%", smileProb * 100);
                        }

                        // メッセージを顔の下に表示
                        if (isFaceMark && !emotionMessage.isEmpty()) {
                            canvas.drawText(emotionMessage, bounds.left, bounds.bottom + 110, textPaint);
                        }
                        if (isFaceValue && !emotionValue.isEmpty() && emotionMessage.toLowerCase().contains("smile")) {
                            canvas.drawText(emotionValue, bounds.left, bounds.top - 110, textValuePaint);
                        }
                    }

                    // ImageViewに変更された画像を表示
                    ImageView imageView = findViewById(R.id.imageView);
                    imageView.setImageBitmap(mutableBitmap); // 枠付き画像をセット

                    int faceCount = faces.size(); // 検出した顔の数
                    human_num = faceCount;
                    ResultSmile();
//                    Toast.makeText(this, "検出した人数: " + faceCount + "人", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
//                    Toast.makeText(this, "顔を検出できませんでした", Toast.LENGTH_LONG).show();
                });
    }

    //パラメータ初期化
    public void ResetSmileCheckerNum(){
        human_num = 0;
        smile_num = 0;
        angry_num = 0;
        sad_num = 0;
        curious_num = 0;
    }

    //笑顔表記
    public void ResultSmile(){

        String message = "";

        float _percent = 0;
        float _smile = 0;
        float _human = 0;

        if (human_num > 0 && smile_num > 0){

            _smile = (float)smile_num;
            _human = (float)human_num;

            _percent = (_smile / _human);

            if (human_num == smile_num || _percent >= 1.0){
                message = "満点の【スマイル】です";

                star_1.setImageResource(R.drawable.star_ok);
                star_2.setImageResource(R.drawable.star_ok);
                star_3.setImageResource(R.drawable.star_ok);
            }
            else if(_percent >= 0.5){
                message = "多くの【スマイル】があります";

                star_1.setImageResource(R.drawable.star_ok);
                star_2.setImageResource(R.drawable.star_ok);
                star_3.setImageResource(R.drawable.star_ng2);
            }
            else{
                message = "少しの【スマイル】があります";

                star_1.setImageResource(R.drawable.star_ok);
                star_2.setImageResource(R.drawable.star_ng2);
                star_3.setImageResource(R.drawable.star_ng2);
            }
        }
        else{
            if (human_num == 0) {
                message = "【顔を検出できません】";
            }
            else{
                message = "【スマイル】がないかな？";
            }
            star_1.setImageResource(R.drawable.star_ng2);
            star_2.setImageResource(R.drawable.star_ng2);
            star_3.setImageResource(R.drawable.star_ng2);
        }
        //共通部分
        message +=
                "\n"+
                "\n　顔の検出　："+human_num+
                "\n　スマイル　："+smile_num+
                "\n　悲しい　　："+sad_num+
                "\n　怒り・立腹："+angry_num+
                "\n　興味・困惑："+curious_num+
                "\n";
                face_result.setText(message);
    }


    // 表情を判定するメソッドベーシック
    private String getEmotionBasice(Face face) {
        String emotionMessage = "Neutral";  // 初期値は「中立」

        // 笑顔の判定
        if (face.getSmilingProbability() != null) {
            float smileProb = face.getSmilingProbability();
            if (smileProb > 0.5) {
                emotionMessage = "Smile";
                smile_num++;
                return emotionMessage;
            }
        }
        return  emotionMessage;
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
//                emotionMessage = "Sad"; // ほぼ笑っていない場合は悲しみ
//                sad_num++;
            }

            if (smileProb > 0.5){
                smile_num++;
                return emotionMessage;
            }
        }

        // ↓↓　以下は笑顔ではない判定の時

        // 目の開き具合で感情を判定
        if (face.getLeftEyeOpenProbability() != null && face.getRightEyeOpenProbability() != null) {
            float leftEyeProb = face.getLeftEyeOpenProbability();
            float rightEyeProb = face.getRightEyeOpenProbability();
            float avgEyeOpen = (leftEyeProb + rightEyeProb) / 2;

            if (avgEyeOpen < 0.3) {
                emotionMessage = "Angry"; // 目を細めている → 怒り
                angry_num++;
            } else if (avgEyeOpen > 0.9) {
                //emotionMessage = "Surprised"; // 目を大きく見開いている → 驚き
            }
        }

        // 頭の傾きで追加判定
        if (face.getHeadEulerAngleX() > 20) {
            emotionMessage = "Sad"; // 下を向いている → 悲しみ
            sad_num++;
        } else if (face.getHeadEulerAngleX() < -20) {
            //emotionMessage = "Confused"; // 上を向いている → 困惑
        } else if (face.getHeadEulerAngleY() > 20 || face.getHeadEulerAngleY() < -20) {
            emotionMessage = "Curious"; // 横に傾けている → 疑問・興味
            curious_num++;
        }

        return emotionMessage;
    }

    //進捗ダイアログ
    public static void showProgressDialog(Context context, int value) {
        // カスタムレイアウトを作成
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog, null);

        String title = "【 AI解析中... 】";
        String message = "\n\n\n\n\n\n\nしばらくお待ちください\n\n\n\n\n\n";

        // タイトルとメッセージをセット
        TextView titleView = view.findViewById(R.id.dialogTitle);
        TextView messageView = view.findViewById(R.id.dialogMessage);
        titleView.setText(title);
        messageView.setText(message);

        // ダイアログを作成
        AlertDialog progressDialog = new AlertDialog.Builder(context)
                .setView(view)           // ProgressBar をセット
                .setCancelable(false)    // 手動で閉じられないようにする
                .create();

        // ダイアログを表示
        progressDialog.show();

        // 1.5秒後にダイアログを閉じる
        new Handler().postDelayed(progressDialog::dismiss, value);
    }

    /**
     * 「ボタン処理」
     **/
    //スクショ確認ダイアログ
    public void showScreenShotsDialog() {

        String title = "【スクリーンショット】";
        String message = "\n\n\n\n\n\n\n現在の判定結果を保存しますか？\n\n\n\n\n\n";

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("はい", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ScreenShotsDone();
            }
        });
        builder.setNegativeButton("いいえ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }
    //スクショ確認ダイアログ
    public void showInformationDialog() {

        String title = "【アプリの使い方】";
        String message =
                        "\n"+
                        "\n【注意】"+
                        "\nAIの判定結果は参考程度にご利用下さい。"+
                        "\n"+
                        "\n【使い方】"+
                        "\nまずは画像を選択して下さい。選択後、AIが顔認識を行いスマイル度を判定します。"+
                        "\n例えばあるシーンを複数枚撮った時、どの写真一番スマイルしているかなどを判定するのに便利です"+
                        "\n"+
                        "\n判定した結果はスクリーンショットとして保存することが出来ます。"+
                        "\nアプリをもっと便利に使える設定もあります。設定画面を確認下さい。"+
                        "\n"+
                        "\n";

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("閉じる", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }
    public void onInformation(View v){
        showInformationDialog();
    }
    public void ScreenShotsDone(){
        MyScreenShots.takeScreenshotAndSave(this);
    }
    /* --- メイン画面 --- */
    public void onScreenShots(View v){
        //MyScreenShots.takeScreenshotAndSave(this);
        showScreenShotsDialog();
    }
    public void onNextImage(View v){
        processNextImage();
    }


    /* --- 設定画面 --- */
    public void onSetup(View v){
        setContentView(R.layout.activity_sub);

        SwFaceFrame = findViewById(R.id.face_frame);
        SwFaceMark = findViewById(R.id.face_mark);
        SwFaceHighMark = findViewById(R.id.face_high_mark);
        SwFaceValue = findViewById(R.id.face_value);
        SwFaceHighSpeed = findViewById(R.id.ai_high_speed);
        SwSelectionNum = findViewById(R.id.selection_num);

        SwFaceFrame.setChecked(isFaceFrame);
        SwFaceFrame.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                isFaceFrame = true;
            } else {
                isFaceFrame = false;
            }
        });

        SwFaceMark.setChecked(isFaceMark);
        SwFaceMark.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                isFaceMark = true;
            } else {
                isFaceMark = false;
            }
        });

        SwFaceHighMark.setChecked(isFaceHighMark);
        SwFaceHighMark.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                isFaceHighMark = true;
            } else {
                isFaceHighMark = false;
            }
        });

        SwFaceValue.setChecked(isFaceValue);
        SwFaceValue.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                isFaceValue = true;
            } else {
                isFaceValue = false;
            }
        });

        SwFaceHighSpeed.setChecked(isAiHighSpeed);
        SwFaceHighSpeed.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                isAiHighSpeed = true;
            } else {
                isAiHighSpeed = false;
            }
        });

        SwSelectionNum.setChecked(isSelectNum);
        SwSelectionNum.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                isSelectNum = true;
            } else {
                isSelectNum = false;
            }
        });

    }

    public void onBack(View v){
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_main);

        MainScreenDisplay();
    }
}
