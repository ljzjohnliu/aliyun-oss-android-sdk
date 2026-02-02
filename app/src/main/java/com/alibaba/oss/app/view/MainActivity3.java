package com.alibaba.oss.app.view;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.oss.R;
import com.alibaba.oss.app.Config;
import com.alibaba.oss.app.service.MyOssService;
import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.common.OSSLog;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider;
import com.alibaba.sdk.android.oss.model.OSSRequest;
import com.alibaba.sdk.android.oss.model.OSSResult;

import java.io.File;
import java.util.Arrays;

public class MainActivity3 extends AppCompatActivity {
    public static String TAG = "MainActivity";
    private static Context context;
    private final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1000;

    //OSS的上传下载
    private MyOssService mService;

    Switch partSwitch;
    EditText filePathEdt;
    EditText sliceSizeEdt;
    TextView resultTv;

    private String[] fileSizeArray = {"5M", "10M", "20M", "50M", "80M", "150M", "400M", "800M", "1G", "10G"};
    private String fileSizeType;
    private String[] picFilePaths;
    private String[] videoFilePaths;
    private String uploadFilePath;

    private static long totalCostTime;
    private static int totalCount;
    private static int sucCount;

    public void updateResult(String fileSizeType) {
        String result = String.format("测试资源是：%s，上传平均耗时：， 成功率：", fileSizeType);
        resultTv.setText(result);
    }

    public void updateResult(final String fileSizeType, final long costTime, final int position, final int total) {
        resultTv.post(new Runnable() {
            @Override
            public void run() {
                String result = String.format("测试资源是：%s，上传平均耗时:%d， 成功率：%d / %d", fileSizeType, costTime, position, total);
                Log.d(TAG, "updateResult: ******* result = " + result);
                resultTv.setText(result);
            }
        });
    }

    class MySelectedListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            Toast.makeText(MainActivity3.this, "您选择的是：" + fileSizeArray[i], Toast.LENGTH_SHORT).show();
            fileSizeType = fileSizeArray[i];
//            updateResult(fileSizeType);

            String picFolderPath = "/mnt/sdcard/picture/" + fileSizeArray[i];
            Log.d(TAG, "onItemSelected: picFolderPath = " + picFolderPath);
            picFilePaths = MyUtil.traverseFolder(new File(picFolderPath));
            Log.d(TAG, "onItemSelected: picFilePaths = " + Arrays.toString(picFilePaths));

            String videoFolderPath = "/mnt/sdcard/video/" + fileSizeArray[i];
            Log.d(TAG, "onItemSelected: videoFolderPath = " + videoFolderPath);
            videoFilePaths = MyUtil.traverseFolder(new File(videoFolderPath));
            Log.d(TAG, "onItemSelected: videoFilePaths = " + Arrays.toString(videoFilePaths));
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    }

    private void initSpinner() {
        ArrayAdapter<String> starAdapter = new ArrayAdapter<>(this, R.layout.item_dropdown, fileSizeArray);
        Spinner sp = (Spinner)findViewById(R.id.file_size_spinner);
        sp.setAdapter(starAdapter);
        sp.setSelection(0);
        sp.setOnItemSelectedListener(new MySelectedListener());
    }

    public MyOssService initOSS(String endpoint, String bucket) {

//        移动端是不安全环境，不建议直接使用阿里云主账号ak，sk的方式。建议使用STS方式。具体参
//        https://help.aliyun.com/document_detail/31920.html
//        注意：SDK 提供的 PlainTextAKSKCredentialProvider 只建议在测试环境或者用户可以保证阿里云主账号AK，SK安全的前提下使用。具体使用如下
//        主账户使用方式
//        String AK = "******";
//        String SK = "******";
//        credentialProvider = new PlainTextAKSKCredentialProvider(AK,SK)
//        以下是使用STS Sever方式。
//        如果用STS鉴权模式，推荐使用OSSAuthCredentialProvider方式直接访问鉴权应用服务器，token过期后可以自动更新。
//        详见：https://help.aliyun.com/document_detail/31920.html
//        OSSClient的生命周期和应用程序的生命周期保持一致即可。在应用程序启动时创建一个ossClient，在应用程序结束时销毁即可。


        OSSCredentialProvider credentialProvider;
        String AK = Config.OSS_ACCESS_KEY_ID;
        String SK = Config.OSS_ACCESS_KEY_SECRET;
        credentialProvider = new OSSPlainTextAKSKCredentialProvider(AK,SK);

//        credentialProvider = new OSSAuthCredentialsProvider(Config.STS_SERVER_URL);

        //使用自己的获取STSToken的类
//        String stsServer = ((EditText) findViewById(R.id.stsserver)).getText().toString();
//        if (TextUtils.isEmpty(stsServer)) {
//            credentialProvider = new OSSAuthCredentialsProvider(Config.STS_SERVER_URL);
//            ((EditText) findViewById(R.id.stsserver)).setText(Config.STS_SERVER_URL);
//        } else {
//            credentialProvider = new OSSAuthCredentialsProvider(stsServer);
//        }

        ClientConfiguration conf = new ClientConfiguration();
        conf.setConnectionTimeout(15 * 1000); // 连接超时，默认15秒
        conf.setSocketTimeout(15 * 1000); // socket超时，默认15秒
        conf.setMaxConcurrentRequest(5); // 最大并发请求书，默认5个
        conf.setMaxErrorRetry(2); // 失败后最大重试次数，默认2次
        OSS oss = new OSSClient(getApplicationContext(), endpoint, credentialProvider, conf);
        OSSLog.enableLog();
        return new MyOssService(oss, bucket);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        context = this;
        partSwitch = (Switch) findViewById(R.id.switch_part);
        filePathEdt = (EditText) findViewById(R.id.file_path);
        sliceSizeEdt = (EditText) findViewById(R.id.slice_size);
        resultTv = (TextView) findViewById(R.id.upload_result);
        initSpinner();
        mService = initOSS(Config.OSS_ENDPOINT, Config.BUCKET_NAME);
        requestPermissions();
//        resultTv.setText("测试资源是：视频5M，上传平均耗时:583， 成功率：1 / 1");
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Toast.makeText(this, "您必须允许读取外部存储权限，否则无法上传文件", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public void videoUpload(View view) {
        sucCount = 0;
        totalCount = videoFilePaths.length;
        totalCostTime = 0;
        updateResult(fileSizeType, 0, 0, totalCount);
        boolean isSingleFile = false;
        if (!isSingleFile) {
            String[] filePaths = videoFilePaths;
            if (filePaths == null) {
                Toast.makeText(MainActivity3.this, "Picture filePaths is null!", Toast.LENGTH_SHORT).show();
                return;
            }
            for (int i = 0; i < filePaths.length; i++) {
                File file = new File(filePaths[i]);
                String fileName = file.getName();
                Log.d(TAG, "videoUpload: uploadFilePath = " + uploadFilePath + ", filePaths[" + i + "] = " + filePaths[i] + "， fileName = " + fileName);
                if (partSwitch.isChecked()) {
                    mService.asyncMultipartUpload(fileName, filePaths[i], new MyOssService.PutFileCallBack() {
                        @Override
                        public void onResult(OSSRequest request, OSSResult putObjectResult, long costTime) {
                            sucCount++;
                            totalCostTime += costTime;
                            long averageCost = totalCostTime / sucCount;
                            Log.d(TAG, "videoUpload: 多视频分片 onResult request = " + request + "，putObjectResult = " + putObjectResult + "，costTime = " + costTime);
                            Log.d(TAG, "videoUpload onResult: costTime = " + costTime + ", sucCount = " + sucCount + ", totalCostTime = " + totalCostTime + ", averageCost = " + averageCost + ", totalCount = " + totalCount);
                            updateResult("视频" + fileSizeType, averageCost, sucCount, totalCount);
                        }

                        @Override
                        public void onFailure(OSSRequest request, ClientException clientException, ServiceException serviceException) {
                            Log.d(TAG, "videoUpload: 多视频分片 onFailure request = " + request + "，clientException = " + clientException + "，serviceException = " + serviceException);
                        }
                    });
                } else {
                    mService.asyncPutObject(fileName, filePaths[i], new MyOssService.PutFileCallBack() {
                        @Override
                        public void onResult(OSSRequest request, OSSResult putObjectResult, long costTime) {
                            sucCount++;
                            totalCostTime += costTime;
                            long averageCost = totalCostTime / sucCount;
//                            Log.d(TAG, "videoUpload: 多视频直传 onResult request = " + request + "，putObjectResult = " + putObjectResult + "，costTime = " + costTime);
                            Log.d(TAG, "videoUpload onResult: costTime = " + costTime + ", sucCount = " + sucCount + ", totalCostTime = " + totalCostTime + ", averageCost = " + averageCost + ", totalCount = " + totalCount);
                            updateResult("视频" + fileSizeType, averageCost, sucCount, totalCount);
                        }

                        @Override
                        public void onFailure(OSSRequest request, ClientException clientException, ServiceException serviceException) {
                            Log.d(TAG, "videoUpload: 多视频直传 onResult request = " + request + "，clientException = " + clientException
                                    + "，serviceException = " + serviceException);
                        }
                    });
                }
            }
        } else {
            uploadFilePath = filePathEdt.getText().toString();
            uploadFilePath = "/mnt/sdcard/test_video1.mp4";
            File file = new File(uploadFilePath);
            String fileName = file.getName();
            Log.d(TAG, "videoUpload: uploadFilePath = " + uploadFilePath + "， fileName = " + fileName);
            if (partSwitch.isChecked()) {
                mService.asyncMultipartUpload(fileName, uploadFilePath, new MyOssService.PutFileCallBack() {
                    @Override
                    public void onResult(OSSRequest request, OSSResult putObjectResult, long costTime) {
                        Log.d(TAG, "videoUpload: 单视频分片 onResult request = " + request + "，putObjectResult = " + putObjectResult + "，costTime = " + costTime);
                        updateResult("视频" + fileSizeType, costTime, 1, 1);
                    }

                    @Override
                    public void onFailure(OSSRequest request, ClientException clientException, ServiceException serviceException) {
                        Log.d(TAG, "videoUpload: 单视频分片 onFailure request = " + request + "，clientException = " + clientException + "，serviceException = " + serviceException);
                    }
                });
            } else {
                mService.asyncPutObject(fileName, uploadFilePath, new MyOssService.PutFileCallBack() {
                    @Override
                    public void onResult(OSSRequest request, OSSResult putObjectResult, long costTime) {
                        Log.d(TAG, "videoUpload: 单视频直传 onResult request = " + request + "，putObjectResult = " + putObjectResult + "，getRequestId() = " + putObjectResult.getRequestId()
                                + "，costTime = " + costTime);
                        updateResult("视频" + fileSizeType, costTime, 1, 1);
                    }

                    @Override
                    public void onFailure(OSSRequest request, ClientException clientException, ServiceException serviceException) {
                        Log.d(TAG, "videoUpload: 单视频直传 onFailure request = " + request + "，clientException = " + clientException
                                + "，serviceException = " + serviceException);
                    }
                });
            }
        }
    }

    public void picUpload(View view) {
        boolean isSingleFile = true;
        if (!isSingleFile) {
            String[] filePaths = picFilePaths;
            if (filePaths == null) {
                Toast.makeText(MainActivity3.this, "Picture filePaths is null!", Toast.LENGTH_SHORT).show();
                return;
            }
            for (int i = 0; i < filePaths.length; i++) {
                File file = new File(filePaths[i]);
                String fileName = file.getName();
                Log.d(TAG, "picUpload: uploadFilePath = " + uploadFilePath + ", filePaths[" + i + "] = " + filePaths[i] + "， fileName = " + fileName);
                if (partSwitch.isChecked()) {
                    mService.asyncMultipartUpload(fileName, filePaths[i], new MyOssService.PutFileCallBack() {
                        @Override
                        public void onResult(OSSRequest request, OSSResult putObjectResult, long costTime) {
                            sucCount++;
                            totalCostTime += costTime;
                            long averageCost = totalCostTime / sucCount;
                            Log.d(TAG, "picUpload: 多图分片 onResult request = " + request + "，putObjectResult = " + putObjectResult + "，costTime = " + costTime);
                            Log.d(TAG, "picUpload onResult: costTime = " + costTime + ", sucCount = " + sucCount + ", totalCostTime = " + totalCostTime + ", averageCost = " + averageCost + ", totalCount = " + totalCount);
                            updateResult("图片" + fileSizeType, averageCost, sucCount, totalCount);
                        }

                        @Override
                        public void onFailure(OSSRequest request, ClientException clientException, ServiceException serviceException) {
                            Log.d(TAG, "picUpload: 多图分片 onFailure request = " + request + "，clientException = " + clientException
                                    + "，serviceException = " + serviceException);
                        }
                    });
                } else {
                    mService.asyncPutObject(fileName, filePaths[i], new MyOssService.PutFileCallBack() {
                        @Override
                        public void onResult(OSSRequest request, OSSResult putObjectResult, long costTime) {
                            sucCount++;
                            totalCostTime += costTime;
                            long averageCost = totalCostTime / sucCount;
                            Log.d(TAG, "picUpload: 多图直传 onResult request = " + request + "，putObjectResult = " + putObjectResult + "，costTime = " + costTime);
                            Log.d(TAG, "picUpload onResult: costTime = " + costTime + ", sucCount = " + sucCount + ", totalCostTime = " + totalCostTime + ", averageCost = " + averageCost + ", totalCount = " + totalCount);
                            updateResult("图片" + fileSizeType, averageCost, sucCount, totalCount);
                        }

                        @Override
                        public void onFailure(OSSRequest request, ClientException clientException, ServiceException serviceException) {
                            Log.d(TAG, "picUpload: 多图直传 onResult request = " + request + "，clientException = " + clientException
                                    + "，serviceException = " + serviceException);
                        }
                    });
                }
            }
        } else {
            uploadFilePath = filePathEdt.getText().toString();
            uploadFilePath = "/mnt/sdcard/test_gif.gif";
            File file = new File(uploadFilePath);
            String fileName = file.getName();
            Log.d(TAG, "picUpload: uploadFilePath = " + uploadFilePath + "， fileName = " + fileName);
            if (partSwitch.isChecked()) {
                mService.asyncMultipartUpload(fileName, uploadFilePath, new MyOssService.PutFileCallBack() {
                    @Override
                    public void onResult(OSSRequest request, OSSResult putObjectResult, long costTime) {
                        Log.d(TAG, "picUpload: 单图分片 onResult request = " + request + "，putObjectResult = " + putObjectResult
                                + "，costTime = " + costTime);
                        updateResult("图片" + fileSizeType, costTime, 1, 1);
                    }

                    @Override
                    public void onFailure(OSSRequest request, ClientException clientException, ServiceException serviceException) {
                        Log.d(TAG, "picUpload: 单图分片 onFailure request = " + request + "，clientException = " + clientException
                                + "，serviceException = " + serviceException);
                    }
                });
            } else {
                mService.asyncPutObject(fileName, uploadFilePath, new MyOssService.PutFileCallBack() {
                    @Override
                    public void onResult(OSSRequest request, OSSResult putObjectResult, long costTime) {
                        Log.d(TAG, "picUpload: 单图直传 onResult request = " + request + "，putObjectResult = " + putObjectResult + "，getRequestId() = " + putObjectResult.getRequestId()
                                + "，costTime = " + costTime);
                        updateResult("图片" + fileSizeType, costTime, 1, 1);
                    }

                    @Override
                    public void onFailure(OSSRequest request, ClientException clientException, ServiceException serviceException) {
                        Log.d(TAG, "picUpload: 单图直传 onFailure request = " + request + "，clientException = " + clientException
                                + "，serviceException = " + serviceException);
                    }
                });
            }
        }
    }
}