package com.qa.tests;

import com.qa.pages.DragDropPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class DragDropTests extends BaseTest {
    private DragDropPage dragDropPage;

    @BeforeEach
    public void openPage(){
        dragDropPage = pageFactory.createDragDropPage();
    }

    @Test
    public void testDragAndDrop(){
        dragDropPage
                .navigationToDragDropPage()
                .isDragDropPage()
                .dragDropArea()
                .assertThatInitialState()
                .dragAToB()
                .assertThatDraggedState();
    }


}
