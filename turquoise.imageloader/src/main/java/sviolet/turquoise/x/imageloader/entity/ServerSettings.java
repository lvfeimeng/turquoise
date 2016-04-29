/*
 * Copyright (C) 2015-2016 S.Violet
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

package sviolet.turquoise.x.imageloader.entity;

import android.content.Context;

import java.io.File;

import sviolet.turquoise.util.common.CheckUtils;
import sviolet.turquoise.util.droid.DeviceUtils;
import sviolet.turquoise.util.droid.DirectoryUtils;
import sviolet.turquoise.x.imageloader.ComponentManager;
import sviolet.turquoise.x.imageloader.drawable.BackgroundDrawableFactory;
import sviolet.turquoise.x.imageloader.drawable.FailedDrawableFactory;
import sviolet.turquoise.x.imageloader.drawable.LoadingDrawableFactory;
import sviolet.turquoise.x.imageloader.drawable.common.CommonBackgroundDrawableFactory;
import sviolet.turquoise.x.imageloader.drawable.common.CommonFailedDrawableFactory;
import sviolet.turquoise.x.imageloader.drawable.common.CommonLoadingDrawableFactory;
import sviolet.turquoise.x.imageloader.handler.DecodeHandler;
import sviolet.turquoise.x.imageloader.handler.ExceptionHandler;
import sviolet.turquoise.x.imageloader.handler.ImageResourceHandler;
import sviolet.turquoise.x.imageloader.handler.NetworkLoadHandler;
import sviolet.turquoise.x.imageloader.handler.common.CommonDecodeHandler;
import sviolet.turquoise.x.imageloader.handler.common.CommonExceptionHandler;
import sviolet.turquoise.x.imageloader.handler.common.CommonImageResourceHandler;
import sviolet.turquoise.x.imageloader.handler.common.CommonNetworkLoadHandler;
import sviolet.turquoise.x.imageloader.node.TaskFactory;
import sviolet.turquoise.x.imageloader.node.TaskFactoryImpl;
import sviolet.turquoise.x.imageloader.stub.StubFactory;
import sviolet.turquoise.x.imageloader.stub.StubFactoryImpl;

/**
 * <p>TILoader global settings</p>
 *
 * Created by S.Violet on 2016/2/16.
 */
public class ServerSettings implements ComponentManager.Component{

    private static class Values{

        //settings////////////////////////////////////////////////////////////////////////////

        private boolean logEnabled = DEFAULT_LOG_ENABLED;
        private boolean wipeDiskCacheWhenUpdate = DEFAULT_WIPE_DISK_CACHE_WHEN_UPDATE;
        private int memoryCacheSize = DEFAULT_MEMORY_CACHE_SIZE;
        private int diskCacheSize = DEFAULT_DISK_CACHE_SIZE;
        private int memoryLoadMaxThread = DEFAULT_MEMORY_LOAD_MAX_THREAD;
        private int diskLoadMaxThread = DEFAULT_DISK_LOAD_MAX_THREAD;
        private int networkLoadMaxThread = DEFAULT_NETWORK_LOAD_MAX_THREAD;
        private long networkConnectTimeout = DEFAULT_NETWORK_CONNECT_TIMEOUT;
        private long networkReadTimeout = DEFAULT_NETWORK_READ_TIMEOUT;
        private File diskCachePath = null;

        //handler////////////////////////////////////////////////////////////////////////////

        private ImageResourceHandler imageResourceHandler = new CommonImageResourceHandler();
        private NetworkLoadHandler networkLoadHandler = new CommonNetworkLoadHandler();
        private DecodeHandler decodeHandler = new CommonDecodeHandler();
        private ExceptionHandler exceptionHandler = new CommonExceptionHandler();

        //configurable factory////////////////////////////////////////////////////////////////////////////

        private final StubFactoryImpl stubFactory = new StubFactoryImpl();
        private LoadingDrawableFactory loadingDrawableFactory;
        private FailedDrawableFactory failedDrawableFactory;
        private BackgroundDrawableFactory backgroundDrawableFactory = new CommonBackgroundDrawableFactory();

        //static factory////////////////////////////////////////////////////////////////////////////

        private final TaskFactory taskFactory = new TaskFactoryImpl();
    }

    public static class Builder{

        private Values values;

        public Builder(){
            values = new Values();
        }

        public ServerSettings build(){
            return new ServerSettings(values);
        }

        //settings////////////////////////////////////////////////////////////////////////////

        /**
         * set if TILoader's logger enabled
         * @param enabled true: enable
         */
        public Builder setLogEnabled(boolean enabled){
            values.logEnabled = enabled;
            return this;
        }

        /**
         * @param wipeDiskCacheWhenUpdate if true, disk cache will wipe when App update (versionCode change)
         */
        public Builder setWipeDiskCacheWhenUpdate(boolean wipeDiskCacheWhenUpdate){
            values.wipeDiskCacheWhenUpdate = wipeDiskCacheWhenUpdate;
            return this;
        }

        /**
         * set the memory cache size by percent of app's MemoryClass
         * @param context context
         * @param percent percent of app's MemoryClass (0f-0.5f)
         */
        public Builder setMemoryCachePercent(Context context, float percent){
            if (context == null){
                throw new RuntimeException("[ServerSettings]setMemoryCachePercent:　context is null!");
            }
            //控制上下限
            if (percent < 0){
                percent = 0;
            }else if (percent > 0.5f){
                percent = 0.5f;
            }
            //应用可用内存级别
            final int memoryClass = DeviceUtils.getMemoryClass(context);
            //计算缓存大小
            values.memoryCacheSize = (int) (1024 * 1024 * memoryClass * percent);
            return this;
        }

        /**
         * set the disk cache size
         * @param sizeMb mb, > 0
         */
        public Builder setDiskCacheSize(float sizeMb){
            //控制上下限
            if (sizeMb <= 0){
                throw new RuntimeException("[ServerSettings]setDiskCacheSize: size must be >0");
            }
            values.diskCacheSize = (int) (sizeMb * 1024 * 1024);
            return this;
        }

        /**
         * set the max thread of memory loading engine
         * @param maxThread max thread num, >=1, {@value DEFAULT_MEMORY_LOAD_MAX_THREAD} by default
         */
        public Builder setMemoryLoadMaxThread(int maxThread){
            if (maxThread < 1){
                throw new RuntimeException("[ServerSettings]setMemoryLoadMaxThread must >= 1");
            }
            values.memoryLoadMaxThread = maxThread;
            return this;
        }

        /**
         * set the max thread of disk loading engine
         * @param maxThread max thread num, >=1, {@value DEFAULT_DISK_LOAD_MAX_THREAD} by default
         */
        public Builder setDiskLoadMaxThread(int maxThread){
            if (maxThread < 1){
                throw new RuntimeException("[ServerSettings]diskLoadMaxThread must >= 1");
            }
            values.diskLoadMaxThread = maxThread;
            return this;
        }

        /**
         * set the max thread of network loading engine
         * @param maxThread max thread num, >=1, {@value DEFAULT_NETWORK_LOAD_MAX_THREAD} by default
         */
        public Builder setNetworkLoadMaxThread(int maxThread){
            if (maxThread < 1){
                throw new RuntimeException("[ServerSettings]networkLoadMaxThread must >= 1");
            }
            values.networkLoadMaxThread = maxThread;
            return this;
        }

        /**
         * set timeout of network connect
         * @param timeout timeout milli second
         */
        public Builder setNetworkConnectTimeout(long timeout){
            if (timeout <= 0){
                throw new RuntimeException("[ServerSettings]connect timeout must > 0");
            }
            values.networkConnectTimeout = timeout;
            return this;
        }

        /**
         * set timeout of network read
         * @param timeout timeout milli second
         */
        public Builder setNetworkReadTimeout(long timeout){
            if (timeout <= 0){
                throw new RuntimeException("[ServerSettings]read timeout must > 0");
            }
            values.networkReadTimeout = timeout;
            return this;
        }

        /**
         * set disk cache path
         * @param context context
         * @param diskCachePath {@link DiskCachePath#INNER_STORAGE} by default
         * @param subPath sub path of cache directory
         */
        public Builder setDiskCachePath(Context context, DiskCachePath diskCachePath, String subPath){
            if (context == null){
                throw new RuntimeException("[ServerSettings]setDiskCachePath:　context is null!");
            }
            values.diskCachePath = fetchDiskCachePath(context, diskCachePath, subPath);
            return this;
        }

        //handler////////////////////////////////////////////////////////////////////////////

        /**
         * @param imageResourceHandler custom ImageResourceHandler
         */
        public Builder setImageResourceHandler(ImageResourceHandler imageResourceHandler){
            if (imageResourceHandler != null){
                values.imageResourceHandler = imageResourceHandler;
            }
            return this;
        }

        /**
         * @param networkLoadHandler custom network load implementation
         */
        public Builder setNetworkLoadHandler(NetworkLoadHandler networkLoadHandler){
            if (networkLoadHandler != null){
                values.networkLoadHandler = networkLoadHandler;
            }
            return this;
        }

        /**
         * @param decodeHandler custom decode implementation
         */
        public Builder setDecodeHandler(DecodeHandler decodeHandler){
            if (decodeHandler != null){
                values.decodeHandler = decodeHandler;
            }
            return this;
        }

        /**
         * @param exceptionHandler custom exception handling
         */
        public Builder setExceptionHandler(ExceptionHandler exceptionHandler){
            if (exceptionHandler != null){
                values.exceptionHandler = exceptionHandler;
            }
            return this;
        }

        //configurable factory////////////////////////////////////////////////////////////////////////////

        /**
         * <p>Example:</p>
         *
         * <p>Basic usage of CustomStubFactory, add support for TextView.
         * TILoader.node(...).load(...) can load image to TextView's left drawable.</p>
         *
         * <pre>{@code
         *
         *      //set customStubFactory
         *      TILoader.setting(new ServerSettings.Builder()
         *          .setCustomStubFactory(new MyStubFactory())
         *          .build());
         *
         *      //implement SubFactory
         *      public class MyStubFactory extends StubFactory {
         *
         *          public Stub newLoadStub(String url, Params params, View view) {
         *              //check view
         *              if (view instanceof TextView){
         *                  return new TextViewStub(url, params, (TextView) view);
         *              }
         *              return null;
         *          }
         *
         *          private static class TextViewStub extends LoadStub<TextView>{
         *
         *              public TextViewStub(String url, Params params, TextView view) {
         *                  super(url, params, view);
         *              }
         *
         *              protected void setDrawableToView(Drawable drawable, TextView view) {
         *                  //set drawable to view
         *                  drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
         *                  view.setCompoundDrawables(drawable, null, null, null);
         *              }
         *          }
         *      }
         * }</pre>
         *
         * @param customStubFactory custom stub factory
         */
        public Builder setCustomStubFactory(StubFactory customStubFactory){
            values.stubFactory.setCustomStubFactory(customStubFactory);
            return this;
        }

        /**
         * show when image loading
         * @param factory loadingDrawable factory
         */
        public Builder setLoadingDrawableFactory(LoadingDrawableFactory factory){
            if (factory != null){
                values.loadingDrawableFactory = factory;
            }
            return this;
        }

        /**
         * show when image load failed
         * @param factory failedDrawable factory
         */
        public Builder setFailedDrawableFactory(FailedDrawableFactory factory){
            if (factory != null){
                values.failedDrawableFactory = factory;
            }
            return this;
        }

        /**
         * for image's background
         * @param backgroundImageResId image resource id
         */
        public Builder setBackgroundImageResId(int backgroundImageResId){
            values.backgroundDrawableFactory.setBackgroundImageResId(backgroundImageResId);
            return this;
        }

        /**
         * for image's background
         * @param backgroundColor color
         */
        public Builder setBackgroundColor(int backgroundColor){
            values.backgroundDrawableFactory.setBackgroundColor(backgroundColor);
            return this;
        }

    }

    //DEFAULT/////////////////////////////////////////////////////////////////////////////

    public static final boolean DEFAULT_LOG_ENABLED = true;
    public static final boolean DEFAULT_WIPE_DISK_CACHE_WHEN_UPDATE = false;
    public static final int DEFAULT_MEMORY_CACHE_SIZE = 0;
    public static final int DEFAULT_DISK_CACHE_SIZE = 30 * 1024 * 1024;
    public static final int DEFAULT_MEMORY_LOAD_MAX_THREAD = 1;
    public static final int DEFAULT_DISK_LOAD_MAX_THREAD = 2;
    public static final int DEFAULT_NETWORK_LOAD_MAX_THREAD = 3;
    public static final long DEFAULT_NETWORK_CONNECT_TIMEOUT = 3000;//ms
    public static final long DEFAULT_NETWORK_READ_TIMEOUT = 3000;//ms

    public static final DiskCachePath DEFAULT_DISK_CACHE_PATH = DiskCachePath.INNER_STORAGE;
    public static final String DEFAULT_DISK_CACHE_SUB_PATH = "TILoader";

    //Var/////////////////////////////////////////////////////////////////////////////////

    private ComponentManager manager;
    private Values values;

    private ServerSettings(Values values) {
        this.values = values;
    }

    @Override
    public void init(ComponentManager manager) {
        this.manager = manager;
        values.taskFactory.init(manager);
        if (values.loadingDrawableFactory == null) {
            values.loadingDrawableFactory = new CommonLoadingDrawableFactory();
        }
        if(values.failedDrawableFactory == null) {
            values.failedDrawableFactory = new CommonFailedDrawableFactory();
        }
    }

    //settings////////////////////////////////////////////////////////////////////////////

    public boolean isLogEnabled(){
        return values.logEnabled;
    }

    public boolean isWipeDiskCacheWhenUpdate(){
        return values.wipeDiskCacheWhenUpdate;
    }

    public int getMemoryCacheSize(){
        return values.memoryCacheSize;
    }

    public int getDiskCacheSize(){
        return values.diskCacheSize;
    }

    public int getMemoryLoadMaxThread(){
        return values.memoryLoadMaxThread;
    }

    public int getNetworkLoadMaxThread(){
        return values.networkLoadMaxThread;
    }

    public int getDiskLoadMaxThread(){
        return values.diskLoadMaxThread;
    }

    /**
     * @return milli second
     */
    public long getNetworkConnectTimeout(){
        return values.networkConnectTimeout;
    }

    /**
     * @return milli second
     */
    public long getNetworkReadTimeout(){
        return values.networkReadTimeout;
    }

    public File getDiskCachePath(){
        if (values.diskCachePath == null){
            values.diskCachePath = fetchDiskCachePath(manager.getApplicationContextImage(), DEFAULT_DISK_CACHE_PATH, null);
        }
        return values.diskCachePath;
    }

    //handler////////////////////////////////////////////////////////////////////////////

    public ImageResourceHandler getImageResourceHandler(){
        return values.imageResourceHandler;
    }

    public NetworkLoadHandler getNetworkLoadHandler(){
        return values.networkLoadHandler;
    }

    public DecodeHandler getDecodeHandler(){
        return values.decodeHandler;
    }

    public ExceptionHandler getExceptionHandler(){
        return values.exceptionHandler;
    }

    //configurable factory////////////////////////////////////////////////////////////////////////////

    public StubFactory getStubFactory(){
        return values.stubFactory;
    }

    public LoadingDrawableFactory getLoadingDrawableFactory(){
        return values.loadingDrawableFactory;
    }

    public FailedDrawableFactory getFailedDrawableFactory(){
        return values.failedDrawableFactory;
    }

    public BackgroundDrawableFactory getBackgroundDrawableFactory(){
        return values.backgroundDrawableFactory;
    }

    //static factory////////////////////////////////////////////////////////////////////////////

    public TaskFactory getTaskFactory(){
        return values.taskFactory;
    }

    /*************************************************************
     * enum
     */

    public enum DiskCachePath{
        INNER_STORAGE,
        EXTERNAL_STORAGE
    }

    /**************************************************************
     * private
     */

    private static File fetchDiskCachePath(Context context, DiskCachePath diskCachePath, String subPath){
        if (CheckUtils.isEmpty(subPath)){
            subPath = DEFAULT_DISK_CACHE_SUB_PATH;
        }
        switch (diskCachePath){
            case EXTERNAL_STORAGE:
                return DirectoryUtils.getCacheDir(context, subPath);
            case INNER_STORAGE:
            default:
                return new File(DirectoryUtils.getInnerCacheDir(context).getAbsolutePath() + File.separator + subPath);
        }
    }

}
