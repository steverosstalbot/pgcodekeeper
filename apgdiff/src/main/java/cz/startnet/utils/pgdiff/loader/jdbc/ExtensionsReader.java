package cz.startnet.utils.pgdiff.loader.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import cz.startnet.utils.pgdiff.PgDiffUtils;
import cz.startnet.utils.pgdiff.loader.JdbcQueries;
import cz.startnet.utils.pgdiff.schema.GenericColumn;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgExtension;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;

public class ExtensionsReader implements PgCatalogStrings {

    private final JdbcLoaderBase loader;
    private final PgDatabase db;

    public ExtensionsReader(JdbcLoaderBase loader, PgDatabase db) {
        this.loader = loader;
        this.db = db;
    }

    public void read() throws SQLException, InterruptedException {
        loader.setCurrentOperation("extensions query");
        String query = JdbcQueries.QUERY_EXTENSIONS.getQuery();

        if (loader.getExtensionSchema() != null) {
            /*
            Collection<ObjectTimestamp> objects = loader.getTimestampOldObjects();
            PgDatabase snapshot = loader.getTimestampSnapshot();

            for (ObjectTimestamp obj : objects) {
                if (obj.getType() == DbObjType.EXTENSION) {
                    db.addExtension((PgExtension) obj.copyStatement(snapshot, loader));
                }
            }
            objects.removeIf(obj -> obj.getType() == DbObjType.EXTENSION);
            query = JdbcReader.excludeObjects(query,
                    loader.getExtensionSchema(), loader.getTimestampLastDate());
             */

            query = JdbcReader.appendTimestamps(query, loader.getExtensionSchema());

        }

        try (ResultSet res = loader.runner.runScript(loader.statement, query)) {
            while (res.next()) {
                PgExtension extension = getExtension(res);
                db.addExtension(extension);
                loader.setAuthor(extension, res);
            }
        }
    }

    private PgExtension getExtension(ResultSet res) throws SQLException {
        String extName = res.getString("extname");
        loader.setCurrentObject(new GenericColumn(extName, DbObjType.EXTENSION));
        PgExtension e = new PgExtension(extName);
        e.setSchema(res.getString("namespace"));
        e.addDep(new GenericColumn(e.getSchema(), DbObjType.SCHEMA));

        String comment = res.getString("description");
        if (comment != null && !comment.isEmpty()) {
            e.setComment(loader.args, PgDiffUtils.quoteString(comment));
        }
        return e;
    }
}
