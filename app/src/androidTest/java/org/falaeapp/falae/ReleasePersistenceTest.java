package org.falaeapp.falae;

import android.content.Context;
import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.falaeapp.falae.model.Category;
import org.falaeapp.falae.model.DownloadCache;
import org.falaeapp.falae.model.Item;
import org.falaeapp.falae.model.Page;
import org.falaeapp.falae.model.SpreadSheet;
import org.falaeapp.falae.model.User;
import org.falaeapp.falae.room.AppDatabase;
import org.falaeapp.falae.room.converter.MutableMapConverter;
import org.falaeapp.falae.room.converter.SpreadSheetConverter;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/** Run against the minified release as well as debug to catch lost Gson signatures. */
@RunWith(AndroidJUnit4.class)
public class ReleasePersistenceTest {
    @Test
    public void boardConverterPreservesNestedTypesAndExistingJson() {
        // Fixed JSON verifies compatibility with existing saved boards, not just new writes.
        String json = "[{\"name\":\"Comunicacao\",\"initialPage\":\"Inicio\",\"pages\":["
                + "{\"name\":\"Inicio\",\"items\":[{\"name\":\"Agua\",\"imgSrc\":\"image.png\","
                + "\"speech\":\"Quero agua\",\"category\":\"NOUN\",\"private\":false}],"
                + "\"rows\":2,\"columns\":2,\"initialPage\":true}]}]";
        List<SpreadSheet> restored = SpreadSheetConverter.toSpreadSheetList(json);
        assertEquals(1, restored.size());
        Item item = restored.get(0).getPages().get(0).getItems().get(0);
        assertEquals("Quero agua", item.getSpeech());
        assertEquals(Category.NOUN, item.getCategory());
        assertEquals(2, restored.get(0).getPages().get(0).getColumns());
        assertTrue(restored.get(0).getPages().get(0).getInitialPage());
        assertEquals(restored, SpreadSheetConverter.toSpreadSheetList(
                SpreadSheetConverter.toJsonString(restored)));
        assertTrue(SpreadSheetConverter.toSpreadSheetList("[]").isEmpty());
    }

    @Test
    public void cacheConverterPreservesExistingJson() {
        Map<String, String> expected = new HashMap<>();
        expected.put("image.png", "cached-image.png");
        assertEquals(expected, MutableMapConverter.toSpreadSheetList(
                "{\"image.png\":\"cached-image.png\"}"));
        assertEquals(expected, MutableMapConverter.toSpreadSheetList(
                MutableMapConverter.toJsonString(expected)));
        assertTrue(MutableMapConverter.toSpreadSheetList("{}").isEmpty());
    }

    @Test
    public void roomRoundTripPreservesBoardsAndDownloadCache() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Never open or clear the user's real falae.db.
        AppDatabase database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        try {
            Item item = new Item("Agua", "image.png", "Quero agua", Category.NOUN, null, false);
            Page page = new Page("Inicio", Collections.singletonList(item), 2, 2, true);
            SpreadSheet board = new SpreadSheet("Comunicacao", "Inicio", Collections.singletonList(page));
            User user = new User(0, "Teste", "", "teste@example.com",
                    Collections.singletonList(board), "perfil", "foto.png");
            long id = database.userModelDao().insert(user);
            User restored = database.userModelDao().findById(id);
            assertEquals(user.getEmail(), restored.getEmail());
            assertEquals(user.getSpreadsheets(), restored.getSpreadsheets());

            Map<String, String> sources = new HashMap<>();
            sources.put("image.png", "cached-image.png");
            database.downloadCacheDao().insert(new DownloadCache(0, "qa-cache", sources));
            DownloadCache cache = database.downloadCacheDao().findByName("qa-cache");
            assertNotNull(cache);
            assertEquals(sources, cache.getSources());
        } finally {
            database.close();
        }
    }
}
