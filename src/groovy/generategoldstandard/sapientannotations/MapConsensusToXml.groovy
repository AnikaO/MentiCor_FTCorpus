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
package generategoldstandard.sapientannotations

@Grab('org.apache.commons:commons-csv:1.2')
import org.apache.commons.csv.CSVParser
import static org.apache.commons.csv.CSVFormat.*

import java.nio.file.Paths
import groovy.json.JsonOutput
/**
 * Consensus for three annotators was determined based on matrices and R script implemented by Shyama. XML files were transferred beforehand using Groovy 
 * (GenerateWeightedMatrices.groovy) into weighted matrices only. The results are then integrated back into the XML files to use existing scripts to 
 * determine coverage of annotation types and numbers.
 * 
 * @author Anika Oellrich (anika.oellrich@kcl.ac.uk)
 */

// assess cleaned XML files of one curator and add in consensus annotations
def curatorDir = new File(args[0])
def outputDir = new File(args[1])
def consensusDir = new File(args[2])
// ordering according to lowest priority so that annotations can be added as first continously and end up in the correct ordering from highest to lowest
// with highest first
def orderingCoreScs = ['Bac', 'Met', 'Exp', 'Obs', 'Mod', 'Res', 'Con', 'Obj', 'Mot', 'Goa', 'Hyp']

curatorDir.eachDir { dir ->

	def article = dir.name.substring(0, dir.name.indexOf("_"))
	def f = new File(dir.absolutePath + "/mode2.xml")
	String consf = consensusDir.absolutePath + "/" + article + "_mode2.txt"
	def consensus = [:]

	Paths.get(consf).withReader { reader ->
		CSVParser csv = new CSVParser(reader, DEFAULT.withHeader())

		for (record in csv.iterator()) {
			// def exp = (record.toMap() as Expando)
			def exp = record.toMap()
			consensus[new Integer(exp.sid)] = exp
		}
	}

	def parser = new XmlParser()
	parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
	parser.setFeature("http://xml.org/sax/features/namespaces", false)

	def log = parser.parse(f)
	def sents = log.article.'**'.findAll { node -> (node instanceof Node) && node.name() == 's' }

	// go through all the sentences; for each sentence delete existing CoreSC(s) and add what should be there from consensus
	// dummy values for identifiers and novelty/advantage/disadvantage
	sents.each { sent ->
		def annos = consensus[new Integer(sent.@sid)]
		def delCh = []

		// delete all CoreSC annotations
		sent.children().each { ch ->
			if ((ch instanceof Node) && ch.name().startsWith("CoreSc")) { delCh.add(ch) }
		}

		if (delCh.size() > 0) { delCh.each { sent.children().remove(it) } }

		// add consensus annotations
		orderingCoreScs.each { coreSc ->
			if ( new Double(annos[coreSc]) > 0) {
				def newCoreSc3 = new Node(null, 'CoreSc', ['conceptID':coreSc+"1", 'novelty':"None", 'type':coreSc, 'advantage':"None", 'atype':"GSC"])
				sent.children().add(0, newCoreSc3)
			}
		}
		
		int counter = 1;
		delCh.clear()
		def insertNode = []
		
		// delete all CoreSC annotations
		sent.children().each { ch ->
			if ((ch instanceof Node) && ch.name().startsWith("CoreSc")) {  
				// some magic here to add numbering for other script to work!
				def newCoreSc = new Node(null, "CoreSc$counter", ['conceptID':ch.@conceptID, 'novelty':ch.@novelty, 'type':ch.@type, 'advantage':ch.@advantage, 'atype':ch.@atype])
				insertNode.add(newCoreSc)
				delCh.add(ch)
				counter += 1
			}
		}
		// println insertNode
		if (delCh.size() > 0) { delCh.each { sent.children().remove(it) } }
		if (insertNode.size() > 0 ) { insertNode.reverse().each { sent.children().add(0, it)} }
	}

	println args[1] + article
	new File(args[1] + article).mkdir()

	//Save File
	def writer = new FileWriter(args[1] + article + "/mode2.xml")

	//Option 1: Write XML all on one line
	new XmlNodePrinter(new IndentPrinter(new PrintWriter(writer), "", false)).print(log)
	writer.close()
}