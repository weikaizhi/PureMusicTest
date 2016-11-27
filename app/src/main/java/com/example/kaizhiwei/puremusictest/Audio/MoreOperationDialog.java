package com.example.kaizhiwei.puremusictest.Audio;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.TextView;

import com.example.kaizhiwei.puremusictest.R;

import java.util.ArrayList;
import java.util.List;

import static com.example.kaizhiwei.puremusictest.Audio.MoreOperationAdapter.*;

/**
 * Created by kaizhiwei on 16/11/26.
 */
public class MoreOperationDialog extends Dialog {
    public static final int MORE_ADD_NORMA = 1;
    public static final int MORE_ALBUM_NORMAL = 2;
    public static final int MORE_BELL_NORMAL = 3;
    public static final int MORE_DELETE_NORMAL = 4;
    public static final int MORE_DOWNLOAD_NORMAL = 5;
    public static final int MORE_HIDE_NORMAL = 6;
    public static final int MORE_LOVE_NORMAL = 7;
    public static final int MORE_MV_NORMAL = 8;
    public static final int MORE_NEXTPLAY_NORMAL = 9;
    public static final int MORE_PLAY_NORMAL = 10;
    public static final int MORE_REMOVE_NORMAL = 11;
    public static final int MORE_SHARE_NORMAL = 12;
    public static final int MORE_SONGER_NORMAL = 13;

    private Context context;
    private  List<MoreOperationAdapter.MoreOperationItemData>  mMoreOperAll;
    private List<MoreOperationAdapter.MoreOperationItemData> mListData = new ArrayList<>();
    private TextView tvTitle;
    private GridView gvMoreOperation;

    public MoreOperationDialog(Context context, int theme_Dialog) {
        super(context);
        this.context = context;
        mMoreOperAll = new ArrayList<>();

        MoreOperationAdapter.MoreOperationItemData data = null;
        data = new MoreOperationAdapter.MoreOperationItemData();
        data.id = MORE_ADD_NORMA;
        data.strText = "添加到";
        data.resImage = R.drawable.ic_listmore_add_normal;
        mMoreOperAll.add(data);

        data = new MoreOperationAdapter.MoreOperationItemData();
        data.id = MORE_ALBUM_NORMAL;
        data.strText = "专辑";
        data.resImage = R.drawable.ic_listmore_album_normal;
        mMoreOperAll.add(data);

        data = new MoreOperationAdapter.MoreOperationItemData();
        data.id = MORE_BELL_NORMAL;
        data.strText = "铃声";
        data.resImage = R.drawable.ic_listmore_bell_normal;
        mMoreOperAll.add(data);

        data = new MoreOperationAdapter.MoreOperationItemData();
        data.id = MORE_DELETE_NORMAL;
        data.strText = "删除";
        data.resImage = R.drawable.ic_listmore_delete_normal;
        mMoreOperAll.add(data);

        data = new MoreOperationAdapter.MoreOperationItemData();
        data.id = MORE_DOWNLOAD_NORMAL;
        data.strText = "下载";
        data.resImage = R.drawable.ic_listmore_download_normal;
        mMoreOperAll.add(data);

        data = new MoreOperationAdapter.MoreOperationItemData();
        data.strText = "下一首播放";
        data.resImage = R.drawable.ic_listmore_nextplay_normal;
        mMoreOperAll.add(data);

        data = new MoreOperationAdapter.MoreOperationItemData();
        data.id = MORE_HIDE_NORMAL;
        data.strText = "隐藏";
        data.resImage = R.drawable.ic_listmore_filter_normal;
        mMoreOperAll.add(data);

        data = new MoreOperationAdapter.MoreOperationItemData();
        data.id = MORE_LOVE_NORMAL;
        data.strText = "喜欢";
        data.resImage = R.drawable.ic_listmore_love_normal;
        data.resSelImage = R.drawable.ic_listmore_love_hl;
        mMoreOperAll.add(data);

        data = new MoreOperationAdapter.MoreOperationItemData();
        data.id = MORE_MV_NORMAL;
        data.strText = "MV";
        data.resImage = R.drawable.ic_listmore_mv_normal;
        mMoreOperAll.add(data);

        data = new MoreOperationAdapter.MoreOperationItemData();
        data.id = MORE_NEXTPLAY_NORMAL;
        data.strText = "下一首播放";
        data.resImage = R.drawable.ic_listmore_nextplay_normal;
        mMoreOperAll.add(data);

        data = new MoreOperationAdapter.MoreOperationItemData();
        data.id = MORE_PLAY_NORMAL;
        data.strText = "播放";
        data.resImage = R.drawable.ic_listmore_play_normal;
        mMoreOperAll.add(data);

        data = new MoreOperationAdapter.MoreOperationItemData();
        data.id = MORE_REMOVE_NORMAL;
        data.strText = "移除";
        data.resImage = R.drawable.ic_listmore_remove_normal;
        mMoreOperAll.add(data);

        data = new MoreOperationAdapter.MoreOperationItemData();
        data.id = MORE_SHARE_NORMAL;
        data.strText = "分享";
        data.resImage = R.drawable.ic_listmore_share_normal;
        mMoreOperAll.add(data);

        data = new MoreOperationAdapter.MoreOperationItemData();
        data.id = MORE_SONGER_NORMAL;
        data.strText = "歌手";
        data.resImage = R.drawable.ic_listmore_songer_normal;
        mMoreOperAll.add(data);
    }

    public String getTitle() {
        return (String) tvTitle.getText();
    }

    public void setTitle(String strTitle) {
        this.tvTitle.setText(strTitle);
    }

    public void setMoreOperData(List<Integer> listMore){
        List<MoreOperationAdapter.MoreOperationItemData> listData = new ArrayList<>();
        for(int i = 0;i < listMore.size();i++){
            for(int j = 0;j < mMoreOperAll.size();j++){
                if(listMore.get(i) == mMoreOperAll.get(j).id){
                    listData.add(mMoreOperAll.get(j));
                }
            }
        }

        MoreOperationAdapter adapter = new MoreOperationAdapter(context, listData);
        gvMoreOperation.setAdapter(adapter);
    }

    public void setContentView(View view) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.setContentView(view);
        tvTitle = (TextView) view.findViewById(R.id.tvTitle);
        gvMoreOperation = (GridView) view.findViewById(R.id.gvMoreOperation);
        gvMoreOperation.setSelector(new ColorDrawable(Color.TRANSPARENT));

        Window window = this.getWindow();
        window.getDecorView().setPadding(0,0,0,0);
        window.setWindowAnimations(R.style.ActionSheetDialogAnimation);
        // 可以在此设置显示动画
        WindowManager.LayoutParams wl = window.getAttributes();
        wl.x = 0;
        // 以下这两句是为了保证按钮可以水平满屏
        wl.width = ViewGroup.LayoutParams.MATCH_PARENT;
        wl.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        wl.gravity = Gravity.BOTTOM;

        window.setAttributes(wl);
    }

    public static class Builder {
        private Context context;


        public Builder(Context context) {
            this.context = context;
        }

        public MoreOperationDialog create() {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final MoreOperationDialog dialog = new MoreOperationDialog(context,R.style.Dialog);
            View layout = inflater.inflate(R.layout.more_operation_dialog, null);


            dialog.setContentView(layout);

            return dialog;
        }
    }
}