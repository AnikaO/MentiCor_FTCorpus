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

package util.informationSupplement

/**
 * Extracts sentences from XML files relevant for abstract and full text of a publication. Results are written to individual folders.
 * 
 * @author Anika Oellrich
 */
def corpusFolder = new File(args[0])
def abstractFolder = args[1]
def textFolder = args[2]

corpusFolder.eachDir { dir ->
	def f = new File(dir.absolutePath + "/mode2.xml")
	def pub = dir.name.substring(0, dir.name.indexOf("_"))
	
	def parser = new XmlSlurper()
	parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
	parser.setFeature("http://xml.org/sax/features/namespaces", false)
	
	def f_parsed = parser.parse(f)
	
	def sents_abstract = f_parsed.article.front.'article-meta'.'abstract'.'**'.findAll { node -> node.name() == 's' }
	def sents_text = f_parsed.article.body.'**'.findAll { node -> node.name() == 's' }
	
	def bfw_abstract = new BufferedWriter(new FileWriter(new File(abstractFolder + pub + ".txt")))
	sents_abstract.each { s ->
		bfw_abstract << s
	}
	bfw_abstract.flush()
	bfw_abstract.close()
	
	def bfw_text = new BufferedWriter(new FileWriter(new File(textFolder + pub + ".txt")))
	sents_text.each { s ->
		bfw_text << s
	}
	bfw_text.flush()
	bfw_text.close()
}
