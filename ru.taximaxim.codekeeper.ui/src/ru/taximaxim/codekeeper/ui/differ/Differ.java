package ru.taximaxim.codekeeper.ui.differ;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableWithProgress;

import ru.taximaxim.codekeeper.apgdiff.ApgdiffConsts;
import ru.taximaxim.codekeeper.apgdiff.UnixPrintWriter;
import ru.taximaxim.codekeeper.ui.Log;
import ru.taximaxim.codekeeper.ui.PgCodekeeperUIException;
import ru.taximaxim.codekeeper.ui.UIConsts.PLUGIN_ID;
import ru.taximaxim.codekeeper.ui.localizations.Messages;
import cz.startnet.utils.pgdiff.PgDiff;
import cz.startnet.utils.pgdiff.PgDiffScript;
import cz.startnet.utils.pgdiff.loader.PgDumpLoader;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgStatement;

public class Differ implements IRunnableWithProgress {
    
    private static final int INITIAL_BUFFER_CAPACITY = 1024;

    private final DbSource dbSource, dbTarget;

    private boolean finished;
    private final boolean needTwoWay;
    private String diffDirect, diffReverse;
    private PgDiffScript script;
    private String timezone;
    
    private PgDatabase sourceDbFull;
    private PgDatabase targetDbFull;
    
    private List<Entry<PgStatement, PgStatement>> additionalDepciesSource;
    private List<Entry<PgStatement, PgStatement>> additionalDepciesTarget;

    public void setFullDbs(PgDatabase sourceDbFull, PgDatabase targetDbFull) {
       this.sourceDbFull = sourceDbFull;
       this.targetDbFull = targetDbFull;
    }
    
    public void setAdditionalDepciesSource(
            List<Entry<PgStatement, PgStatement>> additionalDepcies) {
        this.additionalDepciesSource = additionalDepcies;
    }
    
    public void setAdditionalDepciesTarget(
            List<Entry<PgStatement, PgStatement>> additionalDepcies) {
        this.additionalDepciesTarget = additionalDepcies;
    }
    
    public void addAdditionalDepciesSource(
            List<Entry<PgStatement, PgStatement>> additionalDepcies) {
        if (this.additionalDepciesSource == null) {
            setAdditionalDepciesSource(additionalDepcies);
        } else {
            this.additionalDepciesSource.addAll(additionalDepcies);
        }
    }
    
    public List<Entry<PgStatement, PgStatement>> getAdditionalDepciesSource() {
        return additionalDepciesSource;
    } 

    public Differ(DbSource dbSource, DbSource dbTarget, boolean needTwoWay, String timezone) {
        this.dbSource = dbSource;
        this.dbTarget = dbTarget;
        this.needTwoWay = needTwoWay;
        this.timezone = timezone;
    }
    
    public Job getDifferJob() {
        return new Job(Messages.differ_get_differ) {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    Differ.this.run(monitor);
                } catch (InvocationTargetException e) {
                    return new Status(Status.ERROR, PLUGIN_ID.THIS, 
                            Messages.error_in_the_project_modifier_thread, e);
                } catch (InterruptedException e) {
                    return Status.CANCEL_STATUS;
                }
                return Status.OK_STATUS;
            }
        };
    }
    
    public String getDiffDirect() throws PgCodekeeperUIException {
        checkFinished();
        return diffDirect;
    }
    
    public String getDiffReverse() throws PgCodekeeperUIException {
        checkFinished();
        return diffReverse;
    }

    /**
     * @return the script
     * @throws PgCodekeeperUIException 
     */
    public PgDiffScript getScript() throws PgCodekeeperUIException {
        checkFinished();
        return script;
    }
    
    private void checkFinished() throws PgCodekeeperUIException {
        if(!finished) {
            throw new PgCodekeeperUIException(Messages.runnable_has_not_finished);
        }
    }
    
    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
        SubMonitor pm = SubMonitor.convert(monitor, Messages.calculating_diff, 100); // 0
        
        PgDatabase dbSrc, dbTgt;
        dbSrc = dbTgt = null;
        try {
            dbSrc = this.dbSource.get(pm.newChild(25)); // 25
            dbTgt = this.dbTarget.get(pm.newChild(25)); // 50
        } catch(IOException ex) {
            throw new InvocationTargetException(ex, ex.getLocalizedMessage());
        }
        
        Log.log(Log.LOG_INFO, "Diff from: " + this.dbSource.getOrigin() //$NON-NLS-1$
                + " to: " + this.dbTarget.getOrigin()); //$NON-NLS-1$
        
        pm.newChild(25).subTask(Messages.differ_direct_diff); // 75
        ByteArrayOutputStream diffOut = new ByteArrayOutputStream(INITIAL_BUFFER_CAPACITY);
        try {
            PrintWriter writer = new UnixPrintWriter(
                    new OutputStreamWriter(diffOut, ApgdiffConsts.UTF_8), true);
            
            script = PgDiff.diffDatabaseSchemasAdditionalDepcies(writer,
                    DbSource.getPgDiffArgs(ApgdiffConsts.UTF_8, timezone), dbSrc, dbTgt,
                    sourceDbFull, targetDbFull, additionalDepciesSource,
                    additionalDepciesTarget);
            writer.flush();
            diffDirect = diffOut.toString(ApgdiffConsts.UTF_8).trim();

            if (needTwoWay) {
                Log.log(Log.LOG_INFO, "Diff from: " + this.dbTarget.getOrigin() //$NON-NLS-1$
                        + " to: " + this.dbSource.getOrigin()); //$NON-NLS-1$
                
                pm.newChild(25).subTask(Messages.differ_reverse_diff); // 100
                diffOut.reset();
                PgDiff.diffDatabaseSchemas(writer,
                        DbSource.getPgDiffArgs(ApgdiffConsts.UTF_8, timezone), dbTgt, dbSrc,
                        targetDbFull, sourceDbFull);
                writer.flush();
                diffReverse = diffOut.toString(ApgdiffConsts.UTF_8).trim();
            }
        } catch (UnsupportedEncodingException ex) {
            throw new InvocationTargetException(ex, ex.getLocalizedMessage());
        }
        
        PgDumpLoader.checkCancelled(pm);
        pm.done();
        finished = true;
    }
}