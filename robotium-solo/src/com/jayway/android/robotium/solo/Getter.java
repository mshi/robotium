package com.jayway.android.robotium.solo;

import java.util.ArrayList;
import java.util.HashSet;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

/**
 * Contains various get methods. Examples are: getView(int id), getView(Class<T> classToFilterBy, int index).
 * 
 * @author Renas Reda, renas.reda@jayway.com
 * @modified Michael Shi, mshi@zynga.com
 * 
 */

class Getter {

    private final ActivityUtils activityUtils;
    private final ViewFetcher viewFetcher;
    private final Waiter waiter;
    private final int TIMEOUT = 10000;

    /**
     * Constructs this object.
     * 
     * @param activityUtils
     *            the {@code ActivityUtil} instance.
     * @param viewFetcher
     *            the {@code ViewFetcher} instance.
     * @param waiter
     *            the {@code Waiter} instance
     */

    public Getter(ActivityUtils activityUtils, ViewFetcher viewFetcher, Waiter waiter) {
        this.activityUtils = activityUtils;
        this.viewFetcher = viewFetcher;
        this.waiter = waiter;
    }

    /**
     * Returns a {@code View} with a certain index, from the list of current {@code View}s of the specified type.
     * 
     * @param classToFilterBy
     *            which {@code View}s to choose from
     * @param index
     *            choose among all instances of this type, e.g. {@code Button.class} or {@code EditText.class}
     * @return a {@code View} with a certain index, from the list of current {@code View}s of the specified type
     */

    public <T extends View> T getView(Class<T> classToFilterBy, int index) {
        return waiter.waitForAndGetView(index, classToFilterBy);
    }

    /**
     * Returns a {@code View} that shows a given text, from the list of current {@code View}s of the specified type.
     * 
     * @param classToFilterBy
     *            which {@code View}s to choose from
     * @param text
     *            the text that the view shows
     * @param onlyVisible
     *            {@code true} if only visible texts on the screen should be returned
     * @return a {@code View} showing a given text, from the list of current {@code View}s of the specified type
     */

    public <T extends TextView> T getView(Class<T> classToFilterBy, String text, boolean onlyVisible) {
        return getView(classToFilterBy, text, onlyVisible, false, 1, null);
    }

    /**
     * Returns a {@code View} that shows a given text, from the list of current {@code View}s of the specified type.
     * 
     * @param classToFilterBy
     *            which {@code View}s to choose from
     * @param text
     *            the text that the view shows
     * @param onlyVisible
     *            {@code true} if only visible texts on the screen should be returned
     * @param useRegex
     *            {@code true} if {@value text} should be treated as regular expression
     * @return a {@code View} showing a given text, from the list of current {@code View}s of the specified type
     */

    public <T extends TextView> T getView(Class<T> classToFilterBy, String text, boolean onlyVisible, boolean useRegex) {
        return getView(classToFilterBy, text, onlyVisible, useRegex, 1, null);
    }

    /**
     * Returns a {@code View} that shows a given text, from the list of current {@code View}s of the specified type.
     * 
     * @param classToFilterBy
     *            which {@code View}s to choose from
     * @param text
     *            the text that the view shows
     * @param onlyVisible
     *            {@code true} if only visible texts on the screen should be returned
     * @param useRegex
     *            {@code true} if {@value text} should be treated as regular expression
     * @param match
     *            the regex match that should be returned (1 for first match)
     * @return a {@code View} showing a given text, from the list of current {@code View}s of the specified type
     */

    public <T extends TextView> T getView(Class<T> classToFilterBy, String text, boolean onlyVisible, boolean useRegex, int match) {
        return getView(classToFilterBy, text, onlyVisible, useRegex, match, null);
    }

    /**
     * Returns a {@code View} that shows a given text, from the list of current {@code View}s of the specified type that occurs after {@code String}.
     * 
     * @param classToFilterBy
     *            which {@code View}s to choose from
     * @param text
     *            the text that the view shows
     * @param onlyVisible
     *            {@code true} if only visible texts on the screen should be returned
     * @param useRegex
     *            {@code true} if {@value text} should be treated as regular expression
     * @param match
     *            the regex match that should be returned (1 for first match)
     * @param searchAfter
     *            text to start search at. {@code null} to start at beginning
     * @return a {@code View} showing a given text, from the list of current {@code View}s of the specified type
     */

    public <T extends TextView> T getView(Class<T> classToFilterBy, String text, boolean onlyVisible, boolean useRegex, int match, String searchAfter) {
        waiter.waitForText(text, 0, TIMEOUT, false, onlyVisible);
        ArrayList<T> views = viewFetcher.getCurrentViews(classToFilterBy);
        final HashSet<TextView> uniqueTextViews = new HashSet<TextView>();
        boolean found = false;
        final boolean search = searchAfter != null;
        if (match < 1) {
            throw new IllegalStateException("Wtf are you trying to find with less than 1 matches?");
        }
        if (onlyVisible)
            views = RobotiumUtils.removeInvisibleViews(views);
        T viewToReturn = null;
        for (T view : views) {
            if (!search || found) {
                if (useRegex && RobotiumUtils.checkAndGetMatches(text, view, uniqueTextViews) == match) {
                    viewToReturn = view;
                    break;
                } else if (view.getText().toString().equals(text)) {
                    viewToReturn = view;
                    break;
                }
            } else if (search) {
                if (useRegex && RobotiumUtils.checkAndGetMatches(searchAfter, view, uniqueTextViews) == 1) {
                    uniqueTextViews.clear();
                    found = true;
                } else if (view.getText().toString().equals(searchAfter)) {
                    uniqueTextViews.clear();
                    found = true;
                }
            }
        }

        if (viewToReturn == null) {
            Assert.assertTrue("No " + classToFilterBy.getSimpleName() + " with text " + text + " is found!", false);
        }

        return viewToReturn;
    }

    /**
     * Returns a {@code View} with a given id.
     * 
     * @param id
     *            the R.id of the {@code View} to be returned
     * @return a {@code View} with a given id
     */

    public View getView(int id) {
        final Activity activity = activityUtils.getCurrentActivity(false);

        View view = activity.findViewById(id);
        if (view != null)
            return view;

        return waiter.waitForView(id);
    }
}
