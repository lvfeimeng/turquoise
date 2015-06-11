package sviolet.demoa.slide.sviolet.demoa.slide.view;

import java.util.List;

import sviolet.turquoise.view.listener.OnVelocityOverflowListener;
import sviolet.turquoise.view.slide.SlideEngine;
import sviolet.turquoise.view.slide.SlideEngineGroup;
import sviolet.turquoise.view.slide.SlideView;
import sviolet.turquoise.view.slide.logic.LinearFlingEngine;
import sviolet.turquoise.view.slide.logic.LinearGestureDriver;
import sviolet.turquoise.view.slide.logic.LinearScrollEngine;
import sviolet.turquoise.view.slide.logic.LinearStageScrollEngine;
import sviolet.turquoise.view.slide.view.AdaptListView;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.LinearLayout;
import android.widget.ListView;


/**
 * 卡片控件
 * @author S.Violet
 *
 */
public class CardSlideView extends LinearLayout implements SlideView {

	private LinearGestureDriver mGestureDriver;//手势驱动
	private LinearScrollEngine cardSlideEngine;//滑动引擎(本View用)
	
	private LinearFlingEngine listSlideEngine;//滑动引擎(内部AdaptListView用)
	private LinearFlingEngine titleSlideEngine;//滑动引擎(内部CardSlideTitleView用)
	
	public CardSlideView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public CardSlideView(Context context) {
		super(context);
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
                initSlide();
                return true;
            }
        });
	}
	
	/**
	 * 初始化滑动
	 */
	private void initSlide(){
		//此处可调用getWidth()/getHeight()等方法获得自身宽高, 因为View已完成measure
        int range = getHeight();//滑动范围
        int position = 0;//初始位置
        //后续初始化操作, 创建配置手势驱动/滑动引擎实例
        //手势驱动
		mGestureDriver = new LinearGestureDriver(getContext(), LinearGestureDriver.ORIENTATION_VERTICAL);
		//绑定引擎组
		mGestureDriver.bind(mSlideEngineGroup);
		//本View使用的滑动引擎
		cardSlideEngine = new LinearStageScrollEngine(getContext(), this, range / 4, 5, position, 700);
		//引擎组新增两个引擎(必须设置别名)
		mSlideEngineGroup.addSlideEngine(cardSlideEngine, "card");
		mSlideEngineGroup.addSlideEngine(listSlideEngine, "list");
		mSlideEngineGroup.addSlideEngine(titleSlideEngine, "title");

		//列表速度溢出, AdaptListView不能使用它的SlideEngine监听速度溢出,
		//因为AdaptListView的SlideEngine是无限滚动, 引擎不会到达边界, 也不
		//会出现速度溢出. 需要用AdaptListView自身实现的速度溢出监听
		((AdaptListView)listSlideEngine.getSlideView()).setOnVelocityOverflowListener(new OnVelocityOverflowListener() {
			@Override
			public void onVelocityOverflow(int velocity) {
				if (velocity < 0) {
					//由于titleSlideEngine反向输出, 此处将速度反向
					//减速
					titleSlideEngine.fling(-velocity / 2);
				}
			}
		});
		//标题速度溢出
		titleSlideEngine.setOnVelocityOverflowListener(new OnVelocityOverflowListener() {
			@Override
			public void onVelocityOverflow(int velocity) {
				//由于titleSlideEngine反向输出, 此处将速度反向
				velocity = - velocity;
				if (velocity > 0) {
					listSlideEngine.fling(velocity);
				}
			}
		});
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
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean original = super.onTouchEvent(event);
		if(mGestureDriver != null && mGestureDriver.onTouchEvent(event))
			return true;
		return original;
	}
	
	/**
	 * 输出(外部card)
	 */
	@Override
	public void computeScroll() {
		if(cardSlideEngine != null){
			int value = cardSlideEngine.getPosition() - cardSlideEngine.getRange();
			scrollTo(0, value);
			if(!cardSlideEngine.isStop()){
				postInvalidate();
			}
		}
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

	/***********************************************************************
	 * public
	 */
	
	/**
	 * 绑定内部的MyTitleView
	 * @param titleView
	 */
	public void bindTitleView(CardSlideTitleView titleView){
		if(titleView != null)
			titleSlideEngine = titleView.getSlideEngine();
	}
	
	/**
	 * 绑定内部的ListView(AdaptListView)
	 * @param adaptListView
	 */
	public void bindListView(AdaptListView adaptListView){
		if(adaptListView != null)
			listSlideEngine = adaptListView.getSlideEngine();
	}

	/**
	 * 拉出卡片
	 */
	public void show(){
		cardSlideEngine.scrollToPosition(cardSlideEngine.getPositionOfStage(3), 800, true);
	}
	
	/***********************************************************************
	 * SlideEngineGroup
	 */
	
	private SlideEngineGroup mSlideEngineGroup = new SlideEngineGroup() {
		
		/**
		 * 通用分发
		 */
		@Override
		public void dispatch(List<String> handlingEngines) {
			//分发到所有引擎
			handlingEngines.addAll(getAllSlideEngineAliases());
		}

		/**
		 * 自定义onGestureDrive事件分发
		 */
		@Override
		public void dispatchGestureDrive(List<String> handlingEngines, int curr, int offset, int velocity) {
			//外部卡片引擎(根据别名取)
			LinearStageScrollEngine cardSlideEngine = (LinearStageScrollEngine)getSlideEngine("card");
			//内部列表引擎(根据别名取)
			LinearFlingEngine listSlideEngine = (LinearFlingEngine)getSlideEngine("list");
			//内部标题引擎(根据别名取)
			LinearFlingEngine titleSlideEngine = (LinearFlingEngine) getSlideEngine("title");
			//判断速度方向
			if(velocity < 0){//手势向上
				//判断卡片控件的位置
				if(cardSlideEngine.getCurrentStage() == cardSlideEngine.getStageNum() - 1){
					//若卡片置顶, 则驱动内部控件
					//判断标题控件位置
					if(titleSlideEngine.getCurrentStage() == 0){
						//若标题控件压缩到底, 则驱动列表
						handlingEngines.add("list");
					}else{
						//若标题控件未压缩到底, 则驱动标题
						handlingEngines.add("title");
					}
				}else{
					//若卡片未置顶, 则驱动卡片向上滑动
					handlingEngines.add("card");
				}
			}else{//手势向下
				//取内部列表控件
				AdaptListView listSlideView = (AdaptListView)listSlideEngine.getSlideView();
				//##判断列表是否拉到顶部##
				if(listSlideView.reachTop()){
					//列表拉到顶部, 则驱动其他控件
					//判断标题控件位置
					if(titleSlideEngine.getCurrentStage() == 1){
						//若标题控件拉到最大, 则驱动卡片控件
						handlingEngines.add("card");
					}else{
						//若标题控件未拉到最大, 则驱动标题控件
						handlingEngines.add("title");
					}
				}else{
					//列表未到达顶部, 则驱动内部ListView
					handlingEngines.add("list");
				}
			}
		}
		
		/**
		 * 自定义onGestureRelease处理
		 */
		@Override
		public void handleGestureRelease(SlideEngine engine, String alias, int velocity) {
			
			//手势向下, 且分发给卡片的情况
			if(velocity > 0 && alias.equals("card")){
				//外部卡片引擎(根据别名取)
				LinearStageScrollEngine cardSlideEngine = (LinearStageScrollEngine)getSlideEngine("card");
				//内部列表引擎(根据别名取)
				LinearFlingEngine listSlideEngine = (LinearFlingEngine)getSlideEngine("list");
				//取内部列表控件
				AdaptListView listSlideView = (AdaptListView)listSlideEngine.getSlideView();
				//##判断列表是否拉到顶部##
				if(listSlideView.reachTop()){
					//判断标题控件位置
					if(titleSlideEngine.getCurrentStage() == 1){
						//判断卡片控件拉开距离
						if(cardSlideEngine.getCurrentStage() < (cardSlideEngine.getStageNum() - 1) * 0.9f){
							//收起卡片
							cardSlideEngine.scrollToPosition(cardSlideEngine.getPositionOfStage(0), 800, true);
						}else{
							//弹回
							cardSlideEngine.scrollToPosition(cardSlideEngine.getPositionOfStage(cardSlideEngine.getStageNum() - 1), 500, true);
						}
					}
				}
			}
			
			//通常情况
			super.handleGestureRelease(engine, alias, velocity);
		};
		
	};
	
}
