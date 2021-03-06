package edu.buffalo.cse562.visitor;

import edu.buffalo.cse562.data.Datum;
import edu.buffalo.cse562.schema.ColumnSchema;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.Division;
import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.buffalo.cse562.SchemaIndexConstants.*;

public class EvaluatorProjection extends AbstractExpressionVisitor {

    private ColumnSchema[] inputSchema;
    private List<ColumnSchema> outputSchema;
    private List<Integer> indexes;
    private int currentIndex = -1;
    private String alias;
    private Map<String, Expression> aliasExpressionMap = new HashMap<>();
    private boolean isAnAggregation;

    public EvaluatorProjection(ColumnSchema[] inputSchema, List<ColumnSchema> outputSchema, List<Integer> indexes) {
        this.inputSchema = inputSchema;
        this.outputSchema = outputSchema;
        this.indexes = indexes;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Override
    public void visit(Function function) {
        isAnAggregation = true;

        indexes.add(getOldIndexReferencedByFunction(currentIndex));
        String colName;
        if (alias == null) {
            colName = function.toString();
        } else {
            colName = alias;
        }
        ColumnSchema columnSchema = new ColumnSchema(colName, Datum.type.DOUBLE);
        columnSchema.setColumnAlias(alias);
        columnSchema.setExpression(function);
        columnSchema.setIsDistinct(function.isDistinct());
        outputSchema.add(columnSchema);

        final ExpressionList parameters = function.getParameters();
        if (parameters != null) {
            ((Expression) parameters.getExpressions().get(0)).accept(this);
        } else {
            indexes.set(currentIndex, SCHEMA_INDEX_INDICATING_STAR_INSIDE_FUNCTION);
        }
    }

    @Override
    public void visit(Parenthesis parenthesis) {
        parenthesis.getExpression().accept(this);
    }

    @Override
    public void visit(Column arg0) {
        for (int i = 0; i < inputSchema.length; i++) {
            if (inputSchema[i].matchColumn(arg0)) {
                if (isAnAggregation) {
                    indexes.set(currentIndex, getOldIndexReferencedByFunction(i));
                    return;
                }
                outputSchema.add(inputSchema[i]);
                indexes.add(i);
                return;
            }
        }

        for (String alias : aliasExpressionMap.keySet()) {
            if (arg0.getColumnName().equalsIgnoreCase(alias)) {
                indexes.add(currentIndex);
                ColumnSchema columnSchema = new ColumnSchema(alias, Datum.type.DOUBLE);
                columnSchema.setColumnAlias(alias);
                columnSchema.setExpression(aliasExpressionMap.get(alias));
                outputSchema.add(columnSchema);
                return;
            }
        }
        throw new UnsupportedOperationException("Column not found : " + arg0);
    }

    @Override
    public void visit(Addition arg0) {
        visitBinaryExpression(arg0);
    }

    @Override
    public void visit(Division arg0) {
        visitBinaryExpression(arg0);
    }

    @Override
    public void visit(Multiplication arg0) {
        visitBinaryExpression(arg0);
    }

    @Override
    public void visit(Subtraction arg0) {
        visitBinaryExpression(arg0);
    }

    private void visitBinaryExpression(BinaryExpression binaryExpression) {
        if (isAnAggregation) {
            indexes.set(currentIndex, SCHEMA_INDEX_INDICATING_EXPRESSION_INSIDE_FUNCTION);
            return;
        }

        ColumnSchema newColumnSchema = new ColumnSchema(binaryExpression.toString(), Datum.type.DOUBLE);
        newColumnSchema.setExpression(binaryExpression);
        if (alias != null) {
            aliasExpressionMap.put(alias, binaryExpression);
            newColumnSchema.setColumnAlias(alias);
        }
        outputSchema.add(newColumnSchema);
        indexes.add(SCHEMA_INDEX_INDICATING_EXPRESSION);
    }

    public boolean wasAnAggregation() {
        boolean lastResult = isAnAggregation;
        isAnAggregation = false;
        return lastResult;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void incrementCurrentIndex() {
        ++currentIndex;
    }
}