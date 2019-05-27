package com.xyoye.dandanplay.mvp.view;

import com.xyoye.dandanplay.bean.AnimeDetailBean;
import com.xyoye.dandanplay.utils.interf.view.BaseMvpView;
import com.xyoye.dandanplay.utils.interf.view.LoadDataView;

/**
 * Created by xyoye on 2018/7/20.
 */


public interface AnimeDetailView extends BaseMvpView, LoadDataView{
    void showAnimeDetail(AnimeDetailBean detailBean);

    void afterFollow(boolean isFollow);
}
