package com.thecoderscorner.embedcontrol.core.util;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This is a utility mapping class specifically for TCC applications, it has some raw SQL functions and the smallest
 * possible set of ORM functionality for our own purposes. Note that it is not intended as a general purpose ORM and
 * is just enough of an ORM for our purposes. You are welcome at your own risk to use this more widely but such use
 * is not recommended
 */
public class TccDatabaseUtilities {
    private final System.Logger logger = System.getLogger(TccDatabaseUtilities.class.getSimpleName());
    private final String newLine = System.getProperty("line.separator");
    private final DataSource dataSource;
    private final Connection connection;

    public TccDatabaseUtilities(DataSource dataSource) throws SQLException {
        this.dataSource = dataSource;
        connection = this.dataSource.getConnection();
    }

    public void close() throws Exception {
        if(connection != null) connection.close();
    }

    public <T> List<T> queryRecords(Class<T> databaseType, String whereClause, Object... params) throws DataException {
        var tableInfo = databaseType.getAnnotation(TableMapping.class);

        var list = new ArrayList<T>();
        String wherePart = "";
        if(!StringHelper.isStringEmptyOrNull(whereClause)) {
            wherePart = " WHERE " + whereClause;
        }
        var sql = "SELECT * FROM " + tableInfo.tableName() + wherePart;
        try(var stmt = connection.prepareStatement(sql)) {
            addParamsToStmt(params, stmt);
            var rs = stmt.executeQuery();
            while(rs.next()) {
                list.add(fromResultSet(rs, databaseType));
            }
            return list;
        } catch (Exception ex) {
            throw new DataException("queryObject", ex);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> T fromResultSet(ResultSet rs, Class<T> databaseType) throws Exception {
        var item = databaseType.getConstructor().newInstance();
        for (var field : databaseType.getDeclaredFields()) {
            field.setAccessible(true);

            if(field.isAnnotationPresent(ProvideStore.class)) {
                field.set(item, this);
            }

            if (!field.isAnnotationPresent(FieldMapping.class)) continue;
            var fm = field.getAnnotation(FieldMapping.class);

            switch (fm.fieldType()) {
                case BOOLEAN -> field.setBoolean(item, rs.getInt(fm.fieldName()) == 1);
                case INTEGER -> field.setInt(item, rs.getInt(fm.fieldName()));
                case ISO_DATE -> field.set(item, LocalDateTime.parse(rs.getString(fm.fieldName()), DateTimeFormatter.ISO_DATE_TIME));
                case VARCHAR, BLOB -> field.set(item, rs.getString(fm.fieldName()));
                case ENUM -> field.set(item, Enum.valueOf((Class<Enum>) field.getType(), rs.getString(fm.fieldName())));
            }
        }

        return item;
    }

    public <T> Optional<T> queryPrimaryKey(Class<T> databaseType, Object primaryKey) throws DataException {
        var tableInfo = databaseType.getAnnotation(TableMapping.class);

        var sql = "SELECT * FROM " + tableInfo.tableName() + " WHERE " + tableInfo.uniqueKeyField() + " = ?";
        try(var stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, primaryKey);
            var rs = stmt.executeQuery();
            if(rs.next()) {
                return Optional.of(fromResultSet(rs, databaseType));
            }
            return Optional.empty();
        } catch (Exception ex) {
            throw new DataException("queryObject", ex);
        }
    }

    public <T> int updateRecord(Class<T> databaseType, T data) throws DataException {
        var tableInfo = databaseType.getAnnotation(TableMapping.class);

        try {
            String sql;
            var pkField = Arrays.stream(databaseType.getDeclaredFields()).filter(this::isPrimaryKey).findFirst().orElseThrow();
            pkField.setAccessible(true);
            int pkValue = (int) pkField.get(data);
            if(pkValue == -1) {
                pkValue = nextRecordForTable(tableInfo);
                pkField.set(data, pkValue);
            }
            var count = queryRawSqlSingleInt("SELECT COUNT(*) from " + tableInfo.tableName() + " where " + tableInfo.uniqueKeyField() + " = ?", pkValue);
            var names = new ArrayList<String>();
            var values = new ArrayList<Object>();
            if (count == 0) {
                for (var field : data.getClass().getDeclaredFields()) {
                    if (!field.isAnnotationPresent(FieldMapping.class)) continue;
                    var annotation = field.getAnnotation(FieldMapping.class);
                    field.setAccessible(true);
                    names.add(annotation.fieldName());
                    values.add(field.get(data));
                }
                sql = "INSERT INTO " + tableInfo.tableName() + "(" + String.join(",", names)
                        + ") values(?" + ",?".repeat(values.size() - 1) + ");";
            } else {
                for (var field : data.getClass().getDeclaredFields()) {
                    if (!field.isAnnotationPresent(FieldMapping.class)) continue;
                    field.setAccessible(true);
                    var annotation = field.getAnnotation(FieldMapping.class);
                    names.add("  " + annotation.fieldName() + " = ?");
                    values.add(field.get(data));
                }
                values.add(pkValue);
                sql = "UPDATE " + tableInfo.tableName() + newLine
                        + "SET " + newLine +
                        String.join("," + newLine, names) + newLine
                        + "WHERE " + tableInfo.uniqueKeyField() + "=?";
            }

            logger.log(System.Logger.Level.DEBUG, "Exec update record - " + sql);
            try (var stmt = connection.prepareStatement(sql)) {
                addParamsToStmt(values.toArray(), stmt);
                stmt.executeUpdate();
                return pkValue;
            }
        } catch (Exception e) {
            throw new DataException("Update record " + databaseType.getSimpleName(), e);
        }
    }

    private int nextRecordForTable(TableMapping tableInfo) throws DataException {
        var res = queryRawSqlSingleInt("SELECT MAX("+tableInfo.uniqueKeyField()+") from " + tableInfo.tableName());
        return (res == 0) ? 1 : (res + 1);

    }

    private boolean isPrimaryKey(Field field) {
        if(!field.isAnnotationPresent(FieldMapping.class)) return false;
        return field.getAnnotation(FieldMapping.class).primaryKey();
    }

    public void ensureTableFormatCorrect(Class<?>... databaseTypes) throws DataException {
        for(var databaseType : databaseTypes) {
            var tableMapping = databaseType.getAnnotation(TableMapping.class);
            var fieldMappings = getAnnotationsByType(databaseType, FieldMapping.class);
            boolean changed = false;

            logger.log(System.Logger.Level.INFO, "Checking for table " + tableMapping.tableName());
            var sql = "SELECT COUNT(name) FROM sqlite_master WHERE type='table' AND name=?";
            var res = queryRawSqlSingleInt(sql, tableMapping.tableName());
            if (res == 0) {
                createTableFully(tableMapping, fieldMappings);
                changed = true;
            } else {
                logger.log(System.Logger.Level.INFO, "Checking format of " + tableMapping.tableName());
                try (var stmt = connection.createStatement()) {
                    var rs = stmt.executeQuery("SELECT * FROM " + tableMapping.tableName());
                    var meta = rs.getMetaData();
                    var columnsToAdd = new ArrayList<FieldMapping>();
                    for(var field : databaseType.getDeclaredFields()) {
                        var fieldMapping = field.getAnnotation(FieldMapping.class);
                        if(fieldMapping == null) continue;
                        boolean found = false;
                        for (int i = 1; i <= meta.getColumnCount(); i++) {
                            var colName = meta.getColumnName(i);
                            if(colName.equalsIgnoreCase(fieldMapping.fieldName())) {
                                found = true;
                                break;
                            }
                        }
                        if(!found) columnsToAdd.add(fieldMapping);
                    }
                    if (!columnsToAdd.isEmpty()) {
                        logger.log(System.Logger.Level.INFO, "Changes to be applied: " + columnsToAdd);
                        alterTableToNewSpec(tableMapping, columnsToAdd);
                        changed = true;
                    }
                } catch (Exception ex) {
                    throw new DataException("Checking table structure " + tableMapping.tableName(), ex);
                }
            }
            logger.log(System.Logger.Level.INFO, "Table - " + tableMapping.tableName() + ", changed= " + changed);
        }
    }

    private FieldMapping[] getAnnotationsByType(Class<?> databaseType, Class<?> fieldMappingClass) {
        var l = Arrays.stream(databaseType.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(FieldMapping.class))
                .map(f -> f.getAnnotation(FieldMapping.class))
                .toList();
        return l.toArray(FieldMapping[]::new);
    }

    private void alterTableToNewSpec(TableMapping tableMapping, ArrayList<FieldMapping> columnsToAdd) throws DataException {
        logger.log(System.Logger.Level.DEBUG, "Altering table " + tableMapping.tableName() + " by adding " + columnsToAdd);

        if(columnsToAdd.stream().anyMatch(FieldMapping::primaryKey)) {
            throw new DataException("Extra primary keys cannot be added later to " + tableMapping.tableName());
        }

        for(var col : columnsToAdd) {
            var sql = "ALTER TABLE " + tableMapping.tableName() + " ADD COLUMN " + col.fieldName() + " " + toTypeDecl(col);
            executeRaw(sql);
        }
    }

    private void createTableFully(TableMapping tableMapping, FieldMapping[] fieldMappings) throws DataException {
        logger.log(System.Logger.Level.DEBUG, "Creating table " + tableMapping.tableName());

        var sql = "CREATE TABLE " + tableMapping.tableName() + "(" + newLine;
        sql += Arrays.stream(fieldMappings).map(fm -> "  " + fm.fieldName() + " " + toTypeDecl(fm))
                .collect(Collectors.joining("," + newLine));
        sql += ");";
        executeRaw(sql);
    }

    public void executeRaw(String sql, Object... params) throws DataException {
        logger.log(System.Logger.Level.DEBUG, "Execute raw sql " + sql);
        try (var stmt = connection.prepareStatement(sql)) {
            addParamsToStmt(params, stmt);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Execute raw " + sql, e);
        }
    }

    private String toTypeDecl(FieldMapping fm) {
        var strTy = switch (fm.fieldType()) {
            case ENUM, VARCHAR, ISO_DATE -> "VARCHAR(255)";
            case INTEGER, BOOLEAN -> "INTEGER";
            case BLOB -> "BLOB";
        };
        return fm.primaryKey() ? strTy + " PRIMARY KEY" : strTy;
    }
    public int queryRawSqlSingleInt(String sql, Object... data) throws DataException {
        logger.log(System.Logger.Level.DEBUG, "Query for int " + sql);
        try (var stmt = connection.prepareStatement(sql)) {

            addParamsToStmt(data, stmt);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new DataException("Query had no result but was int query");
        } catch (Exception ex) {
            throw new DataException("Query raw SQL single " + sql, ex);
        }
    }

    private static void addParamsToStmt(Object[] data, PreparedStatement stmt) throws SQLException {
        int i=1;
        for(var d : data) {
            if(d instanceof LocalDateTime ldt) {
                stmt.setObject(i++, ldt.format(DateTimeFormatter.ISO_DATE_TIME));
            } else {
                stmt.setObject(i++, d);
            }
        }
    }

    public void rawSelect(String s, ResultSetConsumer resultConsumer, Object... args) throws Exception {
        try (var stmt = connection.createStatement()) {
            var rs = stmt.executeQuery(s);
            resultConsumer.processResults(rs);
        }
    }

    public List<String> queryStrings(String sql, Object... params) throws DataException {
        logger.log(System.Logger.Level.DEBUG, "Query for strings " + sql);
        try (var stmt = connection.prepareStatement(sql)) {
            addParamsToStmt(params, stmt);
            var rs = stmt.executeQuery();
            var list = new ArrayList<String>();
            while(rs.next()) {
                 list.add(rs.getString(1));
            }
            return list;
        } catch (Exception ex) {
            throw new DataException("Query raw SQL single " + sql, ex);
        }
    }

    public boolean checkTableExists(String table) throws DataException {
        var sql = "SELECT COUNT(name) FROM sqlite_master WHERE type='table' AND name=?";
        return queryRawSqlSingleInt(sql, table) != 0;
    }

    public void ensureTableExists(String tableToCheck, String sqlForCreate) {
        var sql = "SELECT COUNT(name) FROM sqlite_master WHERE type='table' AND name=?";
        try {
            int res = queryRawSqlSingleInt(sql, tableToCheck);
            if (res == 0) {
                executeRaw(sqlForCreate);
            }
        } catch (DataException e) {
            logger.log(System.Logger.Level.ERROR, "Could not check table exists", e);
        }
    }
}
