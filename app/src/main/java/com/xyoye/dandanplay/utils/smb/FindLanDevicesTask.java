package com.xyoye.dandanplay.utils.smb;

import android.os.SystemClock;
import android.util.Log;

import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.StringUtils;
import com.xyoye.dandanplay.bean.LanDeviceBean;
import com.xyoye.dandanplay.utils.Config;
import com.xyoye.dandanplay.utils.JsonUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jcifs.Address;
import jcifs.context.SingletonContext;

/**
 * Created by xyy on 2018/11/19.
 */

public class FindLanDevicesTask implements Runnable {
    private final static String TAG = FindLanDevicesTask.class.getSimpleName();

    private String mLocalIp;
    private boolean mAbort;
    private FindLanDevicesListener mListener;

    public FindLanDevicesTask(String ip, FindLanDevicesListener listener){
        mLocalIp = ip;
        mListener = listener;
    }

    public void abort(){
        mAbort = true;
    }

    @Override
    public void run() {
        LanDeviceBean saveDevice = null;
        //确保保存的设备能新增进去
        boolean isAddSaveDevice = false;
        String device = SPUtils.getInstance().getString(Config.AppConfig.SMB_DEVICE);
        if (!StringUtils.isEmpty(device)){
            saveDevice = JsonUtil.fromJson(device, LanDeviceBean.class);
        }
        List<LanDeviceBean> deviceList = new ArrayList<>();
        String netRange = mLocalIp.substring(0, mLocalIp.lastIndexOf(".") + 1);
        LinkedList<SocketChannel> sockets = new LinkedList<>();
        Selector selector;
        try {
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
            mListener.onEnd(new ArrayList<>());
            return;
        }

        for (int i = 1; i < 255; i++) {
            final String ip = netRange.concat(String.valueOf(i));
            //try to connect to tcp port 445
            SocketChannel socketChannel = null;
            try {
                socketChannel = SocketChannel.open();
            } catch (IOException ignored) {}
            if (socketChannel == null) continue;
            sockets.add(socketChannel);

            try {
                socketChannel.configureBlocking(false);
                socketChannel.register(selector, SelectionKey.OP_CONNECT);
                socketChannel.connect(new InetSocketAddress(ip, 445));
            } catch (IOException ignored) { }
        }

        final long readStartTime = SystemClock.elapsedRealtime();
        while (!mAbort && (SystemClock.elapsedRealtime() - readStartTime) < 1000) {
            int readyChannels = 0;
            if (selector != null) {
                try {
                    readyChannels = selector.select(150);
                } catch (IOException ignored) { }
            }
            if (readyChannels == 0) continue;
            Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
            while(keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                keyIterator.remove();
                if (key.isValid() && key.isConnectable()) {
                    SocketChannel currentChannel = (SocketChannel) key.channel();

                    boolean v = false;
                    try {
                        v = currentChannel.finishConnect();
                    }
                    catch (IOException ignored) {}
                    finally {
                        try {
                            currentChannel.close();
                        } catch (IOException ignored) {}
                    }

                    if (v) {
                        int ipNumber = sockets.indexOf(currentChannel) + 1;
                        String ip = netRange.concat(String.valueOf(ipNumber));
                        String deviceName;
                        deviceName = "UnKnow";
                        try {
                            Address address = SingletonContext.getInstance().getNameServiceClient().getByName(ip);
                            address.firstCalledName();
                            deviceName =  address.nextCalledName(SingletonContext.getInstance());
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                        if (saveDevice != null && saveDevice.getIp().equals(ip)){
                            saveDevice.setDeviceName(deviceName);
                            deviceList.add(saveDevice);
                            isAddSaveDevice = true;
                        }else {
                            deviceList.add(new LanDeviceBean(ip, deviceName));
                        }
                        Log.d(TAG, "found share at " + ip);
                    }
                }
            }
        }

        if(saveDevice != null && !isAddSaveDevice){
            deviceList.add(saveDevice);
        }

        for(SocketChannel socketChannel: sockets) {
            try {
                socketChannel.close();
            } catch (IOException ignored) {}
        }
        try {
            if (selector != null)
                selector.close();
        } catch (IOException ignored) {

        }
        mListener.onEnd(deviceList);
    }


    public interface FindLanDevicesListener{
        void onEnd(List<LanDeviceBean> deviceList);
    }
}
