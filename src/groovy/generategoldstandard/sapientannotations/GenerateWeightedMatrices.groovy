/**
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
package generategoldstandard.sapientannotations

/**
 * Transforms XML annotations into matrix that can be used with old R script.
 *
 * @author Anika Oellrich (anika.oellrich@kcl.ac.uk)
 */

def curatorDir = new File(args[0])
def outputDir = args[1]

// ordering is important as the script for building the consensus has hard-coded priority vector
def corescs = ["Bac","Con","Exp","Goa","Met","Mot","Obs","Res","Mod","Obj","Hyp"]


// go through all the subfolders and find the respective file that needs modifying
curatorDir.eachDir { dir ->

	def f = new File(dir.absolutePath + "/mode2.xml")
	def outf = new BufferedWriter(new FileWriter(new File(outputDir + "/" + dir.name.substring(0,dir.name.indexOf("_")) + "_mode2")))
	
	outf.write("sid," + corescs.join(",") + "\n")
	
	def parser = new XmlParser()
	parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
	parser.setFeature("http://xml.org/sax/features/namespaces", false)

	def log = parser.parse(f)
	def sents = log.article.'**'.findAll { node -> (node instanceof Node) && node.name() == 's' }

	// go through all the sentences and check whether they are digits only
	// if yes, merge with previous sentence
	sents.each { sent ->
		def tmp = [:]
		if ((sent.CoreSc1.@type[0] != null) && (sent.CoreSc2.@type[0] == null) && (sent.CoreSc3.@type[0] == null)) { 
			tmp[sent.CoreSc1.@type[0]] = 1
		} else if ((sent.CoreSc1.@type[0] != null) && (sent.CoreSc2.@type[0] != null) && (sent.CoreSc3.@type[0] == null)) { 
			tmp[sent.CoreSc1.@type[0]] = 0.6
			tmp[sent.CoreSc2.@type[0]] = 0.4
		} else if ((sent.CoreSc1.@type[0] != null) && (sent.CoreSc2.@type[0] != null) && (sent.CoreSc3.@type[0] != null)) { 
			tmp[sent.CoreSc1.@type[0]] = 0.6
			tmp[sent.CoreSc2.@type[0]] = 0.3
			tmp[sent.CoreSc2.@type[0]] = 0.1
		}
		
		outf.write(sent.@sid)
		corescs.each { coresc ->
			if (tmp.containsKey(coresc)) {
				outf.write(","+tmp[coresc])
			} else {
				outf.write(",0.0")
			}
		}
		outf.write("\n")
	}
	
	outf.flush()
	outf.close()
}