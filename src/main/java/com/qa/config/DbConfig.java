package com.qa.config;

import org.aeonbits.owner.Config;

@Config.Sources("classpath:dbconfig.properties")
public interface DbConfig extends Config {
    @Key("db.url")
    String dbUrl();

    @Key("db.user")
    String dbUser();

    @Key("db.password")
    String dbPassword();
}
