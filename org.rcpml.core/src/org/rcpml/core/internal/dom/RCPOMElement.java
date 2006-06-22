package org.rcpml.core.internal.dom;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.batik.css.engine.CSSEngine;
import org.apache.batik.css.engine.CSSStylableElement;
import org.apache.batik.css.engine.StyleMap;
import org.apache.batik.css.engine.value.Value;
import org.apache.batik.dom.AbstractDocument;
import org.apache.batik.dom.GenericElementNS;
import org.rcpml.core.dom.RCPStylableElement;
import org.w3c.dom.Node;

public class RCPOMElement extends GenericElementNS implements
		CSSStylableElement, RCPStylableElement {
	private static final long serialVersionUID = 1L;

	private StyleMap fStyleMap;

	public RCPOMElement(String nsURI, String name, AbstractDocument owner) {
		super(nsURI, name, owner);
		nodeName = name;
	}

	public StyleMap getComputedStyleMap(String arg0) {
		return this.fStyleMap;
	}

	public void setComputedStyleMap(String pseudo, StyleMap styleMap) {
		this.fStyleMap = styleMap;

	}

	public String getXMLId() {
		return getAttributeNS(null, "id");
	}

	public String getCSSClass() {
		return getAttributeNS(null, "class");
	}

	public URL getCSSBase() {
		try {
			if (getXblBoundElement() != null) {
				return null;
			}
			String bu = getBaseURI();
			if (bu == null) {
				return null;
			}
			return new URL(bu);
		} catch (MalformedURLException e) {
			// !!! TODO
			e.printStackTrace();
			throw new InternalError();
		}
	}

	public boolean isPseudoInstanceOf(String pseudoClass) {
		if (pseudoClass.equals("first-child")) {
			Node n = getPreviousSibling();
			while (n != null && n.getNodeType() != ELEMENT_NODE) {
				n = n.getPreviousSibling();
			}
			return n == null;
		}
		return false;
	}

	public CSSEngine getCSSEngine() {
		return ((RCPOMDocument)this.getOwnerDocument()).getCSSEngine();
	}

	public Value getComputedValue(int index) {		
		CSSEngine engine = this.getCSSEngine();
		return engine.getComputedStyle( this, null, index );
	}
}