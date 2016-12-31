package com.example.kaizhiwei.puremusictest.Audio;

import android.annotation.TargetApi;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kaizhiwei.puremusictest.CommonUI.AndroidShare;
import com.example.kaizhiwei.puremusictest.MediaData.FavoritesMusicEntity;
import com.example.kaizhiwei.puremusictest.MediaData.MediaEntity;
import com.example.kaizhiwei.puremusictest.MediaData.MediaLibrary;
import com.example.kaizhiwei.puremusictest.MediaData.PreferenceConfig;
import com.example.kaizhiwei.puremusictest.R;
import com.example.kaizhiwei.puremusictest.ScanMedia.ScanMediaActivity;
import com.example.kaizhiwei.puremusictest.Service.PlaybackService;

import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kaizhiwei on 16/11/12.
 */
@TargetApi(Build.VERSION_CODES.M)
public class LocalAudioFragment extends android.app.Fragment implements ViewPager.OnLongClickListener, ViewPager.OnPageChangeListener
        ,MediaLibrary.IMediaScanListener, AdapterView.OnItemClickListener, AbsListView.OnScrollListener, AudioListViewAdapter.IAudioListViewListener
        ,PlaybackService.Client.Callback, PlaybackService.Callback, AdapterView.OnItemLongClickListener, MoreOperationDialog.IMoreOperationDialogListener, View.OnClickListener {
    private TabLayout mTabLayout;
    private TabLayout.TabLayoutOnPageChangeListener mTVl;
    private ViewPager mViewPager;

    private List<String> mListTitleData;
    private List<View> mListViewData;

    private AudioListView mAllSongListView;
    private AudioListViewAdapter mAllSongAdapter;

    private AudioListView mSongFolderListView;
    private AudioListViewAdapter mSongFolderAdapter;

    private AudioListView mArtistListView;
    private AudioListViewAdapter mArtistAdapter;

    private AudioListView mAlbumListViewData;
    private AudioListViewAdapter mAlbumAdapter;
    private Handler mHandler = new Handler();
    private Vibrator vibrator;

    private PlaybackService.Client mClient = null;
    private PlaybackService mService;
    MoreOperationDialog.Builder mMoreDialogbuilder = null;
    MoreOperationDialog mMoreDialog = null;

    //加载控件
    private int mShowLoadingFlag = 0;
    private RelativeLayout rlLoading;
    private ProgressBar view_loading;
    private Handler loadingHandler = new Handler();

    //删除文件对话框
    private AlertDialogDeleteOne mAlertDialogDeleteOne;
    private AlertDialogDeleteOne.IOnAlertDialogDeleteListener mAlertDialogDeleteListener = new AlertDialogDeleteOne.IOnAlertDialogDeleteListener() {
        @Override
        public void OnDeleteClick(AlertDialogDeleteOne dialog, boolean isDeleteFile) {
            if(dialog == null)
                return;

            List<MediaEntity> listMediaEntity = dialog.getMediaEntityData();
            if(listMediaEntity == null || listMediaEntity.size() == 0)
                return;

            //是否包含正在播放的歌曲
            boolean isContainPlaying = false;
            MediaEntity curMediaEntity = null;
            if(mService != null){
                curMediaEntity = mService.getCurrentMedia();
            }

            int successNum = 0;
            for(int i = 0;i < listMediaEntity.size();i++){
                MediaEntity entity = listMediaEntity.get(i);
                if(entity == null || entity._id < 0)
                    continue;

                if(MediaLibrary.getInstance().removeMediaEntity(entity)){
                    successNum++;
                }
                if(isDeleteFile){
                    if(curMediaEntity != null && curMediaEntity._id == entity._id){
                        isContainPlaying = true;
                        mService.pause();
                    }

                    File file = new File(entity._data);
                    if(file.exists()){
                        file.delete();
                    }
                }

                if(mService != null){
                    mService.deleteMediaById(entity._id);
                }
            }

            String strPromt = "";
            if(successNum == 0){
                strPromt = "删除失败";
            }
            else if(successNum < listMediaEntity.size()){
                strPromt = "删除部分成功,部分失败";
            }
            else{
                strPromt = "删除成功";
            }
            Toast.makeText(LocalAudioFragment.this.getActivity(), strPromt, Toast.LENGTH_SHORT).show();
            initAdapterData();

            if(isContainPlaying && mService != null){
                mService.reCalNextPlayIndex();
            }
        }
    };

    //标题按钮
    private TextView tvTitle;
    private LinearLayout llSearch;
    private RelativeLayout rlTitle;
    private ImageView ivSearch;
    private ImageView ivScan;
    private ImageView ivSort;
    private EditText etSearchKey;
    private TextView tvCancel;
    private TextWatcher tvSearchTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            int curIndex = mViewPager.getCurrentItem();
            switch (curIndex){
                case 0:
                    mAllSongAdapter.setSearchKey(s.toString());
                    break;
                case 1:
                    mSongFolderAdapter.setSearchKey(s.toString());
                    break;
                case 2:
                    mArtistAdapter.setSearchKey(s.toString());
                    break;
                case 3:
                    mAlbumAdapter.setSearchKey(s.toString());
                    break;
            }
        }
    };

    public LocalAudioFragment(){

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View rootView = inflater.inflate(R.layout.activity_audio, null, false);//关联布局文件

        mClient = new PlaybackService.Client(this.getActivity(), this);
        vibrator = (Vibrator)getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        mTabLayout = (TabLayout) getActivity().findViewById(R.id.tabLayout);
        mTVl = new TabLayout.TabLayoutOnPageChangeListener(mTabLayout);
        mViewPager = (ViewPager) rootView.findViewById(R.id.viewPager);
        tvTitle = (TextView) rootView.findViewById(R.id.tvTitle);
        tvTitle.setText("本地音乐");

        llSearch = (LinearLayout) rootView.findViewById(R.id.llSearch);
        rlTitle = (RelativeLayout) rootView.findViewById(R.id.rlTitle);
        ivSearch = (ImageView) rootView.findViewById(R.id.ivSearch);
        ivSearch.setOnClickListener(this);
        ivSearch.setVisibility(View.VISIBLE);
        ivScan = (ImageView) rootView.findViewById(R.id.ivScan);
        ivScan.setOnClickListener(this);
        ivScan.setVisibility(View.VISIBLE);
        ivSort = (ImageView) rootView.findViewById(R.id.ivSort);
        ivSort.setOnClickListener(this);
        ivSort.setVisibility(View.VISIBLE);
        etSearchKey = (EditText) rootView.findViewById(R.id.etSearchKey);
        etSearchKey.addTextChangedListener(tvSearchTextWatcher);
        etSearchKey.setFocusable(true);
        etSearchKey.setFocusableInTouchMode(true);
        etSearchKey.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                InputMethodManager imm = (InputMethodManager) getActivity()
                             .getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
               }
                else{
                    InputMethodManager imm = (InputMethodManager) etSearchKey.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(0, InputMethodManager.SHOW_FORCED);
                }
            }
        });
        tvCancel = (TextView) rootView.findViewById(R.id.tvCancel);
        tvCancel.setOnClickListener(this);

        mListTitleData = new ArrayList<String>();
        mListTitleData.add("歌曲");
        mListTitleData.add("文件夹");
        mListTitleData.add("歌手");
        mListTitleData.add("专辑");

        mAllSongListView = (AudioListView) rootView.findViewById(R.id.lvAllSong);
        mAllSongAdapter = new AudioListViewAdapter(getActivity(), AudioListViewAdapter.ADAPTER_TYPE_ALLSONG, true);
        mAllSongListView.setAdapter(mAllSongAdapter);
        mAllSongListView.setOnItemClickListener(this);
        mAllSongListView.setOnScrollListener(this);
        mAllSongListView.setOnItemLongClickListener(this);
        mAllSongListView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
             @Override
             public void onLayoutChange(View v, int left, int top, int right,
                                        int bottom, int oldLeft, int oldTop, int oldRight,
                                        int oldBottom) {
                 mShowLoadingFlag++;
                 if(mShowLoadingFlag %2 == 0){
                     loadingHandler.postDelayed(new Runnable() {
                         @Override
                         public void run() {
                             rlLoading.setVisibility(View.GONE);
                             if(mService != null){
                                 MediaEntity curMediaEntity = mService.getCurrentMedia();
                                 if(curMediaEntity != null){
                                     mAllSongAdapter.setItemPlayState(curMediaEntity, true);
                                 }
                             }
                         }
                     }, 500);
                 }
             }
         });

        mSongFolderListView = (AudioListView) rootView.findViewById(R.id.lvSongFolder);
        mSongFolderAdapter = new AudioListViewAdapter(getActivity(), AudioListViewAdapter.ADAPTER_TYPE_FOLDER, false);
        mSongFolderListView.setAdapter(mSongFolderAdapter);
        mSongFolderListView.setOnItemClickListener(this);
        mSongFolderListView.setOnScrollListener(this);
        mSongFolderListView.setOnItemLongClickListener(this);

        mArtistListView = (AudioListView) rootView.findViewById(R.id.lvArtist);
        mArtistAdapter = new AudioListViewAdapter(getActivity(), AudioListViewAdapter.ADAPTER_TYPE_ARTIST, false);
        mArtistListView.setAdapter(mArtistAdapter);
        mArtistListView.setOnItemClickListener(this);
        mArtistListView.setOnScrollListener(this);
        mArtistListView.setOnItemLongClickListener(this);

        mAlbumListViewData = (AudioListView) rootView.findViewById(R.id.lvAlbum);
        mAlbumAdapter = new AudioListViewAdapter(getActivity(), AudioListViewAdapter.ADAPTER_TYPE_ALBUM, false);
        mAlbumListViewData.setAdapter(mAlbumAdapter);
        mAlbumListViewData.setOnItemClickListener(this);
        mAlbumListViewData.setOnScrollListener(this);
        mAlbumListViewData.setOnItemLongClickListener(this);

        mListViewData = new ArrayList<View>();
        mListViewData.add(mAllSongListView);
        mListViewData.add(mSongFolderListView);
        mListViewData.add(mArtistListView);
        mListViewData.add(mAlbumListViewData);

        mViewPager.setLongClickable(true);
        mViewPager.setOnLongClickListener(this);
        mViewPager.addOnPageChangeListener(this);
        mViewPager.setOffscreenPageLimit(mListViewData.size());
        AudioViewPagerAdapter adapter = new AudioViewPagerAdapter(mListViewData, mListTitleData);
        mViewPager.setAdapter(adapter);

        for(int i = 0;i < mListTitleData.size();i++){
            mTabLayout.addTab(mTabLayout.newTab().setText(mListTitleData.get(i)));
        }
        //mTabLayout.setBackgroundResource(R.color.backgroundColor);
        mTabLayout.setTabTextColors(this.getResources().getColor(R.color.mainTextColor), this.getResources().getColor(R.color.tabSelectTextColor));
        int indicatorColor = this.getResources().getColor(R.color.tabSeperatorLineColor);
        mTabLayout.setSelectedTabIndicatorColor(indicatorColor);

        mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        mTabLayout.setTabMode(TabLayout.MODE_FIXED);

        rlLoading = (RelativeLayout)rootView.findViewById(R.id.rlLoading);
        rlLoading.setVisibility(View.VISIBLE);
        view_loading = (ProgressBar)rootView.findViewById(R.id.view_loading);

        if(PreferenceConfig.getInstance().getLastFirstLaunch() == false){
            initAdapterData();
        }

        return rootView;
    }

    public void onDestory(){
        super.onDestroy();
        if(mMoreDialog != null){
            mMoreDialog.unregisterListener(this);
        }
    }

    public void onResume(){
        super.onResume();
        MediaLibrary.getInstance().registerListener(this);
        mAllSongAdapter.registerListener(this);
        mSongFolderAdapter.registerListener(this);
        mArtistAdapter.registerListener(this);
        mAlbumAdapter.registerListener(this);
    }

    public void onPause() {
        super.onPause();
        MediaLibrary.getInstance().unregisterListener(this);
        mAllSongAdapter.unregisterListener(this);
        mSongFolderAdapter.unregisterListener(this);
        mArtistAdapter.unregisterListener(this);
        mAlbumAdapter.unregisterListener(this);
    }

    public void onStart() {
        super.onStart();
        mClient.connect();
    }

    public void onStop() {
        super.onStop();
        mClient.disconnect();
    }

    public void onDetach() {
       super.onDetach();
        Log.i("weikaizhi", "onDetach");
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
//        if (keyCode == event.KEYCODE_BACK) {
//            FragmentManager fragmentManager = this.getActivity().getFragmentManager();
//            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
//            fragmentTransaction.hide(this);
//            fragmentTransaction.commit();
//            return true;
//        }

       return false;
    }

    public void initAdapterData(){
        final WeakReference<MediaLibrary> mlibrary = new WeakReference<MediaLibrary>(MediaLibrary.getInstance());
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                MediaLibrary library = mlibrary.get();
                if(library != null){
                    mAllSongAdapter.initData(library.getAllMediaEntrty());
                    mSongFolderAdapter.initData(library.getAllMediaEntrty());
                    mArtistAdapter.initData(library.getAllMediaEntrty());
                    mAlbumAdapter.initData(library.getAllMediaEntrty());
                }
            }
        });
    }

    //ViewPager.OnLongClickListener
    @Override
    public boolean onLongClick(View v) {
        return false;
    }

    //ViewPager.OnPageChangeListener
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        mTVl.onPageScrolled(position, positionOffset, positionOffsetPixels);
    }

    @Override
    public void onPageSelected(int position) {
        mViewPager.setCurrentItem(position);
        mTVl.onPageSelected(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        mTVl.onPageScrollStateChanged(state);
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
        initAdapterData();
    }

    //AdapterView.OnItemClickListener
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        AudioListViewAdapter adapter = (AudioListViewAdapter)parent.getAdapter();
        int adapterType = adapter.getAdapterType();
        List<AudioListViewAdapter.AudioItemData> listAllData = adapter.getAdapterAllData();
        if(listAllData == null || listAllData.size() == 0)
            return;

        AudioListViewAdapter.AudioItemData itemData = listAllData.get(position);
        if(itemData == null)
            return ;

        if(adapterType == AudioListViewAdapter.ADAPTER_TYPE_ALLSONG && itemData.mItemType == AudioListViewAdapter.AudioItemData.TYPE_MEDIA){
//            if(mService.isPlaying()){
//                mService.pause();
//            }
//            else
            {
                int realPosition = -1;
                boolean bAdd = true;
                List<MediaEntity> listMediaEntity = new ArrayList<>();
                for(int i = 0;i < listAllData.size();i++){
                    AudioListViewAdapter.AudioItemData tempData = listAllData.get(i);
                    if(tempData == null)
                        continue;

                    if(tempData.mItemType != AudioListViewAdapter.AudioItemData.TYPE_MEDIA)
                        continue;

                    if(bAdd == true && tempData != itemData){
                        realPosition ++;
                    }
                    else{
                        bAdd = false;
                    }

                    if(tempData.mListMedia != null){
                        listMediaEntity.addAll(tempData.mListMedia);
                    }
                }

                if(realPosition >= 0){
                    mService.play(listMediaEntity, realPosition+1);
                }
            }

            mAllSongAdapter.setItemPlayState(position, true);
        }
        else if(adapterType == AudioListViewAdapter.ADAPTER_TYPE_FOLDER){
            if(itemData instanceof AudioListViewAdapter.AudioFolderItemData){
                AudioListViewAdapter.AudioFolderItemData folderItemData = (AudioListViewAdapter.AudioFolderItemData)itemData;

                Intent intent = new Intent(this.getActivity(), AudioFilterActivity.class);
                intent.putExtra(AudioFilterActivity.FILTER_NAME, folderItemData.mFolderPath);
                intent.putExtra(AudioFilterActivity.FILTER_TYPE, adapterType);
                intent.putExtra(AudioFilterActivity.TITLE_NAME, folderItemData.mFolderName);
                //overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                startActivity(intent);
            }
        }
        else if(adapterType == AudioListViewAdapter.ADAPTER_TYPE_ARTIST || adapterType == AudioListViewAdapter.ADAPTER_TYPE_ALBUM){
            if(itemData instanceof AudioListViewAdapter.AudioArtistAlbumItemData){
                AudioListViewAdapter.AudioArtistAlbumItemData artistItemData = (AudioListViewAdapter.AudioArtistAlbumItemData)itemData;

                Intent intent = new Intent(this.getActivity(), AudioFilterActivity.class);
                intent.putExtra(AudioFilterActivity.FILTER_NAME, artistItemData.mArtistAlbumName);
                intent.putExtra(AudioFilterActivity.FILTER_TYPE, adapterType);
                intent.putExtra(AudioFilterActivity.TITLE_NAME, artistItemData.mArtistAlbumName);
                //overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                startActivity(intent);
            }
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        vibrator.vibrate(30);
        onMoreBtnClick((AudioListViewAdapter) parent.getAdapter(), position);
        return true;
    }

    //AdapterView.OnScrollChangeListener
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    public void onScroll(AbsListView view, int firstVisibleItem,
                         int visibleItemCount, int totalItemCount) {

        boolean enable = false;
        if(view != null && view.getChildCount() > 0){
            // check if the first item of the list is visible
            boolean firstItemVisible = view.getFirstVisiblePosition() == 0;
            // check if the top of the first item is visible
            boolean topOfFirstItemVisible = view.getChildAt(0).getTop() == 0;
            // enabling or disabling the refresh layout
            enable = firstItemVisible && topOfFirstItemVisible;
        }
    }

    //AudioListViewAdapter.IAudioListViewListener
    @Override
    public void onMoreBtnClick(AudioListViewAdapter adapter, int position) {
        if(adapter == null)
            return ;

        if(mMoreDialogbuilder == null){
            mMoreDialogbuilder = new MoreOperationDialog.Builder(this.getActivity());
        }

        if(mMoreDialog == null){
            mMoreDialog = mMoreDialogbuilder.create();
            mMoreDialog.registerListener(this);
        }

        List<Integer> list = new ArrayList<>();
        int adapterType = adapter.getAdapterType();
        AudioListViewAdapter.AudioItemData itemData = adapter.getAudioItemData(position);
        if(adapterType == AudioListViewAdapter.ADAPTER_TYPE_ALLSONG){
            AudioListViewAdapter.AudioSongItemData songItemData = null;
            if(itemData instanceof AudioListViewAdapter.AudioSongItemData){
                songItemData = (AudioListViewAdapter.AudioSongItemData)itemData;
            }

            if(songItemData != null){
                mMoreDialog.setTitle(songItemData.mMainTitle);
                list.add(MoreOperationDialog.MORE_NEXTPLAY_NORMAL);
                list.add(MoreOperationDialog.MORE_LOVE_NORMAL);
                list.add(MoreOperationDialog.MORE_BELL_NORMAL);
                list.add(MoreOperationDialog.MORE_SHARE_NORMAL);
                list.add(MoreOperationDialog.MORE_ADD_NORMA);
                list.add(MoreOperationDialog.MORE_DELETE_NORMAL);
            }
        }
        else if(adapterType == AudioListViewAdapter.ADAPTER_TYPE_FOLDER){
            AudioListViewAdapter.AudioFolderItemData folderItemData = null;
            if(itemData instanceof AudioListViewAdapter.AudioFolderItemData){
                folderItemData = (AudioListViewAdapter.AudioFolderItemData)itemData;
            }

            if(folderItemData != null){
                mMoreDialog.setTitle(folderItemData.mFolderName);
                list.add(MoreOperationDialog.MORE_PLAY_NORMAL);
                list.add(MoreOperationDialog.MORE_ADD_NORMA);
                list.add(MoreOperationDialog.MORE_HIDE_NORMAL);
                list.add(MoreOperationDialog.MORE_DELETE_NORMAL);
            }
        }
        else{
            AudioListViewAdapter.AudioArtistAlbumItemData artistAlbumItemData = null;
            if(itemData instanceof AudioListViewAdapter.AudioArtistAlbumItemData){
                artistAlbumItemData = (AudioListViewAdapter.AudioArtistAlbumItemData)itemData;
            }

            if(artistAlbumItemData != null){
                mMoreDialog.setTitle(artistAlbumItemData.mArtistAlbumName);
                list.add(MoreOperationDialog.MORE_PLAY_NORMAL);
                list.add(MoreOperationDialog.MORE_ADD_NORMA);
                list.add(MoreOperationDialog.MORE_DELETE_NORMAL);
            }
        }

        mMoreDialog.setMoreLVData(adapterType, itemData);
        mMoreDialog.setMoreOperData(list);
        mMoreDialog.setCancelable(true);
        mMoreDialog.show();
    }

    //PlaybackService.Client.Callback
    @Override
    public void onConnected(PlaybackService service) {
        mService = service;
        mService.addCallback(this);
    }

    @Override
    public void onDisconnected() {
        mService = null;
    }

    //PlaybackService.Callback
    @Override
    public void update() {

    }

    @Override
    public void updateProgress() {
        if(mService == null)
            return ;
    }

    @Override
    public void onMediaEvent(Media.Event event) {
        if(mService == null || event == null)
            return;
    }

    @Override
    public void onMediaPlayerEvent(MediaPlayer.Event event) {
        if(mService == null || event == null)
            return;

        if(event.type == MediaPlayer.Event.Playing){
            MediaEntity curMediaEntity = mService.getCurrentMedia();
            if(curMediaEntity != null && mAllSongAdapter != null){
                mAllSongAdapter.setItemPlayState(curMediaEntity, true);
            }
        }
    }

    //MoreOperationDialog.IMoreOperationDialogListener
    @Override
    public void onMoreItemClick(MoreOperationDialog dialog, int tag) {
        if(dialog == null)
            return;

        AudioListViewAdapter.AudioSongItemData mLVSongItemData = null;
        AudioListViewAdapter.AudioFolderItemData mLVFolderItemData = null;
        AudioListViewAdapter.AudioArtistAlbumItemData mLVArtistAlbumItemData = null;

        List<MediaEntity> listOperMediaEntity = null;
        int lvAdapterType = dialog.getLVAdapterType();
        AudioListViewAdapter.AudioItemData itemData = dialog.getAduioItemData();
        if(lvAdapterType == AudioListViewAdapter.ADAPTER_TYPE_ALLSONG){
            if(itemData instanceof AudioListViewAdapter.AudioSongItemData){
                mLVSongItemData = (AudioListViewAdapter.AudioSongItemData)itemData;
                listOperMediaEntity = mLVSongItemData.mListMedia;
            }
        }
        else if(lvAdapterType == AudioListViewAdapter.ADAPTER_TYPE_FOLDER){
            if(itemData instanceof AudioListViewAdapter.AudioFolderItemData){
                mLVFolderItemData = (AudioListViewAdapter.AudioFolderItemData)itemData;
                listOperMediaEntity = mLVFolderItemData.mListMedia;
            }
        }
        else{
            if(itemData instanceof AudioListViewAdapter.AudioArtistAlbumItemData){
                mLVArtistAlbumItemData = (AudioListViewAdapter.AudioArtistAlbumItemData)itemData;
                listOperMediaEntity = mLVArtistAlbumItemData.mListMedia;
            }
        }

        if(listOperMediaEntity == null || listOperMediaEntity.size() == 0){
            Toast.makeText(this.getActivity(), "data error", Toast.LENGTH_SHORT).show();
            return;
        }

        switch (tag){
            case MoreOperationDialog.MORE_ADD_NORMA:
                FavoriteDialog.Builder builderFavorite = new FavoriteDialog.Builder(this.getActivity());
                FavoriteDialog dialogFavorite = builderFavorite.create();
                dialogFavorite.setCancelable(true);
                dialogFavorite.setFavoritelistData(MediaLibrary.getInstance().getAllFavoriteEntity());
                dialogFavorite.setMediaEntityData(listOperMediaEntity);
                dialogFavorite.show();
                dialogFavorite.setTitle("添加到歌单");
                break;
            case MoreOperationDialog.MORE_ALBUM_NORMAL:
                break;
            case MoreOperationDialog.MORE_BELL_NORMAL:
                if(mLVSongItemData == null)
                    return ;

                if(listOperMediaEntity == null || listOperMediaEntity.size() > 1)
                    return;

                MediaEntity mediaEntity = listOperMediaEntity.get(0);
                if(mediaEntity == null)
                    return;

                String filePath = mediaEntity.getFilePath();
                ContentValues cv = new ContentValues();
                Uri uri = null, newUri = null;
                uri = MediaStore.Audio.Media.getContentUriForPath(filePath);
                Cursor cursor = this.getActivity().getContentResolver().query(uri, null, MediaStore.MediaColumns.DATA + "=?", new String[]{filePath}, null);
                if(cursor.moveToFirst() && cursor.getCount() > 0){
                    String _id = cursor.getString(0);
                    cv.put(MediaStore.Audio.Media.IS_RINGTONE, true);
                    cv.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
                    cv.put(MediaStore.Audio.Media.IS_ALARM, false);
                    cv.put(MediaStore.Audio.Media.IS_MUSIC, false);

                    // 把需要设为铃声的歌曲更新铃声库
                    this.getActivity().getContentResolver().update(uri, cv, MediaStore.MediaColumns.DATA + "=?",new String[] { filePath });
                    newUri = ContentUris.withAppendedId(uri, Long.valueOf(_id));
                    RingtoneManager.setActualDefaultRingtoneUri(this.getActivity(), RingtoneManager.TYPE_RINGTONE, newUri);
                    String strPromt = String.format("已将歌曲\"%s\"设置为铃声",mediaEntity.title);
                    Toast.makeText(this.getActivity(), strPromt, Toast.LENGTH_SHORT).show();
                }
                break;
            case MoreOperationDialog.MORE_DELETE_NORMAL:
                if(mAlertDialogDeleteOne == null){
                    mAlertDialogDeleteOne =  new AlertDialogDeleteOne(this.getActivity(), mAlertDialogDeleteListener);
                }
                mAlertDialogDeleteOne.show();
                mAlertDialogDeleteOne.setMediaEntityData(listOperMediaEntity);
                if(mLVSongItemData != null){
                    mAlertDialogDeleteOne.setTitle("确定删除\"" + mLVSongItemData.mMainTitle + "\"吗?");
                }
                else if(mLVFolderItemData != null){
                    mAlertDialogDeleteOne.setTitle("确定删除\"" + mLVFolderItemData.mFolderName + "\"下的所有歌曲吗?");
                }
                else if(mLVArtistAlbumItemData != null){
                    mAlertDialogDeleteOne.setTitle("确定删除该歌手/专辑的所有歌曲吗?");
                }
                break;
            case MoreOperationDialog.MORE_DOWNLOAD_NORMAL:
                break;
            case MoreOperationDialog.MORE_HIDE_NORMAL:
                AlertDialogHide hideOne = new AlertDialogHide(this.getActivity());
                hideOne.show();
                hideOne.setMediaEntityData(listOperMediaEntity);
                break;
            case MoreOperationDialog.MORE_LOVE_NORMAL:
                if(mLVSongItemData == null)
                    return ;

                //多个文件时仅添加,单个文件可添加 可删除
                boolean isMutil = listOperMediaEntity.size() > 1 ? true : false;
                boolean isAddToFavorite = false;
                int iSuccessNum = 0;
                for(int i = 0;i < listOperMediaEntity.size();i++){
                    mediaEntity = listOperMediaEntity.get(i);
                    if(mediaEntity == null)
                        continue;

                    FavoritesMusicEntity favoritesMusicEntity = new FavoritesMusicEntity();
                    favoritesMusicEntity.musicinfo_id = mediaEntity._id;
                    favoritesMusicEntity.artist = mediaEntity.artist;
                    favoritesMusicEntity.album = mediaEntity.album;
                    favoritesMusicEntity.fav_time = System.currentTimeMillis();
                    favoritesMusicEntity.path = mediaEntity._data;
                    favoritesMusicEntity.title = mediaEntity.title;
                    favoritesMusicEntity.favorite_id = MediaLibrary.getInstance().getDefaultFavoriteEntityId();

                    if(MediaLibrary.getInstance().queryIsFavoriteByMediaEntityId(mediaEntity._id, favoritesMusicEntity.favorite_id)){
                        if(isMutil == false){
                            boolean bRet = MediaLibrary.getInstance().removeFavoriteMusicEntity(favoritesMusicEntity.musicinfo_id, favoritesMusicEntity.favorite_id);
                            if(bRet){
                                iSuccessNum++;
                                isAddToFavorite = false;
                            }
                        }
                    }
                    else{
                        boolean bRet = MediaLibrary.getInstance().addFavoriteMusicEntity(favoritesMusicEntity);
                        if(bRet){
                            iSuccessNum++;
                            isAddToFavorite = true;
                        }
                    }
                }

                if(isMutil){
                    Toast.makeText(this.getActivity(), "成功" + iSuccessNum + "首,失败" + (listOperMediaEntity.size() - iSuccessNum) + "首", Toast.LENGTH_SHORT).show();
                }
                else{
                    if(!isAddToFavorite){
                        Toast.makeText(this.getActivity(), "已取消喜欢", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Toast.makeText(this.getActivity(), "已添加到我喜欢的单曲", Toast.LENGTH_SHORT).show();
                    }
                }

                break;
            case MoreOperationDialog.MORE_MV_NORMAL:
                break;
            case MoreOperationDialog.MORE_NEXTPLAY_NORMAL:
                if(mLVSongItemData == null)
                    return ;

                mediaEntity = MediaLibrary.getInstance().getMediaEntityById(mLVSongItemData.id);
                if(mediaEntity == null)
                    return ;

                if(mLVSongItemData != null){
                    mService.addSongToNext(mediaEntity);
                }
                break;
            case MoreOperationDialog.MORE_PLAY_NORMAL:
                break;
            case MoreOperationDialog.MORE_REMOVE_NORMAL:
                break;
            case MoreOperationDialog.MORE_SHARE_NORMAL:
                if(mLVSongItemData == null)
                    return ;

                AndroidShare as = new AndroidShare(
                        this.getActivity(),
                        "哈哈---超方便的分享！！！来自allen",
                        "http://img6.cache.netease.com/cnews/news2012/img/logo_news.png");
                as.show();
                as.setTitle("分享 - " + mLVSongItemData.mMainTitle);
                break;
            case MoreOperationDialog.MORE_SONGER_NORMAL:
                break;
        }
        mMoreDialog.dismiss();
    }

    @Override
    public void onClick(View v) {
        if(v == ivSearch){
            rlTitle.setVisibility(View.GONE);
            llSearch.setVisibility(View.VISIBLE);
            etSearchKey.requestFocus();
        }
        else if(v == ivScan){
            Intent intent = new Intent(LocalAudioFragment.this.getActivity(), ScanMediaActivity.class);
            startActivity(intent);
        }
        else if(v == ivSort){

        }
        else if(v == tvCancel){
            etSearchKey.setText("");
            rlTitle.setVisibility(View.VISIBLE);
            llSearch.setVisibility(View.GONE);
            mAllSongAdapter.clearSearchkKey();
        }
    }
}
