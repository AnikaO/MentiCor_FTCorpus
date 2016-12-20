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

package util.prepareBratServer

/**
 * <p>
 * Extracts identifier, name and synonyms for each drug contained in the DrugBank download file. The output needs to be post-processed with
 * ParseDrugBankFurther.groovy. Relies on XPathXmlSlurper2.groovy implemented by John Wilson.
 * </p>
 * 
 * @author Anika Oellrich
 * 
 * @param file	DrugBank download XML file
 */
def xpath = "/drugbank/drug"
def xpathSlurper = new XPathXmlSlurper2();

// identify each drug based on XML parse tree
def c = { twig, it ->
	def id =  (it.'drugbank-id'[0]).toString()
	def name = it.name.toString()
	def syn = (it.synonyms[0]).toString()
	
	// print current drug
	println id + "\t" + name + "\t" + syn
	twig.purgeCurrent();
}

xpathSlurper.setTwigRootHandler(xpath, c);
xpathSlurper.setKeepWhitespace(true)
def fdata = xpathSlurper.parse(new File(args[0]));



