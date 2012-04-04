package com.jayway.android.robotium.solo;

import android.graphics.RectF;
import android.util.Log;
import android.util.Pair;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * 
 * @author Michael Shi, mshi@zynga.com
 * 
 */
public final class WebViewUtils {

    private final ActivityUtils mActivityUtils;
    private static final String GET_POS_FUNCTION = "(function (element) {var l = t = 0;if (element.offsetParent) {do {l += element.offsetLeft;t += element.offsetTop;}while (element = element.offsetParent);return [l, t];}})";
    // private static final String GET_RECT_FUNCTION =
    // "(function(element){var l=t=h=w=0;if(element.style.pixelWidth){w=element.style.pixelWidth}else{w=element.offsetWidth}if(element.style.pixelHeight){h=element.style.pixelHeight}else{h=element.offsetHeight}if(element.offsetParent){do{l+=element.offsetLeft;t+=element.offsetTop}while(element=element.offsetParent);return[l,t,w,h]}})";
    private static final String GET_RECT_FUNCTION = "(function(element){var l=t=h=w=0;w=element.offsetWidth;h=element.offsetHeight;if(element.offsetParent){do{l+=element.offsetLeft;t+=element.offsetTop}while(element=element.offsetParent);return[l,t,w,h]}})";
    private static final String LOG_TAG = "Robotium.WebViewUtils";
    private static String mWebReturnVal;
    private final static Object locker = new Object();
    private WebView mWebView = null;
    private boolean mInitialized = false;

    public WebViewUtils(ActivityUtils activityUtils) {
        mActivityUtils = activityUtils;
    }

    public void endSession() {
        mInitialized = false;
        mWebView = null;
    }

    public void processJavascript(final WebView view, final String args) {
        final SynchronousJavascriptInterface jsInterface = new SynchronousJavascriptInterface();
        if (mWebView == null) {
            mWebView = view;
        }

        mActivityUtils.getCurrentActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final boolean oldSettings = mWebView.getSettings().getJavaScriptEnabled();
                Log.i(LOG_TAG, "Javascript enabled: " + oldSettings);
                if (!oldSettings) {
                    mWebView.getSettings().setJavaScriptEnabled(true);
                    Log.i(LOG_TAG, "Javascript enabled.");
                }

                if (!mInitialized) {
                    mWebView.addJavascriptInterface(jsInterface, jsInterface.getInterfaceName());
                    mWebView.setWebViewClient(new WebViewClient() {
                        @Override
                        public void onPageFinished(WebView wView, String url) {
                            super.onPageFinished(wView, url);
                            mWebReturnVal = jsInterface.getJSValue(wView, args);
                            synchronized (locker) {
                                locker.notify();
                            }
                            if (!oldSettings) {
                                wView.getSettings().setJavaScriptEnabled(false);
                                Log.i(LOG_TAG, "Javascript reverted back to disabled.");
                            }
                        }
                    });
                    mWebView.reload();
                    mInitialized = true;
                } else {
                    mWebReturnVal = jsInterface.getJSValue(mWebView, args);
                    synchronized (locker) {
                        locker.notify();
                    }
                }
            }
        });

        synchronized (locker) {
            try {
                locker.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public final Pair<Float, Float> getCoordinatesByName(final WebView view, final String name) {
        return getCoordinatesByName(view, name, 0);
    }

    public final Pair<Float, Float> getCoordinatesByName(final WebView view, final String name, final int index) {
        processJavascript(view, String.format("%s(document.getElementsByName('%s')[%d])", GET_POS_FUNCTION, name, index));

        Assert.assertNotNull(mWebReturnVal);

        Log.i(LOG_TAG, mWebReturnVal);
        final String[] tmp = mWebReturnVal.split(",");
        final float scale = view.getScale();
        return new Pair<Float, Float>(Float.parseFloat(tmp[0]) * scale, Float.parseFloat(tmp[1]) * scale);
    }

    public final RectF getRectByName(final WebView view, final String name) {
        return getRectByName(view, name, 0);
    }

    public final RectF getRectByName(final WebView view, final String name, final int index) {
        processJavascript(view, String.format("%s(document.getElementsByName('%s')[%d])", GET_RECT_FUNCTION, name, index));
        Assert.assertNotNull(mWebReturnVal);

        Log.i(LOG_TAG, mWebReturnVal);
        final String[] tmp = mWebReturnVal.split(",");
        final float scale = view.getScale();
        final float x = Integer.parseInt(tmp[0]) * scale;
        final float y = Integer.parseInt(tmp[1]) * scale;
        final float w = Integer.parseInt(tmp[2]) * scale;
        final float h = Integer.parseInt(tmp[3]) * scale;

        Log.i(LOG_TAG, String.format("Rect: %f, %f, %f, %f", x, y, x + w, y + h));
        return new RectF(x, y, x + w, y + h);
    }

    /**
     * Provides an interface for getting synchronous javascript calls
     * 
     * @author Michael Shi, mshi@zynga.com
     * 
     */
    private final static class SynchronousJavascriptInterface {

        private static final String LOG_TAG = "Robotium.SynchronousJavascriptInterface";

        /** The Javascript interface name for adding to web view. */
        private final static String INTERFACE_NAME = "SynchronousJS";

        private final static Object pLocker = new Object();

        /** Return value to wait for. */
        private static String returnValue;

        public SynchronousJavascriptInterface() {
        }

        /**
         * Evaluates the expression and returns the value.
         * 
         * @param webView
         *            {@code WebView} to perform the evaluation on
         * @param expression
         *            expression to evaluate
         * @return the return value of Javascript. {@code null} if error occurred.
         */
        public final String getJSValue(final WebView webView, final String expression) {
            String code = String.format("javascript:%s.setValue((function(){try{return %s+\"\";}catch(err){return err;}})());", INTERFACE_NAME, expression);
            try {
                webView.loadUrl(code);
                synchronized (pLocker) {
                    pLocker.wait(1000);
                }
                return returnValue;
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, "InterruptedException", e);
            }
            Log.e(LOG_TAG, "Error evaluating Javascript.");
            return null;
        }

        /**
         * Retrieves Javascript return value
         * 
         * @param value
         */
        @SuppressWarnings("unused")
        public void setValue(String value) {
            Log.i(LOG_TAG, "setValue called. Valued received: " + value);
            synchronized (pLocker) {
                returnValue = value;
                pLocker.notify();
            }
        }

        /**
         * Gets the interface name
         * 
         * @return
         */
        public final String getInterfaceName() {
            return INTERFACE_NAME;
        }
    }
}
