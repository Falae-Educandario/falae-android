package org.falaeapp.falae;

import android.content.Context;
import android.os.Parcel;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.falaeapp.falae.model.Category;
import org.falaeapp.falae.model.Item;
import org.falaeapp.falae.model.Page;
import org.falaeapp.falae.model.SpreadSheet;
import org.falaeapp.falae.model.User;

import java.util.Collections;

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
        Context appContext = InstrumentationRegistry.getTargetContext();

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
}
