package ru.taximaxim.codekeeper.ui.perspectives;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextService;

import ru.taximaxim.codekeeper.ui.UIConsts;
import ru.taximaxim.codekeeper.ui.UIConsts.PART;
import ru.taximaxim.codekeeper.ui.UIConsts.PART_STACK;

public class MainPersp implements IPerspectiveFactory {

    @Override
    public void createInitialLayout(IPageLayout layout) {
        layout.setEditorAreaVisible(false);
        
        // TODO perspectiveExtensions is the declarative method for this 
        
        IFolderLayout left = layout.createFolder(PART_STACK.PROJXP,
                IPageLayout.LEFT, 0.2f, layout.getEditorArea());
        
        left.addView("org.eclipse.jdt.ui.PackageExplorer");
        layout.getViewLayout("org.eclipse.jdt.ui.PackageExplorer").setCloseable(false);

        IFolderLayout main = layout.createFolder(PART_STACK.EDITORS,
                IPageLayout.TOP, 0.8f, layout.getEditorArea());
        
        main.addView(PART.WELCOME);
        layout.getViewLayout(PART.WELCOME).setCloseable(false);
        
        main.addPlaceholder(PART.SYNC + ":*"); //$NON-NLS-1$
        main.addPlaceholder(PART.DIFF + ":*"); //$NON-NLS-1$
        main.addPlaceholder(PART.SQL_EDITOR + ":*"); //$NON-NLS-1$
        
        IFolderLayout bottom = layout.createFolder(PART_STACK.CONSOLE,
                IPageLayout.BOTTOM, 0.8f, PART_STACK.EDITORS);
        
        bottom.addView(PART.CONSOLE);
        layout.getViewLayout(PART.CONSOLE).setCloseable(false);
        
        IContextService contextService = (IContextService) PlatformUI
                .getWorkbench().getService(IContextService.class);
        if (contextService != null) {
            contextService.activateContext(UIConsts.MAIN_CONTEXT);
        }
    }
}
