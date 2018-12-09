package com.tanjinc.autotool;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


public class Test extends Activity{
    ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
    private void test() {
        startActivity(new Intent(this,WorkService.class));
        Intent intent = new Intent();
        intent.setClassName("com.jifen.qukan", "com.jifen.qkbase.main.MainActivity");


    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    };
}

