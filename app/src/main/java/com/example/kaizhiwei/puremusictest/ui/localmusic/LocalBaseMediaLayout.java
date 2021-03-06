package com.example.kaizhiwei.puremusictest.ui.localmusic;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.kaizhiwei.puremusictest.bean.SearchLrcPicBean;
import com.example.kaizhiwei.puremusictest.contract.LocalMusicContract;
import com.example.kaizhiwei.puremusictest.contract.SearchLrcPicContract;
import com.example.kaizhiwei.puremusictest.dao.FavoriteMusicDao;
import com.example.kaizhiwei.puremusictest.dao.MusicInfoDao;
import com.example.kaizhiwei.puremusictest.model.FavoriteMusicModel;
import com.example.kaizhiwei.puremusictest.model.IMediaDataObserver;
import com.example.kaizhiwei.puremusictest.model.MediaModel;
import com.example.kaizhiwei.puremusictest.presenter.LocalMusicPresenter;
import com.example.kaizhiwei.puremusictest.MediaData.MediaLibrary;
import com.example.kaizhiwei.puremusictest.MediaData.PreferenceConfig;
import com.example.kaizhiwei.puremusictest.R;
import com.example.kaizhiwei.puremusictest.presenter.SearchLrcPicPresenter;
import com.example.kaizhiwei.puremusictest.service.IPlayMusic;
import com.example.kaizhiwei.puremusictest.service.IPlayMusicListener;
import com.example.kaizhiwei.puremusictest.service.PlayMusicService;
import com.example.kaizhiwei.puremusictest.ui.favorite.FavoriteDialog;
import com.example.kaizhiwei.puremusictest.ui.home.HomeActivity;
import com.example.kaizhiwei.puremusictest.util.RingBellUtil;
import com.example.kaizhiwei.puremusictest.widget.RecyclerViewDividerDecoration;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import me.yokeyword.indexablerv.EntityWrapper;
import me.yokeyword.indexablerv.IndexableAdapter;
import me.yokeyword.indexablerv.IndexableLayout;
import me.yokeyword.indexablerv.RealAdapter;

/**
 * Created by kaizhiwei on 17/1/14.
 */
public class LocalBaseMediaLayout extends LinearLayout implements MediaLibrary.IMediaScanListener, MoreOperationDialog.IMoreOperationDialogListener,
        View.OnClickListener, LocalMusicContract.View, LocalAudioAdapter.ILocalAudioListener,
        IndexableAdapter.OnItemContentClickListener<LocalAudioAdapter.LocalAudioItemData>,
        IndexableAdapter.OnItemContentLongClickListener<LocalAudioAdapter.LocalAudioItemData>,
        PlayMusicService.Client.Callback, IPlayMusicListener, SearchLrcPicContract.View, IMediaDataObserver {
    private Context mContext;
    private IndexableLayout indexLayout;
    private LocalAudioAdapter mAdapter;

    private Handler mHandler = new Handler();
    private Vibrator vibrator;

    private PlayMusicService.Client mClient;
    private PlayMusicService mService;
    private MoreOperationDialog.Builder mMoreDialogbuilder = null;
    private MoreOperationDialog mMoreDialog = null;
    private List<Integer> mListMoreOperData;

    private LocalMusicPresenter mPresenter;
    private SearchLrcPicPresenter mSearchLrcPicPresenter;
    private String mSearchKey;

    //加载控件
    private RelativeLayout rlLoading;
    private ProgressBar view_loading;
    private ILocalBaseListener mListener;

    //删除文件对话框
    private AlertDialogDeleteOne mAlertDialogDeleteOne;
    private AlertDialogDeleteOne.IOnAlertDialogDeleteListener mAlertDialogDeleteListener = new AlertDialogDeleteOne.IOnAlertDialogDeleteListener() {
        @Override
        public void OnDeleteClick(AlertDialogDeleteOne dialog, boolean isDeleteFile) {
            if(dialog == null)
                return;

            List<MusicInfoDao> listMusicInfoDao = dialog.getMusicInfoDaoData();
            if(listMusicInfoDao == null || listMusicInfoDao.size() == 0)
                return;

            handleDeleteMedia(listMusicInfoDao, isDeleteFile);
        }
    };

    //隐藏对话框监听
    private AlertDialogHide.IAlertDialogHideListener mAlertDialogHideListener = new AlertDialogHide.IAlertDialogHideListener(){

        @Override
        public void onAlterDialogHideOk(String strFolderPath) {
            List<String> list = PreferenceConfig.getInstance().getScanFilterByFolderName();
            if(!list.contains(strFolderPath))
                list.add(strFolderPath);
            PreferenceConfig.getInstance().setScanFilterByFolderName(list);
        }
    };

    private List<MusicInfoDao> mMusicInfoDaos = new ArrayList<>();

    //对musicinfodao数据进行分类，文件夹、歌手、专辑使用此数据
    private Map<String, List<MusicInfoDao>> mCategaryMusicInfoDaos = new TreeMap<>(new Comparator<String>() {
        @Override
        public int compare(String lhs, String rhs) {
            return lhs.compareTo(rhs);
        }
    });

    private LayoutType mType = LayoutType.ALLSONG;

    @Override
    public void onSearchLrcPicSuccess(SearchLrcPicBean bean) {

    }

    @Override
    public void onMediaDataChanged(int type, long musicId) {
        //initAdapterData();
    }

    public enum LayoutType{
        ALLSONG, FOLDER, ARTIST, ALBUM, CUSTOME
    }

    private IndexableAdapter.IndexCallback<LocalAudioAdapter.LocalAudioItemData> indexCallback = new IndexableAdapter.IndexCallback<LocalAudioAdapter.LocalAudioItemData>() {
        @Override
        public void onFinished(List<EntityWrapper<LocalAudioAdapter.LocalAudioItemData>> datas) {
            if(mType == LayoutType.ALLSONG || mType == LayoutType.CUSTOME){
                List<MusicInfoDao> newList = new ArrayList<>();
                for(int i = 0;i < datas.size();i++){
                    LocalAudioAdapter.LocalAudioItemData itemData = datas.get(i).getData();
                    if(itemData == null)
                        continue;

                    for(int j = 0;j < mMusicInfoDaos.size();j++){
                        if(mMusicInfoDaos.get(j).get_id() == Long.parseLong(itemData.getId())){
                            newList.add(mMusicInfoDaos.get(j));
                        }
                    }
                }

                mMusicInfoDaos.clear();
                mMusicInfoDaos.addAll(newList);
                initSelItem();
            }
        }
    };

    public interface ILocalBaseListener{
        void onFragmentInitFinish(LinearLayout fragment);
        void onMoreOperClick(LocalBaseMediaLayout layout, int flag, Object obj);
    }

    public LocalBaseMediaLayout(Context context) {
        this(context, null, 0);
    }

    public LocalBaseMediaLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LocalBaseMediaLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        LayoutInflater layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        initView(layoutInflater);
    }

    public void setBaseMediaListener(ILocalBaseListener listener){
        mListener = listener;
    }

    public View initView(LayoutInflater inflater) {
        mPresenter = new LocalMusicPresenter(this);
        mSearchLrcPicPresenter = new SearchLrcPicPresenter(this);

        View rootView = inflater.inflate(R.layout.fragment_all_media, null);
        mClient = new PlayMusicService.Client(mContext, this);
        vibrator = (Vibrator)mContext.getSystemService(Context.VIBRATOR_SERVICE);

        indexLayout = (IndexableLayout) rootView.findViewById(R.id.indexLayout);
        indexLayout.setFastCompare(true);
        indexLayout.setStickyEnable(false);
        indexLayout.setLayoutManager(new LinearLayoutManager(mContext));
        indexLayout.setOverlayStyle_Center();
        indexLayout.setVisibility(View.GONE);
        indexLayout.getRecyclerView().addItemDecoration(new RecyclerViewDividerDecoration(mContext, RecyclerViewDividerDecoration.HORIZONTAL_LIST));
        indexLayout.getRecyclerView().setNestedScrollingEnabled(false);
        mAdapter = new LocalAudioAdapter(mContext);
        mAdapter.setListener(this);
        mAdapter.setOnItemContentClickListener(this);
        mAdapter.setOnItemContentLongClickListener(this);
        indexLayout.setAdapter(mAdapter);

        rlLoading = (RelativeLayout)rootView.findViewById(R.id.rlLoading);
        rlLoading.setVisibility(View.VISIBLE);
        view_loading = (ProgressBar)rootView.findViewById(R.id.view_loading);
        if(mListener != null){
            mListener.onFragmentInitFinish(this);
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        this.addView(rootView, params);
        return rootView;
    }

    public void onDestory(){
    }

    public void onResume(){
        MediaModel.getInstance().addObserver(this);
    }

    public void onPause() {
        MediaModel.getInstance().removeObserver(this);
    }

    public void onStart() {
        mClient.connect();
    }

    public void onStop() {
        mClient.disconnect();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
//        if (keyCode == event.KEYCODE_BACK) {
//            FragmentManager fragmentManager = mContext.getFragmentManager();
//            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
//            fragmentTransaction.hide(this);
//            fragmentTransaction.commit();
//            return true;
//        }

        return false;
    }

    public void setSearchKey(String strKey){
        mSearchKey = strKey;
        if(mPresenter == null)
            return;

        indexLayout.setVisibility(View.GONE);
        rlLoading.setVisibility(View.VISIBLE);
        if(mType == LayoutType.ALLSONG){
            mPresenter.queryMusicInfosByName(strKey);
        }
        else if(mType == LayoutType.ARTIST){
            mPresenter.queryMusicInfosByArist(strKey);
        }
        else if(mType == LayoutType.ALBUM){
            mPresenter.queryMuisicInfosByAlbum(strKey);
        }
        else if(mType == LayoutType.FOLDER){
            mPresenter.queryMusicInfosByFolder(strKey);
        }
    }

    public void setFilterFolder(String strFolderName){
//        if(mAllSongAdapter != null){
//            mAllSongAdapter.setFilterFolder(strFolderName);
//        }
    }

    public void setFilterArtist(String strArtistrName){
//        if(mAllSongAdapter != null){
//            mAllSongAdapter.setFilterArtist(strArtistrName);
//        }
    }
//
//    public void setFilterAlbum(String strAlbumName){
//        if(mPresenter != null){
//            mPresenter.setFilterAlbum(strAlbumName);
//        }
//    }

    public LayoutType getType() {
        return mType;
    }

    public void setType(LayoutType mType) {
        this.mType = mType;
        if(mAdapter == null){
            return;
        }

        int contentResId;
        switch (mType){
            case ALLSONG:
                contentResId = R.layout.item_local_audio_allsong;
                indexLayout.showAllLetter(true);
                indexLayout.setIndexBarVisibility(true);
                break;
            case FOLDER:
                contentResId = R.layout.item_local_audio_folder;
                indexLayout.showAllLetter(false);
                indexLayout.setIndexBarVisibility(false);
                indexLayout.showIndexTitle(false);
                break;
            case ARTIST:
            case ALBUM:
                contentResId = R.layout.item_local_audio_artist_album;
                indexLayout.showAllLetter(false);
                indexLayout.setIndexBarVisibility(false);
                indexLayout.showIndexTitle(false);
                break;
            case CUSTOME:
                indexLayout.showAllLetter(false);
                indexLayout.setIndexBarVisibility(false);
                indexLayout.showIndexTitle(false);
            default:
                contentResId = R.layout.item_local_audio_allsong;
                break;
        }
        mAdapter.setContentResId(contentResId);
    }

    public void initAdapterData(){
        indexLayout.setVisibility(View.GONE);
        rlLoading.setVisibility(View.VISIBLE);
        if(mType == LayoutType.ALLSONG){
            mPresenter.getAllMusicInfos();
        }
        else if(mType == LayoutType.FOLDER){
            mPresenter.getMusicInfosByFolder("");
        }
        else if(mType == LayoutType.ARTIST){
            mPresenter.getMusicInfosByArtist("");
        }
        else if(mType == LayoutType.ALBUM){
            mPresenter.getMusicInfosByAlbum("");
        }
    }

    public void initAdaterData(List<MusicInfoDao> list){
        onGetAllMusicInfos(list);
    }

    public void setMoreOperDialogData(List<Integer> list){
        if(list == null || list.size() ==0)
            return;

        if(mListMoreOperData == null){
            mListMoreOperData = new ArrayList<>();
        }
        mListMoreOperData.clear();
        mListMoreOperData.addAll(list);
    }

    @Override
    public void onError(String strErrMsg) {

    }

    void initSelItem(){
        if(mService == null)
            return;

        MusicInfoDao musicInfoDao = mService.getCurPlayMusicDao();
        if(musicInfoDao == null)
            return;

        mAdapter.setSelectItemId(musicInfoDao.get_id());
    }

    @Override
    public void onGetAllMusicInfos(final List<MusicInfoDao> list) {
        rlLoading.setVisibility(View.GONE);
        if(list == null)
            return;

        mMusicInfoDaos.clear();
        mMusicInfoDaos.addAll(list);

        if(list.size() >= 0){
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    indexLayout.setVisibility(View.VISIBLE);
                    List<LocalAudioAdapter.LocalAudioItemData> listData = new ArrayList<>();
                    for(int i = 0; i < mMusicInfoDaos.size();i++){
                        LocalAudioAdapter.LocalAudioItemData itemData = new LocalAudioAdapter.LocalAudioItemData();
                        itemData.setStrMain(mMusicInfoDaos.get(i).getTitle());
                        itemData.setStrSub(mMusicInfoDaos.get(i).getArtist());
                        itemData.setId(mMusicInfoDaos.get(i).get_id() + "");
                        listData.add(itemData);
                    }
                    mAdapter.setDatas(listData, indexCallback);

                    if(mService != null){
                        //MusicInfoDao curMusicInfoDao = mService.getCurrentMedia();
                        //if(curMusicInfoDao != null){
                        //   mAllSongAdapter.setItemPlayState(curMusicInfoDao, true);
                        //}
                    }
                }
            }, 50);
        }
        else{

        }
    }

    @Override
    public void onGetMusicInfosByFolder(Map<String, List<MusicInfoDao>> mapRet) {
        if(mapRet == null)
            return;

        indexLayout.setVisibility(View.VISIBLE);
        rlLoading.setVisibility(View.GONE);

        mCategaryMusicInfoDaos.clear();
        mCategaryMusicInfoDaos.putAll(mapRet);

        List<LocalAudioAdapter.LocalAudioItemData> listData = new ArrayList<>();
        Set<String> keys = mCategaryMusicInfoDaos.keySet();
        for(String key : keys){
            String[] strs = key.split(File.separator);
            List<MusicInfoDao> listDaos = mCategaryMusicInfoDaos.get(key);
            LocalAudioAdapter.LocalAudioItemData itemData = new LocalAudioAdapter.LocalAudioItemData();
            itemData.setStrMain(strs[strs.length-1]);
            itemData.setStrSub(listDaos.size() + "首");
            itemData.setStrThird(listDaos.get(0).getSave_path());
            itemData.setId(strs[strs.length-1]);
            listData.add(itemData);
        }
        mAdapter.setDatas(listData);
    }

    @Override
    public void onGetMusicInfosByArtist(Map<String, List<MusicInfoDao>> mapRet) {
        if(mapRet == null)
            return;

        indexLayout.setVisibility(View.VISIBLE);
        rlLoading.setVisibility(View.GONE);

        mCategaryMusicInfoDaos.clear();
        mCategaryMusicInfoDaos.putAll(mapRet);

        List<LocalAudioAdapter.LocalAudioItemData> listData = new ArrayList<>();
        Set<String> keys = mCategaryMusicInfoDaos.keySet();
        for(String key : keys){
            String[] strs = key.split(File.separator);
            List<MusicInfoDao> listDaos = mCategaryMusicInfoDaos.get(key);
            LocalAudioAdapter.LocalAudioItemData itemData = new LocalAudioAdapter.LocalAudioItemData();
            itemData.setStrMain(strs[strs.length-1]);
            itemData.setStrSub(listDaos.size() + "首");
            itemData.setStrThird(key);
            itemData.setId(key);
            listData.add(itemData);
        }
        mAdapter.setDatas(listData);
    }

    @Override
    public void onGetMusicInfosByAlbum(Map<String, List<MusicInfoDao>> mapRet) {
        if(mapRet == null)
            return;

        indexLayout.setVisibility(View.VISIBLE);
        rlLoading.setVisibility(View.GONE);

        mCategaryMusicInfoDaos.clear();
        mCategaryMusicInfoDaos.putAll(mapRet);

        List<LocalAudioAdapter.LocalAudioItemData> listData = new ArrayList<>();
        Set<String> keys = mCategaryMusicInfoDaos.keySet();
        for(String key : keys){
            String[] strs = key.split(File.separator);
            List<MusicInfoDao> listDaos = mCategaryMusicInfoDaos.get(key);
            LocalAudioAdapter.LocalAudioItemData itemData = new LocalAudioAdapter.LocalAudioItemData();
            itemData.setStrMain(strs[strs.length-1]);
            itemData.setStrSub(listDaos.size() + "首");
            itemData.setStrThird(key);
            itemData.setId(key);
            listData.add(itemData);
        }
        mAdapter.setDatas(listData);
    }

    @Override
    public void onQueryMusicInfosByFolder(List<MusicInfoDao> list) {
        onGetAllMusicInfos(list);
    }

    @Override
    public void onQueryMusicInfosByName(List<MusicInfoDao> list) {
        onGetAllMusicInfos(list);
    }

    @Override
    public void onQueryMusicInfosByArist(List<MusicInfoDao> list) {
        onGetAllMusicInfos(list);
    }

    @Override
    public void onQueryMuisicInfosByAlbum(List<MusicInfoDao> list) {
        onGetAllMusicInfos(list);
    }

    //MediaLibrary.IMediaScanListener
    @Override
    public void onScanStart() {

    }

    @Override
    public void onScaning(String fileInfo, float progress) {

    }

    @Override
    public void onScanFinish() {
    }

    @Override
    public void onItemClick(View v, int originalPosition, int currentPosition, LocalAudioAdapter.LocalAudioItemData entity) {
        if(mType == LayoutType.ALLSONG || mType == LayoutType.CUSTOME){
            final MusicInfoDao musicInfoDao = mPresenter.getMusicInfoById(Long.parseLong(entity.getId()));
            if(musicInfoDao == null || mService == null)
                return;

            mService.setPlaylist(mMusicInfoDaos);
            int realPlayPosition = currentPosition;
            for(int i = 0;i < mMusicInfoDaos.size();i++){
                if(mMusicInfoDaos.get(i).get_id() == musicInfoDao.get_id()){
                    realPlayPosition = i;
                    break;
                }
            }

            mService.setCurPlayIndex(realPlayPosition);
            mService.setDataSource(musicInfoDao.get_data());
            mService.prepareAsync();
            mAdapter.setSelectItemId(Long.parseLong(entity.getId()));
            mAdapter.notifyDataSetChanged();

            if(TextUtils.isEmpty(musicInfoDao.getLyric_path())){
                String query = String.format("%s$$%s", musicInfoDao.getTitle(), musicInfoDao.getArtist());
                mSearchLrcPicPresenter.searchLrcPic("android", "6.0.3.1", "xiaomi", 2, "baidu.ting.search.lrcpic", "json",
                        query, System.currentTimeMillis(), "rqCNe1a9CwfBNPxnUUMbXVipabBzT5%2FpZsq1%2BqF1%2BpeOLc5vuWeTNK2JLBYhBejgp8Nnc%2BWTvSA1g6eF8zVmKQ%3D%3D", 2,
                        new SearchLrcPicContract.View() {
                            @Override
                            public void onSearchLrcPicSuccess(SearchLrcPicBean bean) {
                                if(bean == null || bean.getSonginfo() == null || bean.getSonginfo().size() == 0)return;

                                SearchLrcPicBean.SonginfoBean songinfoBean = bean.getSonginfo().get(0);
                                musicInfoDao.setAlbum(songinfoBean.getAlbum_id() + "");
                                musicInfoDao.setArtist(songinfoBean.getAuthor());
                                musicInfoDao.setLyric_path(songinfoBean.getLrclink());
                                musicInfoDao.setArtist_image(songinfoBean.getArtist_480_800());
                                musicInfoDao.setAlbum_image(songinfoBean.getAvatar_s500());
                                musicInfoDao.setSong_id(songinfoBean.getSong_id());
                                musicInfoDao.set_display_name(songinfoBean.getSong_title());
                                MediaModel.getInstance().updateMusicInfo(musicInfoDao);
                            }

                            @Override
                            public void onError(String strErrMsg) {

                            }
                        });
            }
        }
        else if(mType == LayoutType.FOLDER){
            AudioFilterFragment filterFragment = new AudioFilterFragment();
            HomeActivity.getInstance().addFragment(filterFragment);
            filterFragment.setFilterType(AudioFilterFragment.FILTER_BY_FOLDER);
            filterFragment.setFilterData(entity.getStrMain());
            filterFragment.setTitle(entity.getStrMain());
        }
        else if(mType == LayoutType.ARTIST){
            AudioFilterFragment filterFragment = new AudioFilterFragment();
            HomeActivity.getInstance().addFragment(filterFragment);
            filterFragment.setFilterType(AudioFilterFragment.FILTER_BY_ARTIST);
            filterFragment.setFilterData(entity.getStrMain());
            filterFragment.setTitle(entity.getStrMain());
        }
        else if(mType == LayoutType.ALBUM){
            AudioFilterFragment filterFragment = new AudioFilterFragment();
            HomeActivity.getInstance().addFragment(filterFragment);
            filterFragment.setFilterType(AudioFilterFragment.FILTER_BY_ALBUM);
            filterFragment.setFilterData(entity.getStrMain());
            filterFragment.setTitle(entity.getStrMain());
        }
    }

    @Override
    public void onMoreClick(LocalAudioAdapter adapter, int position) {
        if(adapter == null)
            return ;

        RealAdapter realAdapter = (RealAdapter) indexLayout.getRecyclerView().getAdapter();
        ArrayList<EntityWrapper> listReal = realAdapter.getItems();
        EntityWrapper<LocalAudioAdapter.LocalAudioItemData> wrapper = listReal.get(position);
        int originPosition = wrapper.getOriginalPosition();

        if(mMoreDialogbuilder == null){
            mMoreDialogbuilder = new MoreOperationDialog.Builder(mContext);
        }

        if(mMoreDialog == null){
            mMoreDialog = (MoreOperationDialog)mMoreDialogbuilder.create();
            mMoreDialog.setListener(this);
        }

        mMoreDialog.setKey(wrapper.getData().getId());
        LocalAudioAdapter.LocalAudioItemData itemData = adapter.getItemData(originPosition);
        if(itemData == null)
            return;

        List<Integer> list = new ArrayList<>();
        if(mType == LayoutType.ALLSONG){
            mMoreDialog.setTitle(itemData.getStrMain());
            list.add(MoreOperationDialog.MORE_NEXTPLAY_NORMAL);
            list.add(MoreOperationDialog.MORE_LOVE_NORMAL);
            list.add(MoreOperationDialog.MORE_BELL_NORMAL);
            list.add(MoreOperationDialog.MORE_SHARE_NORMAL);
            list.add(MoreOperationDialog.MORE_ADD_NORMA);
            list.add(MoreOperationDialog.MORE_DELETE_NORMAL);
            mMoreDialog.setFavorite(FavoriteMusicModel.getInstance().isHasFavorite(Long.parseLong(wrapper.getData().getId())));
        }
        else if(mType == LayoutType.FOLDER){
            mMoreDialog.setTitle(itemData.getStrMain());
            list.add(MoreOperationDialog.MORE_PLAY_NORMAL);
            list.add(MoreOperationDialog.MORE_ADD_NORMA);
            list.add(MoreOperationDialog.MORE_DELETE_NORMAL);
        }
        else{
            mMoreDialog.setTitle(itemData.getStrMain());
            list.add(MoreOperationDialog.MORE_PLAY_NORMAL);
            list.add(MoreOperationDialog.MORE_ADD_NORMA);
            list.add(MoreOperationDialog.MORE_DELETE_NORMAL);
        }

        if(mListMoreOperData == null){
            mMoreDialog.setMoreOperData(list);
        }
        else{
            mMoreDialog.setMoreOperData(mListMoreOperData);
        }
        mMoreDialog.setCancelable(true);
        mMoreDialog.show();
    }

    @Override
    public boolean onItemLongClick(View v, int originalPosition, int currentPosition, LocalAudioAdapter.LocalAudioItemData entity) {
        vibrator.vibrate(30);
        onMoreClick(mAdapter, currentPosition);
        return true;
    }

//    //AdapterView.OnItemClickListener
//    @Override
//    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//        AudioListViewAdapter adapter = (AudioListViewAdapter)parent.getAdapter();
//        int adapterType = adapter.getAdapterType();
//        List<AudioListViewAdapter.AudioItemData> listAllData = adapter.getAdapterAllData();
//        if(listAllData == null || listAllData.size() == 0)
//            return;
//
//        AudioListViewAdapter.AudioItemData itemData = listAllData.get(position);
//        if(itemData == null)
//            return ;
//
//        if(adapterType == AudioListViewAdapter.ADAPTER_TYPE_ALLSONG && itemData.mItemType == AudioListViewAdapter.AudioItemData.TYPE_MEDIA){
////            if(mService.isPlaying()){
////                mService.pause();
////            }
////            else
//            {
//                int realPosition = -1;
//                boolean bAdd = true;
//                List<MusicInfoDao> listMusicInfoDao = new ArrayList<>();
//                for(int i = 0;i < listAllData.size();i++){
//                    AudioListViewAdapter.AudioItemData tempData = listAllData.get(i);
//                    if(tempData == null)
//                        continue;
//
//                    if(tempData.mItemType != AudioListViewAdapter.AudioItemData.TYPE_MEDIA)
//                        continue;
//
//                    if(tempData.id == itemData.id){
//                        realPosition++;
//                        bAdd = false;
//                    }
//                    else{
//                        if(bAdd){
//                            realPosition++;
//                        }
//                    }
//
//                    if(tempData.mListMedia != null){
//                        listMusicInfoDao.addAll(tempData.mListMedia);
//                    }
//                }
//
//                if(realPosition >= 0 && mService != null){
//                    mService.play(listMusicInfoDao, realPosition);
//                }
//            }
////
////            mAllSongAdapter.setItemPlayState(position, true);
//        }
//        else if(adapterType == AudioListViewAdapter.ADAPTER_TYPE_FOLDER){
//            if(itemData instanceof AudioListViewAdapter.AudioFolderItemData){
//                AudioListViewAdapter.AudioFolderItemData folderItemData = (AudioListViewAdapter.AudioFolderItemData)itemData;
//                AudioFilterFragment audioFilterActivity = new AudioFilterFragment();
//                Bundle bundle = new Bundle();
//                bundle.putString(AudioFilterFragment.FILTER_NAME, folderItemData.mFolderPath);
//                bundle.putInt(AudioFilterFragment.FILTER_TYPE, adapterType);
//                bundle.putString(AudioFilterFragment.TITLE_NAME, folderItemData.mFolderName);
//                //overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
//                audioFilterActivity.setArguments(bundle);
//                android.support.v4.app.FragmentTransaction transaction = HomeActivity.getInstance().getSupportFragmentManager().beginTransaction();
//                //HomeActivity.getInstance().getFragmentManager().executePendingTransactions();
//                transaction.replace(R.id.flContent, audioFilterActivity);
//                transaction.addToBackStack(null);
//                transaction.commit();
//            }
//        }
//        else if(adapterType == AudioListViewAdapter.ADAPTER_TYPE_ARTIST || adapterType == AudioListViewAdapter.ADAPTER_TYPE_ALBUM){
//            if(itemData instanceof AudioListViewAdapter.AudioArtistAlbumItemData){
//                AudioListViewAdapter.AudioArtistAlbumItemData artistItemData = (AudioListViewAdapter.AudioArtistAlbumItemData)itemData;
//                AudioFilterFragment audioFilterActivity = new AudioFilterFragment();
//                Bundle bundle = new Bundle();
//                bundle.putString(AudioFilterFragment.FILTER_NAME, artistItemData.mArtistAlbumName);
//                bundle.putInt(AudioFilterFragment.FILTER_TYPE, adapterType);
//                bundle.putString(AudioFilterFragment.TITLE_NAME, artistItemData.mArtistAlbumName);
//                //overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
//                audioFilterActivity.setArguments(bundle);
//                android.support.v4.app.FragmentTransaction transaction = HomeActivity.getInstance().getSupportFragmentManager().beginTransaction();
//                HomeActivity.getInstance().getFragmentManager().executePendingTransactions();
//                transaction.add(R.id.flContent, audioFilterActivity);
//                transaction.addToBackStack(null);
//                transaction.commit();
//            }
//        }
//    }
//
//    //AdapterView.OnScrollChangeListener
//    @Override
//    public void onScrollStateChanged(AbsListView view, int scrollState) {
//
//    }
//
//    public void onScroll(AbsListView view, int firstVisibleItem,
//                         int visibleItemCount, int totalItemCount) {
//
//        boolean enable = false;
//        if(view != null && view.getChildCount() > 0){
//            // check if the first item of the list is visible
//            boolean firstItemVisible = view.getFirstVisiblePosition() == 0;
//            // check if the top of the first item is visible
//            boolean topOfFirstItemVisible = view.getChildAt(0).getTop() == 0;
//            // enabling or disabling the refresh layout
//            enable = firstItemVisible && topOfFirstItemVisible;
//        }
//    }
//
//    @Override
//    public void onRandomPlayClick(AudioListViewAdapter adapter) {
//        if(adapter == null || mService == null)
//            return;
//
//        mService.setRepeatMode(PreferenceConfig.PLAYMODE_RANDOM);
//        mService.next(false);
//    }
//
//    @Override
//    public void onBatchMgrClick(AudioListViewAdapter adapter) {
//        Intent intent = new Intent(mContext, BatchMgrFragment.class);
//        List<MusicInfoDao> listTemp = new ArrayList<>();
////        List<MusicInfoDao> temp = mAllSongAdapter.getAdapterOriginData();
////        if(temp != null){
////            listTemp.addAll(temp);
////        }
////        intent.putExtra(BatchMgrFragment.INTENT_LIST_DATA, (Serializable)listTemp);
////        HomeActivity.getInstance().startActivity(intent);
////        HomeActivity.getInstance().overridePendingTransition(R.anim.anim_right_enter, R.anim.anim_right_exit);
//    }

    //MoreOperationDialog.IMoreOperationDialogListener
    @Override
    public void onMoreItemClick(MoreOperationDialog dialog, int tag) {
        if(mPresenter == null || dialog == null)
            return;

        mMoreDialog.dismiss();
        List<MusicInfoDao> listOperMusicInfoDao = new ArrayList<>();
        String strKey = (String)dialog.getKey();
        if(TextUtils.isEmpty(strKey))
            return;

        if(mType == LayoutType.ALLSONG || mType == LayoutType.CUSTOME){
            MusicInfoDao musicInfoDao = mPresenter.getMusicInfoById(Long.parseLong(strKey));
            if(musicInfoDao == null)
                return;

            switch (tag){
                case MoreOperationDialog.MORE_ADD_NORMA:
                    FavoriteDialog.Builder builderFavorite = new FavoriteDialog.Builder(mContext);
                    FavoriteDialog dialogFavorite = (FavoriteDialog)builderFavorite.create();
                    dialogFavorite.setCancelable(true);
                    dialogFavorite.setKeyType(mType);
                    dialogFavorite.setStrKey(musicInfoDao.get_id() + "");
                    dialogFavorite.show();
                    dialogFavorite.setTitle("添加到歌单");
                    break;
                case MoreOperationDialog.MORE_ALBUM_NORMAL:
                    break;
                case MoreOperationDialog.MORE_BELL_NORMAL:
                    RingBellUtil.setRingBell(this.getContext(), musicInfoDao.get_data());
                    String strPromt = String.format("已将歌曲\"%s\"设置为铃声",musicInfoDao.getTitle());
                    Toast.makeText(mContext, strPromt, Toast.LENGTH_SHORT).show();
                    break;
                case MoreOperationDialog.MORE_DELETE_NORMAL:
                    String strTitle = "";
                    strTitle = "确定删除\"" + musicInfoDao.getTitle() + "\"吗?";
                    AlertDialogDeleteOne.Builder builder = new AlertDialogDeleteOne.Builder(this.getContext());
                    AlertDialogDeleteOne deleteDialog = new AlertDialogDeleteOne(this.getContext(), new AlertDialogDeleteOne.IOnAlertDialogDeleteListener() {
                        @Override
                        public void OnDeleteClick(AlertDialogDeleteOne dialog, boolean isDeleteFile) {
                            List<MusicInfoDao> listMusicInfoDao = dialog.getMusicInfoDaoData();
                            if(listMusicInfoDao == null || listMusicInfoDao.size() == 0)
                                return;

                            handleDeleteMedia(listMusicInfoDao, isDeleteFile);
                        }
                    });
                    listOperMusicInfoDao.add(musicInfoDao);
                    deleteDialog.show();
                    deleteDialog.setTitle(strTitle);
                    deleteDialog.setMusicInfoDaoData(listOperMusicInfoDao);
                    break;
//                case MoreOperationDialog.MORE_DOWNLOAD_NORMAL:
//                    break;
//                case MoreOperationDialog.MORE_HIDE_NORMAL:
//                    AlertDialogHide hideOne = new AlertDialogHide(mContext);
//                    hideOne.show();
//                    hideOne.setMusicInfoDaoData(listOperMusicInfoDao);
//                    hideOne.setAlertDialogListener(mAlertDialogHideListener);
//                    break;
                case MoreOperationDialog.MORE_LOVE_NORMAL:
                    boolean bRet = FavoriteMusicModel.getInstance().isHasFavorite(musicInfoDao.get_id());
                    if(!bRet){
                        FavoriteMusicDao dao = new FavoriteMusicDao();
                        dao.setMusicinfo_id(musicInfoDao.get_id());
                        dao.setFav_time(System.currentTimeMillis());
                        dao.setAlbum(musicInfoDao.getAlbum());
                        dao.setArtist(musicInfoDao.getArtist());
                        FavoriteMusicModel.getInstance().addFavoriteMusic(dao);
                    }
                    else{
                        FavoriteMusicModel.getInstance().removeFavoriteMusic(musicInfoDao.get_id());
                    }

                    if(bRet){
                        Toast.makeText(mContext, "已取消喜欢", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Toast.makeText(mContext, "已添加到我喜欢的单曲", Toast.LENGTH_SHORT).show();
                    }
                    break;
//                case MoreOperationDialog.MORE_MV_NORMAL:
//                    break;
                case MoreOperationDialog.MORE_NEXTPLAY_NORMAL:
                    if(mService != null){
                        mService.addToNextPlay(musicInfoDao);
                        Toast.makeText(mContext, "已添加到播放列表", Toast.LENGTH_SHORT).show();
                    }
                    break;
//                case MoreOperationDialog.MORE_PLAY_NORMAL:
//                    if(mService != null){
//                        //mService.play(listOperMusicInfoDao, 0);
//                        //mAllSongAdapter.setItemPlayState(listOperMusicInfoDao.get(0), true);
//                    }
//                    break;
                case MoreOperationDialog.MORE_REMOVE_NORMAL:

                    break;
//                case MoreOperationDialog.MORE_SHARE_NORMAL:
//                    if(mLVSongItemData == null)
//                        return ;
//
//                    AndroidShare as = new AndroidShare(
//                            mContext,
//                            "哈哈---超方便的分享！！！来自allen",
//                            "http://img6.cache.netease.com/cnews/news2012/img/logo_news.png");
//                    as.show();
//                    as.setTitle("分享 - " + mLVSongItemData.mMainTitle);
//                    break;
                case MoreOperationDialog.MORE_SONGER_NORMAL:
                    break;
            }
        }
        else if(mType == LayoutType.FOLDER || mType == LayoutType.ALBUM || mType == LayoutType.ARTIST){
            listOperMusicInfoDao.addAll(mCategaryMusicInfoDaos.get(strKey));
            switch (tag) {
                case MoreOperationDialog.MORE_PLAY_NORMAL:
                    mService.setPlaylist(listOperMusicInfoDao);
                    if(listOperMusicInfoDao.size() > 0){
                        mService.setCurPlayIndex(0);
                        mService.setDataSource(listOperMusicInfoDao.get(0).get_data());
                        mService.prepareAsync();
                    }
                    break;
                case MoreOperationDialog.MORE_ADD_NORMA:
                    FavoriteDialog.Builder builderFavorite = new FavoriteDialog.Builder(mContext);
                    FavoriteDialog dialogFavorite = (FavoriteDialog)builderFavorite.create();
                    dialogFavorite.setCancelable(true);
                    dialogFavorite.setKeyType(mType);
                    dialogFavorite.show();
                    dialogFavorite.setStrKey(strKey);
                    dialogFavorite.setTitle(strKey);
                    break;
                case MoreOperationDialog.MORE_DELETE_NORMAL:
                    String strTitle = "";
                    strTitle = "确定删除文件夹下所有歌曲吗?";
                    AlertDialogDeleteOne.Builder builder = new AlertDialogDeleteOne.Builder(this.getContext());
                    AlertDialogDeleteOne deleteDialog = new AlertDialogDeleteOne(this.getContext(), new AlertDialogDeleteOne.IOnAlertDialogDeleteListener() {
                        @Override
                        public void OnDeleteClick(AlertDialogDeleteOne dialog, boolean isDeleteFile) {
                            List<MusicInfoDao> listMusicInfoDao = dialog.getMusicInfoDaoData();
                            if(listMusicInfoDao == null || listMusicInfoDao.size() == 0)
                                return;

                            handleDeleteMedia(listMusicInfoDao, isDeleteFile);
                        }
                    });
                    deleteDialog.show();
                    deleteDialog.setTitle(strTitle);
                    deleteDialog.setMusicInfoDaoData(listOperMusicInfoDao);
                    break;
            }
        }
    }

    @Override
    public void onClick(View v) {
    }

    @Override
    public void onStateChange(int state) {
        if(state == IPlayMusic.STATE_PAPERED || state == IPlayMusic.STATE_PLAY){
            if(mType == LayoutType.ALLSONG){
                MusicInfoDao musicInfoDao = mService.getCurPlayMusicDao();
                if(musicInfoDao == null)
                    return;

                mAdapter.setSelectItemId(musicInfoDao.get_id());
                mAdapter.notifyDataSetChanged();
            }
        } else if (state == IPlayMusic.STATE_PAUSE) {

        }
    }

    @Override
    public void onPlayPosUpdate(int percent, int curPos, int duration) {

    }

    @Override
    public void onBufferingUpdate(int cur, int total) {

    }

    @Override
    public void onConnect(PlayMusicService service) {
        mService = service;
        mService.addListener(this);
    }

    @Override
    public void onDisconnect() {
        mService.removeListener(this);
        mService = null;
    }


    public void showDeleteAlterDialog(Context context, List<MusicInfoDao> listOperMusicInfoDao, String strTitle, boolean isNeedReCreate){
        if(isNeedReCreate){
            mAlertDialogDeleteOne = new AlertDialogDeleteOne(context, mAlertDialogDeleteListener);
        }

        if(mAlertDialogDeleteOne == null){
            mAlertDialogDeleteOne =  new AlertDialogDeleteOne(context, mAlertDialogDeleteListener);
        }

        mAlertDialogDeleteOne.show();
        mAlertDialogDeleteOne.setMusicInfoDaoData(listOperMusicInfoDao);
        mAlertDialogDeleteOne.setTitle(strTitle);
    }

    public void handleDeleteMedia(List<MusicInfoDao> listMusicInfoDao, boolean isDeleteFile){
        if(listMusicInfoDao == null)
            return;

        //是否包含正在播放的歌曲
        boolean isContainPlaying = false;
        MusicInfoDao curMusicInfoDao = null;
        curMusicInfoDao = mService.getCurPlayMusicDao();

        boolean delDBRet = false;
        boolean delFileRet = false;
        int successNum = 0;
        for(int i = 0;i < listMusicInfoDao.size();i++){
            MusicInfoDao entity = listMusicInfoDao.get(i);
            if(entity == null || entity.get_id() < 0)
                continue;

            if(curMusicInfoDao != null && curMusicInfoDao.get_id() == entity.get_id()){
                if(curMusicInfoDao.get_id() == entity.get_id()){
                    isContainPlaying = true;
                }
            }
            else{
                mService.removeMusicInfo(entity.get_id());
                delDBRet = MediaModel.getInstance().deleteMusicInfo(entity);
                if(isDeleteFile){
                    File file = new File(entity.get_data());
                    if(file.exists()){
                        delFileRet = file.delete();
                    }
                }
                else{
                    delFileRet = true;
                }

                if(delDBRet && delFileRet){
                    successNum++;
                }
            }
        }

        String strPromt = "";
        if(successNum == 0){
            strPromt = "删除失败";
        }
        else if(successNum < listMusicInfoDao.size()){
            strPromt = "删除部分成功,部分失败";
        }
        else{
            strPromt = "删除成功";
        }
        Toast.makeText(mContext, strPromt, Toast.LENGTH_SHORT).show();

        if(isContainPlaying && mService != null){
            mService.next();
        }
    }
}
