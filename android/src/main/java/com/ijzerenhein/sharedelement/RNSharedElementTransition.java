package com.ijzerenhein.sharedelement;

import java.util.ArrayList;

import android.os.Build;
import android.util.Log;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Point;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.react.bridge.Callback;
import com.facebook.react.uimanager.ThemedReactContext;

public class RNSharedElementTransition extends ViewGroup {
    static private String LOG_TAG = "RNSharedElementTransition";

    static private int ITEM_START_ANCESTOR = 0;
    static private int ITEM_END_ANCESTOR = 1;
    static private int ITEM_START = 2;
    static private int ITEM_END = 3;

    private String mAnimation = "move";
    private float mNodePosition = 0.0f;
    private boolean mReactLayoutSet = false;
    private boolean mInitialLayoutPassCompleted = false;
    private ArrayList<RNSharedElementTransitionItem> mItems = new ArrayList<RNSharedElementTransitionItem>();
    private int[] mParentLocation = new int[2];
    private boolean mRequiresClipping = false;
    private View mStartView;
    private View mEndView;
    private RNSharedElementDrawable mStartDrawable;
    private RNSharedElementDrawable mEndDrawable;

    public RNSharedElementTransition(ThemedReactContext context, RNSharedElementNodeManager nodeManager) {
        super(context);
        mItems.add(new RNSharedElementTransitionItem(nodeManager, "startAncestor", true));
        mItems.add(new RNSharedElementTransitionItem(nodeManager, "endAncestor", true));
        mItems.add(new RNSharedElementTransitionItem(nodeManager, "startNode", false));
        mItems.add(new RNSharedElementTransitionItem(nodeManager, "endNode", false));

        mStartView = new View(context);
        mStartView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mStartDrawable = new RNSharedElementDrawable();
        mStartView.setBackground(mStartDrawable);
        addView(mStartView);

        mEndView = new View(context);
        mEndView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mEndDrawable = new RNSharedElementDrawable();
        mEndView.setBackground(mEndDrawable);
        addView(mEndView);
    }

    public void releaseData() {
        for (RNSharedElementTransitionItem item : mItems) {
            item.setNode(null);
        }
    }

    public void setStartNode(RNSharedElementNode node) {
        mItems.get(ITEM_START).setNode(node);
        requestStylesAndContent(false);
    }

    public void setEndNode(RNSharedElementNode node) {
        mItems.get(ITEM_END).setNode(node);
        requestStylesAndContent(false);
    }

    public void setStartAncestor(RNSharedElementNode node) {
        mItems.get(ITEM_START_ANCESTOR).setNode(node);
        requestStylesAndContent(false);
    }

    public void setEndAncestor(RNSharedElementNode node) {
        mItems.get(ITEM_END_ANCESTOR).setNode(node);
        requestStylesAndContent(false);
    }

    public void setAnimation(final String animation) {
        if (mAnimation != animation) {
            mAnimation = animation;
            updateLayout();
        }
    }

    public void setNodePosition(final float nodePosition) {
        if (mNodePosition != nodePosition) {
            mNodePosition = nodePosition;
            //Log.d(LOG_TAG, "setNodePosition " + nodePosition + ", mInitialLayoutPassCompleted: " + mInitialLayoutPassCompleted);
            updateLayout();
        }
    }

    /*@Override
    // @SuppressLint("MissingSuperCall")
    public void requestLayout() {
        // No-op, terminate `requestLayout` here, UIManagerModule handles laying out children and
        // `layout` is called on all RN-managed views by `NativeViewHierarchyManager`
    }*/

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (!mReactLayoutSet) {
            mReactLayoutSet = true;

            // TODO - do this later after the whole layout pass
            // has completed
            requestStylesAndContent(true);
            mInitialLayoutPassCompleted = true;
            updateLayout();
            updateNodeVisibility();
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        //Log.d(LOG_TAG, "dispatchDraw, mRequiresClipping: " + mRequiresClipping + ", width: " + getWidth() + ", height: " + getHeight());
        if (mRequiresClipping) {
            canvas.clipRect(0, 0, getWidth(), getHeight());
        }
        super.dispatchDraw(canvas);

        // Draw content
        //Paint backgroundPaint = new Paint();
        //backgroundPaint.setColor(Color.argb(128, 255, 0, 0));
        //canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);
    }

    private void requestStylesAndContent(boolean force) {
        if (!mInitialLayoutPassCompleted && !force) return;
        for (final RNSharedElementTransitionItem item : mItems) {
            if (item.getNeedsStyle()) {
                item.setNeedsStyle(false);
                item.getNode().requestStyle(new Callback() {
                    @Override
                    public void invoke(Object... args) {
                        RNSharedElementStyle style = (RNSharedElementStyle) args[0];
                        item.setStyle(style);
                        updateLayout();
                        updateNodeVisibility();
                    }
                });
            }
            if (item.getNeedsContent()) {
                item.setNeedsContent(false);
                item.getNode().requestContent(new Callback() {
                    @Override
                    public void invoke(Object... args) {
                        RNSharedElementContent content = (RNSharedElementContent) args[0];
                        item.setContent(content);
                        updateLayout();
                        updateNodeVisibility();
                    }
                });
            }
        }
    }

    private void updateViewAndDrawable(
        View view,
        RNSharedElementDrawable drawable,
        Rect layout,
        Rect parentLayout,
        RNSharedElementContent content,
        Rect originalLayout,
        RNSharedElementStyle style,
        float alpha,
        float position) {

        // Update drawable
        boolean useScaling = drawable.update(content, style, position);
        if (useScaling) {

            // Update view
            view.layout(
                layout.left - parentLayout.left,
                layout.top - parentLayout.top,
                (layout.left - parentLayout.left) + originalLayout.width(),
                (layout.top - parentLayout.top) + originalLayout.height()
            );

            // Update scale
            float scaleX = (float)layout.width() / (float)originalLayout.width();
            float scaleY = (float)layout.height() / (float)originalLayout.height();
            if ((scaleX >= 1) && (scaleY >= 1)) {
                scaleX = Math.min(scaleX, scaleY);
                scaleY = scaleX;
            } else if ((scaleX <= 1) && (scaleY <= 1)) {
                scaleX = Math.max(scaleX, scaleY);
                scaleY = scaleX;
            }
            view.setScaleX(scaleX);
            view.setScaleY(scaleY);
        }
        else {

            // Update view
            view.layout(
                layout.left - parentLayout.left,
                layout.top - parentLayout.top,
                (layout.left - parentLayout.left) + layout.width(),
                (layout.top - parentLayout.top) + layout.height()
            );   
        }

        // Update view opacity and elevation
        view.setAlpha(alpha);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            view.setElevation(style.elevation);
        }
    }

    private void updateLayout() {
        if (!mInitialLayoutPassCompleted) return;

        // Local data
        RNSharedElementTransitionItem startItem = mItems.get(ITEM_START);
        RNSharedElementTransitionItem startAncestor = mItems.get(ITEM_START_ANCESTOR);
        RNSharedElementTransitionItem endItem = mItems.get(ITEM_END);
        RNSharedElementTransitionItem endAncestor = mItems.get(ITEM_END_ANCESTOR);

        // Get styles & content
        RNSharedElementStyle startStyle = startItem.getStyle();
        RNSharedElementStyle endStyle = endItem.getStyle();
        if ((startStyle == null) && (endStyle == null)) return;
        RNSharedElementContent startContent = startItem.getContent();
        RNSharedElementContent endContent = endItem.getContent();

        // Get layout
        getLocationOnScreen(mParentLocation);
        Rect startLayout = (startStyle != null) ? normalizeLayout(startStyle.layout, startAncestor) : new Rect();
        Rect endLayout = (endStyle != null) ? normalizeLayout(endStyle.layout, endAncestor) : new Rect();
        Rect parentLayout = new Rect(startLayout);
        parentLayout.union(endLayout);

        // APPLY CLIPPING

        /*Rect startClippedLayout = (startStyle != null) ? normalizeLayout(startItem.getClippedLayout(startAncestor), startAncestor) : new Rect();
        Rect startClipInsets = getClipInsets(startLayout, startClippedLayout);
        Rect endClippedLayout = (endStyle != null) ? normalizeLayout(endItem.getClippedLayout(endAncestor), endAncestor) : new Rect();
        Rect endClipInsets = getClipInsets(endLayout, endClippedLayout);*/

        // Get interpolated layout
        Rect interpolatedLayout;
        //Rect interpolatedClipInsets;
        RNSharedElementStyle interpolatedStyle;
        if ((startStyle != null) && (endStyle != null)) {
            interpolatedLayout = getInterpolatedLayout(startLayout, endLayout, mNodePosition);
            //interpolatedClipInsets = getInterpolatedClipInsets(interpolatedLayout, startClipInsets, startClippedLayout, endClipInsets, endClippedLayout, mNodePosition);
            interpolatedStyle = getInterpolatedStyle(startStyle, startContent, endStyle, endContent, mNodePosition);
        } else if (startStyle != null) {
            interpolatedLayout = startLayout;
            //interpolatedClipInsets = startClipInsets;
            interpolatedStyle = startStyle;
        } else {
            interpolatedLayout = endLayout;
            //interpolatedClipInsets = endClipInsets;
            interpolatedStyle = endStyle;
        }

        // Calculate clipped layout
        mRequiresClipping = !parentLayout.contains(interpolatedLayout);

        // Update outer viewgroup layout. The outer viewgroup hosts 2 inner views
        // which draw the content & elevation. The outer viewgroup performs additional
        // clipping on these views.
        super.layout(
            0,
            0,
            parentLayout.width(),
            parentLayout.height()
        );
        setTranslationX(parentLayout.left);
        setTranslationY(parentLayout.top);

        // Render the start view
        boolean isCrossFade = !mAnimation.equals("move");
        float startAlpha = !isCrossFade
            ? interpolatedStyle.opacity
            : ((startStyle != null) ? startStyle.opacity : 1) * (1 - mNodePosition);
        updateViewAndDrawable(
            mStartView,
            mStartDrawable,
            interpolatedLayout,
            parentLayout,
            startContent,
            startLayout,
            interpolatedStyle,
            startAlpha,
            mNodePosition
        );
        
        // Render the end view as well for the "cross-fade" animations
        if (isCrossFade) {

            // Render the end view
            float endAlpha = ((endStyle != null) ? endStyle.opacity : 1) * mNodePosition;
            updateViewAndDrawable(
                mEndView,
                mEndDrawable,
                interpolatedLayout,
                parentLayout,
                endContent,
                endLayout,
                interpolatedStyle,
                endAlpha,
                mNodePosition
            );

            // Also apply a fade effect on the elevation. This reduces the shadow visibility
            // underneath the view which becomes visible when the transparency of the view
            // is set. This in turn makes the shadow very visible and gives the whole view
            // a "grayish" appearance. The following code tries to reduce that visual artefact.
            if (interpolatedStyle.elevation > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    mStartView.setOutlineAmbientShadowColor(Color.argb(startAlpha, 0, 0, 0));
                    mStartView.setOutlineSpotShadowColor(Color.argb(startAlpha, 0, 0, 0));
                    mEndView.setOutlineAmbientShadowColor(Color.argb(endAlpha, 0, 0, 0));
                    mEndView.setOutlineSpotShadowColor(Color.argb(endAlpha, 0, 0, 0));
                }
            }
        }
    }

    private void updateNodeVisibility() {
        for (RNSharedElementTransitionItem item : mItems) {
            boolean hidden = mInitialLayoutPassCompleted &&
                (item.getStyle() != null) &&
                (item.getContent() != null) &&
                !item.isAncestor();
            item.setHidden(hidden);
        }
    }

    private Rect normalizeLayout(Rect layout, RNSharedElementTransitionItem ancestor) {
        RNSharedElementStyle style = ancestor.getStyle();
        if (style == null) {
            return new Rect(
                layout.left - mParentLocation[0],
                layout.top - mParentLocation[1],
                layout.right - mParentLocation[0],
                layout.bottom - mParentLocation[1]
            );
        }

        Rect ancestorLayout = style.layout;
        return new Rect(
            layout.left - ancestorLayout.left,
            layout.top - ancestorLayout.top,
            layout.right - ancestorLayout.left,
            layout.bottom - ancestorLayout.top
        );
    }

    private Rect getClipInsets(Rect layout, Rect clippedLayout) {
        return new Rect(
            (clippedLayout.left > layout.left) ? clippedLayout.left : 0,
            (clippedLayout.top > layout.top) ? clippedLayout.top : 0,
            (clippedLayout.right < layout.right) ? clippedLayout.right : 0,
            (clippedLayout.bottom < layout.bottom) ? clippedLayout.bottom : 0
        );
    }


    private Rect getInterpolatedClipInsets(
        Rect interpolatedLayout,
        Rect startClipInsets,
        Rect startClippedLayout,
        Rect endClipInsets,
        Rect endClippedLayout,
        float position) {
        Rect clipInsets = new Rect();

        // Top
        if ((endClipInsets.top == 0) && (startClipInsets.top != 0) && (startClippedLayout.top <= endClippedLayout.top)) {
            clipInsets.top = Math.max(0, startClippedLayout.top - interpolatedLayout.top);
        } else if ((startClipInsets.top == 0) && (endClipInsets.top != 0) && (endClippedLayout.top <= startClippedLayout.top)) {
            clipInsets.top = Math.max(0, endClippedLayout.top - interpolatedLayout.top);
        } else {
            clipInsets.top = (int) (startClipInsets.top + ((endClipInsets.top - startClipInsets.top) * position));
        }

        // Bottom
        if ((endClipInsets.bottom == 0) && (startClipInsets.bottom != 0) && (startClippedLayout.bottom >= endClippedLayout.bottom)) {
            clipInsets.bottom = Math.max(0, interpolatedLayout.bottom - startClippedLayout.bottom);
        } else if ((startClipInsets.bottom == 0) && (endClipInsets.bottom != 0) && (endClippedLayout.bottom >= startClippedLayout.bottom)) {
            clipInsets.bottom = Math.max(0, interpolatedLayout.bottom - endClippedLayout.bottom);
        } else {
            clipInsets.bottom = (int) (startClipInsets.bottom + ((endClipInsets.bottom - startClipInsets.bottom) * position));
        }

        // Left
        if ((endClipInsets.left == 0) && (startClipInsets.left != 0) && (startClippedLayout.left <= endClippedLayout.left)) {
            clipInsets.left = Math.max(0, startClippedLayout.left - interpolatedLayout.left);
        } else if ((startClipInsets.left == 0) && (endClipInsets.left != 0) && (endClippedLayout.left <= startClippedLayout.left)) {
            clipInsets.left = Math.max(0, endClippedLayout.left - interpolatedLayout.left);
        } else {
            clipInsets.left = (int) (startClipInsets.left + ((endClipInsets.left - startClipInsets.left) * position));
        }

         // Right
        if ((endClipInsets.right == 0) && (startClipInsets.right != 0) && (startClippedLayout.right >= endClippedLayout.right)) {
            clipInsets.right = Math.max(0, interpolatedLayout.right - startClippedLayout.right);
        } else if ((startClipInsets.right == 0) && (endClipInsets.right != 0) && (endClippedLayout.right >= startClippedLayout.right)) {
            clipInsets.right = Math.max(0, interpolatedLayout.right - endClippedLayout.right);
        } else {
            clipInsets.right = (int) (startClipInsets.right + ((endClipInsets.right - startClipInsets.right) * position));
        }

        return clipInsets;
    }

    private Rect getInterpolatedLayout(Rect layout1, Rect layout2, float position) {
        return new Rect(
            (int) (layout1.left + ((layout2.left - layout1.left) * position)),
            (int) (layout1.top + ((layout2.top - layout1.top) * position)),
            (int) (layout1.right + ((layout2.right - layout1.right) * position)),
            (int) (layout1.bottom + ((layout2.bottom - layout1.bottom) * position))
        );
    }

    private int getInterpolatedColor(int color1, int color2, float position) {
        int redA = Color.red(color1);
        int greenA = Color.green(color1);
        int blueA = Color.blue(color1);
        int alphaA = Color.alpha(color1);
        int redB = Color.red(color2);
        int greenB = Color.green(color2);
        int blueB = Color.blue(color2);
        int alphaB = Color.alpha(color2);
        return Color.argb(
            (int) (alphaA + ((alphaB - alphaA) * position)),
            (int) (redA + ((redB - redA) * position)),
            (int) (greenA + ((greenB - greenA) * position)),
            (int) (blueA + ((blueB - blueA) * position))
        );
    }

    private RNSharedElementStyle getInterpolatedStyle(
        RNSharedElementStyle style1,
        RNSharedElementContent content1,
        RNSharedElementStyle style2,
        RNSharedElementContent content2,
        float position
    ) {
        RNSharedElementStyle result = new RNSharedElementStyle();
        result.scaleType = RNSharedElementStyle.getInterpolatingScaleType(style1, style2, position);
        result.opacity = style1.opacity + ((style2.opacity - style1.opacity) * position);
        result.backgroundColor = getInterpolatedColor(style1.backgroundColor, style2.backgroundColor, position);
        result.borderTopLeftRadius = style1.borderTopLeftRadius + ((style2.borderTopLeftRadius - style1.borderTopLeftRadius) * position);
        result.borderTopRightRadius = style1.borderTopRightRadius + ((style2.borderTopRightRadius - style1.borderTopRightRadius) * position);
        result.borderBottomLeftRadius = style1.borderBottomLeftRadius + ((style2.borderBottomLeftRadius - style1.borderBottomLeftRadius) * position);
        result.borderBottomRightRadius = style1.borderBottomRightRadius + ((style2.borderBottomRightRadius - style1.borderBottomRightRadius) * position);
        result.borderWidth = style1.borderWidth + ((style2.borderWidth - style1.borderWidth) * position);
        result.borderColor = getInterpolatedColor(style1.borderColor, style2.borderColor, position);
        result.borderStyle = style1.borderStyle;
        result.elevation = style1.elevation + ((style2.elevation - style1.elevation) * position);
        return result;
    }

    private void fireMeasureEvent() {
        /*ReactContext reactContext = (ReactContext)getContext();
        WritableMap eventData = Arguments.createMap();
        WritableMap layoutData = Arguments.createMap();
        layoutData.putFloat();
        //eventData.putString("message", "MyMessage");
        eventData.putString("node", item.name);
        eventData.putMap("layout", layoutData)
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
            getId(),
            "onMeasure",
            eventData);*/

        /*
        - (void) fireMeasureEvent:(RNSharedElementTransitionItem*) item layout:(CGRect)layout visibleLayout:(CGRect)visibleLayout contentLayout:(CGRect)contentLayout
{
    if (!self.onMeasureNode) return;
    NSDictionary* eventData = @{
                                @"node": item.name,
                                @"layout": @{
                                        @"x": @(layout.origin.x),
                                        @"y": @(layout.origin.y),
                                        @"width": @(layout.size.width),
                                        @"height": @(layout.height()),
                                        @"visibleX": @(visibleLayout.origin.x),
                                        @"visibleY": @(visibleLayout.origin.y),
                                        @"visibleWidth": @(visibleLayout.size.width),
                                        @"visibleHeight": @(visibleLayout.height()),
                                        @"contentX": @(contentLayout.origin.x),
                                        @"contentY": @(contentLayout.origin.y),
                                        @"contentWidth": @(contentLayout.size.width),
                                        @"contentHeight": @(contentLayout.height()),
                                        },
                                @"contentType": item.contentTypeName,
                                @"style": @{
                                        @"borderRadius": @(item.style.cornerRadius)
                                        }
                                };
    self.onMeasureNode(eventData);
}*/
    }
}