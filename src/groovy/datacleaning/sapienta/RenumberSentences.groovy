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
 * This script renumbers the sentences after those have been deleted that only contained digits.
 * 
 * @author: Anika Oellrich
 */

def curatorDir = new File(args[0])
def outputDir = new File(args[1])
//outputDir.mkdir()

// go through all the subfolders and find the respective file that needs modifying
curatorDir.eachDir { dir ->
	
	def f = new File(dir.absolutePath + "/mode2.xml")

	def parser = new XmlSlurper()
	parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
	parser.setFeature("http://xml.org/sax/features/namespaces", false)

	def log = parser.parse(f)
	def sents = log.article.'**'.findAll { node -> node.name() == 's' }

	// go through all the sentences and see whether numbers add up

	int lastSentId = 0

	sents.each { sent ->
		int newSentId = lastSentId + 1

		if (!(newSentId == new Integer(sent.@sid.toString()))) {
			// println newSentId
			sent.@sid = "" + newSentId.toString()
		}

		lastSentId = newSentId
	}

	println args[1] + dir.name
	new File(args[1] + dir.name).mkdir()

	//Save File
	def writer = new FileWriter(args[1] + dir.name + "/mode2.xml")

	//Option 1: Write XML all on one line
	def builder = new StreamingMarkupBuilder()
	writer << builder.bind {
		mkp.yield log
	}
}