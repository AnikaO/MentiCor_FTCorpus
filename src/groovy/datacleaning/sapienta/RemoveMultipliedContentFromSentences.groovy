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
 * During the manual annotation through curators, due to the current implementation of Sapient, a number of sentences have their content multiplied. It seems that 
 * whenever the tool generated a corrupted file and produced multiplied annotations, the textual representation also changed. This script needs to be run first 
 * before any of the other cleaning scripts are executed as it relies on the originally assigned sentence numbering.
 * 
 * @author Anika Oellrich
 */


def curatorDir = new File(args[0])
def referenceDir = new File(args[1])


// go through all the subfolders and find the respective file that needs modifying
curatorDir.eachDir { dir ->

	def f = new File(dir.absolutePath + "/mode2.xml")

	println f
	// open annotated file that possibly contains sentences with duplicated content
    //def f = new File(args[0])
	
	def parser = new XmlParser()
	parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
	parser.setFeature("http://xml.org/sax/features/namespaces", false)

	def log = parser.parse(f)
	def sents = log.article.'**'.findAll { node -> (node instanceof Node) && (node.name() == 's' )}
	
	// open reference file to which individual sentences are compared
	def ref = new File(referenceDir.absolutePath + "/" + dir.name.substring(0, dir.name.indexOf("_")) + "/mode2.xml")
	
	def log_ref = parser.parse(ref)
	
	// go through all the sentences and check with reference whether they possess duplicated content
	// replace duplicated content with the sentences coming from the reference resource
	sents.each { sent ->
		def originalSent = log_ref.article.'**'.find { node -> (node instanceof Node) && node.name() == 's' && node.@sid == sent.@sid }
		
		if (originalSent != null) {
			if ( !sent.toString().equals(originalSent.toString()) ) {
				
				def addOn = originalSent.clone()
				def counter = 0
				
				sent.children().each { ch ->
					if ( (ch instanceof Node) && ch.name().startsWith("CoreSc") ) {
						addOn.children().add(counter , ch.clone())
						counter += 1
					}
				}
				
				sent.parent().children().add(addOn)
				sent.parent().remove(sent)
				
				//sent.replaceBody(st.toString() + originalSent.toString())
				// sent.value = "ttt" // originalSent.toString()
				// println sent.childNodes()
			}
		} else {
			println "Error -- original sentence not to be found in reference file"
		}
	}

	// println args[2] + dir.name
	new File(args[2] + dir.name).mkdir()

	//Save File
	def writer = new FileWriter(args[2] + dir.name + "/mode2.xml")
	
	// def writer = new FileWriter("test.xml")
	new XmlNodePrinter(new IndentPrinter(new PrintWriter(writer), "", false)).print(log)
	writer.close()
}