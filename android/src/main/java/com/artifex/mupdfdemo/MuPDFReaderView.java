package com.artifex.mupdfdemo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.ContextCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;

import com.github.react.mbbLibrary.R;
import com.github.react.mbbLibrary.RCTMuPdfModule;
import com.github.react.mbbLibrary.constants.SPConsts;
import com.github.react.mbbLibrary.util.SharedPreferencesUtil;

public class MuPDFReaderView extends ReaderView {

	private MuPDFReaderViewListener listener;
	public enum Mode {Viewing, Selecting, Drawing}
	private final Context mContext;
	private boolean mLinksEnabled = false;
	private boolean isLinkHighlightColor = false;
	private Mode mMode = Mode.Viewing;
	private boolean tapDisabled = false;
	private int tapPageMargin;

	private int mLinkHighlightColor;

	protected void onTapMainDocArea() {
        checkMuPDFReaderViewListener();
		listener.onTapMainDocArea();
	}
	protected void onDocMotion() {
        checkMuPDFReaderViewListener();
		listener.onDocMotion();
	}
	protected void onHit(Hit item) {
        checkMuPDFReaderViewListener();
		listener.onHit(item);
	}


	public void setLinksEnabled(boolean b) {
		mLinksEnabled = b;
		resetupChildren();
	}


	public void setLinkHighlightColor(int color) {
		isLinkHighlightColor = true;
		mLinkHighlightColor = color;
		resetupChildren();
	}


	public void setSearchTextColor(int color) {
        SharedPreferencesUtil.put(SPConsts.SP_COLOR_SEARCH_TEXT, color);
		resetupChildren();
	}


	public void setInkColor(int color) {
//		SharedPreferencesUtil.put(SPConsts.SP_COLOR_SEARCH_TEXT, color);
		((MuPDFView) getCurrentView()).setInkColor(color);
	}


    public void setPaintStrockWidth(float inkThickness) {
//		SharedPreferencesUtil.put(SPConsts.SP_COLOR_SEARCH_TEXT, color);
        ((MuPDFView) getCurrentView()).setPaintStrockWidth(inkThickness);
    }

    public float getCurrentScale() {
		return ((MuPDFView) getCurrentView()).getCurrentScale();
	}

	public void setMode(Mode m) {
		mMode = m;
	}

	private void setup() {
		// Get the screen size etc to customise tap margins.
		// We calculate the size of 1 inch of the screen for tapping.
		// On some devices the dpi values returned are wrong, so we
		// sanity check it: we first restrict it so that we are never
		// less than 100 pixels (the smallest Android device screen
		// dimension I've seen is 480 pixels or so). Then we check
		// to ensure we are never more than 1/5 of the screen width.
		DisplayMetrics dm = new DisplayMetrics();
		WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		wm.getDefaultDisplay().getMetrics(dm);
		tapPageMargin = (int)dm.xdpi;
		if (tapPageMargin < 100) {
			tapPageMargin = 100;
		}
		if (tapPageMargin > dm.widthPixels/5) {
			tapPageMargin = dm.widthPixels/5;
		}
		// set view backgroundColor
		if(RCTMuPdfModule.dark.equals("true")){
			setBackgroundColor(ContextCompat.getColor(mContext, R.color.muPDFReaderView_dark_bg));
		}else{
			setBackgroundColor(ContextCompat.getColor(mContext, R.color.muPDFReaderView_bg));
		}
	}

	public MuPDFReaderView(Context context) {
		super(context);
		mContext = context;
		setup();
	}

	

	public MuPDFReaderView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		mContext = context;
		setup();

	}


	public boolean onSingleTapUp(MotionEvent e) {
		LinkInfo link;

		if (mMode == Mode.Viewing && !tapDisabled && !RCTMuPdfModule.OpenMode.equals("Accused")) {
			MuPDFView pageView = (MuPDFView) getDisplayedView();
			Hit item = pageView.passClickEvent(e.getX(), e.getY());
			onHit(item);
			if (item == Hit.Nothing) {
				if (mLinksEnabled && (link = pageView.hitLink(e.getX(), e.getY())) != null) {
					link.acceptVisitor(new LinkInfoVisitor() {
						@Override
						public void visitInternal(LinkInfoInternal li) {
							// Clicked on an internal (GoTo) link
							setDisplayedViewIndex(li.pageNumber);
						}

						@Override
						public void visitExternal(LinkInfoExternal li) {
							Intent intent = new Intent(Intent.ACTION_VIEW, Uri
									.parse(li.url));
							mContext.startActivity(intent);
						}

						@Override
						public void visitRemote(LinkInfoRemote li) {
							// Clicked on a remote (GoToR) link
						}
					});
				} else if (e.getX() < tapPageMargin) {
					super.smartMoveBackwards();
				} else if (e.getX() > super.getWidth() - tapPageMargin) {
					super.smartMoveForwards();
				} else if (e.getY() < tapPageMargin) {
					super.smartMoveBackwards();
				} else if (e.getY() > super.getHeight() - tapPageMargin) {
					super.smartMoveForwards();
				} else {
					onTapMainDocArea();
				}
			}
		}
		return super.onSingleTapUp(e);
	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                            float distanceY) {
		if(RCTMuPdfModule.OpenMode.equals("Accused")){
			return false;
		}
		MuPDFView pageView = (MuPDFView)getDisplayedView();
		switch (mMode) {
		case Viewing:
			if (!tapDisabled)
				onDocMotion();

			return super.onScroll(e1, e2, distanceX, distanceY);
		case Selecting:
			if (pageView != null)
				pageView.selectText(e1.getX(), e1.getY(), e2.getX(), e2.getY());
			return true;
		default:
			return true;
		}
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY) {
		if(RCTMuPdfModule.OpenMode.equals("Accused")){
			return false;
		}
		switch (mMode) {
		case Viewing:
			return super.onFling(e1, e2, velocityX, velocityY);
		default:
			return true;
		}
	}

	public boolean onScaleBegin(ScaleGestureDetector d) {
		// Disabled showing the buttons until next touch.
		// Not sure why this is needed, but without it
		// pinch zoom can make the buttons appear
		tapDisabled = true;
		return super.onScaleBegin(d);
	}

	public boolean onTouchEvent(MotionEvent event) {

		if ( mMode == Mode.Drawing )
		{
			float x = event.getX();
			float y = event.getY();
			switch (event.getAction())
			{
				case MotionEvent.ACTION_DOWN:
					touch_start(x, y);
					break;
				case MotionEvent.ACTION_MOVE:
					touch_move(x, y);
					break;
			}
		}

		if ((event.getAction() & event.getActionMasked()) == MotionEvent.ACTION_DOWN)
		{
			tapDisabled = false;
		}

		return super.onTouchEvent(event);
	}

	private float mX, mY;

	private static final float TOUCH_TOLERANCE = 2;

	public void touch_start(float x, float y) {

		MuPDFView pageView = (MuPDFView)getDisplayedView();
		if (pageView != null)
		{
			pageView.startDraw(x, y);
		}
		mX = x;
		mY = y;
	}

	public void touch_move(float x, float y) {

		float dx = Math.abs(x - mX);
		float dy = Math.abs(y - mY);
		if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE)
		{
			MuPDFView pageView = (MuPDFView)getDisplayedView();
			if (pageView != null)
			{
				pageView.continueDraw(x, y);
			}
			mX = x;
			mY = y;
		}
	}

	protected void onChildSetup(int i, View v) {
		if (SearchTaskResult.get() != null && SearchTaskResult.get().pageNumber == i) {
			((MuPDFView) v).setSearchBoxes(SearchTaskResult.get().searchBoxes);
		} else {
			((MuPDFView) v).setSearchBoxes(null);
		}

		((MuPDFView) v).setLinkHighlighting(mLinksEnabled);


		if (isLinkHighlightColor) {
			((MuPDFView) v).setLinkHighlightColor(mLinkHighlightColor);
		}

		((MuPDFView) v).setChangeReporter(new Runnable() {
			public void run() {
				applyToChildren(new ReaderView.ViewMapper() {
					@Override
					public void applyToView(View view) {
						((MuPDFView) view).update();
					}
				});
			}
		});
	}

	public static int mPageCore = 0;
	protected void onMoveToChild(int i) {
		if (SearchTaskResult.get() != null
				&& SearchTaskResult.get().pageNumber != i) {
			SearchTaskResult.set(null);
			resetupChildren();
		}
        checkMuPDFReaderViewListener();
		listener.onMoveToChild(i);
		mPageCore = i;

	}

	@Override
	protected void onMoveOffChild(int i) {
		View v = getView(i);
		if (v != null)
			((MuPDFView)v).deselectAnnotation();
	}

	protected void onSettle(View v) {
		// When the layout has settled ask the page to render
		// in HQ
		((MuPDFView) v).updateHq(false);
	}

	protected void onUnsettle(View v) {
		// When something changes making the previous settled view
		// no longer appropriate, tell the page to remove HQ
		((MuPDFView) v).removeHq();
	}

	@Override
	protected void onNotInUse(View v) {
		((MuPDFView) v).releaseResources();
	}

	@Override
	protected void onScaleChild(View v, Float scale) {
		((MuPDFView) v).setScale(scale);
	}


	public void setListener(MuPDFReaderViewListener listener) {
		this.listener = listener;
	}

	private void checkMuPDFReaderViewListener() {
        if (listener == null) {
            listener = new MuPDFReaderViewListener() {
                @Override
                public void onMoveToChild(int i) {

                }

                @Override
                public void onTapMainDocArea() {

                }

                @Override
                public void onDocMotion() {

                }

                @Override
                public void onHit(Hit item) {

                }
            };
        }
    }
}
