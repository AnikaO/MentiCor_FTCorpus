/*
 * Copyright (c) <2016>, <Anika Oellrich>
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer 
 *    in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this 
 *    software without specific prior written permission.
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS 
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE 
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT 
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH 
 * DAMAGE.
 */

package util.informationSupplement

/**
 * Script to extract meta-data from EuropePMC downloaded XML to allow for easy plotting in R.
 * 
 * @author Anika Oellrich (anika.oellrich@kcl.ac.uk)
 */

def corpusFolder = new File(args[0])

println "pmcid\tauthors\ttitle length\tyear\tmesh\tcountry"
corpusFolder.eachFile { f ->
	
	print f.name.substring(0,f.name.indexOf("."))
	
	def parser = new XmlSlurper()
	parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
	parser.setFeature("http://xml.org/sax/features/namespaces", false)
	
	def f_parsed = parser.parse(f)
	
	def authors = f_parsed.resultList.result.authorList.'**'.findAll { node -> node.name() == 'author' }
	print "\t" + authors.size()
	
	def title = f_parsed.resultList.result.title
	print "\t" + title.toString().tokenize().size()
	
	print "\t" + f_parsed.resultList.result.pubYear
	
	def mesh = f_parsed.resultList.result.meshHeadingList.'**'.findAll { node -> node.name() == 'meshHeading' }
	print "\t" + mesh.size()
	
	def affiliation = f_parsed.resultList.result.affiliation
	if (affiliation.toString().tokenize().size() > 0) { 
		print "\t" + affiliation.toString().tokenize()[-1]
	}
	
	print "\n"
	
}