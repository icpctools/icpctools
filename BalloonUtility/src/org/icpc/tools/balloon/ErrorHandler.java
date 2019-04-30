package org.icpc.tools.balloon;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.icpc.tools.contest.Trace;

public class ErrorHandler {
	protected static Shell shell;

	public static void setShell(Shell newShell) {
		shell = newShell;
	}

	public static void error(String s) {
		error(s, null);
	}

	public static void error(final String s, final Throwable t) {
		Trace.trace(Trace.ERROR, s, t);

		if (shell != null && !shell.isDisposed()) {
			shell.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					MessageBox dialog = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
					dialog.setText(shell.getText());
					if (t != null)
						dialog.setMessage(s + ": " + t.getMessage());
					else
						dialog.setMessage(s);
					dialog.open();
				}
			});
		}
	}
}