package org.falaeapp.falae;

import org.falaeapp.falae.model.Page;
import org.falaeapp.falae.viewmodel.DisplayViewModel;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class PageNavigationEventTest {
    @Test
    public void restoringAnObserverDoesNotRepeatHandledNavigation() {
        Page page = new Page("Home", Collections.emptyList(), 2, 2, true);
        DisplayViewModel.PageNavigation navigation = new DisplayViewModel.PageNavigation(page);
        assertSame(page, navigation.consume());
        assertNull(navigation.consume());
    }

    @Test
    public void aNewRequestCanOpenTheSamePageAgain() {
        Page page = new Page("Home", Collections.emptyList(), 2, 2, true);
        DisplayViewModel.PageNavigation first = new DisplayViewModel.PageNavigation(page);
        DisplayViewModel.PageNavigation next = new DisplayViewModel.PageNavigation(page);
        assertSame(page, first.consume());
        assertNull(first.consume());
        assertSame(page, next.consume());
    }
}
