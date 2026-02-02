package com.alibaba.oss.app.service;

import android.net.Uri;
import android.util.Log;

import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback;
import com.alibaba.sdk.android.oss.common.OSSLog;
import com.alibaba.sdk.android.oss.internal.OSSAsyncTask;
import com.alibaba.sdk.android.oss.model.CompleteMultipartUploadResult;
import com.alibaba.sdk.android.oss.model.MultipartUploadRequest;
import com.alibaba.sdk.android.oss.model.OSSRequest;
import com.alibaba.sdk.android.oss.model.OSSResult;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;

import java.io.File;

/**
 * Created by ljz on 2026/1/27
 * 支持普通上传 分片上传
 */
public class MyOssService {

    private static final String TAG = "OssService";
    public OSS mOss;
    private String mBucket;
    private PutFileCallBack mPutFileCallBack;

    public MyOssService(OSS oss, String bucket) {
        this.mOss = oss;
        this.mBucket = bucket;
    }

    public void setBucketName(String bucket) {
        this.mBucket = bucket;
    }

    public void initOss(OSS _oss) {
        this.mOss = _oss;
    }

    public interface PutFileCallBack {
        void onResult(OSSRequest request, OSSResult putObjectResult, long costTime);
        void onFailure(OSSRequest request, ClientException clientException, ServiceException serviceException);
    }

    public void asyncPutObject(String object, String localFile, final PutFileCallBack putFileCallBack) {
        final long upload_start = System.currentTimeMillis();
        OSSLog.logDebug("upload start upload_start = " + upload_start);
        Log.d(TAG, "asyncPutObject: upload start ====================== localFile = " + localFile + ", upload_start = " + upload_start);

        if (object.equals("")) {
            Log.w(TAG,"asyncPutObject object is Null");
            return;
        }

        File file = new File(localFile);
        if (!file.exists()) {
            Log.w(TAG, "syncPutObject， localFile = " + localFile + ", FileNotExist");
            return;
        }

        // 构造上传请求
        OSSLog.logDebug("create PutObjectRequest ");
        PutObjectRequest put = new PutObjectRequest(mBucket, object, localFile);

        // 异步上传时可以设置进度回调
        put.setProgressCallback(new OSSProgressCallback<PutObjectRequest>() {
            @Override
            public void onProgress(PutObjectRequest request, long currentSize, long totalSize) {
                int progress = (int) (100 * currentSize / totalSize);
                Log.d("PutObject", "currentSize: " + currentSize + " totalSize: " + totalSize + ", progress = " + progress);
            }
        });

        OSSLog.logDebug(" asyncPutObject ");
        OSSAsyncTask task = mOss.asyncPutObject(put, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
            @Override
            public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                Log.d(TAG, "PutObject, UploadSuccess ETag = " + result.getETag() + ", RequestId = " + result.getRequestId());
                long cost = (System.currentTimeMillis() - upload_start);
                Log.d(TAG,"asyncPutObject upload cost: " + cost + ", currentThread = " + Thread.currentThread());
                if (putFileCallBack != null) {
                    putFileCallBack.onResult(request, result, cost);
                }
                Log.d(TAG, "asyncPutObject PutObject Bucket: " + mBucket
                        + "\nObject: " + request.getObjectKey()
                        + "\nETag: " + result.getETag()
                        + "\nRequestId: " + result.getRequestId()
                        + "\nCallback: " + result.getServerCallbackReturnBody());
            }

            @Override
            public void onFailure(PutObjectRequest request, ClientException clientException, ServiceException serviceException) {
                if (putFileCallBack != null) {
                    putFileCallBack.onFailure(request, clientException, serviceException);
                }
                // 请求异常
                if (clientException != null) {
                    // 本地异常如网络异常等
                    clientException.printStackTrace();
                    Log.d(TAG, "onFailure: clientExcepion = " + clientException);
                }
                if (serviceException != null) {
                    // 服务异常
                    Log.e(TAG, "onFailure: ErrorCode = " + serviceException.getErrorCode() + ", RequestId = " + serviceException.getRequestId()
                            + ", HostId = " + serviceException.getHostId() + ", RawMessage = " + serviceException.getRawMessage());
                }
            }
        });
    }

    public void asyncMultipartUpload(String uploadKey, String uploadFilePath, final PutFileCallBack putFileCallBack) {
        final long upload_start = System.currentTimeMillis();
        MultipartUploadRequest request = new MultipartUploadRequest(mBucket, uploadKey, uploadFilePath);
        // 设置PartSize。PartSize默认值为256 KB，最小值为100 KB。
        request.setPartSize(1024 * 1024);
        request.setProgressCallback(new OSSProgressCallback<MultipartUploadRequest>() {
            @Override
            public void onProgress(MultipartUploadRequest request, long currentSize, long totalSize) {
                int progress = (int) (100 * currentSize / totalSize);
                Log.d(TAG, "onProgress: progress = " + progress);
                OSSLog.logDebug("[testMultipartUpload] - " + currentSize + " " + totalSize, false);
            }
        });
        mOss.asyncMultipartUpload(request, new OSSCompletedCallback<MultipartUploadRequest, CompleteMultipartUploadResult>() {
            @Override
            public void onSuccess(MultipartUploadRequest request, CompleteMultipartUploadResult result) {
                Log.d(TAG, "asyncMultipartUpload, onSuccess: result = " + result);
                long cost = (System.currentTimeMillis() - upload_start);
                Log.d(TAG,"asyncMultipartUpload, upload cost: " + cost);
                if (putFileCallBack != null) {
                    putFileCallBack.onResult(request, result, cost);
                }
                Log.d(TAG, "asyncMultipartUpload PutObject Bucket: " + mBucket
                        + "\nObject: " + request.getObjectKey()
                        + "\nETag: " + result.getETag()
                        + "\nRequestId: " + result.getRequestId()
                        + "\nCallback: " + result.getServerCallbackReturnBody());
            }

            @Override
            public void onFailure(MultipartUploadRequest request, ClientException clientException, ServiceException serviceException) {
                Log.d(TAG, "onFailure: clientException = " + clientException + ", serviceException = " + serviceException);
                if (putFileCallBack != null) {
                    putFileCallBack.onFailure(request, clientException, serviceException);
                }
            }
        });
    }
}
