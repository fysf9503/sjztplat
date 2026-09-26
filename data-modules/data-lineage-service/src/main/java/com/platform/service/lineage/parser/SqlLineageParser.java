package com.platform.service.lineage.parser;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLAllColumnExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class SqlLineageParser {

    @Data
    public static class TableLineage {
        private String sourceCatalog;
        private String sourceDatabase;
        private String sourceTable;
        private String targetDatabase;
        private String targetTable;
    }

    @Data
    public static class FieldLineage {
        private String sourceCatalog;
        private String sourceDatabase;
        private String sourceTable;
        private String sourceField;
        private String targetDatabase;
        private String targetTable;
        private String targetField;
        private String transformExpr;
        private String fieldType;
    }

    public ParseResult parse(String sql) {
        ParseResult result = new ParseResult();
        try {
            List<SQLStatement> stmts = SQLUtils.parseStatements(sql, "mysql");
            for (SQLStatement stmt : stmts) {
                if (stmt instanceof MySqlInsertStatement insert) {
                    parseInsert(insert, result);
                } else if (stmt instanceof SQLCreateTableStatement create) {
                    parseCreateTableAsSelect(create, result);
                }
            }
        } catch (Exception e) {
            log.error("SQL血缘解析失败: {}", e.getMessage());
        }
        return result;
    }

    private void parseInsert(MySqlInsertStatement insert, ParseResult result) {
        SQLExprTableSource target = insert.getTableSource();
        TableInfo targetTable = parseTableSource(target);

        List<SQLSelectQueryBlock> selectBlocks = extractSelectBlocks(insert.getQuery());
        for (SQLSelectQueryBlock block : selectBlocks) {
            parseSelectBlock(block, targetTable, result);
        }
    }

    private void parseCreateTableAsSelect(SQLCreateTableStatement create, ParseResult result) {
        if (create.getSelect() == null) return;
        TableInfo targetTable = parseTableSource(create.getTableSource());
        List<SQLSelectQueryBlock> selectBlocks = extractSelectBlocks(create.getSelect());
        for (SQLSelectQueryBlock block : selectBlocks) {
            parseSelectBlock(block, targetTable, result);
        }
    }

    private void parseSelectBlock(SQLSelectQueryBlock block, TableInfo targetTable, ParseResult result) {
        SQLTableSource fromSource = block.getFrom();
        List<TableInfo> sourceTables = extractSourceTables(fromSource);

        for (TableInfo sourceTable : sourceTables) {
            TableLineage tl = new TableLineage();
            tl.setSourceCatalog(sourceTable.catalog);
            tl.setSourceDatabase(sourceTable.database);
            tl.setSourceTable(sourceTable.table);
            tl.setTargetDatabase(targetTable.database);
            tl.setTargetTable(targetTable.table);
            result.tableLineages.add(tl);
        }

        List<SQLSelectItem> selectItems = block.getSelectList();
        for (int i = 0; i < selectItems.size(); i++) {
            SQLSelectItem item = selectItems.get(i);
            String targetField = item.getAlias() != null ? item.getAlias() : "col_" + (i + 1);
            if (item.getExpr() instanceof SQLIdentifierExpr idExpr) {
                for (TableInfo sourceTable : sourceTables) {
                    result.fieldLineages.add(buildFieldLineage(
                            sourceTable, idExpr.getName(), targetTable, targetField, null, "direct"));
                }
            } else if (item.getExpr() instanceof SQLPropertyExpr propExpr) {
                String sourceField = propExpr.getName();
                for (TableInfo sourceTable : sourceTables) {
                    result.fieldLineages.add(buildFieldLineage(
                            sourceTable, sourceField, targetTable, targetField, null, "direct"));
                }
            } else if (item.getExpr() instanceof SQLAllColumnExpr) {
                for (TableInfo sourceTable : sourceTables) {
                    result.fieldLineages.add(buildFieldLineage(
                            sourceTable, "*", targetTable, targetField, null, "wildcard"));
                }
            } else {
                String exprStr = item.getExpr().toString();
                FieldLineage fl = new FieldLineage();
                fl.setSourceField(exprStr);
                fl.setTargetDatabase(targetTable.database);
                fl.setTargetTable(targetTable.table);
                fl.setTargetField(targetField);
                fl.setTransformExpr(exprStr);
                fl.setFieldType("derived");
                result.fieldLineages.add(fl);
            }
        }
    }

    private FieldLineage buildFieldLineage(TableInfo sourceTable, String sourceField,
                                           TableInfo targetTable, String targetField,
                                           String transformExpr, String fieldType) {
        FieldLineage fl = new FieldLineage();
        fl.setSourceCatalog(sourceTable.catalog);
        fl.setSourceDatabase(sourceTable.database);
        fl.setSourceTable(sourceTable.table);
        fl.setSourceField(sourceField);
        fl.setTargetDatabase(targetTable.database);
        fl.setTargetTable(targetTable.table);
        fl.setTargetField(targetField);
        fl.setTransformExpr(transformExpr);
        fl.setFieldType(fieldType);
        return fl;
    }

    private List<SQLSelectQueryBlock> extractSelectBlocks(SQLSelect query) {
        List<SQLSelectQueryBlock> blocks = new ArrayList<>();
        if (query == null) return blocks;
        if (query.getQuery() instanceof SQLSelectQueryBlock block) {
            blocks.add(block);
        } else if (query.getQuery() instanceof SQLUnionQuery union) {
            for (SQLSelectQuery sub : union.getRelations()) {
                if (sub instanceof SQLSelectQueryBlock subBlock) {
                    blocks.add(subBlock);
                }
            }
        }
        return blocks;
    }

    private List<TableInfo> extractSourceTables(SQLTableSource fromSource) {
        List<TableInfo> tables = new ArrayList<>();
        if (fromSource == null) return tables;
        if (fromSource instanceof SQLExprTableSource exprTable) {
            TableInfo info = parseTableSource(exprTable);
            if (info != null) tables.add(info);
        } else if (fromSource instanceof SQLJoinTableSource joinSource) {
            tables.addAll(extractSourceTables(joinSource.getLeft()));
            tables.addAll(extractSourceTables(joinSource.getRight()));
        }
        return tables;
    }

    private TableInfo parseTableSource(SQLExprTableSource tableSource) {
        if (tableSource == null) return null;
        String fullName = tableSource.getExpr() instanceof SQLPropertyExpr prop
                ? prop.toString()
                : tableSource.toString();
        return parseTableName(fullName);
    }

    private TableInfo parseTableName(String fullName) {
        if (fullName == null || fullName.isEmpty()) return null;
        fullName = fullName.replace("`", "").replace("\"", "");
        String[] parts = fullName.split("\\.");
        if (parts.length >= 3) {
            return new TableInfo(parts[0], parts[1], parts[2]);
        } else if (parts.length == 2) {
            return new TableInfo(null, parts[0], parts[1]);
        } else {
            return new TableInfo(null, null, parts[0]);
        }
    }

    public static class ParseResult {
        public List<TableLineage> tableLineages = new ArrayList<>();
        public List<FieldLineage> fieldLineages = new ArrayList<>();
    }

    private record TableInfo(String catalog, String database, String table) {}
}
