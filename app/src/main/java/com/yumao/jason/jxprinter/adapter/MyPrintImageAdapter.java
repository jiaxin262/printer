package com.yumao.jason.jxprinter.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.pdf.PrintedPdfDocument;
import android.support.v4.print.PrintHelper;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;

public class MyPrintImageAdapter extends PrintDocumentAdapter {
    public static final String TAG = "MyPrintImageAdapter";

    private Context mContext;
    private PrintAttributes mAttributes;
    private Bitmap bitmap;

    private int pageHeight;
    private int pageWidth;
    private PdfDocument mPdfDocument;

    public MyPrintImageAdapter(Context context) {
        this.mContext = context;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    @Override
    public void onLayout(PrintAttributes oldPrintAttributes,
                         PrintAttributes newPrintAttributes,
                         CancellationSignal cancellationSignal,
                         LayoutResultCallback layoutResultCallback,
                         Bundle bundle) {

        mAttributes = newPrintAttributes;
        mPdfDocument = new PrintedPdfDocument(mContext, newPrintAttributes);

        pageHeight = newPrintAttributes.getMediaSize().getHeightMils() / 1000 * 72;
        pageWidth = newPrintAttributes.getMediaSize().getWidthMils() / 1000 * 72;

        PrintDocumentInfo info = new PrintDocumentInfo.Builder("test-print-test1.jpg")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_PHOTO)
                .setPageCount(1)
                .build();
        boolean changed = !newPrintAttributes.equals(oldPrintAttributes);
        layoutResultCallback.onLayoutFinished(info, changed);
    }

    @Override
    public void onWrite(PageRange[] pageRanges, ParcelFileDescriptor fileDescriptor,
                        CancellationSignal cancellationSignal,
                        WriteResultCallback writeResultCallback) {

//        PdfDocument.PageInfo newPage = new PdfDocument.PageInfo
//                .Builder(pageWidth, pageHeight, 1)
//                .create();//创建对应的Page
//
//        PdfDocument.Page page = mPdfDocument.startPage(newPage);  //创建新页面
//
//        if (cancellationSignal.isCanceled()) {  //取消信号
//            writeResultCallback.onWriteCancelled();
//            mPdfDocument.close();
//            mPdfDocument = null;
//            return;
//        }
//        RectF content = new RectF(page.getInfo().getContentRect());
//
//        Matrix matrix = getMatrix(bitmap.getWidth(), bitmap.getHeight(),
//                content, PrintHelper.SCALE_MODE_FILL);
//
//        // Draw the bitmap.
//        page.getCanvas().drawBitmap(bitmap, matrix, null);
//        mPdfDocument.finishPage(page);
//        try {
//            // Write the document.
//            mPdfDocument.writeTo(new FileOutputStream(
//                    fileDescriptor.getFileDescriptor()));
//            // Done.
//            writeResultCallback.onWriteFinished(
//                    new PageRange[]{PageRange.ALL_PAGES});
//        } catch (IOException ioe) {
//            // Failed.
//            Log.e(TAG, "Error writing printed content", ioe);
//            writeResultCallback.onWriteFailed(null);
//        } finally {
//            if (mPdfDocument != null) {
//                mPdfDocument.close();
//            }
//            if (fileDescriptor != null) {
//                try {
//                    fileDescriptor.close();
//                } catch (IOException ioe) {
//                    /* ignore */
//                }
//            }
//        }


        PrintedPdfDocument pdfDocument = new PrintedPdfDocument(mContext, mAttributes);
        try {
            PdfDocument.Page page = pdfDocument.startPage(1);

            RectF content = new RectF(page.getInfo().getContentRect());

            Matrix matrix = getMatrix(bitmap.getWidth(), bitmap.getHeight(),
                    content, PrintHelper.SCALE_MODE_FIT);

            // Draw the bitmap.
            page.getCanvas().drawBitmap(bitmap, matrix, null);

            // Finish the page.
            pdfDocument.finishPage(page);

            try {
                // Write the document.
                pdfDocument.writeTo(new FileOutputStream(
                        fileDescriptor.getFileDescriptor()));
                // Done.
                writeResultCallback.onWriteFinished(
                        new PageRange[]{PageRange.ALL_PAGES});
            } catch (IOException ioe) {
                // Failed.
                Log.e(TAG, "Error writing printed content", ioe);
                writeResultCallback.onWriteFailed(null);
            }
        } finally {
            if (pdfDocument != null) {
                pdfDocument.close();
            }
            if (fileDescriptor != null) {
                try {
                    fileDescriptor.close();
                } catch (IOException ioe) {
                }
            }
        }
    }

    @Override
    public void onFinish() {
        Log.d(TAG, "Print onFinish()");
    }

    /**
     * Calculates the transform the print an Image to fill the page
     *
     * @param imageWidth  with of bitmap
     * @param imageHeight height of bitmap
     * @param content     The output page dimensions
     * @param fittingMode The mode of fitting
     * @return Matrix to be used in canvas.drawBitmap(bitmap, matrix, null) call
     */
    private Matrix getMatrix(int imageWidth, int imageHeight, RectF content, int fittingMode) {
        Matrix matrix = new Matrix();

        // Compute and apply scale to fill the page.
        float scale = content.width() / imageWidth;
        if (fittingMode == PrintHelper.SCALE_MODE_FILL) {
            scale = Math.max(scale, content.height() / imageHeight);
        } else {
            scale = Math.min(scale, content.height() / imageHeight);
        }
        matrix.postScale(scale, scale);

        // Center the content.
        final float translateX = (content.width()
                - imageWidth * scale) / 2;
        final float translateY = (content.height()
                - imageHeight * scale) / 2;
        matrix.postTranslate(translateX, translateY);
        return matrix;
    }
}
