/**
 * 
 */
package com.jayway.android.robotium.solo;

import android.app.Activity;
import android.graphics.Bitmap;

/**
 * @author mshi
 * 
 */
public interface BaseExtensionUtils {
    public abstract Bitmap takeScreenShot(final Activity activity);
}
