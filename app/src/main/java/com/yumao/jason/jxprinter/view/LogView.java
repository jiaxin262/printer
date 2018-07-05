package com.yumao.jason.jxprinter.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.yumao.jason.jxprinter.R;

import java.util.Date;

public class LogView extends ScrollView{
    public static final String TAG = "LogView";

    private Context mContext;
    private LinearLayout mLogViewRootLl;

    public LogView(Context context) {
        super(context);
        initView(context, null);
    }

    public LogView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        Log.d(TAG, "initView()");
        this.mContext = context;
        LayoutInflater.from(context).inflate(R.layout.log_view_layout, this);
        mLogViewRootLl = (LinearLayout) findViewById(R.id.log_view_root_ll);
    }

    public void addLog(String log) {
        Log.d(TAG, "addLog() log:" + log);
        Date date = new Date();
        TextView tv = new TextView(mContext);
        tv.setText(log);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setPadding(5, 5, 5, 5);
        mLogViewRootLl.addView(tv);
    }
}
