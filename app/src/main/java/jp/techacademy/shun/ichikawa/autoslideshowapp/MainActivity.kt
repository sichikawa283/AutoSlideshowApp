package jp.techacademy.shun.ichikawa.autoslideshowapp

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private val PERMISSIONS_REQUEST_CODE = 100
    private var cursor: Cursor? = null
    private var timer: Timer? = null
    private var handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // R.layout.◯◯という引数でLayoutフォルダ以下に配置されているレイアウトファイルを指定
        setContentView(R.layout.activity_main)

        // Permissionを確認して端末内の画像にアクセスする
        // Android 6.0以降の場合
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // パーミッションの許可状態を確認する
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // 許可されている
                cursor = getCursor()
            } else {
                // 許可されていないので許可ダイアログを表示する
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSIONS_REQUEST_CODE)
            }
            // Android 5系以下の場合
        } else {
            cursor = getCursor()
        }

        // アプリを開いた時、先頭のcursor画像を表示する
        showImage()

        // 進むボタンタップの動作
        next_button.setOnClickListener {
            // cursorがnullではない場合、cursorを次に動かし、最後のcursorまで動いたら最初に戻る
            if (cursor != null) {
                if (!cursor!!.moveToNext()) cursor!!.moveToFirst()
                showImage()
            }
        }

        // 戻るボタンタップの動作
        back_button.setOnClickListener {
            if (cursor != null) {
                if (!cursor!!.moveToPrevious()) cursor!!.moveToLast()
                showImage()
            }
        }

        // 再生/停止ボタンの動作
        start_stop_button.setOnClickListener {
            if (timer == null) {
                timer = Timer()
                timer!!.schedule(object : TimerTask() {
                    override fun run() {
                        // 描画の依頼
                        handler.post {
                            if (cursor != null) {
                                start_stop_button.text = String.format("停止")
                                if (!cursor!!.moveToNext()) cursor!!.moveToFirst()
                                showImage()
                                next_button.isClickable = false
                                back_button.isClickable = false
                            }
                        }
                    }
                }, 2000, 2000) // 最初に始動させるまで2000ミリ秒、ループの間隔を2000ミリ秒 に設定
            } else  {
                start_stop_button.text = String.format("再生")
                timer!!.cancel()
                timer = null
                next_button.isClickable = true
                back_button.isClickable = true
            }
        }
        // TODO: ユーザがパーミッションの利用を「拒否」した場合にも、アプリの強制終了やエラーが発生しないようにしてください。
    }

    // ユーザーのPermission選択結果を受け取る
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE ->
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getCursor()
                }
        }
    }

    // cursorを使えるようにする
    private fun getCursor(): Cursor? {
        // ContentProviderのデータを参照するcontentResolverクラスのインスタンス生成
        // ContentProvider：他アプリとのデータをやり取りする仕組み
        val resolver = contentResolver

        // 画像のデータをcursorに入れる
        val cursor = resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // データの種類
            null, // 項目（null = 全項目）
            null, // フィルタ条件（null = フィルタなし）
            null, // フィルタ用パラメータ
            null // ソート (nullソートなし）
        )

        // 画像のデータが入ったcursorをreturnする
        return if (cursor!!.moveToFirst()) { // DB上の検索結果を格納するcursorが先頭にある場合
            cursor // 先頭のcursor
        } else {
            cursor.close()
            null
        }
    }

    // cursorに入れた画像を表示する
    private fun showImage() {
        if (cursor != null) {
            val fieldIndex = cursor!!.getColumnIndex(MediaStore.Images.Media._ID)
            val id = cursor!!.getLong(fieldIndex)
            val imageUri =
                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            imageView.setImageURI(imageUri)
        }
    }
}
