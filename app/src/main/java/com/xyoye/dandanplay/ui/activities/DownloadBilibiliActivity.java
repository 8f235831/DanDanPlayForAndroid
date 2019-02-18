package com.xyoye.dandanplay.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.blankj.utilcode.util.ToastUtils;
import com.jaeger.library.StatusBarUtil;
import com.xyoye.dandanplay.R;
import com.xyoye.dandanplay.base.BaseMvcActivity;
import com.xyoye.dandanplay.ui.weight.dialog.BilibiliDownloadDialog;
import com.xyoye.dandanplay.utils.CommonUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by YE on 2018/7/28.
 */

public class DownloadBilibiliActivity extends BaseMvcActivity implements View.OnClickListener{
    public final static int SELECT_WEB = 106;

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.download_by_av_button)
    Button downloadAvButton;
    @BindView(R.id.download_by_url_button)
    Button downloadUrlButton;
    @BindView(R.id.select_url_bt)
    Button selectUrlBt;
    @BindView(R.id.av_input_et)
    TextInputEditText avInputEt;
    @BindView(R.id.url_input_et)
    TextInputEditText urlInputEt;

    @Override
    protected int initPageLayoutID() {
        return R.layout.activity_download_bilibili;
    }

    @Override
    public void initPageView() {

    }

    @Override
    public void initPageViewListener() {
        downloadAvButton.setOnClickListener(this);
        downloadUrlButton.setOnClickListener(this);
        selectUrlBt.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.download_by_av_button:
                String avNumber = avInputEt.getText().toString();
                if (avNumber.isEmpty()){
                    ToastUtils.showShort("AV号不能为空");
                }else if(!CommonUtils.isNum(avNumber)){
                    ToastUtils.showShort("请输入纯数字AV号");
                }else {
                    BilibiliDownloadDialog downloadByAvDialog = new BilibiliDownloadDialog(DownloadBilibiliActivity.this, R.style.Dialog, avNumber, "av");
                    downloadByAvDialog.show();
                }
                break;
            case R.id.download_by_url_button:
                String urlLink = urlInputEt.getText().toString();
                if (urlLink.isEmpty()){
                    ToastUtils.showShort("视频链接不能为空");
                }else if (!CommonUtils.isUrl(urlLink)){
                    ToastUtils.showShort("请输入正确视频链接");
                }else {
                    BilibiliDownloadDialog downloadByUrlDialog = new BilibiliDownloadDialog(DownloadBilibiliActivity.this, R.style.Dialog, urlLink, "url");
                    downloadByUrlDialog.show();
                }
                break;
            case R.id.select_url_bt:
                Intent intent = new Intent(DownloadBilibiliActivity.this, WebViewActivity.class);
                intent.putExtra("title","选择链接");
                intent.putExtra("link", "http://www.bilibili.com");
                intent.putExtra("isSelect", true);
                startActivityForResult(intent, SELECT_WEB);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_WEB && data!=null){
            String selectUrl = data.getStringExtra("selectUrl");
            if (selectUrl.isEmpty()){
                ToastUtils.showShort( "视频链接不能为空");
            }else if (!CommonUtils.isUrl(selectUrl)){
                ToastUtils.showShort("请输入正确视频链接");
            }else {
                BilibiliDownloadDialog downloadByUrlDialog = new BilibiliDownloadDialog(DownloadBilibiliActivity.this, R.style.Dialog, selectUrl, "url");
                downloadByUrlDialog.show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
