/**
 * 
 */
package com.jayway.android.robotium.solo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import junit.framework.Assert;
import android.app.Activity;
import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;

/**
 * @author mshi
 * 
 */
public class ExtensionUtils implements BaseExtensionUtils {

    private final Activity mActivity;
    private final Instrumentation mInstrumentation;
    private final ActivityUtils mActivityUtils;
    private final static String LOG_TAG = "ExtentionUtils";

    public ExtensionUtils(final Activity activity, final Instrumentation instrumentation, final ActivityUtils activityUtils) {
        mActivity = activity;
        mInstrumentation = instrumentation;
        mActivityUtils = activityUtils;
    }

    /**
     * Gets cache of the test project. i.e this project
     * 
     * @return Returns the cache of the local project
     */
    public final File getTestCache() {
        final File cache = mInstrumentation.getContext().getCacheDir();
        return cache == null ? mInstrumentation.getContext().getDir("cache", 0) : cache;
    }

    /**
     * Gets the cache of the project that is being tested on. e.g. Scramble
     * 
     * @return Cache directory of remote project
     */
    public final File getActivityCache() {
        final File cache = mActivity.getApplicationContext().getCacheDir();
        return cache == null ? mActivity.getApplicationContext().getDir("cache", 0) : cache;
    }

    /**
     * Returns the screenshots path where all screenshots should be stored
     * 
     * @return screenshots path
     */
    public final String getScreenshotsPath() {
        return mActivity.getApplicationContext().getExternalCacheDir().getAbsolutePath() + "/screenshots/";
    }

    /**
     * Take screenshot of the current screen
     * 
     * @return
     */
    public final Bitmap takeScreenShot() {
        return takeScreenShot(mActivityUtils.getCurrentActivity());
    }

    /**
     * Take screenshot of {@code activity}
     * 
     * @param activity
     *            Activity to take screenshot of
     * @return Bitmap representing the screenshot
     */

    public Bitmap takeScreenShot(final Activity activity) {
        return takeScreenShot(activity.findViewById(android.R.id.content).getRootView());
    }

    /**
     * Take screenshot of {@code view}
     * 
     * @param view
     *            View to take screenshot of
     * @return Bitmap representing the screenshot
     */
    public final Bitmap takeScreenShot(final View view) {
        view.setDrawingCacheEnabled(true);
        final Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
        view.setDrawingCacheEnabled(false);
        return bitmap;
    }

    /**
     * Save the {@code drawable} as {@code name}.jpg in local cache directory
     * 
     * @param drawable
     *            Drawable to save
     * @param name
     *            Name of file to save as
     * @throws IOException
     */
    public void saveDrawable(final Drawable drawable, final String name) throws IOException {
        if (drawable != null) {
            saveBitmap(((BitmapDrawable) drawable).getBitmap(), name);
        } else {
            Log.e(LOG_TAG, "Null Drawable!");
        }
    }

    /**
     * Save the {@code bitmap} to local file in cache directory as {@code name}.jpg
     * 
     * @param bitmap
     *            Bitmap to save
     * @param name
     *            Name of the file to save as
     * @throws IOException
     */
    public void saveBitmap(final Bitmap bitmap, final String name) throws IOException {

        FileOutputStream fos = null;
        try {
            String dir = getScreenshotsPath();
            File sddir = new File(dir);
            if (!sddir.exists()) {
                sddir.mkdirs();
            }
            String path = dir + name + "_" + System.currentTimeMillis() + ".jpg";
            fos = new FileOutputStream(path);
            Log.i(LOG_TAG, "Saving screenshot: " + path);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error saving file screenshot");
            e.printStackTrace();
        }
    }

    /**
     * Take screenshot of the {@code activity} and save it as {@code name}.jpg in cache directory
     * 
     * @param activity
     *            Activity to take screenshot of
     * @param name
     *            Name of file to be saved
     * @return
     * @throws Exception
     */
    public final Bitmap takeScreenShotAndSave(final Activity activity, final String name) throws IOException {
        final Bitmap bitmap = takeScreenShot(activity);
        saveBitmap(bitmap, name);
        return bitmap;
    }

    /**
     * Take screenshot of the {@code view} and save it as {@code name}.jpg in the cache directory
     * 
     * @param view
     *            View to take screenshot of
     * @param name
     *            Name of the file to be saved
     * @return Returns a Bitmap representation of the current view (screenshot)
     * @throws Exception
     */
    public final Bitmap takeScreenShotAndSave(final View view, final String name) throws IOException {
        final Bitmap bitmap = takeScreenShot(view);
        saveBitmap(bitmap, name);
        return bitmap;
    }

    /**
     * Function that fails the current test and logs it in logcat
     * 
     * @param msg
     *            Meaningful message to describe what is expected and what happened
     * @param screenshot
     *            {@code true} if screenshot should be taken
     * @param name
     *            name of filename of screenshot if screenshot is selected
     */

    public void fail(String msg, boolean screenshot, String name) {
        if (screenshot) {
            try {
                takeScreenShotAndSave(mActivityUtils.getCurrentActivity(), "fail");
            } catch (Exception e) {
                Log.e(LOG_TAG, "Problem saving screenshot of current activity that failed.");
                e.printStackTrace();
            }
        }
        Log.e(LOG_TAG, String.format("%s. Current activity: ", msg, mActivityUtils.getCurrentActivity().toString()));
        Assert.fail(msg);
    }
}
