package com.jayway.android.robotium.solo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.SlidingDrawer;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;

/**
 * Contains all the methods that the sub-classes have. It supports test cases that span over multiple activities.
 * 
 * Robotium has full support for Activities, Dialogs, Toasts, Menus and Context Menus.
 * 
 * When writing tests there is no need to plan for or expect new activities in the test case. All is handled automatically by Robotium-Solo. Robotium-Solo can
 * be used in conjunction with ActivityInstrumentationTestCase2. The test cases are written from a user perspective were technical details are not needed.
 * 
 * 
 * Example of usage (test case spanning over multiple activities):
 * 
 * <pre>
 * 
 * public void setUp() throws Exception {
 *     solo = new Solo(getInstrumentation(), getActivity());
 * }
 * 
 * public void testTextShows() throws Exception {
 * 
 *     solo.clickOnText(&quot;Categories&quot;);
 *     solo.clickOnText(&quot;Other&quot;);
 *     solo.clickOnButton(&quot;Edit&quot;);
 *     solo.searchText(&quot;Edit Window&quot;);
 *     solo.clickOnButton(&quot;Commit&quot;);
 *     assertTrue(solo.searchText(&quot;Changes have been made successfully&quot;));
 * }
 * 
 * </pre>
 * 
 * 
 * @author Renas Reda, renas.reda@jayway.com
 * @modified Michael Shi, mshi@zynga.com
 * 
 */

public class Solo {

    private final Asserter asserter;
    private final ViewFetcher viewFetcher;
    private final Checker checker;
    private final Clicker clicker;
    private final Presser presser;
    private final Searcher searcher;
    private final ActivityUtils activityUtils;
    private final DialogUtils dialogUtils;
    private final TextEnterer textEnterer;
    private final Scroller scroller;
    private final RobotiumUtils robotiumUtils;
    private final Sleeper sleeper;
    private final Waiter waiter;
    private final Setter setter;
    private final Getter getter;
    private final ExtensionUtils extUtils;
    private final static int TIMEOUT = 20000;
    private final static int SMALLTIMEOUT = 10000;
    private final static String LOG_TAG = "Robotium";
    public final static int LANDSCAPE = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE; // 0
    public final static int PORTRAIT = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT; // 1
    public final static int RIGHT = KeyEvent.KEYCODE_DPAD_RIGHT;
    public final static int LEFT = KeyEvent.KEYCODE_DPAD_LEFT;
    public final static int UP = KeyEvent.KEYCODE_DPAD_UP;
    public final static int DOWN = KeyEvent.KEYCODE_DPAD_DOWN;
    public final static int ENTER = KeyEvent.KEYCODE_ENTER;
    public final static int MENU = KeyEvent.KEYCODE_MENU;
    public final static int DELETE = KeyEvent.KEYCODE_DEL;
    public final static int CLOSED = 0;
    public final static int OPENED = 1;
    public final static int LOCATION_ABOVE = Constants.LOCATION_ABOVE;
    public final static int LOCATION_BELOW = Constants.LOCATION_BELOW;

    public Solo(Instrumentation instrumentation, Activity activity, final BaseExtensionUtils extensionUtils) {
        this.sleeper = new Sleeper();
        this.activityUtils = new ActivityUtils(instrumentation, activity, sleeper);
        if (extensionUtils != null) {
            this.extUtils = new ExtensionUtils(activity, instrumentation, activityUtils) {
                @Override
                public final Bitmap takeScreenShot(final Activity activity) { // leaving room for AndEngine screenshots
                    return extensionUtils.takeScreenShot(activity);
                }
            };
        } else {
            this.extUtils = new ExtensionUtils(activity, instrumentation, activityUtils);
        }
        Assert.ExtensionUtils = this.extUtils;
        this.viewFetcher = new ViewFetcher(activityUtils, sleeper);
        this.dialogUtils = new DialogUtils(viewFetcher, sleeper);
        this.scroller = new Scroller(instrumentation, activityUtils, viewFetcher, sleeper);
        this.searcher = new Searcher(viewFetcher, scroller, sleeper);
        this.waiter = new Waiter(activityUtils, viewFetcher, searcher, scroller, sleeper);
        this.setter = new Setter(activityUtils);
        this.getter = new Getter(activityUtils, viewFetcher, waiter);
        this.asserter = new Asserter(activityUtils, waiter);
        this.checker = new Checker(viewFetcher, waiter);
        this.robotiumUtils = new RobotiumUtils(instrumentation, sleeper);
        this.clicker = new Clicker(viewFetcher, scroller, robotiumUtils, instrumentation, sleeper, waiter, searcher);
        this.presser = new Presser(clicker, instrumentation, sleeper, waiter);
        this.textEnterer = new TextEnterer(instrumentation, clicker);
        this.viewFetcher.setScroller(this.scroller);
    }

    public Solo(Instrumentation instrumentation, final BaseExtensionUtils extensionUtils) {
        this(instrumentation, null, extensionUtils);
    }

    /**
     * Constructor that takes in the instrumentation and the start activity.
     * 
     * @param instrumentation
     *            the {@link Instrumentation} instance
     * @param activity
     *            the start {@link Activity} or {@code null} if no start activity is provided
     * 
     */

    public Solo(Instrumentation instrumentation, Activity activity) {
        this(instrumentation, activity, null);

    }

    /**
     * Constructor that takes in the instrumentation.
     * 
     * @param instrumentation
     *            the {@link Instrumentation} instance
     * 
     */

    public Solo(Instrumentation instrumentation) {
        this(instrumentation, null, null);
    }

    /**
     * Choose whether to turn on/off screenshot capture when assertion fails. Default is false.
     * 
     * @param enable
     *            {@code true} to enable screenshot captures on assertion fails
     */
    public void enableAssertionScreenshot(boolean enable) {
        Constants.SCREENSHOT_ASSERTS = enable;
    }

    /**
     * Returns the ActivityMonitor used by Robotium.
     * 
     * @return the ActivityMonitor used by Robotium
     */

    public ActivityMonitor getActivityMonitor() {
        return activityUtils.getActivityMonitor();
    }

    /**
     * Returns an ArrayList of all the View objects located in the focused Activity or Dialog.
     * 
     * @return an {@code ArrayList} of the {@link View} objects located in the focused window
     * 
     */

    public ArrayList<View> getViews() {
        try {
            return viewFetcher.getViews(null, false);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns an ArrayList of the View objects contained in the parent View.
     * 
     * @param parent
     *            the parent view from which to return the views
     * @return an {@code ArrayList} of the {@link View} objects contained in the given {@code View}
     * 
     */

    public ArrayList<View> getViews(View parent) {
        try {
            return viewFetcher.getViews(parent, false);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns the absolute top parent View for a given View.
     * 
     * @param view
     *            the {@link View} whose top parent is requested
     * @return the top parent {@link View}
     * 
     */

    public View getTopParent(View view) {
        View topParent = viewFetcher.getTopParent(view);
        return topParent;
    }

    /**
     * Waits for a text to be shown. Default timeout is 20 seconds.
     * 
     * @param text
     *            the text to wait for
     * @return {@code true} if text is shown and {@code false} if it is not shown before the timeout
     * 
     */

    public boolean waitForText(String text) {
        return waiter.waitForText(text);
    }

    /**
     * Waits for a text to be shown.
     * 
     * @param text
     *            the text to wait for
     * @param minimumNumberOfMatches
     *            the minimum number of matches that are expected to be shown. {@code 0} means any number of matches
     * @param timeout
     *            the the amount of time in milliseconds to wait
     * @return {@code true} if text is shown and {@code false} if it is not shown before the timeout
     * 
     */

    public boolean waitForText(String text, int minimumNumberOfMatches, long timeout) {
        return waiter.waitForText(text, minimumNumberOfMatches, timeout);
    }

    /**
     * Waits for a text to be shown.
     * 
     * @param text
     *            the text to wait for
     * @param minimumNumberOfMatches
     *            the minimum number of matches that are expected to be shown. {@code 0} means any number of matches
     * @param timeout
     *            the the amount of time in milliseconds to wait
     * @param scroll
     *            {@code true} if scrolling should be performed
     * @return {@code true} if text is shown and {@code false} if it is not shown before the timeout
     * 
     */

    public boolean waitForText(String text, int minimumNumberOfMatches, long timeout, boolean scroll) {
        return waiter.waitForText(text, minimumNumberOfMatches, timeout, scroll);
    }

    /**
     * Waits for a text to be shown.
     * 
     * @param text
     *            the text to wait for
     * @param minimumNumberOfMatches
     *            the minimum number of matches that are expected to be shown. {@code 0} means any number of matches
     * @param timeout
     *            the the amount of time in milliseconds to wait
     * @param scroll
     *            {@code true} if scrolling should be performed
     * @param onlyVisible
     *            {@code true} if only visible text views should be waited for
     * @return {@code true} if text is shown and {@code false} if it is not shown before the timeout
     * 
     */

    public boolean waitForText(String text, int minimumNumberOfMatches, long timeout, boolean scroll, boolean onlyVisible) {
        return waiter.waitForText(text, minimumNumberOfMatches, timeout, scroll, onlyVisible);
    }

    /**
     * Waits for a View of a certain class to be shown. Default timeout is 20 seconds.
     * 
     * @param viewClass
     *            the {@link View} class to wait for
     */

    public <T extends View> boolean waitForView(final Class<T> viewClass) {

        return waiter.waitForView(viewClass, 0, TIMEOUT, true);
    }

    /**
     * Waits for a View to be shown. Default timeout is 20 seconds.
     * 
     * @param view
     *            the {@link View} object to wait for
     * 
     * @return {@code true} if view is shown and {@code false} if it is not shown before the timeout
     */

    public <T extends View> boolean waitForView(View view) {
        return waiter.waitForView(view);
    }

    /**
     * Waits for a View to be shown.
     * 
     * @param view
     *            the {@link View} object to wait for
     * @param timeout
     *            the amount of time in milliseconds to wait
     * @param scroll
     *            {@code true} if scrolling should be performed
     * 
     * @return {@code true} if view is shown and {@code false} if it is not shown before the timeout
     */

    public <T extends View> boolean waitForView(View view, int timeout, boolean scroll) {
        return waiter.waitForView(view, timeout, scroll);
    }

    /**
     * Waits for a View of a certain class to be shown.
     * 
     * @param viewClass
     *            the {@link View} class to wait for
     * @param minimumNumberOfMatches
     *            the minimum number of matches that are expected to be shown. {@code 0} means any number of matches
     * @param timeout
     *            the amount of time in milliseconds to wait
     * @return {@code true} if view is shown and {@code false} if it is not shown before the timeout
     */

    public <T extends View> boolean waitForView(final Class<T> viewClass, final int minimumNumberOfMatches, final int timeout) {
        int index = minimumNumberOfMatches - 1;

        if (index < 1)
            index = 0;

        return waiter.waitForView(viewClass, index, timeout, true);
    }

    /**
     * Waits for a View of a certain class to be shown.
     * 
     * @param viewClass
     *            the {@link View} class to wait for
     * @param minimumNumberOfMatches
     *            the minimum number of matches that are expected to be shown. {@code 0} means any number of matches
     * @param timeout
     *            the amount of time in milliseconds to wait
     * @param scroll
     *            {@code true} if scrolling should be performed
     * @return {@code true} if the {@link View} is shown and {@code false} if it is not shown before the timeout
     */

    public <T extends View> boolean waitForView(final Class<T> viewClass, final int minimumNumberOfMatches, final int timeout, final boolean scroll) {
        int index = minimumNumberOfMatches - 1;

        if (index < 1)
            index = 0;

        return waiter.waitForView(viewClass, index, timeout, scroll);
    }

    /**
     * Searches for a text string in the EditText objects currently shown and returns true if found. Will automatically scroll when needed.
     * 
     * @param text
     *            the text to search for
     * @return {@code true} if an {@link EditText} with the given text is found or {@code false} if it is not found
     * 
     */

    public boolean searchEditText(String text) {
        return searcher.searchWithTimeoutFor(EditText.class, text, 1, true, false);
    }

    /**
     * Searches for a text string in the EditText objects currently shown and returns true if found. Will automatically scroll when needed.
     * 
     * @param text
     *            the text to search for
     * @param timeout
     *            milliseconds to timeout for
     * @return {@code true} if an {@link EditText} with the given text is found or {@code false} if it is not found
     * 
     */

    public boolean searchEditText(String text, int timeout) {
        return searcher.searchWithTimeoutFor(EditText.class, text, 1, true, false, timeout);
    }

    /**
     * Searches for a Button with the given text string and returns true if at least one Button is found. Will automatically scroll when needed.
     * 
     * @param text
     *            the text to search for. The parameter will be interpreted as a regular expression
     * @return {@code true} if a {@link Button} with the given text is found and {@code false} if it is not found
     * 
     */

    public boolean searchButton(String text) {
        return searcher.searchWithTimeoutFor(Button.class, text, 0, true, false);
    }

    /**
     * Searches for a Button with the given text string and returns true if at least one Button is found. Will automatically scroll when needed.
     * 
     * @param text
     *            the text to search for. The parameter will be interpreted as a regular expression
     * @param onlyVisible
     *            {@code true} if only {@link Button} visible on the screen should be searched
     * @return {@code true} if a {@link Button} with the given text is found and {@code false} if it is not found
     * 
     */

    public boolean searchButton(String text, boolean onlyVisible) {
        return searcher.searchWithTimeoutFor(Button.class, text, 0, true, onlyVisible);
    }

    /**
     * Searches for a ToggleButton with the given text string and returns {@code true} if at least one ToggleButton is found. Will automatically scroll when
     * needed.
     * 
     * @param text
     *            the text to search for. The parameter will be interpreted as a regular expression
     * @return {@code true} if a {@link ToggleButton} with the given text is found and {@code false} if it is not found
     * 
     */

    public boolean searchToggleButton(String text) {
        return searcher.searchWithTimeoutFor(ToggleButton.class, text, 0, true, false);
    }

    /**
     * Searches for a Button with the given text string and returns {@code true} if the searched Button is found a given number of times. Will automatically
     * scroll when needed.
     * 
     * @param text
     *            the text to search for. The parameter will be interpreted as a regular expression
     * @param minimumNumberOfMatches
     *            the minimum number of matches expected to be found. {@code 0} matches means that one or more matches are expected to be found
     * @return {@code true} if a {@link Button} with the given text is found a given number of times and {@code false} if it is not found
     * 
     */

    public boolean searchButton(String text, int minimumNumberOfMatches) {
        return searcher.searchWithTimeoutFor(Button.class, text, minimumNumberOfMatches, true, false);
    }

    /**
     * Searches for a Button with the given text string and returns {@code true} if the searched Button is found a given number of times. Will automatically
     * scroll when needed.
     * 
     * @param text
     *            the text to search for. The parameter will be interpreted as a regular expression
     * @param minimumNumberOfMatches
     *            the minimum number of matches expected to be found. {@code 0} matches means that one or more matches are expected to be found
     * @param onlyVisible
     *            {@code true} if only {@link Button} visible on the screen should be searched
     * @return {@code true} if a {@link Button} with the given text is found a given number of times and {@code false} if it is not found
     * 
     */

    public boolean searchButton(String text, int minimumNumberOfMatches, boolean onlyVisible) {
        return searcher.searchWithTimeoutFor(Button.class, text, minimumNumberOfMatches, true, onlyVisible);
    }

    /**
     * Searches for a ToggleButton with the given text string and returns {@code true} if the searched ToggleButton is found a given number of times. Will
     * automatically scroll when needed.
     * 
     * @param text
     *            the text to search for. The parameter will be interpreted as a regular expression
     * @param minimumNumberOfMatches
     *            the minimum number of matches expected to be found. {@code 0} matches means that one or more matches are expected to be found
     * @return {@code true} if a {@link ToggleButton} with the given text is found a given number of times and {@code false} if it is not found
     * 
     */

    public boolean searchToggleButton(String text, int minimumNumberOfMatches) {
        return searcher.searchWithTimeoutFor(ToggleButton.class, text, minimumNumberOfMatches, true, false);
    }

    /**
     * Searches for a text string and returns {@code true} if at least one item is found with the expected text. Will automatically scroll when needed.
     * 
     * @param text
     *            the text to search for. The parameter will be interpreted as a regular expression
     * @return {@code true} if the search string is found and {@code false} if it is not found
     * 
     */

    public boolean searchText(String text) {
        return searcher.searchWithTimeoutFor(TextView.class, text, 0, true, false);
    }

    /**
     * Searches for a text string and returns {@code true} if at least one item is found with the expected text. Will automatically scroll when needed.
     * 
     * @param text
     *            the text to search for. The parameter will be interpreted as a regular expression
     * @param onlyVisible
     *            {@code true} if only texts visible on the screen should be searched
     * @return {@code true} if the search string is found and {@code false} if it is not found
     * 
     */

    public boolean searchText(String text, boolean onlyVisible) {
        return searcher.searchWithTimeoutFor(TextView.class, text, 0, true, onlyVisible);
    }

    /**
     * Searches for a text string and returns {@code true} if the searched text is found a given number of times. Will automatically scroll when needed.
     * 
     * @param text
     *            the text to search for. The parameter will be interpreted as a regular expression
     * @param minimumNumberOfMatches
     *            the minimum number of matches expected to be found. {@code 0} matches means that one or more matches are expected to be found
     * @return {@code true} if text string is found a given number of times and {@code false} if the text string is not found
     * 
     */

    public boolean searchText(String text, int minimumNumberOfMatches) {
        return searcher.searchWithTimeoutFor(TextView.class, text, minimumNumberOfMatches, true, false);
    }

    /**
     * Searches for a text string and returns {@code true} if the searched text is found a given number of times.
     * 
     * @param text
     *            the text to search for. The parameter will be interpreted as a regular expression.
     * @param minimumNumberOfMatches
     *            the minimum number of matches expected to be found. {@code 0} matches means that one or more matches are expected to be found
     * @param scroll
     *            {@code true} if scrolling should be performed
     * @return {@code true} if text string is found a given number of times and {@code false} if the text string is not found
     * 
     */

    public boolean searchText(String text, int minimumNumberOfMatches, boolean scroll) {
        return searcher.searchWithTimeoutFor(TextView.class, text, minimumNumberOfMatches, scroll, false);
    }

    /**
     * Searches for a text string and returns {@code true} if the searched text is found a given number of times.
     * 
     * @param text
     *            the text to search for. The parameter will be interpreted as a regular expression.
     * @param minimumNumberOfMatches
     *            the minimum number of matches expected to be found. {@code 0} matches means that one or more matches are expected to be found
     * @param scroll
     *            {@code true} if scrolling should be performed
     * @param onlyVisible
     *            {@code true} if only texts visible on the screen should be searched
     * @return {@code true} if text string is found a given number of times and {@code false} if the text string is not found
     * 
     */

    public boolean searchText(String text, int minimumNumberOfMatches, boolean scroll, boolean onlyVisible) {
        return searcher.searchWithTimeoutFor(TextView.class, text, minimumNumberOfMatches, scroll, onlyVisible);
    }

    public boolean searchText(String text, int minimumNumberOfMatches, boolean scroll, boolean onlyVisible, int timeout) {
        return searcher.searchWithTimeoutFor(TextView.class, text, minimumNumberOfMatches, scroll, onlyVisible, timeout);
    }

    public boolean searchTextAfter(String text, String after, int minimumNumberOfMatches, boolean scroll, boolean onlyVisible) {
        return searcher.searchWithTimeoutFor(TextView.class, text, after, minimumNumberOfMatches, scroll, onlyVisible);
    }

    /**
     * Sets the Orientation (Landscape/Portrait) for the current activity.
     * 
     * @param orientation
     *            the orientation to be set. <code>Solo.</code>{@link #LANDSCAPE} for landscape or <code>Solo.</code>{@link #PORTRAIT} for portrait.
     * 
     */

    public void setActivityOrientation(int orientation) {
        activityUtils.setActivityOrientation(orientation);
    }

    /**
     * Returns an ArrayList of all the opened/active activities.
     * 
     * @return an ArrayList of all the opened/active activities
     * 
     */

    public ArrayList<Activity> getAllOpenedActivities() {
        return activityUtils.getAllOpenedActivities();
    }

    /**
     * Returns the current Activity.
     * 
     * @return the current Activity
     * 
     */

    public Activity getCurrentActivity() {
        return activityUtils.getCurrentActivity();
    }

    /**
     * Asserts that the expected Activity is the currently active one.
     * 
     * @param message
     *            the message that should be displayed if the assert fails
     * @param name
     *            the name of the {@link Activity} that is expected to be active e.g. {@code "MyActivity"}
     * 
     */

    public void assertCurrentActivity(String message, String name) {
        asserter.assertCurrentActivity(message, name);
    }

    /**
     * Asserts that the expected Activity is the currently active one.
     * 
     * @param message
     *            the message that should be displayed if the assert fails
     * @param expectedClass
     *            the {@code Class} object that is expected to be active e.g. {@code MyActivity.class}
     * 
     */

    public void assertCurrentActivity(String message, Class<? extends Activity> expectedClass) {
        asserter.assertCurrentActivity(message, expectedClass);

    }

    /**
     * Asserts that the expected Activity is the currently active one, with the possibility to verify that the expected Activity is a new instance of the
     * Activity.
     * 
     * @param message
     *            the message that should be displayed if the assert fails
     * @param name
     *            the name of the activity that is expected to be active e.g. {@code "MyActivity"}
     * @param isNewInstance
     *            {@code true} if the expected {@link Activity} is a new instance of the {@link Activity}
     * 
     */

    public void assertCurrentActivity(String message, String name, boolean isNewInstance) {
        asserter.assertCurrentActivity(message, name, isNewInstance);
    }

    /**
     * Asserts that the expected Activity is the currently active one, with the possibility to verify that the expected Activity is a new instance of the
     * Activity.
     * 
     * @param message
     *            the message that should be displayed if the assert fails
     * @param expectedClass
     *            the {@code Class} object that is expected to be active e.g. {@code MyActivity.class}
     * @param isNewInstance
     *            {@code true} if the expected {@link Activity} is a new instance of the {@link Activity}
     * 
     */

    public void assertCurrentActivity(String message, Class<? extends Activity> expectedClass, boolean isNewInstance) {
        asserter.assertCurrentActivity(message, expectedClass, isNewInstance);
    }

    /**
     * Asserts that the available memory in the system is not low.
     * 
     */

    public void assertMemoryNotLow() {
        asserter.assertMemoryNotLow();
    }

    /**
     * Waits for a Dialog to close.
     * 
     * @param timeout
     *            the amount of time in milliseconds to wait
     * @return {@code true} if the {@link android.app.Dialog} is closed before the timeout and {@code false} if it is not closed
     * 
     */

    public boolean waitForDialogToClose(long timeout) {
        return dialogUtils.waitForDialogToClose(timeout);
    }

    /**
     * Simulates pressing the hardware back key.
     * 
     */

    public void goBack() {
        robotiumUtils.goBack();
    }

    /**
     * Clicks on a given coordinate on the screen.
     * 
     * @param x
     *            the x coordinate
     * @param y
     *            the y coordinate
     * 
     */

    public void clickOnScreen(float x, float y) {
        sleeper.sleep();
        clicker.clickOnScreen(x, y);
    }
    /**
     * Long clicks a given coordinate on the screen.
     * 
     * @param x
     *            the x coordinate
     * @param y
     *            the y coordinate
     * 
     */

    public void clickLongOnScreen(float x, float y) {
        clicker.clickLongOnScreen(x, y, 0);
    }

    /**
     * Long clicks a given coordinate on the screen for a given amount of time.
     * 
     * @param x
     *            the x coordinate
     * @param y
     *            the y coordinate
     * @param time
     *            the amount of time to long click
     * 
     */

    public void clickLongOnScreen(float x, float y, int time) {
        clicker.clickLongOnScreen(x, y, time);
    }

    /**
     * Click on any View with given text. Will scroll if necessary
     * 
     * @param name
     *            The name/text to search for. This <strong>will</strong> be interpreted as a regular expression
     */
    public void clickOn(String name) {
        clicker.clickOnAny(name, true);
    }

    /**
     * Click on any View with given text.
     * 
     * @param name
     *            The name/text to search for. This <strong>will</strong> be interpreted as a regular expression
     * @param scroll
     *            {@code true} if scrolling is allowed to search for View
     */
    public void clickOn(String name, boolean scroll) {
        clicker.clickOnAny(name, scroll);
    }

    /**
     * Clicks on a Button with a given text. Will automatically scroll when needed.
     * 
     * @param name
     *            the name of the {@link Button} presented to the user. The parameter will be interpreted as a regular expression
     * 
     */

    public void clickOnButton(String name) {
        clicker.clickOn(Button.class, name);

    }

    /**
     * Clicks on an ImageButton with a given index.
     * 
     * @param index
     *            the index of the {@link ImageButton} to be clicked. 0 if only one is available
     * 
     */

    public void clickOnImageButton(int index) {
        clicker.clickOn(ImageButton.class, index);
    }

    /**
     * Clicks on a ToggleButton with a given text.
     * 
     * @param name
     *            the name of the {@link ToggleButton} presented to the user. The parameter will be interpreted as a regular expression
     * 
     */

    public void clickOnToggleButton(String name) {
        clicker.clickOn(ToggleButton.class, name);
    }

    /**
     * Clicks on a MenuItem with a given text.
     * 
     * @param text
     *            the menu text that should be clicked on. The parameter will be interpreted as a regular expression
     * 
     */

    public void clickOnMenuItem(String text) {
        clicker.clickOnMenuItem(text);
    }

    /**
     * Clicks on a MenuItem with a given text.
     * 
     * @param text
     *            the menu text that should be clicked on. The parameter will be interpreted as a regular expression
     * @param subMenu
     *            true if the menu item could be located in a sub menu
     * 
     */

    public void clickOnMenuItem(String text, boolean subMenu) {
        clicker.clickOnMenuItem(text, subMenu);
    }

    /**
     * Presses a MenuItem with a given index. Index {@code 0} is the first item in the first row, Index {@code 3} is the first item in the second row and index
     * {@code 6} is the first item in the third row.
     * 
     * @param index
     *            the index of the {@link android.view.MenuItem} to be pressed
     * 
     */

    public void pressMenuItem(int index) {
        presser.pressMenuItem(index);
    }

    /**
     * Presses a MenuItem with a given index. Supports three rows with a given amount of items. If itemsPerRow equals 5 then index 0 is the first item in the
     * first row, index 5 is the first item in the second row and index 10 is the first item in the third row.
     * 
     * @param index
     *            the index of the {@link android.view.MenuItem} to be pressed
     * @param itemsPerRow
     *            the amount of menu items there are per row.
     * 
     */

    public void pressMenuItem(int index, int itemsPerRow) {
        presser.pressMenuItem(index, itemsPerRow);
    }

    /**
     * Presses on a Spinner (drop-down menu) item.
     * 
     * @param spinnerIndex
     *            the index of the {@link Spinner} menu to be used
     * @param itemIndex
     *            the index of the {@link Spinner} item to be pressed relative to the currently selected item A Negative number moves up on the {@link Spinner},
     *            positive moves down
     * 
     */

    public void pressSpinnerItem(int spinnerIndex, int itemIndex) {
        presser.pressSpinnerItem(spinnerIndex, itemIndex);
    }

    /**
     * Clicks on a given View.
     * 
     * @param view
     *            the {@link View} that should be clicked
     * 
     */

    public void clickOnView(View view) {
        waiter.waitForView(view, SMALLTIMEOUT);
        clicker.clickOnScreen(view);
    }

    /**
     * Long clicks on a given View.
     * 
     * @param view
     *            the {@link View} that should be long clicked
     * 
     */

    public void clickLongOnView(View view) {
        clicker.clickOnScreen(view, true, 0);

    }

    /**
     * Long clicks on a given View for a given amount of time.
     * 
     * @param view
     *            the {@link View} that should be long clicked
     * @param time
     *            the amount of time to long click
     * 
     */

    public void clickLongOnView(View view, int time) {
        clicker.clickOnScreen(view, true, time);

    }

    /**
     * Clicks on a View displaying a given text. Will automatically scroll when needed.
     * 
     * @param text
     *            the text that should be clicked. The parameter will be interpreted as a regular expression
     * 
     */

    public void clickOnText(String text) {
        clicker.clickOnText(text, false, 1, true, 0);
    }

    /**
     * Clicks on a View displaying a given text. Will automatically scroll when needed.
     * 
     * @param text
     *            the text that should be clicked. The parameter will be interpreted as a regular expression
     * @param match
     *            the match of the text that should be clicked
     * 
     */

    public void clickOnText(String text, int match) {
        clicker.clickOnText(text, false, match, true, 0);
    }

    /**
     * Clicks on a View displaying a given text.
     * 
     * @param text
     *            the text that should be clicked on. The parameter will be interpreted as a regular expression
     * @param match
     *            the match of the text that should be clicked
     * @param scroll
     *            true if scrolling should be performed
     * 
     */

    public void clickOnText(String text, int match, boolean scroll) {
        clicker.clickOnText(text, false, match, scroll, 0);
    }

    /**
     * Long clicks on a given View. Will automatically scroll when needed. {@link #clickOnText(String)} can then be used to click on the context menu items that
     * appear after the long click.
     * 
     * @param text
     *            the text that should be clicked. The parameter will be interpreted as a regular expression
     * 
     */

    public void clickLongOnText(String text) {
        clicker.clickOnText(text, true, 1, true, 0);
    }

    /**
     * Long clicks on a given View. Will automatically scroll when needed. {@link #clickOnText(String)} can then be used to click on the context menu items that
     * appear after the long click.
     * 
     * @param text
     *            the text that should be clicked. The parameter will be interpreted as a regular expression
     * @param match
     *            the match of the text that should be clicked
     * 
     */

    public void clickLongOnText(String text, int match) {
        clicker.clickOnText(text, true, match, true, 0);
    }

    /**
     * Long clicks on a given View. {@link #clickOnText(String)} can then be used to click on the context menu items that appear after the long click.
     * 
     * @param text
     *            the text that should be clicked. The parameter will be interpreted as a regular expression
     * @param match
     *            the match of the text that should be clicked
     * @param scroll
     *            true if scrolling should be performed
     * 
     */

    public void clickLongOnText(String text, int match, boolean scroll) {
        clicker.clickOnText(text, true, match, scroll, 0);
    }

    /**
     * Long clicks on a given View. {@link #clickOnText(String)} can then be used to click on the context menu items that appear after the long click.
     * 
     * @param text
     *            the text that should be clicked. The parameter will be interpreted as a regular expression
     * @param match
     *            the match of the text that should be clicked
     * @param time
     *            the amount of time to long click
     */

    public void clickLongOnText(String text, int match, int time) {
        clicker.clickOnText(text, true, match, true, time);
    }

    /**
     * Long clicks on a given View and then selects an item from the context menu that appears. Will automatically scroll when needed.
     * 
     * @param text
     *            the text to be clicked. The parameter will be interpreted as a regular expression
     * @param index
     *            the index of the menu item to be pressed. {@code 0} if only one is available
     * 
     */

    public void clickLongOnTextAndPress(String text, int index) {
        clicker.clickLongOnTextAndPress(text, index);
    }

    /**
     * Clicks on a Button with a given index.
     * 
     * @param index
     *            the index of the {@link Button} to be clicked. {@code 0} if only one is available
     * 
     */

    public void clickOnButton(int index) {
        clicker.clickOn(Button.class, index);
    }

    /**
     * Clicks on a RadioButton with a given index.
     * 
     * @param index
     *            the index of the {@link RadioButton} to be clicked. {@code 0} if only one is available
     * 
     */

    public void clickOnRadioButton(int index) {
        clicker.clickOn(RadioButton.class, index);
    }

    /**
     * Clicks on a CheckBox with a given index.
     * 
     * @param index
     *            the index of the {@link CheckBox} to be clicked. {@code 0} if only one is available
     * 
     */

    public void clickOnCheckBox(int index) {
        clicker.clickOn(CheckBox.class, index);
    }

    /**
     * Clicks on an EditText with a given index.
     * 
     * @param index
     *            the index of the {@link EditText} to be clicked. {@code 0} if only one is available
     * 
     */

    public void clickOnEditText(int index) {
        clicker.clickOn(EditText.class, index);
    }

    /**
     * Clicks on a given list line and returns an ArrayList of the TextView objects that the list line is showing. Will use the first list it finds.
     * 
     * @param line
     *            the line that should be clicked
     * @return an {@code ArrayList} of the {@link TextView} objects located in the list line
     * 
     */

    public ArrayList<TextView> clickInList(int line) {
        scrollToTop();
        return clicker.clickInList(line);
    }

    /**
     * Clicks on a given list line on a specified list and returns an ArrayList of the TextView objects that the list line is showing.
     * 
     * @param line
     *            the line that should be clicked
     * @param index
     *            the index of the list. 1 if two lists are available
     * @return an {@code ArrayList} of the {@link TextView} objects located in the list line
     * 
     */

    public ArrayList<TextView> clickInList(int line, int index) {
        scrollToTop();
        return clicker.clickInList(line, index, false, 0);
    }

    /**
     * Long clicks on a given list line and returns an ArrayList of the TextView objects that the list line is showing. Will use the first list it finds.
     * 
     * @param line
     *            the line that should be clicked
     * @return an {@code ArrayList} of the {@link TextView} objects located in the list line
     * 
     */
    public ArrayList<TextView> clickLongInList(int line) {
        scrollToTop();
        return clicker.clickInList(line, 0, true, 0);
    }

    /**
     * Long clicks on a given list line on a specified list and returns an ArrayList of the TextView objects that the list line is showing.
     * 
     * @param line
     *            the line that should be clicked
     * @param index
     *            the index of the list. 1 if two lists are available
     * @return an {@code ArrayList} of the {@link TextView} objects located in the list line
     * 
     */
    public ArrayList<TextView> clickLongInList(int line, int index) {
        scrollToTop();
        return clicker.clickInList(line, index, true, 0);
    }

    /**
     * Long clicks on a given list line on a specified list and returns an ArrayList of the TextView objects that the list line is showing.
     * 
     * @param line
     *            the line that should be clicked
     * @param index
     *            the index of the list. 1 if two lists are available
     * @param time
     *            the amount of time to long click
     * @return an {@code ArrayList} of the {@link TextView} objects located in the list line
     * 
     */
    public ArrayList<TextView> clickLongInList(int line, int index, int time) {
        scrollToTop();
        return clicker.clickInList(line, index, true, time);
    }

    /**
     * Simulate touching a given location and dragging it to a new location.
     * 
     * This method was copied from {@code TouchUtils.java} in the Android Open Source Project, and modified here.
     * 
     * @param fromX
     *            X coordinate of the initial touch, in screen coordinates
     * @param toX
     *            X coordinate of the drag destination, in screen coordinates
     * @param fromY
     *            X coordinate of the initial touch, in screen coordinates
     * @param toY
     *            Y coordinate of the drag destination, in screen coordinates
     * @param stepCount
     *            How many move steps to include in the drag
     * 
     */

    public void drag(float fromX, float toX, float fromY, float toY, int stepCount) {
        scroller.drag(fromX, toX, fromY, toY, stepCount);
    }

    /**
     * Simulate touching a given location and dragging it to a new location based on given steps. First element in list is starting point.
     * 
     * @param steps
     *            list of {@link Pair} that represents (x,y) coordinates to drag across
     */

    public void drag(final List<Pair<Float, Float>> steps) {
        scroller.drag(steps);
    }

    /**
     * Scrolls down the screen.
     * 
     * @return {@code true} if more scrolling can be done and {@code false} if it is at the end of the screen
     * 
     */

    public boolean scrollDown() {
        waiter.waitForViews(AbsListView.class, ScrollView.class);
        return scroller.scroll(Scroller.DOWN);
    }

    /**
     * Scrolls up the screen.
     * 
     * @return {@code true} if more scrolling can be done and {@code false} if it is at the top of the screen
     * 
     */

    public boolean scrollUp() {
        waiter.waitForViews(AbsListView.class, ScrollView.class);
        return scroller.scroll(Scroller.UP);
    }

    /**
     * Scroll to top of current view
     */
    public void scrollToTop() {
        scroller.scrollToTop();
    }

    /**
     * Scroll to bottom of current view
     */
    public void scrollToBottom() {
        scroller.scrollToBottom();
    }
    /**
     * Scrolls down a list with a given index.
     * 
     * @param index
     *            the {@link ListView} to be scrolled. {@code 0} if only one list is available
     * @return {@code true} if more scrolling can be done
     * 
     */

    public boolean scrollDownList(int index) {
        return scroller.scrollList(ListView.class, waiter.waitForAndGetView(index, ListView.class), Scroller.DOWN, null);
    }

    /**
     * Scrolls up a list with a given index.
     * 
     * @param index
     *            the {@link ListView} to be scrolled. {@code 0} if only one list is available
     * @return {@code true} if more scrolling can be done
     * 
     */

    public boolean scrollUpList(int index) {
        return scroller.scrollList(ListView.class, waiter.waitForAndGetView(index, ListView.class), Scroller.UP, null);
    }

    /**
     * Scrolls horizontally.
     * 
     * @param side
     *            the side to which to scroll; {@link #RIGHT} or {@link #LEFT}
     * 
     */

    public void scrollToSide(int side) {
        switch (side) {
            case RIGHT :
                scroller.scrollToSide(Scroller.Side.RIGHT);
                break;
            case LEFT :
                scroller.scrollToSide(Scroller.Side.LEFT);
                break;
        }
    }

    /**
     * Sets the date in a DatePicker with a given index.
     * 
     * @param index
     *            the index of the {@link DatePicker}. {@code 0} if only one is available
     * @param year
     *            the year e.g. 2011
     * @param monthOfYear
     *            the month which starts from zero e.g. 0 for January
     * @param dayOfMonth
     *            the day e.g. 10
     * 
     */

    public void setDatePicker(int index, int year, int monthOfYear, int dayOfMonth) {
        setDatePicker(waiter.waitForAndGetView(index, DatePicker.class), year, monthOfYear, dayOfMonth);
    }

    /**
     * Sets the date in a given DatePicker.
     * 
     * @param datePicker
     *            the {@link DatePicker} object.
     * @param year
     *            the year e.g. 2011
     * @param monthOfYear
     *            the month which starts from zero e.g. 03 for April
     * @param dayOfMonth
     *            the day e.g. 10
     * 
     */

    public void setDatePicker(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
        waiter.waitForView(datePicker, SMALLTIMEOUT);
        setter.setDatePicker(datePicker, year, monthOfYear, dayOfMonth);
    }

    /**
     * Sets the time in a TimePicker with a given index.
     * 
     * @param index
     *            the index of the {@link TimePicker}. {@code 0} if only one is available
     * @param hour
     *            the hour e.g. 15
     * @param minute
     *            the minute e.g. 30
     * 
     */

    public void setTimePicker(int index, int hour, int minute) {
        setTimePicker(waiter.waitForAndGetView(index, TimePicker.class), hour, minute);
    }

    /**
     * Sets the time in a given TimePicker.
     * 
     * @param timePicker
     *            the {@link TimePicker} object.
     * @param hour
     *            the hour e.g. 15
     * @param minute
     *            the minute e.g. 30
     * 
     */

    public void setTimePicker(TimePicker timePicker, int hour, int minute) {
        waiter.waitForView(timePicker, SMALLTIMEOUT);
        setter.setTimePicker(timePicker, hour, minute);
    }

    /**
     * Sets the progress of a ProgressBar with a given index. Examples are SeekBar and RatingBar.
     * 
     * @param index
     *            the index of the {@link ProgressBar}
     * @param progress
     *            the progress that the {@link ProgressBar} should be set to
     * 
     */

    public void setProgressBar(int index, int progress) {
        setProgressBar(waiter.waitForAndGetView(index, ProgressBar.class), progress);
    }

    /**
     * Sets the progress of a given ProgressBar. Examples are SeekBar and RatingBar.
     * 
     * @param progressBar
     *            the {@link ProgressBar}
     * @param progress
     *            the progress that the {@link ProgressBar} should be set to
     * 
     */

    public void setProgressBar(ProgressBar progressBar, int progress) {
        waiter.waitForView(progressBar, SMALLTIMEOUT);
        setter.setProgressBar(progressBar, progress);
    }

    /**
     * Sets the status of a SlidingDrawer with a given index. Examples are Solo.CLOSED and Solo.OPENED.
     * 
     * @param index
     *            the index of the {@link SlidingDrawer}
     * @param status
     *            the status that the {@link SlidingDrawer} should be set to
     * 
     */

    public void setSlidingDrawer(int index, int status) {
        setSlidingDrawer(waiter.waitForAndGetView(index, SlidingDrawer.class), status);
    }

    /**
     * Sets the status of a given SlidingDrawer. Examples are Solo.CLOSED and Solo.OPENED.
     * 
     * @param slidingDrawer
     *            the {@link SlidingDrawer}
     * @param status
     *            the status that the {@link SlidingDrawer} should be set to
     * 
     */

    public void setSlidingDrawer(SlidingDrawer slidingDrawer, int status) {
        waiter.waitForView(slidingDrawer, SMALLTIMEOUT);
        setter.setSlidingDrawer(slidingDrawer, status);
    }

    /**
     * Enters text in an EditText with a given index.
     * 
     * @param index
     *            the index of the {@link EditText}. {@code 0} if only one is available
     * @param text
     *            the text string to enter into the {@link EditText} field
     * 
     */

    public void enterText(int index, String text) {
        textEnterer.setEditText(waiter.waitForAndGetView(index, EditText.class), text);
    }

    /**
     * Enters text in a given EditText.
     * 
     * @param editText
     *            the {@link EditText} to enter text into
     * @param text
     *            the text string to enter into the {@link EditText} field
     * 
     */

    public void enterText(EditText editText, String text) {
        waiter.waitForView(editText, SMALLTIMEOUT);
        textEnterer.setEditText(editText, text);
    }

    /**
     * Types text in an EditText with a given index.
     * 
     * @param index
     *            the index of the {@link EditText}. {@code 0} if only one is available
     * @param text
     *            the text string to type in the {@link EditText} field
     * 
     */

    public void typeText(int index, String text) {
        textEnterer.typeText(waiter.waitForAndGetView(index, EditText.class), text);
    }

    /**
     * Types text in a given EditText.
     * 
     * @param editText
     *            the {@link EditText} to type text in
     * @param text
     *            the text string to type in the {@link EditText} field
     * 
     */

    public void typeText(EditText editText, String text) {
        waiter.waitForView(editText, SMALLTIMEOUT);
        textEnterer.typeText(editText, text);
    }

    /**
     * Clears the value of an EditText.
     * 
     * @param index
     *            the index of the {@link EditText} that should be cleared. 0 if only one is available
     * 
     */

    public void clearEditText(int index) {
        textEnterer.setEditText(waiter.waitForAndGetView(index, EditText.class), "");
    }

    /**
     * Clears the value of an EditText.
     * 
     * @param editText
     *            the {@link EditText} that should be cleared
     * 
     */

    public void clearEditText(EditText editText) {
        waiter.waitForView(editText, SMALLTIMEOUT);
        textEnterer.setEditText(editText, "");
    }

    /**
     * Clicks on an ImageView with a given index.
     * 
     * @param index
     *            the index of the {@link ImageView} to be clicked. {@code 0} if only one is available
     * 
     */

    public void clickOnImage(int index) {
        clicker.clickOn(ImageView.class, index);
    }

    /**
     * Returns an EditText with a given index.
     * 
     * @param index
     *            the index of the {@link EditText}. {@code 0} if only one is available
     * @return the {@link EditText} with a specified index or {@code null} if index is invalid
     * 
     */

    public EditText getEditText(int index) {
        return getter.getView(EditText.class, index);
    }

    /**
     * Returns a Button with a given index.
     * 
     * @param index
     *            the index of the {@link Button}. {@code 0} if only one is available
     * @return the {@link Button} with a specified index or {@code null} if index is invalid
     * 
     */

    public Button getButton(int index) {
        return getter.getView(Button.class, index);
    }

    /**
     * Returns a TextView with a given index.
     * 
     * @param index
     *            the index of the {@link TextView}. {@code 0} if only one is available
     * @return the {@link TextView} with a specified index or {@code null} if index is invalid
     * 
     */

    public TextView getText(int index) {
        return getter.getView(TextView.class, index);
    }

    /**
     * Returns an ImageView with a given index.
     * 
     * @param index
     *            the index of the {@link ImageView}. {@code 0} if only one is available
     * @return the {@link ImageView} with a specified index or {@code null} if index is invalid
     * 
     */

    public ImageView getImage(int index) {
        return getter.getView(ImageView.class, index);
    }

    /**
     * Returns an ImageButton with a given index.
     * 
     * @param index
     *            the index of the {@link ImageButton}. {@code 0} if only one is available
     * @return the {@link ImageButton} with a specified index or {@code null} if index is invalid
     * 
     */

    public ImageButton getImageButton(int index) {
        return getter.getView(ImageButton.class, index);
    }

    /**
     * Returns a TextView which shows a given text.
     * 
     * @param text
     *            the text that is shown
     * @return the {@link TextView} that shows the given text
     */

    public TextView getText(String text) {
        return getter.getView(TextView.class, text, false);
    }

    /**
     * Returns a TextView which shows a given text.
     * 
     * @param text
     *            the text that is shown
     * @param onlyVisible
     *            {@code true} if only visible texts on the screen should be returned
     * @return the {@link TextView} that shows the given text
     */

    public TextView getText(String text, boolean onlyVisible) {
        return getter.getView(TextView.class, text, onlyVisible);
    }

    /**
     * Returns a TextView which shows a given text.
     * 
     * @param text
     *            the text that is shown
     * @param onlyVisible
     *            {@code true} if only visible texts on the screen should be returned
     * @return the {@link TextView} that shows the given text
     */

    public TextView getText(String text, boolean onlyVisible, boolean useRegex) {
        return getter.getView(TextView.class, text, onlyVisible, useRegex);
    }

    /**
     * Returns a TextView which shows a given text.
     * 
     * @param text
     *            the text that is shown
     * @param onlyVisible
     *            {@code true} if only visible texts on the screen should be returned
     * @param match
     *            which occurrence should be returned (1 for first occurrence)
     * @return the {@link TextView} that shows the given text
     */

    public TextView getText(String text, boolean onlyVisible, boolean useRegex, int match) {
        return getter.getView(TextView.class, text, onlyVisible, useRegex, match);
    }

    /**
     * Returns a TextView which shows a given text that appears after {@code String}.
     * 
     * @param text
     *            the text that is shown
     * @param onlyVisible
     *            {@code true} if only visible texts on the screen should be returned
     * @param match
     *            which occurrence should be returned (1 for first occurrence)
     * @param searchAfter
     *            the text where the search should start. {@code null} to start from beginning
     * @return the {@link TextView} that shows the given text
     */
    public TextView getText(String text, boolean onlyVisible, boolean useRegex, int match, String searchAfter) {
        return getter.getView(TextView.class, text, onlyVisible, useRegex, match, searchAfter);
    }

    /**
     * Returns a Button which shows a given text.
     * 
     * @param text
     *            the text that is shown
     * @return the {@link Button} that shows the given text
     */

    public Button getButton(String text) {
        return getter.getView(Button.class, text, false);
    }

    /**
     * Returns a Button which shows a given text.
     * 
     * @param text
     *            the text that is shown
     * @param onlyVisible
     *            {@code true} if only visible buttons on the screen should be returned
     * @return the {@link Button} that shows the given text
     */

    public Button getButton(String text, boolean onlyVisible) {
        return getter.getView(Button.class, text, onlyVisible);
    }

    /**
     * Returns an EditText which shows a given text.
     * 
     * @param text
     *            the text that is shown
     * @return the {@link EditText} which shows the given text
     */

    public EditText getEditText(String text) {
        return getter.getView(EditText.class, text, false);
    }

    /**
     * Returns an EditText which shows a given text.
     * 
     * @param text
     *            the text that is shown
     * @param onlyVisible
     *            {@code true} if only visible EditTexts on the screen should be returned
     * @return the {@link EditText} which shows the given text
     */

    public EditText getEditText(String text, boolean onlyVisible) {
        return getter.getView(EditText.class, text, onlyVisible);
    }

    /**
     * Returns a View with a given id.
     * 
     * @param id
     *            the R.id of the {@link View} to be returned
     * @return a {@link View} with a given id
     */

    public View getView(int id) {
        return getter.getView(id);
    }

    /**
     * Returns a View of a given class and index.
     * 
     * @param viewClass
     *            the class of the requested view
     * @param index
     *            the index of the {@link View}. {@code 0} if only one is available
     * @return a {@link View} with a given class and index
     */

    public <T extends View> T getView(Class<T> viewClass, int index) {
        return waiter.waitForAndGetView(index, viewClass);
    }

    /**
     * Returns an ArrayList of the View objects currently shown in the focused Activity or Dialog.
     * 
     * @return an {@code ArrayList} of the {@link View} objects currently shown in the focused window
     * 
     */

    public ArrayList<View> getCurrentViews() {
        return viewFetcher.getViews(null, true);
    }

    /**
     * Returns an ArrayList of the ImageView objects currently shown in the focused Activity or Dialog.
     * 
     * @return an {@code ArrayList} of the {@link ImageView} objects currently shown in the focused window
     * 
     */

    public ArrayList<ImageView> getCurrentImageViews() {
        return viewFetcher.getCurrentViews(ImageView.class);
    }

    /**
     * Returns an ArrayList of the EditText objects currently shown in the focused Activity or Dialog.
     * 
     * @return an {@code ArrayList} of the {@link EditText} objects currently shown in the focused window
     * 
     */

    public ArrayList<EditText> getCurrentEditTexts() {
        return viewFetcher.getCurrentViews(EditText.class);
    }

    /**
     * Returns an ArrayList of the ListView objects currently shown in the focused Activity or Dialog.
     * 
     * @return an {@code ArrayList} of the {@link ListView} objects currently shown in the focused window
     * 
     */

    public ArrayList<ListView> getCurrentListViews() {
        return viewFetcher.getCurrentViews(ListView.class);
    }

    /**
     * Returns an ArrayList of the ScrollView objects currently shown in the focused Activity or Dialog.
     * 
     * @return an {@code ArrayList} of the {@link ScrollView} objects currently shown in the focused window
     * 
     */

    public ArrayList<ScrollView> getCurrentScrollViews() {
        return viewFetcher.getCurrentViews(ScrollView.class);
    }

    /**
     * Returns an ArrayList of the Spinner objects (drop-down menus) currently shown in the focused Activity or Dialog.
     * 
     * @return an {@code ArrayList} of the {@link Spinner} objects (drop-down menus) currently shown in the focused window
     * 
     */

    public ArrayList<Spinner> getCurrentSpinners() {
        return viewFetcher.getCurrentViews(Spinner.class);
    }

    /**
     * Returns an ArrayList of the TextView objects currently shown in the focused Activity or Dialog.
     * 
     * @param parent
     *            the parent {@link View} from which the {@link TextView} objects should be returned. {@code null} if all TextView objects from the currently
     *            focused window e.g. Activity should be returned
     * 
     * @return an {@code ArrayList} of the {@link TextView} objects currently shown in the focused window
     * 
     */
    public ArrayList<TextView> getCurrentTextViews() {
        return viewFetcher.getCurrentViews(TextView.class, null);
    }

    /**
     * Returns an ArrayList of the TextView objects currently shown in the focused Activity or Dialog.
     * 
     * @param parent
     *            the parent {@link View} from which the {@link TextView} objects should be returned. {@code null} if all TextView objects from the currently
     *            focused window e.g. Activity should be returned
     * 
     * @return an {@code ArrayList} of the {@link TextView} objects currently shown in the focused window
     * 
     */

    public ArrayList<TextView> getCurrentTextViews(View parent) {
        return viewFetcher.getCurrentViews(TextView.class, parent);
    }

    /**
     * Returns an ArrayList of the GridView objects currently shown in the focused Activity or Dialog.
     * 
     * @return an {@code ArrayList} of the {@link GridView} objects currently shown in the focused window
     * 
     */

    public ArrayList<GridView> getCurrentGridViews() {
        return viewFetcher.getCurrentViews(GridView.class);
    }

    /**
     * Returns an ArrayList of the Button objects currently shown in the focused Activity or Dialog.
     * 
     * @return an {@code ArrayList} of the {@link Button} objects currently shown in the focused window
     * 
     */

    public ArrayList<Button> getCurrentButtons() {
        return viewFetcher.getCurrentViews(Button.class);
    }

    /**
     * Returns an ArrayList of the ToggleButton objects currently shown in the focused Activity or Dialog.
     * 
     * @return an {@code ArrayList} of the {@link ToggleButton} objects currently shown in the focused window
     * 
     */

    public ArrayList<ToggleButton> getCurrentToggleButtons() {
        return viewFetcher.getCurrentViews(ToggleButton.class);
    }

    /**
     * Returns an ArrayList of the RadioButton objects currently shown in the focused Activity or Dialog.
     * 
     * @return an {@code ArrayList} of the {@link RadioButton} objects currently shown in the focused window
     * 
     */

    public ArrayList<RadioButton> getCurrentRadioButtons() {
        return viewFetcher.getCurrentViews(RadioButton.class);
    }

    /**
     * Returns an ArrayList of the CheckBox objects currently shown in the focused Activity or Dialog.
     * 
     * @return an {@code ArrayList} of the {@link CheckBox} objects currently shown in the focused window
     * 
     */

    public ArrayList<CheckBox> getCurrentCheckBoxes() {
        return viewFetcher.getCurrentViews(CheckBox.class);
    }

    /**
     * Returns an ArrayList of the ImageButton objects currently shown in the focused Activity or Dialog.
     * 
     * @return an {@code ArrayList} of the {@link ImageButton} objects currently shown in the focused window
     * 
     */

    public ArrayList<ImageButton> getCurrentImageButtons() {
        return viewFetcher.getCurrentViews(ImageButton.class);
    }

    /**
     * Returns an ArrayList of the DatePicker objects currently shown in the focused Activity or Dialog.
     * 
     * @return an {@code ArrayList} of the {@link DatePicker} objects currently shown in the focused window
     * 
     */

    public ArrayList<DatePicker> getCurrentDatePickers() {
        return viewFetcher.getCurrentViews(DatePicker.class);
    }

    /**
     * Returns an ArrayList of the TimePicker objects currently shown in the focused Activity or Dialog.
     * 
     * @return an {@code ArrayList} of the {@link TimePicker} objects currently shown in the focused window
     * 
     */

    public ArrayList<TimePicker> getCurrentTimePickers() {
        return viewFetcher.getCurrentViews(TimePicker.class);
    }

    /**
     * Returns an ArrayList of the SlidingDrawer objects currently shown in the focused Activity or Dialog.
     * 
     * @return an {@code ArrayList} of the {@link SlidingDrawer} objects currently shown in the focused window
     * 
     */

    public ArrayList<SlidingDrawer> getCurrentSlidingDrawers() {
        return viewFetcher.getCurrentViews(SlidingDrawer.class);
    }

    /**
     * Returns an ArrayList of the ProgressBar objects currently shown in the focused Activity or Dialog.
     * 
     * @return an {@code ArrayList} of the {@link ProgressBar} objects currently shown in the focused window
     * 
     */

    public ArrayList<ProgressBar> getCurrentProgressBars() {
        return viewFetcher.getCurrentViews(ProgressBar.class);
    }

    /**
     * Checks if a RadioButton with a given index is checked.
     * 
     * @param index
     *            of the {@link RadioButton} to check. {@code 0} if only one is available
     * @return {@code true} if {@link RadioButton} is checked and {@code false} if it is not checked
     * 
     */

    public boolean isRadioButtonChecked(int index) {
        return checker.isButtonChecked(RadioButton.class, index);
    }

    /**
     * Checks if a RadioButton with a given text is checked.
     * 
     * @param text
     *            the text that the {@link RadioButton} shows
     * @return {@code true} if a {@link RadioButton} with the given text is checked and {@code false} if it is not checked
     * 
     */

    public boolean isRadioButtonChecked(String text) {
        return checker.isButtonChecked(RadioButton.class, text);
    }

    /**
     * Checks if a CheckBox with a given index is checked.
     * 
     * @param index
     *            of the {@link CheckBox} to check. {@code 0} if only one is available
     * @return {@code true} if {@link CheckBox} is checked and {@code false} if it is not checked
     * 
     */

    public boolean isCheckBoxChecked(int index) {
        return checker.isButtonChecked(CheckBox.class, index);
    }

    /**
     * Checks if a ToggleButton with a given text is checked.
     * 
     * @param text
     *            the text that the {@link ToggleButton} shows
     * @return {@code true} if a {@link ToggleButton} with the given text is checked and {@code false} if it is not checked
     * 
     */

    public boolean isToggleButtonChecked(String text) {
        return checker.isButtonChecked(ToggleButton.class, text);
    }

    /**
     * Checks if a ToggleButton with a given index is checked.
     * 
     * @param index
     *            of the {@link ToggleButton} to check. {@code 0} if only one is available
     * @return {@code true} if {@link ToggleButton} is checked and {@code false} if it is not checked
     * 
     */

    public boolean isToggleButtonChecked(int index) {
        return checker.isButtonChecked(ToggleButton.class, index);
    }

    /**
     * Checks if a CheckBox with a given text is checked.
     * 
     * @param text
     *            the text that the {@link CheckBox} shows
     * @return {@code true} if a {@link CheckBox} with the given text is checked and {@code false} if it is not checked
     * 
     */

    public boolean isCheckBoxChecked(String text) {
        return checker.isButtonChecked(CheckBox.class, text);
    }

    /**
     * Checks if the given text is checked.
     * 
     * @param text
     *            the text that the {@link CheckedTextView} or {@link CompoundButton} objects show
     * @return {@code true} if the given text is checked and {@code false} if it is not checked
     */

    public boolean isTextChecked(String text) {
        waiter.waitForViews(CheckedTextView.class, CompoundButton.class);

        if (viewFetcher.getCurrentViews(CheckedTextView.class).size() > 0 && checker.isCheckedTextChecked(text))
            return true;

        if (viewFetcher.getCurrentViews(CompoundButton.class).size() > 0 && checker.isButtonChecked(CompoundButton.class, text))
            return true;

        return false;
    }

    /**
     * Checks if a given text is selected in any Spinner located in the current screen.
     * 
     * @param text
     *            the text that is expected to be selected
     * @return {@code true} if the given text is selected in any {@link Spinner} and false if it is not
     * 
     */

    public boolean isSpinnerTextSelected(String text) {
        return checker.isSpinnerTextSelected(text);
    }

    /**
     * Checks if a given text is selected in a given Spinner.
     * 
     * @param index
     *            the index of the spinner to check. {@code 0} if only one spinner is available
     * @param text
     *            the text that is expected to be selected
     * @return true if the given text is selected in the given {@link Spinner} and false if it is not
     */

    public boolean isSpinnerTextSelected(int index, String text) {
        return checker.isSpinnerTextSelected(index, text);
    }

    /**
     * Sends a key: Right, Left, Up, Down, Enter, Menu or Delete.
     * 
     * @param key
     *            the key to be sent. Use {@code Solo.}{@link #RIGHT}, {@link #LEFT}, {@link #UP}, {@link #DOWN}, {@link #ENTER}, {@link #MENU}, {@link #DELETE}
     * 
     */

    public void sendKey(int key) {
        switch (key) {
            case RIGHT :
                robotiumUtils.sendKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT);
                break;
            case LEFT :
                robotiumUtils.sendKeyCode(KeyEvent.KEYCODE_DPAD_LEFT);
                break;
            case UP :
                robotiumUtils.sendKeyCode(KeyEvent.KEYCODE_DPAD_UP);
                break;
            case DOWN :
                robotiumUtils.sendKeyCode(KeyEvent.KEYCODE_DPAD_DOWN);
                break;
            case ENTER :
                robotiumUtils.sendKeyCode(KeyEvent.KEYCODE_ENTER);
                break;
            case MENU :
                robotiumUtils.sendKeyCode(KeyEvent.KEYCODE_MENU);
                break;
            case DELETE :
                robotiumUtils.sendKeyCode(KeyEvent.KEYCODE_DEL);
                break;
            default :
                robotiumUtils.sendKeyCode(key);
                break;
        }
    }

    /**
     * Returns to the given Activity.
     * 
     * @param name
     *            the name of the {@link Activity} to return to, e.g. {@code "MyActivity"}
     * 
     */

    public void goBackToActivity(String name) {
        activityUtils.goBackToActivity(name);
    }

    /**
     * Waits for the given Activity. Default timeout is 20 seconds.
     * 
     * @param name
     *            the name of the {@code Activity} to wait for e.g. {@code "MyActivity"}
     * @return {@code true} if {@code Activity} appears before the timeout and {@code false} if it does not
     * 
     */

    public boolean waitForActivity(String name) {
        return waiter.waitForActivity(name, TIMEOUT);
    }

    /**
     * Waits for the given Activity.
     * 
     * @param name
     *            the name of the {@link Activity} to wait for e.g. {@code "MyActivity"}
     * @param timeout
     *            the amount of time in milliseconds to wait
     * @return {@code true} if {@link Activity} appears before the timeout and {@code false} if it does not
     * 
     */

    public boolean waitForActivity(String name, int timeout) {
        return waiter.waitForActivity(name, timeout);
    }

    /**
     * Returns a localized string.
     * 
     * @param resId
     *            the resource ID for the string
     * @return the localized string
     * 
     */

    public String getString(int resId) {
        return activityUtils.getString(resId);
    }

    /**
     * Robotium will sleep for a specified time.
     * 
     * @param time
     *            the time in milliseconds that Robotium should sleep
     * 
     */

    public void sleep(int time) {
        sleeper.sleep(time);
    }

    /**
     * 
     * Finalizes the solo object and removes the ActivityMonitor.
     * 
     * @see #finishOpenedActivities() finishOpenedActivities() to close the activities that have been active.
     * 
     */

    public void finalize() throws Throwable {
        activityUtils.finalize();
    }

    /**
     * All inactive activities are finished.
     * 
     */

    public void finishInactiveActivities() {
        activityUtils.finishInactiveActivities();
    }

    /**
     * 
     * All activities that have been active are finished.
     * 
     */

    public void finishOpenedActivities() {
        activityUtils.finishOpenedActivities();
    }

    /**
     * Ensures that the {@code View} is visible and not null
     * 
     * @param id
     *            The id that represents the view to find
     */
    public void ensureVisible(int id) {

    }

    /**
     * Returns an ArrayList of the all unique View objects contained in the parent View. Scrolling is required to get all the views. This is not a cheap call.
     * 
     * @return an {@code ArrayList} of the {@link View} objects contained in the given {@code View}
     * 
     */
    public final ArrayList<View> getAllViews() {
        return viewFetcher.getAllViews(null);
    }

    /**
     * Returns an ArrayList of the all unique View objects contained in the parent View. Scrolling is required to get all the views. This is not a cheap call.
     * 
     * @param parent
     *            the parent view from which to return the views
     * @return an {@code ArrayList} of the {@link View} objects contained in the given {@code View}
     * 
     */
    public final ArrayList<View> getAllViews(View parent) {
        return viewFetcher.getAllViews(parent);
    }

    public final <T extends View> ArrayList<T> getAllViews(Class<T> viewClass) {
        return viewFetcher.getAllViews(viewClass, null);
    }

    public final <T extends View> ArrayList<T> getAllViews(Class<T> viewClass, View parent) {
        return viewFetcher.getAllViews(viewClass, parent);
    }

    public final ArrayList<ListView> getAllListViews() {
        return viewFetcher.getAllViews(ListView.class, null);
    }

    public void clickOnUnattachedButton(String name, int locationOfText) {
        clicker.clickOnUnattached(Button.class, name, locationOfText);
    }

    public void clickOnUnattachedImageButton(String name, int locationOfText) {
        clicker.clickOnUnattached(ImageButton.class, name, locationOfText);
    }

    public void clickOnUnattachedToggleButton(String name, int locationOfText) {
        clicker.clickOnUnattached(ToggleButton.class, name, locationOfText);
    }

    /**
     * Gets cache of the test project. i.e this project
     * 
     * @return Returns the cache of the local project
     */
    public final File getTestCache() {
        return extUtils.getTestCache();
    }

    /**
     * Gets the cache of the project that is being tested on. e.g. Scramble
     * 
     * @return Cache directory of remote project
     */
    public final File getActivityCache() {
        return extUtils.getActivityCache();
    }

    /**
     * Returns the screenshots path where all screenshots should be stored
     * 
     * @return screenshots path
     */
    public final String getScreenshotsPath() {
        return extUtils.getScreenshotsPath();
    }

    /**
     * Take screenshot of the current screen
     * 
     * @return
     */
    public final Bitmap takeScreenShot() {
        return extUtils.takeScreenShot();
    }

    /**
     * Take screenshot of {@code activity}
     * 
     * @param activity
     *            Activity to take screenshot of
     * @return Bitmap representing the screenshot
     */
    public Bitmap takeScreenShot(final Activity activity) {
        return extUtils.takeScreenShot(activity);
    }

    /**
     * Take screenshot of {@code view}
     * 
     * @param view
     *            View to take screenshot of
     * @return Bitmap representing the screenshot
     */
    public final Bitmap takeScreenShot(final View view) {
        return extUtils.takeScreenShot(view);
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
        extUtils.saveDrawable(drawable, name);
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
        extUtils.saveBitmap(bitmap, name);
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
        return extUtils.takeScreenShotAndSave(activity, name);
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
        return extUtils.takeScreenShotAndSave(view, name);
    }

    /**
     * Function that fails the current test and logs it in logcat and takes screenshot
     * 
     * @param msg
     *            Meaningful message to describe what is expected and what happened
     */
    public void fail(String msg) {
        extUtils.fail(msg, true, "fail");
    }

    /**
     * Function that fails the current test and logs it in logcat with option to take screenshot
     * 
     * @param msg
     *            Meaningful message to describe what is expected and what happened
     * @param screenshot
     *            {@code true} if screenshot should be taken
     */
    public void fail(String msg, boolean screenshot) {
        extUtils.fail(msg, screenshot, "fail");
    }

    /**
     * Function that fails the current test and logs it in logcat and takes screenshot
     * 
     * @param msg
     *            Meaningful message to describe what is expected and what happened
     * @param name
     *            name of the screenshot
     */
    public void fail(String msg, String name) {
        extUtils.fail(msg, true, name);
    }

    /**
     * Function that fails the current test and logs it in logcat with option to take screenshot
     * 
     * @param msg
     *            Meaningful message to describe what is expected and what happened
     * @param screenshot
     *            {@code true} if screenshot should be taken
     * @param name
     *            name of filename of screenshot if screenshot is selected
     */

    public void fail(String msg, boolean screenshot, String name) {
        extUtils.fail(msg, screenshot, name);
    }
}
