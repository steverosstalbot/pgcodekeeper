package ru.taximaxim.codekeeper.ui.sqledit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.IHandler;
import org.eclipse.datatools.sqltools.sqlbuilder.views.source.SQLSourceEditingEnvironment;
import org.eclipse.datatools.sqltools.sqlbuilder.views.source.SQLSourceViewerConfiguration;
import org.eclipse.datatools.sqltools.sqleditor.internal.sql.ISQLPartitions;
import org.eclipse.datatools.sqltools.sqleditor.internal.sql.SQLCodeScanner;
import org.eclipse.datatools.sqltools.sqleditor.internal.sql.SQLPartitionScanner;
import org.eclipse.datatools.sqltools.sqleditor.internal.utils.SQLColorProvider;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.texteditor.ChangeEncodingAction;
import org.eclipse.ui.texteditor.DefaultRangeIndicator;
import org.eclipse.ui.texteditor.FindReplaceAction;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.IUpdate;

public class SqlSourceViewer extends SourceViewer 
    implements IMenuListener, ITextListener, ISelectionChangedListener {
    
    private static final String[] GLOBAL_ACTIONS= {
        ActionFactory.UNDO.getId(),
        ActionFactory.REDO.getId(),
        ActionFactory.CUT.getId(),
        ActionFactory.COPY.getId(),
        ActionFactory.PASTE.getId(),
        ActionFactory.DELETE.getId(),
        ActionFactory.SELECT_ALL.getId(),
        ActionFactory.FIND.getId(),
        ITextEditorActionDefinitionIds.LINE_GOTO
    };
    private static final String[] TEXT_ACTIONS= {
        SqlSourceViewer.UNDO_ID,
        SqlSourceViewer.REDO_ID,
        SqlSourceViewer.CUT_ID,
        SqlSourceViewer.COPY_ID,
        SqlSourceViewer.PASTE_ID,
        SqlSourceViewer.DELETE_ID,
        SqlSourceViewer.SELECT_ALL_ID,
        SqlSourceViewer.FIND_ID,
        SqlSourceViewer.GOTO_LINE_ID
    };
    public static final String UNDO_ID= "undo"; //$NON-NLS-1$
    public static final String REDO_ID= "redo"; //$NON-NLS-1$
    public static final String CUT_ID= "cut"; //$NON-NLS-1$
    public static final String COPY_ID= "copy"; //$NON-NLS-1$
    public static final String PASTE_ID= "paste"; //$NON-NLS-1$
    public static final String DELETE_ID= "delete"; //$NON-NLS-1$
    public static final String SELECT_ALL_ID= "selectAll"; //$NON-NLS-1$
    public static final String FIND_ID= "find"; //$NON-NLS-1$
    public static final String GOTO_LINE_ID= "gotoLine"; //$NON-NLS-1$
    public static final String CHANGE_ENCODING_ID= "changeEncoding"; //$NON-NLS-1$

    private FastPartitioner _partitioner = new FastPartitioner(
            new SQLPartitionScanner(), SQLPartitionScanner.SQL_PARTITION_TYPES);

    private IHandlerActivation contentAssistHandlerActivation;
    private IHandlerService handlerService;
    private HashMap<String, IAction> fActions= new HashMap<>();
    private List<IHandlerActivation> fActionHandlers = new ArrayList<>();
    
    public SqlSourceViewer(Composite parent, int style) {
        super(parent, new CompositeRuler(), 
                SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.BORDER | style);
        
        SQLSourceEditingEnvironment.connect();
        parent.addDisposeListener(new DisposeListener() {
            
            @Override
            public void widgetDisposed(DisposeEvent e) {
                freeObjects();
            }
        });
        
        this.setRangeIndicator(new DefaultRangeIndicator());
        this.configure(new SQLSourceViewerConfiguration() {
            
            @Override
            public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
                return SQLPartitionScanner.SQL_PARTITION_TYPES;
            }
            
            @Override
            public IPresentationReconciler getPresentationReconciler(
                    ISourceViewer sourceViewer) {
                SQLColorProvider colorProvider = new SQLColorProvider();
                SQLCodeScanner scanner = new SQLCodeScanner(colorProvider);
                scanner.setSQLSyntax(new SqlPostgresSyntax());
                
                PresentationReconciler reconciler = new PresentationReconciler();
                reconciler.setDocumentPartitioning(SQLPartitionScanner.SQL_PARTITIONING);
                
                for(String token : SQLPartitionScanner.SQL_PARTITION_TYPES) {
                    DefaultDamagerRepairer dr = new DefaultDamagerRepairer(scanner);
                    reconciler.setDamager(dr, token);
                    reconciler.setRepairer(dr, token);
                }
                
                return reconciler;
            }
            @Override
            protected IContentAssistant createAndInitContentAssistant() {
                // TODO переопределить метод, расширить класс SQLPartitionScanner
                // добавить выражения для постгресс и будет полная поддержка автоввода
                // пока только базовая некоторых выражений
                return super.createAndInitContentAssistant();
            }
        });
        
        this.getTextWidget().setFont(JFaceResources.getTextFont());
        
        handlerService = (IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class);
        
        MenuManager menu= new MenuManager();
        menu.setRemoveAllWhenShown(true);
        menu.addMenuListener(this);
        StyledText te= getSourceViewer().getTextWidget();
        te.setMenu(menu.createContextMenu(te));
        Menu menu1 = menu.createContextMenu(this.getControl());
        this.getControl().setMenu(menu1);
//        fContainer.registerContextMenu(menu, getSourceViewer());
        contributeFindAction();
//        connectGlobalActions();
    }
    
    private void freeObjects() {
        SQLSourceEditingEnvironment.disconnect();
        if (handlerService != null
                && contentAssistHandlerActivation != null) {
            handlerService.deactivateHandler(contentAssistHandlerActivation);
            clearHandlers();
        }
    }

    private void clearHandlers() {
        handlerService.deactivateHandlers(fActionHandlers);
        fActionHandlers.clear();
    }
    
    public void activateAutocomplete() {
        final SqlSourceViewer sqlEditor = this;
        IHandler caHandler = new AbstractHandler() {

        @Override
        public Object execute(ExecutionEvent event)
                throws org.eclipse.core.commands.ExecutionException {
            sqlEditor.doOperation(ISourceViewer.CONTENTASSIST_PROPOSALS);
            return null;
        }
        };
        if (contentAssistHandlerActivation != null) {
            handlerService.deactivateHandler(contentAssistHandlerActivation);
        }
        contentAssistHandlerActivation = handlerService.activateHandler(
                ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS,
        caHandler);
    }
    
    public void addLineNumbers() {
        LineNumberRulerColumn decorator = new LineNumberRulerColumn();
        decorator.setFont(JFaceResources.getTextFont());
        decorator.setForeground(this.getControl().getDisplay().getSystemColor(SWT.COLOR_GRAY));
        ((CompositeRuler) this.getVerticalRuler()).addDecorator(0, decorator);
    }
    
public void menuAboutToShow(IMenuManager menu) {
        
        menu.add(new Separator("undo")); //$NON-NLS-1$
        addMenu(menu, UNDO_ID);
        addMenu(menu, REDO_ID);
        menu.add(new Separator("ccp")); //$NON-NLS-1$
        addMenu(menu, CUT_ID);
        addMenu(menu, COPY_ID);
        addMenu(menu, PASTE_ID);
        addMenu(menu, DELETE_ID);
        addMenu(menu, SELECT_ALL_ID);
//        menu.add(new Separator("edit")); //$NON-NLS-1$
//        addMenu(menu, CHANGE_ENCODING_ID);
        menu.add(new Separator("find")); //$NON-NLS-1$
        addMenu(menu, FIND_ID);
        
        // update all actions
        // to get undo redo right
        updateActions();
}

private void contributeFindAction() {
    IAction action;
    action = new FindReplaceAction(getResourceBundle(), "Editor.FindReplace.", this.getControl().getShell(), getFindReplaceTarget()); //$NON-NLS-1$
    action.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE);
    this.addAction(SqlSourceViewer.FIND_ID, action);
}

private ResourceBundle getResourceBundle() {
    return ResourceBundle.getBundle("org.eclipse.compare.contentmergeviewer.TextMergeViewerResources");
}

/**
 * TODO Переход на линию, кодировка нужно реализовать интерфейс IAdaptable 
 */
/*private void contributeGotoLineAction(SqlSourceViewer viewer) {
    IAction action = new GotoLineAction((ITextEditor) viewer.getAdapter(ITextEditor.class));
    action.setActionDefinitionId(ITextEditorActionDefinitionIds.LINE_GOTO);
    viewer.addAction(SqlSourceViewer.GOTO_LINE_ID, action);
}

private void contributeChangeEncodingAction(SqlSourceViewer viewer) {
    IAction action = new ChangeEncodingAction(getTextEditorAdapter());
    viewer.addAction(SqlSourceViewer.CHANGE_ENCODING_ID, action);
}
*/
private void addMenu(IMenuManager menu, String actionId) {
    IAction action= getAction(actionId);
    if (action != null)
        menu.add(action);
}

public IAction getAction(String actionId) {
    IAction action= (IAction) fActions.get(actionId);
    if (action == null) {
        action= createAction(actionId);
        if (action == null)
            return null;
        if (action instanceof SqlViewerAction) {
            SqlViewerAction mva = (SqlViewerAction) action;
            if (mva.isContentDependent())
                getSourceViewer().addTextListener(this);
            if (mva.isSelectionDependent())
                getSourceViewer().addSelectionChangedListener(this);
            
            initAction(action, getResourceBundle(), "action." + actionId + ".");           //$NON-NLS-1$ //$NON-NLS-2$
        }
        addAction(actionId, action);
            
    }
    if (action instanceof SqlViewerAction) {
        SqlViewerAction mva = (SqlViewerAction) action;
        if (mva.isEditableDependent() && !getSourceViewer().isEditable())
            return null;
    }
    return action;
}

/*private void connectGlobalActions() {
    if (handlerService != null) {
        updateActions();
        clearHandlers();
        this.getControl().getDisplay().asyncExec(new Runnable() {
            public void run() {
                for (int i= 0; i < GLOBAL_ACTIONS.length; i++) {
                    IAction action= null;
                    action= getAction(TEXT_ACTIONS[i]);
//                    fHandlerService.setGlobalActionHandler(GLOBAL_ACTIONS[i], action);
                    if (action != null) {
                        fActionHandlers.add(handlerService.activateHandler(action.getActionDefinitionId(), new ActionHandler(action)));
                    }
                }
            }
        });
    }
}*/

public void textChanged(TextEvent event) {
    updateContentDependantActions();
}

void updateContentDependantActions() {
    Iterator<IAction> e= fActions.values().iterator();
    while (e.hasNext()) {
        Object next = e.next();
        if (next instanceof SqlViewerAction) {
            SqlViewerAction action = (SqlViewerAction) next;
            if (action.isContentDependent())
                action.update();
        }
    }
}

public void selectionChanged(SelectionChangedEvent event) {
    Iterator<IAction> e= fActions.values().iterator();
    while (e.hasNext()) {
        Object next = e.next();
        if (next instanceof SqlViewerAction) {
            SqlViewerAction action = (SqlViewerAction) next;
            if (action.isSelectionDependent())
                action.update();
        }
    }
}

/**
 * update all actions independent of their type
 *
 */
public void updateActions() {
    Iterator<IAction> e= fActions.values().iterator();
    while (e.hasNext()) {
        Object next = e.next();
        if (next instanceof SqlViewerAction) {
            SqlViewerAction action = (SqlViewerAction) next;
            action.update();
        } else if (next instanceof FindReplaceAction) {
            FindReplaceAction action = (FindReplaceAction) next;
            action.update();
        } else if (next instanceof ChangeEncodingAction) {
            ChangeEncodingAction action = (ChangeEncodingAction) next;
            action.update();
        }
    }
}

private SourceViewer getSourceViewer() {
    return this;
}

public void addAction(String id, IAction action) {
    fActions.put(id, action);
}

protected IAction createAction(String actionId) {
    if (UNDO_ID.equals(actionId))
        return new TextOperationAction(ITextOperationTarget.UNDO, IWorkbenchCommandConstants.EDIT_UNDO, true, false, true);
    if (REDO_ID.equals(actionId))
        return new TextOperationAction(ITextOperationTarget.REDO, IWorkbenchCommandConstants.EDIT_REDO, true, false, true);
    if (CUT_ID.equals(actionId))
        return new TextOperationAction(ITextOperationTarget.CUT, IWorkbenchCommandConstants.EDIT_CUT, true, true, false);
    if (COPY_ID.equals(actionId))
        return new TextOperationAction(ITextOperationTarget.COPY, IWorkbenchCommandConstants.EDIT_COPY, false, true, false);
    if (PASTE_ID.equals(actionId))
        return new TextOperationAction(ITextOperationTarget.PASTE, IWorkbenchCommandConstants.EDIT_PASTE, true, false, false);
    if (DELETE_ID.equals(actionId))
        return new TextOperationAction(ITextOperationTarget.DELETE, IWorkbenchCommandConstants.EDIT_DELETE, true, false, false);
    if (SELECT_ALL_ID.equals(actionId))
        return new TextOperationAction(ITextOperationTarget.SELECT_ALL, IWorkbenchCommandConstants.EDIT_SELECT_ALL, false, false, false);
    return null;
}

class TextOperationAction extends SqlViewerAction {
    
    private int fOperationCode;
    
    TextOperationAction(int operationCode, boolean mutable, boolean selection, boolean content) {
        this(operationCode, null, mutable, selection, content);
    }
    
    public TextOperationAction(int operationCode, String actionDefinitionId, boolean mutable, boolean selection, boolean content) {
        super(mutable, selection, content);
        if (actionDefinitionId != null)
            setActionDefinitionId(actionDefinitionId);
        fOperationCode= operationCode;
        update();
    }

    public void run() {
        if (isEnabled())
            getSourceViewer().doOperation(fOperationCode);
    }

    public boolean isEnabled() {
        return fOperationCode != -1 && getSourceViewer().canDoOperation(fOperationCode);
    }
    
    public void update() {
        setEnabled(isEnabled());
    }
}

public abstract class SqlViewerAction extends Action implements IUpdate {
    
    private boolean fMutable;
    private boolean fSelection;
    private boolean fContent;
    
    public SqlViewerAction(boolean mutable, boolean selection, boolean content) {
        fMutable= mutable;
        fSelection= selection;
        fContent= content;
    }

    public boolean isSelectionDependent() {
        return fSelection;
    }
    
    public boolean isContentDependent() {
        return fContent;
    }
    
    public boolean isEditableDependent() {
        return fMutable;
    }
    
    public void update() {
        // empty default implementation
    }
}

/*
 * Initialize the given Action from a ResourceBundle.
 */
public static void initAction(IAction a, ResourceBundle bundle, String prefix) {
    
    String labelKey= "label"; //$NON-NLS-1$
    String tooltipKey= "tooltip"; //$NON-NLS-1$
    String imageKey= "image"; //$NON-NLS-1$
    String descriptionKey= "description"; //$NON-NLS-1$
    
    if (prefix != null && prefix.length() > 0) {
        labelKey= prefix + labelKey;
        tooltipKey= prefix + tooltipKey;
        imageKey= prefix + imageKey;
        descriptionKey= prefix + descriptionKey;
    }
    
    a.setText(getString(bundle, labelKey, labelKey));
    a.setToolTipText(getString(bundle, tooltipKey, null));
    a.setDescription(getString(bundle, descriptionKey, null));
    
}
public static String getString(ResourceBundle bundle, String key, String dfltValue) {
    
    if (bundle != null) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException x) {
            // fall through
        }
    }
    return dfltValue;
}

    @Override
    protected void inputChanged(Object newInput, Object oldInput) {
        if (oldInput instanceof IDocumentExtension3) {
            IDocumentExtension3 doc = (IDocumentExtension3) oldInput;
            doc.setDocumentPartitioner(ISQLPartitions.SQL_PARTITIONING, null);
            _partitioner.disconnect();
        }
        
        if (newInput instanceof IDocumentExtension3) {
            IDocumentExtension3 extension3 = (IDocumentExtension3) newInput;
            _partitioner.connect((IDocument) newInput);
            extension3.setDocumentPartitioner(
                    ISQLPartitions.SQL_PARTITIONING, _partitioner);
        }
        super.inputChanged(newInput, oldInput);
    }
}
