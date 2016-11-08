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

package sviolet.turquoise.ui.viewgroup.gesture;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import java.util.ArrayList;
import java.util.List;

import sviolet.turquoise.common.compat.CompatOverScroller;
import sviolet.turquoise.ui.util.ListViewUtils;
import sviolet.turquoise.ui.util.ScrollViewUtils;
import sviolet.turquoise.util.droid.MotionEventUtils;

/**
 * <p>垂直方向越界拖动容器(RelativeLayout), 可用于实现下拉刷新上拉加载</p>
 *
 * <p>
 *     支持的子控件:<br/>
 *     1.ScrollView<br/>
 *     2.ListView<br/>
 * </p>
 *
 * <p>
 *     将VerticalOverDragContainer作为父控件, 将需要越界拖动的ScrollView/ListView等作为子控件放置在其内部,
 *     原则上, VerticalOverDragContainer只能容纳一个子控件, 如果必须要容纳多个控件, 请务必将ScrollView/ListView等
 *     自身会滚动的控件放置在最后一个, VerticalOverDragContainer会根据最后一个子控件是否到达顶部/底部来判断
 *     是否要发生越界滚动.
 * </p>
 *
 * <pre>{@code
 *      <sviolet.turquoise.ui.viewgroup.gesture.VerticalOverDragContainer
 *          android:layout_width="match_parent"
 *          android:layout_height="match_parent">
 *
 *          ...
 *
 *          <ScrollView
 *              android:layout_width="match_parent"
 *              android:layout_height="match_parent">
 *
 *              ...
 *
 *          </ScrollView>
 *      </sviolet.turquoise.ui.viewgroup.gesture.VerticalOverDragContainer>
 * }</pre>
 *
 * Created by S.Violet on 2016/11/3.
 */
public class VerticalOverDragContainer extends RelativeLayout {

    //状态
    private static final int STATE_RELEASE = 0;
    private static final int STATE_HOLD = 1;
    private static final int STATE_TOP_OVER_DRAG = 2;
    private static final int STATE_BOTTOM_OVER_DRAG = 3;
    private static final int STATE_HORIZONTAL_DRAG = 4;

    //手势方向
    private static final int DRAG_DIRECTION_HORIZONTAL = -1;
    private static final int DRAG_DIRECTION_UNKNOWN = 0;
    private static final int DRAG_DIRECTION_VERTICAL = 1;

    //////////////////////////////////////////////////////

    //true:当出现水平方向的手势时, 禁用越界拖动
    private boolean disableIfHorizontalDrag = true;

    //越界拖动界限, 超过该界限则进入PARK状态(停止在界限上, 用于实现下拉刷新上拉加载)
    private int overDragThreshold = 300;
    //越界拖动阻尼, 0-1, 值越小拖动越慢
    private float overDragResistance = 0.4f;

    //scroller的回弹时间
    private int scrollDuration = 300;

    //顶部PARK允许
    private boolean topParkEnabled = true;
    //底部PARK允许
    private boolean bottomParkEnabled = true;

    //监听器
    private OnOverDragStateChangeListener onOverDragStateChangeListener;
    private OnOverDragScrollListener onOverDragScrollListener;
    private OnOverDragParkListener onOverDragParkListener;
    private List<RefreshView> refreshViewList;

    //////////////////////////////////////////////////////

    private int mTouchSlop;

    private int state = STATE_RELEASE;
    private int dragDirection = DRAG_DIRECTION_UNKNOWN;

    //当前位置
    private float scrollY;

    //按下时的坐标
    private float downX;
    private float downY;
    //上一次的Y值
    private float lastY;
    //上一次触点ID
    private int lastPointId = -1;

    private boolean topParked = false;
    private boolean bottomParked = false;

    private CompatOverScroller scroller;

    private MotionEventUtils.TouchPoints touchPoints = new MotionEventUtils.TouchPoints();

    public VerticalOverDragContainer(Context context) {
        super(context);
        //初始化
        init();
    }

    public VerticalOverDragContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        //初始化
        init();
    }

    public VerticalOverDragContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //初始化
        init();
    }

    /**
     * 初始化
     */
    private void init() {
        this.mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        this.scroller = new CompatOverScroller(getContext());
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        //没有子控件时直接分发事件
        int childCount = getChildCount();
        if (childCount < 1) {
            return super.dispatchTouchEvent(ev);
        }

        //取滚动的子控件
        View child = getScrollChild();
        //子控件是否在顶部或底部
        ReachState reachState = checkReachState(child);

        //当前坐标
        float currX = ev.getX();
        float currY = ev.getY();

        //Y方向位移量
        float distanceY = calculateMoveDistance(ev);

        //ACTION_CANCEL
        boolean isCancel = false;

        switch (ev.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                //重置状态
                resetState();
                //记录坐标
                this.downX = currX;
                this.downY = currY;
                this.lastY = currY;

                if (scrollY == 0) {
                    //没有越界, 普通持有模式
                    stateToHold();
                    //事件分发给子控件处理
                    return super.dispatchTouchEvent(ev);
                } else if (scrollY > 0){
                    //上方越界
                    stateToTopOverDrag();
                    return true;
                } else {
                    //下方越界
                    stateToBottomOverDrag();
                    return true;
                }
            case MotionEvent.ACTION_POINTER_DOWN:
                if (this.state == STATE_HOLD || this.state == STATE_HORIZONTAL_DRAG){
                    return super.dispatchTouchEvent(ev);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                switch (this.state){
                    case STATE_HOLD:
                        /**
                         * 判定为横向滑动时, 禁止拦截事件, 可配置
                         */
                        if (disableIfHorizontalDrag && dragDirection == DRAG_DIRECTION_UNKNOWN){
                            if (Math.abs(currY - downY) > mTouchSlop){
                                dragDirection = DRAG_DIRECTION_VERTICAL;
                            } else if (Math.abs(currX - downX) > mTouchSlop) {
                                dragDirection = DRAG_DIRECTION_HORIZONTAL;
                                stateToHorizontalDrag();
                                return super.dispatchTouchEvent(ev);
                            }
                        }
                        //判断状态
                        if (reachState.reachTop() && distanceY > mTouchSlop){
                            /**
                             * 子控件到达顶部, 且继续向下拉动
                             */
                            //上边越界
                            stateToTopOverDrag();
                            //模拟取消事件给子控件
                            emulateCancelEvent(ev);
                            //越界阻尼
                            if (scrollY >= 0 && distanceY > 0){
                                distanceY = distanceY * overDragResistance;
                            }
                            //越界滚动
                            scrollByOffset(distanceY);
                            return true;
                        } else if (reachState.reachBottom() && distanceY < -mTouchSlop){
                            /**
                             * 子控件到达底部, 且继续向上拉动
                             */
                            //下边越界
                            stateToBottomOverDrag();
                            //模拟取消事件给子控件
                            emulateCancelEvent(ev);
                            //越界阻尼
                            if (scrollY <= 0 && distanceY < 0){
                                distanceY = distanceY * overDragResistance;
                            }
                            //越界滚动
                            scrollByOffset(distanceY);
                            return true;
                        } else {
                            /**
                             * 子控件未到达顶部或底部
                             */
                            //无越界, 直接分发事件给子控件
                            return super.dispatchTouchEvent(ev);
                        }
                    case STATE_TOP_OVER_DRAG:
                        if (scrollY + distanceY <= 0){
                            //上边越界结束
                            if (reachState.reachBottom()){
                                //如果滚动子控件同时也到达底部的话, 直接进入下边越界状态
                                stateToBottomOverDrag();
                                //越界滚动
                                scrollByOffset(distanceY);
                                return true;
                            }
                            //归位
                            scrollToTarget(0);
                            //HOLD模式
                            stateToHold();
                            //模拟DOWN事件给子控件
                            emulateDownEvent(ev);
                            //分发事件给子控件
                            return super.dispatchTouchEvent(ev);
                        }
                        //越界阻尼
                        if (scrollY >= 0 && distanceY > 0){
                            distanceY = distanceY * overDragResistance;
                        }
                        //越界滚动
                        scrollByOffset(distanceY);
                        return true;
                    case STATE_BOTTOM_OVER_DRAG:
                        if (scrollY + distanceY >= 0){
                            //下边越界结束
                            if (reachState.reachTop()){
                                //如果滚动子控件同时也到达顶部的话, 直接进入上边越界状态
                                stateToTopOverDrag();
                                //越界滚动
                                scrollByOffset(distanceY);
                                return true;
                            }
                            //归位
                            scrollToTarget(0);
                            //HOLD模式
                            stateToHold();
                            //模拟DOWN事件给子控件
                            emulateDownEvent(ev);
                            //分发事件给子控件
                            return super.dispatchTouchEvent(ev);
                        }
                        //越界阻尼
                        if (scrollY <= 0 && distanceY < 0){
                            distanceY = distanceY * overDragResistance;
                        }
                        //越界滚动
                        scrollByOffset(distanceY);
                        return true;
                    case STATE_HORIZONTAL_DRAG:
                        //横向拖动模式直接分发给子控件
                        return super.dispatchTouchEvent(ev);
                }
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                if (this.state == STATE_HOLD || this.state == STATE_HORIZONTAL_DRAG){
                    return super.dispatchTouchEvent(ev);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                //事件被上层拦截
                isCancel = true;
            case MotionEvent.ACTION_UP:
                int lastState = this.state;
                stateToRelease();
                switch (lastState){
                    case STATE_TOP_OVER_DRAG:
                        if (!isCancel) {
                            if (topParkEnabled && scrollY > overDragThreshold) {
                                if (!topParked) {
                                    topParked = true;
                                    callbackTopPark();
                                }
                            }
                        }
                        //弹回
                        free();
                        return true;
                    case STATE_BOTTOM_OVER_DRAG:
                        if (!isCancel) {
                            if (bottomParkEnabled && scrollY < -overDragThreshold) {
                                if (!bottomParked) {
                                    bottomParked = true;
                                    callbackBottomPark();
                                }
                            }
                        }
                        //弹回
                        free();
                        return true;
                    case STATE_HOLD:
                    case STATE_HORIZONTAL_DRAG:
                    case STATE_RELEASE:
                    default:
                        return super.dispatchTouchEvent(ev);
                }
        }
        return true;
    }

    @Override
    public void computeScroll() {
        //计算scroller
        if (this.state == STATE_RELEASE && !scroller.isFinished()){
            scroller.computeScrollOffset();
            scrollToTarget(scroller.getCurrY());
        }

        //计算当前位置
        int _scrollY;
        if (Math.abs(scrollY) < 1){
            _scrollY = 0;
        } else {
            _scrollY = (int) -scrollY;
        }

        //滚动控件
        scrollTo(0, _scrollY);

        //必须实现自刷新
        if (this.state == STATE_RELEASE && !scroller.isFinished())
            postInvalidate();
    }

    /*****************************************************************************88
     * 状态变化
     */

    /**
     * 重置状态
     */
    private void resetState() {
        //停止滚动
        scroller.abortAnimation();
        //拖动方向重置
        dragDirection = DRAG_DIRECTION_UNKNOWN;
    }

    private void stateToRelease() {
        this.state = STATE_RELEASE;
        callbackStateChanged();
    }

    private void stateToHorizontalDrag() {
        this.state = STATE_HORIZONTAL_DRAG;
        callbackStateChanged();
    }

    private void stateToBottomOverDrag() {
        this.state = STATE_BOTTOM_OVER_DRAG;
        callbackStateChanged();
    }

    private void stateToTopOverDrag() {
        this.state = STATE_TOP_OVER_DRAG;
        callbackStateChanged();
    }

    private void stateToHold() {
        this.state = STATE_HOLD;
        callbackStateChanged();
    }

    private void callbackStateChanged() {
        if (onOverDragStateChangeListener != null){
            onOverDragStateChangeListener.onStateChanged(this.state);
        }
        if (refreshViewList != null){
            for (RefreshView refreshView : refreshViewList){
                refreshView.onStateChanged(this.state);
            }
        }
    }

    /**********************************************************************
     * 滚动
     */

    private void scrollByOffset(float offset){
        scrollY += offset;
        postInvalidate();
        callbackScroll();
    }

    private void scrollToTarget(float target){
        scrollY = target;
        postInvalidate();
        callbackScroll();
    }

    private void callbackScroll() {
        if (onOverDragScrollListener != null){
            onOverDragScrollListener.onScroll(this.state, this.scrollY);
        }
        if (refreshViewList != null){
            for (RefreshView refreshView : refreshViewList){
                refreshView.onScroll(this.state, this.scrollY);
            }
        }
    }

    /************************************************************************
     * PARK
     */

    private void callbackTopPark() {
        if (onOverDragParkListener != null) {
            onOverDragParkListener.onTopPark();
        }
        if (refreshViewList != null){
            for (RefreshView refreshView : refreshViewList){
                refreshView.onTopPark();
            }
        }
    }

    private void callbackBottomPark() {
        if (onOverDragParkListener != null) {
            onOverDragParkListener.onBottomPark();
        }
        if (refreshViewList != null){
            for (RefreshView refreshView : refreshViewList){
                refreshView.onBottomPark();
            }
        }
    }

    /*************************************************************************
     * public
     */

    public void resetTopPark(){
        topParked = false;
        free();
    }

    public void resetBottomPark(){
        bottomParked = false;
        free();
    }

    /**
     * 获取滚动子控件
     */
    protected View getScrollChild(){
        return getChildAt(getChildCount() - 1);
    }

    /**
     * 判断子控件是否达到顶部/底部(增加对View的支持, 可复写此方法实现)
     */
    protected ReachState checkReachState(View child) {
        boolean reachTop;
        boolean reachBottom;

        if (child instanceof ScrollView) {
            reachTop = ScrollViewUtils.reachTop((ScrollView) child);
            reachBottom = ScrollViewUtils.reachBottom((ScrollView) child);
        } else if (child instanceof ListView) {
            reachTop = ListViewUtils.reachTop((ListView) child);
            reachBottom = ListViewUtils.reachBottom((ListView) child);
        } else {
            throw new RuntimeException("[VerticalOverDragContainer]child view is not supported, view:" + child);
        }

        if (reachTop && reachBottom) {
            return ReachState.REACH_BOTH;
        } else if (reachTop) {
            return ReachState.REACH_TOP;
        } else if (reachBottom) {
            return ReachState.REACH_BOTTOM;
        }

        return ReachState.HALFWAY;
    }

    /**
     * @param onOverDragStateChangeListener 设置状态变化监听器
     */
    public void setOnOverDragStateChangeListener(OnOverDragStateChangeListener onOverDragStateChangeListener) {
        this.onOverDragStateChangeListener = onOverDragStateChangeListener;
    }

    /**
     * @param onOverDragScrollListener 设置滚动位置监听器
     */
    public void setOnOverDragScrollListener(OnOverDragScrollListener onOverDragScrollListener) {
        this.onOverDragScrollListener = onOverDragScrollListener;
    }

    /**
     * @param onOverDragParkListener 设置顶部/底部驻留监听器(可用于实现下拉刷新/上拉加载)
     */
    public void setOnOverDragParkListener(OnOverDragParkListener onOverDragParkListener) {
        this.onOverDragParkListener = onOverDragParkListener;
    }

    /**
     * @param refreshView 下拉刷新/上拉加载的效果需要实现RefreshView接口, 并通过该方法添加
     */
    public void addRefreshView(RefreshView refreshView){
        if (refreshView == null){
            return;
        }
        if (refreshViewList == null){
            refreshViewList = new ArrayList<>();
        }
        refreshView.setContainer(this);
        refreshViewList.add(refreshView);
    }

    /**
     * @param disableIfHorizontalDrag true:当发生横向手势, 禁止越界拖动, 常用与嵌套横向滑动的控件, 例如ViewPager
     */
    public void setDisableIfHorizontalDrag(boolean disableIfHorizontalDrag) {
        this.disableIfHorizontalDrag = disableIfHorizontalDrag;
    }

    /**
     * @param overDragResistance 0-1, 越界拖动阻尼, 值约小越界拖动越慢
     */
    public void setOverDragResistance(float overDragResistance) {
        if (overDragResistance < 0 || overDragResistance > 1){
            throw new RuntimeException("overDragResistance must >= 0 and <= 1");
        }
        this.overDragResistance = overDragResistance;
    }

    /**
     * @param scrollDuration ms, 越界弹回的时间
     */
    public void setScrollDuration(int scrollDuration) {
        if (scrollDuration < 0){
            throw new RuntimeException("scrollDuration must >= 0");
        }
        this.scrollDuration = scrollDuration;
    }

    /**
     * @param overDragThreshold >=0, 当越界拖动超过该设定值时, 会发生PARK时间, 滚动位置会停滞在设定值上, 用于实现下拉刷新上拉加载
     */
    public void setOverDragThreshold(int overDragThreshold) {
        if (overDragThreshold < 0){
            throw new RuntimeException("overDragThreshold must >= 0");
        }
        this.overDragThreshold = overDragThreshold;
    }

    /**
     * @param topParkEnabled true:允许顶部PARK
     */
    public void setTopParkEnabled(boolean topParkEnabled) {
        this.topParkEnabled = topParkEnabled;
    }

    /**
     * @param bottomParkEnabled true:允许底部PARK
     */
    public void setBottomParkEnabled(boolean bottomParkEnabled) {
        this.bottomParkEnabled = bottomParkEnabled;
    }

    /*************************************************************************
     * private
     */

    /**
     * 回弹
     */
    private void free(){
        //当前位置
        int currScrollY = (int) scrollY;
        //计算弹回目标
        int target = 0;
        if (topParkEnabled && currScrollY > overDragThreshold){
            target = overDragThreshold;
        }else if (bottomParkEnabled && currScrollY < -overDragThreshold){
            target = -overDragThreshold;
        }

        //过滤
        if (currScrollY == target){
            return;
        }
        //回弹
        scroller.startScroll(0, currScrollY, 0, target - currScrollY, scrollDuration);
        //刷新
        postInvalidate();
    }

    /**
     * 模拟CANCEL时间分发给子控件
     */
    private void emulateCancelEvent(MotionEvent ev){
        touchPoints.setCapacity(ev.getPointerCount());
        for (int i = 0 ; i < touchPoints.getCapacity() ; i++){
            touchPoints.setX(i, ev.getX(i));
            touchPoints.setY(i, ev.getY(i));
            touchPoints.setId(i, ev.getPointerId(i));
        }
        MotionEvent emuEvent = MotionEventUtils.obtain(MotionEvent.ACTION_CANCEL, touchPoints, ev.getDownTime());
        super.dispatchTouchEvent(emuEvent);
    }

    /**
     * 模拟DOWN事件分发给子控件
     */
    private void emulateDownEvent(MotionEvent ev){
        //TODO 简易处理, 后续改成吧多个点变成多个POINTER_DOWN时间, 模拟的更逼真
        touchPoints.setCapacity(ev.getPointerCount());
        for (int i = 0 ; i < touchPoints.getCapacity() ; i++){
            touchPoints.setX(i, ev.getX(i));
            touchPoints.setY(i, ev.getY(i));
            touchPoints.setId(i, ev.getPointerId(i));
        }
        MotionEvent emuEvent = MotionEventUtils.obtain(MotionEvent.ACTION_DOWN, touchPoints, ev.getDownTime());
        super.dispatchTouchEvent(emuEvent);
    }

    /**
     * 计算Y方向上的位移
     */
    protected float calculateMoveDistance(MotionEvent event) {
        //触摸事件结束后, 重置状态
        if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            lastPointId = -1;
            return 0;
        }
        //获得第一个触点的ID
        int pointId = event.getPointerId(0);
        if (pointId == lastPointId) {
            //若第一个触点ID未变, 则计算点位移
            float distance = event.getY() - lastY;
            this.lastY = event.getY();
            return distance;
        } else {
            //若第一个触点ID变化, 则记录新的ID, 本次视为没有移动
            this.lastPointId = pointId;
            this.lastY = event.getY();
            return 0;
        }
    }

    /*********************************************************************************
     * interface / enum
     */

    protected enum ReachState {

        HALFWAY(false, false),
        REACH_TOP(true, false),
        REACH_BOTTOM(false, true),
        REACH_BOTH(true, true);

        private boolean reachTop;
        private boolean reachBottom;

        ReachState(boolean reachTop, boolean reachBottom) {
            this.reachTop = reachTop;
            this.reachBottom = reachBottom;
        }

        protected boolean reachTop() {
            return reachTop;
        }

        protected boolean reachBottom() {
            return reachBottom;
        }

    }

    public interface OnOverDragStateChangeListener {

        /**
         * private static final int STATE_RELEASE = 0;
         * private static final int STATE_HOLD = 1;
         * private static final int STATE_TOP_OVER_DRAG = 2;
         * private static final int STATE_BOTTOM_OVER_DRAG = 3;
         * private static final int STATE_HORIZONTAL_DRAG = 4;
         */
        void onStateChanged(int state);

    }

    public interface OnOverDragScrollListener {

        /**
         * @param state 当前状态
         * @param scrollY Y方向越界拖动位置, +:顶部越界拖动, -:底部越界拖动
         */
        void onScroll(int state, float scrollY);

    }

    public interface OnOverDragParkListener {

        /**
         * 顶部PARK事件, 事件发生后需要手动重置状态(resetTopPark方法), 在重置状态前, 不会再发生相同事件
         */
        void onTopPark();

        /**
         * 底部PARK, 事件发生后需要手动重置状态(resetBottomPark方法), 在重置状态前, 不会再发生相同事件
         */
        void onBottomPark();

    }

    /**
     * 下拉刷新上拉加载效果控件接口
     */
    public interface RefreshView extends OnOverDragStateChangeListener, OnOverDragScrollListener, OnOverDragParkListener{

        void setContainer(VerticalOverDragContainer container);

    }

}