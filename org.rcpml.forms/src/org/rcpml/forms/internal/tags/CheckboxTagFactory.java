package org.rcpml.forms.internal.tags;

import org.eclipse.swt.SWT;
import org.rcpml.core.IController;
import org.rcpml.core.RCPMLException;
import org.rcpml.core.bridge.AbstractBridgeFactory;
import org.rcpml.core.bridge.IBridge;
import org.w3c.dom.Node;

public class CheckboxTagFactory extends AbstractBridgeFactory {

	private static class CheckBridge extends AbstractEclipseFormsButtonBridge {
		protected CheckBridge(Node node, IController controller ) {
			super( node, controller );
		}

		protected int getStyle() {
			return SWT.CHECK;
		}
	}
	public CheckboxTagFactory() {
		super();	
	}

	public IBridge createBridge(Node node) throws RCPMLException {
		return new CheckBridge( node, this.getController() );
	}

}