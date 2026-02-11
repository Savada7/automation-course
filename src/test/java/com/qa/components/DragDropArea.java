package com.qa.components;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DragDropArea {
    private final Page page;

    Locator elementA;
    Locator elementB;

    public DragDropArea(Page page){
        this.page = page;
    }

    public DragDropArea dragAToB(){
        elementA.dragTo(elementB);
        return this;
    }
    public DragDropArea assertThatInitialState() {
        elementA = page.locator("#column-a");
        elementB = page.locator("#column-b");

        assertEquals("A", elementA.textContent());
        assertEquals("B", elementB.textContent());
        return this;
    }

    public DragDropArea assertThatDraggedState() {
        assertEquals("B", elementA.textContent());
        assertEquals("A", elementB.textContent());
        return this;
    }
}
