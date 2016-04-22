/*
 * Copyright (C) 2015 S.Violet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project GitHub: https://github.com/shepherdviolet/turquoise
 * Email: shepherdviolet@163.com
 */

package sviolet.demoaimageloader;

import sviolet.demoaimageloader.common.Constants;
import sviolet.demoaimageloader.demos.extra.MyNetworkLoadHandler;
import sviolet.turquoise.enhance.app.TApplication;
import sviolet.turquoise.enhance.app.annotation.setting.ApplicationSettings;
import sviolet.turquoise.enhance.app.annotation.setting.DebugSettings;
import sviolet.turquoise.enhance.app.annotation.setting.ReleaseSettings;
import sviolet.turquoise.utilx.tlogger.TLogger;
import sviolet.turquoise.x.imageloader.TILoader;
import sviolet.turquoise.x.imageloader.entity.ServerSettings;

@ApplicationSettings(
        DEBUG = BuildConfig._DEBUG //Debug模式, 装载DebugSetting配置
)
//发布配置
@ReleaseSettings(
        enableStrictMode = false,
        enableCrashRestart = true,
        enableCrashHandle = true,
        logDefaultTag = Constants.TAG,
        logGlobalLevel = TLogger.ERROR | TLogger.INFO
)
//调试配置
@DebugSettings(
        enableStrictMode = true,
        enableCrashRestart = false,
        enableCrashHandle = true,
        logDefaultTag = Constants.TAG,
        logGlobalLevel = TLogger.ERROR | TLogger.INFO | TLogger.WARNING | TLogger.DEBUG
)
public class MyApplication extends TApplication {

    @Override
    public void onCreate() {
        super.onCreate();

        TILoader.setting(new ServerSettings.Builder()
                .setMemoryCachePercent(getApplicationContext(), 0.1f)
                .setDiskCacheSize(10)
//                .setNetworkLoadHandler(new MyNetworkLoadHandler())
                .setLogEnabled(true)
                .build());
    }

    @Override
    public void onUncaughtException(Throwable ex, boolean isCrashRestart) {
        //TODO 异常处理
    }

}
