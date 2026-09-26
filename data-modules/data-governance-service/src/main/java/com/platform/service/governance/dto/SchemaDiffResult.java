package com.platform.service.governance.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SchemaDiffResult {
    private String tableSchemaId;
    private String sourceTable;
    private String targetTable;
    private List<ColumnInfo> sourceColumns = new ArrayList<>();
    private List<ColumnInfo> targetColumns = new ArrayList<>();
    private List<DiffItem> diffs = new ArrayList<>();
    private boolean hasDiff;

    @Data
    public static class DiffItem {
        private String type;
        private String columnName;
        private String sourceType;
        private String targetType;
        private String sourceComment;
        private String targetComment;
        private String detail;

        public DiffItem(String type, String columnName) {
            this.type = type;
            this.columnName = columnName;
        }
    }
}
