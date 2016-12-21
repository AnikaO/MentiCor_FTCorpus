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
 * Script to merge sentences that should not have been split, e.g. ending in "i.e." or "e.g." -- mostly with capital characters at the start of the next sentence or numbers.
 * Mutation names also caused splits to occur which had not been anticipated.
 * 
 * @author: Anika Oellrich (anika.oellrich@kcl.ac.uk)
 */

def curatorDir = new File(args[0])

// go through all the subfolders and find the respective XML file that needs modifying
curatorDir.eachDir { dir ->

	println	dir.name
	def f = new File(dir.absolutePath + "/mode2.xml")

	// configure XML Parser
	def parser = new XmlParser()
	parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
	parser.setFeature("http://xml.org/sax/features/namespaces", false)

	// parse XML content of file
	def log = parser.parse(f)

	// find all sentences
	def sents = log.depthFirst().findAll { node -> (node instanceof Node) && node.name() == 's'}
	def sentsToBeMerged = []
	def sentsToBeMergedSpecial = []
	def sentIds = []

	sents.each { sen ->
		if (sen.text().trim().endsWith("e.g.") || sen.text().trim().endsWith("i.e.") || sen.text().trim().endsWith("ie.") || sen.text().trim().endsWith("eg.")
		|| sen.text().trim().endsWith("Dr.") || sen.text().trim().endsWith("Prof.") || sen.text().trim().endsWith("Ref.") || sen.text().trim().endsWith("Drn.")) {
			// println sen.text()
			// record sentence that needs to be merged to this one (which is the one following the currently assessed which is why it cannot be deleted)
			sentsToBeMerged.add(sents.find { node -> new Integer(node.@sid) == (new Integer(sen.@sid.toString()) + 1) })
		} else if (!sen.text().trim().endsWith(". p.") && (sen.text().trim().endsWith(" p.") || sen.text().trim().endsWith(" g.") || sen.text().trim().endsWith(" c.") 
			|| sen.text().trim().endsWith(" (g.") || sen.text().trim().endsWith(" (p."))) {

			def followedBy = sents.find { node -> new Integer(node.@sid) == (new Integer(sen.@sid.toString()) + 1) }
			if (!followedBy.text().startsWith(" ")) {
				sentsToBeMergedSpecial.add(sents.find { node -> new Integer(node.@sid) == (new Integer(sen.@sid.toString()) + 1) })
				sentIds.add(sen.@sid)
			}
		}
	}

	// println sentIds
	// go through all the sentences that have been identified as needed to be merged with prev sentence
	sentsToBeMerged.each { sen ->
		int sentCounter = (new Integer(sen.@sid.toString()) - 1)
		def prevSent = sents.find { node -> new Integer(node.@sid) == sentCounter}

		// sentences are merged but no check that CoreSC annotation match -- it's assumed that the first annotations apply for all parts of the sentences
		sen.children().collect { it instanceof Node ? it.clone() : it }.each {
			if ( !(it instanceof Node) || ( (it instanceof Node) && !(it.name().startsWith("CoreSc")) ) ) {
				prevSent.children().add(it)
			}
		}
		sen.parent().remove(sen)
	}

	def ssentSpecial = sentsToBeMergedSpecial.sort{ new Integer(it.@sid) }


	ssentSpecial.each { sen ->
		// println "-----------------"
		// println sen.@sid

		int sentCounter = new Integer(sen.@sid.toString()) - 1

		while ( (sentsToBeMergedSpecial.find { node -> new Integer(node.@sid) == sentCounter}) != null) {
			sentCounter = sentCounter - 1
		}

		// println sentCounter

		def prevSent = sents.find { node -> new Integer(node.@sid) == sentCounter}
		sen.children().collect { it instanceof Node ? it.clone() : it }.each {
			if ( !(it instanceof Node) || ( (it instanceof Node) && !(it.name().startsWith("CoreSc")) ) ) {
				prevSent.children().add(it)
			}
		}
		sen.parent().remove(sen)

		// println prevSent.text()
		// println "----------------"
	}

	// write the altered XML back to file, in a separate folder (second script parameter)

	// println args[1] + dir.name
	new File(args[1] + dir.name).mkdir()
	def writer = new FileWriter(args[1] + dir.name + "/mode2.xml")
	new XmlNodePrinter(new IndentPrinter(new PrintWriter(writer), "", false)).print(log)
	writer.close()

}