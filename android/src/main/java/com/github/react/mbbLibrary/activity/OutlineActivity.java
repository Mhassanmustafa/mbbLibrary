package com.github.react.mbbLibrary.activity;

import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artifex.mupdfdemo.OutlineActivityData;
import com.artifex.mupdfdemo.OutlineItem;
import com.facebook.react.ReactActivity;
import com.github.react.mbbLibrary.R;
import com.github.react.mbbLibrary.adapter.OutlineAdapter;
import com.github.react.mbbLibrary.widget.OnRecyclerItemClickListener;


public class OutlineActivity extends ReactActivity {

    private RecyclerView recyclerView;
    private OutlineAdapter adapter;
    private OutlineItem mItems[];
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outline);

        initData();
        initView();
        setListener();
    }


    private void initData(){
        mItems = OutlineActivityData.get().items;
    }


    private void initView(){
        adapter = new OutlineAdapter(this,mItems);
        recyclerView = (RecyclerView)findViewById(R.id.outline_rv);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);//HORIZONTAL 水平
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);
    }


    private void setListener(){
        recyclerView.addOnItemTouchListener(new OnRecyclerItemClickListener(recyclerView) {
            @Override
            public void onItemClick(RecyclerView.ViewHolder vh) {
                OutlineActivityData.get().position = vh.getLayoutPosition();
                setResult(mItems[vh.getLayoutPosition()].page);
                finish();
            }

            @Override
            public void onItemLongClick(RecyclerView.ViewHolder vh) {

            }
        });
    }
}
