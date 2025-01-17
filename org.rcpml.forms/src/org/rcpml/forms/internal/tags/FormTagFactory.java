package org.rcpml.forms.internal.tags;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.rcpml.core.IController;
import org.rcpml.core.RCPMLException;
import org.rcpml.core.RCPMLTagConstants;
import org.rcpml.core.bridge.AbstractBridgeFactory;
import org.rcpml.core.bridge.IBridge;
import org.rcpml.core.css.RCPCSSConstants;
import org.rcpml.core.dom.RCPStylableElement;
import org.rcpml.forms.internal.AbstractEclipseFormsBridge;
import org.rcpml.forms.internal.EclipseFormsUtil;
import org.w3c.dom.Node;

public class FormTagFactory extends AbstractBridgeFactory {

	private static class FormBridge extends AbstractEclipseFormsBridge {
		private static final String TITLE_ATTR = RCPMLTagConstants.TITLE_ATTR;
		private static final String SCROLL_ATTR = RCPMLTagConstants.SCROLL_ATTR;		
		private static final String TRUE_STATE = RCPCSSConstants.TRUE_VALUE;	
		
		private Form fForm;
		private ScrolledForm fSForm;
		public FormBridge(Node node, IController controller) {
			super(node, controller, true );		
		}

		protected void construct(Composite parent) {
			String scroll = this.getAttribute( SCROLL_ATTR );			
			if( scroll.equals( TRUE_STATE ) ) {
				this.fSForm = this.getFormToolkit().createScrolledForm(parent);
				this.fForm = this.fSForm.getForm();
			}			
			if( this.fForm == null ) {
				this.fForm = this.getFormToolkit().createForm(parent);
			}
			String text = this.getAttribute( TITLE_ATTR );
			this.fForm.setText(text);
			getFormToolkit().decorateFormHeading(fForm);
						
			this.fForm.getBody().setLayout( EclipseFormsUtil.constructLayout((RCPStylableElement)this.getNode()) );
			this.fForm.setLayoutData(this.constructLayoutData(this.fForm.getParent()));
			
			fForm.addDisposeListener( new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					disposeDataBinding();
				}			
			});
		}		

		public Object getPresentation() {
			if( this.fSForm != null ) {
				return this.fSForm.getBody();
			}
			if( this.fForm != null ) {
				return this.fForm.getBody();
			}
			return null;
		}

		public void update() {
			this.fForm.getBody().layout();
			if( this.fSForm != null ) {
				this.fSForm.layout();
				this.fSForm.reflow(true);	
			}
			this.fForm.layout();
		}
		public void parentUpdate() {
			update();
			super.parentUpdate();
		}
	}
	
	public FormTagFactory() {
		super();
	}

	public IBridge createBridge(Node node) throws RCPMLException {
		return new FormBridge( node, this.getController() );
	}
}
