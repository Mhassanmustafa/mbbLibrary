package com.artifex.mupdfdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;
import android.provider.Settings;

import com.github.react.mbbLibrary.R;
import com.github.react.mbbLibrary.RCTMuPdfModule;
import com.github.react.mbbLibrary.constants.RCTEnum;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@SuppressWarnings("JniMissingFunction")
public class MuPDFCore {
	private static final String TAG = MuPDFCore.class.getSimpleName();
	/* load our native library */
	private static boolean gs_so_available = false;
	static {
		Log.e(TAG, "Loading dll");
		System.loadLibrary("mupdf_java32");
		Log.e(TAG, "Loaded dll");
	}

	/* Readable members */
	private int numPages = -1;
	private float pageWidth;
	private float pageHeight;
	private long globals;
	private byte fileBuffer[];
	private String file_format;
	private boolean isUnencryptedPDF;
	private final boolean wasOpenedFromBuffer;
	public static boolean flag = false;
	private String filePath;
	private boolean encryptFlag = true;

	/* The native functions */
	private static native boolean gprfSupportedInternal();

	private native long openFile(String filename);

	private native long openBuffer(String magic);

	private native String fileFormatInternal();

	private native boolean isUnencryptedPDFInternal();

	private native int countPagesInternal();

	private native void gotoPageInternal(int localActionPageNum);

	private native float getPageWidth();

	private native float getPageHeight();

	private native void drawPage(Bitmap bitmap, int pageW, int pageH, int patchX, int patchY, int patchW, int patchH,
			long cookiePtr, int mode);

	private native void updatePageInternal(Bitmap bitmap, int page, int pageW, int pageH, int patchX, int patchY,
			int patchW, int patchH, long cookiePtr, int mode);

	private native RectF[] searchPage(String text);

	private native TextChar[][][][] text();

	private native byte[] textAsHtml();

	private native void addMarkupAnnotationInternal(PointF[] quadPoints, int type);
	// private native void addInkAnnotationInternal(PointF[][] arcs);

	private native void addInkAnnotationInternal(PointF[][] arcs, float colorR, float colorG, float colorB,
			float inkThickness);

	private native void deleteAnnotationInternal(int annot_index);

	private native int passClickEventInternal(int page, float x, float y);

	private native void setFocusedWidgetChoiceSelectedInternal(String[] selected);

	private native String[] getFocusedWidgetChoiceSelected();

	private native String[] getFocusedWidgetChoiceOptions();

	private native int getFocusedWidgetSignatureState();

	private native String checkFocusedSignatureInternal();

	private native boolean signFocusedSignatureInternal(String keyFile, String password);

	private native int setFocusedWidgetTextInternal(String text);

	private native String getFocusedWidgetTextInternal();

	private native int getFocusedWidgetTypeInternal();

	private native LinkInfo[] getPageLinksInternal(int page);

	private native RectF[] getWidgetAreasInternal(int page);

	private native Annotation[] getAnnotationsInternal(int page);

	private native OutlineItem[] getOutlineInternal();

	private native boolean hasOutlineInternal();

	private native boolean needsPasswordInternal();

	private native boolean authenticatePasswordInternal(String password);

	private native MuPDFAlertInternal waitForAlertInternal();

	private native void replyToAlertInternal(MuPDFAlertInternal alert);

	private native void startAlertsInternal();

	private native void stopAlertsInternal();

	private native void destroying();

	private native boolean hasChangesInternal();

	private native void saveInternal();

	private native long createCookie();

	private native void destroyCookie(long cookie);

	private native void abortCookie(long cookie);

	private native String startProofInternal(int resolution);

	private native void endProofInternal(String filename);

	private native int getNumSepsOnPageInternal(int page);

	private native int controlSepOnPageInternal(int page, int sep, boolean disable);

	private native Separation getSepInternal(int page, int sep);

	public native boolean javascriptSupported();

	public class Cookie {
		private final long cookiePtr;

		public Cookie() {
			cookiePtr = createCookie();
			if (cookiePtr == 0)
				throw new OutOfMemoryError();
		}

		public void abort() {
			abortCookie(cookiePtr);
		}

		public void destroy() {
			// We could do this in finalize, but there's no guarantee that
			// a finalize will occur before the muPDF context occurs.
			destroyCookie(cookiePtr);
		}
	}

	public void getDark() {
		if (flag) {
			flag = false;
		} else {
			flag = true;
		}
	}

	public MuPDFCore(Context context, String filename) throws Exception {
		if (!encryptFlag) {
			encryptFlag = true;
		}

		filePath = filename;
		if (openFile(filename) == 0) {
			decryptWithAes(filename, getAndriodId(context).getBytes());
			globals = openFile(filename);
			if (globals == 0) {
				throw new Exception(String.format(context.getString(R.string.cannot_open_file_Path), filename));
			}
			file_format = fileFormatInternal();
			isUnencryptedPDF = isUnencryptedPDFInternal();
			wasOpenedFromBuffer = false;
		} else {
			throw new Exception(String.format(context.getString(R.string.file_not_supported), filename));
		}
	}

	public MuPDFCore(Context context, byte buffer[], String magic) throws Exception {
		fileBuffer = buffer;
		globals = openBuffer(magic != null ? magic : "");
		if (globals == 0) {
			throw new Exception(context.getString(R.string.cannot_open_buffer));
		}
		file_format = fileFormatInternal();
		isUnencryptedPDF = isUnencryptedPDFInternal();
		wasOpenedFromBuffer = true;
	}

	public int countPages() {
		if (numPages < 0)
			numPages = countPagesSynchronized();
		return numPages;
	}

	public String fileFormat() {
		return file_format;
	}

	public boolean isUnencryptedPDF() {
		return isUnencryptedPDF;
	}

	public boolean wasOpenedFromBuffer() {
		return wasOpenedFromBuffer;
	}

	private synchronized int countPagesSynchronized() {
		return countPagesInternal();
	}

	/* Shim function */
	private void gotoPage(int page) {
		if (page > numPages - 1)
			page = numPages - 1;
		else if (page < 0)
			page = 0;
		gotoPageInternal(page);
		this.pageWidth = getPageWidth();
		this.pageHeight = getPageHeight();
	}

	public synchronized PointF getPageSize(int page) {
		gotoPage(page);
		return new PointF(pageWidth, pageHeight);
	}

	public MuPDFAlert waitForAlert() {
		MuPDFAlertInternal alert = waitForAlertInternal();
		return alert != null ? alert.toAlert() : null;
	}

	public void replyToAlert(MuPDFAlert alert) {
		replyToAlertInternal(new MuPDFAlertInternal(alert));
	}

	public void stopAlerts() {
		stopAlertsInternal();
	}

	public void startAlerts() {
		startAlertsInternal();
	}

	public synchronized void onDestroy() {
		destroying();
		globals = 0;
	}

	private int mPgaeOnce = -1;

	public synchronized void drawPage(Bitmap bm, int page, int pageW, int pageH, int patchX, int patchY, int patchW,
			int patchH, MuPDFCore.Cookie cookie) {
		gotoPage(page);
		if (flag) {
			drawPage(bm, pageW, pageH, patchX, patchY, patchW, patchH, cookie.cookiePtr, 1);
		} else {
			drawPage(bm, pageW, pageH, patchX, patchY, patchW, patchH, cookie.cookiePtr, 0);
		}

		if (!RCTMuPdfModule.OpenMode.equals("Accused") && mPgaeOnce != MuPDFReaderView.mPageCore) {
			mPgaeOnce = MuPDFReaderView.mPageCore;
			RCTMuPdfModule.sendPageChangeEvent(MuPDFReaderView.mPageCore);
		}
	}

	public synchronized void updatePage(Bitmap bm, int page, int pageW, int pageH, int patchX, int patchY, int patchW,
			int patchH, MuPDFCore.Cookie cookie) {

		if (flag) {
			updatePageInternal(bm, page, pageW, pageH, patchX, patchY, patchW, patchH, cookie.cookiePtr, 1);
		} else {
			updatePageInternal(bm, page, pageW, pageH, patchX, patchY, patchW, patchH, cookie.cookiePtr, 0);
		}
	}

	public synchronized PassClickResult passClickEvent(int page, float x, float y) {
		boolean changed = passClickEventInternal(page, x, y) != 0;

		switch (WidgetType.values()[getFocusedWidgetTypeInternal()]) {
			case TEXT:
				return new PassClickResultText(changed, getFocusedWidgetTextInternal());
			case LISTBOX:
			case COMBOBOX:
				return new PassClickResultChoice(changed, getFocusedWidgetChoiceOptions(),
						getFocusedWidgetChoiceSelected());
			case SIGNATURE:
				return new PassClickResultSignature(changed, getFocusedWidgetSignatureState());
			default:
				return new PassClickResult(changed);
		}

	}

	public synchronized boolean setFocusedWidgetText(int page, String text) {
		boolean success;
		gotoPage(page);
		success = setFocusedWidgetTextInternal(text) != 0;

		return success;
	}

	public synchronized void setFocusedWidgetChoiceSelected(String[] selected) {
		setFocusedWidgetChoiceSelectedInternal(selected);
	}

	public synchronized String checkFocusedSignature() {
		return checkFocusedSignatureInternal();
	}

	public synchronized boolean signFocusedSignature(String keyFile, String password) {
		return signFocusedSignatureInternal(keyFile, password);
	}

	public synchronized LinkInfo[] getPageLinks(int page) {
		return getPageLinksInternal(page);
	}

	public synchronized RectF[] getWidgetAreas(int page) {
		return getWidgetAreasInternal(page);
	}

	public synchronized Annotation[] getAnnoations(int page) {
		return getAnnotationsInternal(page);
	}

	public synchronized RectF[] searchPage(int page, String text) {
		gotoPage(page);
		return searchPage(text);
	}

	public synchronized byte[] html(int page) {
		gotoPage(page);
		return textAsHtml();
	}

	public synchronized TextWord[][] textLines(int page) {
		gotoPage(page);
		TextChar[][][][] chars = text();

		// The text of the page held in a hierarchy (blocks, lines, spans).
		// Currently we don't need to distinguish the blocks level or
		// the spans, and we need to collect the text into words.
		ArrayList<TextWord[]> lns = new ArrayList<TextWord[]>();

		for (TextChar[][][] bl : chars) {
			if (bl == null)
				continue;
			for (TextChar[][] ln : bl) {
				ArrayList<TextWord> wds = new ArrayList<TextWord>();
				TextWord wd = new TextWord();

				for (TextChar[] sp : ln) {
					for (TextChar tc : sp) {
						if (tc.c != ' ') {
							wd.Add(tc);
						} else if (wd.w.length() > 0) {
							wds.add(wd);
							wd = new TextWord();
						}
					}
				}

				if (wd.w.length() > 0)
					wds.add(wd);

				if (wds.size() > 0)
					lns.add(wds.toArray(new TextWord[wds.size()]));
			}
		}

		return lns.toArray(new TextWord[lns.size()][]);
	}

	public synchronized void addMarkupAnnotation(int page, PointF[] quadPoints, Annotation.Type type, RCTEnum rctEnum) {
		gotoPage(page);
		addMarkupAnnotationInternal(quadPoints, type.ordinal());
		/**
		 * @ReactMethod
		 **/
		if (rctEnum == RCTEnum.RCT_ACTIVE) {
			RCTMuPdfModule.sendMarkupAnnotationEvent(page, quadPoints, type);
		}
	}

	public synchronized void addInkAnnotation(int page, PointF[][] arcs, float color[], float inkThickness,
			RCTEnum rctEnum) {
		gotoPage(page);
		addInkAnnotationInternal(arcs, color[0], color[1], color[2], inkThickness);
		/**
		 * @ReactMethod
		 **/
		if (rctEnum == RCTEnum.RCT_ACTIVE) {
			RCTMuPdfModule.sendInkAnnotationEvent(page, arcs, color, inkThickness);
		}
	}

	public synchronized void deleteAnnotation(int page, int annot_index, RCTEnum rctEnum) {
		gotoPage(page);
		deleteAnnotationInternal(annot_index);
		/**
		 * @ReactMethod
		 **/
		if (rctEnum == RCTEnum.RCT_ACTIVE) {
			RCTMuPdfModule.sendDeleteSelectedAnnotationEvent(page, annot_index);
		}
	}

	public synchronized boolean hasOutline() {
		return hasOutlineInternal();
	}

	public synchronized OutlineItem[] getOutline() {
		return getOutlineInternal();
	}

	public synchronized boolean needsPassword() {
		return needsPasswordInternal();
	}

	public synchronized boolean authenticatePassword(String password) {
		return authenticatePasswordInternal(password);
	}

	public synchronized boolean hasChanges() {
		return hasChangesInternal();
	}

	public synchronized void save() {
		saveInternal();
	}

	public synchronized String startProof(int resolution) {
		return startProofInternal(resolution);
	}

	public synchronized void endProof(String filename) {
		endProofInternal(filename);
	}

	public static boolean gprfSupported() {
		return gs_so_available != false && gprfSupportedInternal();
	}

	public boolean canProof() {
		String format = fileFormat();
		return format.contains("PDF");
	}

	public synchronized int getNumSepsOnPage(int page) {
		return getNumSepsOnPageInternal(page);
	}

	public synchronized int controlSepOnPage(int page, int sep, boolean disable) {
		return controlSepOnPageInternal(page, sep, disable);
	}

	public synchronized Separation getSep(int page, int sep) {
		return getSepInternal(page, sep);
	}

	// @SuppressLint("NewApi")
	public void encryptWithAes(String filenamePlain, byte[] key) throws Exception {
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
		GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, key);
		cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmParameterSpec);
		String orginalPath = filenamePlain.substring(0, filenamePlain.lastIndexOf("/")) + "/";
		String fileName = filenamePlain.substring(filenamePlain.lastIndexOf("/") + 1);
		String path_ = (orginalPath + "enc" + fileName);

		FileInputStream fis = new FileInputStream(filenamePlain);
		BufferedInputStream in = new BufferedInputStream(fis);
		FileOutputStream out = new FileOutputStream(path_);
		BufferedOutputStream bos = new BufferedOutputStream(out);
		byte[] ibuf = new byte[1024];
		int len;
		while ((len = in.read(ibuf)) != -1) {
			byte[] obuf = cipher.update(ibuf, 0, len);
			if (obuf != null)
				bos.write(obuf);
		}
		byte[] obuf = cipher.doFinal();
		if (obuf != null)
			bos.write(obuf);

		bos.flush();
		bos.close();
		fis.close();
		encryptFlag = true;
		File dir = new File(orginalPath);
		if (dir.exists()) {
			File from = new File(dir, "enc" + fileName);
			File to = new File(dir, fileName);
			if (from.exists())
				from.renameTo(to);
		}

	}

	// @SuppressLint("NewApi")
	public void decryptWithAes(String filenameEnc, byte[] key) throws Exception {
		String orignalPath = filenameEnc.substring(0, filenameEnc.lastIndexOf("/")) + "/";
		String fileName = filenameEnc.substring(filenameEnc.lastIndexOf("/") + 1);
		String path_ = (orignalPath + "dec" + fileName);

		FileInputStream in = new FileInputStream(filenameEnc);
		FileOutputStream out = new FileOutputStream(path_);
		byte[] ibuf = new byte[1024];
		int len;
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
		GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, key);

		cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec);
		while ((len = in.read(ibuf)) != -1) {
			byte[] obuf = cipher.update(ibuf, 0, len);
			if (obuf != null)
				out.write(obuf);
		}
		byte[] obuf = cipher.doFinal();
		if (obuf != null)
			out.write(obuf);
		out.flush();
		out.close();
		encryptFlag = false;
		File dir = new File(orignalPath);
		if (dir.exists()) {
			File from = new File(dir, "dec" + fileName);
			File to = new File(dir, fileName);
			if (from.exists())
				from.renameTo(to);
		}
	}

	public String getAndriodId(Context context) {
		String android_id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
		return android_id;
	}

	public boolean checkFileEncrypted() {
		return encryptFlag;
	}

	public String getCurrentFilePath() {
		return filePath;
	}

}
