package cn.reactnative.modules.update;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.bridge.JSBundleLoader;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by tdzl2003 on 3/31/16.
 */
public class UpdateModule extends ReactContextBaseJavaModule{
    UpdateContext updateContext;
    public static ReactApplicationContext mContext;
    public UpdateModule(ReactApplicationContext reactContext, UpdateContext updateContext) {
        super(reactContext);
        this.updateContext = updateContext;
        mContext=reactContext;
    }

    public UpdateModule(ReactApplicationContext reactContext) {
        this(reactContext, new UpdateContext(reactContext.getApplicationContext()));
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("downloadRootDir", updateContext.getRootDir());
        constants.put("packageVersion", updateContext.getPackageVersion());
        constants.put("currentVersion", updateContext.getCurrentVersion());
        constants.put("buildTime", updateContext.getBuildTime());
        constants.put("isUsingBundleUrl", updateContext.getIsUsingBundleUrl());
        boolean isFirstTime = updateContext.isFirstTime();
        constants.put("isFirstTime", isFirstTime);
        if (isFirstTime) {
            updateContext.clearFirstTime();
        }
        boolean isRolledBack = updateContext.isRolledBack();
        constants.put("isRolledBack", isRolledBack);
        if (isRolledBack) {
            updateContext.clearRollbackMark();
        }
        constants.put("blockUpdate", updateContext.getBlockUpdate());
        constants.put("uuid", updateContext.getUuid());
        return constants;
    }

    @Override
    public String getName() {
        return "RCTPushy";
    }

    @ReactMethod
    public void downloadUpdate(ReadableMap options, final Promise promise){
        String url = options.getString("updateUrl");
        String hash = options.getString("hash");
        updateContext.downloadFile(url, hash, new UpdateContext.DownloadFileListener() {
            @Override
            public void onDownloadCompleted() {
                promise.resolve(null);
            }

            @Override
            public void onDownloadFailed(Throwable error) {
                promise.reject(error);
            }
        });
    }

    @ReactMethod
    public void downloadPatchFromPackage(ReadableMap options, final Promise promise){
        String url = options.getString("updateUrl");
        String hash = options.getString("hash");
        updateContext.downloadPatchFromApk(url, hash, new UpdateContext.DownloadFileListener() {
            @Override
            public void onDownloadCompleted() {
                promise.resolve(null);
            }

            @Override
            public void onDownloadFailed(Throwable error) {
                promise.reject(error);
            }
        });
    }

    @ReactMethod
    public void downloadPatchFromPpk(ReadableMap options, final Promise promise){
        String url = options.getString("updateUrl");
        String hash = options.getString("hash");
        String originHash = options.getString("originHash");
        updateContext.downloadPatchFromPpk(url, hash, originHash, new UpdateContext.DownloadFileListener() {
            @Override
            public void onDownloadCompleted() {
                promise.resolve(null);
            }

            @Override
            public void onDownloadFailed(Throwable error) {
                promise.reject(error);
            }
        });
    }

    @ReactMethod
    public void reloadUpdate(ReadableMap options) {
        final String hash = options.getString("hash");

        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    updateContext.switchVersion(hash);
                    Activity activity = getCurrentActivity();
                    Application application = activity.getApplication();
                    ReactInstanceManager instanceManager = updateContext.getCustomReactInstanceManager();

                    if (instanceManager == null) {
                        instanceManager = ((ReactApplication) application).getReactNativeHost().getReactInstanceManager();
                    }

                    try {
                        JSBundleLoader loader = JSBundleLoader.createFileLoader(UpdateContext.getBundleUrl(application));
                        Field loadField = instanceManager.getClass().getDeclaredField("mBundleLoader");
                        loadField.setAccessible(true);
                        loadField.set(instanceManager, loader);
                    } catch (Throwable err) {
                        Field jsBundleField = instanceManager.getClass().getDeclaredField("mJSBundleFile");
                        jsBundleField.setAccessible(true);
                        jsBundleField.set(instanceManager, UpdateContext.getBundleUrl(application));
                    }

                    try {
                        instanceManager.recreateReactContextInBackground();
                    } catch(Throwable err) {
                        activity.recreate();
                    }

                } catch (Throwable err) {
                    Log.e("pushy", "switchVersion failed", err);
                }
            }
        });
    }

    @ReactMethod
    public void setNeedUpdate(ReadableMap options) {
        final String hash = options.getString("hash");

        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    updateContext.switchVersion(hash);
                } catch (Throwable err) {
                    Log.e("pushy", "switchVersionLater failed", err);
                }
            }
        });
    }

    @ReactMethod
    public void markSuccess() {
        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateContext.markSuccess();
            }
        });
    }

    @ReactMethod
    public void setBlockUpdate(ReadableMap options) {
        final int until = options.getInt("until");
        final String reason = options.getString("reason");
        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateContext.setBlockUpdate(until, reason);
            }
        });
    }

    @ReactMethod
    public void setUuid(final String uuid) {
        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateContext.setUuid(uuid);
            }
        });
    }

    /* 发送事件*/
    public static void sendEvent(String eventName,  WritableMap params) {
        ((ReactContext) mContext).getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName,
                params);
    }
}
