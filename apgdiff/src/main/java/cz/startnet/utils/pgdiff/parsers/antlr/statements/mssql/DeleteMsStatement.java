package cz.startnet.utils.pgdiff.parsers.antlr.statements.mssql;

import java.util.Arrays;

import cz.startnet.utils.pgdiff.parsers.antlr.TSQLParser.Delete_statementContext;
import cz.startnet.utils.pgdiff.parsers.antlr.TSQLParser.Qualified_nameContext;
import cz.startnet.utils.pgdiff.parsers.antlr.statements.ParserAbstract;
import cz.startnet.utils.pgdiff.schema.GenericColumn;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;
import ru.taximaxim.codekeeper.apgdiff.utils.Pair;

public class DeleteMsStatement extends ParserAbstract {

    private final Delete_statementContext ctx;

    public DeleteMsStatement(Delete_statementContext ctx, PgDatabase db) {
        super(db);
        this.ctx = ctx;
    }

    @Override
    public void parseObject() {
        Qualified_nameContext qname = ctx.qualified_name();
        if (qname != null) {
            addObjReference(Arrays.asList(qname.schema, qname.name),
                    DbObjType.TABLE, ACTION_DELETE);
        }
    }

    @Override
    protected Pair<String, GenericColumn> getActionAndObjForStmtAction() {
        Qualified_nameContext qname = ctx.qualified_name();
        return new Pair<>(ACTION_DELETE, new GenericColumn(
                qname.schema.getText(), qname.name.getText(), DbObjType.TABLE));
    }
}
