package com.yumao.jason.jxprinter;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

/*******************************************************************************
 * Copyright (C) 2017-2018 wormpex-btalk. All rights reserved
 * Creation    : Created by jiaxin on 2018/6/30.
 * Description :
 *
 ******************************************************************************/


public class TestActivity extends Activity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_activity_layout);


    }

    @Override
    protected void onResume() {
        super.onResume();

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 0.01f;
        getWindow().setAttributes(lp);
    }
}
