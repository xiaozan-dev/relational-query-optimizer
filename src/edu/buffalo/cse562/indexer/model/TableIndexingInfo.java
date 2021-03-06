package edu.buffalo.cse562.indexer.model;

import edu.buffalo.cse562.model.TableInfo;
import edu.buffalo.cse562.schema.SchemaUtils;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TableIndexingInfo extends TableInfo {

    private Set<Integer> indexPositions;

    public TableIndexingInfo(String tableName, List<ColumnDefinition> columnDefinitions, Long size) {
        super(tableName, columnDefinitions, size);
        columnIndexesUsed = getIndexesForAllColumnDefinitions();
        indexPositions = new HashSet<>();
    }

    public void addIndex(Column column) {
        final String columnName = column.getColumnName();
        final Integer oldPosition = SchemaUtils.getColumnIndexInColDefn(columnDefinitions, columnName);
        indexPositions.add(oldPosition);
    }

    public Set<Integer> getIndexPositions() {
        return indexPositions;
    }

}
