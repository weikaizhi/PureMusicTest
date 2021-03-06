package com.example.kaizhiwei.puremusictest.PlayingDetail;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kaizhiwei.puremusictest.dao.MusicInfoDao;
import com.example.kaizhiwei.puremusictest.service.IPlayMusic;
import com.example.kaizhiwei.puremusictest.service.IPlayMusicListener;
import com.example.kaizhiwei.puremusictest.service.PlayMusicImpl;
import com.example.kaizhiwei.puremusictest.service.PlayMusicService;
import com.example.kaizhiwei.puremusictest.ui.localmusic.PlaylistDialog;
import com.example.kaizhiwei.puremusictest.ui.localmusic.PlayListViewAdapter;
import com.example.kaizhiwei.puremusictest.CommonUI.MyImageView;
import com.example.kaizhiwei.puremusictest.MediaData.MediaLibrary;
import com.example.kaizhiwei.puremusictest.MediaData.PreferenceConfig;
import com.example.kaizhiwei.puremusictest.R;
import com.viewpagerindicator.CirclePageIndicator;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by kaizhiwei on 17/1/4.
 */
public class PlayingActivity extends FragmentActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, PlayMusicService.Client.Callback, IPlayMusicListener {
    private MyImageView mivBack;
    private MyImageView mivLike;
    private MyImageView mivDownload;
    private MyImageView mivShare;
    private MyImageView mivComment;
    private MyImageView mivMore;

    private MyImageView mivPlayMode;
    private MyImageView mivPre;
    private MyImageView mivPlayPause;
    private MyImageView mivNext;
    private MyImageView mivPlaylist;
    private ViewPager viewPager;
    private CirclePageIndicator linePageIndicator;
    private PlayingPagerAdapter mPagerAdapter;
    private List<Fragment> mListFragment;
    private PlayingMoreOperDialog mPlayingMoreOperDialog;
    private PlayingArtistInfoFragment artistInfoFragment;
    private PlayingLyricInfoFragment lyricInfoFragment;
    private PlayingMusicInfoFragment musicInfoFragment;

    private TextView tvCurTime;
    private TextView tvTotalTime;
    private SeekBar sbProgress;
    private boolean isUpdateCurTimeFromUser = false;

    public static final String PLAYING_MEDIA_ENTITY = "PLAYING_MEDIA_ENTITY";
    private List<List<Integer>> listPLaymodeImageRes;

    private PlayMusicService.Client mClient = null;
    private PlayMusicService mService;
    private PlaylistDialog mPlayListDialog;
    private PlaylistDialog.IPlayListDialogListener mListener = new PlaylistDialog.IPlayListDialogListener() {
        @Override
        public void onDeleteClick(PlayListViewAdapter adapter, int position) {
            if(adapter == null || position < 0)
                return;

            if(mService == null)
                return;

            PlayListViewAdapter.PlayListItemData itemData = (PlayListViewAdapter.PlayListItemData)adapter.getItem(position);
            if(itemData == null || itemData.MusicInfoDao == null)
                return;

            MusicInfoDao curPlayMedia = mService.getCurPlayMusicDao();
            mPlayListDialog.removeItem(position);
            mService.removeMusicInfo(itemData.MusicInfoDao.get_id());
            if(curPlayMedia != null){
                if(curPlayMedia.get_id() == itemData.MusicInfoDao.get_id()){
                    mService.next();
                }
            }
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if(parent == null || view == null || position < 0 || mService == null)
                return;

            PlayListViewAdapter adapter = (PlayListViewAdapter)parent.getAdapter();
            PlayListViewAdapter.PlayListItemData itemData = (PlayListViewAdapter.PlayListItemData)adapter.getItem(position);
            if(itemData == null || itemData.MusicInfoDao == null)
                return;

            mService.setCurPlayIndex(position);
            mService.setDataSource(itemData.MusicInfoDao.get_data());
        }

        @Override
        public void onClearPlaylist() {
            if(mService == null)
                return;

            mService.clearPlaylist();
        }

        @Override
        public void onChangePlayMode() {
            if(mService == null || mPlayListDialog == null)
                return;

            int curPlayMode = mService.getPlayMode().ordinal();
            int nextPlayMode = (curPlayMode + 1)%(PlayMusicImpl.PlayMode.values().length);
            PlayMusicImpl.PlayMode mode = PlayMusicImpl.PlayMode.values()[nextPlayMode];
            mService.setPlayMode(mode);
            mPlayListDialog.updatePlaymodeState(mode, true);
            updatePlaymodeState(nextPlayMode, false);
        }
    };

    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playing_main);
        mivBack = (MyImageView)this.findViewById(R.id.mivBack);
        mivBack.setResId(R.drawable.bt_playpage_button_return_normal_new, R.drawable.bt_playpage_button_return_press_new);
        mivBack.setOnClickListener(this);

        mivLike = (MyImageView)this.findViewById(R.id.mivLike);
        mivLike.setOnClickListener(this);

        mivDownload = (MyImageView)this.findViewById(R.id.mivDownload);
        mivDownload.setResId(R.drawable.bt_playpage_button_download_normal_new, R.drawable.bt_playpage_button_download_press_new);
        mivDownload.setOnClickListener(this);

        mivShare = (MyImageView)this.findViewById(R.id.mivShare);
        mivShare.setResId(R.drawable.bt_playpage_button_share_normal_new, R.drawable.bt_playpage_button_share_press_new);
        mivShare.setOnClickListener(this);

        mivComment = (MyImageView)this.findViewById(R.id.mivComment);
        mivComment.setResId(R.drawable.bt_playpage_button_comment_normal, R.drawable.bt_playpage_button_comment_press);
        mivComment.setOnClickListener(this);

        mivMore = (MyImageView)this.findViewById(R.id.mivMore);
        mivMore.setResId(R.drawable.bt_playpage_moreactions_normal_new, R.drawable.bt_playpage_moreactions_press_new);
        mivMore.setOnClickListener(this);

        mivPlayMode = (MyImageView)this.findViewById(R.id.mivPlayMode);
        mivPlayMode.setOnClickListener(this);

        mivPre = (MyImageView)this.findViewById(R.id.mivPre);
        mivPre.setResId(R.drawable.bt_playpage_button_previous_normal_new, R.drawable.bt_playpage_button_previous_press_new);
        mivPre.setOnClickListener(this);

        mivPlayPause = (MyImageView)this.findViewById(R.id.mivPlayPause);
        mivPlayPause.setResId(R.drawable.bt_playpage_button_play_normal_new, R.drawable.bt_playpage_button_play_press_new);
        mivPlayPause.setOnClickListener(this);

        mivNext = (MyImageView)this.findViewById(R.id.mivNext);
        mivNext.setResId(R.drawable.bt_playpage_button_next_normal_new, R.drawable.bt_playpage_button_next_press_new);
        mivNext.setOnClickListener(this);

        mivPlaylist = (MyImageView)this.findViewById(R.id.mivPlaylist);
        mivPlaylist.setResId(R.drawable.bt_playpage_button_list_normal_new, R.drawable.bt_playpage_button_list_press_new);
        mivPlaylist.setOnClickListener(this);

        viewPager = (ViewPager)this.findViewById(R.id.viewPager);
        linePageIndicator = (CirclePageIndicator)this.findViewById(R.id.linePageIndicator);

        mListFragment = new ArrayList<>();
        artistInfoFragment = new PlayingArtistInfoFragment();
        musicInfoFragment = new PlayingMusicInfoFragment();
        lyricInfoFragment = new PlayingLyricInfoFragment();
        mListFragment.add(artistInfoFragment);
        mListFragment.add(musicInfoFragment);
        mListFragment.add(lyricInfoFragment);

        mPagerAdapter = new PlayingPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(mPagerAdapter);
        viewPager.setOffscreenPageLimit(3);

        linePageIndicator.setViewPager(viewPager);
        viewPager.setCurrentItem(1);
        final float density = getResources().getDisplayMetrics().density;
        linePageIndicator.setFillColor(0xFF0000);
        linePageIndicator.setPageColor(0x00FF00);
        linePageIndicator.setStrokeWidth(2 * density);
//        linePageIndicator.setLineWidth(30 * density);

        tvCurTime = (TextView) this.findViewById(R.id.tvCurTime);
        tvTotalTime = (TextView) this.findViewById(R.id.tvTotalTime);
        sbProgress = (SeekBar)this.findViewById(R.id.sbProgress);
        sbProgress.setOnSeekBarChangeListener(this);
        mClient = new PlayMusicService.Client(this, this);

        listPLaymodeImageRes = new ArrayList<>();
        List<Integer> list = new ArrayList<>();
        list.add(R.drawable.bt_playpage_order_normal_new);
        list.add(R.drawable.bt_playpage_order_press_new);
        list.add(R.string.play_mode_order);
        listPLaymodeImageRes.add(list);

        list = new ArrayList<>();
        list.add(R.drawable.bt_playpage_roundsingle_normal_new);
        list.add(R.drawable.bt_playpage_roundsingle_press_new);
        list.add(R.string.play_mode_roundsingle);
        listPLaymodeImageRes.add(list);

        list = new ArrayList<>();
        list.add(R.drawable.bt_playpage_loop_normal_new);
        list.add(R.drawable.bt_playpage_loop_press_new);
        list.add(R.string.play_mode_round);
        listPLaymodeImageRes.add(list);

        list = new ArrayList<>();
        list.add(R.drawable.bt_playpage_random_normal_new);
        list.add(R.drawable.bt_playpage_random_press_new);
        list.add(R.string.play_mode_random);
        listPLaymodeImageRes.add(list);

       // Intent intent =  getIntent();
        //Bundle extras = intent.getExtras();
        //MediaEntity MediaEntity = (MediaEntity)extras.getParcelable(PLAYING_MEDIA_ENTITY);
        initUI();
    }

    private void initUI(){
        if(mService == null)
            return;

        updatePlayPauseState(mService.isPlaying());
        PlayMusicImpl.PlayMode playMode = mService.getPlayMode();

        updatePlaymodeState(playMode.ordinal(), false);

        List<MusicInfoDao> list = mService.getPlaylist();
        int index = mService.getCurPlayIndex();
        if(index < 0)
            return;

        MusicInfoDao curMusicInfoDao = list.get(index);
        if(curMusicInfoDao != null){
            artistInfoFragment.setArtistAlbumInfo(curMusicInfoDao);
            musicInfoFragment.setMusciInfo(curMusicInfoDao);
            boolean bFavorite = MediaLibrary.getInstance().queryIsFavoriteByMusicInfoDaoId(curMusicInfoDao.get_id(), MediaLibrary.getInstance().getDefaultFavoriteEntityId());
            updateLoveState(bFavorite);
        }
    }

    private void updatePlayProgress(int iCur, int iMax){
        sbProgress.setMax(iMax);
        sbProgress.setProgress(iCur);
        if(isUpdateCurTimeFromUser == false){
            tvCurTime.setText(formatTime(iCur));
        }
        tvTotalTime.setText(formatTime(iMax));
    }

    private String formatTime(int time){
        String str;
        time = time/1000;
        str = String.format("%02d:%02d", time/60, time%60);
        return str;
    }

    private void updateLoveState(boolean bLoved){
        if(bLoved){
            mivLike.setResId(R.drawable.bt_playpage_button_like_hl_new, R.drawable.bt_playpage_button_like_press_new);
        }
        else{
            mivLike.setResId(R.drawable.bt_playpage_button_like_normal_new, R.drawable.bt_playpage_button_like_press_new);
        }
    }

    private void updatePlayPauseState(boolean isPlaying){
        if(isPlaying){
            mivPlayPause.setResId(R.drawable.bt_playpage_button_pause_normal_new, R.drawable.bt_playpage_button_pause_press_new);
        }
        else{
            mivPlayPause.setResId(R.drawable.bt_playpage_button_play_normal_new, R.drawable.bt_playpage_button_play_press_new);
        }
    }

    private void updatePlaymodeState(int playMode, boolean isShowToast){
        if(playMode < 0 || playMode >= PreferenceConfig.PLAYMODE_NUM)
            return;

        List<Integer> list = listPLaymodeImageRes.get(playMode);
        mivPlayMode.setResId(list.get(0), list.get(1));
        if(isShowToast)
            Toast.makeText(this, list.get(2), Toast.LENGTH_SHORT).show();
    }

    private void resetUI(){
        tvCurTime.setText("00:00");
        tvTotalTime.setText("00:00");
        updateLoveState(false);
        updatePlaymodeState(0, false);
        updatePlayPauseState(false);
        updatePlayProgress(0,0);
    }

    @Override
    public void onClick(View v) {
        if(mService == null)
            return;

        if(v == mivLike){
//            MusicInfoDao MusicInfoDao = mService.getCurrentMedia();
//            if(MusicInfoDao == null)
//                return;

//            FavoritesMusicEntity favoritesMusicEntity = new FavoritesMusicEntity();
//            favoritesMusicEntity.musicinfo_id = MusicInfoDao.get_id();
//            favoritesMusicEntity.title = MusicInfoDao.getArtist();
//            favoritesMusicEntity.album = MusicInfoDao.getAlbum();
//            favoritesMusicEntity.fav_time = System.currentTimeMillis();
//            favoritesMusicEntity.path = MusicInfoDao.get_data();
//            favoritesMusicEntity.title = MusicInfoDao.getTitle();
//            favoritesMusicEntity.favorite_id = MediaLibrary.getInstance().getDefaultFavoriteEntityId();
//
//            boolean isAddToFavorite = false;
//            if(MediaLibrary.getInstance().queryIsFavoriteByMusicInfoDaoId(MusicInfoDao.get_id(), favoritesMusicEntity.favorite_id)){
//                boolean bRet = MediaLibrary.getInstance().removeFavoriteMusicEntity(favoritesMusicEntity.musicinfo_id, favoritesMusicEntity.favorite_id);
//                if(bRet){
//                    isAddToFavorite = false;
//                }
//            }
//            else{
//                boolean bRet = MediaLibrary.getInstance().addFavoriteMusicEntity(favoritesMusicEntity);
//                if(bRet){
//                    isAddToFavorite = true;
//                }
//            }
//
//            if(!isAddToFavorite){
//                Toast.makeText(this, "已取消喜欢", Toast.LENGTH_SHORT).show();
//            }
//            else {
//                Toast.makeText(this, "已添加到我喜欢的单曲", Toast.LENGTH_SHORT).show();
//            }
//            updateLoveState(isAddToFavorite);
        }
        else if(v == mivDownload){

        }
        else if(v == mivShare){
//            MusicInfoDao MusicInfoDao = mService.getCurrentMedia();
//            if(MusicInfoDao == null)
//                return;
//
//            String strShareTitle = getResources().getString(R.string.app_name);
//            strShareTitle = String.format("分享一首%s的%s - 来自%s", MusicInfoDao.getArtist(), MusicInfoDao.getTitle(), strShareTitle);
//            AndroidShare as = new AndroidShare(
//                    this,
//                    strShareTitle,
//                    "http://img6.cache.netease.com/cnews/news2012/img/logo_news.png");
//            as.show();
//            as.setTitle("分享 - " + MusicInfoDao.getTitle());
        }
        else if(v == mivComment){

        }
        else if(v == mivMore){
            if(mPlayingMoreOperDialog == null){
                mPlayingMoreOperDialog = new PlayingMoreOperDialog(this);
            }
            mPlayingMoreOperDialog.show();
//            mPlayingMoreOperDialog.setCurrentMusicInfoDao(mService.getCurrentMedia());
        }
        else if(v == mivPlayMode){
            int playMode = mService.getPlayMode().ordinal();
            playMode = (playMode+1)% PlayMusicImpl.PlayMode.values().length;
            mService.setPlayMode(PlayMusicImpl.PlayMode.values()[playMode]);
            updatePlaymodeState(playMode, true);
        }
        else if(v == mivPre){
//            if(mService.getPlaylistSize() == 0){
//                Toast.makeText(this, "当前无可播放的歌曲", Toast.LENGTH_SHORT).show();
//                return;
//            }

            mService.pre();
        }
        else if(v == mivPlayPause){
//            if(mService.getPlaylistSize() == 0){
//                Toast.makeText(this, "当前无可播放的歌曲", Toast.LENGTH_SHORT).show();
//                return;
//            }

            if(mService.isPlaying()){
                mService.pause();
            }
            else{
                mService.play();
            }
        }
        else if(v == mivNext){
//            if(mService.getPlaylistSize() == 0){
//                Toast.makeText(this, "当前无可播放的歌曲", Toast.LENGTH_SHORT).show();
//                return;
//            }

            mService.next();
        }
        else if(v == mivPlaylist){
            if(mPlayListDialog == null){
                PlaylistDialog.Builder builder = new PlaylistDialog.Builder(this);
                mPlayListDialog = (PlaylistDialog)builder.create();
            }
            mPlayListDialog.setCancelable(true);
            mPlayListDialog.setPlaylistData(mService.getPlaylist());
            mPlayListDialog.setItemPlayState(mService.getCurPlayIndex(), true, mService.isPlaying());
            mPlayListDialog.setPlayListAdapterListener(mListener);
            mPlayListDialog.updatePlaymodeState(mService.getPlayMode(), false);
            mPlayListDialog.show();
        }
        else if(v == mivBack){
            finish();
        }
    }

    public void onStart() {
        super.onStart();
        mClient.connect();
    }

    public void onStop() {
        super.onStop();
        mClient.disconnect();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(fromUser){
            isUpdateCurTimeFromUser = true;
            tvCurTime.setText(formatTime(progress));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if(mService != null){
            mService.seekTo(seekBar.getProgress());
        }
        isUpdateCurTimeFromUser = false;
    }

    @Override
    public void onConnect(PlayMusicService service) {
        mService = service;
        mService.addListener(this);
        initUI();
    }

    @Override
    public void onDisconnect() {
        mService.removeListener(this);
        mService = null;
    }

    @Override
    public void onStateChange(int state) {
        if(mService == null)
            return;

        if(state == IPlayMusic.STATE_PAPERED || state == IPlayMusic.STATE_PLAY){
            updatePlayPauseState(true);

            MusicInfoDao curMusicInfoDao = mService.getCurPlayMusicDao();
            if(curMusicInfoDao != null){
                updatePlayPauseState(true);
            }
            sbProgress.setMax((int)curMusicInfoDao.getDuration());
            tvTotalTime.setText(formatTime((int)curMusicInfoDao.getDuration()));
            artistInfoFragment.setArtistAlbumInfo(mService.getCurPlayMusicDao());
            musicInfoFragment.setMusciInfo(mService.getCurPlayMusicDao());
            if(mPlayListDialog != null && mPlayListDialog.isShowing()){
                mPlayListDialog.setItemPlayState(mService.getCurPlayIndex(), true, true);
            }

            if(mPlayingMoreOperDialog != null){
                mPlayingMoreOperDialog.setCurrentMusicInfoDao(mService.getCurPlayMusicDao());
            }
        }
        else if(state == IPlayMusic.STATE_PAUSE){
            updatePlayPauseState(false);
            if(mPlayListDialog != null && mPlayListDialog.isShowing()){
                mPlayListDialog.setItemPlayState(mService.getCurPlayIndex(), true, false);
            }
        }
//        else if(event.type == MediaPlayer.Event.Stopped){
//            resetUI();
//        }
//        else if(event.type == MediaPlayer.Event.TimeChanged){
//            if(lyricInfoFragment != null){
//                lyricInfoFragment.updateLyric(mService.getTIme());
//            }
//        }
    }

    @Override
    public void onPlayPosUpdate(int percent, int curPos, int duration) {
        updatePlayProgress(curPos, duration);
    }

    @Override
    public void onBufferingUpdate(int cur, int total) {
        sbProgress.setSecondaryProgress(cur);
    }

    class PlayingPagerAdapter extends FragmentStatePagerAdapter {

        public PlayingPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return PlayingActivity.this.mListFragment.get(position);
        }

        @Override
        public int getCount() {
            return PlayingActivity.this.mListFragment.size();
        }
    }
}
