package org.rcpml.core.internal;

import java.util.HashMap;
import java.util.Map;

import org.apache.batik.css.engine.CSSContext;
import org.apache.batik.css.engine.CSSEngine;
import org.apache.batik.css.engine.CSSEngineUserAgent;
import org.apache.batik.util.ParsedURL;
import org.osgi.framework.Bundle;
import org.rcpml.core.IController;
import org.rcpml.core.IRCPMLConstructor;
import org.rcpml.core.bridge.IBridge;
import org.rcpml.core.bridge.IVisitor;
import org.rcpml.core.internal.dom.RCPDOMImplementation;
import org.rcpml.core.internal.dom.RCPOMDocument;
import org.rcpml.core.internal.scripting.ScriptContextManager;
import org.rcpml.core.scripting.IScriptContextManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

/**
 * This is General Bridges Tree implementation.
 */
public class Controller implements IController, IVisitor, EventListener,
		CSSContext {

	private static final String DOMATTR_MODIFIED = "DOMAttrModified";

	private static final String DOMNODE_INSERTED_INTO_DOCUMENT = "DOMNodeInsertedIntoDocument";

	private static final String DOMNODE_REMOVED_FROM_DOCUMENT = "DOMNodeRemovedFromDocument";

	private static final String DOMNODE_REMOVED = "DOMNodeRemoved";

	private static final String DOMNODE_INSERTED = "DOMNodeInserted";

	private static final String DOMSUBTREE_MODIFIED = "DOMSubtreeModified";

	private BridgeFactoryManager fBridgeBuilder;

	private Document fDocument = null;

	private Map/*<Node, IBridge>*/ fNodeToBridgeMap = new HashMap/*<Node, IBridge>*/();

	//private Bundle fBundle;

	private IScriptContextManager fScriptContextManager;

	private IBridge fRootBridge;

	private boolean fWithConstructor = false;
	
	public Controller(Document document ) {
		this(document, false);
	}
	
	public Controller(Document document, boolean withConstructor ) {
		this.fDocument = document;
		//this.fBundle = bundle;
		this.fBridgeBuilder = new BridgeFactoryManager();
		this.fBridgeBuilder.setController(this);
		this.fWithConstructor  = withConstructor;

		this.fScriptContextManager = new ScriptContextManager(this.fDocument );
		
		// Initialize css
		this.initializeCSS(this.fDocument);

		this.setEventHandler();

		this.visit(this.fDocument);
		if( !this.fWithConstructor ) {
			this.update();
		}
	}

	public Document getDocument() {
		return this.fDocument;
	}

	public IBridge getBridge(Node node) {
		if (this.fNodeToBridgeMap.containsKey(node)) {
			return (IBridge)this.fNodeToBridgeMap.get(node);
		}
		return null;
	}

//	public Bundle getBundle() {
//		return this.fBundle;
//	}

	public IBridge createBridge(Node node) {
		IBridge bridge = this.fBridgeBuilder.createBridge(node);
		if (this.fRootBridge == null) {
			this.fRootBridge = bridge;
		}
		this.fNodeToBridgeMap.put(node, bridge);
		return bridge;
	}

	public IScriptContextManager getScriptManager() {
		return this.fScriptContextManager;
	}

	public Object getPresentation() {
		if (this.fRootBridge != null) {
			return this.fRootBridge.getPresentation();
		}
		return null;
	}

	public void visit(Node node) {
		if (!(this.fNodeToBridgeMap.containsKey(node))) {
			int type = node.getNodeType();
			if (type == Node.ELEMENT_NODE) {
				if( this.fRootBridge == null ) {
					IBridge bridge = this.createBridge(node);
					if( this.fWithConstructor == true ) {
						return;
					}
					bridge.visit(this);
				}
				else {
					IBridge bridge = this.createBridge(node);
					bridge.visit(this);
				}
			} else if (type == Node.DOCUMENT_NODE) {
				NodeList inner = node.getChildNodes();
				if (inner.getLength() == 0) {
					// empty tree
					return;
				}
				Node nde = inner.item(0);
				visit(nde);
			}
		} else {
			IBridge bridge = this.getBridge(node);
			bridge.visit(this); // update sub childrens.
		}
	}

	/**
	 * Handle mutation events from document modification.
	 */
	public void handleEvent(Event event) {
		String type = event.getType();
		EventTarget target = event.getTarget();
		if (target instanceof Node) {
			Node node = (Node) event.getTarget();
			if (DOMNODE_INSERTED_INTO_DOCUMENT.equals(type)) {
				Node parent = this.findExistParent(node);
				if (parent != null) {
					visit(parent);
				}
				//this.update(parent);
			} else if (DOMNODE_REMOVED_FROM_DOCUMENT.equals(type)) {
				//Node parent = this.findExistParent(node);
				this.disposeNode(node);
				//this.update(parent);
			}
			else if( DOMATTR_MODIFIED.equals(type) ) {
				//this.update(node);
				return;
			}
			if( DOMSUBTREE_MODIFIED.equals(type) ) {
				Node parent = this.findExistParent(node);
				this.update(parent);
			}
		}			
	}	

	/**
	 * Disposes node and all subnodes.
	 * 
	 * @param node
	 */
	private void disposeNode(Node node) {
		IVisitor disposeVisitor = new IVisitor() {
			public void visit(Node node) {
				IBridge bridge = getBridge(node);
				if (bridge != null) {
					bridge.dispose();
					fNodeToBridgeMap.remove(node);
					bridge.visit(this);
				}
			}
		};
		disposeVisitor.visit(node);
	}

	private Node findExistParent(Node node) {
		Node parent = node.getParentNode();
		if (parent.getNodeType() != Node.DOCUMENT_NODE) {
			if (this.fNodeToBridgeMap.containsKey(parent)) {
				return parent;
			} else {
				return findExistParent(parent);
			}
		}
		return fRootBridge.getNode();
	}

	private void setEventHandler() {
		EventTarget et = (EventTarget) this.fDocument;

		et.addEventListener(DOMSUBTREE_MODIFIED, this, true);
		et.addEventListener(DOMNODE_INSERTED, this, true);
		et.addEventListener(DOMNODE_REMOVED, this, true);
		et.addEventListener(DOMNODE_REMOVED_FROM_DOCUMENT, this, true);
		et.addEventListener(DOMNODE_INSERTED_INTO_DOCUMENT, this, true);
		et.addEventListener(DOMATTR_MODIFIED, this, true);
	}

	public void update() {
		if (this.fRootBridge != null) {
			IVisitor updateVisitor = this.createUpdateVisitor();
			Node rootNode = this.fRootBridge.getNode();
			updateVisitor.visit(rootNode);
		}
	}
	// incremental update
	private void update(Node node) {
		if (this.fRootBridge != null) {
			IVisitor updateVisitor = this.createUpdateVisitor();			
			updateVisitor.visit(node);
		}		
	}

	protected IVisitor createUpdateVisitor() {
		return new IVisitor() {
			public void visit(Node node) {
				IBridge bridge = getBridge(node);
				if (bridge != null) {
					bridge.visit(this);
					bridge.update();
				}
			}
		};
	}

	// CSS Specific methods
	protected void initializeCSS(Document document) {
		RCPOMDocument doc = (RCPOMDocument) document;
		CSSEngine eng = doc.getCSSEngine();
		if (eng == null) {
			RCPDOMImplementation impl;
			impl = (RCPDOMImplementation) doc.getImplementation();
			eng = impl.createCSSEngine(doc, this);
			eng.setCSSEngineUserAgent(new RCPCSSEngineUserAgent());
			doc.setCSSEngine(eng);
			//eng.setMedia(userAgent.getMedia());
			//String uri = userAgent.getUserStyleSheetURI();
			/*if (uri != null) {
				try {
					URL url = new URL(uri);
					eng.setUserAgentStyleSheet(eng.parseStyleSheet(url, "all"));
				} catch (MalformedURLException e) {
					userAgent.displayError(e);
				}
			}*/
			//eng.setAlternateStyleSheet(userAgent.getAlternateStyleSheet());
		}
	}

	//TODO: Add correct code here.
	public static class RCPCSSEngineUserAgent implements CSSEngineUserAgent {		
		RCPCSSEngineUserAgent() {
		}

		/**
		 * Displays an error resulting from the specified Exception.
		 */
		public void displayError(Exception ex) {
			//ua.displayError(ex);
			System.out.println("RCPCSSEngine:" + ex.getMessage() );
		}

		/**
		 * Displays a message in the User Agent interface.
		 */
		public void displayMessage(String message) {
			//ua.displayMessage(message);
			System.out.println("RCPCSSEngine:" + message );
		}
	}

	public org.apache.batik.css.engine.value.Value getSystemColor(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public org.apache.batik.css.engine.value.Value getDefaultFontFamily() {
		// TODO Auto-generated method stub
		return null;
	}

	public float getLighterFontWeight(float arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	public float getBolderFontWeight(float arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	public float getPixelUnitToMillimeter() {
		// TODO Auto-generated method stub
		return 0;
	}

	public float getPixelToMillimeter() {
		// TODO Auto-generated method stub
		return 0;
	}

	public float getMediumFontSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	public float getBlockWidth(Element arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	public float getBlockHeight(Element arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	public void checkLoadExternalResource(ParsedURL arg0, ParsedURL arg1) throws SecurityException {
		// TODO Auto-generated method stub
		
	}

	public boolean isDynamic() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isInteractive() {
		// TODO Auto-generated method stub
		return false;
	}

	public CSSEngine getCSSEngineForElement(Element arg0) {
		RCPOMDocument doc = (RCPOMDocument)this.fDocument;
		return doc.getCSSEngine();
	}

	// if this is root element then lets dispose all tree.
	public void bridgeDisposed(IBridge bridge) {
		if( bridge.equals(this.fRootBridge)) {
			this.disposeNode(bridge.getNode());
		}		
	}

	public IRCPMLConstructor getRootBridge() {
		if( fRootBridge != null && fRootBridge instanceof IRCPMLConstructor ) {
			return (IRCPMLConstructor)fRootBridge;
		}
		return null;
	}

	public boolean isWithConstructor() {
		return this.fWithConstructor;
	}	
}
