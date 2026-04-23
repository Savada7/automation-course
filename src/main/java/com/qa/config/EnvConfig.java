package com.qa.config;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.Sources;

@Sources({
        "file:src/main/resources/config.properties",
        "classpath:config.properties"
})
public interface EnvConfig extends Config {

    @Key("browser")
    @DefaultValue("chromium")
    String browser();

    @Key("headless")
    @DefaultValue("true")
    boolean headless();

    @Key("base_url")
    String baseUrl();
}
