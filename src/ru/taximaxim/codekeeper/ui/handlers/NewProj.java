 
package ru.taximaxim.codekeeper.ui.handlers;

import java.io.IOException;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import ru.taximaxim.codekeeper.ui.UIConsts;
import ru.taximaxim.codekeeper.ui.pgdbproject.NewProjWizard;

public class NewProj {
	@Execute
	public void execute(
			@Named(UIConsts.PREF_STORE)
			IPreferenceStore prefStore,
			@Named(IServiceConstants.ACTIVE_SHELL)
			Shell shell,
			EPartService partService, EModelService model, MApplication app,
			IEventBroker events) throws IOException {
		NewProjWizard newProj = new NewProjWizard(prefStore);
		WizardDialog dialog = new WizardDialog(shell, newProj);
		dialog.create();
		if(dialog.open() == Dialog.OK) {
			LoadProj.load(newProj.getProject(), partService, model, app, events);
		}
	}
}