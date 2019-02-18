package com.xyoye.dandanplay.utils.torrent;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.StringUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.xyoye.dandanplay.app.IApplication;
import com.xyoye.dandanplay.utils.AppConfig;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import libtorrent.Libtorrent;
import libtorrent.StatsTorrent;

/**
 * Created by xyy on 2018/10/23.
 */

public class TorrentTask{

    private Context context;

    public TorrentTask(Context context){
        this.context = context;
    }

    public Torrent prepare(Torrent torrent){
        return prepareTorrent(torrent);
    }

    public boolean start(Torrent torrent){
        return startTorrent(torrent);
    }

    public void pause(Torrent torrent){
        pauseTorrent(torrent);
    }

    //解析torrent内容
    private Torrent prepareTorrent(Torrent oldTorrent){
        Torrent torrent = new Torrent();
        torrent.setPath(oldTorrent.getPath());
        torrent.setAnimeTitle(oldTorrent.getAnimeTitle());
        torrent.setEpisodeId(oldTorrent.getEpisodeId());
        torrent.setDanmuPath(oldTorrent.getDanmuPath());
        torrent.setDone(oldTorrent.isDone());
        torrent.setMagnet(oldTorrent.getMagnet());

        byte[] torrentData;
        File torrentFile = new File(oldTorrent.getPath());
        try {
            torrentData = FileUtils.readFileToByteArray(torrentFile);
        } catch (IOException e) {
            ToastUtils.showShort("找不到种子文件，目前暂不支持读取SD卡文件");
            throw new RuntimeException(e);
        }

        String downloadFolder = StringUtils.isEmpty(torrent.getAnimeTitle())
                ? AppConfig.getInstance().getDownloadFolder()
                : AppConfig.getInstance().getDownloadFolder() + "/" + torrent.getAnimeTitle();
        File folder = new File(downloadFolder);
        if (!folder.exists()){
            if (!folder.mkdirs()){
                throw new RuntimeException("not found download folder, create folder fail ："+downloadFolder);
            }
        }
        Uri uri = Uri.fromFile(folder);

        if (!TorrentUtil.prepareTorrentFromBytes(torrent, uri, torrentData)){
            Toast.makeText(context, "解析种子文件失败", Toast.LENGTH_LONG).show();
            return null;
        }else {
            return torrent;
        }
    }

    //下载torrent内容
    private boolean startTorrent(Torrent torrent) {
        int torrentStatus = Libtorrent.torrentStatus(torrent.getId());
        torrent.setStatus(torrentStatus);
        if (torrentStatus == Libtorrent.StatusPaused || torrentStatus == Libtorrent.StatusQueued){
            if (!Libtorrent.startTorrent(torrent.getId())){
                LogUtils.e(Libtorrent.error());
                Toast.makeText(context, "错误，无法下载", Toast.LENGTH_LONG).show();
                return false;
            }
            if (Libtorrent.torrentTrackersCount(torrent.getId()) == 0){
                for (String tracker : IApplication.trackers){
                    if (!StringUtils.isEmpty(tracker) && !tracker.startsWith("#"))
                        Libtorrent.torrentTrackerAdd(torrent.getId(), tracker);
                }
            }
            LogUtils.e("tracker:"+Libtorrent.torrentTrackersCount(torrent.getId()));
            torrent.setUpdate(true);
            StatsTorrent b = Libtorrent.torrentStats(torrent.getId());
            torrent.downloaded.start(b.getDownloaded());
            torrent.uploaded.start(b.getUploaded());
        }
        return true;
    }

    //暂停torrent下载
    private void pauseTorrent(Torrent torrent){
        if (torrent.getId() == -1)
            return;
        Libtorrent.stopTorrent(torrent.getId());
        TorrentUtil.updateTorrent(torrent);
        StatsTorrent b = Libtorrent.torrentStats(torrent.getId());
        torrent.downloaded.end(b.getDownloaded());
        torrent.uploaded.end(b.getUploaded());
    }
}
