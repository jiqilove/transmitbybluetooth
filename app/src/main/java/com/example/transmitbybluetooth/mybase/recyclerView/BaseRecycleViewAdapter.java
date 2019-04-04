package com.example.transmitbybluetooth.mybase.recyclerView;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.List;

public abstract class BaseRecycleViewAdapter<T> extends RecyclerView.Adapter<BaseViewHolder> {

    private Context context;
    private LayoutInflater inflater;
    private List<T> date;
    private int layoutId;

    //点击事件等等


    public BaseRecycleViewAdapter(Context context, List<T> date, int layoutId) {
        this.context = context;
        this.date = date;
        this.layoutId = layoutId;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        BaseViewHolder baseViewHolder = new BaseViewHolder(inflater.inflate(layoutId, viewGroup, false));
        return baseViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder baseViewHolder, int i) {
        bindData(baseViewHolder,date.get(i), i);
    }

    @Override
    public int getItemCount() {
        return date == null ? 0 : (date.size() );
    }

    protected abstract void bindData(BaseViewHolder baseViewHolder, T date,int position);

}
