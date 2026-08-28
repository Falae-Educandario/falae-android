package org.falaeapp.falae;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.SystemClock;
import android.widget.EditText;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.navigation.NavigationView;
import org.falaeapp.falae.activity.MainActivity;
import org.falaeapp.falae.activity.DisplayActivity;
import org.falaeapp.falae.repository.UserRepository;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.falaeapp.falae.model.Category;
import org.falaeapp.falae.model.Item;
import org.falaeapp.falae.model.Page;
import org.falaeapp.falae.model.SpreadSheet;
import org.falaeapp.falae.model.User;

import java.util.Collections;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertEquals("org.falaeapp.falae", appContext.getPackageName());
    }

    @Test
    public void communicationBoardSurvivesParcelRoundTrip() {
        Item item = new Item("Agua", "image.png", "Quero agua", Category.NOUN, null, false);
        Page page = new Page("Inicio", Collections.singletonList(item), 2, 2, true);
        SpreadSheet board = new SpreadSheet("Comunicacao", "Inicio", Collections.singletonList(page));
        User original = new User(7, "Teste", "test-token", "teste@example.com",
                Collections.singletonList(board), "perfil", "foto.png");

        Parcel parcel = Parcel.obtain();
        try {
            original.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            assertEquals(original, User.CREATOR.createFromParcel(parcel));
        } finally {
            parcel.recycle();
        }
    }

    @Test
    public void upgradePreservesAccessibilitySettingsAndSelectedUser() throws Exception {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final SharedPreferences preferences = appContext.getSharedPreferences("upgrade_test", Context.MODE_PRIVATE);
        Context isolatedContext = new ContextWrapper(appContext) {
            @Override public Context getApplicationContext() { return this; }
            @Override public SharedPreferences getSharedPreferences(String name, int mode) { return preferences; }
        };
        try {
            preferences.edit().clear().putInt("versionCode", 30)
                    .putBoolean("scanMode", true).putLong("scanModeDuration", 1500L)
                    .putLong("LastConnectedUser", 7L).commit();
            UserRepository repository = new UserRepository(isolatedContext);
            BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE,
                    (scope, continuation) -> repository.handleNewVersion(31, continuation));
            assertEquals(31, preferences.getInt("versionCode", -1));
            assertTrue(preferences.getBoolean("scanMode", false));
            assertEquals(1500L, preferences.getLong("scanModeDuration", 0));
            assertEquals(7L, preferences.getLong("LastConnectedUser", 0));
        } finally {
            preferences.edit().clear().commit();
        }
    }

    @Test
    public void mainNavigationAndLoginValidationWork() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            AtomicBoolean ready = new AtomicBoolean();
            long deadline = SystemClock.uptimeMillis() + 10000;
            while (!ready.get() && SystemClock.uptimeMillis() < deadline) {
                scenario.onActivity(activity -> ready.set(activity.findViewById(R.id.viewpager) != null));
                SystemClock.sleep(50);
            }
            assertTrue("Initial user tabs should load", ready.get());
            scenario.onActivity(activity -> {
                NavigationView navigation = activity.findViewById(R.id.nav_view);
                activity.onNavigationItemSelected(navigation.getMenu().findItem(R.id.add_user));
                activity.getSupportFragmentManager().executePendingTransactions();
                EditText email = activity.findViewById(R.id.email);
                assertNotNull(email);
                email.setText("");
                activity.findViewById(R.id.email_sign_in_button).performClick();
                assertEquals(activity.getString(R.string.error_field_required), email.getError().toString());
                activity.onNavigationItemSelected(navigation.getMenu().findItem(R.id.settings));
                activity.getSupportFragmentManager().executePendingTransactions();
                assertNotNull(activity.findViewById(R.id.scan_mode));
            });
        }
    }

    @Test
    public void boardPaginationSurvivesActivityRecreation() {
        Item first = new Item("Agua", "", "Quero agua", Category.NOUN, null, false);
        Item second = new Item("Comida", "", "Quero comida", Category.NOUN, null, false);
        Page page = new Page("Inicio", Arrays.asList(first, second), 1, 1, true);
        SpreadSheet board = new SpreadSheet("Comunicacao", "Inicio", Collections.singletonList(page));
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), DisplayActivity.class);
        intent.putExtra(DisplayActivity.SPREADSHEET, board);
        try (ActivityScenario<DisplayActivity> scenario = ActivityScenario.launch(intent)) {
            awaitBoard(scenario);
            scenario.onActivity(activity -> {
                ViewPager pager = activity.findViewById(R.id.pager);
                pager.setCurrentItem(1, false);
                assertEquals(1, pager.getCurrentItem());
            });
            scenario.recreate();
            awaitBoard(scenario);
        }
    }

    private void awaitBoard(ActivityScenario<DisplayActivity> scenario) {
        AtomicBoolean ready = new AtomicBoolean();
        long deadline = SystemClock.uptimeMillis() + 10000;
        while (!ready.get() && SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity(activity -> {
                ViewPager pager = activity.findViewById(R.id.pager);
                ready.set(pager != null && pager.getAdapter() != null && pager.getAdapter().getCount() == 2);
            });
            SystemClock.sleep(50);
        }
        assertTrue("Both board pages should render", ready.get());
    }
}
