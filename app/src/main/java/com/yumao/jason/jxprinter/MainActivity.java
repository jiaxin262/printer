package com.yumao.jason.jxprinter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.print.PrintAttributes;
import android.print.PrintJob;
import android.print.PrintJobId;
import android.print.PrintJobInfo;
import android.print.PrintManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = "MainActivity";

    public static final String SP_NAME_CANON_PRINTER_HELPER = "canon_printer_helper";
    // pdf文件路径
    public static final String EXTRA_PRINT_PDF_FILE_PATH =
            "CANON_PRINT_PDF_FILE_PATH";
    // 打印份数
    public static final String EXTRA_PRINT_COPIES =
            "CANON_PRINT_COPIES";

    // 打印服务UI透明度 0-1
    public static final String EXTRA_PRINT_PAGE_ALPHA =
            "CANON_PRINT_PAGE_ALPHA";
    // 是否自动触发打印
    public static final String EXTRA_PRINT_AUTO_START =
            "CANON_PRINT_AUTP_START";

    private static final int ORIENTATION_PORTRAIT = 0;
    private static final int ORIENTATION_LANDSCAPE = 1;

    private static final PrintAttributes.MediaSize L_89_127 = new PrintAttributes.MediaSize("CANON_MEDIA_SIZE_L",
            "L 89x127mm", 3503, 5000);

    private static final String TEST_PDF_PATH = "/mnt/internal_sd/tmp/test.pdf";
    private static final String TEST_IMG_PDF_PATH = "/mnt/internal_sd/tmp/Marvel.pdf";
    private static final int TOTAL_PAGE_COUNT = 4;

    private TextView mPrintDocTv;
    private TextView mPrintImgTv;
    private LinearLayout mPrintLogLl;

    private TextView mOpenPageTv;
    private TextView mGetExternalDir;

    private PrintManager mPrintManager;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private int mPrintJobState;
    private String mPrintJobStateReason;

    private float mUiAlpha = 1;
    private boolean mAutoStartPrint = false;

    private String mImgFilePath;
    private String mPdfFilePath;
    private int mCopies = 1;
    private int mOrientation; // 0是纵向,1是横向

    private SharedPreferences mPrintAttrsSp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPrintDocTv = (TextView) findViewById(R.id.print_doc_tv);
        mPrintImgTv = (TextView) findViewById(R.id.print_img_tv);
        mPrintLogLl = (LinearLayout) findViewById(R.id.print_log_ll);
        mOpenPageTv = (TextView) findViewById(R.id.open_new_activity);
        mGetExternalDir = (TextView) findViewById(R.id.get_external_dir);

        mOpenPageTv.setVisibility(View.GONE);
        mGetExternalDir.setVisibility(View.GONE);

        mPrintDocTv.setOnClickListener(this);
        mPrintImgTv.setOnClickListener(this);
        mOpenPageTv.setOnClickListener(this);
        mGetExternalDir.setOnClickListener(this);

        mPrintAttrsSp = getSharedPreferences(SP_NAME_CANON_PRINTER_HELPER, Context.MODE_WORLD_READABLE | Context.MODE_MULTI_PROCESS);

    }

    @Override
    public void onClick(View v) {
        final int viewId = v.getId();
        if (viewId == R.id.print_doc_tv) {
            Log.d(TAG, "click print doc btn");
            doPrintDoc();
        } else if (viewId == R.id.print_img_tv) {
            Log.d(TAG, "click print img btn");
            doPrintImg();
        } else if (viewId == R.id.open_new_activity) {
            Log.d(TAG, "open new page");
            openNewPage();
        } else if (viewId == R.id.get_external_dir) {
            String dirStr = getExternalDir();
            Log.d(TAG, "get external dir:" + dirStr);
            TextView tv = new TextView(MainActivity.this);
            tv.setText(dirStr);
            mPrintLogLl.addView(tv);
        }
    }

    private void openNewPage() {
        Intent intent = new Intent(MainActivity.this, TestActivity.class);
        startActivity(intent);
    }

    private void doPrintDoc() {
        Log.d(TAG, "doPrintDoc()");
        if (mPrintManager == null) {
            mPrintManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        }

        generateSpContent();

        PrintAttributes.MediaSize mediaSize = PrintAttributes.MediaSize.ISO_A4;
        if (mOrientation == 1) {
            mediaSize = mediaSize.asLandscape();
        } else {
            mediaSize = mediaSize.asPortrait();
        }
        PrintAttributes attr = new PrintAttributes.Builder()
                .setMediaSize(mediaSize)
                .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
//                .setResolution(new PrintAttributes.Resolution("600*1200", "600*1200", 600, 1200))
                .build();

        MyPrintDocumentAdapter printDocumentAdapter = new MyPrintDocumentAdapter(MainActivity.this);
        printDocumentAdapter.setPdfFilePath(mPdfFilePath);
        printDocumentAdapter.setTotalPages(TOTAL_PAGE_COUNT);

        PrintJob printJob = mPrintManager.print("test-print-doc", printDocumentAdapter, attr);
        checkPrintJob(printJob);
    }

    private void checkPrintJob(PrintJob printJob) {
        if (printJob == null) {
            Log.e(TAG, "printJob is null!!!");
        }
        PrintJobId printJobId = printJob.getId();
        PrintJobInfo printJobInfo = printJob.getInfo();
        Log.d(TAG, "printJobInfo:" + printJobInfo.toString());
        mPrintJobState = printJobInfo.getState();
        mPrintJobStateReason = parseTag(printJobInfo.toString());
        Log.d(TAG, "print state:" + mPrintJobState);
        if (mPrintJobState == PrintJobInfo.STATE_CREATED) {
            Log.d(TAG, "print job " + printJobId.toString() + " is created");
            postRequestDelay(printJob);
        } else if (mPrintJobState == PrintJobInfo.STATE_BLOCKED) {
            Log.d(TAG, "print job " + printJobId.toString() + " is blocked");
            Toast toast = Toast.makeText(MainActivity.this, "打印任务阻塞,已取消", Toast.LENGTH_LONG);
            toast.show();
            cancelPrintJob(printJobId);
        } else if (mPrintJobState == PrintJobInfo.STATE_CANCELED) {
            Log.d(TAG, "print job " + printJobId.toString() + " is cancelled");

        } else if (mPrintJobState == PrintJobInfo.STATE_COMPLETED) {
            Log.d(TAG, "print job " + printJobId.toString() + " is completed");

        } else if (mPrintJobState == PrintJobInfo.STATE_FAILED) {
            Log.d(TAG, "print job " + printJobId.toString() + " is failed");

        } else if (mPrintJobState == PrintJobInfo.STATE_QUEUED) {
            Log.d(TAG, "print job " + printJobId.toString() + " is queued");
            postRequestDelay(printJob);
        } else if (mPrintJobState == PrintJobInfo.STATE_STARTED) {
            Log.d(TAG, "print job " + printJobId.toString() + " is started");
            postRequestDelay(printJob);
        }
    }

    private void cancelPrintJob(PrintJobId printJobId) {
        Log.d(TAG, "cancelPrintJob() printJobId:" + printJobId);
        List<PrintJob> printJobs = mPrintManager.getPrintJobs();
        if (printJobs.size() > 0) {
            for (PrintJob job : printJobs) {
                if (job != null && printJobId.equals(job.getId()) && job.isBlocked()) {
                    Log.d(TAG, "printJob:" + printJobId.toString() + " has been canceled!!!");
                    job.cancel();
                    return;
                }
            }
        }
    }

    private String parseTag(String printJobInfoStr) {
        String result = "";
        int start = printJobInfoStr.indexOf(" tag: ");
        int end = -1;
        if (start > 0 && start < printJobInfoStr.length()) {
            end = printJobInfoStr.indexOf(", ", start);
        }
        if (end > start && end < printJobInfoStr.length()) {
            result = printJobInfoStr.substring(start + 6, end);
        }
        Log.d(TAG, "parseTag() result:" + result);
        return result;
    }

    private void postRequestDelay(final PrintJob printJob) {
        Log.d(TAG, "postRequestDelay()");
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                List<PrintJob> printJobs = mPrintManager.getPrintJobs();
                if (printJobs.size() > 0) {
                    for (PrintJob job : printJobs) {
                        if (job != null && job.getId().equals(printJob.getId())) {
                            checkPrintJob(job);
                        }
                    }
                }
            }
        }, 1000);
    }

    private void doPrintImg() {
        Log.d(TAG, "doPrintImg()");

        generateSpContent();



        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.neimaer
        );
        if (bitmap == null) {
            return;
        }
        if (mPrintManager == null) {
            mPrintManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        }

        PrintAttributes.MediaSize mediaSize = L_89_127;
        if (bitmap.getWidth() > bitmap.getHeight()) {
            mediaSize = mediaSize.asLandscape();
        } else {
            mediaSize = mediaSize.asPortrait();
        }
        PrintAttributes attr = new PrintAttributes.Builder()
                .setMediaSize(mediaSize)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
//                .setResolution(new PrintAttributes.Resolution("600*1200", "600*1200", 600, 1200))
                .build();

        MyPrintImageAdapter printImageAdapter = new MyPrintImageAdapter(MainActivity.this);
        printImageAdapter.setBitmap(bitmap);

        PrintJob printJob = mPrintManager.print("test-print-test1.jpg", printImageAdapter, attr);
        checkPrintJob(printJob);
    }

    private void generateSpContent() {
        mImgFilePath = TEST_IMG_PDF_PATH;
        mPdfFilePath = TEST_PDF_PATH;
        mCopies = 1;
        mOrientation = ORIENTATION_LANDSCAPE;
        SharedPreferences.Editor printAttrEditor = mPrintAttrsSp.edit();
        printAttrEditor.putString(EXTRA_PRINT_PDF_FILE_PATH, mPdfFilePath);
        // 打印份数
        printAttrEditor.putString(EXTRA_PRINT_COPIES, String.valueOf(mCopies));

        printAttrEditor.putFloat(EXTRA_PRINT_PAGE_ALPHA, mUiAlpha);
        printAttrEditor.putBoolean(EXTRA_PRINT_AUTO_START, mAutoStartPrint);
        printAttrEditor.commit();
    }

    public String getExternalDir() {
        File f1 = Environment.getExternalStorageDirectory();
        if (f1 != null && f1.exists()) {
            return f1.getPath();
        }
        File f= MainActivity.this.getExternalFilesDir(null);
        if (f != null) {
            return f.getPath();
        }
        return "";
    }
}
