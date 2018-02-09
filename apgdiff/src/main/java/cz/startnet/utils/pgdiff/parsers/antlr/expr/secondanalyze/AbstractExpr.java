package cz.startnet.utils.pgdiff.parsers.antlr.expr.secondanalyze;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cz.startnet.utils.pgdiff.PgDiffUtils;
import cz.startnet.utils.pgdiff.loader.SupportedVersion;
import cz.startnet.utils.pgdiff.parsers.antlr.AntlrParser;
import cz.startnet.utils.pgdiff.parsers.antlr.QNameParser;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Data_typeContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Function_args_parserContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.IdentifierContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Schema_qualified_nameContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Schema_qualified_name_nontypeContext;
import cz.startnet.utils.pgdiff.parsers.antlr.statements.ParserAbstract;
import cz.startnet.utils.pgdiff.schema.GenericColumn;
import cz.startnet.utils.pgdiff.schema.IRelation;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.system.PgSystemRelation;
import cz.startnet.utils.pgdiff.schema.system.PgSystemStorage;
import ru.taximaxim.codekeeper.apgdiff.Log;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;

public abstract class AbstractExpr {

    // TODO get postgresql version.
    // Need to get version. I can get it from JdbcLoader(READER),
    // but I can't get it from PgDumpLoader(WRITER).
    protected final PgSystemStorage systemStorage;
    protected final String schema;
    private final AbstractExpr parent;
    private final Set<GenericColumn> depcies;

    protected final PgDatabase db;

    public Set<GenericColumn> getDepcies() {
        return Collections.unmodifiableSet(depcies);
    }

    public AbstractExpr(String schema, PgDatabase db) {
        this.schema = schema;
        parent = null;
        depcies = new LinkedHashSet<>();
        this.db = db;
        systemStorage = PgSystemStorage.getObjectsFromResources(SupportedVersion.VERSION_9_5);
    }

    protected AbstractExpr(AbstractExpr parent) {
        this.schema = parent.schema;
        this.parent = parent;
        depcies = parent.depcies;
        this.db = parent.db;
        this.systemStorage = parent.systemStorage;
    }

    protected List<Entry<String, String>> findCte(String cteName) {
        return parent == null ? null : parent.findCte(cteName);
    }

    protected boolean hasCte(String cteName) {
        return findCte(cteName) != null;
    }

    /**
     * @param schema optional schema qualification of name, may be null
     * @param name alias of the referenced object
     * @param column optional referenced column alias, may be null
     * @return a pair of (Alias, Dealiased name) where Alias is the given name.
     *          Dealiased name can be null if the name is internal to the query
     *          and is not a reference to external table.<br>
     *          null if the name is not found
     */
    protected Entry<String, GenericColumn> findReference(String schema, String name, String column) {
        return parent == null ? null : parent.findReference(schema, name, column);
    }

    /**
     * @param schema optional schema qualification of name, may be null
     * @param name alias of the referenced object
     * @return a pair of (Alias, ColumnsList) where Alias is the given name.
     *          ColumnsList list of columns as pair 'columnName-columnType' of the internal query.<br>
     */
    protected Entry<String, List<Entry<String, String>>> findReferenceComplex(String name) {
        return parent == null ? null : parent.findReferenceComplex(name);
    }

    /**
     * @return {@link cz.startnet.utils.pgdiff.parsers.antlr.expr.secondanalyze.AbstractExprWithNmspc#cte
     * CTE names that current level of FROM has access to.}
     */
    protected Map<String, List<Entry<String, String>>> getAllCte() {
        return parent == null ? null : parent.getAllCte();
    }

    /**
     * @return {@link cz.startnet.utils.pgdiff.parsers.antlr.expr.secondanalyze.AbstractExprWithNmspc#unaliasedNamespace
     * unaliased namespaces}
     */
    protected Set<GenericColumn> getAllUnaliasedNmsp() {
        return parent == null ? null : parent.getAllUnaliasedNmsp();
    }

    /**
     * @return {@link cz.startnet.utils.pgdiff.parsers.antlr.expr.secondanalyze.AbstractExprWithNmspc#namespace
     * The local namespace of this Select.}
     */
    protected Map<String, GenericColumn> getAllReferences() {
        return parent == null ? null : parent.getAllReferences();
    }

    /**
     * @return {@link cz.startnet.utils.pgdiff.parsers.antlr.expr.secondanalyze.AbstractExprWithNmspc#complexNamespace
     * Map contains alias and list of pairs. Pairs returned by aliased subquery.}
     */
    protected Map<String, List<Entry<String, String>>> getAllReferencesComplex() {
        return parent == null ? null : parent.getAllReferencesComplex();
    }

    protected GenericColumn addObjectDepcy(List<IdentifierContext> ids, DbObjType type) {
        GenericColumn depcy = new GenericColumn(
                getSchemaNameForRelation(ids), QNameParser.getFirstName(ids), type);
        depcies.add(depcy);
        return depcy;
    }

    private String getSchemaNameForRelation(List<IdentifierContext> ids) {
        IdentifierContext schemaCtx = QNameParser.getSchemaNameCtx(ids);
        if (schemaCtx == null) {
            String tableName = QNameParser.getFirstName(ids);
            if (db.getSchema(schema).getTable(tableName) != null) {
                return schema;
            } else {
                if (systemStorage.getSchema(PgSystemStorage.SCHEMA_PG_CATALOG).getRelations()
                        .map(relation -> (PgSystemRelation)relation)
                        .anyMatch(sysRelation -> tableName.equals(sysRelation.getName()))) {
                    return PgSystemStorage.SCHEMA_PG_CATALOG;
                } else if (systemStorage.getSchema(PgSystemStorage.SCHEMA_INFORMATION_SCHEMA).getRelations()
                        .map(relation -> (PgSystemRelation)relation)
                        .anyMatch(sysRelation -> tableName.equals(sysRelation.getName()))) {
                    return PgSystemStorage.SCHEMA_INFORMATION_SCHEMA;
                } else {
                    return schema;
                }
            }
        } else {
            return schemaCtx.getText();
        }
    }

    private String getSchemaNameForFunction(IdentifierContext sch, String signature) {
        if (sch == null) {
            if (db.getSchema(schema).getFunction(signature) != null) {
                return schema;
            } else {
                if (systemStorage.getSchema(PgSystemStorage.SCHEMA_PG_CATALOG).getFunctions()
                        .stream().anyMatch(func -> signature.equals(func.getName()))) {
                    return PgSystemStorage.SCHEMA_PG_CATALOG;
                } else if (systemStorage.getSchema(PgSystemStorage.SCHEMA_INFORMATION_SCHEMA).getFunctions()
                        .stream().anyMatch(func -> signature.equals(func.getName()))) {
                    return PgSystemStorage.SCHEMA_INFORMATION_SCHEMA;
                } else {
                    return schema;
                }
            }
        } else {
            return sch.getText();
        }
    }

    protected GenericColumn addFunctionDepcy(Schema_qualified_name_nontypeContext funcNameCtx, String signature){
        GenericColumn depcy = new GenericColumn(getSchemaNameForFunction(funcNameCtx.schema, signature),
                signature, DbObjType.FUNCTION);
        depcies.add(depcy);
        return depcy;
    }

    protected void addTypeDepcy(Data_typeContext type) {
        Schema_qualified_name_nontypeContext typeName = type.predefined_type().schema_qualified_name_nontype();

        if (typeName != null) {
            IdentifierContext qual = typeName.identifier();
            String schema = qual == null ? this.schema : qual.getText();

            depcies.add(new GenericColumn(schema,
                    typeName.identifier_nontype().getText(), DbObjType.TYPE));
        }
    }

    /**
     * @return column with its type
     */
    protected Entry<String, String> addColumnDepcy(Schema_qualified_nameContext qname) {
        List<IdentifierContext> ids = qname.identifier();
        String columnName = QNameParser.getFirstName(ids);
        String columnType = TypesSetManually.COLUMN;
        String columnParent = null;
        Entry<String, String> pair = new SimpleEntry<>(columnName, null);

        if (ids.size() > 1) {
            String schemaName = QNameParser.getThirdName(ids);
            columnParent = QNameParser.getSecondName(ids);

            Entry<String, GenericColumn> ref = findReference(schemaName, columnParent, columnName);
            if (ref != null) {
                GenericColumn referencedTable = ref.getValue();
                if (referencedTable != null) {
                    columnParent = referencedTable.table;
                    GenericColumn genericColumn = new GenericColumn(referencedTable.schema, columnParent, columnName, DbObjType.COLUMN);
                    depcies.add(genericColumn);

                    columnType = getColumnType(genericColumn);
                } else {
                    Entry<String, List<Entry<String, String>>> refComplex = findReferenceComplex(columnParent);
                    if (refComplex != null) {
                        columnType = refComplex.getValue().stream()
                                .filter(entry -> columnName.equals(entry.getKey()))
                                .map(Entry::getValue)
                                .findAny().orElse(TypesSetManually.COLUMN);
                    }
                }
            } else {
                Log.log(Log.LOG_WARNING, "Unknown column reference: "
                        + schemaName + ' ' + columnParent + ' ' + columnName);
            }
        } else {
            // table-less columns analysis
            columnType = getTablelessColumnType(columnName);
        }

        pair.setValue(columnType);

        return pair;
    }

    private String getColumnType(GenericColumn genericColumn) {
        for (IRelation relation : (Iterable<IRelation>)findRelations(genericColumn.schema, genericColumn.table)::iterator) {
            for (Entry<String, String> colPair : (Iterable<Entry<String, String>>)relation.getRelationColumns()::iterator ) {
                if (genericColumn.column.equals(colPair.getKey())) {
                    return colPair.getValue();
                }
            }
        }
        return TypesSetManually.COLUMN;
    }

    private String getTablelessColumnType(String columnName) {
        String columnType;

        // In this case when table-less columns is column of cte.
        columnType = getTypeOfCorrespondingColComplex(columnName, getAllCte());
        if (columnType != null) {
            return columnType;
        }

        // In this case when table-less columns is column of unaliased namespace.
        for (GenericColumn gTableOrView : getAllUnaliasedNmsp()) {
            columnType = getTypeOfCorrespondingColTblOrView(columnName, gTableOrView);
            if (columnType != null) {
                return columnType;
            }
        }

        // In this case when table-less columns is column of aliased table or view.
        for (Entry<String, GenericColumn> nmsp : getAllReferences().entrySet()) {
            GenericColumn gTableOrView = nmsp.getValue();

            if (gTableOrView == null) {
                continue;
            }

            columnType = getTypeOfCorrespondingColTblOrView(columnName, gTableOrView);
            if (columnType != null) {
                return columnType;
            }
        }

        // In this case when table-less columns is column of subquery.
        columnType = getTypeOfCorrespondingColComplex(columnName, getAllReferencesComplex());
        if (columnType != null) {
            return columnType;
        }

        return TypesSetManually.COLUMN;
    }

    private String getTypeOfCorrespondingColComplex(String columnName, Map<String, List<Entry<String, String>>> colsOfAlias) {
        for (Entry<String, List<Entry<String, String>>> nmsp : colsOfAlias.entrySet()) {
            for (Entry<String, String> colPair : nmsp.getValue()) {
                if (columnName.equals(colPair.getKey())) {
                    return colPair.getValue();
                }
            }
        }
        return null;
    }

    private String getTypeOfCorrespondingColTblOrView(String columnName, GenericColumn gTableOrView) {
        for (Entry<String, String> colPair : getTableOrViewColumns(gTableOrView.schema, gTableOrView.table)) {
            if (columnName.equals(colPair.getKey())) {
                addColumnDepcy(gTableOrView.schema, gTableOrView.table, columnName);
                return colPair.getValue();
            }
        }
        return null;
    }

    protected void addColumnsDepcies(Schema_qualified_nameContext table, List<IdentifierContext> cols) {
        List<IdentifierContext> ids = table.identifier();
        String schemaName = QNameParser.getSchemaName(ids, schema);
        String tableName = QNameParser.getFirstName(ids);
        for (IdentifierContext col : cols) {
            depcies.add(new GenericColumn(schemaName, tableName, col.getText(), DbObjType.COLUMN));
        }
    }

    protected void addColumnsDepcies(String schemaName, String tableOrView, List<Entry<String, String>> cols) {
        String sName = schemaName != null ? schemaName : this.schema;
        for (Entry<String, String> col : cols) {
            depcies.add(new GenericColumn(sName, tableOrView, col.getKey(), DbObjType.COLUMN));
        }
    }

    protected void addColumnDepcy(String schemaName, String tableOrView, String columnName) {
        String sName = schemaName != null ? schemaName : this.schema;
        depcies.add(new GenericColumn(sName, tableOrView, columnName, DbObjType.COLUMN));
    }

    protected void addFunctionSigDepcy(String signature) {
        SQLParser p = AntlrParser.makeBasicParser(SQLParser.class, signature, "function signature");
        Function_args_parserContext sig = p.function_args_parser();
        List<IdentifierContext> ids = sig.schema_qualified_name().identifier();
        depcies.add(new GenericColumn(getSchemaNameForFunction(QNameParser.getSchemaNameCtx(ids), signature),
                PgDiffUtils.getQuotedName(QNameParser.getFirstName(ids)) +
                ParserAbstract.getFullCtxText(sig.function_args()),
                DbObjType.FUNCTION));
    }

    protected void addSchemaDepcy(List<IdentifierContext> ids) {
        depcies.add(new GenericColumn(QNameParser.getFirstName(ids), DbObjType.SCHEMA));
    }

    /**
     * Returns colPairs (name-type) from the 'tableOrView' of 'qualSchemaName' schema.
     */
    protected List<Entry<String, String>> getTableOrViewColumns(String qualSchemaName, String tableOrView) {
        List<Entry<String, String>> ret = new ArrayList<>();
        findRelations(qualSchemaName, tableOrView)
        .forEach(relation -> ret.addAll(relation.getRelationColumns().collect(Collectors.toList())));
        return ret;
    }

    protected Stream<IRelation> findRelations(String schemaName, String relationName) {
        Stream<IRelation> foundRelations;
        if (PgSystemStorage.SCHEMA_PG_CATALOG.equals(schemaName)
                || PgSystemStorage.SCHEMA_INFORMATION_SCHEMA.equals(schemaName)) {
            foundRelations = systemStorage.getSchema(schemaName).getRelations();
        } else if (schemaName != null) {
            foundRelations = db.getSchema(schemaName).getRelations();
        } else {
            foundRelations = Stream.concat(db.getSchema(schema).getRelations(),
                    systemStorage.getSchema(PgSystemStorage.SCHEMA_PG_CATALOG).getRelations());
        }

        return foundRelations.filter(r -> r.getName().equals(relationName));
    }
}