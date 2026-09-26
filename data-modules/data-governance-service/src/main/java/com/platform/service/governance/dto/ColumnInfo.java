package com.platform.service.governance.dto;

import lombok.Data;

@Data
public class ColumnInfo {
    private String name;
    private String type;
    private String comment;
    private String nullable;
    private String defaultValue;
    private int ordinal;

    public ColumnInfo() {}

    public ColumnInfo(String name, String type, String comment, String nullable, int ordinal) {
        this.name = name;
        this.type = type;
        this.comment = comment;
        this.nullable = nullable;
        this.ordinal = ordinal;
    }
}
