package com.yumao.jason.jxprinter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.print.PrintAttributes;
import android.print.PrintJob;
import android.print.PrintJobId;
import android.print.PrintJobInfo;
import android.print.PrintManager;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.yumao.jason.jxprinter.adapter.MyPrintDocumentAdapter;
import com.yumao.jason.jxprinter.adapter.MyPrintImageAdapter;
import com.yumao.jason.jxprinter.view.AmountView;
import com.yumao.jason.jxprinter.view.LogView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = "MainActivity";


    public static final String SP_NAME_CANON_PRINTER_HELPER = "canon_printer_helper";
    public static final String CHECK_PRINTER_VALID_NAME = "check-printer-valid";
    // 打印份数
    public static final String EXTRA_PRINT_COPIES = "CANON_PRINT_COPIES";
    // 打印服务UI透明度 0-1
    public static final String EXTRA_PRINT_PAGE_ALPHA = "CANON_PRINT_PAGE_ALPHA";
    // 是否自动触发打印
    public static final String EXTRA_PRINT_AUTO_START = "CANON_PRINT_AUTP_START";
    private static final PrintAttributes.MediaSize L_89_127 = new PrintAttributes.MediaSize("CANON_MEDIA_SIZE_L",
            "L 89x127mm", 3503, 5000);
    private static final String TEST_PDF_PATH = "/mnt/internal_sd/tmp/test.pdf";
    private static final String TEST_IMG_PDF_PATH = "/mnt/internal_sd/tmp/Marvel.pdf";
    private static final int[] IMAGE_IDS = {R.drawable.neimaer, R.drawable.yumao1, R.drawable.yumao2, R.drawable.marvel};

    private static final int TOTAL_PAGE_COUNT = 4;
    private static final int ORIENTATION_PORTRAIT = 0;
    private static final int ORIENTATION_LANDSCAPE = 1;

    private Button mPrintDocTv;
    private Button mPrintImgTv;
    private Button mPrintMultiImgsTv;
    private Button mCheckPrinterValidTv;
    private Button mCheckDisplayBtn;
    private LogView mLogContainer;
    private Button mClearLogBtn;
    private RadioGroup mShowSysUiRg;
    private RadioGroup mAutoStartPrintRg;
    private AmountView mPrintCopiesView;
    private RadioGroup mColorModeRg;

    private PrintManager mPrintManager;
    private DisplayManager mDisplayManager;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private SharedPreferences mPrintAttrsSp;

    private int mPrintJobState;
    private String mPrintJobStateReason;
    private float mUiAlpha = 1;
    private boolean mAutoStartPrint = false;
    private String mImgFilePath;
    private String mPdfFilePath;
    private int mCopies = 1;
    private int mOrientation;
    private int mColorMode = PrintAttributes.COLOR_MODE_COLOR;
    private long mCheckPrinterStartTime = 0;
    private int mCurrentPosition = 0;
    private boolean mIsMultiPrint = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPrintDocTv = (Button) findViewById(R.id.print_doc_tv);
        mPrintImgTv = (Button) findViewById(R.id.print_img_tv);
        mCheckPrinterValidTv = (Button) findViewById(R.id.check_printer_valid);
        mLogContainer = (LogView) findViewById(R.id.log_view_container_ll);
        mClearLogBtn = (Button) findViewById(R.id.clear_log_btn);
        mShowSysUiRg = (RadioGroup) findViewById(R.id.show_sys_print_rg);
        mAutoStartPrintRg = (RadioGroup) findViewById(R.id.auto_start_print_rg);
        mPrintCopiesView = (AmountView) findViewById(R.id.print_copies_view);
        mColorModeRg = (RadioGroup) findViewById(R.id.color_mode_rg);
        mCheckDisplayBtn = (Button) findViewById(R.id.check_display);
        mPrintMultiImgsTv = (Button) findViewById(R.id.print_multi_img_tv);

        mPrintDocTv.setOnClickListener(this);
        mPrintImgTv.setOnClickListener(this);
        mCheckDisplayBtn.setOnClickListener(this);
        mCheckPrinterValidTv.setOnClickListener(this);
        mClearLogBtn.setOnClickListener(this);
        mPrintMultiImgsTv.setOnClickListener(this);
        mShowSysUiRg.setOnCheckedChangeListener(mShowSysUiRgListener);
        mAutoStartPrintRg.setOnCheckedChangeListener(mAutoPrintRgListener);
        mPrintCopiesView.setOnAmountChangeListener(new AmountView.OnAmountChangeListener() {
            @Override
            public void onAmountChange(View view, int amount) {
                Log.d(TAG, "onAmountChange() amount:" + amount);
                if (amount >= 1) {
                    mCopies = amount;
                }
            }
        });
        mColorModeRg.setOnCheckedChangeListener(mColorModeRgListener);

        mPrintAttrsSp = getSharedPreferences(SP_NAME_CANON_PRINTER_HELPER, Context.MODE_WORLD_READABLE | Context.MODE_MULTI_PROCESS);

        mDisplayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        mDisplayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int displayId) {
                Log.d(TAG, "onDisplayAdded() displayId:" + displayId);
            }

            @Override
            public void onDisplayRemoved(int displayId) {
                Log.d(TAG, "onDisplayRemoved() displayId:" + displayId);
            }

            @Override
            public void onDisplayChanged(int displayId) {
                Log.d(TAG, "onDisplayChanged() displayId:" + displayId);
            }
        }, null);
    }

    @Override
    public void onClick(View v) {
        final int viewId = v.getId();
        if (viewId == R.id.print_doc_tv) {
            Log.d(TAG, "click print doc btn");
            doPrintDoc("print-doc");
        } else if (viewId == R.id.print_img_tv) {
            Log.d(TAG, "click print img btn");
            doPrintImg();
        } else if (viewId == R.id.check_printer_valid) {
            Log.d(TAG, "check printer valid");
            checkPrinterValid();
        } else if (viewId == R.id.clear_log_btn) {
            Log.d(TAG, "clear log views");
            mLogContainer.clearLogs();
        } else if (viewId == R.id.check_display) {
            Log.d(TAG, "click check display");
            checkDisplay();
        } else if (viewId == R.id.print_multi_img_tv) {
            Log.d(TAG, "click print multi imgs btn");
            doPrintMultiImgs();
        }
    }

    private void doPrintMultiImgs() {
        mIsMultiPrint = true;
        mCurrentPosition = 0;
        doPrintImg();
    }

    private void checkDisplay() {
        Display[] displays = mDisplayManager.getDisplays();
        for (Display display : displays) {
            Log.d(TAG, display.toString());
            int displayId = display.getDisplayId();

        }
    }

    private void checkPrinterValid() {
        mCheckPrinterStartTime = System.currentTimeMillis();
        doPrintDoc(CHECK_PRINTER_VALID_NAME);
    }

    private void doPrintDoc(String printJobName) {
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
                .setColorMode(mColorMode)
//                .setResolution(new PrintAttributes.Resolution("600*1200", "600*1200", 600, 1200))
                .build();

        MyPrintDocumentAdapter printDocumentAdapter = new MyPrintDocumentAdapter(MainActivity.this);
        printDocumentAdapter.setPdfFilePath(mPdfFilePath);
        printDocumentAdapter.setTotalPages(TOTAL_PAGE_COUNT);

        PrintJob printJob = mPrintManager.print(printJobName, printDocumentAdapter, attr);
        checkPrintJob(printJob);
    }

    private void checkPrintJob(PrintJob printJob) {
        if (printJob == null) {
            Log.e(TAG, "printJob is null!!!");
        }
        String printJobId = printJob.getId().toString();
        PrintJobInfo printJobInfo = printJob.getInfo();
        Log.d(TAG, "printJobInfo:" + printJobInfo.toString());
        mPrintJobState = printJobInfo.getState();
        mPrintJobStateReason = parseTag(printJobInfo.toString());
        Log.d(TAG, "print state:" + mPrintJobState);
        if (mPrintJobState == PrintJobInfo.STATE_CREATED) {
            Log.d(TAG, "print job " + printJobId + " is created");
            mLogContainer.addLog("state:" + mPrintJobState + " 打印任务已创建 jobId:" + printJobId);
            postRequestDelay(printJob);
        } else if (mPrintJobState == PrintJobInfo.STATE_BLOCKED) {
            Log.d(TAG, "print job " + printJobId + " is blocked");
            mLogContainer.addLog("state:" + mPrintJobState + " 打印任务阻塞");
            postRequestDelay(printJob);
//            Toast toast = Toast.makeText(MainActivity.this, "打印任务阻塞,已取消", Toast.LENGTH_LONG);
//            toast.show();
//            cancelPrintJob(printJob.getId());
        } else if (mPrintJobState == PrintJobInfo.STATE_CANCELED) {
            Log.d(TAG, "print job " + printJobId + " is cancelled, printJobInfo.getLabel():" + printJobInfo.getLabel());
            mLogContainer.addLog("state:" + mPrintJobState + " 打印任务已取消");
            if (CHECK_PRINTER_VALID_NAME.equals(printJobInfo.getLabel())) {
                Log.d(TAG, "CHECK_PRINTER_VALID_NAME cancel");
                String logStr = "";
                if (System.currentTimeMillis() - mCheckPrinterStartTime > 10000) {
                    logStr = "检查任务-检查到打印机不可用";
                } else {
                    logStr = "检查任务-检查到打印机可以使用";
                }
                mLogContainer.addLog(logStr);
                Toast toast = Toast.makeText(MainActivity.this, logStr, Toast.LENGTH_SHORT);
                toast.show();
            } else {
                mLogContainer.addLog("state:" + mPrintJobState + " 打印任务已取消,若不是手动返回,请检查打印机是否正常!");
                Toast toast = Toast.makeText(MainActivity.this, "打印任务已取消", Toast.LENGTH_SHORT);
                toast.show();
            }
        } else if (mPrintJobState == PrintJobInfo.STATE_COMPLETED) {
            Log.d(TAG, "print job " + printJobId + " is completed. mIsMultiPrint:" + mIsMultiPrint +
            ", mCurrentPosition:" + mCurrentPosition);
            mLogContainer.addLog("state:" + mPrintJobState + " 打印任务已完成. mIsMultiPrint:" +
                    mIsMultiPrint + ", mCurrentPosition:" + mCurrentPosition);
            if (mIsMultiPrint) {
                if (mCurrentPosition < IMAGE_IDS.length - 1) {
                    mCurrentPosition++;
                    doPrintImg();
                } else {
                    mCurrentPosition = 0;
                    mIsMultiPrint = false;
                }
            }
        } else if (mPrintJobState == PrintJobInfo.STATE_FAILED) {
            Log.d(TAG, "print job " + printJobId + " is failed");
            mLogContainer.addLog("state:" + mPrintJobState + " 打印任务失败");
        } else if (mPrintJobState == PrintJobInfo.STATE_QUEUED) {
            Log.d(TAG, "print job " + printJobId + " is queued");
            mLogContainer.addLog("state:" + mPrintJobState + " 打印任务已入队列");
            postRequestDelay(printJob);
        } else if (mPrintJobState == PrintJobInfo.STATE_STARTED) {
            Log.d(TAG, "print job " + printJobId + " is started");
            mLogContainer.addLog("state:" + mPrintJobState + " 打印任务已开始");
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


        if (mCurrentPosition > IMAGE_IDS.length - 1) {
            mCurrentPosition = 0;
        }

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), IMAGE_IDS[mCurrentPosition]);
        if (bitmap == null) {
            return;
        }

        Log.d(TAG, "bitmap.width:" + bitmap.getWidth());
        Log.d(TAG, "bitmap.height:" + bitmap.getHeight());

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
                .setColorMode(mColorMode)
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
        mOrientation = ORIENTATION_PORTRAIT;
        SharedPreferences.Editor printAttrEditor = mPrintAttrsSp.edit();
        // 打印份数
        printAttrEditor.putString(EXTRA_PRINT_COPIES, String.valueOf(mCopies));
        printAttrEditor.putFloat(EXTRA_PRINT_PAGE_ALPHA, mUiAlpha);
        printAttrEditor.putBoolean(EXTRA_PRINT_AUTO_START, mAutoStartPrint);
        printAttrEditor.commit();
    }

    private RadioGroup.OnCheckedChangeListener mShowSysUiRgListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            Log.d(TAG, "mShowSysUiRgListener onCheckedChanged() checkedId:" + checkedId);
            if (checkedId == R.id.show_sys_print_ui_rb) {
                mUiAlpha = 1;
            } else {
                mUiAlpha = 0;
            }
        }
    };

    private RadioGroup.OnCheckedChangeListener mAutoPrintRgListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            Log.d(TAG, "mAutoPrintRgListener onCheckedChanged() checkedId:" + checkedId);
            if (checkedId == R.id.auto_start_print_rb) {
                mAutoStartPrint = true;
            } else {
                mAutoStartPrint = false;
            }
        }
    };

    private RadioGroup.OnCheckedChangeListener mColorModeRgListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            Log.d(TAG, "mColorModeRgListener onCheckedChanged() checkedId:" + checkedId);
            if (checkedId == R.id.color_mode_monochrome) {
                mColorMode = PrintAttributes.COLOR_MODE_MONOCHROME;
            } else {
                mColorMode = PrintAttributes.COLOR_MODE_COLOR;
            }
        }
    };
}
