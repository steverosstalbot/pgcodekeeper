/**
 * Copyright 2006 StartNet s.r.o.
 *
 * Distributed under MIT license
 */
package cz.startnet.utils.pgdiff;

import java.util.ArrayList;
import java.util.List;

import cz.startnet.utils.pgdiff.schema.PgIndex;
import cz.startnet.utils.pgdiff.schema.PgSchema;
import cz.startnet.utils.pgdiff.schema.PgTable;

/**
 * Diffs indexes.
 *
 * @author fordfrog
 */
public class PgDiffIndexes {

    /**
     * Outputs statements for creation of new indexes.
     *
     * @param writer           writer the output should be written to
     * @param oldSchema        original schema
     * @param newSchema        new schema
     * @param searchPathHelper search path helper
     */
    public static void createIndexes(final PgDiffScript script,
            final PgSchema oldSchema, final PgSchema newSchema,
            final SearchPathHelper searchPathHelper) {
        for (final PgTable newTable : newSchema.getTables()) {
            final String newTableName = newTable.getName();

            // Add new indexes
            if (oldSchema == null) {
                for (PgIndex index : newTable.getIndexes()) {
                    searchPathHelper.outputSearchPath(script);
                    PgDiff.writeCreationSql(script, null, index);
                }
            } else {
                for (PgIndex index : getNewIndexes(oldSchema.getTable(newTableName), newTable)) {
                    searchPathHelper.outputSearchPath(script);
                    PgDiff.writeCreationSql(script, null, index);
                }
            }
        }
    }

    /**
     * Outputs statements for dropping indexes that exist no more.
     *
     * @param writer           writer the output should be written to
     * @param oldSchema        original schema
     * @param newSchema        new schema
     * @param searchPathHelper search path helper
     */
    public static void dropIndexes(final PgDiffScript script,
            final PgSchema oldSchema, final PgSchema newSchema,
            final SearchPathHelper searchPathHelper) {
        for (final PgTable newTable : newSchema.getTables()) {
            final String newTableName = newTable.getName();
            final PgTable oldTable;

            if (oldSchema == null) {
                oldTable = null;
            } else {
                oldTable = oldSchema.getTable(newTableName);
            }

            // Drop indexes that do not exist in new schema or are modified
            for (final PgIndex index : getDropIndexes(oldTable, newTable)) {
                searchPathHelper.outputSearchPath(script);
                PgDiff.writeDropSql(script, null, index);
            }
        }
        
        // КОСТЫЛЬ
        if (oldSchema == null){
            return;
        }
        
        for (final PgTable oldTable : oldSchema.getTables()) {
            if (newSchema.getTable(oldTable.getName()) == null && !PgDiff.isFullSelection(oldTable)) {
                PgTable newTable = new PgTable(oldTable.getName(), null, null);
                for (final PgIndex index : getDropIndexes(oldTable, newTable)) {
                    searchPathHelper.outputSearchPath(script);
                    PgDiff.writeDropSql(script, null, index);
                }
            }
        }// КОСТЫЛЬ
    }

    /**
     * Returns list of indexes that should be dropped.
     *
     * @param oldTable original table
     * @param newTable new table
     *
     * @return list of indexes that should be dropped
     *
     * @todo Indexes that are depending on a removed field should not be added
     * to drop because they are already removed.
     */
    private static List<PgIndex> getDropIndexes(final PgTable oldTable,
            final PgTable newTable) {
        final List<PgIndex> list = new ArrayList<PgIndex>();

        if (newTable != null && oldTable != null) {
            for (final PgIndex index : oldTable.getIndexes()) {
                if (!newTable.containsIndex(index.getName())
                        || !newTable.getIndex(index.getName()).equals(index)) {
                    list.add(index);
                }
            }
        }

        return list;
    }

    /**
     * Returns list of indexes that should be added.
     *
     * @param oldTable original table
     * @param newTable new table
     *
     * @return list of indexes that should be added
     */
    private static List<PgIndex> getNewIndexes(final PgTable oldTable,
            final PgTable newTable) {
        final List<PgIndex> list = new ArrayList<PgIndex>();

        if (newTable != null) {
            if (oldTable == null) {
                for (final PgIndex index : newTable.getIndexes()) {
                    list.add(index);
                }
            } else {
                for (final PgIndex index : newTable.getIndexes()) {
                    if (!oldTable.containsIndex(index.getName())
                            || !oldTable.getIndex(index.getName()).
                            equals(index)) {
                        list.add(index);
                    }
                }
            }
        }

        return list;
    }

    /**
     * Outputs statements for index comments that have changed.
     *
     * @param writer           writer
     * @param oldSchema        old schema
     * @param newSchema        new schema
     * @param searchPathHelper search path helper
     */
    public static void alterComments(final PgDiffScript script,
            final PgSchema oldSchema, final PgSchema newSchema,
            final SearchPathHelper searchPathHelper) {
        if (oldSchema == null) {
            return;
        }

        for(PgTable oldTable : oldSchema.getTables()) {
            final PgTable newTable = newSchema.getTable(oldTable.getName());

            if (newTable == null) {
                continue;
            }
            
            for (final PgIndex oldIndex : oldTable.getIndexes()) {
                final PgIndex newIndex = newTable.getIndex(oldIndex.getName());
                
                if (newIndex == null) {
                    continue;
                }
    
                if (oldIndex.getComment() == null
                        && newIndex.getComment() != null
                        || oldIndex.getComment() != null
                        && newIndex.getComment() != null
                        && !oldIndex.getComment().equals(
                        newIndex.getComment())) {
                    searchPathHelper.outputSearchPath(script);
                    script.addStatement("COMMENT ON INDEX "
                            + PgDiffUtils.getQuotedName(newIndex.getName())
                            + " IS " + newIndex.getComment() + ';');
                } else if (oldIndex.getComment() != null
                        && newIndex.getComment() == null) {
                    searchPathHelper.outputSearchPath(script);
                    script.addStatement("COMMENT ON INDEX "
                            + PgDiffUtils.getQuotedName(newIndex.getName())
                            + " IS NULL;");
                }
            }
        }
    }

    /**
     * Creates a new instance of PgDiffIndexes.
     */
    private PgDiffIndexes() {
    }
}
