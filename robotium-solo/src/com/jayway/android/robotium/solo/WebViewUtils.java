package com.jayway.android.robotium.solo;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.net.http.SslError;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
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
    public static final WebViewClient DEFAULT_CLIENT = new WebViewClient();

    public WebViewUtils(ActivityUtils activityUtils) {
        mActivityUtils = activityUtils;
    }

    public void endSession() {
        mInitialized = false;
        mWebView = null;
    }

    public void processJavascript(final WebView webView, final String args, final WebViewClient customClient) {
        final SynchronousJavascriptInterface jsInterface = new SynchronousJavascriptInterface();
        if (mWebView == null) {
            mWebView = webView;
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
                        public void onFormResubmission(WebView view, Message dontResend, Message resend) {
                            customClient.onFormResubmission(view, dontResend, resend);
                        }

                        @Override
                        public void onLoadResource(WebView view, String url) {
                            customClient.onLoadResource(view, url);
                        }

                        @Override
                        public void onPageFinished(WebView view, String url) {
                            customClient.onPageFinished(view, url);
                            mWebReturnVal = jsInterface.getJSValue(view, args);
                            synchronized (locker) {
                                locker.notify();
                            }
                            if (!oldSettings) {
                                view.getSettings().setJavaScriptEnabled(false);
                                Log.i(LOG_TAG, "Javascript reverted back to disabled.");
                            }
                        }

                        @Override
                        public void onPageStarted(WebView view, String url, Bitmap favicon) {
                            customClient.onPageStarted(view, url, favicon);
                        }

                        @Override
                        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                            customClient.onReceivedError(view, errorCode, description, failingUrl);
                        }

                        @Override
                        public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
                            customClient.onReceivedHttpAuthRequest(view, handler, host, realm);
                        }

                        @Override
                        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                            customClient.onReceivedSslError(view, handler, error);
                        }
                        @Override
                        public void onScaleChanged(WebView view, float oldScale, float newScale) {
                            customClient.onScaleChanged(view, oldScale, newScale);
                        }
                        @Override
                        public void onTooManyRedirects(WebView view, Message cancelMsg, Message continueMsg) {
                            customClient.onTooManyRedirects(view, cancelMsg, continueMsg);
                        }
                        @Override
                        public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
                            customClient.onUnhandledKeyEvent(view, event);
                        }
                        @Override
                        public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
                            return customClient.shouldOverrideKeyEvent(view, event);
                        }
                        @Override
                        public boolean shouldOverrideUrlLoading(WebView view, String url) {
                            return customClient.shouldOverrideUrlLoading(view, url);
                        }
                        @Override
                        public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                            customClient.doUpdateVisitedHistory(view, url, isReload);
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
        return getCoordinatesByName(view, name, 0, DEFAULT_CLIENT);
    }

    public final Pair<Float, Float> getCoordinatesByName(final WebView view, final String name, final int index, final WebViewClient customClient) {
        processJavascript(view, String.format("%s(document.getElementsByName('%s')[%d])", GET_POS_FUNCTION, name, index), customClient);

        Assert.assertNotNull(mWebReturnVal);

        Log.i(LOG_TAG, mWebReturnVal);
        final String[] tmp = mWebReturnVal.split(",");
        final float scale = view.getScale();
        return new Pair<Float, Float>(Float.parseFloat(tmp[0]) * scale, Float.parseFloat(tmp[1]) * scale);
    }

    public final RectF getRectByName(final WebView view, final String name) {
        return getRectByName(view, name, 0, DEFAULT_CLIENT);
    }

    public final RectF getRectByName(final WebView view, final String name, final int index, final WebViewClient customClient) {
        processJavascript(view, String.format("%s(document.getElementsByName('%s')[%d])", GET_RECT_FUNCTION, name, index), customClient);
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
