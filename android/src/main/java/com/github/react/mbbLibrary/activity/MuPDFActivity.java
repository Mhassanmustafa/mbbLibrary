package com.github.react.mbbLibrary.activity;

import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.WindowManager;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.artifex.mupdfdemo.Annotation;
import com.artifex.mupdfdemo.Hit;
import com.artifex.mupdfdemo.MuPDFAlert;
import com.artifex.mupdfdemo.MuPDFCore;
import com.artifex.mupdfdemo.MuPDFPageAdapter;
import com.artifex.mupdfdemo.MuPDFReaderView;
import com.artifex.mupdfdemo.MuPDFReaderViewListener;
import com.artifex.mupdfdemo.MuPDFView;
import com.artifex.mupdfdemo.OutlineActivityData;
import com.artifex.mupdfdemo.OutlineItem;
import com.artifex.mupdfdemo.ReaderView;
import com.artifex.mupdfdemo.SearchTask;
import com.artifex.mupdfdemo.SearchTaskResult;
import com.github.react.mbbLibrary.RCTMuPdfModule;
import com.github.react.mbbLibrary.MyListener;
import com.github.react.mbbLibrary.util.SharedPreferencesUtil;
import com.github.react.mbbLibrary.R;

import java.util.concurrent.Executor;

import com.facebook.react.ReactActivity;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

public class MuPDFActivity extends ReactActivity implements Thread.UncaughtExceptionHandler {
    private static final String TAG = MuPDFActivity.class.getSimpleName();

    private final int OUTLINE_REQUEST = 0;
    private String filePath = Environment.getExternalStorageDirectory() + "/Download/pdf_t2.pdf";

    private int mRCTPage;

    private AlertDialog.Builder mAlertBuilder;

    private MuPDFCore muPDFCore;
    private MuPDFReaderView muPDFReaderView;

    private boolean mAlertsActive = false;
    private AsyncTask<Void, Void, MuPDFAlert> mAlertTask;
    private AlertDialog mAlertDialog;
    // tools
    private ViewAnimator mTopBarSwitcher;
    private ImageButton mLinkButton;
    private ImageButton mOutlineButton;
    private ImageButton mSearchButton;
    private ImageButton mAnnotButton;
    // tools
    private EditText et_searchText;
    private ImageButton mSearchBack;
    private ImageButton mSearchFwd;
    // tools
    private TextView mAnnotTypeText;

    private TextView mPageNumberView;
    private SeekBar mPageSlider;
    private int mPageSliderRes;
    private boolean mButtonsVisible;
    private TopBarMode mTopBarMode = TopBarMode.Main;
    private AcceptMode mAcceptMode;

    private SearchTask mSearchTask;
    private boolean mLinkHighlight = false;
    private int currentPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_mupdf);

        initView();

    }

    private void initView() {
        SharedPreferencesUtil.init(getApplication());

        muPDFReaderView = (MuPDFReaderView) findViewById(R.id.mu_pdf_mupdfreaderview);

        Intent intent = getIntent();
        filePath = intent.getStringExtra("Uri");
        mRCTPage = intent.getIntExtra("Page", 0);

        initToolsView();

        createPDF();
    }

    private void initToolsView() {

        mTopBarSwitcher = (ViewAnimator) findViewById(R.id.switcher);
        mLinkButton = (ImageButton) findViewById(R.id.linkButton);
        mAnnotButton = (ImageButton) findViewById(R.id.reflowButton);
        mOutlineButton = (ImageButton) findViewById(R.id.outlineButton);
        mSearchButton = (ImageButton) findViewById(R.id.searchButton);

        et_searchText = (EditText) findViewById(R.id.searchText);
        mSearchBack = (ImageButton) findViewById(R.id.searchBack);
        mSearchFwd = (ImageButton) findViewById(R.id.searchForward);

        mAnnotTypeText = (TextView) findViewById(R.id.annotType);

        mPageNumberView = (TextView) findViewById(R.id.pageNumber);
        mPageSlider = (SeekBar) findViewById(R.id.pageSlider);

        mTopBarSwitcher.setVisibility(View.INVISIBLE);
        mPageNumberView.setVisibility(View.INVISIBLE);
        mPageSlider.setVisibility(View.INVISIBLE);
    }

    private void createPDF() {
        mAlertBuilder = new AlertDialog.Builder(this);

        muPDFCore = openFile(filePath);

        SearchTaskResult.set(null);

        if (muPDFCore == null) {
            AlertDialog alert = mAlertBuilder.create();
            alert.setTitle(R.string.file_not_supported);
            alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dismiss),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
            alert.setOnCancelListener(new DialogInterface.OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            });
            alert.show();
            return;
        }

        muPDFReaderView.setAdapter(new MuPDFPageAdapter(this, muPDFCore));

        muPDFReaderView.setDisplayedViewIndex(mRCTPage);

        RCTMuPdfModule.setUpListener(new MyListener() {
            @Override
            public void onEvent(String str) {
                try {
                    MuPDFView pageView = (MuPDFView) muPDFReaderView.getDisplayedView();
                    JsonParser jsonParser = new JsonParser();
                    JsonObject jsonObject = (JsonObject) jsonParser.parse(str);

                    switch (jsonObject.get("type").getAsString()) {

                        case "update_page":
                            if (pageView != null && jsonObject.get("page").getAsInt() >= 0
                                    && jsonObject.get("page").getAsInt() != pageView.getPage()) {
                                final int page = jsonObject.get("page").getAsInt();

                                runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {

                                        muPDFReaderView.setDisplayedViewIndex(page);

                                    }
                                });
                            }
                            break;

                        case "add_annotation":
                            JsonArray jsonArray = jsonObject.get("path").getAsJsonArray();
                            PointF[][] p = new PointF[jsonArray.size()][];
                            for (int i = 0; i < jsonArray.size(); i++) {
                                JsonArray two = jsonArray.get(i).getAsJsonArray();
                                PointF[] points = new PointF[two.size()];
                                for (int j = 0; j < two.size(); j++) {
                                    points[j] = new PointF(two.get(j).getAsJsonArray().get(0).getAsFloat(),
                                            two.get(j).getAsJsonArray().get(1).getAsFloat());
                                }
                                p[i] = points;
                            }

                            if (pageView != null) {
                                pageView.saveDraw(jsonObject.get("page").getAsInt(), p,
                                        SharedPreferencesUtil.hextoRGB("#FF0000"), 4);
                            }
                            break;

                        case "add_markup_annotation":
                            JsonArray jsonArray2 = jsonObject.get("path").getAsJsonArray();
                            PointF[] p2 = new PointF[jsonArray2.size()];
                            for (int i = 0; i < jsonArray2.size(); i++) {
                                p2[i] = new PointF(jsonArray2.get(i).getAsJsonArray().get(0).getAsFloat(),
                                        jsonArray2.get(i).getAsJsonArray().get(1).getAsFloat());
                            }
                            if (pageView != null) {
                                switch (jsonObject.get("annotation_type").getAsString()) {
                                    case "UNDERLINE":
                                        pageView.markupSelection(jsonObject.get("page").getAsInt(), p2,
                                                Annotation.Type.UNDERLINE);
                                        break;
                                    case "HIGHLIGHT":
                                        pageView.markupSelection(jsonObject.get("page").getAsInt(), p2,
                                                Annotation.Type.HIGHLIGHT);
                                        break;
                                }

                            }
                            break;

                        case "delete_annotation":
                            if (pageView != null) {
                                pageView.deleteSelectedAnnotation(jsonObject.get("page").getAsInt(),
                                        jsonObject.get("annot_index").getAsInt());
                            }
                            break;
                    }

                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }

            }
        });

        if (!RCTMuPdfModule.OpenMode.equals("Accused")) {
            // Set up the page slider
            int smax = Math.max(muPDFCore.countPages() - 1, 1);
            mPageSliderRes = ((10 + smax - 1) / smax) * 2;

            mSearchTask = new SearchTask(this, muPDFCore) {
                @Override
                protected void onTextFound(SearchTaskResult result) {
                    SearchTaskResult.set(result);
                    // Ask the ReaderView to move to the resulting page
                    muPDFReaderView.setDisplayedViewIndex(result.pageNumber);
                    // Make the ReaderView act on the change to SearchTaskResult
                    // via overridden onChildSetup method.
                    muPDFReaderView.resetupChildren();
                }
            };

            // Search invoking buttons are disabled while there is no text specified
            mSearchBack.setEnabled(false);
            mSearchFwd.setEnabled(false);
            mSearchBack.setColorFilter(Color.argb(0xFF, 250, 250, 250));
            mSearchFwd.setColorFilter(Color.argb(0xFF, 250, 250, 250));

            if (muPDFCore.hasOutline()) {

                mOutlineButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        OutlineItem outline[] = muPDFCore.getOutline();
                        if (outline != null) {
                            OutlineActivityData.get().items = outline;
                            Intent intent = new Intent(MuPDFActivity.this, OutlineActivity.class);
                            startActivityForResult(intent, OUTLINE_REQUEST);
                        }
                    }
                });
            } else {
                mOutlineButton.setVisibility(View.GONE);
            }

            setListener();
        }
    }

    private MuPDFCore openFile(String path) {

        Log.e(TAG, "Trying to open " + path);
        try {
            muPDFCore = new MuPDFCore(this, path);

            OutlineActivityData.set(null);
        } catch (Exception e) {
            Log.e(TAG, "openFile catch:" + e.toString());
            return null;
        } catch (OutOfMemoryError e) {
            // out of memory is not an Exception, so we catch it separately.
            Log.e(TAG, "openFile catch: OutOfMemoryError " + e.toString());
            return null;
        }
        return muPDFCore;
    }

    private void setListener() {

        setMuPDFReaderViewListener();

        mPageSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar seekBar) {
                muPDFReaderView.setDisplayedViewIndex((seekBar.getProgress() + mPageSliderRes / 2) / mPageSliderRes);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updatePageNumView((progress + mPageSliderRes / 2) / mPageSliderRes);
            }
        });

        mSearchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                searchModeOn();
            }
        });

        if (muPDFCore.fileFormat().startsWith("PDF") && muPDFCore.isUnencryptedPDF()
                && !muPDFCore.wasOpenedFromBuffer()) {
            mAnnotButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mTopBarMode = TopBarMode.Annot;
                    mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
                }
            });
        } else {
            mAnnotButton.setVisibility(View.GONE);
        }

        et_searchText.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                boolean haveText = s.toString().length() > 0;
                setButtonEnabled(mSearchBack, haveText);
                setButtonEnabled(mSearchFwd, haveText);

                // Remove any previous search results
                if (SearchTaskResult.get() != null
                        && !et_searchText.getText().toString().equals(SearchTaskResult.get().txt)) {
                    SearchTaskResult.set(null);
                    muPDFReaderView.resetupChildren();
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        // React to Done button on keyboard
        et_searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE)
                    search(1);
                return false;
            }
        });

        et_searchText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER)
                    search(1);
                return false;
            }
        });

        // Activate search invoking buttons
        mSearchBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                search(-1);
            }
        });
        mSearchFwd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                search(1);
            }
        });

        mLinkButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // setLinkHighlight(!mLinkHighlight);
                muPDFCore.getDark();
                mRCTPage = currentPage;
                muPDFReaderView.setDisplayedViewIndex(mRCTPage);
                // SharedPreferencesUtil.init(getApplication());

                // muPDFReaderView = (MuPDFReaderView)findViewById(R.id.mu_pdf_mupdfreaderview);

                // mRCTPage = currentPage;

                // initToolsView();

                // createPDF();
            }
        });
    }

    private void setMuPDFReaderViewListener() {
        muPDFReaderView.setListener(new MuPDFReaderViewListener() {
            @Override
            public void onMoveToChild(int i) {
                if (muPDFCore == null) {
                    return;
                }
                mPageNumberView.setText(String.format("%d / %d", i + 1, muPDFCore.countPages()));
                currentPage = i;
                mPageSlider.setMax((muPDFCore.countPages() - 1) * mPageSliderRes);
                mPageSlider.setProgress(i * mPageSliderRes);
            }

            @Override
            public void onTapMainDocArea() {
                if (!mButtonsVisible) {
                    showButtons();
                } else {
                    if (mTopBarMode == TopBarMode.Main)
                        hideButtons();
                }
            }

            @Override
            public void onDocMotion() {
                hideButtons();
            }

            @Override
            public void onHit(Hit item) {
                switch (mTopBarMode) {
                    case Annot:
                        if (item == Hit.Annotation) {
                            showButtons();
                            mTopBarMode = TopBarMode.Delete;
                            mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
                        }
                        break;
                    case Delete:
                        mTopBarMode = TopBarMode.Annot;
                        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
                        // fall through
                    default:
                        // Not in annotation editing mode, but the pageview will
                        // still select and highlight hit annotations, so
                        // deselect just in case.
                        MuPDFView pageView = (MuPDFView) muPDFReaderView.getDisplayedView();
                        if (pageView != null) {
                            pageView.deselectAnnotation();
                        }
                        break;
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case OUTLINE_REQUEST:
                if (resultCode >= 0)
                    muPDFReaderView.setDisplayedViewIndex(resultCode);
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void uncaughtException(final Thread thread, final Throwable throwable) {
        new Thread() {
            @Override
            public void run() {
                RCTMuPdfModule.mPromise.reject(throwable.getMessage());

            }
        }.start();
    }

    private void showButtons() {
        if (muPDFCore == null)
            return;
        if (!mButtonsVisible) {
            mButtonsVisible = true;
            // Update page number text and slider
            int index = muPDFReaderView.getDisplayedViewIndex();
            updatePageNumView(index);
            mPageSlider.setMax((muPDFCore.countPages() - 1) * mPageSliderRes);
            mPageSlider.setProgress(index * mPageSliderRes);
            if (mTopBarMode == TopBarMode.Search) {
                et_searchText.requestFocus();
                showKeyboard();
            }

            Animation anim = new TranslateAnimation(0, 0, -mTopBarSwitcher.getHeight(), 0);
            anim.setDuration(200);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                    mTopBarSwitcher.setVisibility(View.VISIBLE);
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                }
            });
            mTopBarSwitcher.startAnimation(anim);

            anim = new TranslateAnimation(0, 0, mPageSlider.getHeight(), 0);
            anim.setDuration(200);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                    mPageSlider.setVisibility(View.VISIBLE);
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    mPageNumberView.setVisibility(View.VISIBLE);
                }
            });
            mPageSlider.startAnimation(anim);
        }
    }

    private void hideButtons() {
        if (mButtonsVisible) {
            mButtonsVisible = false;
            hideKeyboard();

            Animation anim = new TranslateAnimation(0, 0, 0, -mTopBarSwitcher.getHeight());
            anim.setDuration(200);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    mTopBarSwitcher.setVisibility(View.INVISIBLE);
                }
            });
            mTopBarSwitcher.startAnimation(anim);

            anim = new TranslateAnimation(0, 0, 0, mPageSlider.getHeight());
            anim.setDuration(200);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                    mPageNumberView.setVisibility(View.INVISIBLE);
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    mPageSlider.setVisibility(View.INVISIBLE);
                }
            });
            mPageSlider.startAnimation(anim);
        }
    }

    private void updatePageNumView(int index) {
        if (muPDFCore == null)
            return;
        mPageNumberView.setText(String.format("%d / %d", index + 1, muPDFCore.countPages()));
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.showSoftInput(et_searchText, 0);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.hideSoftInputFromWindow(et_searchText.getWindowToken(), 0);
    }

    public void OnEditAnnotButtonClick(View v) {
        mTopBarMode = TopBarMode.Main;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
    }

    public void OnCopyTextButtonClick(View v) {
        mTopBarMode = TopBarMode.Accept;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        mAcceptMode = AcceptMode.CopyText;
        muPDFReaderView.setMode(MuPDFReaderView.Mode.Selecting);
        mAnnotTypeText.setText(getString(R.string.copy_text));
        showInfo(getString(R.string.select_text));
    }

    public void OnCancelSearchButtonClick(View v) {
        searchModeOff();
    }

    public void OnCancelMoreButtonClick(View v) {
        mTopBarMode = TopBarMode.Main;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
    }

    private void searchModeOn() {
        if (mTopBarMode != TopBarMode.Search) {
            mTopBarMode = TopBarMode.Search;
            // Focus on EditTextWidget
            et_searchText.requestFocus();
            showKeyboard();
            mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        }
    }

    private void searchModeOff() {
        if (mTopBarMode == TopBarMode.Search) {
            mTopBarMode = TopBarMode.Main;
            hideKeyboard();
            mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
            SearchTaskResult.set(null);
            // Make the ReaderView act on the change to mSearchTaskResult
            // via overridden onChildSetup method.
            muPDFReaderView.resetupChildren();
        }
    }

    public void OnHighlightButtonClick(View v) {
        mTopBarMode = TopBarMode.Accept;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        mAcceptMode = AcceptMode.Highlight;
        muPDFReaderView.setMode(MuPDFReaderView.Mode.Selecting);
        mAnnotTypeText.setText(R.string.pdf_tools_highlight);
        showInfo(getString(R.string.select_text));
    }

    public void OnUnderlineButtonClick(View v) {
        mTopBarMode = TopBarMode.Accept;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        mAcceptMode = AcceptMode.Underline;
        muPDFReaderView.setMode(MuPDFReaderView.Mode.Selecting);
        mAnnotTypeText.setText(R.string.pdf_tools_underline);
        showInfo(getString(R.string.select_text));
    }

    public void OnStrikeOutButtonClick(View v) {
        mTopBarMode = TopBarMode.Accept;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        mAcceptMode = AcceptMode.StrikeOut;
        muPDFReaderView.setMode(MuPDFReaderView.Mode.Selecting);
        mAnnotTypeText.setText(R.string.pdf_tools_strike_out);
        showInfo(getString(R.string.select_text));
    }

    public void OnInkButtonClick(View v) {
        mTopBarMode = TopBarMode.Accept;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        mAcceptMode = AcceptMode.Ink;
        muPDFReaderView.setMode(MuPDFReaderView.Mode.Drawing);
        mAnnotTypeText.setText(R.string.pdf_tools_ink);
        showInfo(getString(R.string.pdf_tools_draw_annotation));
    }

    private void refresh() {
        mRCTPage = currentPage;
        muPDFReaderView.setDisplayedViewIndex(mRCTPage);
    }

    public void OnDeleteButtonClick(View v) {
        MuPDFView pageView = (MuPDFView) muPDFReaderView.getDisplayedView();
        if (pageView != null)
            pageView.deleteSelectedAnnotation();
        refresh();

        mTopBarMode = TopBarMode.Annot;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
    }

    public void OnCancelDeleteButtonClick(View v) {
        MuPDFView pageView = (MuPDFView) muPDFReaderView.getDisplayedView();
        if (pageView != null)
            pageView.deselectAnnotation();
        mTopBarMode = TopBarMode.Annot;
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
    }

    public void OnCancelAcceptButtonClick(View v) {
        MuPDFView pageView = (MuPDFView) muPDFReaderView.getDisplayedView();
        if (pageView != null) {
            pageView.deselectText();
            pageView.cancelDraw();
        }
        muPDFReaderView.setMode(MuPDFReaderView.Mode.Viewing);
        switch (mAcceptMode) {
            case CopyText:
                mTopBarMode = TopBarMode.Main;
                break;
            default:
                mTopBarMode = TopBarMode.Annot;
                break;
        }
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
    }

    public void OnAcceptButtonClick(View v) {
        MuPDFView pageView = (MuPDFView) muPDFReaderView.getDisplayedView();
        boolean success = false;
        switch (mAcceptMode) {
            case CopyText:
                if (pageView != null)
                    success = pageView.copySelection();
                refresh();
                mTopBarMode = TopBarMode.Main;
                showInfo(success ? getString(R.string.copied_to_clipboard) : getString(R.string.no_text_selected));
                break;
            case Highlight:
                if (pageView != null) {
                    success = pageView.markupSelection(Annotation.Type.HIGHLIGHT);
                    refresh();
                }
                mTopBarMode = TopBarMode.Annot;
                if (!success) {
                    showInfo(getString(R.string.no_text_selected));
                }
                break;
            case Underline:
                if (pageView != null)
                    success = pageView.markupSelection(Annotation.Type.UNDERLINE);
                refresh();
                mTopBarMode = TopBarMode.Annot;
                if (!success)
                    showInfo(getString(R.string.no_text_selected));
                break;

            case StrikeOut:
                if (pageView != null)
                    success = pageView.markupSelection(Annotation.Type.STRIKEOUT);
                refresh();
                mTopBarMode = TopBarMode.Annot;
                if (!success)
                    showInfo(getString(R.string.no_text_selected));
                break;

            case Ink:
                if (pageView != null)
                    success = pageView.saveDraw();
                refresh();
                mTopBarMode = TopBarMode.Annot;
                if (!success)
                    showInfo(getString(R.string.nothing_to_save));
                break;
        }
        mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        muPDFReaderView.setMode(MuPDFReaderView.Mode.Viewing);
    }

    private void setButtonEnabled(ImageButton button, boolean enabled) {
        button.setEnabled(enabled);
        button.setColorFilter(enabled ? Color.argb(0xFF, 250, 250, 250) : Color.argb(0xFF, 250, 250, 250));
    }

    private void search(int direction) {
        hideKeyboard();
        int displayPage = muPDFReaderView.getDisplayedViewIndex();
        SearchTaskResult r = SearchTaskResult.get();
        int searchPage = r != null ? r.pageNumber : -1;
        mSearchTask.go(align(et_searchText.getText().toString()), direction, displayPage, searchPage);
    }

    public boolean is_English(String value) {
        Pattern VALID_NAME_PATTERN_REGEX = Pattern.compile("[a-zA-Z_0-9]+$");

        return VALID_NAME_PATTERN_REGEX.matcher(value).find();
    }

    public String align(String str) {
        if (is_English(str)) {
            return str;
        } else {
            String original, reverse = "";

            original = str;

            int length = original.length();

            for (int i = length - 1; i >= 0; i--)
                reverse = reverse + original.charAt(i);

            return reverseName(reverse);
        }
    }

    public static String reverseName(String name) {

        name = name.trim();

        StringBuilder reversedNameBuilder = new StringBuilder();
        StringBuilder subNameBuilder = new StringBuilder();

        for (int i = 0; i < name.length(); i++) {

            char currentChar = name.charAt(i);

            if (currentChar != ' ' && currentChar != '-') {
                subNameBuilder.append(currentChar);
            } else {
                reversedNameBuilder.insert(0, currentChar + subNameBuilder.toString());
                subNameBuilder.setLength(0);
            }

        }

        return reversedNameBuilder.insert(0, subNameBuilder.toString()).toString();

    }

    // private void setLinkHighlight(boolean highlight) {
    // mLinkHighlight = highlight;
    // // LINK_COLOR tint
    // mLinkButton.setColorFilter(highlight ? Color.argb(0xFF, 255, 160, 0) :
    // Color.argb(0xFF, 255, 255, 255));
    // // Inform pages of the change.
    // muPDFReaderView.setLinksEnabled(highlight);
    // }

    private void showInfo(String message) {

        LayoutInflater inflater = getLayoutInflater();
        View toastLayout = inflater.inflate(R.layout.toast, (ViewGroup) findViewById(R.id.toast_root_view));

        TextView header = (TextView) toastLayout.findViewById(R.id.toast_message);
        header.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.FILL_HORIZONTAL | Gravity.BOTTOM, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(toastLayout);
        toast.show();
    }

    public void createAlertWaiter() {
        mAlertsActive = true;
        // All mupdf library calls are performed on asynchronous tasks to avoid stalling
        // the UI. Some calls can lead to javascript-invoked requests to display an
        // alert dialog and collect a reply from the user. The task has to be blocked
        // until the user's reply is received. This method creates an asynchronous task,
        // the purpose of which is to wait of these requests and produce the dialog
        // in response, while leaving the core blocked. When the dialog receives the
        // user's response, it is sent to the core via replyToAlert, unblocking it.
        // Another alert-waiting task is then created to pick up the next alert.
        if (mAlertTask != null) {
            mAlertTask.cancel(true);
            mAlertTask = null;
        }
        if (mAlertDialog != null) {
            mAlertDialog.cancel();
            mAlertDialog = null;
        }
        mAlertTask = new AsyncTask<Void, Void, MuPDFAlert>() {

            @Override
            protected MuPDFAlert doInBackground(Void... arg0) {
                if (!mAlertsActive)
                    return null;

                return muPDFCore.waitForAlert();
            }

            @Override
            protected void onPostExecute(final MuPDFAlert result) {
                // core.waitForAlert may return null when shutting down
                if (result == null)
                    return;
                final MuPDFAlert.ButtonPressed pressed[] = new MuPDFAlert.ButtonPressed[3];
                for (int i = 0; i < 3; i++)
                    pressed[i] = MuPDFAlert.ButtonPressed.None;
                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mAlertDialog = null;
                        if (mAlertsActive) {
                            int index = 0;
                            switch (which) {
                                case AlertDialog.BUTTON1:
                                    index = 0;
                                    break;
                                case AlertDialog.BUTTON2:
                                    index = 1;
                                    break;
                                case AlertDialog.BUTTON3:
                                    index = 2;
                                    break;
                            }
                            result.buttonPressed = pressed[index];
                            // Send the user's response to the core, so that it can
                            // continue processing.
                            muPDFCore.replyToAlert(result);
                            // Create another alert-waiter to pick up the next alert.
                            createAlertWaiter();
                        }
                    }
                };
                mAlertDialog = mAlertBuilder.create();
                mAlertDialog.setTitle(result.title);
                mAlertDialog.setMessage(result.message);
                switch (result.iconType) {
                    case Error:
                        break;
                    case Warning:
                        break;
                    case Question:
                        break;
                    case Status:
                        break;
                }
                switch (result.buttonGroupType) {
                    case OkCancel:
                        mAlertDialog.setButton(AlertDialog.BUTTON2, getString(R.string.cancel), listener);
                        pressed[1] = MuPDFAlert.ButtonPressed.Cancel;
                    case Ok:
                        mAlertDialog.setButton(AlertDialog.BUTTON1, getString(R.string.okay), listener);
                        pressed[0] = MuPDFAlert.ButtonPressed.Ok;
                        break;
                    case YesNoCancel:
                        mAlertDialog.setButton(AlertDialog.BUTTON3, getString(R.string.cancel), listener);
                        pressed[2] = MuPDFAlert.ButtonPressed.Cancel;
                    case YesNo:
                        mAlertDialog.setButton(AlertDialog.BUTTON1, getString(R.string.yes), listener);
                        pressed[0] = MuPDFAlert.ButtonPressed.Yes;
                        mAlertDialog.setButton(AlertDialog.BUTTON2, getString(R.string.no), listener);
                        pressed[1] = MuPDFAlert.ButtonPressed.No;
                        break;
                }
                mAlertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        mAlertDialog = null;
                        if (mAlertsActive) {
                            result.buttonPressed = MuPDFAlert.ButtonPressed.None;
                            muPDFCore.replyToAlert(result);
                            createAlertWaiter();
                        }
                    }
                });

                mAlertDialog.show();
            }
        };

        mAlertTask.executeOnExecutor(new ThreadPerTaskExecutor());
    }

    public void destroyAlertWaiter() {
        mAlertsActive = false;
        if (mAlertDialog != null) {
            mAlertDialog.cancel();
            mAlertDialog = null;
        }
        if (mAlertTask != null) {
            mAlertTask.cancel(true);
            mAlertTask = null;
        }
    }

    @Override
    protected void onStart() {
        if (muPDFCore != null) {
            muPDFCore.startAlerts();
            createAlertWaiter();
        }
        super.onStart();
    }

    @Override
    protected void onPause() {

        if (muPDFCore != null && muPDFCore.checkFileEncrypted() == false) {
            System.out.println("hello pause call");
            encryptPdf();
        }
        super.onPause();

        if (mSearchTask != null) {
            mSearchTask.stop();
        }
    }

    @Override
    protected void onResume() {

        if (muPDFCore != null) {
            if (muPDFCore.checkFileEncrypted()) {
                decryptPdf();
            }
        }
        super.onResume();
    }

    @Override
    protected void onStop() {
        if (muPDFCore != null) {
            destroyAlertWaiter();
            muPDFCore.stopAlerts();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {

        if (muPDFReaderView != null) {
            muPDFReaderView.applyToChildren(new ReaderView.ViewMapper() {
                public void applyToView(View view) {
                    ((MuPDFView) view).releaseBitmaps();
                }
            });
        }
        if (muPDFCore != null) {
            if (muPDFCore.checkFileEncrypted() == false) {
                encryptPdf();
            }
            muPDFCore.onDestroy();
        }

        if (mAlertTask != null) {
            mAlertTask.cancel(true);
            mAlertTask = null;
        }
        muPDFCore = null;
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (muPDFCore != null && muPDFCore.hasChanges()) {
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (which == AlertDialog.BUTTON_POSITIVE) {
                        muPDFCore.save();
                        if (!muPDFCore.checkFileEncrypted()) {
                            encryptPdf();
                        }
                    }
                    finish();
                }
            };
            AlertDialog alert = mAlertBuilder.create();
            alert.setTitle(R.string.dialog_title);
            alert.setMessage(getString(R.string.document_has_changes_save_them));
            alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes), listener);
            alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.no), listener);
            alert.show();
        } else {
            finish();
        }
    }

    private void encryptPdf() {

        try {
            muPDFCore.encryptWithAes(muPDFCore.getCurrentFilePath(), muPDFCore.getAndriodId(this).getBytes());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    private void decryptPdf() {
        try {
            muPDFCore.decryptWithAes(muPDFCore.getCurrentFilePath(), muPDFCore.getAndriodId(this).getBytes());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    class ThreadPerTaskExecutor implements Executor {
        public void execute(Runnable r) {
            new Thread(r).start();
        }
    }

    enum TopBarMode {
        Main, Search, Annot, Delete, Accept
    }

    enum AcceptMode {
        Highlight, Underline, StrikeOut, Ink, CopyText
    }
}
