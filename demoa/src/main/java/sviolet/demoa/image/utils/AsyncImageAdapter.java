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
import sviolet.turquoise.enhance.app.TActivity;
import sviolet.turquoise.util.common.BitmapUtils;
import sviolet.turquoise.util.common.CachedBitmapUtils;
import sviolet.turquoise.modelx.bitmaploader.BitmapLoader;
import sviolet.turquoise.modelx.bitmaploader.entity.BitmapRequest;
import sviolet.turquoise.modelx.bitmaploader.listener.OnBitmapLoadedListener;
import sviolet.turquoise.enhance.common.ViewHolder;
import sviolet.turquoise.utilx.tlogger.TLogger;
import sviolet.turquoise.util.droid.MeasureUtils;
import sviolet.turquoise.ui.view.GradualImageView;

/**
 * ListView适配器
 * Created by S.Violet on 2015/7/7.
 */
public class AsyncImageAdapter extends BaseAdapter {

    private static final String DEFAULT_BITMAP_KEY = "default_bitmap";

    private TLogger logger = TLogger.get(this);

    private Context context;
    private List<AsyncImageItem> itemList;
    private BitmapLoader bitmapLoader;
    private Drawable defaultBitmapDrawableLarge, defaultBitmapDrawableSmall;
    private int widthHeightLarge, widthHeightSmall;

    /**
     * @param context context
     * @param itemList 数据
     * @param bitmapLoader 用于图片动态加载缓存
     * @param cachedBitmapUtils 用于解码默认图(TActivity.getCachedBitmapUtils())
     */
    public AsyncImageAdapter(Context context, List<AsyncImageItem> itemList, BitmapLoader bitmapLoader, CachedBitmapUtils cachedBitmapUtils){
        this.context = context;
        this.itemList = itemList;
        this.bitmapLoader = bitmapLoader;

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

        ViewHolder holder = ViewHolder.create(context, convertView, parent, R.layout.image_async_item);

        GradualImageView[] images = new GradualImageView[5];
        images[0] = (GradualImageView) holder.get(R.id.image_async_item_imageview0);
        images[1] = (GradualImageView) holder.get(R.id.image_async_item_imageview1);
        images[2] = (GradualImageView) holder.get(R.id.image_async_item_imageview2);
        images[3] = (GradualImageView) holder.get(R.id.image_async_item_imageview3);
        images[4] = (GradualImageView) holder.get(R.id.image_async_item_imageview4);

        /*
            当createTimes() > 1时, imageView已加载过图片, 需要进行清理和unused操作
         */

        if (holder.createTimes() > 1){
            for (int i = 0 ; i < 5 ; i++){
                //去除ImageView中原有图片
                images[i].setImageBitmapImmediate(null);
                //取出之前的TaskInfo
                TaskInfo taskInfo = (TaskInfo) images[i].getTag();
                //将之前的位图资源置为unused状态以便回收资源 [重要]
                bitmapLoader.unused(taskInfo.url);
            }
        }

        AsyncImageItem item = itemList.get(position);
        ((TextView) holder.get(R.id.image_async_item_title)).setText(item.getTitle());
        ((TextView) holder.get(R.id.image_async_item_content)).setText(item.getContent());

        Bitmap bitmap;//图片

        for (int i = 0 ; i < 5 ; i++) {
            //将包含url的TaskInfo存入imageView的TAG中, 来标识当前的图片[重要]
            images[i].setTag(new TaskInfo(item.getUrl(i)));
            //从内存缓存中取位图
            bitmap = bitmapLoader.get(item.getUrl(i));
            if (bitmap != null && !bitmap.isRecycled()) {
                //若内存缓存中存在, 则直接设置图片
                images[i].setImageBitmapImmediate(bitmap);
                //去除默认背景图(防OverDraw)
                images[i].setBackgroundColor(Color.TRANSPARENT);
            }else {
                //若内存缓存中不存在, 交由BitmapLoader.load异步加载
                //第一张图为160*160dp, 其余80*80dp
                if (i == 0){
                    //设置默认背景图(大)
                    images[i].setBackgroundDrawable(defaultBitmapDrawableLarge);
                    //异步加载, BitmapLoader会根据需求尺寸加载合适大小的位图, 以节省内存
                    //将ImageView作为参数传入, 便于在回调函数中设置图片
                    bitmapLoader.load(item.getUrl(i), widthHeightLarge, widthHeightLarge, images[i], mOnBitmapLoadedListener);
                }else{
                    //设置默认背景图(小)
                    images[i].setBackgroundDrawable(defaultBitmapDrawableSmall);
                    //异步加载, BitmapLoader会根据需求尺寸加载合适大小的位图, 以节省内存
                    //将ImageView作为参数传入, 便于在回调函数中设置图片
                    bitmapLoader.load(item.getUrl(i), widthHeightSmall, widthHeightSmall, images[i], mOnBitmapLoadedListener);
                }
            }
        }

        return holder.getConvertView();
    }

    /**
     * 图片异步加载回调
     */
    private OnBitmapLoadedListener mOnBitmapLoadedListener = new OnBitmapLoadedListener() {
        @Override
        public void onLoadSucceed(BitmapRequest request, Object params, Bitmap bitmap) {
            //参数为load传入的ImageView
            GradualImageView imageView = ((GradualImageView) params);
            //从imageView中获取当前应该显示图片的url, 高并发场合需要
            TaskInfo taskInfo = (TaskInfo)imageView.getTag();
            /**
             * 高并发场合需要<br/>
             * ListView中的View是复用的, 一个图片加载任务完成时, 同一个ImageView可能已经需要显示其他图片
             * 了, 因此判断url是否相符, 若不相符则直接return
             */
            if (request.getUrl() != null && !request.getUrl().equals(taskInfo.url)){
                logger.e("加载的Bitmap与应该显示的图片不符:" + request.getUrl());
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
                    logger.e("加载成功后找不到位图url:" + request.getUrl());
                }
                Toast.makeText(context, "[AsyncImageAdapter]加载成功后找不到位图url:" + request.getUrl(), Toast.LENGTH_SHORT).show();
            }
        }
        @Override
        public void onLoadFailed(BitmapRequest request, Object params) {
            //参数为load传入的ImageView
            GradualImageView imageView = ((GradualImageView) params);
            TaskInfo taskInfo = (TaskInfo)imageView.getTag();

            //根据TaskInfo记录的重新加载次数, 判断是否重新加载
            if (taskInfo.reloadTimes < TaskInfo.RELOAD_TIMES_MAX){
                taskInfo.reloadTimes++;
                //重新加载
                bitmapLoader.load(request, params, this);
            }
        }
        @Override
        public void onLoadCanceled(BitmapRequest request, Object params) {
            //加载取消处理, 通常不做处理
        }
    };

    /**
     * 加载任务信息
     */
    private class TaskInfo{

        static final int RELOAD_TIMES_MAX = 2;//最大重加载次数

        String url;//加载url
        int reloadTimes;//重加载次数

        TaskInfo(String url){
            this.url = url;
        }
    }

}
