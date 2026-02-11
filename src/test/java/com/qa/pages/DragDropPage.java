package com.qa.pages;

import com.microsoft.playwright.Page;
import com.qa.components.DragDropArea;
import io.qameta.allure.Step;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.assertThat;



public class DragDropPage extends BasePage{
    private DragDropArea dragDropArea;

    String headerText;

    public DragDropPage(Page page){
        super(page);
    }

    public DragDropArea dragDropArea(){
        if (dragDropArea == null) {
            dragDropArea = new DragDropArea(page);
        }
        return dragDropArea;
    }

    @Step
    @DisplayName("Навигация на страницу drag and drop")
    public DragDropPage navigationToDragDropPage(){
        page.navigate("https://the-internet.herokuapp.com/drag_and_drop");
        return this;
    }

    @Step
    @DisplayName("проверка, что текущая страница drag and drop page")
    public DragDropPage isDragDropPage(){
        headerText = page.locator("h3").textContent();
        assertThat(headerText).isEqualTo("Drag and Drop");
        return this;
    }
}
