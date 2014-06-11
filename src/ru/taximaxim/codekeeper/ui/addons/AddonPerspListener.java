 
package ru.taximaxim.codekeeper.ui.addons;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PerspectiveAdapter;

import ru.taximaxim.codekeeper.ui.UIConsts;
import ru.taximaxim.codekeeper.ui.pgdbproject.PgDbProject;

public class AddonPerspListener {

    @Inject
    private IEventBroker events;
    
    @Inject
    @Optional
    private void resetMainOnActivation(
            @EventTopic(UIEvents.UILifeCycle.APP_STARTUP_COMPLETE)
            MApplication app,
            @Named(ISources.ACTIVE_WORKBENCH_WINDOW_NAME)
            IWorkbenchWindow wbw,
            final PgDbProject proj) {
        wbw.addPerspectiveListener(new PerspectiveAdapter() {
            
            @Override
            public void perspectiveActivated(IWorkbenchPage page,
                    IPerspectiveDescriptor perspective) {
                if (perspective.getId().equals(UIConsts.PERSP_MAIN_ID)) {
                    events.send(UIConsts.EVENT_REOPEN_PROJECT, proj);
                }
            }
        });
    }
}