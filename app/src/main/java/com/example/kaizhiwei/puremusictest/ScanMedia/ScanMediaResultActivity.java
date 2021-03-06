package com.example.kaizhiwei.puremusictest.ScanMedia;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.kaizhiwei.puremusictest.model.scanmusic.ExternFileSource;
import com.example.kaizhiwei.puremusictest.CommonUI.BaseActivty;
import com.example.kaizhiwei.puremusictest.MediaData.MediaLibrary;
import com.example.kaizhiwei.puremusictest.MediaData.MediaScanHelper;
import com.example.kaizhiwei.puremusictest.R;
import com.example.kaizhiwei.puremusictest.model.scanmusic.IScanListener;
import com.r0adkll.slidr.Slidr;

import java.util.HashMap;


/**
 * Created by kaizhiwei on 16/12/30.
 */
public class ScanMediaResultActivity extends BaseActivty implements IScanListener {
    private TextView tvScanResult;
    private TextView tvScanProgressing;
    private TextView tvFinish;
    private ProgressBar pbScanProgring;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_result);
        tvScanResult = (TextView)this.findViewById(R.id.tvScanResult);
        tvScanProgressing = (TextView)this.findViewById(R.id.tvScanProgressing);
        pbScanProgring = (ProgressBar)this.findViewById(R.id.pbScanProgring);
        tvFinish = (TextView)this.findViewById(R.id.tvFinish);
        tvFinish.setVisibility(View.GONE);
        tvFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        setTitle("歌曲扫描中...");
        pbScanProgring.setProgress(0);
        pbScanProgring.setMax(100);
        Slidr.attach(this);
    }

    @Override
    protected void onResume(){
        super.onResume();
        //MediaScanHelper.getInstance().addScanListener(this);
        //MediaScanHelper.getInstance().scanFile(ScanMediaResultActivity.this,"");
    }

    @Override
    protected void onPause(){
        super.onPause();
        //MediaScanHelper.getInstance().removeScanListener(this);
    }

    @Override
    public void onScanStart() {
        tvScanResult.setText("正在扫描...");
        tvScanProgressing.setText("正在计算文件夹大小...");
    }

    @Override
    public void onProcess(String fileName, String filePath, int process) {
        tvScanProgressing.setText(filePath);
        pbScanProgring.setProgress(process);
    }

    @Override
    public void onScanFinish() {
        tvFinish.setVisibility(View.VISIBLE);
//        tvScanResult.setText("扫描完毕共" + mapResult.size() + "首");
//        tvScanProgressing.setText("为您过滤" + filterNum + "个音乐片段");
//        pbScanProgring.setProgress(100);
//        MediaLibrary.getInstance().resetAllMusicInfoDaoInfo(mapResult);
    }
}
