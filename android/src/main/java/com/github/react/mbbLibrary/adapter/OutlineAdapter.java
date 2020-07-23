package com.github.react.mbbLibrary.adapter;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.artifex.mupdfdemo.OutlineItem;
import com.github.react.mbbLibrary.R;


public class OutlineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context mContext;
    private OutlineItem mList[];

    public OutlineAdapter(Context context, OutlineItem mList[]){
        this.mContext = context;
        this.mList = mList;

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.outline_item,parent,false);
        return new OutlineViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        OutlineItem bean = mList[position];
        if (null == bean)
            return;
        OutlineViewHolder viewHolder = (OutlineViewHolder) holder;

        int level = bean.level;
        if (level > 8) level = 8;
        String space = "";
        for (int i=0; i<level;i++)
            space += "   ";
        String text = space + bean.title;

        viewHolder.tv_title.setText(text);
        viewHolder.tv_page.setText(String.valueOf(bean.page+1));
    }

    @Override
    public int getItemCount() {
        return mList.length;
    }
    public static class OutlineViewHolder extends  RecyclerView.ViewHolder{
        public TextView tv_title;
        public TextView tv_page;

        public OutlineViewHolder(View itemView){
            super(itemView);
            tv_title = (TextView) itemView.findViewById(R.id.outline_item_title);
            tv_page = (TextView) itemView.findViewById(R.id.outline_item_page);
        }
    }

}
