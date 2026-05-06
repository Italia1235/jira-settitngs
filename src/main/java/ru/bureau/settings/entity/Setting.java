package ru.bureau.settings.entity;

import net.java.ao.Entity;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Unique;

public interface Setting extends Entity {

    @Unique
    String getName();

    void setName(String name);

    @NotNull @StringLength(StringLength.UNLIMITED)
    String getValue();
    void setValue(String value);


    @StringLength(StringLength.UNLIMITED)
    String getExplanation();
    void setExplanation(String explanation);
}
