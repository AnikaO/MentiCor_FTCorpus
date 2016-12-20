/*
 * Copyright 2003-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package util;

import groovy.util.slurpersupport.GPathResult;
import groovy.util.slurpersupport.Node;
import groovy.util.slurpersupport.NodeChild;
import groovy.xml.FactorySupport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.lang.management.*;

/**
 * <p>Parse XML into a document tree that may be traversed similar to XPath 
 * expressions.  For example:</p>
 * <pre>
 * def rootNode = new XmlSlurper().parseText( 
 *    '&lt;root&gt;&lt;one a1="uno!"/&gt;&lt;two&gt;Some text!&lt;/two&gt;&lt;/root&gt;' )
 *    
 * assert rootNode.name() == 'root'
 * assert rootNode.one[0].@a1 == 'uno!'
 * assert rootNode.two.text() == 'Some text!'
 * rootNode.children().each { assert it.name() in ['one','two'] }  
 * </pre>
 * 
 * <p>Note that in some cases, a 'selector' expression may not resolve to a 
 * single node.  For example: </p>
 * <pre>
 * def rootNode = new XmlSlurper().parseText( 
 *    '''&lt;root&gt;
 *         &lt;a&gt;one!&lt;/a&gt;
 *         &lt;a&gt;two!&lt;/a&gt;
 *       &lt;/root&gt;''' )
 *    
 * assert rootNode.a.size() == 2
 * rootNode.a.each { assert it.text() in ['one!','two!'] }  
 * </pre>
 * @see GPathResult
 * @author John Wilson
 */
public class XPathXmlSlurper2 extends DefaultHandler {
	private final XMLReader reader;
	private Node currentNode = null;
	private final Stack stack = new Stack();
	private final StringBuffer charBuffer = new StringBuffer();
	private final Map<String, String> namespaceTagHints = new Hashtable<String, String>();
	private boolean keepWhitespace = false;

	def twig_roots_handlers = [:];
	def mem = ManagementFactory.memoryMXBean;

	/**
	 * Uses the defaults of not validating and namespace aware.
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public XPathXmlSlurper2() throws ParserConfigurationException, SAXException {
		this(false, true);
	}

	def purge(){
		//println "purging the twig";
		currentNode = null;
		stack.clear();
		charBuffer.setLength(0);
	}

	def memUsage (){
		return "Heap Memory Usage: (${Math.round(mem.heapMemoryUsage.used / 1024 /1024)} MB)";
	}

	/* we should change this for an "add" */
	public setTwigRootHandler(path, closure){
		twig_roots_handlers[path] = closure;
	}

	public XPathXmlSlurper2(final boolean validating, final boolean namespaceAware) throws ParserConfigurationException, SAXException {
		SAXParserFactory factory = FactorySupport.createSaxParserFactory();
		factory.setNamespaceAware(namespaceAware);
		factory.setValidating(validating);
		this.reader = factory.newSAXParser().getXMLReader();
	}

	public XPathXmlSlurper2(final XMLReader reader) {
		this.reader = reader;
	}

	public XPathXmlSlurper2(final SAXParser parser) throws SAXException {
		this(parser.getXMLReader());
	}

	/**
	 * @param keepWhitespace
	 * 
	 * If true then whitespace before elements is kept.
	 * The default is to discard the whitespace.
	 */
	public void setKeepWhitespace(boolean keepWhitespace) {
		this.keepWhitespace = keepWhitespace;
	}

	/**
	 * @return The GPathResult instance created by consuming a stream of SAX events
	 * Note if one of the parse methods has been called then this returns null
	 * Note if this is called more than once all calls after the first will return null
	 *
	 */
	public GPathResult getDocument() {
		try {
			return new NodeChild(this.currentNode, null, this.namespaceTagHints);
		} finally {
			this.currentNode = null;
		}
	}

	/**
	 * Parse the content of the specified input source into a GPathResult object
	 * 
	 * @param input
	 * @return An object which supports GPath expressions
	 * @throws IOException
	 * @throws SAXException
	 */
	public GPathResult parse(final InputSource input) throws IOException, SAXException {
		this.reader.setContentHandler(this);
		this.reader.parse(input);

		return getDocument();

	}

	/**
	 * Parses the content of the given file as XML turning it into a GPathResult object
	 * 
	 * @param file
	 * @return An object which supports GPath expressions
	 * @throws IOException
	 * @throws SAXException
	 */
	public GPathResult parse(final File file) throws IOException, SAXException {
		final InputSource input = new InputSource(new FileInputStream(file));

		input.setSystemId("file://" + file.getAbsolutePath());

		return parse(input);

	}

	/**
	 * Parse the content of the specified input stream into an GPathResult Object.
	 * Note that using this method will not provide the parser with any URI
	 * for which to find DTDs etc
	 * 
	 * @param input
	 * @return An object which supports GPath expressions
	 * @throws IOException
	 * @throws SAXException
	 */
	public GPathResult parse(final InputStream input) throws IOException, SAXException {
		return parse(new InputSource(input));
	}

	//  /**
	//   * Parse the content of the specified reader into a GPathResult Object.
	//   * Note that using this method will not provide the parser with any URI
	//   * for which to find DTDs etc
	//   *
	//   * @param in
	//   * @return An object which supports GPath expressions
	//   * @throws IOException
	//   * @throws SAXException
	//   */
	//  public GPathResult parse(final Reader in) throws IOException, SAXException {
	//    return parse(new InputSource(in));
	//  }

	/**
	 * Parse the content of the specified URI into a GPathResult Object
	 * 
	 * @param uri
	 * @return An object which supports GPath expressions
	 * @throws IOException
	 * @throws SAXException
	 */
	public GPathResult parse(final String uri) throws IOException, SAXException {
		return parse(new InputSource(uri));
	}

	/**
	 * A helper method to parse the given text as XML
	 * 
	 * @param text
	 * @return An object which supports GPath expressions
	 */
	public GPathResult parseText(final String text) throws IOException, SAXException {
		return parse(new StringReader(text));
	}

	// Delegated XMLReader methods
	//------------------------------------------------------------------------

	/* (non-Javadoc)
	 * @see org.xml.sax.XMLReader#getDTDHandler()
	 */
	public DTDHandler getDTDHandler() {
		return this.reader.getDTDHandler();
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.XMLReader#getEntityResolver()
	 */
	public EntityResolver getEntityResolver() {
		return this.reader.getEntityResolver();
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.XMLReader#getErrorHandler()
	 */
	public ErrorHandler getErrorHandler() {
		return this.reader.getErrorHandler();
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.XMLReader#getFeature(java.lang.String)
	 */
	public boolean getFeature(final String uri) throws SAXNotRecognizedException, SAXNotSupportedException {
		return this.reader.getFeature(uri);
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.XMLReader#getProperty(java.lang.String)
	 */
	public Object getProperty(final String uri) throws SAXNotRecognizedException, SAXNotSupportedException {
		return this.reader.getProperty(uri);
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.XMLReader#setDTDHandler(org.xml.sax.DTDHandler)
	 */
	public void setDTDHandler(final DTDHandler dtdHandler) {
		this.reader.setDTDHandler(dtdHandler);
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.XMLReader#setEntityResolver(org.xml.sax.EntityResolver)
	 */
	public void setEntityResolver(final EntityResolver entityResolver) {
		this.reader.setEntityResolver(entityResolver);
	}

	/**
	 * Resolves entities against using the supplied URL as the base for relative URLs
	 * 
	 * @param base
	 * The URL used to resolve relative URLs
	 */
	public void setEntityBaseUrl(final URL base) {
		this.reader.setEntityResolver(new EntityResolver() {
					public InputSource resolveEntity(final String publicId, final String systemId) throws IOException {
						return new InputSource(new URL(base, systemId).openStream());
					}
				});
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.XMLReader#setErrorHandler(org.xml.sax.ErrorHandler)
	 */
	public void setErrorHandler(final ErrorHandler errorHandler) {
		this.reader.setErrorHandler(errorHandler);
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
	 */
	public void setFeature(final String uri, final boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
		this.reader.setFeature(uri, value);
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.XMLReader#setProperty(java.lang.String, java.lang.Object)
	 */
	public void setProperty(final String uri, final Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
		this.reader.setProperty(uri, value);
	}


	// ContentHandler interface
	//-------------------------------------------------------------------------

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#startDocument()
	 */
	public void startDocument() throws SAXException {
		this.currentNode = null;
		this.charBuffer.setLength(0);
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startPrefixMapping(java.lang.String, java.lang.String)
	 */
	public void startPrefixMapping(final String tag, final String uri) throws SAXException {
		this.namespaceTagHints.put(tag, uri);
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(final String namespaceURI, final String localName, final String qName, final Attributes atts) throws SAXException {
		addCdata();

		final Map attributes = new HashMap();
		final Map attributeNamespaces = new HashMap();

		for (int i = atts.getLength() - 1; i != -1; i--) {
			if (atts.getURI(i).length() == 0) {
				attributes.put(atts.getQName(i), atts.getValue(i));
			} else {
				attributes.put(atts.getLocalName(i), atts.getValue(i));
				attributeNamespaces.put(atts.getLocalName(i), atts.getURI(i));
			}

		}

		final Node newElement;

		if (namespaceURI.length() == 0){
			newElement = new Node(this.currentNode, qName, attributes, attributeNamespaces, namespaceURI);
		} else {
			newElement = new Node(this.currentNode, localName, attributes, attributeNamespaces, namespaceURI);
		}

		if (this.currentNode != null) {
			this.currentNode.addChild(newElement);
		}

		this.stack.push(this.currentNode);
		this.currentNode = newElement;
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
	 */
	public void characters(final char[] ch, final int start, final int length) throws SAXException {
		this.charBuffer.append(ch, start, length);
	}


	def purgeCurrent(){
		//println "purging current Node (${currentNode.name()})";
		def list = currentNode.children();
		list.clear();
	}


	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void endElement(final String namespaceURI, final String localName, final String qName) throws SAXException {
		addCdata();

		/* let's see if we have a twig handler matching this stack */
		def iterator = this.stack.iterator();
		def path = [];

		/* this is a lame approach...  we should actually go backwards to allow 
		 relative-path xpath queries... */
		while(iterator.hasNext()){
			def node = iterator.next();
			if(node != null) {
				path << node.name();
			} else {
				path << "";
			}
		}
		path << localName;


		/*  processing all registered closures from
		 all xpaths, going from the most specific one to the less
		 generic one:
		 /entries/entry/semantic
		 entries/entry/semantic
		 entry/semantic
		 semantic
		 */
		def l = (0..<path.size());
		l.each { a ->
			def xpathA = path.getAt(a..-1).join("/");
			execClosure(xpathA);
		}
		final Object oldCurrentNode = this.stack.pop();

		if (oldCurrentNode != null) {
			this.currentNode = (Node)oldCurrentNode;
		}
	}

	def  execClosure(xpathA) {
		if(this.twig_roots_handlers[xpathA] != null){
			//println "the xpath [$xpath] has a match...";
			def clos = this.twig_roots_handlers[xpathA];
			//println "calling closure on node ${currentNode.name()} ...";
			clos(this, new NodeChild(this.currentNode, null, this.namespaceTagHints)  );
		}
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#endDocument()
	 */
	public void endDocument() throws SAXException {
	}

	// Implementation methods
	//-------------------------------------------------------------------------

	/**
	 * 
	 */
	private void addCdata() {
		if (this.charBuffer.length() != 0) {
			//
			// This element is preceded by CDATA if keepWhitespace is false (the default setting) and
			// it's not whitespace add it to the body
			// Note that, according to the XML spec, we should preserve the CDATA if it's all whitespace
			// but for the sort of work I'm doing ignoring the whitespace is preferable
			//
			final String cdata = this.charBuffer.toString();

			this.charBuffer.setLength(0);
			if (this.keepWhitespace || cdata.trim().length() != 0) {
				this.currentNode.addChild(cdata);
			}
		}
	}
}
