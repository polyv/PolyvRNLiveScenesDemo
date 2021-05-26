package com.polyv.rn;

import android.text.TextUtils;
import android.util.Log;

import com.easefun.polyv.livecloudclass.scenes.PLVLCCloudClassActivity;
import com.easefun.polyv.livecommon.module.config.PLVLiveChannelConfigFiller;
import com.easefun.polyv.livecommon.module.config.PLVLiveScene;
import com.easefun.polyv.livecommon.module.utils.result.PLVLaunchResult;
import com.easefun.polyv.liveecommerce.scenes.PLVECLiveEcommerceActivity;
import com.easefun.polyv.livescenes.config.PolyvLiveChannelType;
import com.easefun.polyv.livescenes.feature.login.IPLVSceneLoginManager;
import com.easefun.polyv.livescenes.feature.login.PLVSceneLoginManager;
import com.easefun.polyv.livescenes.feature.login.PolyvLiveLoginResult;
import com.easefun.polyv.livescenes.feature.login.PolyvPlaybackLoginResult;
import com.easefun.polyv.livescenes.feature.login.model.PLVSLoginVO;
import com.easefun.polyv.livescenes.playback.video.PolyvPlaybackListType;
import com.easefun.polyv.livestreamer.modules.login.PLVLSLoginStreamerActivity;
import com.easefun.polyv.livestreamer.scenes.PLVLSLiveStreamerActivity;
import com.easefun.polyvsdk.cloudclass.R;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.plv.thirdpart.blankj.utilcode.util.ToastUtils;

import java.io.IOException;

import retrofit2.HttpException;

import static com.polyv.rn.PolyvErrorCode.channleLoadFailed;
import static com.polyv.rn.PolyvErrorCode.noAppScrect;

/**
 * @author ysh
 * @create 2021/2/22
 * @Describe 多场景封装的RN登录操作类，可登录进入多场景直播回放。（目前包括云课堂、直播带货场景
 */
public class PolyvSceneRNModule extends ReactContextBaseJavaModule {

    private static final String NAME = "PolyvSceneRNModule";
    public static final String TAG = NAME;

    private String viewerId;
    private String nickName;
    private String viewerAvatar;

    private String userId;
    private String appId;
    private String appSecret;

    private IPLVSceneLoginManager loginManager;

    public PolyvSceneRNModule(ReactApplicationContext reactContext) {
        super(reactContext);
        loginManager = new PLVSceneLoginManager();
    }

    // <editor-fold defaultstate="collapsed" desc="RN重写方法">

    @Override
    public String getName() {
        return NAME;
    }

    // </editor-fold >


    // <editor-fold defaultstate="collapsed" desc="RN-多场景 sdk 接口">

    @ReactMethod
    public void setConfig(String userId, String appId, String appSecret, Promise promise){
        int code = PolyvErrorCode.success;
        if(TextUtils.isEmpty(userId)){
            code = PolyvErrorCode.noUserId;
        } else if(TextUtils.isEmpty(appId)){
            code = PolyvErrorCode.noAppId;
        } else if(TextUtils.isEmpty(appSecret)){
            code = noAppScrect;
        }

        if(code != PolyvErrorCode.success){
            String errorMassage =  PolyvErrorCode.getDesc(code);
            RuntimeException exception = new RuntimeException(errorMassage);
            promise.reject(code + "",errorMassage, exception );
            return;
        }

        this.userId = userId;
        this.appId = appId;
        this.appSecret = appSecret;

        PLVLiveChannelConfigFiller.setupAccount(userId, appId, appSecret);
        WritableMap map = Arguments.createMap();
        map.putInt("code", PolyvErrorCode.success);
        promise.resolve(map);
    }

    @ReactMethod
    public void setViewerInfo(String viewerId, String viewerName, String viewerAvatar, Promise promise){
        int code = PolyvErrorCode.success;
        if (TextUtils.isEmpty(viewerId)) {
            code = PolyvErrorCode.noViewId;
            sendErrorMessage(promise, code);
            return;
        }

        this.viewerId = viewerId;
        this.nickName = viewerName;
        this.viewerAvatar = viewerAvatar;
        PLVLiveChannelConfigFiller.setupUser(viewerId, viewerName, viewerAvatar);
        sendSuccessMessage(promise);
    }


    @ReactMethod
    public void loginLive(int reactTag, int sceneType, String channelId, Promise promise){
        PLVLiveScene curScene = getSceneType(sceneType);

        loginManager.loginLive(appId, appSecret, userId, channelId, new IPLVSceneLoginManager.OnLoginListener<PolyvLiveLoginResult>() {
            @Override
            public void onLoginSuccess(PolyvLiveLoginResult polyvLiveLoginResult) {
                PolyvLiveChannelType channelType = polyvLiveLoginResult.getChannelType();
                switch (curScene) {
                    //进入云课堂场景
                    case CLOUDCLASS:
                        if (PLVLiveScene.isCloudClassSceneSupportType(channelType)) {
                            PLVLaunchResult launchResult = PLVLCCloudClassActivity.launchLive(getCurrentActivity(), channelId, channelType, viewerId, nickName, viewerAvatar);
                            if (launchResult.isSuccess()) {
                                sendSuccessMessage(promise);
                            } else {
                                RuntimeException exception = new RuntimeException(launchResult.getErrorMessage());
                                errorStatus(exception, promise);
                            }
                        } else {
//                            ToastUtils.showShort(R.string.plv_scene_login_toast_cloudclass_no_support_type);
                            String exceptionMsg = getReactApplicationContext().getString(R.string.plv_scene_login_toast_cloudclass_no_support_type);
                            RuntimeException exception = new RuntimeException(exceptionMsg);
                            errorStatus(exception, promise);
                        }
                        break;
                    //进入直播带货场景
                    case ECOMMERCE:
                        if (PLVLiveScene.isLiveEcommerceSceneSupportType(channelType)) {
                            PLVLaunchResult launchResult = PLVECLiveEcommerceActivity.launchLive(getCurrentActivity(), channelId, viewerId, nickName, viewerAvatar);
                            if (launchResult.isSuccess()) {
                                sendSuccessMessage(promise);
                            } else {
                                RuntimeException exception = new RuntimeException(launchResult.getErrorMessage());
                                errorStatus(exception, promise);
                            }
                        } else {
//                            ToastUtils.showShort(R.string.plv_scene_login_toast_liveecommerce_no_support_type);
                            String exceptionMsg = getReactApplicationContext().getString(R.string.plv_scene_login_toast_liveecommerce_no_support_type);
                            RuntimeException exception = new RuntimeException(exceptionMsg);
                            errorStatus(exception, promise);
                        }
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onLoginFailed(String s, Throwable throwable) {
                Log.e(TAG, throwable.getMessage());
                throwable.printStackTrace();
                errorStatus(throwable, promise);
            }
        });
    }

    @ReactMethod
    public void loginPlayback(int reactTag, int sceneType, String channelId, String vid, int vodType, Promise promise){
        PLVLiveScene curScene = getSceneType(sceneType);
        loginManager.loginPlayback(appId, appSecret, userId, channelId, vid, new IPLVSceneLoginManager.OnLoginListener<PolyvPlaybackLoginResult>() {
            @Override
            public void onLoginSuccess(PolyvPlaybackLoginResult polyvPlaybackLoginResult) {
                PolyvLiveChannelType channelType = polyvPlaybackLoginResult.getChannelType();
                switch (curScene) {
                    //进入云课堂场景
                    case CLOUDCLASS:
                        if (PLVLiveScene.isCloudClassSceneSupportType(channelType)) {
                            PLVLaunchResult launchResult = PLVLCCloudClassActivity.launchPlayback(getCurrentActivity(), channelId, channelType,
                                    vid, viewerId, nickName,viewerAvatar,
                                    vodType == PolyvPlaybackListType.VOD ? PolyvPlaybackListType.VOD : PolyvPlaybackListType.PLAYBACK
                            );
                            if (launchResult.isSuccess()) {
//                                ToastUtils.showShort(launchResult.getErrorMessage());
                                sendSuccessMessage(promise);
                            } else {
                                RuntimeException exception = new RuntimeException(launchResult.getErrorMessage());
                                errorStatus(exception, promise);
                            }
                        } else {
//                            ToastUtils.showShort(R.string.plv_scene_login_toast_cloudclass_no_support_type);
                            String exceptionMsg = getReactApplicationContext().getString(R.string.plv_scene_login_toast_cloudclass_no_support_type);
                            RuntimeException exception = new RuntimeException(exceptionMsg);
                            errorStatus(exception, promise);
                        }
                        break;
                    //进入直播带货场景
                    case ECOMMERCE:
                        if (PLVLiveScene.isLiveEcommerceSceneSupportType(channelType)) {
                            PLVLaunchResult launchResult = PLVECLiveEcommerceActivity.launchPlayback(getCurrentActivity(), channelId,
                                    vid, viewerId, nickName,viewerAvatar,
                                    vodType == PolyvPlaybackListType.VOD ? PolyvPlaybackListType.VOD : PolyvPlaybackListType.PLAYBACK);
                            if (launchResult.isSuccess()) {
                                sendSuccessMessage(promise);
                            } else {
                                RuntimeException exception = new RuntimeException(launchResult.getErrorMessage());
                                errorStatus(exception, promise);
                            }
                        } else {
                            String exceptionMsg = getReactApplicationContext().getString(R.string.plv_scene_login_toast_liveecommerce_no_support_type);
                            RuntimeException exception = new RuntimeException(exceptionMsg);
                            errorStatus(exception, promise);
                        }
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onLoginFailed(String s, Throwable throwable) {
                Log.e(TAG, throwable.getMessage());
                throwable.printStackTrace();
                errorStatus(throwable, promise);
            }
        });
    }

/*
    @ReactMethod
    public void loginStreamer(String channelId, String password, Promise promise){
        loginManager.loginStreamer(channelId, password, new IPLVSceneLoginManager.OnLoginListener<PLVSLoginVO>() {
            @Override
            public void onLoginSuccess(PLVSLoginVO plvsLoginVO) {
                //不填写登录昵称时，使用登录接口返回的后台设置的昵称
                String loginNick = TextUtils.isEmpty(nickName) ? plvsLoginVO.getTeacherNickname() : nickName;
                String loginAvatar = TextUtils.isEmpty(viewerAvatar) ? plvsLoginVO.getTeacherAvatar() : viewerAvatar;
                //进入手机开播场景
                PLVLSLiveStreamerActivity.launchStreamer(
                        getCurrentActivity(),
                        plvsLoginVO.getChannelId(),
                        plvsLoginVO.getAccountId(),
                        loginNick,
                        loginAvatar,
                        plvsLoginVO.getTeacherActor(),
                        isOpenMic,
                        isOpenCamera,
                        isFrontCamera
                );

            }

            @Override
            public void onLoginFailed(String s, Throwable throwable) {

            }
        });
    }*/

    // </editor-fold >


    // <editor-fold defaultstate="collapsed" desc="内部方法">
    private void sendSuccessMessage(Promise promise) {
        WritableMap map = Arguments.createMap();
        map.putInt("code", PolyvErrorCode.success);
        promise.resolve(map);
    }

    private void sendErrorMessage(Promise promise, int code) {
        String errorCode = "" + code;
        String errorDesc = PolyvErrorCode.getDesc(code);
        Throwable throwable = new Throwable(errorDesc);
        Log.e(TAG, "errorCode=" + errorCode + "  errorDesc=" + errorDesc);
        promise.reject(errorCode, errorDesc, throwable);
    }

    private void sendErrorCallback(Callback callback, int code){
        if(callback != null){
            String errorCode = "" + code;
            String errorDesc = PolyvErrorCode.getDesc(code);
            callback.invoke(errorCode, errorDesc);
        }
    }

    private void sendSuccessCallback(Callback callback){
        if(callback != null){
            WritableMap map = Arguments.createMap();
            map.putInt("code", PolyvErrorCode.success);
            callback.invoke(map);
        }
    }

    private void errorStatus(Throwable e, Promise promise) {
        if (e instanceof HttpException) {
            try {
                sendErrorMessage(promise,channleLoadFailed);
                ToastUtils.showLong(((HttpException) e).response().errorBody().string());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } else {
            ToastUtils.showLong(e.getMessage());
        }
    }

    /**
     * 获取多场景类型
     * @param sceneType
     * 1 - 云课堂
     * 2 - 直播带货
     * @return 默认是云课堂场景
     */
    private PLVLiveScene getSceneType(int sceneType){
        switch (sceneType){
            case 1:
                return PLVLiveScene.CLOUDCLASS;
            case 2:
                return PLVLiveScene.ECOMMERCE;
            default:
                Log.e(TAG, "非法场景sceneType："+sceneType+", 将默认使用云课堂场景");
                return PLVLiveScene.CLOUDCLASS;
        }
    }
    // </editor-fold >
}
