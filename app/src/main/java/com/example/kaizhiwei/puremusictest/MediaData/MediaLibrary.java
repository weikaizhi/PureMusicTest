package com.example.kaizhiwei.puremusictest.MediaData;

import android.net.Uri;
import android.os.Message;
import android.text.TextUtils;

import com.example.kaizhiwei.puremusictest.dao.MusicInfoDao;
import com.example.kaizhiwei.puremusictest.util.BusinessCode;
import com.example.kaizhiwei.puremusictest.util.DeviceUtil;
import com.example.kaizhiwei.puremusictest.util.ExtensionUtil;
import com.example.kaizhiwei.puremusictest.util.StringUtil;
import com.example.kaizhiwei.puremusictest.base.BaseHandler;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by kaizhiwei on 16/11/6.
 */

public class MediaLibrary {

    public interface IMediaScanListener{
        public void onScanStart();
        public void onScaning(String fileInfo, float progress);
        public void onScanFinish();
    }

    private static MediaLibrary instance;
    private static final List<String> FOLDER_BLACKLIST;
    private List<MusicInfoDao> mMusicInfoDaoList = new ArrayList<MusicInfoDao>();
    private ReadWriteLock mMusicInfoDaoListLock = new ReentrantReadWriteLock();

    private List<FavoriteEntity> mFavoriteListData = new ArrayList<>();

    private List<FavoritesMusicEntity> mFavoriteMusicListData = new ArrayList<>();
    private ReadWriteLock mFavoriteMusicListLock = new ReentrantReadWriteLock();

    private boolean isScaning;
    private boolean isForceStop;
    private ExecutorService mPool;

    private Map<IMediaScanListener,Object> mMapListener;

    static {
        String[] folder_blacklist = {
                "/alarms",
                "/notifications",
                "/ringtones",
                "/media/alarms",
                "/media/notifications",
                "/media/ringtones",
                "/media/audio/alarms",
                "/media/audio/notifications",
                "/media/audio/ringtones",
                "/android/data",
                "/android/media"
        };

        FOLDER_BLACKLIST = Arrays.asList(folder_blacklist);
    }

    synchronized public static MediaLibrary getInstance(){
        if(instance == null){
            instance = new MediaLibrary();
        }
        return  instance;
    }

    private MediaLibrary(){
        mPool = Executors.newFixedThreadPool(1);
    }

    public void resetAllMusicInfoDaoInfo(HashMap<String, String> mapResult){
        if(mapResult == null)
            return;

        MediaDataBase.getInstance().deleteAllMusicInfo();
        mMusicInfoDaoListLock.writeLock().lock();
        mMusicInfoDaoList.clear();
        for(HashMap.Entry<String, String> entry : mapResult.entrySet()){
            File file = new File(entry.getKey());
            if(file.exists() == false)
                continue;

            String fileUri = Uri.fromFile(file).toString();
            Media media = new Media(VLCInstance.getInstance(), Uri.parse(fileUri));
            media.parse();
            if(media.getDuration() <= 0 || (media.getTrackCount() != 0 && TextUtils.isEmpty(media.getTrack(0).codec))){
                media.release();
                continue;
            }

            MusicInfoDao MusicInfoDao = new MusicInfoDao();
            media.release();
            MediaDataBase.getInstance().insertMusicInfo(MusicInfoDao);
            mMusicInfoDaoList.add(MusicInfoDao);
        }
        mMusicInfoDaoListLock.writeLock().unlock();
    }

    public void initData(){
        if(mMusicInfoDaoList == null || mMusicInfoDaoList.size() == 0){
            mMusicInfoDaoListLock.writeLock().lock();
            List<MusicInfoDao> list = MediaDataBase.getInstance().queryAllMusicInfo();
            mMusicInfoDaoList.addAll(list);
            mMusicInfoDaoListLock.writeLock().unlock();
        }

        if(mFavoriteListData.size() == 0){
            List<FavoriteEntity> list = MediaDataBase.getInstance().queryAllFavoriteInfo();
            mFavoriteListData.addAll(list);
        }

        if(mFavoriteMusicListData == null || mFavoriteMusicListData.size() == 0){
            mFavoriteMusicListLock.writeLock().lock();
            List<FavoritesMusicEntity> list = MediaDataBase.getInstance().queryAllFavoriteMusicInfo();
            mFavoriteMusicListData.addAll(list);
            mFavoriteMusicListLock.writeLock().unlock();

            for(int i = 0;i < list.size();i++){
                FavoriteEntity favoriteEntity = getFavoriteEntityById(list.get(i).favorite_id);
                if(favoriteEntity != null){
                    favoriteEntity.favoriteMusicNum++;
                }
            }
        }
    }

    public List<MusicInfoDao> getAllMediaEntrty(){

        if(mMusicInfoDaoList == null || mMusicInfoDaoList.size() == 0){
            getAllFavoriteMusicEntity();
        }

        mMusicInfoDaoListLock.readLock().lock();
        List<MusicInfoDao> newList = new ArrayList<>();
        newList.addAll(mMusicInfoDaoList);
        mMusicInfoDaoListLock.readLock().unlock();
        return newList;
    }

    public void asyncGetAllMusicInfoDao(final BaseHandler handler){
        if(mPool == null || handler == null){
            return;
        }

        mPool.execute(new Runnable() {
            @Override
            public void run() {
                List<MusicInfoDao> list = getAllMediaEntrty();
                Message msg = handler.obtainMessage(BusinessCode.BUSINESS_CODE_SUCCESS, list);
                handler.sendMessage(msg);
            }
        });
    }

    public int getMusicInfoDaoSize(){
        if(mMusicInfoDaoList == null)
            return 0;

        return mMusicInfoDaoList.size();
    }

    public boolean removeMusicInfoDao(MusicInfoDao entity){
        if(entity == null || entity.get_id() < 0)
            return false;

        boolean bRet = MediaDataBase.getInstance().deleteMusicInfoByEntityId(entity);
        if(bRet){
            for(int i = 0;i < mMusicInfoDaoList.size();i++){
                if(mMusicInfoDaoList.get(i).get_id() == entity.get_id()){
                    mMusicInfoDaoList.remove(i);
                    break;
                }
            }
        }
        return bRet;
    }

    public boolean mutilRemoveMusicInfoDao(List<MusicInfoDao> list){
        if(list == null)
            return false;

        boolean bRet = false;
        for(int i = 0;i < mMusicInfoDaoList.size();i++){
            for(int j = 0;j < list.size();j++){
                if(mMusicInfoDaoList.get(i).get_id() == list.get(j).get_id()){
                    bRet = MediaDataBase.getInstance().deleteMusicInfoByEntityId(list.get(j));
                    if(bRet){
                        mMusicInfoDaoList.remove(i);
                        i--;
                        break;
                    }
                }
            }
        }
        return bRet;
    }

    public MusicInfoDao getMusicInfoDaoById(long id){
        if(mMusicInfoDaoList == null || mMusicInfoDaoList.size() == 0){
            return MediaDataBase.getInstance().queryMusicInfoById(id);
        }

        MusicInfoDao entity = null;
        for(int i = 0;i < mMusicInfoDaoList.size();i++){
            if(mMusicInfoDaoList.get(i).get_id() == id){
                entity = mMusicInfoDaoList.get(i);
                break;
            }
        }

        return entity;
    }

    public List<FavoritesMusicEntity> getAllFavoriteMusicEntity(){
        if(mFavoriteMusicListData == null || mFavoriteMusicListData.size() == 0){
            mFavoriteMusicListLock.writeLock().lock();
            List<FavoritesMusicEntity> list = MediaDataBase.getInstance().queryAllFavoriteMusicInfo();
            mFavoriteMusicListData.addAll(list);
            mFavoriteMusicListLock.writeLock().unlock();
        }

        mFavoriteMusicListLock.readLock().lock();
        List<FavoritesMusicEntity> newList = new ArrayList<>();
        newList.addAll(mFavoriteMusicListData);
        mFavoriteMusicListLock.readLock().unlock();
        return newList;
    }

    public boolean addFavoriteMusicEntity(FavoritesMusicEntity entity){
        if(entity == null)
            return false;

        if(!isExistFavorite(entity.favorite_id))
            return false;

        if(queryIsFavoriteByMusicInfoDaoId(entity._id, getDefaultFavoriteEntityId()))
            return true;

        mFavoriteMusicListLock.writeLock().lock();
        mFavoriteMusicListData.add(entity);
        mFavoriteMusicListLock.writeLock().unlock();
        boolean bRet =  MediaDataBase.getInstance().insertFavoriteMusicInfo(entity);
        if(bRet){
            FavoriteEntity favoriteEntity = getFavoriteEntityById(entity.favorite_id);
            if(favoriteEntity != null){
                favoriteEntity.favoriteMusicNum++;
            }
        }

        return bRet;
    }

    public boolean removeFavoriteMusicEntity(long musicEntityId, long favoriteEntityId){
        if(musicEntityId < 0 || favoriteEntityId < 0)
            return false;

        if(!isExistFavorite(favoriteEntityId))
            return false;

        mFavoriteMusicListLock.writeLock().lock();
        for(int i = 0;i < mFavoriteMusicListData.size();i++){
            if(musicEntityId == mFavoriteMusicListData.get(i).musicinfo_id && mFavoriteMusicListData.get(i).favorite_id == favoriteEntityId){
                mFavoriteMusicListData.remove(i);
                break;
            }
        }
        mFavoriteMusicListLock.writeLock().unlock();
        boolean bRet =  MediaDataBase.getInstance().deleteFavoriteMusicInfoByMusicInfoDaoId(musicEntityId, favoriteEntityId);
        if(bRet){
            FavoriteEntity favoriteEntity = getFavoriteEntityById(favoriteEntityId);
            if(favoriteEntity != null){
                favoriteEntity.favoriteMusicNum--;
            }
        }

        return bRet;

    }

    public boolean queryIsFavoriteByMusicInfoDaoId(long MusicInfoDaoId, long favoriteEntityId){
        mFavoriteMusicListLock.readLock().lock();
        boolean bFavorite = false;
        for(int i = 0;i < mFavoriteMusicListData.size();i++){
            if(mFavoriteMusicListData.get(i).musicinfo_id == MusicInfoDaoId && mFavoriteMusicListData.get(i).favorite_id == favoriteEntityId){
                bFavorite = true;
                break;
            }
        }
        mFavoriteMusicListLock.readLock().unlock();
        return bFavorite;
    }

    public List<FavoriteEntity> getAllFavoriteEntity(){
        if(mFavoriteListData == null || mFavoriteListData.size() == 0){
            List<FavoriteEntity> list = MediaDataBase.getInstance().queryAllFavoriteInfo();
            mFavoriteListData.addAll(list);
        }

        List<FavoriteEntity> newList = new ArrayList<>();
        newList.addAll(mFavoriteListData);
        return newList;
    }

    public boolean addFavoriteEntity(FavoriteEntity entity){
        if(entity == null)
            return false;

        boolean bRet = MediaDataBase.getInstance().insertFavoriteInfo(entity);
        if(bRet)
            mFavoriteListData.add(entity);
        return bRet;
    }

    public boolean modifyFavoriteEntity(FavoriteEntity entity){
        if(entity == null || entity._id < 0)
            return false;

        boolean bRet = MediaDataBase.getInstance().modifyFavoriteInfo(entity);
        if(bRet){
            for(int i = 0;i < mFavoriteListData.size();i++){
                if(mFavoriteListData.get(i)._id == entity._id){
                    mFavoriteListData.get(i).strFavoriteName = entity.strFavoriteName;
                    mFavoriteListData.get(i).strFavoriteDesc = entity.strFavoriteDesc;
                    mFavoriteListData.get(i).strFavoriteImgPath = entity.strFavoriteImgPath;
                }
            }
        }
        return bRet;
    }

    public boolean removeFavoriteEntity(FavoriteEntity entity){
        if(entity == null || entity._id < 0)
            return false;

        boolean bRet = MediaDataBase.getInstance().deleteFavoriteInfo(entity);
        if(bRet){
            List<FavoritesMusicEntity> list = getFavoriteMusicById(entity._id);
            for(int i = 0;i < list.size();i++){
                removeFavoriteMusicEntity(list.get(i)._id, entity._id);
            }
            mFavoriteListData.remove(entity);
        }

        return bRet;
    }

    public long getDefaultFavoriteEntityId(){
        for(int i = 0;i < mFavoriteListData.size();i++){
            if(mFavoriteListData.get(i).favoriteType == FavoriteEntity.DEFAULT_FAVORITE_TYPE){
                return mFavoriteListData.get(i)._id;
            }
        }

        return -1;
    }

    public FavoriteEntity getFavoriteEntityById(long favoriteEntityId){
        for(int i = 0;i < mFavoriteListData.size();i++){
            if(mFavoriteListData.get(i)._id == favoriteEntityId){
                return mFavoriteListData.get(i);
            }
        }

        return null;
    }

    public List<FavoritesMusicEntity> getFavoriteMusicById(long favoriteEntityId){
        List<FavoritesMusicEntity> list = new ArrayList<>();
        for(int i = 0;i < mFavoriteMusicListData.size();i++){
            FavoritesMusicEntity musicEntity = mFavoriteMusicListData.get(i);
            if(musicEntity == null)
                continue;

            if(musicEntity.favorite_id == favoriteEntityId){
                list.add(musicEntity);
            }
        }

        return list;
    }

    public boolean isExistFavorite(long favoriteEntityId){
        for(int i = 0;i < mFavoriteListData.size();i++){
            if(mFavoriteListData.get(i)._id == favoriteEntityId){
                return true;
            }
        }

        return false;
    }

    public boolean isExistFavorite(String favoriteName, long excludeId){
        for(int i = 0;i < mFavoriteListData.size();i++){
            if(excludeId == mFavoriteListData.get(i)._id)
                continue;

            if(mFavoriteListData.get(i).strFavoriteName.equals(favoriteName)){
                return true;
            }
        }

        return false;
    }

    public void startScan(){
        if(isScaning){
            isForceStop = true;
            isScaning = false;
        }

        isScaning = true;
        notifyScanStart();
        mPool.execute(scanRunnable);
        isForceStop = false;
    }

    public void stopScan(){
        isForceStop = false;
        isScaning = false;
        if(!mPool.isShutdown()){
            mPool.shutdown();
        }
    }

    public void notifyScanStart(){
        if(mMapListener == null)
            return ;

        for(IMediaScanListener key : mMapListener.keySet()){
            if(key != null){
                key.onScanStart();
            }
        }
    }

    public void notifyScaning(String fileInfo, float progress){
        if(mMapListener == null)
            return ;

        for(IMediaScanListener key : mMapListener.keySet()){
            if(key != null){
                key.onScaning(fileInfo, progress);
            }
        }
    }

    public void notifyScanFinish(){
        if(MediaDataBase.getInstance().isEmptyMusicInfo()){

        }
        else{
            MediaDataBase.getInstance().deleteAllMusicInfo();
        }
        for(int i = 0;i < mMusicInfoDaoList.size();i++){
            MediaDataBase.getInstance().insertMusicInfo(mMusicInfoDaoList.get(i));
        }

        if(mMapListener == null)
            return ;

        for(IMediaScanListener key : mMapListener.keySet()){
            if(key != null){
                key.onScanFinish();
            }
        }
        isScaning = false;
    }

    public void registerListener(IMediaScanListener listener){
        if(mMapListener == null){
            mMapListener = new HashMap<>();
        }

        mMapListener.put(listener,listener);
    }

    public void unregisterListener(IMediaScanListener listener){
        if(mMapListener == null){
            mMapListener = new HashMap<>();
        }

        if(mMapListener.containsKey(listener)){
            mMapListener.remove(listener);
        }
    }

    private static class MediaFileFilter implements FileFilter{

        @Override
        public boolean accept(File file) {
            boolean accept = false;
            if(file.isHidden() == false){
                if(file.isDirectory() && FOLDER_BLACKLIST.contains(file.getPath().toLowerCase())){
                    accept = true;
                }
                else{
                    String fileExtention = StringUtil.getExtention(file.getAbsolutePath());
                    if(ExtensionUtil.AUDIO.contains(fileExtention)  ||
                            ExtensionUtil.SUBTITLES.contains(fileExtention) ||
                            ExtensionUtil.PLAYLIST.contains(fileExtention)){
                        accept = true;
                    }
                }

            }
            return accept;
        }
    }

    private Runnable scanRunnable = new Runnable() {
        private final Stack<File> directoies = new Stack<File>();
        private List<String> scannedPath = new ArrayList<String>();

        @Override
        public void run() {
            mMusicInfoDaoListLock.writeLock().lock();
            mMusicInfoDaoList.clear();
            mMusicInfoDaoListLock.writeLock().unlock();

            scannedPath.clear();
            directoies.clear();
            LibVLC libVlc = VLCInstance.getInstance();

            List<String> mediaDirs =  DeviceUtil.getStorageDirectories();
            for(int i = 0;i < mediaDirs.size();i++){
                File file = new File(mediaDirs.get(i));
                if(file.exists())
                    directoies.add(file);
            }

            LinkedList<File> mediaToScan = new LinkedList<File>();
            MediaFileFilter mediaFileFilter = new MediaFileFilter();
            List<String> ignoreList = new ArrayList<String>();
            while (directoies.isEmpty() == false){
                File dir = directoies.pop();
                String dirPath = dir.getAbsolutePath();
                //过滤系统目录
                if(dirPath.startsWith("/proc/") || dirPath.startsWith("/sys/") || dirPath.startsWith("/dev/"))
                    continue;

                if(scannedPath.contains(dirPath))
                    continue;

                scannedPath.add(dirPath);

                if(new File(dirPath + File.separator + ".nomedia").exists()){
                    ignoreList.add("file://" + dirPath);
                    continue;
                }

                String[] files = dir.list();
                if(files != null){
                    for(String fileName : files){
                        File subFile = new File(dirPath + File.separator + fileName);
                        if(subFile.isDirectory())
                        {
                            directoies.add(subFile);
                        }
                        else if(subFile.isFile()){
                            if(mediaFileFilter.accept(subFile)){
                                mediaToScan.add(subFile);
                                notifyScaning(fileName, 0);
                            }
                        }
                    }
                }

                if(isForceStop)
                    break;
            }

            if(isForceStop)
                return ;

            for(File file : mediaToScan){
                String fileUri = Uri.fromFile(file).toString();
                Media media = new Media(libVlc, Uri.parse(fileUri));
                media.parse();
                if(media.getDuration() <= 0 || (media.getTrackCount() != 0 && TextUtils.isEmpty(media.getTrack(0).codec))){
                    media.release();
                    continue;
                }

                MusicInfoDao entrty = new MusicInfoDao();
                media.release();
                mMusicInfoDaoListLock.writeLock().lock();
                mMusicInfoDaoList.add(entrty);
                mMusicInfoDaoListLock.writeLock().unlock();
            }
            notifyScanFinish();
        }
    };


}
