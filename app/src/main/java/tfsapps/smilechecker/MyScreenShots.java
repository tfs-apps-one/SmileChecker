package tfsapps.smilechecker;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Environment;
import android.view.PixelCopy;
import android.view.Window;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import android.os.Handler;
import android.os.Looper;

import android.media.MediaScannerConnection;
import android.content.Context;

public class MyScreenShots {
    public static void takeScreenshotAndSave(Activity activity) {
        Window window = activity.getWindow();
        Bitmap bitmap = Bitmap.createBitmap(window.getDecorView().getWidth(),
                window.getDecorView().getHeight(),
                Bitmap.Config.ARGB_8888);

        PixelCopy.request(window, bitmap, result -> {
            if (result == PixelCopy.SUCCESS) {
                saveBitmap(activity, bitmap);
            } else {
                Toast.makeText(activity, "スクリーンショットの取得に失敗しました", Toast.LENGTH_SHORT).show();
            }
        }, new Handler(Looper.getMainLooper()));
    }

    private static void saveBitmap(Activity activity, Bitmap bitmap) {
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "screenshots");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = "screenshot_" + System.currentTimeMillis() + ".png";
        File file = new File(directory, fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            Toast.makeText(activity, "Saved ... ", Toast.LENGTH_LONG).show();

            scanFile(activity, file.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(activity, "Save Error !!", Toast.LENGTH_SHORT).show();
        }
    }


    // メディアスキャンを実行してギャラリーに反映
    private static void scanFile(Context context, String path) {
        MediaScannerConnection.scanFile(context, new String[]{path}, null,
                (scannedPath, uri) -> {
                    // スキャン完了（ギャラリーに反映）
                });
    }
}
