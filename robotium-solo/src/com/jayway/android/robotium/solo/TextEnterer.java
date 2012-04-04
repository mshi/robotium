package com.jayway.android.robotium.solo;

import android.app.Instrumentation;
import android.graphics.RectF;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.widget.EditText;

/**
 * Contains setEditText() to enter text into text fields.
 * 
 * @author Renas Reda, renas.reda@jayway.com
 * @modified Michael Shi, mshi@zynga.com
 */

class TextEnterer {

    private static final String LOG_TAG = "Robotium.TextEnterer";
    private final Instrumentation inst;
    private final Clicker clicker;
    private final WebViewUtils mWebViewUtils;
    private final Sleeper mSleeper;

    /**
     * Construct object
     * 
     * @param inst
     * @param clicker
     * @param webViewUtils
     */
    public TextEnterer(Instrumentation inst, Clicker clicker, WebViewUtils webViewUtils, Sleeper sleeper) {
        this.inst = inst;
        this.clicker = clicker;
        mWebViewUtils = webViewUtils;
        mSleeper = sleeper;
    }

    /**
     * Constructs this object.
     * 
     * @param inst
     *            the {@code Instrumentation} instance.
     * @param clicker
     *            the {@code Clicker} instance.
     * 
     */

    public TextEnterer(Instrumentation inst, Clicker clicker) {
        this(inst, clicker, null, new Sleeper());
    }

    /**
     * Sets an {@code EditText} text
     * 
     * @param index
     *            the index of the {@code EditText}
     * @param text
     *            the text that should be set
     */

    public void setEditText(final EditText editText, final String text) {
        if (editText != null) {
            final String previousText = editText.getText().toString();
            if (!editText.isEnabled())
                Assert.assertTrue("Edit text is not enabled!", false);

            inst.runOnMainSync(new Runnable() {
                public void run() {
                    editText.setInputType(InputType.TYPE_NULL);
                    editText.performClick();
                    if (text.equals(""))
                        editText.setText(text);
                    else {
                        editText.setText(previousText + text);
                        editText.setCursorVisible(false);
                    }
                }
            });
        }
    }

    /**
     * Types text in an {@code EditText}
     * 
     * @param index
     *            the index of the {@code EditText}
     * @param text
     *            the text that should be typed
     */

    public void typeText(final EditText editText, final String text) {
        if (editText != null) {
            inst.runOnMainSync(new Runnable() {
                public void run() {
                    editText.setInputType(InputType.TYPE_NULL);
                }
            });
            clicker.clickOnScreen(editText, false, 0);
            inst.sendStringSync(text);
        }
    }

    /**
     * Type text to whatever is focused.
     * 
     * @param text
     *            text that should be typed
     */
    public void typeTextToWebViewElementByName(final WebView webView, final String name, final String text) {
        final int[] xy = new int[2];
        final RectF rect = mWebViewUtils.getRectByName(webView, name, 0);

        webView.getLocationOnScreen(xy);
        Log.i(LOG_TAG, String.format("Location of view: %d, %d", xy[0], xy[1]));
        clicker.clickOnScreen(rect.centerX() + xy[0], rect.centerY() + xy[1]);
        Log.i(LOG_TAG, "I CLICKED IT!!!");
        mSleeper.sleep();
        inst.sendStringSync(text);
        mSleeper.sleep();
        inst.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK); // hide the keyboard
        mSleeper.sleep();
    }
}
