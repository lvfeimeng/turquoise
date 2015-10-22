package sviolet.turquoise.utils.bitmap.loader;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

import sviolet.turquoise.utils.Logger;
import sviolet.turquoise.model.queue.TQueue;
import sviolet.turquoise.model.queue.TTask;
import sviolet.turquoise.utils.bitmap.BitmapUtils;
import sviolet.turquoise.utils.bitmap.CachedBitmapUtils;
import sviolet.turquoise.utils.cache.DiskLruCache;
import sviolet.turquoise.utils.sys.ApplicationUtils;
import sviolet.turquoise.utils.sys.DirectoryUtils;

/**
 * BitmapLoader<Br/>
 * 图片双缓存网络异步加载器<br/>
 * <br/>
 * 异步加载Bitmap, 适用性广泛, 兼容性好, 使用较复杂.<br/>
 * <br/>
 * ****************************************************************<br/>
 * * * * * BitmapLoader使用说明:<br/>
 * ****************************************************************<br/>
 * <br/>
 * -------------------初始化设置----------------<br/>
 * <br/>
 * 1.实现接口BitmapLoaderImplementor<br/>
 * 2.实例化BitmapLoader(Context,String,BitmapLoaderImplementor) <br/>
 * 3.设置参数:<br/>
 *   try {
 *       mBitmapLoader = new BitmapLoader(this, "bitmap", new MyBitmapLoaderImplementor())
 *          .setRamCache(0.125f, 0.125f)//设置内存缓存大小,启用回收站
 *          //.setRamCache(0.125f, 0)//回收站设置为0禁用
 *          .setDiskCache(50, 5, 15)//设置磁盘缓存容量50M, 磁盘加载并发数5, 等待队列15
 *          .setNetLoad(3, 15)//设置网络加载并发数3, 等待队列15
 *          .setImageQuality(Bitmap.CompressFormat.JPEG, 70)//设置磁盘缓存保存格式和质量
 *          //.setDiskCacheInner()//强制使用内部储存
 *          //.setDuplicateLoadEnable(true)//允许相同图片同时加载(慎用)
 *          //.setWipeOnNewVersion()//当APP更新时清空磁盘缓存
 *          //.setLogger(getLogger())
 *          .open();//必须调用
 *   } catch (IOException e) {
 *      //磁盘缓存打开失败的情况, 可提示客户磁盘已满等
 *   }
 * <br/>
 *      [上述代码说明]:<br/>
 *      位图内存缓存占用应用最大可用内存的12.5%,回收站最大可能占用额外的12.5%,
 *      内存缓存能容纳2-3页的图片为宜. 设置过小, 存放不下一页的内容, 图片显示不全,
 *      设置过大, 缓存占用应用可用内存过大, 影响性能或造成OOM. <br/>
 *      在路径/sdcard/Android/data/<application package>/cache/bitmap或
 *      /data/data/<application package>/cache/bitmap下存放磁盘缓存数据,
 *      缓存最大容量50M, 磁盘缓存容量根据实际情况设置, 磁盘缓存加载最大并发量5,
 *      并发量应考虑图片质量/大小, 若图片较大, 应考虑减少并发量, 磁盘缓存等待队列
 *      容量15, 即只会加载最后请求的15个任务, 更早的加载请求会被取消, 等待队列容
 *      量根据屏幕中最多可能展示的图片数决定, 设定值为屏幕最多可能展示图片数的1-
 *      2倍为宜, 设置过少会导致屏幕中图片未全部加载完, 例如屏幕中最多可能展示10
 *      张图片, 则设置15-20较为合适, 若设置了10, 屏幕中会有几张图未加载. <br/>
 *      网络加载并发量为3, 根据网络情况和图片大小决定, 过多的并发量会阻塞网络, 过
 *      少会导致图片加载太慢, 网络加载等待队列容量15, 建议与磁盘缓存等待队列容量
 *      相等, 根据屏幕中最多可能展示的图片数决定(略大于), 设置过少会导致屏幕中图
 *      片未全部加载完.<br/>
 *      设置日志打印器后, BitmapLoader会打印出一些日志用于调试, 例如内存缓存使用
 *      情况, 图片加载日志等, 可根据日志调试/选择上述参数的值.<br/>
 * <br/>
 * <br/>
 * -------------------加载器使用----------------<br/>
 * <br/>
 * 1.load <br/>
 *      加载图片,加载结束后回调OnBitmapLoadedListener<Br/>
 * 2.get <br/>
 *      从内存缓冲获取图片,若不存在返回null<br/>
 * 3.unused [重要] <br/>
 *      不再使用的图片须及时用该方法废弃,尤其是大量图片的场合,未被废弃(unused)的图片
 *      将不会被BitmapLoader回收.请参看"名词解释".<br/>
 *      该方法能取消加载任务,有助于减少不必要的加载,节省流量,使需要显示的图片尽快加载.<br/>
 * 4.destroy [重要] <br/>
 *      清除全部图片及加载任务,通常在Activity.onDestroy中调用<br/>
 * 5.reduce <br/>
 *      强制清空内存缓存中不再使用(unused)的图片.<br/>
 *      用于暂时减少缓存的内存占用,请勿频繁调用.<br/>
 *      通常是内存紧张的场合, 可以在Activity.onStop()中调用, Activity暂时不显示的情况下,
 *      将缓存中已被标记为unused的图片回收掉, 减少内存占用. 但这样会使得重新显示时, 加载
 *      变慢(需要重新加载).<br/>
 * <Br/>
 * -------------------注意事项----------------<br/>
 * <br/>
 * 1.ListView等View复用的场合,应先unused废弃原Bitmap,再设置新的:
 *      holder.imageView.setImageBitmap(null);//置空(或默认图)
 *      String oldUrl = (String) holder.imageView.getTag();//原图的url
 *      if(oldUrl != null)
 *          bitmapLoader.unused(oldUrl);//将原图标识为不再使用,并取消原加载任务
 *      holder.imageView.setTag(newUrl);//记录新图的url,用于下次unused
 *      bitmapLoader.load(newUrl, reqWidth, reqHeight, holder.imageView, mOnBitmapLoadedListener);//加载图片
 * <Br/>
 * ****************************************************************<br/>
 * * * * * 名词解释:<br/>
 * ****************************************************************<br/>
 * <Br/>
 * url:<br/>
 *      BitmapLoader中每个位图资源都由url唯一标识, url在BitmapLoader内部
 *      将由getCacheKey()方法计算为一个cacheKey, 内存缓存/磁盘缓存/队列key都将使用
 *      这个cacheKey标识唯一的资源<br/>
 * <Br/>
 * 缓存区:<br/>
 *      缓存区满后, 会清理最早创建或最少使用的Bitmap. 若被清理的Bitmap已被置为unused不再
 *      使用状态, 则Bitmap会被立刻回收(recycle()), 否则会进入回收站等待被unused. 因此, 必须及时
 *      使用unused(url)方法将不再使用的Bitmap置为unused状态, 使得Bitmap尽快被回收.<br/>
 * <Br/>
 * 回收站:<br/>
 *      用于存放因缓存区满被清理,但仍在被使用的Bitmap(未被标记为unused).<br/>
 *      显示中的Bitmap可能因为被引用(get)早,判定为优先度低而被清理出缓存区,绘制时出现"trying to use a
 *      recycled bitmap"异常,设置合适大小的回收站有助于减少此类事件发生.但回收站的使用会增加内存消耗,
 *      请适度设置.若设置为0禁用,缓存区清理时无视unused状态一律做回收(Bitmap.recycle)处理,且不进入回收站!!<br/>
 *      AsyncBitmapDrawableLoader中禁用.<br/>
 * <br/>
 * ****************************************************************<br/>
 * * * * * 错误处理:<br/>
 * ****************************************************************<br/>
 * <br/>
 * 1.Exception::[BitmapCache]recycler Out Of Memory!!!<br/>
 *      当回收站内存占用超过设定值时, 会触发此异常<Br/>
 *      解决方案:<br/>
 *      1).请合理使用BitmapCache.unused()方法, 将不再使用的Bitmap设置为"不再使用"状态,
 *          Bitmap只有被设置为此状态, 才会被回收(recycle()), 否则在缓存区满后, 会进入回收站,
 *          但并不会释放资源, 这么做是为了防止回收掉正在使用的Bitmap而报错.<br/>
 *      2).设置合理的缓存区及回收站大小, 分配过小可能会导致不够用而报错, 分配过大会使应用
 *          其他占用内存受限.<br/>
 * <br/>
 * 2.当一个页面中需要同时加载相同图片(相同url).<br/>
 *      当同时加载相同图片时,若发现只加载出一个,其余的都被取消(onLoadCanceled).<br/>
 *      解决方案:<br/>
 *      尝试设置setDuplicateLoadEnable(true);<Br/>
 * <Br/>
 * 3.网络加载失败,需要重新加载.<br/>
 *      使用SimpleBitmapLoader/AsyncBitmapDrawableLoader无需特殊处理, 它们自带重新加载功能.<br/>
 *      推荐方案:<br/>
 *      1).在网络加载失败(failed)时, 有限次地重新加载, 不应重新加载unused的任务.<br/>
 * <Br/>
 * 4.Exception::[SimpleBitmapLoaderTask]don't use View.setTag() when view load by SimpleBitmapLoader!!<Br/>
 *      使用SimpleBitmapLoader加载控件时, 控件禁止使用View.setTag()自行设置TAG,
 *      因为SimpleBitmapLoader会把SimpleBitmapLoaderTask通过setTag()绑定在控件上!<Br/>
 * <Br/>
 * <Br/>
 * Created by S.Violet on 2015/7/3.
 */
public class BitmapLoader {

    private CachedBitmapUtils mCachedBitmapUtils;//带缓存的Bitmap工具
    private DiskLruCache mDiskLruCache;//磁盘缓存器

    private TQueue mDiskCacheQueue;//磁盘缓存加载队列
    private TQueue mNetLoadQueue;//网络加载队列

    //SETTINGS//////////////////////////////////////////////

    private WeakReference<Context> context;
    private String diskCacheName;//磁盘缓存名(必须)
    private BitmapLoaderImplementor implementor;//实现器(必须)
    private long diskCacheSize = 1024 * 1024 * 10;//磁盘缓存大小(Mb)
    private float ramCacheSizePercent = 0.125f;//内存缓存大小(占应用可用内存比例)
    private float ramCacheRecyclerSizePercent = 0.125f;//内存缓存回收站大小(占应用可用内存比例)
    private int diskLoadConcurrency = 5;//磁盘加载任务并发量
    private int diskLoadVolume = 10;//磁盘加载等待队列容量
    private int netLoadConcurrency = 3;//网络加载任务并发量
    private int netLoadVolume = 10;//网络加载等待队列容量
    private Bitmap.CompressFormat imageFormat = Bitmap.CompressFormat.JPEG;//缓存图片保存格式
    private int imageQuality = 70;//缓存图片保存质量
    private int keyConflictPolicy = TQueue.KEY_CONFLICT_POLICY_CANCEL;//TQueue同名任务冲突策略
    private int appVersionCode = 1;//应用版本versionCode
    private File cacheDir;//缓存路径
    private WeakReference<Logger> logger;//日志打印器
    private boolean destroyed = false;//是否已被销毁

    /**
     * @param context 上下文
     * @param diskCacheName 磁盘缓存目录名
     * @param implementor 实现器
     */
    public BitmapLoader(Context context, String diskCacheName, BitmapLoaderImplementor implementor) {
        if (implementor == null){
            throw new RuntimeException("[BitmapLoader]implementor is null !!", new NullPointerException());
        }
        if (context == null){
            throw new RuntimeException("[BitmapLoader]context is null !!", new NullPointerException());
        }
        if (diskCacheName == null || "".equals(diskCacheName)){
            throw new RuntimeException("[BitmapLoader]diskCacheName is null !!", new NullPointerException());
        }
        this.context = new WeakReference<Context>(context);
        this.diskCacheName = diskCacheName;
        this.implementor = implementor;
        cacheDir = DirectoryUtils.getCacheDir(context, diskCacheName);
    }

    /************************************************************************************
     * Settings
     */

    /**
     * @param netLoadConcurrency 网络加载任务并发量, 默认3
     * @param netLoadVolume 网络加载等待队列容量, 默认10
     */
    public BitmapLoader setNetLoad(int netLoadConcurrency, int netLoadVolume){
        this.netLoadConcurrency = netLoadConcurrency;
        this.netLoadVolume = netLoadVolume;
        return this;
    }

    /**
     * @param diskCacheSizeMib 磁盘缓存最大容量, 默认10, 单位Mb
     * @param diskLoadConcurrency 磁盘加载任务并发量, 默认5
     * @param diskLoadVolume 磁盘加载等待队列容量, 默认10
     */
    public BitmapLoader setDiskCache(int diskCacheSizeMib, int diskLoadConcurrency, int diskLoadVolume){
        this.diskCacheSize = 1024L * 1024L * diskCacheSizeMib;
        this.diskLoadConcurrency = diskLoadConcurrency;
        this.diskLoadVolume = diskLoadVolume;
        return this;
    }

    /**
     * 缓存区:缓存区满后, 会清理最早创建或最少使用的Bitmap. 若被清理的Bitmap已被置为unused不再
     * 使用状态, 则Bitmap会被立刻回收(recycle()), 否则会进入回收站等待被unused. 因此, 必须及时
     * 使用unused(url)方法将不再使用的Bitmap置为unused状态, 使得Bitmap尽快被回收.
     * <Br/>
     * 回收站:用于存放因缓存区满被清理,但仍在被使用的Bitmap(未被标记为unused).<br/>
     * 显示中的Bitmap可能因为被引用(get)早,判定为优先度低而被清理出缓存区,绘制时出现"trying to use a
     * recycled bitmap"异常,设置合适大小的回收站有助于减少此类事件发生.但回收站的使用会增加内存消耗,
     * 请适度设置.<br/>
     * 若设置为0禁用,缓存区清理时无视unused状态一律做回收(Bitmap.recycle)处理,且不进入回收站!!<br/>
     * <br/>
     * Exception: [BitmapCache]recycler Out Of Memory!!!<br/>
     * 当回收站内存占用超过设定值时, 会触发此异常<Br/>
     * 解决方案:<br/>
     * 1.请合理使用BitmapCache.unused()方法, 将不再使用的Bitmap设置为"不再使用"状态,
     * Bitmap只有被设置为此状态, 才会被回收(recycle()), 否则在缓存区满后, 会进入回收站,
     * 但并不会释放资源, 这么做是为了防止回收掉正在使用的Bitmap而报错.<br/>
     * 2.设置合理的缓存区及回收站大小, 分配过小可能会导致不够用而报错, 分配过大会使应用
     * 其他占用内存受限.<br/>
     *
     * @param ramCacheSizePercent 内存缓存区占用应用可用内存的比例 (0, 1], 默认值0.125f
     * @param ramCacheRecyclerSizePercent 内存缓存回收站占用应用可用内存的比例 [0, 1], 设置为0禁用回收站, 默认值0.125f
     */
    public BitmapLoader setRamCache(float ramCacheSizePercent, float ramCacheRecyclerSizePercent){
        this.ramCacheSizePercent = ramCacheSizePercent;
        this.ramCacheRecyclerSizePercent = ramCacheRecyclerSizePercent;
        return this;
    }

    /**
     * 设置磁盘缓存路径为内部储存<br/>
     * 若不设置, 则优先选择外部储存, 当外部储存不存在时使用内部储存
     */
    public BitmapLoader setDiskCacheInner(){
        cacheDir = new File(DirectoryUtils.getInnerCacheDir(getContext()).getAbsolutePath() + File.separator + diskCacheName);
        return this;
    }

    /**
     * 设置缓存文件的图片保存格式和质量<br/>
     * 默认Bitmap.CompressFormat.JPEG, 70
     *
     * @param format 图片格式
     * @param quality 图片质量 0-100
     */
    public BitmapLoader setImageQuality(Bitmap.CompressFormat format, int quality){
        this.imageFormat = format;
        this.imageQuality = quality;
        return this;
    }

    /**
     * 相同图片同时加载<br/>
     * <br/>
     * ----------------------------------------------<br/>
     * <br/>
     * false:禁用(默认)<Br/>
     * 适用于大多数场合,同一个页面不会出现相同图片(相同的url)的情况.<br/>
     * <br/>
     * 为优化性能,同一张图片并发加载时,采用TQueue的同名任务取消策略,取消多余的并发任务,只保留一个任务完成.
     * 因此,同一个页面同时加载同一张图片时,最终只有一张图片完成加载,其他会被取消.在使用ListView等场合时,
     * 可以避免在频繁滑动时重复执行加载,以优化性能.<br/>
     * <br/>
     * ----------------------------------------------<br/>
     * true:启用<br/>
     * 适用于同一个页面会出现相同图片(相同的url)的场合,性能可能会下降,不适合高并发加载,不适合ListView
     * 等View复用控件的场合.<br/>
     * <br/>
     * 为了满足在一个屏幕中同时显示多张相同图片(相同的url)的情况,在同一张图片并发加载时,采用TQueue的
     * 同名任务跟随策略,其中一个任务执行,其他同名任务等待其完成后,同时回调OnLoadCompleteListener,并传入
     * 同一个结果(Bitmap).这种方式在高并发场合,例如:频繁滑动ListView,任务会持有大量的对象用以回调,而绝大
     * 多数的View已不再显示在屏幕上.<Br/>
     *
     */
    public BitmapLoader setDuplicateLoadEnable(boolean duplicateLoadEnable){
        if (duplicateLoadEnable){
            keyConflictPolicy = TQueue.KEY_CONFLICT_POLICY_FOLLOW;
        }else{
            keyConflictPolicy = TQueue.KEY_CONFLICT_POLICY_CANCEL;
        }
        return this;
    }

    /**
     * 设置App更新时清空磁盘缓存<br/>
     * 默认:不清空缓存<br/>
     * <br/>
     * 当应用versionCode发生变化时, 会清空磁盘缓存. 注意是versionCode, 非versionName.<br/>
     */
    public BitmapLoader setWipeOnNewVersion(){
        this.appVersionCode = ApplicationUtils.getAppVersion(getContext());
        return this;
    }

    /**
     * 设置日志打印器, 用于输出调试日志, 不设置则不输出日志
     */
    public BitmapLoader setLogger(Logger logger) {
        this.logger = new WeakReference<Logger>(logger);
        if (mCachedBitmapUtils != null)
            mCachedBitmapUtils.getBitmapCache().setLogger(logger);
        return this;
    }

    /**
     * [重要]启用BitmapLoader, 在实例化并设置完BitmapLoader后, 必须调用此
     * 方法, 开启磁盘缓存/内存缓存. 否则会抛出异常.<Br/>
     *
     * @throws IOException 磁盘缓存启动失败抛出异常
     */
    public BitmapLoader open() throws IOException {
        this.mDiskLruCache = DiskLruCache.open(cacheDir, appVersionCode, 1, diskCacheSize);
        this.mCachedBitmapUtils = new CachedBitmapUtils(getContext(), ramCacheSizePercent, ramCacheRecyclerSizePercent);
        this.mDiskCacheQueue = new TQueue(true, diskLoadConcurrency)
                .setVolumeMax(diskLoadVolume)
                .waitCancelingTask(true)
                .setKeyConflictPolicy(keyConflictPolicy);
        this.mNetLoadQueue = new TQueue(true, netLoadConcurrency)
                .setVolumeMax(netLoadVolume)
                .waitCancelingTask(true)
                .setKeyConflictPolicy(keyConflictPolicy);
        if(getLogger() != null)
            mCachedBitmapUtils.getBitmapCache().setLogger(getLogger());
        return this;
    }

    /************************************************************************************
     * FUNCTION
     */

    /**
     * 加载图片, 加载成功后回调mOnLoadCompleteListener<br/>
     * 回调方法的params参数为此方法传入的params, 并非Bitmap<Br/>
     * <br/>
     * BitmapLoader中每个位图资源都由url唯一标识, url在BitmapLoader内部
     * 将由getCacheKey()方法计算为一个cacheKey, 内存缓存/磁盘缓存/队列key都将使用
     * 这个cacheKey标识唯一的资源<br/>
     * <Br/>
     * 需求尺寸(reqWidth/reqHeight)参数用于节省内存消耗,请根据界面展示所需尺寸设置(像素px).图片解码时会
     * 根据需求尺寸整数倍缩小,且长宽保持原图比例,解码后的Bitmap尺寸通常不等于需求尺寸.设置为0不缩小图片.<Br/>
     *
     * @param url 图片URL地址
     * @param reqWidth 需求宽度 px
     * @param reqHeight 需求高度 px
     * @param params 参数,会带入mOnLoadCompleteListener回调方法,通常为ImageView,便于设置图片
     * @param mOnBitmapLoadedListener 回调监听器
     */
    public void load(String url, int reqWidth, int reqHeight, Object params, OnBitmapLoadedListener mOnBitmapLoadedListener) {
        if(checkIsOpen())
            return;
        //计算缓存key
        String cacheKey = implementor.getCacheKey(url);
        if (getLogger() != null) {
            getLogger().d("[BitmapLoader]load:start:  url<" + url + "> cacheKey<" + cacheKey + ">");
        }
        //尝试内存缓存中取Bitmap
        Bitmap bitmap = mCachedBitmapUtils.getBitmap(cacheKey);
        if (bitmap != null && !bitmap.isRecycled()) {
            //缓存中存在直接回调:成功
            mOnBitmapLoadedListener.onLoadSucceed(url, reqWidth, reqHeight, params, bitmap);
            if (getLogger() != null) {
                getLogger().d("[BitmapLoader]load:succeed:  from:BitmapCache url<" + url + "> cacheKey<" + cacheKey + ">");
            }
            return;
        }
        //若缓存中不存在, 加入磁盘缓存加载队列
        mDiskCacheQueue.put(cacheKey, new DiskCacheTask(url, reqWidth, reqHeight, mOnBitmapLoadedListener).setParams(params));
    }

    /**
     * 从内存缓存中取Bitmap, 若不存在或已被回收, 则返回null<br/>
     * <br/>
     * BitmapLoader中每个位图资源都由url唯一标识, url在BitmapLoader内部
     * 将由getCacheKey()方法计算为一个cacheKey, 内存缓存/磁盘缓存/队列key都将使用
     * 这个cacheKey标识唯一的资源<br/>
     *
     * @param url 图片URL地址
     * @return 若不存在或已被回收, 则返回null
     */
    public Bitmap get(String url) {
        if(checkIsOpen())
            return null;
        //计算缓存key
        String cacheKey = implementor.getCacheKey(url);
        //尝试从内存缓存中取Bitmap
        Bitmap bitmap = mCachedBitmapUtils.getBitmap(cacheKey);
        if (bitmap != null && !bitmap.isRecycled()) {
            //若存在且未被回收, 返回Bitmap
            return bitmap;
        }
        //若不存在或已被回收, 返回null
        return null;
    }

    /**
     * [重要]尝试取消加载任务,将指定Bitmap标示为不再使用,利于回收(Bitmap.recycle)<Br/>
     * <br/>
     * 当图片不再显示时,及时unused有助于减少不必要的加载,节省流量,使需要显示的图片尽快加载.
     * 例如:ListView高速滑动时,中间很多项是来不及加载的,也无需显示图片,及时取消加载任务,可
     * 以跳过中间项的加载,使滚动停止后需要显示的项尽快加载出来.<br/>
     * <Br/>
     * 将一个Bitmap标记为不再使用, 缓存中的Bitmap不会被立即回收, 在内存不足时,
     * 会进行缓存清理, 清理时会将最早的被标记为unused的Bitmap.recycle()回收掉.
     * 已进入回收站的Bitmap会被立即回收.<br/>
     * <br/>
     * <br/>
     * URL::<Br/>
     * BitmapLoader中每个位图资源都由url唯一标识, url在BitmapLoader内部
     * 将由getCacheKey()方法计算为一个cacheKey, 内存缓存/磁盘缓存/队列key都将使用
     * 这个cacheKey标识唯一的资源<br/>
     *
     * @param url 图片URL地址
     */
    public void unused(String url) {
        if(checkIsOpen())
            return;
        //计算缓存key
        String cacheKey = implementor.getCacheKey(url);
        //网络加载队列取消
        mNetLoadQueue.cancel(cacheKey);
        //磁盘缓存加载队列取消
        mDiskCacheQueue.cancel(cacheKey);
        //将位图标识为不再使用
        mCachedBitmapUtils.unused(cacheKey);
        if (getLogger() != null) {
            getLogger().d("[BitmapLoader]unused:  url<" + url + "> cacheKey<" + cacheKey + ">");
        }
    }

    /**
     * 强制清空内存缓存中不再使用(unused)的图片<br/>
     * <br/>
     * 用于暂时减少缓存的内存占用,请勿频繁调用.<br/>
     * 通常是内存紧张的场合, 可以在Activity.onStop()中调用, Activity暂时不显示的情况下,
     * 将缓存中已被标记为unused的图片回收掉, 减少内存占用. 但这样会使得重新显示时, 加载
     * 变慢(需要重新加载).<br/>
     */
    public void reduce(){
        if (mCachedBitmapUtils != null)
            mCachedBitmapUtils.reduce();
    }

    /**
     * [重要]将所有资源回收销毁, 建议在Activity.onDestroy()时调用该方法
     */
    public void destroy() {

        destroyed = true;

        if (mNetLoadQueue != null) {
            mNetLoadQueue.destroy();
            mNetLoadQueue = null;
        }
        if (mDiskCacheQueue != null) {
            mDiskCacheQueue.destroy();
            mDiskCacheQueue = null;
        }
        if (mDiskLruCache != null) {
            try {
                mDiskLruCache.close();
                mDiskLruCache = null;
            } catch (IOException e) {
                implementor.onException(e);
            }
        }
        if (mCachedBitmapUtils != null) {
            mCachedBitmapUtils.recycleAll();
            mCachedBitmapUtils = null;
        }
        if (implementor != null)
            implementor.onDestroy();
        if (getLogger() != null) {
            getLogger().d("[BitmapLoader]destroy");
        }
    }

    /****************************************************************************
     * static function
     */

    /**
     * [慎用]清除磁盘缓存数据<br/>
     * 若外部储存存在, 则清除外部储存的缓存, 否则清除内部储存的缓存<Br/>
     * <br/>
     * 注意:在该方法调用期间, 若对该磁盘缓存区进行读写操作, 可能会
     * 抛出异常. 请确保调用期间该磁盘缓存区不被使用.
     *
     * @param context context
     * @param diskCacheName 缓存目录名
     * @throws IOException
     */
    public static void wipeDiskCache(Context context, String diskCacheName) throws IOException {
        DiskLruCache.deleteContents(DirectoryUtils.getCacheDir(context, diskCacheName));
    }

    /**
     * [慎用]清除磁盘缓存数据<br/>
     * 强制清除内部储存的缓存<br/>
     * <br/>
     * 注意:在该方法调用期间, 若对该磁盘缓存区进行读写操作, 可能会
     * 抛出异常. 请确保调用期间该磁盘缓存区不被使用.
     *
     * @param context context
     * @param diskCacheName 缓存目录名
     * @throws IOException
     */
    public static void wipeInnerDiskCache(Context context, String diskCacheName) throws IOException {
        DiskLruCache.deleteContents(new File(DirectoryUtils.getInnerCacheDir(context).getAbsolutePath() + File.separator + diskCacheName));
    }

    /************************************************************************
     * inner
     */

    /**
     * 磁盘缓存加载任务
     */
    class DiskCacheTask extends TTask {

        private static final int RESULT_SUCCEED = 0;
        private static final int RESULT_FAILED = 1;
        private static final int RESULT_CANCELED = 2;
        private static final int RESULT_CONTINUE = 3;

        private String url;
        private int reqWidth;
        private int reqHeight;
        private OnBitmapLoadedListener mOnBitmapLoadedListener;

        public DiskCacheTask(String url, int reqWidth, int reqHeight, OnBitmapLoadedListener mOnBitmapLoadedListener) {
            this.url = url;
            this.reqWidth = reqWidth;
            this.reqHeight = reqHeight;
            this.mOnBitmapLoadedListener = mOnBitmapLoadedListener;
        }

        @Override
        public void onPreExecute(Object params) {

        }

        @Override
        public Object doInBackground(Object params) {
            //异常检查
            if (mDiskLruCache == null || mCachedBitmapUtils == null) {
                implementor.onException(new RuntimeException("[BitmapLoader]cachedBitmapUtils is null"));
                return RESULT_CANCELED;
            }
            //计算缓存key
            String cacheKey = implementor.getCacheKey(url);
            try {
                //得到缓存文件
                File cacheFile = mDiskLruCache.getFile(cacheKey, 0);
                if (cacheFile != null) {
                    //若缓存文件存在, 从缓存中加载Bitmap
                    mCachedBitmapUtils.decodeFromFile(cacheKey, cacheFile.getAbsolutePath(), reqWidth, reqHeight);
                    //若此时任务已被取消, 则废弃位图
                    if (isCancel()){
                        mCachedBitmapUtils.unused(cacheKey);
                        return RESULT_CANCELED;
                    }
                    return RESULT_SUCCEED;
                } else {
                    return RESULT_CONTINUE;
                }
            } catch (Exception e) {
                implementor.onException(e);
            }
            return RESULT_FAILED;
        }

        @Override
        public void onPostExecute(Object result, boolean isCancel) {
            String cacheKey = implementor.getCacheKey(url);
            //若任务被取消
            if (isCancel) {
                if (mOnBitmapLoadedListener != null)
                    mOnBitmapLoadedListener.onLoadCanceled(url, reqWidth, reqHeight, getParams());
                if (mCachedBitmapUtils != null)
                    mCachedBitmapUtils.unused(cacheKey);
                if (getLogger() != null) {
                    getLogger().d("[BitmapLoader]load:canceled:  from:DiskCache url<" + url + "> cacheKey<" + cacheKey + ">");
                }
                return;
            }
            switch ((int) result) {
                case RESULT_SUCCEED:
                    if (mOnBitmapLoadedListener != null && mCachedBitmapUtils != null)
                        mOnBitmapLoadedListener.onLoadSucceed(url, reqWidth, reqHeight, getParams(), mCachedBitmapUtils.getBitmap(cacheKey));
                    if (getLogger() != null) {
                        getLogger().d("[BitmapLoader]load:succeed:  from:DiskCache url<" + url + "> cacheKey<" + cacheKey + ">");
                    }
                    break;
                case RESULT_FAILED:
                    if (mOnBitmapLoadedListener != null)
                        mOnBitmapLoadedListener.onLoadFailed(url, reqWidth, reqHeight, getParams());
                    if (mCachedBitmapUtils != null)
                        mCachedBitmapUtils.unused(cacheKey);
                    if (getLogger() != null) {
                        getLogger().d("[BitmapLoader]load:failed:  from:DiskCache url<" + url + "> cacheKey<" + cacheKey + ">");
                    }
                    break;
                case RESULT_CANCELED:
                    if (mOnBitmapLoadedListener != null)
                        mOnBitmapLoadedListener.onLoadCanceled(url, reqWidth, reqHeight, getParams());
                    if (mCachedBitmapUtils != null)
                        mCachedBitmapUtils.unused(cacheKey);
                    if (getLogger() != null) {
                        getLogger().d("[BitmapLoader]load:canceled:  from:DiskCache url<" + url + "> cacheKey<" + cacheKey + ">");
                    }
                    break;
                case RESULT_CONTINUE:
                    //若缓存文件不存在, 加入网络加载队列
                    mNetLoadQueue.put(cacheKey, new NetLoadTask(url, reqWidth, reqHeight, mOnBitmapLoadedListener).setParams(getParams()));
                default:
                    break;
            }
        }
    }

    /**
     * 网络加载任务
     */
    class NetLoadTask extends TTask {

        private static final int RESULT_SUCCEED = 0;
        private static final int RESULT_FAILED = 1;
        private static final int RESULT_CANCELED = 2;
        private static final int RESULT_CONTINUE = 3;

        private String url;
        private int reqWidth;
        private int reqHeight;
        private OnBitmapLoadedListener mOnBitmapLoadedListener;
        private BitmapLoaderMessenger messenger;

        public NetLoadTask(String url, int reqWidth, int reqHeight, OnBitmapLoadedListener mOnBitmapLoadedListener) {
            this.url = url;
            this.reqWidth = reqWidth;
            this.reqHeight = reqHeight;
            this.mOnBitmapLoadedListener = mOnBitmapLoadedListener;
        }

        @Override
        public void onPreExecute(Object params) {

        }

        @Override
        public Object doInBackground(Object params) {
            //检查异常
            if (mDiskLruCache == null || mCachedBitmapUtils == null) {
                implementor.onException(new RuntimeException("[BitmapLoader]cachedBitmapUtils is null"));
                return RESULT_CANCELED;
            }
            //计算缓存key
            String cacheKey = implementor.getCacheKey(url);
            OutputStream outputStream = null;
            DiskLruCache.Editor editor;
            try {
                //打开缓存编辑对象
                editor = mDiskLruCache.edit(cacheKey);
                //若同时Edit一个缓存文件时, 会返回null, 取消任务
                if (editor == null) {
                    return RESULT_CANCELED;
                }
                //获得输出流, 用于写入缓存
                outputStream = editor.newOutputStream(0);
                //结果容器
                messenger = new BitmapLoaderMessenger();
                //从网络加载Bitmap
                implementor.loadFromNet(url, reqWidth, reqHeight, messenger);
                //阻塞等待并获取结果Bitmap
                int result = messenger.getResult();
                if (result == BitmapLoaderMessenger.RESULT_SUCCEED && messenger.getBitmap() != null && !messenger.getBitmap().isRecycled()){
                    //结果为加载成功,且Bitmap正常的情况
                    //写入文件缓存即使失败也不影响返回Bitmap
                    try {
                        //把图片写入缓存
                        BitmapUtils.syncSaveBitmap(messenger.getBitmap(), outputStream, imageFormat, imageQuality, false, null);
                        //尝试flush输出流
                        try {
                            if (outputStream != null)
                                outputStream.flush();
                        } catch (Exception ignored) {
                        }
                        //写入缓存成功commit
                        editor.commit();
                        //写缓存日志
                        mDiskLruCache.flush();
                    }catch(Exception e){
                        implementor.onCacheWriteException(e);
                    }
                    //若加载任务尚未被取消
                    if (!isCancel()) {
                        //加入内存缓存
                        mCachedBitmapUtils.cacheBitmap(cacheKey, messenger.getBitmap());
                        return RESULT_SUCCEED;
                    }
                    //若任务被取消, 则回收加载出的bitmap
                    if (messenger.getBitmap() != null && !messenger.getBitmap().isRecycled()){
                        messenger.getBitmap().recycle();
                    }
                    //若加载任务被取消,即使返回结果是加载成功,也只会存入缓存,不会返回成功的结果
                    return RESULT_CANCELED;
                } else {
                    try {
                        //若加载失败/取消
                        //写入缓存失败abort
                        editor.abort();
                        //写缓存日志
                        mDiskLruCache.flush();
                    }catch (Exception ignored){
                    }
                    //加载取消结果返回
                    if (result == BitmapLoaderMessenger.RESULT_CANCELED)
                        return RESULT_CANCELED;
                    //异常处理
                    if (messenger.getThrowable() != null){
                        implementor.onException(messenger.getThrowable());
                    }
                }
            } catch (Exception e) {
                implementor.onException(e);
            }finally {
                try {
                    if (outputStream != null)
                        outputStream.close();
                } catch (Exception ignored) {
                }
            }
            //加载失败
            return RESULT_FAILED;
        }

        @Override
        public void onPostExecute(Object result, boolean isCancel) {
            String cacheKey = implementor.getCacheKey(url);
            //若任务被取消
            if (isCancel) {
                if (mOnBitmapLoadedListener != null)
                    mOnBitmapLoadedListener.onLoadCanceled(url, reqWidth, reqHeight, getParams());
                if (mCachedBitmapUtils != null)
                    mCachedBitmapUtils.unused(cacheKey);
                if (getLogger() != null) {
                    getLogger().d("[BitmapLoader]load:canceled:  from:NetLoad url<" + url + "> cacheKey<" + cacheKey + ">");
                }
                return;
            }
            switch ((int) result) {
                case RESULT_SUCCEED:
                    if (mOnBitmapLoadedListener != null && mCachedBitmapUtils != null)
                        mOnBitmapLoadedListener.onLoadSucceed(url, reqWidth, reqHeight, getParams(), mCachedBitmapUtils.getBitmap(cacheKey));
                    if (getLogger() != null) {
                        getLogger().d("[BitmapLoader]load:succeed:  from:NetLoad url<" + url + "> cacheKey<" + cacheKey + ">");
                    }
                    break;
                case RESULT_FAILED:
                    if (mOnBitmapLoadedListener != null)
                        mOnBitmapLoadedListener.onLoadFailed(url, reqWidth, reqHeight, getParams());
                    if (mCachedBitmapUtils != null)
                        mCachedBitmapUtils.unused(cacheKey);
                    if (getLogger() != null) {
                        getLogger().d("[BitmapLoader]load:failed:  from:NetLoad url<" + url + "> cacheKey<" + cacheKey + ">");
                    }
                    break;
                case RESULT_CANCELED:
                    if (mOnBitmapLoadedListener != null)
                        mOnBitmapLoadedListener.onLoadCanceled(url, reqWidth, reqHeight, getParams());
                    if (mCachedBitmapUtils != null)
                        mCachedBitmapUtils.unused(cacheKey);
                    if (getLogger() != null) {
                        getLogger().d("[BitmapLoader]load:canceled:  from:NetLoad url<" + url + "> cacheKey<" + cacheKey + ">");
                    }
                    break;
                default:
                    break;
            }
        }

        /**
         * 当任务被取消时
         */
        @Override
        public void onCancel() {
            super.onCancel();
            if (messenger != null)
                messenger.cancel();
//            if (messenger != null)
//                messenger.interrupt();
        }

        /**
         * 销毁
         */
        @Override
        protected void onDestroy() {
            super.onDestroy();

            if (messenger != null)
                messenger.destroy();

            this.mOnBitmapLoadedListener = null;
            this.messenger = null;
        }
    }

    /**
     * 检查BitmapLoader是否open(), 若未open()则抛出异常, 若已被销毁则返回true<br/>
     * 遇到此异常, 请检查代码, BitmapLoader实例化/设置后必须调用open()方法启动.
     */
    boolean checkIsOpen(){
        if (destroyed){
            if (getLogger() != null)
                getLogger().e("[BitmapLoader]can't use BitmapLoader after destroy");
            return true;
        }
        if (mDiskLruCache == null || mCachedBitmapUtils == null || mDiskCacheQueue == null || mNetLoadQueue == null){
            throw new RuntimeException("[BitmapLoader]can't use BitmapLoader without BitmapLoader.open()!!!");
        }
        return false;
    }

    private Context getContext(){
        if (context != null){
            return context.get();
        }
        return null;
    }

    /**
     * 获得其中的日志打印器
     * @return
     */
    public Logger getLogger(){
        if (logger != null)
            return logger.get();
        return null;
    }

}