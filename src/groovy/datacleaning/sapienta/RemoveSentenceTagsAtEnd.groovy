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
import groovy.xml.XmlUtil

/**
 * Script to remove sentence tags that have been assigned, while they should not have been. It works based on section headings even though this is not the most reliable
 * due to text boxes.
 * 
 * @author Anika Oellrich
 */

def curatorDir = new File(args[0])

// go through all the subfolders and find the respective file that needs modifying
curatorDir.eachDir { dir ->

	def f = new File(dir.absolutePath + "/mode2.xml")

	// open annotated file that possibly contains sentences with duplicated content
	// def f = new File(args[0])

	def parser = new XmlParser()
	// parser.setTrimWhitespace(true)
	// parser.setKeepIgnorableWhitespace(false)
	parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
	parser.setFeature("http://xml.org/sax/features/namespaces", false)

	def log = parser.parse(f)

	def secs = log.depthFirst().findAll { node -> (node instanceof Node) && node.name() == 'sec'}

	// go through all the sections and remove sentence tags on those where
	// the title is wrapped with a sentence tag
	secs.each { sec ->
		if (!sec.s.title.toString().equals("")) {
			sec.s.each { sen ->
				sen.children().collect { it instanceof Node ? it.clone() : it }.each {
					if ( !(it instanceof Node) || ( (it instanceof Node) && !(it.name().startsWith("CoreSc")) ) ) {
						sen.parent().children().add(it)
					}
				}
				sen.parent().remove(sen)
			}
		}
	}

	// go through all the boxed-text and remove the sentence tags there
	def bts = log.'**'.findAll { node -> (node instanceof Node) && node.name() == 'boxed-text' }

	bts.each { bt ->
		def sents = bt.'**'.findAll { node -> (node instanceof Node) && node.name() == 's' }

		sents.each { sent ->
			sent.children().collect { it instanceof Node ? it.clone() : it }.each {
				if ( !(it instanceof Node) || ( (it instanceof Node) && !(it.name().startsWith("CoreSc")) ) ) {
					sent.parent().children().add(it)
				}
			}
			sent.parent().remove(sent)
		}
	}

	// go through all the sections and delete sentence tag if conclusions or supplement already passed
	secs = log.article.'**'.findAll { node -> (node instanceof Node) && node.name() == 'sec' }
	Boolean conclusionsPassed = false

	secs.each { sec ->
		// abstract can have conclusion section, too, so make sure it's outside the abstract
		if (((sec.title.text().contains("Conclusion")) || (sec.title.text().startsWith("Supplementary "))) && (!sec.parent().name().equals("abstract"))) {
			conclusionsPassed = true
		} else if (conclusionsPassed || (sec.title.text().startsWith("Disclosure"))) {
			def sents = sec.'**'.findAll { node -> (node instanceof Node) && node.name() == 's' }

			sents.each { sent ->
				sent.children().collect { it instanceof Node ? it.clone() : it }.each {
					if ( !(it instanceof Node) || ( (it instanceof Node) && !(it.name().startsWith("CoreSc")) ) ) {
						sent.parent().children().add(it)
					}
				}
				sent.parent().remove(sent)
			}
		}
	}
	
	// remove sentences in back part of paper
	def sents = log.article.back.'**'.findAll { node -> (node instanceof Node) && node.name() == 's' }

	sents.each { sent ->
		sent.children().collect { it instanceof Node ? it.clone() : it }.each {
			if ( !(it instanceof Node) || ( (it instanceof Node) && !(it.name().startsWith("CoreSc")) ) ) {
				sent.parent().children().add(it)
			}
		}
		sent.parent().remove(sent) 
	}
	
	println args[1] + dir.name
	new File(args[1] + dir.name).mkdir()

	//Save File
	def writer = new FileWriter(args[1] + dir.name + "/mode2.xml")
	// def writer = new FileWriter("test.xml")
	// def xmlu = new XmlUtil()

	// using this method, the resulting XML contains white spaces of all sorts -- need to be renumbered afterwards so that all the spaces can be removed then
	// def result = XmlUtil.serialize(log)
	
	new XmlNodePrinter(new IndentPrinter(new PrintWriter(writer), "", false)).print(log)
	// writer.write(result)
	writer.close()

	//Option 1: Write XML all on one line
	//def builder = new StreamingMarkupBuilder()
	//writer << builder.bind {
	//	mkp.yield log
	//}
}