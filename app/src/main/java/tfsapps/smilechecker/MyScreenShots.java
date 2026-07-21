package tfsapps.smilechecker;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.PixelCopy;
import android.view.Window;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MyScreenShots {

    public static final int REQUEST_WRITE_STORAGE_PERMISSION = 2001;

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 (API 29) 以降: スコープドストレージ対応のため MediaStore 経由で保存
            // （WRITE_EXTERNAL_STORAGE 権限は不要）
            saveBitmapWithMediaStore(activity, bitmap);
        } else {
            // Android 9 (API 28) 以下: 従来通りファイルへ直接保存（要ストレージ権限）
            saveBitmapLegacy(activity, bitmap);
        }
    }

    private static void saveBitmapWithMediaStore(Context context, Bitmap bitmap) {
        String fileName = "screenshot_" + System.currentTimeMillis() + ".png";
        ContentResolver resolver = context.getContentResolver();

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/screenshots");
        values.put(MediaStore.Images.Media.IS_PENDING, 1);

        Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        Uri itemUri = resolver.insert(collection, values);

        if (itemUri == null) {
            Toast.makeText(context, "Save Error !!", Toast.LENGTH_SHORT).show();
            return;
        }

        try (OutputStream out = resolver.openOutputStream(itemUri)) {
            if (out == null) {
                throw new IOException("Failed to open output stream for " + itemUri);
            }
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);

            values.clear();
            values.put(MediaStore.Images.Media.IS_PENDING, 0);
            resolver.update(itemUri, values, null, null);

            Toast.makeText(context, "Saved ... ", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            resolver.delete(itemUri, null, null);
            Toast.makeText(context, "Save Error !!", Toast.LENGTH_SHORT).show();
        }
    }

    private static void saveBitmapLegacy(Activity activity, Bitmap bitmap) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                    REQUEST_WRITE_STORAGE_PERMISSION);
            Toast.makeText(activity, "ストレージへのアクセスを許可してから、もう一度お試しください。", Toast.LENGTH_LONG).show();
            return;
        }

        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "screenshots");
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

    // メディアスキャンを実行してギャラリーに反映（レガシー経路のみ必要。MediaStore経路は自動で反映される）
    private static void scanFile(Context context, String path) {
        MediaScannerConnection.scanFile(context, new String[] { path }, null,
                (scannedPath, uri) -> {
                    // スキャン完了（ギャラリーに反映）
                });
    }
}
