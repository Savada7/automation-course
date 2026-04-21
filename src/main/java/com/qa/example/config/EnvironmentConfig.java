package com.qa.example.config;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.Sources;



@Config.Sources({"classpath:config-${env}.properties"})
public interface EnvironmentConfig extends Config {

    @Key("baseUrl")
    String baseUrl();

    @Key("envName")
    String envName();
}
