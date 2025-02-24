package tfsapps.smilechecker;

import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyOpenHelper extends SQLiteOpenHelper
{
    private static final String TABLE = "appinfo";
    public MyOpenHelper(Context context) {
        super(context, "AppDB", null, 1);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE + "("
                + "is_open integer,"             // DBオープン
                + "face_frame integer,"         // 顔認識枠表記
                + "face_mark integer,"          // スマイル表記
                + "face_high_mark integer,"      // スマイル段階表記
                + "face_value integer,"         // スマイル数値表記
                + "face_high_speed integer,"     // AI解析高速化
                + "face_select_num integer,"     // 複数枚選択増加
                + "premium_plan integer,"      // プレミアム加入
                + "system1 integer,"            // 予備 1〜5
                + "system2 integer,"
                + "system3 integer,"
                + "system4 integer,"
                + "system5 integer);");
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}