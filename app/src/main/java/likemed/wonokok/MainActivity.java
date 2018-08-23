package likemed.wonokok;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static long backPressedAt;
    Handler handler;
    WebView mWebView;

    class ConnectThread extends Thread {
        String id;

        public ConnectThread(String tid) {
            id = tid;
        }

        public void run() {
            try {
                final String output = request();
                handler.post(new Runnable() {
                    public void run() {
                        String[] no_output = output.split("@");
                        int total_count = Integer.parseInt(no_output[0]);
                        int output_count = Integer.parseInt(no_output[1]);
                        int input_count = Integer.parseInt(no_output[2]);

                        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        if (total_count>0) {

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                /* Create or update. */
                                NotificationChannel channel = new NotificationChannel("1", "WonOkOk", NotificationManager.IMPORTANCE_MIN);
                                notificationManager.createNotificationChannel(channel);
                            }
                            Intent appcall = getPackageManager().getLaunchIntentForPackage("likemed.wonokok");
                            appcall.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);

                            if (output_count>0) {
                                appcall.putExtra("code", "sms");
                            } else {
                                appcall.putExtra("code", "bank");
                            }

                            PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, appcall, PendingIntent.FLAG_UPDATE_CURRENT);

                            NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this, "1");
                            builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), android.R.drawable.star_on));
                            builder.setSmallIcon(android.R.drawable.star_on);
                            builder.setTicker("WonOkOk (" + total_count + ")");
                            builder.setContentTitle("WonOkOk (" + total_count + ")");
                            builder.setContentText("총 " + total_count + "개 항목(지출:" + output_count + "/수입:" + input_count + ")이 입력 대기 중입니다.");
                            builder.setWhen(System.currentTimeMillis());
                            builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
                            builder.setContentIntent(pendingIntent);
                            builder.setAutoCancel(true);
                            builder.setPriority(Notification.PRIORITY_MIN);
                            builder.setNumber(total_count);
                            notificationManager.notify(0, builder.build());

                            Intent intent_badge = new Intent("android.intent.action.BADGE_COUNT_UPDATE");
                            intent_badge.putExtra("badge_count_package_name", getComponentName().getPackageName());
                            intent_badge.putExtra("badge_count_class_name", getComponentName().getClassName());
                            intent_badge.putExtra("badge_count", total_count);
                            sendBroadcast(intent_badge);
                        } else if (total_count==0) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                notificationManager.deleteNotificationChannel("1");
                            }
                            notificationManager.cancel(0);
                        }
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        private String request() {
            StringBuilder output = new StringBuilder();
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL("http://wonokok.dothome.co.kr/wonsms_notemp.php").openConnection();
                if (conn != null) {
                    conn.setConnectTimeout(10000);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    String urlParameters = new Uri.Builder().appendQueryParameter("id", id).appendQueryParameter("code", "iloveyou").build().getEncodedQuery();
                    DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                    wr.writeBytes(urlParameters);
                    wr.flush();
                    wr.close();
                    if (conn.getResponseCode() == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        while (true) {
                            String line = reader.readLine();
                            if (line == null) {
                                break;
                            }
                            output.append(line);
                        }
                        reader.close();
                    }
                    conn.disconnect();
                }
            } catch (Exception ex) {
                Log.e("WonOkOk", "Exception in processing response.", ex);
                ex.printStackTrace();
            }
            return output.toString();
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler();
        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.getSettings().setDefaultTextEncodingName("UTF-8");
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setSaveFormData(false);
        final Context myApp = this;
        mWebView.setWebChromeClient(new WebChromeClient() {
            ProgressBar pb = (ProgressBar)findViewById(R.id.progressBar);

            public void onProgressChanged(WebView view, int progress) {
                pb.getProgressDrawable().setColorFilter(Color.parseColor("#BEBEBE"), PorterDuff.Mode.SRC_IN);
                pb.setProgress(progress);
                if (progress == 100) {
                    pb.setVisibility(View.GONE);
                } else {
                    pb.setVisibility(View.VISIBLE);
                }
            }

            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                new AlertDialog.Builder(myApp)
                        .setTitle("WonOkOk")
                        .setMessage(message)
                        .setPositiveButton("확인", new AlertDialog.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm();
                            }
                        })
                        .setNegativeButton("취소", new AlertDialog.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                result.cancel();
                            }
                        })
                        .show();
                return true;
            }

            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                new AlertDialog.Builder(myApp)
                        .setTitle("WonOkOk 알림")
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok,
                                new AlertDialog.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        result.confirm();
                                    }
                                })
                        .setCancelable(false)
                        .create()
                        .show();
                return true;
            }

        });
        processIntent(getIntent());
        mWebView.setWebViewClient(new WebViewClient(){

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);

                if(errorCode== -2) {
                    mWebView.loadUrl("about:blank"); // 빈페이지 출력
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setPositiveButton("종료", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
                    builder.setNegativeButton("업데이트", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent it = new Intent(Intent.ACTION_VIEW);
                            it.setData(Uri.parse("market://details?id=likemed.wonokok"));
                            startActivity(it);
                            finish();
                        }
                    });
                    builder.setMessage("네트워크 상태가 원활하지 않거나 웹 서버가 변경되었습니다. 지속적으로 접속이 안되면 앱을 업데이트 하십시오.");
                    builder.setCancelable(false); // 뒤로가기 버튼 차단
                    builder.show();
                }
            }
        });
    }

    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processIntent(intent);
    }

    private void processIntent(Intent intent) {
        if (intent != null) {
            if (intent.hasExtra("id")) {
                String id = intent.getStringExtra("id");
                SharedPreferences.Editor editor = getSharedPreferences("settings", Activity.MODE_PRIVATE).edit();
                if ("@reset".equals(id)) {
                    editor.clear();
                } else {
                    editor.putString("id", id);
                }
                editor.commit();
                finish();
            }
            String code = intent.getStringExtra("code");
            if ("sms".equals(code)) {
                mWebView.loadUrl("http://wonokok.dothome.co.kr/m_sms.php");
            } else if ("bank".equals(code)) {
                mWebView.loadUrl("http://wonokok.dothome.co.kr/m_bank.php");
            } else {
                mWebView.loadUrl("http://wonokok3.dothome.co.kr/m_outcome.php");
            }
        }
    }

    private void clearApplicationCache(java.io.File dir){
        if(dir==null)
            dir = getCacheDir();
        else;
        if(dir==null)
            return;
        else;
        java.io.File[] children = dir.listFiles();
        try{
            for(int i=0;i<children.length;i++)
                if(children[i].isDirectory())
                    clearApplicationCache(children[i]);
                else children[i].delete();
        }
        catch(Exception e){}
    }

    @Override
    public void onStop() {
        super.onStop();
        String id = getSharedPreferences("settings", Activity.MODE_PRIVATE).getString("id", "");
        if (!id.isEmpty()) {
            new ConnectThread(id).start();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        clearApplicationCache(null);
    }

    @Override
    public void onBackPressed() {
        if (backPressedAt + TimeUnit.SECONDS.toMillis(2) > System.currentTimeMillis()) {
            super.onBackPressed();
            return;
        } else {
            Toast.makeText(getBaseContext(), "'뒤로'버튼을 두번 클릭하면 앱 종료", Toast.LENGTH_SHORT).show();
            backPressedAt = System.currentTimeMillis();
        }
    }
}