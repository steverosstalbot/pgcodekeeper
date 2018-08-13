package cz.startnet.utils.pgdiff;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import cz.startnet.utils.pgdiff.schema.PgDatabase;
import ru.taximaxim.codekeeper.apgdiff.ApgdiffTestUtils;
import ru.taximaxim.codekeeper.apgdiff.Log;
import ru.taximaxim.codekeeper.apgdiff.UnixPrintWriter;

/**
 * Tests for MS SQL statements
 *
 * @author galiev_mr
 */
@RunWith(value = Parameterized.class)
public class MsDiffTest {

    @Parameters
    public static Collection<?> parameters() {
        return Arrays.asList(
                new Object[][] {
                    // Tests scenario where MS PRIVILEGES for columns is added.
                    {"add_ms_column_privileges"},
                    // Tests scenario where MS COLUMN type is modified.
                    {"modify_ms_column_type"},
                    // Tests scenario where MS SEQUENCE cashe is modified.
                    {"modify_ms_sequence_cache"},
                    // Tests scenario where MS TABLE identity is modified.
                    {"modify_ms_table_identity"},

                    // Tests scenario where MS TABLE is added.
                    {"add_ms_table"},
                    // Tests scenario where MS TABLE is dropped.
                    {"drop_ms_table"},
                    // Tests scenario where MS TABLE is modified.
                    // {"modify_ms_table_..."},

                    // Tests scenario where MS VIEW is added.
                    {"add_ms_view"},
                    // Tests scenario where MS VIEW is dropped.
                    {"drop_ms_view"},
                    // Tests scenario where MS VIEW is modified.
                    // {"modify_ms_view"},

                    // TODO Uncomment the code when user-defined type will be supported.
                    // Tests scenario where MS TYPE is added.
                    // {"add_ms_type"},
                    // Tests scenario where MS TYPE is dropped.
                    // {"drop_ms_type"},
                    // Tests scenario where MS TYPE is modified.
                    // {"modify_ms_type"},

                    // Tests scenario where MS INDEX is added.
                    {"add_ms_index"},
                    // Tests scenario where MS INDEX is dropped.
                    {"drop_ms_index"},
                    // Tests scenario where MS INDEX definition is modified.
                    // {"modify_index"},

                    // Tests scenario where MS FUNCTION without args is added.
                    {"add_ms_function_noargs"},
                    // Tests scenario where MS FUNCTION without args is dropped.
                    {"drop_ms_function_noargs"},
                    // Tests scenario where MS FUNCTION without args is modified.
                    // {"modify_ms_function_noargs"},

                    // Tests scenario where MS FUNCTION with args is added.
                    {"add_ms_function_args"},
                    // Tests scenario where MS FUNCTION with args is dropped.
                    {"drop_ms_function_args"},
                    // Tests scenario where MS FUNCTION with args is modified.
                    // {"modify_ms_function_args"},

                    // Tests scenario where MS TABLE CONSTRAINT of column is added.
                    {"add_ms_constraint_column"},
                    // Tests scenario where MS TABLE CONSTRAINT of column is dropped.
                    {"drop_ms_constraint_column"},
                    // Tests scenario where MS TABLE CONSTRAINT of column is modified.
                    {"modify_ms_constraint_column"},

                    // Tests scenario where MS SEQUENCE is added.
                    {"add_ms_sequence"},
                    // Tests scenario where MS SEQUENCE is dropped.
                    // {"drop_ms_sequence"},
                    // Tests scenario where ... is modified on MS SEQUENCE.
                    // {"modify_ms_sequence_..."},



                    //// TODO add this tests:

                    // Tests scenario where MS TRIGGER is added.
                    // {"add_ms_trigger"},
                    // Tests scenario where MS TRIGGER is dropped.
                    // {"drop_ms_trigger"},
                    // Tests scenario where MS TRIGGER is modified.
                    // {"modify_ms_trigger"},

                    // Tests scenario where COLUMN is added to MS TABLE definition.
                    // {"add_ms_column"},
                    // Tests scenario where COLUMN is dropped from MS TABLE.
                    // {"drop_ms_column"},
                    // Tests scenario where COLUMN with index dependency is dropped from MS TABLE.
                    // {"drop_ms_column_with_idx"},
                    // Tests scenario where COLUMN of MS TABLE is modified.
                    // {"modify_ms_column"},


                    // test privileges
                    // tests for fts

                    // dependencies tests

                });
    }

    /**
     * Template name for file names that should be used for the test. Testing
     * method adds _original.sql, _new.sql and _diff.sql to the file name
     * template.
     */
    private final String fileNameTemplate;

    public MsDiffTest(final String fileNameTemplate) {
        this.fileNameTemplate = fileNameTemplate;
        Locale.setDefault(Locale.ENGLISH);
        Log.log(Log.LOG_DEBUG, fileNameTemplate);
    }

    public void runDiffSame(PgDatabase db) throws IOException, InterruptedException {
        final ByteArrayOutputStream diffInput = new ByteArrayOutputStream();
        final PrintWriter writer = new UnixPrintWriter(diffInput, true);
        final PgDiffArguments arguments = new PgDiffArguments();
        arguments.setMsSql(true);
        PgDiff.diffDatabaseSchemas(writer, arguments, db, db, null);
        writer.flush();

        Assert.assertEquals("File name template: " + fileNameTemplate, "", diffInput.toString().trim());
    }

    @Test
    public void runDiff() throws IOException, InterruptedException {
        PgDiffArguments args = new PgDiffArguments();
        args.setMsSql(true);

        PgDatabase dbOld = ApgdiffTestUtils.loadTestDump(
                fileNameTemplate + FILES_POSTFIX.ORIGINAL_SQL, MsDiffTest.class, args);
        PgDatabase dbNew = ApgdiffTestUtils.loadTestDump(
                fileNameTemplate + FILES_POSTFIX.NEW_SQL, MsDiffTest.class, args);

        runDiffSame(dbOld);
        runDiffSame(dbNew);

        final ByteArrayOutputStream diffInput = new ByteArrayOutputStream();
        final PrintWriter writer = new UnixPrintWriter(diffInput, true);
        PgDiff.diffDatabaseSchemas(writer, args, dbOld, dbNew, null);
        writer.flush();

        StringBuilder sbExpDiff;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                MsDiffTest.class.getResourceAsStream(fileNameTemplate
                        + FILES_POSTFIX.DIFF_SQL)))) {
            sbExpDiff = new StringBuilder(1024);

            String line;
            while ((line = reader.readLine()) != null) {
                sbExpDiff.append(line);
                sbExpDiff.append('\n');
            }
        }

        Assert.assertEquals("File name template: " + fileNameTemplate,
                sbExpDiff.toString().trim(),
                diffInput.toString().trim());
    }
}