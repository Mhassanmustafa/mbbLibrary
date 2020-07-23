package com.github.react.mbbLibrary.adapter;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.react.mbbLibrary.R;
import com.github.react.mbbLibrary.entry.MainBean;

import java.util.List;

public class MainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context mContext;
    private List<MainBean> mList;

    public MainAdapter(Context context, List<MainBean> list){
        this.mContext = context;
        this.mList = list;

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.main_item,parent,false);
        return new MainViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        MainBean bean = mList.get(position);
        if (null == bean)
            return;
        MainViewHolder viewHolder = (MainViewHolder) holder;

        viewHolder.tv_title.setText(bean.getTitle());
        viewHolder.tv_describe.setText(bean.getDescribe());
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public static class MainViewHolder extends  RecyclerView.ViewHolder{
        public TextView tv_title;
        public TextView tv_describe;

        public MainViewHolder(View itemView){
            super(itemView);
            tv_title = (TextView) itemView.findViewById(R.id.main_item_tv_title);
            tv_describe = (TextView) itemView.findViewById(R.id.main_item_tv_describe);
        }
    }

}
