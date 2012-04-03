package com.jayway.android.robotium.solo;

import java.util.ArrayList;
import java.util.List;

import android.app.Instrumentation;
import android.os.SystemClock;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.ScrollView;

/**
 * Contains scroll methods. Examples are scrollDown(), scrollUpList(), scrollToSide().
 * 
 * @author Renas Reda, renas.reda@jayway.com
 * @modified Michael Shi, mshi@zynga.com
 * 
 */

class Scroller {

    public enum Direction {
        UP,
        DOWN
    }
    public static final int DOWN = 0;
    public static final int UP = 1;
    public enum Side {
        LEFT,
        RIGHT
    }
    private final Instrumentation inst;
    private final ActivityUtils activityUtils;
    private final ViewFetcher viewFetcher;
    private final Sleeper sleeper;

    /**
     * Constructs this object.
     * 
     * @param inst
     *            the {@code Instrumentation} instance.
     * @param activityUtils
     *            the {@code ActivityUtils} instance.
     * @param viewFetcher
     *            the {@code ViewFetcher} instance.
     * @param sleeper
     *            the {@code Sleeper} instance
     */

    public Scroller(Instrumentation inst, ActivityUtils activityUtils, ViewFetcher viewFetcher, Sleeper sleeper) {
        this.inst = inst;
        this.activityUtils = activityUtils;
        this.viewFetcher = viewFetcher;
        this.sleeper = sleeper;
    }

    /**
     * Simulate touching a given location and dragging it to a new location based on given steps. First element in list is starting point.
     * 
     * @param steps
     *            list of {@link Pair} that represents (x,y) coordinates to drag across
     */

    public void drag(final List<Pair<Float, Float>> steps) {
        final long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();

        final Pair<Float, Float> start = steps.get(0);
        final Pair<Float, Float> end = steps.get(steps.size() - 1);

        MotionEvent event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, start.first, start.second, 0);
        inst.sendPointerSync(event);
        inst.waitForIdleSync();

        for (Pair<Float, Float> coord : steps) {
            eventTime = SystemClock.uptimeMillis();
            event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, coord.first, coord.second, 0);
            inst.sendPointerSync(event);
            inst.waitForIdleSync();
        }

        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, end.first, end.second, 0);
        inst.sendPointerSync(event);
        inst.waitForIdleSync();

    }

    /**
     * Simulate touching a specific location and dragging to a new location.
     * 
     * This method was copied from {@code TouchUtils.java} in the Android Open Source Project, and modified here.
     * 
     * @param fromX
     *            X coordinate of the initial touch, in screen coordinates
     * @param toX
     *            Xcoordinate of the drag destination, in screen coordinates
     * @param fromY
     *            X coordinate of the initial touch, in screen coordinates
     * @param toY
     *            Y coordinate of the drag destination, in screen coordinates
     * @param stepCount
     *            How many move steps to include in the drag
     * 
     */

    public void drag(float fromX, float toX, float fromY, float toY, int stepCount) {
        final List<Pair<Float, Float>> steps = new ArrayList<Pair<Float, Float>>(stepCount + 2);
        float y = fromY;
        float x = fromX;

        float yStep = (toY - fromY) / stepCount;
        float xStep = (toX - fromX) / stepCount;

        steps.add(Pair.create(x, y));

        for (int i = 0; i < stepCount; ++i) {
            y += yStep;
            x += xStep;
            steps.add(Pair.create(x, y));
        }
        steps.add(Pair.create(x, y));
        drag(steps);
    }

    /**
     * Scrolls a ScrollView.
     * 
     * @param direction
     *            the direction to be scrolled
     * @return {@code true} if more scrolling can be done
     * 
     */

    private boolean scrollScrollView(int direction, ArrayList<ScrollView> scrollViews) {
        final ScrollView scroll = viewFetcher.getView(ScrollView.class, scrollViews);
        int scrollAmount = 0;

        if (scroll != null) {
            int height = scroll.getHeight();
            height--;
            int scrollTo = 0;

            if (direction == DOWN) {
                scrollTo = (height);
            }

            else if (direction == UP) {
                scrollTo = (-height);
            }
            scrollAmount = scroll.getScrollY();
            scrollScrollViewTo(scroll, 0, scrollTo);
            if (scrollAmount == scroll.getScrollY()) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * Scroll the list to a given line
     * 
     * @param listView
     *            the listView to scroll
     * @param line
     *            the line to scroll to
     */

    private void scrollScrollViewTo(final ScrollView scrollView, final int x, final int y) {
        inst.runOnMainSync(new Runnable() {
            public void run() {
                scrollView.scrollBy(x, y);
            }
        });
    }

    /**
     * Scrolls up and down.
     * 
     * @param direction
     *            the direction in which to scroll
     * @return {@code true} if more scrolling can be done
     * 
     */

    public boolean scroll(int direction) {
        final ArrayList<View> viewList = RobotiumUtils.removeInvisibleViews(viewFetcher.getViews(null, true));
        final ArrayList<ListView> listViews = RobotiumUtils.filterViews(ListView.class, viewList);

        if (listViews.size() > 0) {
            return scrollList(ListView.class, null, direction, listViews);
        }

        final ArrayList<GridView> gridViews = RobotiumUtils.filterViews(GridView.class, viewList);

        if (gridViews.size() > 0) {
            return scrollList(GridView.class, null, direction, gridViews);
        }

        final ArrayList<ScrollView> scrollViews = RobotiumUtils.filterViews(ScrollView.class, viewList);

        if (scrollViews.size() > 0) {
            return scrollScrollView(direction, scrollViews);
        }
        return false;
    }

    /**
     * Scrolls to top of the current view
     */
    public void scrollToTop() {
        while (scroll(UP)) {
            sleeper.sleep(Constants.SPIN_WAIT);
        }
    }

    /**
     * Scrolls to bottom of the current view
     */
    public void scrollToBottom() {
        while (scroll(DOWN)) {
            sleeper.sleep(Constants.SPIN_WAIT);
        }
    }

    /**
     * Scrolls a list.
     * 
     * @param listIndex
     *            the list to be scrolled
     * @param direction
     *            the direction to be scrolled
     * @return {@code true} if more scrolling can be done
     * 
     */

    public <T extends AbsListView> boolean scrollList(Class<T> classToFilterBy, T absListView, int direction, ArrayList<T> listViews) {

        if (absListView == null)
            absListView = (T) viewFetcher.getView(classToFilterBy, listViews);

        if (absListView == null)
            return false;

        if (direction == DOWN) {
            if (absListView.getLastVisiblePosition() >= absListView.getCount() - 1) {
                scrollListToLine(absListView, absListView.getLastVisiblePosition());
                return false;
            }

            if (absListView.getFirstVisiblePosition() != absListView.getLastVisiblePosition())
                scrollListToLine(absListView, absListView.getLastVisiblePosition());

            else
                scrollListToLine(absListView, absListView.getFirstVisiblePosition() + 1);

        } else if (direction == UP) {
            if (absListView.getFirstVisiblePosition() < 2) {
                scrollListToLine(absListView, 0);
                return false;
            }

            final int lines = absListView.getLastVisiblePosition() - absListView.getFirstVisiblePosition();
            int lineToScrollTo = absListView.getFirstVisiblePosition() - lines;

            if (lineToScrollTo == absListView.getLastVisiblePosition())
                lineToScrollTo--;

            if (lineToScrollTo < 0)
                lineToScrollTo = 0;

            scrollListToLine(absListView, lineToScrollTo);
        }
        sleeper.sleep();
        return true;
    }

    /**
     * Scroll the list to a given line
     * 
     * @param listView
     *            the listView to scroll
     * @param line
     *            the line to scroll to
     */

    private <T extends AbsListView> void scrollListToLine(final T view, final int line) {

        final int lineToMoveTo;
        if (view instanceof GridView)
            lineToMoveTo = line + 1;
        else
            lineToMoveTo = line;

        inst.runOnMainSync(new Runnable() {
            public void run() {
                view.setSelection(lineToMoveTo);
            }
        });
    }

    /**
     * Scrolls horizontally.
     * 
     * @param side
     *            the side to which to scroll; {@link Side#RIGHT} or {@link Side#LEFT}
     * 
     */

    public void scrollToSide(Side side) {
        int screenHeight = activityUtils.getCurrentActivity().getWindowManager().getDefaultDisplay().getHeight();
        int screenWidth = activityUtils.getCurrentActivity(false).getWindowManager().getDefaultDisplay().getWidth();
        float x = screenWidth / 2.0f;
        float y = screenHeight / 2.0f;
        if (side == Side.LEFT)
            drag(0, x, y, y, 40);
        else if (side == Side.RIGHT)
            drag(x, 0, y, y, 40);
    }

}
