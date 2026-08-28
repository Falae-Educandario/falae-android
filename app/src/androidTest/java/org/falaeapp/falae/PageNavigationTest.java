package org.falaeapp.falae;

import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.viewpager.widget.ViewPager;
import org.falaeapp.falae.activity.DisplayActivity;
import org.falaeapp.falae.model.Category;
import org.falaeapp.falae.model.Item;
import org.falaeapp.falae.model.Page;
import org.falaeapp.falae.model.SpreadSheet;
import org.falaeapp.falae.viewmodel.DisplayViewModel;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PageNavigationTest {
    @Test
    public void followingLinkAndReturningRestoresOriginalPageAndPosition() {
        try (ActivityScenario<DisplayActivity> scenario = launchBoard()) {
            awaitPage(scenario, 2, "Home first");
            scenario.onActivity(activity -> ((ViewPager) activity.findViewById(R.id.pager)).setCurrentItem(1, false));
            for (int i = 0; i < 3; i++) {
                openDetails(scenario);
                scenario.onActivity(DisplayActivity::onBackPressed);
                awaitPage(scenario, 2, "Home second");
                scenario.onActivity(activity -> {
                    assertEquals(1, ((ViewPager) activity.findViewById(R.id.pager)).getCurrentItem());
                    assertEquals(0, activity.getSupportFragmentManager().getBackStackEntryCount());
                });
            }
        }
    }

    @Test
    public void recreationOnLinkedPagePreservesHistoryAndPagerPosition() {
        try (ActivityScenario<DisplayActivity> scenario = launchBoard()) {
            awaitPage(scenario, 2, "Home first");
            scenario.onActivity(activity -> ((ViewPager) activity.findViewById(R.id.pager)).setCurrentItem(1, false));
            openDetails(scenario);
            scenario.onActivity(activity -> ((ViewPager) activity.findViewById(R.id.pager)).setCurrentItem(2, false));
            for (int i = 0; i < 3; i++) {
                scenario.recreate();
                awaitPage(scenario, 3, "Details third");
                scenario.onActivity(activity -> {
                    assertEquals(2, ((ViewPager) activity.findViewById(R.id.pager)).getCurrentItem());
                    assertEquals(1, activity.getSupportFragmentManager().getBackStackEntryCount());
                });
            }
            scenario.onActivity(DisplayActivity::onBackPressed);
            awaitPage(scenario, 2, "Home second");
            scenario.onActivity(activity -> assertEquals(1,
                    ((ViewPager) activity.findViewById(R.id.pager)).getCurrentItem()));
        }
    }

    private ActivityScenario<DisplayActivity> launchBoard() {
        Page home = new Page("Home", Arrays.asList(item("Home first"), item("Home second")), 1, 1, true);
        Page details = new Page("Details", Arrays.asList(item("Details first"), item("Details second"), item("Details third")), 1, 1, false);
        SpreadSheet board = new SpreadSheet("Navigation QA", "Home", Arrays.asList(home, details));
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), DisplayActivity.class);
        intent.putExtra(DisplayActivity.SPREADSHEET, board);
        return ActivityScenario.launch(intent);
    }

    private Item item(String name) {
        return new Item(name, "", name, Category.NOUN, null, false);
    }

    private void openDetails(ActivityScenario<DisplayActivity> scenario) {
        // Same navigation request as a linked tile, without starting TTS in this test.
        scenario.onActivity(activity -> new ViewModelProvider(activity).get(DisplayViewModel.class).openPage("Details"));
        awaitPage(scenario, 3, "Details first");
    }

    private void awaitPage(ActivityScenario<DisplayActivity> scenario, int count, String label) {
        AtomicBoolean ready = new AtomicBoolean();
        long deadline = SystemClock.uptimeMillis() + 15000;
        while (!ready.get() && SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity(activity -> {
                ViewPager pager = activity.findViewById(R.id.pager);
                if (pager == null || pager.getAdapter() == null || pager.getAdapter().getCount() != count) return;
                ArrayList<View> matches = new ArrayList<>();
                pager.findViewsWithText(matches, label, View.FIND_VIEWS_WITH_TEXT);
                ready.set(!matches.isEmpty());
            });
            SystemClock.sleep(50);
        }
        assertTrue("Expected board page: " + label, ready.get());
    }
}
