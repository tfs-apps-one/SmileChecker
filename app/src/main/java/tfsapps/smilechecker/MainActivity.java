package tfsapps.smilechecker;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

    private MyOpenHelper helper;    //DBアクセス
    private boolean is_open = false;    //DB使用したか
    private boolean isFaceFrame = true;
    private boolean isFaceMark = true;
    private boolean isFaceHighMark = false;
    private boolean isFaceValue = false;
    private boolean isAiHighSpeed = false;
    private boolean isSelectNum = false;
    private boolean isPremium = false;
    private int db_system1 = 0;
    private int db_system2 = 0;
    private int db_system3 = 0;
    private int db_system4 = 0;
    private int db_system5 = 0;

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

    /**
     * OS関連処理
     */
    @Override
    public void onStart() {
        super.onStart();
        //DBのロード
        /* データベース */
        helper = new MyOpenHelper(this);
        AppDBInitRoad();

        /*
        //評価ポップアップ処理
        if (db_system1 <= REVIEW_POP){
            if (ReviewCount != 0){
                db_system1++;
                ReviewCount = 0;
            }
        }
        ShowRatingPopup();
         */
    }

    @Override
    public void onResume() {
        super.onResume();
        /*
        //サブスク
        //BillingClientを初期化
        billingClient = BillingClient.newBuilder(this)
                .setListener(this)
                .enablePendingPurchases()
                .build();

        // Google Playへの接続
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "@@@@@@ Billing Client connected ");
                    checkSubscriptionStatus();
                } else {
                    Log.e(TAG, "@@@@@@ Billing connection failed: " + billingResult.getDebugMessage());
//                    Log.e(TAG, "Billing Client connection failed");
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.e(TAG, "@@@@@@ Billing Service disconnected");
            }
        });
        */
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        //  DB更新
        AppDBUpdated();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        if (billingClient != null){
//            billingClient.endConnection();
//        }
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
        else{
            if (imageUris.isEmpty()){
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

                    // 画像の幅と高さを取得
                    int imageWidth = mutableBitmap.getWidth();
                    int imageHeight = mutableBitmap.getHeight();
                    // 画像の短辺を基準にスケーリング
                    int minSize = Math.min(imageWidth, imageHeight);
                    // 枠線の太さを画像サイズに応じて調整 (短辺の 1% にする)
                    float strokeWidth = (minSize/3) * 0.01f; // 1% の太さ
                    // テキストサイズを画像サイズに応じて調整 (短辺の 5% にする)
                    float textSize = (minSize/3)*2 * 0.05f; // 5% の大きさ

                    Paint paint = new Paint();
                    paint.setColor(Color.GREEN);  // 緑色の枠
                    paint.setStyle(Paint.Style.STROKE);  // 枠線のみ描画
                    paint.setStrokeWidth(strokeWidth);
//                    paint.setStrokeWidth(8);  // 枠線の太さを8pxに設定


                    // テキスト用のペイントオブジェクト
                    Paint textPaint = new Paint();
                    textPaint.setColor(Color.GREEN);
                    textPaint.setTextSize(textSize);
//                    textPaint.setTextSize(110);
                    textPaint.setStyle(Paint.Style.FILL);

                    // テキスト用のペイントオブジェクト
                    Paint textValuePaint = new Paint();
                    textValuePaint.setColor(Color.GREEN);
                    textValuePaint.setTextSize(textSize);
//                    textValuePaint.setTextSize(110);
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
                            canvas.drawText(emotionMessage, bounds.left, bounds.bottom + textSize, textPaint);
//                            canvas.drawText(emotionMessage, bounds.left, bounds.bottom + 110, textPaint);
                        }
                        if (isFaceValue && !emotionValue.isEmpty() && emotionMessage.toLowerCase().contains("smile")) {
                            canvas.drawText(emotionValue, bounds.left, bounds.top - textSize, textValuePaint);
//                            canvas.drawText(emotionValue, bounds.left, bounds.top - 110, textValuePaint);
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
                        "\n本アプリは「顔認識AI」を活用してスマイルを判定するアプリです。"+
                        "\n例えば家族であるシーンを複数枚撮った時、この写真の内どの一枚が家族全員スマイルしているかを調べるのに便利です。"+
                        "\n"+
                        "\n【注意】"+
                        "\nAIの判定結果は参考程度にご利用下さい。"+
                        "\n"+
                        "\n【使い方】"+
                        "\nまずは画像を選択して下さい。選択後、AIが顔認識を行いスマイル度を判定します。"+
                        "\n画像の複数枚選択機能は3枚(初期設定)までとなります。それ以上選択した場合は最初の3枚が優先されます。"+
                        "\n"+
                        "\n判定した結果はスクリーンショットとして保存することが出来ます。"+
                        "\nアプリをもっと便利に使える設定もあります。設定画面を確認下さい。【プレミアム】の利用でより高度な機能を使うことが出来ます。"+
                        "\n"+
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


    /***************************************************
         DB初期ロードおよび設定
     ****************************************************/
    public void AppDBInitRoad() {
        SQLiteDatabase db = helper.getReadableDatabase();
        StringBuilder sql = new StringBuilder();
        int temp_int = 0;

        sql.append(" SELECT");
        sql.append(" is_open");
        sql.append(" ,face_frame,face_mark");
        sql.append(" ,face_high_mark,face_value");
        sql.append(" ,face_high_speed,face_select_num");
        sql.append(" ,premium_plan");
        sql.append(" ,system1,system2");
        sql.append(" ,system3,system4");
        sql.append(" ,system5");
        sql.append(" FROM appinfo;");
        try {
            Cursor cursor = db.rawQuery(sql.toString(), null);
            //TextViewに表示
            StringBuilder text = new StringBuilder();

            if (cursor.moveToNext()) {
                temp_int = cursor.getInt(0);
                if (temp_int > 0)   is_open = true;
                else                is_open = false;

                temp_int = cursor.getInt(1);
                if (temp_int > 0)   isFaceFrame = true;
                else                isFaceFrame = false;

                temp_int = cursor.getInt(2);
                if (temp_int > 0)   isFaceMark = true;
                else                isFaceMark = false;

                temp_int = cursor.getInt(3);
                if (temp_int > 0)   isFaceHighMark = true;
                else                isFaceHighMark = false;

                temp_int = cursor.getInt(4);
                if (temp_int > 0)   isFaceValue = true;
                else                isFaceValue = false;

                temp_int = cursor.getInt(5);
                if (temp_int > 0)   isAiHighSpeed = true;
                else                isAiHighSpeed = false;

                temp_int = cursor.getInt(6);
                if (temp_int > 0)   isSelectNum = true;
                else                isSelectNum = false;

                temp_int = cursor.getInt(7);
                if (temp_int > 0)   isPremium = true;
                else                isPremium = false;

                db_system1 = cursor.getInt(8);
                db_system2 = cursor.getInt(9);
                db_system3 = cursor.getInt(10);
                db_system4 = cursor.getInt(11);
                db_system5 = cursor.getInt(12);
            }
        } finally {
            db.close();
        }

        db = helper.getWritableDatabase();
        if (is_open == false) {
            long ret;
            // 新規レコード追加
            ContentValues insertValues = new ContentValues();
            insertValues.put("is_open", 1);
            insertValues.put("face_frame", 1);
            insertValues.put("face_mark", 1);
            insertValues.put("face_high_mark", 0);
            insertValues.put("face_value", 0);
            insertValues.put("face_high_speed", 0);
            insertValues.put("face_select_num", 0);
            insertValues.put("premium_plan", 0);
            insertValues.put("system1", 0);
            insertValues.put("system2", 0);
            insertValues.put("system3", 0);
            insertValues.put("system4", 0);
            insertValues.put("system5", 0);
            try {
                ret = db.insert("appinfo", null, insertValues);
            } finally {
                db.close();
            }
            /*
            if (ret == -1) {
                Toast.makeText(this, "DataBase Create.... ERROR", Toast.LENGTH_SHORT).show();
            } else {
                is_open = true;
                Toast.makeText(this, "DataBase Create.... OK", Toast.LENGTH_SHORT).show();
            }
             */

        } else {
            /*
            if (is_open){
                temp_int = 1;
            }
            else{
                temp_int = 0;
            }
            Toast.makeText(this, "Data Loading...  isopen:" + temp_int, Toast.LENGTH_SHORT).show();
             */
        }
    }
    /***************************************************
     DB更新
     ****************************************************/
    public void AppDBUpdated() {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues insertValues = new ContentValues();

        int temp_int = 0;

        if (is_open)        temp_int = 1;
        else                temp_int = 0;
        insertValues.put("is_open", temp_int);

        if (isFaceFrame)    temp_int = 1;
        else                temp_int = 0;
        insertValues.put("face_frame", temp_int);

        if (isFaceMark)     temp_int = 1;
        else                temp_int = 0;
        insertValues.put("face_mark", temp_int);

        if (isFaceHighMark) temp_int = 1;
        else                temp_int = 0;
        temp_int = 0;   //test_make
        insertValues.put("face_high_mark", temp_int);

        if (isFaceValue)    temp_int = 1;
        else                temp_int = 0;
        temp_int = 0;   //test_make
        insertValues.put("face_value", temp_int);

        if (isAiHighSpeed)  temp_int = 1;
        else                temp_int = 0;
        temp_int = 0;   //test_make
        insertValues.put("face_high_speed", temp_int);

        if (isSelectNum)    temp_int = 1;
        else                temp_int = 0;
        temp_int = 0;   //test_make
        insertValues.put("face_select_num", temp_int);

        if (isPremium)      temp_int = 1;
        else                temp_int = 0;
        temp_int = 0;   //test_make
        insertValues.put("premium_plan", temp_int);

        /*
        *   プログラム固定で！！
        * */

        insertValues.put("system1", db_system1);
        insertValues.put("system2", db_system2);
        insertValues.put("system3", db_system3);
        insertValues.put("system4", db_system4);
        insertValues.put("system5", db_system5);
        int ret;
        try {
            ret = db.update("appinfo", insertValues, null, null);
        } finally {
            db.close();
        }
        /*
        if (ret != -1){
            Context context = getApplicationContext();
            Toast.makeText(context, "セーブ中...", Toast.LENGTH_SHORT).show();
//            Toast.makeText(context, "セーブ中...("+db_data1+")...", Toast.LENGTH_SHORT).show();
        }
        */

        /*
        if (ret == -1) {
            Toast.makeText(this, "Saving.... ERROR ", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Saving.... OK ", Toast.LENGTH_SHORT).show();
        }
         */
    }

}
