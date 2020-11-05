package com.liucx.tool.qrcodetool;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import android.support.v4.app.ActivityCompat;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        chooseBt = findViewById(R.id.button);
        getQrcodeBt = findViewById(R.id.button2);
        outString = findViewById(R.id.result);
        iv1 = findViewById(R.id.imageView);
        chooseBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseBt.setClickable(false);
                choosePicture(v);
                chooseBt.setClickable(true);
            }
        });
        getQrcodeBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getQrcodeBt.setClickable(false);
                iv1.setImageBitmap(ZxingQrcode.syncEncodeQRCode(outString.getText().toString(),
                        240/*width*/,
                        Color.BLACK,
                        Color.WHITE,
                        true/*scale*/,
                        null,
                        ErrorCorrectionLevel.H/*errorCorrectionLevel*/));
                getQrcodeBt.setClickable(true);
            }
        });
        /*outString.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView textView  =(TextView) v;
                ClipboardManager cmb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText(null, textView.getText());
                cmb.setPrimaryClip(clipData);
            }
        });*/

    }
    private Button chooseBt;
    private Button getQrcodeBt;
    private EditText outString;
    private ImageView iv1;

    public void choosePicture(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // 申请权限，参数1是当前activity 参数2是我要申请的相关权限（一个String数组）
                //参数3是我定义的requestCode，在onRequestPermissionResult（）要用来识别是否我的返回，
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                        , Manifest.permission.READ_EXTERNAL_STORAGE}, 123);
            } else {
                gotoPic();
            }
        } else {
            /**
             * 打开选择图片的界面
             */
            /*Intent intent = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");//相片类型
            startActivityForResult(intent, 125);*/
            gotoPic();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // requestCode识别，找到我自己定义的requestCode
        if (requestCode==123) {
            boolean grantedAll=true;
            for (int rangtResult:grantResults) {
                //判断用户是否给予权限
                if (rangtResult!=PackageManager.PERMISSION_GRANTED){
                    grantedAll=false;
                    break;
                }
            }
            if (grantedAll){
                /**
                 * 打开选择图片的界面
                 */
                /*Intent intent = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");//相片类型
                startActivityForResult(intent, 125);*/
                gotoPic();
            }
        }
    }

    private void gotoPic() {
        /*Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("iamge/*");
        startActivityForResult(intent, 300);*/
        Intent intent;
        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
        } else {
            intent = new Intent(
                    Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }
        startActivityForResult(intent, 300);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            //获取照片数据
            Bitmap camera = data.getParcelableExtra("data");
            iv1.setImageBitmap(camera);
        }
        if (requestCode == 200) {
            if (data != null) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(data.getData()));
                    iv1.setImageBitmap(bitmap);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        if (requestCode == 300) {
            String photoPath = getImagePathFromURI(this, data.getData());
            if (photoPath == null) {
                Log.d("liucx","路径获取失败");
            } else {
                //解析图片
                prasePhoto(photoPath);
            }
        }
    }
    private String getRealFilePath(Context c, Uri uri) {
        String result;
        Cursor cursor = c.getContentResolver().query(uri,
                new String[]{MediaStore.Images.ImageColumns.DATA},//
                null, null, null);
        if (cursor == null) result = uri.getPath();
        else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(index);
            cursor.close();
        }
        return result;
    }

    private void prasePhoto(final String path) {
        Log.d("liucx","路径:" + path);
        Log.d("liucx","path exists:" + new File(path).exists());
        AsyncTask myTask = new AsyncTask<String, Integer, String>() {
            @Override
            protected String doInBackground(String... params) {
                // 解析二维码/条码
                return QRCodeDecoder.syncDecodeQRCode(path);
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                if (null == s) {
                    Log.d("liucx","图片获取失败,请重试");
                } else {
                    // 识别出图片二维码/条码，内容为s
                    outString.setText(s);
                    Log.d("liucx",s);
                }
            }
        }.execute(path);
    }

    public static String getImagePathFromURI(Activity activity,Uri uri) {
        Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null);
        String path = null;
        if (cursor != null) {
            cursor.moveToFirst();
            String document_id = cursor.getString(0);
            document_id = document_id.substring(document_id.lastIndexOf(":") + 1);
            cursor.close();

            cursor = activity.getContentResolver().query(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
            if (cursor != null) {
                cursor.moveToFirst();
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                cursor.close();
            }
        }
        return path;
    }

    public static String getFPUriToPath(Context context, Uri uri) {
        try {
            List<PackageInfo> packs = context.getPackageManager().getInstalledPackages(PackageManager.GET_PROVIDERS);
            if (packs != null) {
                String fileProviderClassName = FileProvider.class.getName();
                for (PackageInfo pack : packs) {
                    ProviderInfo[] providers = pack.providers;
                    if (providers != null) {
                        for (ProviderInfo provider : providers) {
                            if (uri.getAuthority().equals(provider.authority)) {
                                if (provider.name.equalsIgnoreCase(fileProviderClassName)) {
                                    Class fileProviderClass = FileProvider.class;
                                    try {
                                        Method getPathStrategy = fileProviderClass.getDeclaredMethod("getPathStrategy", Context.class, String.class);
                                        getPathStrategy.setAccessible(true);
                                        Object invoke = getPathStrategy.invoke(null, context, uri.getAuthority());
                                        if (invoke != null) {
                                            String PathStrategyStringClass = FileProvider.class.getName() + "$PathStrategy";
                                            Class PathStrategy = Class.forName(PathStrategyStringClass);
                                            Method getFileForUri = PathStrategy.getDeclaredMethod("getFileForUri", Uri.class);
                                            getFileForUri.setAccessible(true);
                                            Object invoke1 = getFileForUri.invoke(invoke, uri);
                                            if (invoke1 instanceof File) {
                                                String filePath = ((File) invoke1).getAbsolutePath();
                                                return filePath;
                                            }
                                        }
                                    } catch (NoSuchMethodException e) {
                                        e.printStackTrace();
                                    } catch (InvocationTargetException e) {
                                        e.printStackTrace();
                                    } catch (IllegalAccessException e) {
                                        e.printStackTrace();
                                    } catch (ClassNotFoundException e) {
                                        e.printStackTrace();
                                    }
                                    break;
                                }
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
