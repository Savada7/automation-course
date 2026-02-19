package com.qa.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

public class DynamicControlsPage {
    private final Page page;

    public DynamicControlsPage(Page page){
        this.page = page;
    }

    private Locator getCheckBox(){
        return page.locator("#checkbox");
    }

    private Locator getRemoveButton(){
        return page.locator("button:has-text('Remove')");
    }

    private Locator getAddButton(){
        return page.locator("button:has-text('Add')");
    }

    private Locator getMessage(){
        return page.locator("#message");
    }

    //Действия
    public DynamicControlsPage navigate(){
        page.navigate("https://the-internet.herokuapp.com/dynamic_controls");
        return this;
    }

    public DynamicControlsPage clickRemoveButton(){
        getRemoveButton().click();
        return this;
    }

    public DynamicControlsPage clickAddButton(){
        getAddButton().click();
        return this;
    }
    public DynamicControlsPage checkboxClick(){
        getCheckBox().click();
        return this;
    }

    //Состояния
    public boolean isCheckboxVisible(){
        return getCheckBox().isVisible();
    }

    public DynamicControlsPage verifyCheckboxVisible(boolean expected){
        assert isCheckboxVisible() == expected;
        return this;
    }

    public DynamicControlsPage verifyMessage(String expectedMessage){
        assert getMessageText().equals(expectedMessage);
        return this;
    }

    //Ожидания
    public DynamicControlsPage waitForCheckboxToDisappear(){
        getCheckBox().waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.HIDDEN));
        return this;
    }

    public DynamicControlsPage waitForCheckboxToAppear(){
        getCheckBox().waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE));
        return this;
    }

    public DynamicControlsPage waitForMessage(){
        getMessage().waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE));
        return this;
    }

    //Получение данных
    public String getMessageText(){
        return getMessage().textContent();
    }
}
