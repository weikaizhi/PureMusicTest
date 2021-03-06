package com.example.kaizhiwei.puremusictest.ui.netmusic.tuijian;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.kaizhiwei.puremusictest.CommonUI.CommonTitleView;
import com.example.kaizhiwei.puremusictest.R;
import com.example.kaizhiwei.puremusictest.base.MyBaseActivity;
import com.example.kaizhiwei.puremusictest.bean.ArtistGetListBean;
import com.example.kaizhiwei.puremusictest.constant.PureMusicContant;
import com.example.kaizhiwei.puremusictest.contract.ArtistGetArtistListInfoContract;
import com.example.kaizhiwei.puremusictest.presenter.ArtistGetArtistListInfoPresenter;
import com.example.kaizhiwei.puremusictest.widget.RecyclerViewDividerDecoration;
import com.jcodecraeer.xrecyclerview.ProgressStyle;
import com.jcodecraeer.xrecyclerview.XRecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;

/**
 * Created by kaizhiwei on 17/6/29.
 */

public class ArtistArtistListActivity extends MyBaseActivity implements ArtistGetArtistListInfoContract.View, ArtistIndexDialog.IArtistIndexDialogListener{
    @Bind(R.id.rvArtistList)
    XRecyclerView rvArtistList;

    @Bind(R.id.tvIndexType)
    TextView tvIndexType;

    @Bind(R.id.commonTitle)
    CommonTitleView commonTitle;

    ArtistGetArtistListInfoContract.Presenter mPresenter;
    private ArtistGetListBean mArtistGetListBean = new ArtistGetListBean();
    private ArtistSelActivity.ArtistTagInfo mArtistTagInfo;
    private int mCurPage = 0;
    private MyAdapter mAdapter;
    private String mFilter = "";
    private boolean mNeedClear = false;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    public static final String BUNDLE_ARTISTTAGINFO= "BUNDLE_ArtistTagInfo";

    @Override
    public int getLayoutId() {
        return R.layout.activity_artist_artistlist;
    }

    @Override
    public void initPresenter() {
        mPresenter = new ArtistGetArtistListInfoPresenter(this);
    }

    @Override
    public void initView() {
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rvArtistList.setPullRefreshEnabled(false);
        rvArtistList.setLoadingMoreEnabled(true);
        rvArtistList.setLayoutManager(linearLayoutManager);
        rvArtistList.addItemDecoration(new RecyclerViewDividerDecoration(this, RecyclerViewDividerDecoration.HORIZONTAL_LIST));
//        rvArtistList.setOnScrollListener(new RecyclerView.OnScrollListener(){
//            @Override
//            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
//                super.onScrollStateChanged(recyclerView, newState);
//            }
//
//            @Override
//            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
//                super.onScrolled(recyclerView, dx, dy);
//
//                if (dy > 0) //向下滚动
//                {
//                    int visibleItemCount = linearLayoutManager.getChildCount();
//                    int totalItemCount = linearLayoutManager.getItemCount();
//                    int pastVisiblesItems = linearLayoutManager.findFirstVisibleItemPosition();
//
//                    if (!mLoadingData && (visibleItemCount + pastVisiblesItems) >= totalItemCount) {
//                        mLoadingData = true;
//                        loadMoreDate();
//                    }
//                }
//            }
//        });

        rvArtistList.setLoadingMoreProgressStyle(ProgressStyle.BallPulse);
        rvArtistList.setLoadingListener(new XRecyclerView.LoadingListener() {
            @Override
            public void onRefresh() {

            }

            @Override
            public void onLoadMore() {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadMoreDate();
                    }
                }, 500);
            }
        });
        mAdapter = new MyAdapter();
        rvArtistList.setAdapter(mAdapter);

        commonTitle.setTitleVisible(false);
        if(mArtistTagInfo != null){
            commonTitle.setTitleViewInfo(mArtistTagInfo.title, "", "");
        }
        commonTitle.setTitleViewListener(new CommonTitleView.onTitleClickListener() {
            @Override
            public void onLeftBtnClicked() {
                finish();
            }

            @Override
            public void onRightBtnClicked() {
                ArtistIndexDialog.Builder builder = new ArtistIndexDialog.Builder(ArtistArtistListActivity.this);
                ArtistIndexDialog dialog = builder.create();
                dialog.setListener(ArtistArtistListActivity.this);
                dialog.setSelectIndex(mFilter);
                dialog.show();
            }
        });
    }

    private void loadMoreDate() {
        int offset = 0;
        int limit = 0;
        if(mCurPage == 0){
            offset = 0;
            limit = 48;
        }
        else{
            offset = (mCurPage + 1)*30;
            limit = 30;
        }
        Log.e("weikaizhi", "offset = " + offset + ", limit = " + limit);
        mCurPage++;
        if(mPresenter != null){
            if(TextUtils.isEmpty(mFilter)){
                mPresenter.getArtistListInfo(PureMusicContant.DEVICE_TYPE, PureMusicContant.APP_VERSION, PureMusicContant.CHANNEL, "2", "baidu.ting.artist.getList"
                        , PureMusicContant.FORMAT_JSON, offset + "", limit+"", "1", mArtistTagInfo.area + "", mArtistTagInfo.sex + "");
            }
            else{
                mPresenter.getArtistListInfoWithFilter(PureMusicContant.DEVICE_TYPE, PureMusicContant.APP_VERSION, PureMusicContant.CHANNEL, "2", "baidu.ting.artist.getList"
                        , PureMusicContant.FORMAT_JSON, offset + "", limit+"", "1", mArtistTagInfo.area + "", mArtistTagInfo.sex + "", mFilter);
            }
        }
    }

    @Override
    public void initData() {
        List<ArtistGetListBean.ArtistBean> list = new ArrayList<>();
        mArtistGetListBean.setArtist(list);

        Intent intent = getIntent();
        if(intent == null)
            return;

        Bundle bundle = intent.getExtras();
        if(bundle == null)
            return;

        mArtistTagInfo = (ArtistSelActivity.ArtistTagInfo)bundle.getParcelable(BUNDLE_ARTISTTAGINFO);
        if(mPresenter != null){
            mPresenter.getArtistListInfo(PureMusicContant.DEVICE_TYPE, PureMusicContant.APP_VERSION, PureMusicContant.CHANNEL, "2", "baidu.ting.artist.getList"
                    , PureMusicContant.FORMAT_JSON, mCurPage*48 + "", "48", "1", mArtistTagInfo.area + "", mArtistTagInfo.sex + "");
        }
    }

    @Override
    public void onError(String strErrMsg) {
        showToast(strErrMsg);
        rvArtistList.loadMoreComplete();
    }

    @Override
    public void onGetArtistListInfoSuccess(ArtistGetListBean bean) {
        if(bean == null || bean.getArtist() == null){
            showToast("data error");
            return;
        }

        rvArtistList.loadMoreComplete();
        if(mNeedClear){
            mArtistGetListBean.getArtist().clear();
            mNeedClear = false;
        }
        mArtistGetListBean.getArtist().addAll(bean.getArtist());
        mAdapter.notifyDataSetChanged();
        Log.e("weikaizhi", "ArtistGetListBean size = " + bean.getArtist().size() + ", mArtistGetListBean = " + mArtistGetListBean.getArtist().size());
    }

    @Override
    public void onItemClick(int position, String key, String value) {
        mCurPage = 0;
        mFilter = key;
        tvIndexType.setText(value);
        mNeedClear = true;
        loadMoreDate();
    }

    private class MyAdapter extends RecyclerView.Adapter<MyViewHolder>{

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(ArtistArtistListActivity.this);
            View view = inflater.inflate(R.layout.item_artist_list_view, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, final int position) {
            ArtistGetListBean.ArtistBean bean = mArtistGetListBean.getArtist().get(position);
            if(bean == null)
                return;

            holder.llContent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(ArtistArtistListActivity.this, ArtistSongListActivity.class);
                    intent.putExtra(ArtistSongListActivity.ARTIST_BEAN, mArtistGetListBean.getArtist().get(position));
                    startActivity(intent);
                }
            });
            holder.tvArtistName.setText(bean.getName());
            Glide.with(ArtistArtistListActivity.this).load(bean.getAvatar_middle()).error(R.drawable.default_live_ic).into(holder.ivArtistPic);
        }

        @Override
        public int getItemCount() {
            if(mArtistGetListBean == null || mArtistGetListBean.getArtist() == null)
                return 0;

            return mArtistGetListBean.getArtist().size();
        }
    }

    private class MyViewHolder extends RecyclerView.ViewHolder{
        private ImageView ivArtistPic;
        private TextView tvArtistName;
        private LinearLayout llContent;

        public MyViewHolder(View itemView) {
            super(itemView);
            llContent = (LinearLayout)itemView.findViewById(R.id.llContent);
            ivArtistPic = (ImageView)itemView.findViewById(R.id.ivArtistPic);
            tvArtistName = (TextView)itemView.findViewById(R.id.tvArtistName);
        }
    }
}
