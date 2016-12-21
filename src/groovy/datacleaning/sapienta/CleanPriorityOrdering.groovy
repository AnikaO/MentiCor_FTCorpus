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
 * Script to assess whether the correct ordering (priority) has been used when multiple CoreSC annotations are assigned to one sentence. If not, reorder sentences.
 * 
 * @author Anika Oellrich
 */

def curatorDir = new File(args[0])
def outputDir = new File(args[1])
outputDir.mkdir()

// go through all the subfolders and find the respective file that needs modifying
curatorDir.eachDir { dir ->
	
	println "---------------------------"
	println dir.absolutePath
	def f = new File(dir.absolutePath + "/mode2.xml")
	// def f = new File(args[0])

	// println f
	// open annotated file that possibly contains sentences with duplicated content
	//def f = new File(args[0])

	def parser = new XmlParser()
	parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
	parser.setFeature("http://xml.org/sax/features/namespaces", false)

	def log = parser.parse(f)
	def sents = log.article.'**'.findAll { node -> (node instanceof Node) && (node.name() == 's' )}
	def orderingCoreScs = ['Bac':1, 'Met':2, 'Exp':3, 'Obs':4, 'Mod':5, 'Res':6, 'Con':7, 'Obj':8, 'Mot':9, 'Goa':10, 'Hyp':11]

	// println (orderingCoreScs['Hyp'] > orderingCoreScs['Obs']) --> true
	// println (orderingCoreScs['Bac'] > orderingCoreScs['Obs']) --> false

	sents.each { sent ->
		def coreSc1 = sent.CoreSc1.@type[0]
		def coreSc2 = sent.CoreSc2.@type[0]
		def coreSc3 = sent.CoreSc3.@type[0]

		if (coreSc2 != null) {
			if (coreSc3 != null) {
				if ((orderingCoreScs[coreSc1] < orderingCoreScs[coreSc2]) || (orderingCoreScs[coreSc1] < orderingCoreScs[coreSc3]) || (orderingCoreScs[coreSc2] < orderingCoreScs[coreSc3])) {
					println "CoreSC1, CoreSC2 and CoreSC3 reordered in sentence: " + sent.@sid
					def tmp_coreSc1 = sent.CoreSc1.clone()
					def tmp_coreSc2 = sent.CoreSc2.clone()
					def tmp_coreSc3 = sent.CoreSc3.clone()
					def inserts = [:]

					inserts[orderingCoreScs[tmp_coreSc1[0].@type]] = tmp_coreSc1[0]
					inserts[orderingCoreScs[tmp_coreSc2[0].@type]] = tmp_coreSc2[0]
					inserts[orderingCoreScs[tmp_coreSc3[0].@type]] = tmp_coreSc3[0]

					sent.children().remove(0)
					sent.children().remove(0)
					sent.children().remove(0)

					def replaceWith = inserts[inserts.keySet().sort()[0]]
					def newCoreSc3 = new Node(null, 'CoreSc3', ['conceptID':replaceWith.@conceptID, 'novelty':replaceWith.@novelty, 'type':replaceWith.@type, 'advantage':replaceWith.@advantage, 'atype':replaceWith.@atype])
					sent.children().add(0, newCoreSc3)
					replaceWith = inserts[inserts.keySet().sort()[1]]
					def newCoreSc2 = new Node(null, 'CoreSc2', ['conceptID':replaceWith.@conceptID, 'novelty':replaceWith.@novelty, 'type':replaceWith.@type, 'advantage':replaceWith.@advantage, 'atype':replaceWith.@atype])
					sent.children().add(0, newCoreSc2)
					replaceWith = inserts[inserts.keySet().sort()[2]]
					def newCoreSc1 = new Node(null, 'CoreSc1', ['conceptID':replaceWith.@conceptID, 'novelty':replaceWith.@novelty, 'type':replaceWith.@type, 'advantage':replaceWith.@advantage, 'atype':replaceWith.@atype])
					sent.children().add(0, newCoreSc1)

				}
			} else if (orderingCoreScs[coreSc1] < orderingCoreScs[coreSc2]) {
				println "CoreSC1 and CoreSC2 reordered in sentence: " + sent.@sid

				def tmp_coreSc1 = sent.CoreSc1.clone()
				def tmp_coreSc2 = sent.CoreSc2.clone()

				sent.children().remove(0)
				sent.children().remove(0)

				// println tmp_coreSc1[0].@type
				def newCoreSc1 = new Node(null, 'CoreSc1', ['conceptID':tmp_coreSc2[0].@conceptID, 'novelty':tmp_coreSc2[0].@novelty, 'type':tmp_coreSc2[0].@type, 'advantage':tmp_coreSc2[0].@advantage, 'atype':tmp_coreSc2[0].@atype])
				sent.children().add(0, newCoreSc1)
				def newCoreSc2 = new Node(null, 'CoreSc2', ['conceptID':tmp_coreSc1[0].@conceptID, 'novelty':tmp_coreSc1[0].@novelty, 'type':tmp_coreSc1[0].@type, 'advantage':tmp_coreSc1[0].@advantage, 'atype':tmp_coreSc1[0].@atype])
				sent.children().add(1, newCoreSc2)
			}
		}
	}

	//Save File
	new File(args[1] + dir.name).mkdir()
	def writer = new FileWriter(args[1] + dir.name + "/mode2.xml")
	//def writer = new FileWriter(args[1])

	//Option 1: Write XML all on one line
	new XmlNodePrinter(new IndentPrinter(new PrintWriter(writer), "", false)).print(log)
	writer.close()
}