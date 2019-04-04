package com.example.transmitbybluetooth.mybase.recyclerView;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;

public class BaseViewHolder extends RecyclerView.ViewHolder {

    private SparseArray<View> views;

    public BaseViewHolder(@NonNull View itemView) {
        super(itemView);
        this.views = new SparseArray<>();
    }

    public <T extends View> T getView(int viewId) {
        View view = views.get(viewId);
        if (view == null) {
            view = itemView.findViewById(viewId);
            views.put(viewId, view);
        }
        return (T) view;
    }

    public <T extends View> T setText(int viewId, String string) {
        View view = getView(viewId);
        if (view instanceof TextView) {
            ((TextView) view).setText(string);
            return (T) view;
        }
        return (T) view;
    }

    public <T extends View> T setVisiable(int viewId, int visibility) {
        View view = getView(viewId);
           view.setVisibility(visibility);
        return (T) view;
    }

    public <T extends View> T setOnClickListener(int viewId, View.OnClickListener onClickListener) {
        View view = getView(viewId);
        view.setOnClickListener(onClickListener);
        return (T) view;
    }

      public <T extends View> T setOnLongClickListener(int viewId, View.OnLongClickListener onLongClickListener) {
        View view = getView(viewId);
        view.setOnLongClickListener(onLongClickListener);
        return (T) view;
    }



    public View getRootView() {
        return itemView;
    }

}
