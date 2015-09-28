package sviolet.turquoise.utils.cache;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sviolet.turquoise.compat.CompatLruCache;
import sviolet.turquoise.enhance.utils.Logger;
import sviolet.turquoise.utils.sys.DeviceUtils;

/**
 * Bitmap内存缓存<br/>
 * <br/>
 * 缓存区:缓存区满后, 会清理最早创建或最少使用的Bitmap. 若被清理的Bitmap已被置为unused不再
 * 使用状态, 则Bitmap会被立刻回收(recycle()), 否则会进入回收站等待被unused. 因此, 必须及时
 * 使用unused(key)方法将不再使用的Bitmap置为unused状态, 使得Bitmap尽快被回收.
 * <Br/>
 * 回收站:用于存放因缓存区满被清理,但仍在被使用的Bitmap(未被标记为unused).<br/>
 * 显示中的Bitmap可能因为被引用(get)早,判定为优先度低而被清理出缓存区,绘制时出现"trying to use a
 * recycled bitmap"异常,设置合适大小的回收站有助于减少此类事件发生.但回收站的使用会增加内存消耗,
 * 请适度设置.<br/>
 * 若设置为0禁用,缓存区清理时无视unused状态一律做回收(Bitmap.recycle)处理,且不进入回收站.需要配合
 * SafeBitmapDrawableFactory使用<br/>
 * <br/>
 * Exception: [BitmapCache]recycler Out Of Memory!!!<br/>
 * 当回收站内存占用超过设定值时, 会触发此异常<Br/>
 * 解决方案:<br/>
 * 1.请合理使用BitmapCache.unused()方法, 将不再使用的Bitmap设置为"不再使用"状态,
 * Bitmap只有被设置为此状态, 才会被回收(recycle()), 否则在缓存区满后, 会进入回收站,
 * 但并不会释放资源, 这么做是为了防止回收掉正在使用的Bitmap而报错.<br/>
 * 2.设置合理的缓存区及回收站大小, 分配过小可能会导致不够用而报错, 分配过大会使应用
 * 其他占用内存受限.<br/>
 * <br/>
 * Tips:<br/>
 * 1.使用SafeBitmapDrawableFactory将Bitmap包装为SafeBitmapDrawable设置给ImageView使用,
 * 可有效避免"trying to use a recycled bitmap"异常, SafeBitmapDrawable在原Bitmap被回收
 * 时,会绘制预先设置的默认图.采用这种方式可以将回收站recyclerMaxSize设置为0, 禁用回收站.<Br/>
 */
public class BitmapCache extends CompatLruCache<String, Bitmap> {

    private static final float DEFAULT_CACHE_MEMORY_PERCENT = 0.125f;

    //单线程线程池
    private ExecutorService singleThreadPool;
    //回收站 : 存放被清理出缓存但未被标记为unused的Bitmap
    private HashMap<String, Bitmap> recyclerMap;
    //不再使用标记
    private HashMap<String, Boolean> unusedMap;
    //回收站分配空间
    private int recyclerMaxSize = 0;
    //回收站当前占用
    private int recyclerSize = 0;

    //日志打印器
    private Logger logger;

    /**
     * 创建缓存实例<Br/>
     * 缓存区容量为默认值DEFAULT_CACHE_MEMORY_PERCENT = 0.125f<Br/>
     * 回收站容量为默认值DEFAULT_CACHE_MEMORY_PERCENT = 0.125f<Br/>
     * <br/>
     * 缓存区:缓存区满后, 会清理最早创建或最少使用的Bitmap. 若被清理的Bitmap已被置为unused不再
     * 使用状态, 则Bitmap会被立刻回收(recycle()), 否则会进入回收站等待被unused. 因此, 必须及时
     * 使用unused(key)方法将不再使用的Bitmap置为unused状态, 使得Bitmap尽快被回收.
     * <Br/>
     * 回收站:用于存放因缓存区满被清理,但仍在被使用的Bitmap(未被标记为unused).<br/>
     * 显示中的Bitmap可能因为被引用(get)早,判定为优先度低而被清理出缓存区,绘制时出现"trying to use a
     * recycled bitmap"异常,设置合适大小的回收站有助于减少此类事件发生.但回收站的使用会增加内存消耗,
     * 请适度设置.<br/>
     * 若设置为0禁用,缓存区清理时无视unused状态一律做回收(Bitmap.recycle)处理,且不进入回收站.需要配合
     * SafeBitmapDrawableFactory使用<br/>
     * <br/>
     * Exception: [BitmapCache]recycler Out Of Memory!!!<br/>
     * 当回收站内存占用超过设定值时, 会触发此异常<Br/>
     * 解决方案:<br/>
     * 1.请合理使用BitmapCache.unused()方法, 将不再使用的Bitmap设置为"不再使用"状态,
     * Bitmap只有被设置为此状态, 才会被回收(recycle()), 否则在缓存区满后, 会进入回收站,
     * 但并不会释放资源, 这么做是为了防止回收掉正在使用的Bitmap而报错.<br/>
     * 2.设置合理的缓存区及回收站大小, 分配过小可能会导致不够用而报错, 分配过大会使应用
     * 其他占用内存受限.<br/>
     * <br/>
     * Tips:<br/>
     * 1.使用SafeBitmapDrawableFactory将Bitmap包装为SafeBitmapDrawable设置给ImageView使用,
     * 可有效避免"trying to use a recycled bitmap"异常, SafeBitmapDrawable在原Bitmap被回收
     * 时,会绘制预先设置的默认图.采用这种方式可以将回收站recyclerMaxSize设置为0, 禁用回收站.<Br/>
     *
     * @param context
     * @return
     */
    public static BitmapCache newInstance(Context context) {
        //应用可用内存级别
        final int memoryClass = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
        //计算缓存大小
        final int cacheSize = (int) (1024 * 1024 * memoryClass * DEFAULT_CACHE_MEMORY_PERCENT);
        //实例化
        return new BitmapCache(cacheSize, cacheSize);
    }

    /**
     * 创建缓存实例<Br/>
     * 根据实际情况设置缓存占用应用可用内存的比例, 参考值0.125, 建议不超过0.25<Br/>
     * <Br/>
     * 缓存区:缓存区满后, 会清理最早创建或最少使用的Bitmap. 若被清理的Bitmap已被置为unused不再
     * 使用状态, 则Bitmap会被立刻回收(recycle()), 否则会进入回收站等待被unused. 因此, 必须及时
     * 使用unused(key)方法将不再使用的Bitmap置为unused状态, 使得Bitmap尽快被回收.
     * <Br/>
     * 回收站:用于存放因缓存区满被清理,但仍在被使用的Bitmap(未被标记为unused).<br/>
     * 显示中的Bitmap可能因为被引用(get)早,判定为优先度低而被清理出缓存区,绘制时出现"trying to use a
     * recycled bitmap"异常,设置合适大小的回收站有助于减少此类事件发生.但回收站的使用会增加内存消耗,
     * 请适度设置.<br/>
     * 若设置为0禁用,缓存区清理时无视unused状态一律做回收(Bitmap.recycle)处理,且不进入回收站.需要配合
     * SafeBitmapDrawableFactory使用<br/>
     * <br/>
     * Exception: [BitmapCache]recycler Out Of Memory!!!<br/>
     * 当回收站内存占用超过设定值时, 会触发此异常<Br/>
     * 解决方案:<br/>
     * 1.请合理使用BitmapCache.unused()方法, 将不再使用的Bitmap设置为"不再使用"状态,
     * Bitmap只有被设置为此状态, 才会被回收(recycle()), 否则在缓存区满后, 会进入回收站,
     * 但并不会释放资源, 这么做是为了防止回收掉正在使用的Bitmap而报错.<br/>
     * 2.设置合理的缓存区及回收站大小, 分配过小可能会导致不够用而报错, 分配过大会使应用
     * 其他占用内存受限.<br/>
     * <br/>
     * Tips:<br/>
     * 1.使用SafeBitmapDrawableFactory将Bitmap包装为SafeBitmapDrawable设置给ImageView使用,
     * 可有效避免"trying to use a recycled bitmap"异常, SafeBitmapDrawable在原Bitmap被回收
     * 时,会绘制预先设置的默认图.采用这种方式可以将回收站recyclerMaxSize设置为0, 禁用回收站.<Br/>
     *
     * @param context
     * @param cachePercent Bitmap缓存区占用应用可用内存的比例 (0, 1]
     * @param recyclerPercent Bitmap回收站占用应用可用内存的比例 [0, 1], 使用SafeBitmapDrawableFactory时设置为0禁用回收站
     * @return
     */
    public static BitmapCache newInstance(Context context, float cachePercent, float recyclerPercent) {
        //应用可用内存级别
        final int memoryClass = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
        //计算缓存大小
        final int cacheSize = (int) (1024 * 1024 * memoryClass * cachePercent);
        //计算回收站大小
        final int recyclerSize = (int) (1024 * 1024 * memoryClass * recyclerPercent);
        //实例化
        return new BitmapCache(cacheSize, recyclerSize);
    }

    /**
     * 创建缓存实例<Br/>
     * 根据实际情况设置缓冲区占用最大内存, 建议不超过应用可用内存的1/4<br/>
     * <Br/>
     * 缓存区:缓存区满后, 会清理最早创建或最少使用的Bitmap. 若被清理的Bitmap已被置为unused不再
     * 使用状态, 则Bitmap会被立刻回收(recycle()), 否则会进入回收站等待被unused. 因此, 必须及时
     * 使用unused(key)方法将不再使用的Bitmap置为unused状态, 使得Bitmap尽快被回收.
     * <Br/>
     * 回收站:用于存放因缓存区满被清理,但仍在被使用的Bitmap(未被标记为unused).<br/>
     * 显示中的Bitmap可能因为被引用(get)早,判定为优先度低而被清理出缓存区,绘制时出现"trying to use a
     * recycled bitmap"异常,设置合适大小的回收站有助于减少此类事件发生.但回收站的使用会增加内存消耗,
     * 请适度设置.<br/>
     * 若设置为0禁用,缓存区清理时无视unused状态一律做回收(Bitmap.recycle)处理,且不进入回收站.需要配合
     * SafeBitmapDrawableFactory使用<br/>
     * <br/>
     * Exception: [BitmapCache]recycler Out Of Memory!!!<br/>
     * 当回收站内存占用超过设定值时, 会触发此异常<Br/>
     * 解决方案:<br/>
     * 1.请合理使用BitmapCache.unused()方法, 将不再使用的Bitmap设置为"不再使用"状态,
     * Bitmap只有被设置为此状态, 才会被回收(recycle()), 否则在缓存区满后, 会进入回收站,
     * 但并不会释放资源, 这么做是为了防止回收掉正在使用的Bitmap而报错.<br/>
     * 2.设置合理的缓存区及回收站大小, 分配过小可能会导致不够用而报错, 分配过大会使应用
     * 其他占用内存受限.<br/>
     * <br/>
     * Tips:<br/>
     * 1.使用SafeBitmapDrawableFactory将Bitmap包装为SafeBitmapDrawable设置给ImageView使用,
     * 可有效避免"trying to use a recycled bitmap"异常, SafeBitmapDrawable在原Bitmap被回收
     * 时,会绘制预先设置的默认图.采用这种方式可以将回收站recyclerMaxSize设置为0, 禁用回收站.<Br/>
     *
     * @param cacheMaxSize Bitmap缓存区占用最大内存 单位byte (0, ?)
     * @param recyclerMaxSize Bitmap回收站占用最大内存 单位byte [0, ?), 使用SafeBitmapDrawableFactory时设置为0禁用回收站
     */
    public static BitmapCache newInstance(int cacheMaxSize, int recyclerMaxSize) {
        return new BitmapCache(cacheMaxSize, recyclerMaxSize);
    }

    /**
     * 缓存区:缓存区满后, 会清理最早创建或最少使用的Bitmap. 若被清理的Bitmap已被置为unused不再
     * 使用状态, 则Bitmap会被立刻回收(recycle()), 否则会进入回收站等待被unused. 因此, 必须及时
     * 使用unused(key)方法将不再使用的Bitmap置为unused状态, 使得Bitmap尽快被回收.
     * <Br/>
     * 回收站:用于存放因缓存区满被清理,但仍在被使用的Bitmap(未被标记为unused).<br/>
     * 显示中的Bitmap可能因为被引用(get)早,判定为优先度低而被清理出缓存区,绘制时出现"trying to use a
     * recycled bitmap"异常,设置合适大小的回收站有助于减少此类事件发生.但回收站的使用会增加内存消耗,
     * 请适度设置.<br/>
     * 若设置为0禁用,缓存区清理时无视unused状态一律做回收(Bitmap.recycle)处理,且不进入回收站.需要配合
     * SafeBitmapDrawableFactory使用<br/>
     * <br/>
     * Exception: [BitmapCache]recycler Out Of Memory!!!<br/>
     * 当回收站内存占用超过设定值时, 会触发此异常<Br/>
     * 解决方案:<br/>
     * 1.请合理使用BitmapCache.unused()方法, 将不再使用的Bitmap设置为"不再使用"状态,
     * Bitmap只有被设置为此状态, 才会被回收(recycle()), 否则在缓存区满后, 会进入回收站,
     * 但并不会释放资源, 这么做是为了防止回收掉正在使用的Bitmap而报错.<br/>
     * 2.设置合理的缓存区及回收站大小, 分配过小可能会导致不够用而报错, 分配过大会使应用
     * 其他占用内存受限.<br/>
     * <br/>
     * Tips:<br/>
     * 1.使用SafeBitmapDrawableFactory将Bitmap包装为SafeBitmapDrawable设置给ImageView使用,
     * 可有效避免"trying to use a recycled bitmap"异常, SafeBitmapDrawable在原Bitmap被回收
     * 时,会绘制预先设置的默认图.采用这种方式可以将回收站recyclerMaxSize设置为0, 禁用回收站.<Br/>
     *
     * @param cacheMaxSize Bitmap缓存区占用最大内存 单位byte
     * @param recyclerMaxSize Bitmap回收站占用最大内存 单位byte, 使用SafeBitmapDrawableFactory时设置为0禁用回收站
     */
    private BitmapCache(int cacheMaxSize, int recyclerMaxSize) {
        super(cacheMaxSize);
        this.unusedMap = new HashMap<String, Boolean>();
        //分配回收站空间
        if (recyclerMaxSize > 0) {
            this.recyclerMaxSize = recyclerMaxSize;
            this.recyclerMap = new HashMap<String, Bitmap>();
        }
    }

    /*****************************************************************************
     * function
     */

    /**
     * [重要]<br/>
     * [异步]将一个Bitmap标记为不再使用, 缓存中的Bitmap不会被立即回收, 在内存不足时,
     * 会进行缓存清理, 清理时会将最早的被标记为unused的Bitmap.recycle()回收掉.
     * 已进入回收站的Bitmap会被立即回收.
     *
     * @param key
     */
    public void asyncUnused(final String key){
        execute(new Runnable() {
            @Override
            public void run() {
                unused(key);
            }
        });
    }

    /**
     * [重要]<br/>
     * [同步]将一个Bitmap标记为不再使用, 缓存中的Bitmap不会被立即回收, 在内存不足时,
     * 会进行缓存清理, 清理时会将最早的被标记为unused的Bitmap.recycle()回收掉.
     * 已进入回收站的Bitmap会被立即回收.<br/>
     * 同步操作, 可能会阻塞<br/>
     *
     * @param key
     */
    public void unused(String key) {
        Bitmap bitmap = null;
        synchronized (this) {
            //若Bitmap存在回收站中, 则直接清除回收资源
            if (recyclerMap != null && recyclerMap.containsKey(key)) {
                bitmap = recyclerMap.remove(key);
                recyclerSize -= sizeOf(key, bitmap);
            }
            //置为不再使用状态
            unusedMap.put(key, true);
        }
        //回收资源
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        //打印内存使用情况
        if (logger != null)
            logger.i(getMemoryReport());
    }

    /**
     * 从缓存中取Bitmap<br/>
     * 若该Bitmap已被标记为unused, 则会清除unused标记<Br/>
     * 不会从回收站中取Bitmap<br/>
     *
     * @param key
     * @return
     */
    @Override
    public Bitmap get(String key) {
        //移除不再使用标记
        unusedMap.remove(key);
        //返回Bitmap
        return super.get(key);
    }

    /**
     * 将一个Bitmap放入缓存<Br/>
     * 放入前会强制回收已存在的同名Bitmap(包括缓存和回收站),
     * 不当的使用可能会导致异常 : 回收了正在使用的Bitmap
     *
     * @param key
     * @param value
     * @return
     */
    @Override
    public Bitmap put(String key, Bitmap value) {
        //先强制移除并回收同名Bitmap
        remove(key);
        return super.put(key, value);
    }

    /**
     * 从缓存中移除<br/>
     * 强制回收Bitmap(包括缓存和回收站)并返回null<br/>
     *
     * @param key
     * @return 返回null
     */
    @Override
    public Bitmap remove(String key) {
        Bitmap bitmap;
        Bitmap recyclerBitmap = null;
        synchronized (this) {
            //从缓存中移除
            bitmap = super.remove(key);
            //从回收站移除
            if (recyclerMap != null) {
                recyclerBitmap = recyclerMap.remove(key);
                recyclerSize -= sizeOf(key, recyclerBitmap);
            }
            //移除不再使用标志
            unusedMap.remove(key);
        }
        //回收资源
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        //回收"回收站"中的资源
        if (recyclerBitmap != null && !recyclerBitmap.isRecycled()) {
            recyclerBitmap.recycle();
        }
        //返回空
        return null;
    }

    /**
     * 强制清除并回收所有Bitmap(包括缓存和回收站)
     */
    public void removeAll() {
        int counter = 0;
        //移除缓存中的所有资源
        for (Map.Entry<String, Bitmap> entry : getMap().entrySet()) {
            Bitmap bitmap = entry.getValue();
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
                counter++;
            }
        }
        //移除回收站资源
        if(recyclerMap != null) {
            for (Map.Entry<String, Bitmap> entry : recyclerMap.entrySet()) {
                Bitmap bitmap = entry.getValue();
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                    counter++;
                }
            }
        }
        //清理
        synchronized (this) {
            getMap().clear();
            if(recyclerMap != null)
                recyclerMap.clear();
            unusedMap.clear();
            setSize(0);
            recyclerSize = 0;
        }
        //打印日志
        if (logger != null){
            logger.i("[BitmapCache] recycled:" + counter);
        }
    }

    /**
     * 销毁缓存
     */
    public void destroy(){
        removeAll();
        if (singleThreadPool != null)
            singleThreadPool.shutdown();
    }

    /**
     * 获得回收站占用内存byte
     *
     * @return
     */
    public int recyclerSize() {
        return recyclerSize;
    }

    /**
     * @return 回收站位图数量
     */
    public int recyclerQuantity() {
        if (recyclerMap != null)
            return recyclerMap.size();
        return 0;
    }

    /**
     * 当前缓存使用内存情况<br/>
     * Cache/Recycler max: 缓存和回收站各自的最大容量<br/>
     * Cache used: 缓存使用情况 (pcs Bitmap数)<Br/>
     * Recycler used: 回收站使用情况 (pcs Bitmap数)<Br/>
     */
    public String getMemoryReport() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[BitmapCache]MemoryReport:  ");
        stringBuilder.append("[max]: ");
        stringBuilder.append(maxSize() / (1024 * 1024));
        stringBuilder.append("  [CacheUsed]: ");
        stringBuilder.append(size() / (1024 * 1024));
        stringBuilder.append("m ");
        stringBuilder.append(quantity());
        stringBuilder.append("pcs  ");
        stringBuilder.append("[RecyclerUsed]: ");
        stringBuilder.append(recyclerSize() / (1024 * 1024));
        stringBuilder.append("m ");
        stringBuilder.append(recyclerQuantity());
        stringBuilder.append("pcs");
        return stringBuilder.toString();
    }

    /**
     * 设置日志打印器, 用于调试输出日志<br/>
     *
     * @param logger
     */
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * 用单线程线程池执行任务
     * @param runnable
     */
    private void execute(Runnable runnable){
        if (singleThreadPool == null){
            singleThreadPool = Executors.newSingleThreadExecutor();
        }
        singleThreadPool.execute(runnable);
    }

    /******************************************************
     * override
     */

    @Override
    protected void trimToSize(int maxSize) {
        while (true) {
            String key;
            Bitmap value;
            synchronized (this) {
                if (size() < 0 || (getMap().isEmpty() && size() != 0)) {
                    throw new IllegalStateException(getClass().getName() + ".sizeOf() is reporting inconsistent results!");
                }

                if (size() <= maxSize) {
                    break;
                }

                Map.Entry<String, Bitmap> toEvict = null;

                for (Map.Entry<String, Bitmap> entry : getMap().entrySet()) {
                    toEvict = entry;
                    //LruCache中没有break
                    //原来取最后一项, 即为最新加入或最近操作过的一项
                    //此处改为取第一项, 即为最早加入或最少操作的一项
                    break;
                }

                if (toEvict == null) {
                    break;
                }

                key = toEvict.getKey();
                value = toEvict.getValue();

                setSize(size() - safeSizeOf(key, value));

                //禁用回收站 或 被标记为不再使用 直接回收bitmap
                if (recyclerMap == null || unusedMap.containsKey(key)) {
                    //回收不再使用的Bitmap
                    if (value != null && !value.isRecycled()) {
                        value.recycle();
                    }
                    //清除标记
                    unusedMap.remove(key);
                } else {
                    //加入回收站前清理回收站中的同名资源
                    if (recyclerMap.containsKey(key)) {
                        Bitmap recyclerBitmap = recyclerMap.remove(key);
                        if (recyclerBitmap != null && !recyclerBitmap.isRecycled()) {
                            recyclerSize -= sizeOf(key, recyclerBitmap);
                            recyclerBitmap.recycle();
                        }
                    }
                    //放入回收站
                    recyclerMap.put(key, value);
                    recyclerSize += sizeOf(key, value);
                    /*
                        当回收站内存占用超过设定值时, 会触发此异常,
                        解决方案:
                        1.请合理使用BitmapCache.unused()方法, 将不再使用的Bitmap设置为"不再使用"状态,
                           Bitmap只有被设置为此状态, 才会被回收(recycle()), 否则在缓存区满后, 会进入回收站,
                           但并不会释放资源, 这么做是为了防止回收掉正在使用的Bitmap而报错.
                        2.给BitmapCache设置合理的最大占用内存(或占比), 分配过小可能会导致不够用而报错,
                          分配过大可能使应用其他占用内存受限.
                     */
                    if (recyclerSize > recyclerMaxSize) {
                        throw new RuntimeException("[BitmapCache]recycler Out Of Memory!!! see Notes of BitmapCache");
                    }
                }

                getMap().remove(key);
                setEvictionCount(getEvictionCount() + 1);
            }

            entryRemoved(true, key, value, null);
        }
        //打印内存使用情况
        if (logger != null)
            logger.i(getMemoryReport());
    }

    @SuppressLint("NewApi")
    @Override
    protected int sizeOf(String key, Bitmap value) {
        //资源不存在或被回收返回0
        if (value == null || value.isRecycled())
            return 0;
        //计算图片占内存大小
        if (DeviceUtils.getVersionSDK() >= 12) {
            return value.getByteCount();
        }
        return value.getRowBytes() * value.getHeight();
    }

}