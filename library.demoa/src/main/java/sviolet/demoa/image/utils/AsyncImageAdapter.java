package sviolet.demoa.image.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import sviolet.demoa.R;
import sviolet.turquoise.enhance.TActivity;
import sviolet.turquoise.utils.bitmap.loader.AsyncBitmapLoader;
import sviolet.turquoise.utils.bitmap.BitmapUtils;
import sviolet.turquoise.utils.bitmap.CachedBitmapUtils;
import sviolet.turquoise.utils.bitmap.loader.OnBitmapLoadedListener;
import sviolet.turquoise.utils.sys.MeasureUtils;
import sviolet.turquoise.view.GradualImageView;

/**
 * ListView适配器
 * Created by S.Violet on 2015/7/7.
 */
public class AsyncImageAdapter extends BaseAdapter {

    private static final String DEFAULT_BITMAP_KEY = "default_bitmap";

    private Context context;
    private List<AsyncImageItem> itemList;
    private AsyncBitmapLoader asyncBitmapLoader;
    private Drawable defaultBitmapDrawableLarge, defaultBitmapDrawableSmall;
    private int widthHeightLarge, widthHeightSmall;

    /**
     * @param context context
     * @param itemList 数据
     * @param asyncBitmapLoader 用于图片动态加载缓存
     * @param cachedBitmapUtils 用于解码默认图(TActivity.getCachedBitmapUtils())
     */
    public AsyncImageAdapter(Context context, List<AsyncImageItem> itemList, AsyncBitmapLoader asyncBitmapLoader, CachedBitmapUtils cachedBitmapUtils){
        this.context = context;
        this.itemList = itemList;
        this.asyncBitmapLoader = asyncBitmapLoader;

        //用CachedBitmapUtils解码的默认图, 会缓存在其内建BtimapCache中, 在TActivity.onDestroy()时会回收资源
        cachedBitmapUtils.decodeFromResource(DEFAULT_BITMAP_KEY, context.getResources(), R.mipmap.async_image_null);
        //大小尺寸的默认背景图
        defaultBitmapDrawableLarge = BitmapUtils.bitmapToDrawable(cachedBitmapUtils.getBitmap(DEFAULT_BITMAP_KEY));
        defaultBitmapDrawableSmall = BitmapUtils.bitmapToDrawable(cachedBitmapUtils.getBitmap(DEFAULT_BITMAP_KEY));
        //图片大小尺寸的长宽值
        widthHeightLarge = MeasureUtils.dp2px(context, 160);//160dp*160dp
        widthHeightSmall = MeasureUtils.dp2px(context, 80);//80dp*80dp
    }

    @Override
    public int getCount() {
        return itemList.size();
    }

    @Override
    public Object getItem(int position) {
        return itemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder;
        if (view == null){
            view = View.inflate(context, R.layout.image_async_item, null);
            holder = new ViewHolder();
            holder.imageView[0] = (GradualImageView) view.findViewById(R.id.image_async_item_imageview0);
            holder.imageView[1] = (GradualImageView) view.findViewById(R.id.image_async_item_imageview1);
            holder.imageView[2] = (GradualImageView) view.findViewById(R.id.image_async_item_imageview2);
            holder.imageView[3] = (GradualImageView) view.findViewById(R.id.image_async_item_imageview3);
            holder.imageView[4] = (GradualImageView) view.findViewById(R.id.image_async_item_imageview4);
            holder.titleTextView = (TextView) view.findViewById(R.id.image_async_item_title);
            holder.contentTextView = (TextView) view.findViewById(R.id.image_async_item_content);
            view.setTag(holder);
        }else{
            holder = (ViewHolder) view.getTag();
            for (int i = 0 ; i < 5 ; i++){
                //去除ImageView中原有图片
                holder.imageView[i].setImageBitmapImmediate(null);
                //将之前的位图资源置为unused状态以便回收资源 [重要]
                String url = (String) holder.imageView[i].getTag();
                asyncBitmapLoader.unused(url);
            }
        }
        AsyncImageItem item = itemList.get(position);
        holder.titleTextView.setText(item.getTitle());
        holder.contentTextView.setText(item.getContent());

        Bitmap bitmap;//图片

        for (int i = 0 ; i < 5 ; i++) {
            //将url存入imageView的TAG中, 来标识当前的图片[重要]
            holder.imageView[i].setTag(item.getUrl(i));
            //从内存缓存中取位图
            bitmap = asyncBitmapLoader.get(item.getUrl(i));
            if (bitmap != null && !bitmap.isRecycled()) {
                //若内存缓存中存在, 则直接设置图片
                holder.imageView[i].setImageBitmapImmediate(bitmap);
                //去除默认背景图(防OverDraw)
                holder.imageView[i].setBackgroundColor(Color.TRANSPARENT);
            }else {
                //若内存缓存中不存在, 交由BitmapLoader.load异步加载
                //第一张图为160*160dp, 其余80*80dp
                if (i == 0){
                    //设置默认背景图(大)
                    holder.imageView[i].setBackgroundDrawable(defaultBitmapDrawableLarge);
                    //异步加载, BitmapLoader会根据需求尺寸加载合适大小的位图, 以节省内存
                    //将ImageView作为参数传入, 便于在回调函数中设置图片
                    asyncBitmapLoader.load(item.getUrl(i), widthHeightLarge, widthHeightLarge, holder.imageView[i], mOnBitmapLoadedListener);
                }else{
                    //设置默认背景图(小)
                    holder.imageView[i].setBackgroundDrawable(defaultBitmapDrawableSmall);
                    //异步加载, BitmapLoader会根据需求尺寸加载合适大小的位图, 以节省内存
                    //将ImageView作为参数传入, 便于在回调函数中设置图片
                    asyncBitmapLoader.load(item.getUrl(i), widthHeightSmall, widthHeightSmall, holder.imageView[i], mOnBitmapLoadedListener);
                }
            }
        }
        return view;
    }

    /**
     * 图片异步加载回调
     */
    private OnBitmapLoadedListener mOnBitmapLoadedListener = new OnBitmapLoadedListener() {
        @Override
        public void onLoadSucceed(String url, Object params, Bitmap bitmap) {
            //参数为load传入的ImageView
            GradualImageView imageView = ((GradualImageView) params);
            //从imageView中获取当前应该显示图片的url, 高并发场合需要
            String currentUrl = (String)imageView.getTag();
            /**
             * 高并发场合需要<br/>
             * ListView中的View是复用的, 一个图片加载任务完成时, 同一个ImageView可能已经需要显示其他图片
             * 了, 因此判断url是否相符, 若不相符则直接return
             */
            if (url != null && !url.equals(currentUrl)){
                ((TActivity) context).getLogger().e("[AsyncImageAdapter]加载的Bitmap与应该显示的图片不符:" + url);
                return;
            }

            if (bitmap != null && !bitmap.isRecycled()) {
                //若图片存在且未被回收, 设置图片(渐渐显示)
                imageView.setImageBitmapGradual(bitmap);
                //去除默认背景图(防OverDraw)
                imageView.setBackgroundColor(Color.TRANSPARENT);
            } else {
                //若图片不存在, 可以考虑重新发起加载请求
                //loader.load(url, widthHeight, widthHeight, params, mOnLoadCompleteListener);
                //此Demo不做重发
                if (context instanceof TActivity) {
                    ((TActivity) context).getLogger().e("[AsyncImageAdapter]加载成功后找不到位图url:" + url);
                }
                Toast.makeText(context, "[AsyncImageAdapter]加载成功后找不到位图url:" + url, Toast.LENGTH_SHORT).show();
            }
        }
        @Override
        public void onLoadFailed(String url, Object params) {
            //加载失败处理
        }
        @Override
        public void onLoadCanceled(String url, Object params) {
            //加载取消处理
        }
    };

    private class ViewHolder{
        TextView titleTextView;
        TextView contentTextView;
        GradualImageView[] imageView = new GradualImageView[5];
    }

}
