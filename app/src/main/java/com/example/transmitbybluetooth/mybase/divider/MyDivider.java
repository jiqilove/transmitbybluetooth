package com.example.transmitbybluetooth.mybase.divider;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.example.transmitbybluetooth.R;
import com.example.transmitbybluetooth.mybase.DisplayUtil;


public class MyDivider extends RecyclerView.ItemDecoration {

    private int dividerHeight;
    private Paint dividerPaint;
    private int span = 0;
    private boolean drawGrid = false;
    private RecyclerView recyclerView;


    //====默认颜色和默认分割线 color e5e5e5  分割线高度3px
    public MyDivider(Context context, RecyclerView recyclerView) {
        dividerPaint = new Paint();
        dividerPaint.setColor(context.getResources().getColor(R.color.gary_e5e5e5));
        dividerHeight = 3;
        this.recyclerView = recyclerView;
        span = getSpanCount(recyclerView);
    }

    public MyDivider(Context context, RecyclerView recyclerView, boolean drawGrid) {
        dividerPaint = new Paint();
        dividerPaint.setColor(context.getResources().getColor(R.color.gary_e5e5e5));
        dividerHeight = 3;
        this.drawGrid = drawGrid;
        this.recyclerView = recyclerView;
        span = getSpanCount(recyclerView);
    }

    // ====自定义颜色和分割线颜色====
    public MyDivider(Context context, int color, int dividerHeightOfDp, RecyclerView recyclerView) {
        dividerPaint = new Paint();
        dividerPaint.setColor(color);
        dividerHeight = DisplayUtil.px2dp(context, dividerHeightOfDp);
        this.recyclerView = recyclerView;
        span = getSpanCount(recyclerView);
    }

    public MyDivider(Context context, int color, int dividerHeightOfDp, boolean drawGrid, RecyclerView recyclerView) {
        dividerPaint = new Paint();
        dividerPaint.setColor(color);
        dividerHeight = DisplayUtil.px2dp(context, dividerHeightOfDp);
        this.drawGrid = drawGrid;
        this.recyclerView = recyclerView;
        span = getSpanCount(recyclerView);
    }


    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        outRect.bottom = dividerHeight;
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (drawGrid) {
            drawVertical(c, parent);
        }
        drawHorizontal(c, parent);
    }

    //垂直线条
    private void drawVertical(Canvas c, RecyclerView parent) {
        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount - 1; i++) {
            if ((i + 1) % span == 0) { //如果是最后一列，直接下一个子view·
                continue;
            }
            View view = parent.getChildAt(i);
            int top = view.getTop();
            int bottom = view.getBottom();
            float left = view.getRight();
            float right = view.getRight() + dividerHeight;
            Log.e("cnb", "drawVertical: " + top + "---" + bottom + "--" + left + "---" + right);
            c.drawRect(left, top, right, bottom, dividerPaint);
        }
    }

    //水平线条
    private void drawHorizontal(Canvas c, RecyclerView parent) {
        int childCount = parent.getChildCount();
        int unDrawCount = 1; //不需要绘制的子 view
        if (drawGrid) {
            unDrawCount = span;
        }
        for (int i = 0; i < childCount - unDrawCount; i++) {
            View view = parent.getChildAt(i);
            int left = view.getLeft();
            int right = view.getRight();
            float top = view.getBottom();
            float bottom = view.getBottom() + dividerHeight;
            c.drawRect(left, top, right, bottom, dividerPaint);

        }
    }


    private int getSpanCount(RecyclerView recyclerView) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof GridLayoutManager) {
            return ((GridLayoutManager) layoutManager).getSpanCount();
        } else {
            return 1;
        }
    }

    //是否最后一行
    private boolean isLastRow(int position) {
        int count = recyclerView.getChildCount();
        if (position < count) {
            return false;
        }
        return true;
    }

    //是否最后一列
    private boolean isLastColum(int position) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        int spanCount = span;

        if (layoutManager instanceof GridLayoutManager) {
            if ((position + 1) % spanCount == 0) {
                return true;
            }
        }
        return false;
    }

}