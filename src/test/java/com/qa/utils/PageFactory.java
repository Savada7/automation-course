package com.qa.utils;

import com.microsoft.playwright.Page;
import com.qa.pages.DragDropPage;

public class PageFactory {
    private final Page page;

    public PageFactory(Page page){
        this.page = page;
    }

    public DragDropPage createDragDropPage(){
        return new DragDropPage(page);
    }
}
