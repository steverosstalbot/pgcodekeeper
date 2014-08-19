/**
 * Copyright 2006 StartNet s.r.o.
 *
 * Distributed under MIT license
 */
package cz.startnet.utils.pgdiff;

import cz.startnet.utils.pgdiff.schema.PgFunction;
import cz.startnet.utils.pgdiff.schema.PgSchema;
import cz.startnet.utils.pgdiff.schema.PgSequence;

/**
 * Diffs functions.
 *
 * @author fordfrog
 */
public class PgDiffFunctions {

    /**
     * Outputs statements for new or modified functions.
     *
     * @param writer           writer the output should be written to
     * @param arguments        object containing arguments settings
     * @param oldSchema        original schema
     * @param newSchema        new schema
     * @param searchPathHelper search path helper
     */
    public static void createFunctions(final PgDiffScript script,
            final PgDiffArguments arguments, final PgSchema oldSchema,
            final PgSchema newSchema, final SearchPathHelper searchPathHelper) {
        // Add new functions and replace modified functions
        for (final PgFunction newFunction : newSchema.getFunctions()) {
            final PgFunction oldFunction;

            if (oldSchema == null) {
                oldFunction = null;
            } else {
                oldFunction = oldSchema.getFunction(newFunction.getSignature());
            }

            if ((oldFunction == null) || !newFunction.equalsWhitespace(
                    oldFunction, arguments.isIgnoreFunctionWhitespace())) {
                PgFunction fullFunc = PgDiff.dbNew.getSchema(newSchema.getName()).getFunction(newFunction.getName());
                PgDiff.addUniqueTableDependenciesOnCreateEdit(script, fullFunc);
                
                searchPathHelper.outputSearchPath(script);
                PgDiff.writeCreationSql(script, null, newFunction);
            }
        }
    }

    /**
     * Outputs statements for dropping of functions that exist no more.
     *
     * @param writer           writer the output should be written to
     * @param oldSchema        original schema
     * @param newSchema        new schema
     * @param searchPathHelper search path helper
     */
    public static void dropFunctions(final PgDiffScript script,
            final PgSchema oldSchema, final PgSchema newSchema,
            final SearchPathHelper searchPathHelper) {
        if (oldSchema == null) {
            return;
        }

        // Drop functions that exist no more
        for (final PgFunction oldFunction : oldSchema.getFunctions()) {
            if (!newSchema.containsFunction(oldFunction.getSignature())) {
                searchPathHelper.outputSearchPath(script);
                PgDiff.writeDropSql(script, null, oldFunction);
            }
        }
    }

    /**
     * Outputs statements for function comments that have changed.
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

        for (final PgFunction oldfunction : oldSchema.getFunctions()) {
            final PgFunction newFunction =
                    newSchema.getFunction(oldfunction.getSignature());

            if (newFunction == null) {
                continue;
            }

            if (oldfunction.getComment() == null
                    && newFunction.getComment() != null
                    || oldfunction.getComment() != null
                    && newFunction.getComment() != null
                    && !oldfunction.getComment().equals(
                    newFunction.getComment())) {
                searchPathHelper.outputSearchPath(script);
                
                StringBuilder sb = new StringBuilder();
                sb.append("COMMENT ON FUNCTION ");
                newFunction.appendFunctionSignature(sb, false);
                sb.append(" IS ");
                sb.append(newFunction.getComment());
                sb.append(';');
                script.addStatement(sb.toString());
            } else if (oldfunction.getComment() != null
                    && newFunction.getComment() == null) {
                searchPathHelper.outputSearchPath(script);
                
                StringBuilder sb = new StringBuilder();
                sb.append("COMMENT ON FUNCTION ");
                newFunction.appendFunctionSignature(sb, false);
                sb.append(" IS NULL;");
                script.addStatement(sb.toString());
            }
        }
    }

    /**
     * Creates a new instance of PgDiffFunctions.
     */
    private PgDiffFunctions() {
    }
}
