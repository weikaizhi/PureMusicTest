package com.example.kaizhiwei.puremusictest.ScanMedia;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kaizhiwei.puremusictest.MediaData.MediaEntity;
import com.example.kaizhiwei.puremusictest.MediaData.MediaLibrary;
import com.example.kaizhiwei.puremusictest.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by 24820 on 2016/12/14.
 */
public class SelectFolderDialog extends Dialog implements View.OnClickListener{
    private TextView btnDelete;
    private TextView tvTitle;
    private SelectFolderAdapter mAdapter;
    private ListView lvFolder;
    private Map<ISelectFolderDialogListener,ISelectFolderDialogListener> mMapListener;

    public interface ISelectFolderDialogListener{
        void onDestory();

        public void onSelectFolderFinish(SelectFolderDialog dialog, int tag);
    }

    protected SelectFolderDialog(Context context) {
        super(context);
    }

    public SelectFolderDialog(@NonNull Context context, @StyleRes int themeResId) {
        super(context, themeResId);
    }

    protected SelectFolderDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    public void registerListener(ISelectFolderDialogListener listener){
        if(mMapListener == null){
            mMapListener = new HashMap<>();
        }

        mMapListener.put(listener,listener);
    }

    public void unregisterListener(ISelectFolderDialogListener listener){
        if(mMapListener == null){
            mMapListener = new HashMap<>();
        }

        if(mMapListener.containsKey(listener)){
            mMapListener.remove(listener);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setContentView(View view) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN , WindowManager.LayoutParams. FLAG_FULLSCREEN);
        super.setContentView(view);
        lvFolder = (ListView) this.findViewById(R.id.lvFolder);
        btnDelete = (TextView) this.findViewById(R.id.btnDelete);
        tvTitle = (TextView) this.findViewById(R.id.tvTitle);
        btnDelete.setOnClickListener(this);

        DisplayMetrics dm = null;
        dm = getContext().getApplicationContext().getResources().getDisplayMetrics();

        Window window = this.getWindow();
        window.getDecorView().setPadding(0,0,0,0);
        window.setWindowAnimations(R.style.ActionSheetDialogAnimation);
        window.setBackgroundDrawableResource(android.R.color.transparent);
        // 可以在此设置显示动画
        WindowManager.LayoutParams wl = window.getAttributes();
        wl.x = 0;
        wl.y = 0;
        // 以下这两句是为了保证按钮可以水平满屏
        wl.width = ViewGroup.LayoutParams.MATCH_PARENT;
        wl.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        //wl.height = view.getHeight() > 300 * dm.densityDpi ? 300 * dm.densityDpi : view.getHeight();
        wl.gravity = Gravity.BOTTOM;

        window.setAttributes(wl);
    }

    public void show(){
        super.show();
    }

    public void setTitle(String title){
        if(tvTitle != null){
            tvTitle.setText(title);
        }
    }

    public List<String> getFilterFolders(){
        List<String> listFilterFolder = new ArrayList<>();
        if(mAdapter == null)
            return listFilterFolder;

        List<Integer> listChecked = mAdapter.getCheckedItems();
        for(int i = 0;i < listChecked.size();i++){
            listFilterFolder.add((String)mAdapter.getItem(listChecked.get(i)));
        }

        return listFilterFolder;
    }

    public void setMediaEntityData(List<MediaEntity> list){
        if(list == null)
            return;

        List<String> listFolderData = new ArrayList<>();
        for(int i = 0;i < list.size();i++){
            if(listFolderData.contains(list.get(i).save_path) == false){
                listFolderData.add(list.get(i).save_path);
            }
        }

        mAdapter = new SelectFolderAdapter(this.getContext(), listFolderData);
        lvFolder.setAdapter(mAdapter);
    }

    @Override
    public void onClick(View v) {
        if(v == btnDelete){
            for (Map.Entry<ISelectFolderDialogListener,ISelectFolderDialogListener> entry : mMapListener.entrySet()) {

                ISelectFolderDialogListener listener = entry.getValue();
                listener.onSelectFolderFinish(this, 0);
            }
            dismiss();
        }
    }

    public static class Builder {
        private Context context;


        public Builder(Context context) {
            this.context = context;
        }

        public SelectFolderDialog create() {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final SelectFolderDialog dialog = new SelectFolderDialog(context,R.style.Dialog);
            View layout = inflater.inflate(R.layout.alertdialog_select_folder, null);

            dialog.setContentView(layout);

            return dialog;
        }
    }
}
