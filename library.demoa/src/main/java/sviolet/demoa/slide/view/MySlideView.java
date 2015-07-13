package sviolet.demoa.slide.view;

import sviolet.turquoise.utils.MeasureUtils;
import sviolet.turquoise.view.slide.SlideEngine;
import sviolet.turquoise.view.slide.SlideView;
import sviolet.turquoise.view.slide.logic.LinearGestureDriver;
import sviolet.turquoise.view.slide.logic.LinearScrollEngine;
import sviolet.turquoise.view.slide.logic.LinearStageScrollEngine;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.LinearLayout;

/**
 * 自定义SlideView
 * @author S.Violet
 */
@SuppressWarnings("unused")
@SuppressLint("ClickableViewAccessibility")
public class MySlideView extends LinearLayout implements SlideView{

	private LinearGestureDriver mGestureDriver = new LinearGestureDriver(getContext());
	private LinearStageScrollEngine mSlideEngine = new LinearStageScrollEngine(getContext(), this);
	
	private OnClickListener mOnGestureHoldListener;
	
	public MySlideView(Context context) {
		super(context);
		init();
	}
	
	public MySlideView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	/**
	 * 控件自身获取宽高方法
	 */
	private void init(){
		//View获取自身宽高等参数方法
		//绘制监听器, 也可以使用addOnGlobalLayoutListener监听layout事件
		getViewTreeObserver().addOnPreDrawListener(new OnPreDrawListener() {
			@Override
			public boolean onPreDraw() {
				//移除监听器, 以免重复调用
				getViewTreeObserver().removeOnPreDrawListener(this); 
                initStageLinearSlide();
                return true;
            }
        });
	}
	
	//LinearSlideEngine初始化示例
//	private void initLinearSlide() {
//		//此处可调用getWidth()/getHeight()等方法获得自身宽高, 因为View已完成measure
//        int range = getHeight() - MeasureUtils.dp2px(getContext(), 60);//滑动范围 = 控件高 - 30dp
//        int position = range;//初始位置
//        //后续初始化操作, 创建配置手势驱动/滑动引擎实例
//		mGestureDriver.setOrientation(LinearGestureDriver.ORIENTATION_VERTICAL);
//		mSlideEngine.setMaxRange(range);
//		mSlideEngine.setInitPosition(position);
//		mSlideEngine.setStageDuration(1000);
//		mGestureDriver.bind(mSlideEngine);
//		//允许拖动越界, 越界阻尼0.7
//		mSlideEngine.setOverScroll(true, 0.7f);
//		mSlideEngine.setOnGestureHoldListener(mOnGestureHoldListener);
//	}
	
	//StageLinearSlideEngine初始化示例
	private void initStageLinearSlide() {
		//此处可调用getWidth()/getHeight()等方法获得自身宽高, 因为View已完成measure
        int range = getHeight() - MeasureUtils.dp2px(getContext(), 60);//滑动范围 = 控件高 - 30dp
        int position = range;//初始位置
        //后续初始化操作, 创建配置手势驱动/滑动引擎实例
		mGestureDriver.setOrientation(LinearGestureDriver.ORIENTATION_VERTICAL);
		mSlideEngine.setStageRange(range / 2);//一个阶段的滑动距离
		mSlideEngine.setStageNum(3);//阶段数
		mSlideEngine.setInitPosition(position);//初始位置
		mSlideEngine.setStageDuration(1000);//阶段滑动时间
		mGestureDriver.bind(mSlideEngine);
		//允许拖动越界, 越界阻尼0.7
//		mSlideEngine.setOverScroll(true, 0.7f);
		mSlideEngine.setOnGestureHoldListener(mOnGestureHoldListener);
	}
	
	/**
	 * 捕获触摸事件
	 */
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		boolean original = super.onInterceptTouchEvent(ev);
		if(mGestureDriver != null && mGestureDriver.onInterceptTouchEvent(ev))
			return true;
		return original;
	}
	
	/**
	 * 捕获触摸事件
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean original = super.onTouchEvent(event);
		if(mGestureDriver != null && mGestureDriver.onTouchEvent(event))
			return true;
		return original;
	}
	
	/**
	 * 输出
	 */
	@Override
	public void computeScroll() {
		if(mSlideEngine != null){
			int value = mSlideEngine.getPosition();
			scrollTo(0, value);
			if(!mSlideEngine.isStop()){
				postInvalidate();
			}
		}
	}
	
	public void scrollToStage(int stage){
		if(mSlideEngine != null)
			mSlideEngine.scrollToStage(stage);
	}
	
	/**
	 * 通知刷新
	 */
	@Override
	public void notifyRefresh() {
		postInvalidate();
	}

	@Override
	public void destroy() {
		
	}
	
	public void setOnGestureHoldListener(OnClickListener listener){
		this.mOnGestureHoldListener = listener;
	}

	public SlideEngine getSlideEngine(){
		return mSlideEngine;
	}
}