package com.qa.tests;

import com.microsoft.playwright.*;
import com.qa.pages.DynamicControlsPage;
import com.qa.utils.TestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DynamicControlsTest {
    private TestContext context;
    private DynamicControlsPage controlsPage;

    @BeforeEach
    public void setup() {
        context = new TestContext();
        controlsPage = new DynamicControlsPage(context.getPage());
        controlsPage.navigate();
    }

    @Test
    void testDynamicCheckbox() {
        controlsPage
                .verifyCheckboxVisible(true)
                .clickRemoveButton()
                .waitForCheckboxToDisappear()
                .verifyCheckboxVisible(false)
                .verifyMessage("It's gone!")
                .clickAddButton()
                .waitForCheckboxToAppear()
                .verifyCheckboxVisible(true)
                .verifyMessage("It's back!");

    }

    @AfterEach
    public void teardown() {
        if (context != null) {
            context.close();
        }
    }
}