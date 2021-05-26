package com.easefun.polyvsdk.cloudclass;

import android.app.Application;

import com.easefun.polyv.livecommon.module.config.PLVLiveSDKConfig;
import com.facebook.react.ReactApplication;
import com.facebook.react.ReactNativeHost;
import com.facebook.react.ReactPackage;
import com.facebook.react.shell.MainReactPackage;
import com.facebook.soloader.SoLoader;
import com.polyv.rn.PolyvSceneRNPackage;
import com.swmansion.gesturehandler.react.RNGestureHandlerPackage;
import com.swmansion.reanimated.ReanimatedPackage;

import java.util.Arrays;
import java.util.List;

public class MainApplication extends Application implements ReactApplication {

  private final ReactNativeHost mReactNativeHost = new ReactNativeHost(this) {
    @Override
    public boolean getUseDeveloperSupport() {
      return BuildConfig.DEBUG;
    }

    @Override
    protected List<ReactPackage> getPackages() {
      return Arrays.<ReactPackage>asList(
          new MainReactPackage(),
            new RNGestureHandlerPackage(),
            new ReanimatedPackage(),
              new PolyvSceneRNPackage()
      );
    }

    @Override
    protected String getJSMainModuleName() {
      return "index";
    }
  };

  @Override
  public ReactNativeHost getReactNativeHost() {
    return mReactNativeHost;
  }

  @Override
  public void onCreate() {
//    PolyvLiveSDKClient.getInstance().initContext(this);
    super.onCreate();
    SoLoader.init(this, /* native exopackage */ false);
    //多场景sdk初始化
    PLVLiveSDKConfig.init(
            new PLVLiveSDKConfig.Parameter(this)//sdk初始化所需参数
                    .isOpenDebugLog(true)//是否打开调试日志
                    .isEnableHttpDns(false)//是否使用httpdns
    );
  }
}
