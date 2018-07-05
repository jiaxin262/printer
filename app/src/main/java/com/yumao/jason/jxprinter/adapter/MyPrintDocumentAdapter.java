package com.yumao.jason.jxprinter.adapter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.pdf.PrintedPdfDocument;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/*******************************************************************************
 * Copyright (C) 2017-2018 wormpex-btalk. All rights reserved
 * Creation    : Created by jiaxin on 2018/6/25.
 * Description :
 *
 ******************************************************************************/


public class MyPrintDocumentAdapter extends PrintDocumentAdapter {
    public static final String TAG = "MyPrintDocumentAdapter";

    private Context mContext;
    private PdfDocument mPdfDocument;
    private int mTotalPages = 0;
    private int pageHeight;
    private int pageWidth;
    private String mPdfFilePath;

    private File mSourcePdfFile;

    public MyPrintDocumentAdapter(Context context) {
        this.mContext = context;
    }

    public void setPdfFilePath(String pdfFilePath) {
        this.mPdfFilePath = pdfFilePath;
        if (!TextUtils.isEmpty(mPdfFilePath)) {
            mSourcePdfFile = new File(mPdfFilePath);
            if (!mSourcePdfFile.exists()) {
                Log.d(TAG, "mSourcePdfFile 不存在：" + mSourcePdfFile);
            }
        }
    }

    public void setTotalPages(int pagesCount) {
        this.mTotalPages = pagesCount;
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart()");

    }

    @Override
    public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                         CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle extras) {
        Log.d(TAG, "onLayout()");
        mPdfDocument = new PrintedPdfDocument(mContext, newAttributes);

        if (cancellationSignal.isCanceled()) {
            Log.d(TAG, "打印任务取消");
            callback.onLayoutCancelled();
            return;
        }

        pageHeight = newAttributes.getMediaSize().getHeightMils() / 1000 * 72;
        pageWidth = newAttributes.getMediaSize().getWidthMils() / 1000 * 72;

        Log.d(TAG, "页数：" + mTotalPages);
        if (mTotalPages > 0) {
            PrintDocumentInfo info = new PrintDocumentInfo
                    .Builder("print_output.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(mTotalPages)
                    .build();
            callback.onLayoutFinished(info, true);
        } else {
            callback.onLayoutFailed("打印失败-页数计算失败");
        }
    }

    @Override
    public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                        CancellationSignal cancellationSignal, WriteResultCallback callback) {
        Log.d(TAG, "onWrite()");
//        for (int i = 0; i < mTotalPages; i++) {
//            if (pageInRange(pages, i)) //保证页码正确
//            {
//                PdfDocument.PageInfo newPage = new PdfDocument.PageInfo
//                        .Builder(pageWidth, pageHeight, mTotalPages)
//                        .create();//创建对应的Page
//
//                PdfDocument.Page page = mPdfDocument.startPage(newPage);  //创建新页面
//
//                if (cancellationSignal.isCanceled()) {  //取消信号
//                    callback.onWriteCancelled();
//                    mPdfDocument.close();
//                    mPdfDocument = null;
//                    return;
//                }
//                drawPage(page, i);  //将内容绘制到页面Canvas上
//                mPdfDocument.finishPage(page);
//            }
//        }

        if (cancellationSignal.isCanceled()) {  //取消信号
            callback.onWriteCancelled();
            return;
        }

        if (!mSourcePdfFile.exists()) {
            Log.e(TAG, "要打印的文件不存在!");
            return;
        }

        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(mSourcePdfFile);
            out = new FileOutputStream(destination.getFileDescriptor());
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();
        } catch (FileNotFoundException e) {
            return;
        } catch (IOException e) {
            callback.onWriteFailed(e.toString());
            return;
        } finally {
//            mPdfDocument.close();
            mPdfDocument = null;
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        callback.onWriteFinished(pages);
    }

    @Override
    public void onFinish() {
        Log.d(TAG, "onFinish()");
    }


    //页面绘制
    private void drawPage(PdfDocument.Page page,
                          int pagenumber) {
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint();

        PdfDocument.PageInfo pageInfo = page.getInfo();

        paint.setColor(Color.BLUE);
        paint.setTextSize(19);
        canvas.drawText("A", 54, 72, paint);

        paint.setTextSize(11);
        canvas.drawText("B", 54, 72 + 25, paint);

    }


}
