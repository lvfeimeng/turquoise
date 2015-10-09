package sviolet.demoa.image;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ListView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import sviolet.demoa.R;
import sviolet.demoa.common.DemoDescription;
import sviolet.demoa.image.utils.AsyncImageAdapter;
import sviolet.demoa.image.utils.AsyncImageItem;
import sviolet.demoa.image.utils.MyBitmapLoaderImplementor;
import sviolet.turquoise.enhance.annotation.setting.ActivitySettings;
import sviolet.turquoise.enhance.annotation.inject.ResourceId;
import sviolet.turquoise.enhance.TActivity;
import sviolet.turquoise.utils.bitmap.loader.AsyncBitmapLoader;

@DemoDescription(
        title = "AsyncImageList",
        type = "Image",
        info = "an Async. Image ListView powered by Common AsyncBitmapLoader"
)

/**
 * 图片动态加载Demo<br/>
 * 内存/磁盘双缓存<br/>
 * 采用AsyncBitmapLoader实现, 启用缓存回收站<br/>
 *
 * Created by S.Violet on 2015/7/7.
 */
@ResourceId(R.layout.image_async)
@ActivitySettings(
        statusBarColor = 0xFF209090,
        navigationBarColor = 0xFF209090
)
public class AsyncImageActivity extends TActivity {

    @ResourceId(R.id.image_async_listview)
    private ListView listView;

    private AsyncImageAdapter adapter;

    private AsyncBitmapLoader mAsyncBitmapLoader;//图片加载器

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            /*
            由于该Demo图片密度大, 最多同时出现四个Item, 每个Item5张图, 因此
            设置等待队列容量为25, 一般的情况下, 设置默认值10足够, 该值设置过大
            会导致快速滑动时, 下载更多的图, 增加流量消耗.
            */
            //初始化图片加载器
            mAsyncBitmapLoader = new AsyncBitmapLoader(this, "AsyncImageActivity", new MyBitmapLoaderImplementor(this))
                    .setRamCache(0.1f, 0.1f)//缓存和回收站各占10%内存
                    .setDiskCache(50, 5, 25)//磁盘缓存50M, 5线程磁盘加载, 等待队列容量25
                    .setNetLoad(3, 25)//3线程网络加载, 等待队列容量25
                    .setDiskCacheInner()//强制使用内部储存
                    .setImageQuality(Bitmap.CompressFormat.JPEG, 70)//设置保存格式和质量
//                    .setImageQuality(Bitmap.CompressFormat.PNG, 70)//设置保存格式和质量(透明图需要PNG)
//                    .setLogger(getLogger())//打印日志
                    .open();//启动(必须)

            //设置适配器, 传入图片加载器, 图片解码工具
            adapter = new AsyncImageAdapter(this, makeItemList(), mAsyncBitmapLoader, getCachedBitmapUtils());
            listView.setAdapter(adapter);
        } catch (IOException e) {
            //磁盘缓存打开失败的情况, 可提示客户磁盘已满等
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //销毁图片加载器(回收位图占用内存)
        mAsyncBitmapLoader.destroy();
    }

    /****************************************************
     * 定时刷新
     * 解决网络加载失败,实现重新加载
     * 通过刷新UI方式触发图片重新加载, 在静止时只会重新加载显示中的图片
     */

    @Override
    protected void onResume() {
        super.onResume();
        handler.sendEmptyMessageDelayed(HANDLER_REFRESH, 2000L);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeMessages(HANDLER_REFRESH);
    }

    private static final int HANDLER_REFRESH = 0;

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case HANDLER_REFRESH:
                    adapter.notifyDataSetChanged();//刷新UI, 触发图片重新加载
                    handler.sendEmptyMessageDelayed(HANDLER_REFRESH, 2000L);
                    break;
                default:
                    break;
            }
            return true;
        }
    });

    /****************************************************
     * 模拟数据生成
     */

    private List<AsyncImageItem> makeItemList(){
        List<AsyncImageItem> list = new ArrayList<>();
        for (int i = 0 ; i < 100 ; i++){
            list.add(makeItem(i));
        }
        return list;
    }

    private AsyncImageItem makeItem(int id){
        AsyncImageItem item = new AsyncImageItem();
        for (int i = 0 ; i < 5 ; i++) {
            item.setUrl(i, "http://a/" + String.valueOf(id) + "-" + String.valueOf(i));
        }
        item.setTitle("Title of AsyncImageList " + String.valueOf(id));
        item.setContent("Content of asyncImagelist content of asyncimagelist content of asyncImagelist " + String.valueOf(id));
        return item;
    }
}
