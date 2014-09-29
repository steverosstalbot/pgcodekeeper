 
package ru.taximaxim.codekeeper.ui.parts;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.menus.IMenuService;

import ru.taximaxim.codekeeper.apgdiff.ApgdiffConsts;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.TreeElement;
import ru.taximaxim.codekeeper.apgdiff.model.exporter.ModelExporter;
import ru.taximaxim.codekeeper.ui.Activator;
import ru.taximaxim.codekeeper.ui.UIConsts.EVENT;
import ru.taximaxim.codekeeper.ui.UIConsts.FILE;
import ru.taximaxim.codekeeper.ui.UIConsts.MENU;
import ru.taximaxim.codekeeper.ui.differ.DbSource;
import ru.taximaxim.codekeeper.ui.differ.TreeDiffer;
import ru.taximaxim.codekeeper.ui.localizations.Messages;
import ru.taximaxim.codekeeper.ui.pgdbproject.PgDbProject;
import cz.startnet.utils.pgdiff.schema.PgDatabase;

public class ProjectExplorer {
    
    @Inject
    private Composite parent;
    @Inject
    private PgDbProject proj;
    @Inject
    private MPart part;
    @Inject
    private UISynchronize sync;
    @Inject
    private IMenuService menuService;
    @Inject
    private IWorkbenchPage page;
    
    private LocalResourceManager lrm;
    // key-value pairs to store hash vs db object name
    private Map<String, String> fileNamesHash = new HashMap<>();

    private TreeViewer treeDb;
    
    @PostConstruct
    private void postConstruct()
            throws IOException, InterruptedException, InvocationTargetException {
        parent.setLayout(new FillLayout());
        this.lrm = new LocalResourceManager(JFaceResources.getResources(), parent);

        treeDb = new TreeViewer(parent, SWT.BORDER);
        treeDb.setContentProvider(new ITreeContentProvider() {
            
            private final List<String> ignoredFiles = Arrays.asList(
                    new String[] {
                            ".git", //$NON-NLS-1$
                            ApgdiffConsts.FILENAME_WORKING_DIR_MARKER
                    });
            
            private final FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return !ignoredFiles.contains(name);
                }
            };
            
            @Override
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }
            
            @Override
            public void dispose() {
            }
            
            @Override
            public boolean hasChildren(Object element) {
                File f = (File) element;
                return f.isDirectory() && (f.list().length != 0);
            }
            
            @Override
            public Object getParent(Object element) {
                return ((File) element).getParentFile();
            }
            
            @Override
            public Object[] getElements(Object inputElement) {
                File[] files = ((File) inputElement).listFiles(filter);
                Arrays.sort(files, new Comparator<File>() {
                    @Override
                    public int compare(File o1, File o2) {
                        if(o1.isDirectory()) {
                            return o2.isDirectory()? getObjNameFromHash(o1).compareTo(
                                    getObjNameFromHash(o2)) : -1;
                        } else if(o2.isDirectory()) {
                            return 1;
                        } else {
                            return getObjNameFromHash(o1).compareTo(getObjNameFromHash(o2));
                        }
                    }
                });
                return files;
            }
            
            @Override
            public Object[] getChildren(Object parentElement) {
                if(((File) parentElement).isDirectory()) {
                    return this.getElements(parentElement);
                }
                
                return null;
            }
        });
        treeDb.setLabelProvider(new LabelProvider() {
            
            private final Image imgFolder = lrm.createImage(
                    ImageDescriptor.createFromURL(Activator.getContext()
                            .getBundle().getResource(FILE.ICONDIR)));
            
            private final Image imgFile = lrm.createImage(
                    ImageDescriptor.createFromURL(Activator.getContext()
                            .getBundle().getResource(FILE.ICONFILE)));
            
            @Override
            public String getText(Object element) {
                return getObjNameFromHash((File) element);
            }
            
            @Override
            public Image getImage(Object element) {
                if(((File) element).isDirectory()) {
                    return imgFolder;
                }

                return imgFile;
            }
        });
        treeDb.addDoubleClickListener(new IDoubleClickListener() {
            
            @Override
            public void doubleClick(DoubleClickEvent event) {
                TreePath []p = ((TreeSelection) event.getSelection()).getPaths();
                TreePath path = p[0];
                final File f = (File) path.getLastSegment();
                
                if(f.isDirectory()) {
                    TreeViewer viewer = (TreeViewer)event.getViewer();
                    viewer.setExpandedState(path, !viewer.getExpandedState(path));
                    viewer.refresh();
                } else {
                    SqlEditorDescr.openNew(f, getObjNameFromHash(f), page);
                }
            }
        });
        
        MenuManager mm = new MenuManager();
        menuService.populateContributionManager(mm, MENU.PROJXP_TREE_POPUP);
        treeDb.getControl().setMenu(mm.createContextMenu(treeDb.getControl()));
        
        changeProject(proj, proj);
    }
    
    private String getObjNameFromHash(File f) {
        String filename = f.getName();
        final String extension = ".sql"; //$NON-NLS-1$
        int indexOfExt = filename.lastIndexOf(extension);
        if (indexOfExt != -1 && filename.length() - indexOfExt == extension.length()){
            filename = filename.substring(0, indexOfExt);
        }
        String res = fileNamesHash.get(filename);
        if(res == null){
            res = filename;
        }
        return res;
    }
    
    private void initialHash(PgDbProject proj, SubMonitor monitor) 
            throws InvocationTargetException {
        SubMonitor pm = SubMonitor.convert(monitor, 10);
        DbSource src = DbSource.fromProject(proj);
        TreeDiffer differ = new TreeDiffer(src, new DbSource("empty db") { //$NON-NLS-1$
            
            @Override
            protected PgDatabase loadInternal(SubMonitor monitor) {
                SubMonitor.convert(monitor, 1).newChild(1);
                return new PgDatabase();
            }
        });
        
        differ.run(pm.newChild(6));
        
        pm.newChild(4).subTask(Messages.projectExplorer_generating_object_hashes);
        visit(differ.getDiffTree(), src.getDbObject());
    }
    
    private void visit(TreeElement subtree, PgDatabase source) {
        if (subtree.hasChildren()) {
            for (TreeElement child : subtree.getChildren()) {
                visit(child, source);
            }
        }
        if (subtree.getType() != TreeElement.DbObjType.CONTAINER
                && subtree.getType() != TreeElement.DbObjType.DATABASE) {
            fileNamesHash.put(
                    ModelExporter.getExportedFilename(subtree.getPgStatement(source)),
                    subtree.getName());
        }
    }

    @Inject
    private void changeProject(
            final PgDbProject proj,
            @Optional
            @EventTopic(EVENT.REOPEN_PROJECT)
            PgDbProject proj2)
            throws InvocationTargetException, InterruptedException {
        if (treeDb != null) {
            final File treeInput = proj == null? null : proj.getPathToProject().toFile();
            
            if(proj != null) {
                IRunnableWithProgress loadRunnable = new IRunnableWithProgress() {
                    
                    @Override
                    public void run(IProgressMonitor monitor)
                            throws InvocationTargetException, InterruptedException {
                        SubMonitor pm = SubMonitor.convert(monitor, Messages.projectExplorer_loading_project, 10);
                        initialHash(proj, pm.newChild(10));
                        
                        pm.done();
                    }
                };
                new ProgressMonitorDialog(parent.getShell())
                        .run(true, false, loadRunnable);
            }
            sync.asyncExec(new Runnable() {
                
                @Override
                public void run() {
                    treeDb.setInput(treeInput);
                }
            });
        }
        
        String partLabel = Messages.projectExplorer_project_explorer;
        if (proj != null) {
            partLabel += " - " + proj.getProjectName(); //$NON-NLS-1$
        }
        final String newLabel = partLabel;
        sync.asyncExec(new Runnable() {
            
            @Override
            public void run() {
                part.setLabel(newLabel);
            }
        });
    }
}
