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

/**
 * Script to remove wrapped tables from sentence tags as it cause the table text to be contained in text for Brat annotations. Text needs to be removed 
 * retrospectively now. While this script does not change the sentence numbering, if it's the last to be executed, the Renumbering needs to be run to 
 * remove all the whitespaces that would prevent the XML from being loaded in Sapient.
 * 
 * @author Anika Oellrich
 */

def curatorDir = new File(args[0])

// go through all the subfolders and find the respective file that needs modifying
curatorDir.eachDir { dir ->

	def f = new File(dir.absolutePath + "/mode2.xml")

	def parser = new XmlParser()
	parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
	parser.setFeature("http://xml.org/sax/features/namespaces", false)

	def log = parser.parse(f)

	def sents = log.depthFirst().findAll { node -> (node instanceof Node) && node.name() == 's'}
	
	// identify those tables that have been wrapped within a sentence tag and remove
	sents.each { sen ->
		def dels = sen.children().findAll { node -> (node instanceof Node) && (node.name() == 'table-wrap')}
		dels.each { del -> 
			sen.parent().children().add(del.clone())
			sen.children().remove(del) 
		}
	}
	
	// write the result file back 
	println args[1] + dir.name
	new File(args[1] + dir.name).mkdir()

	def writer = new FileWriter(args[1] + dir.name + "/mode2.xml")
	new XmlNodePrinter(new IndentPrinter(new PrintWriter(writer), "", false)).print(log)
	writer.close()
}
