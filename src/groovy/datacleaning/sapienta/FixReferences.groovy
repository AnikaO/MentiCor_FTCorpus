/*
 * Copyright (c) <2016>, <Anika Oellrich>
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer
 *    in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package datacleaning.sapienta

import groovy.xml.StreamingMarkupBuilder

/**
 * References have been assigned wrongly to the beginning of the following sentence in the case of superscript references. This also results in sentences having 
 * only references as content. To correct for this, sentences need to be identified that have either references in the beginning or only contain references and
 * their content needs to be added to the previous sentence.
 * 
 * @author: Anika Oellrich
 */

def curatorDir = new File(args[0])
def outputDir = new File(args[1])
outputDir.mkdir()

// go through all the subfolders and find the respective file that needs modifying
curatorDir.eachDir { dir ->

	def f = new File(dir.absolutePath + "/mode2.xml")

	def parser = new XmlParser()
	parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
	parser.setFeature("http://xml.org/sax/features/namespaces", false)

	def log = parser.parse(f)
	def sents = log.article.'**'.findAll { node -> (node instanceof Node) && node.name() == 's' }

	// go through all the sentences and check whether they are digits only
	// if yes, merge with previous sentence
	sents.each { sent ->
		Boolean others = false;
		int sentCounter = (new Integer(sent.@sid.toString()) - 1)
		def prevSent = sents.find { node -> new Integer(node.@sid) == sentCounter} 
		def delCh = []
		
		sent.children().each { ch ->
			if ((ch instanceof Node) && (ch.name() == 'xref') && !others) {
				prevSent.children().add(ch.clone())
				delCh.add(ch)
			} else if ( (ch instanceof Node) && (ch.name() == 'sup')) { // for xrefs nested in sup tag
				// this is not the cleanest solution as it doesn't check that sups only has xrefs!
				def supCh = ch.children()	
				boolean childAdd = false
							
				// println "ccccccccccccc"
				
				supCh.each { cch -> 
					// println "bbbbbbb"
					
					if ((cch instanceof Node) && (cch.name() == 'xref') && !others) {
						// println "aaaaa"
						childAdd = true
					} else { others = true }
				}
				
				if (childAdd) { prevSent.children().add(ch.clone()); delCh.add(ch) }
				
			} else if ( (ch instanceof Node) && (ch.name().startsWith("CoreSc"))) {
				// just don't take them into counting as they would appear first  
			} else { others = true }	
		}
		
		if (delCh.size() > 0) {
			delCh.each { sent.children().remove(it) }
		}
		
		if (!others) {
			sent.parent().remove(sent)
		}
		
		/*
		if (sent.toString().matches("[0-9]+")) {
			int sentCounter = (new Integer(sent.@sid.toString()) - 1)

			def prevSent = log.article.'**'.find { node -> node.name() == 's' && node.@sid == sentCounter }
			sentCounter = sentCounter - 1

			if ( prevSent != null ) {
				prevSent.replaceBody(prevSent.toString() + sent.toString())
			}

			// println prevSent
			sent.replaceNode {}
		}*/
	}

	println args[1] + dir.name
	new File(args[1] + dir.name).mkdir()

	//Save File
	def writer = new FileWriter(args[1] + dir.name + "/mode2.xml")

	//Option 1: Write XML all on one line
	new XmlNodePrinter(new IndentPrinter(new PrintWriter(writer), "", false)).print(log)
	writer.close()
}