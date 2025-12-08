package courseplayw;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CartTest extends BaseTest{

    @Test
    void testAddRemoveElements(){
        page.navigate("https://the-internet.herokuapp.com/add_remove_elements/");
        page.click("button:has-text('Add Element')");
        Locator deleteButton = page.locator("button.added-manually");
        Assertions.assertTrue(deleteButton.isVisible());
        captureScreenshot("add 1 element");

        deleteButton.click();

        Assertions.assertEquals(0, page.locator("button.added-manually").count());
        captureScreenshot("deleted element");
    }
}
