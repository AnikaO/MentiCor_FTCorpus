#' Copyright (c) <2016>, <Shyamasree Saha>
#' All rights reserved.
#' Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions 
#' are met:
#' 
#' 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
#' 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer 
#'    in the documentation and/or other materials provided with the distribution.
#' 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products 
#'    derived from this software without specific prior written permission.
#'    
#' THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
#' LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
#' HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
#' LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
#' THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
#' OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

#'--
#' This script has been initially developed by Shyamasree Saha and used for analysis as part of the development 
#' of the Multi-label CoreSC Cancer Risk Assessment corpus (see 
#' http://www.sapientaproject.com/wp-content/uploads/2016/05/LREC2016_Ravenscroft.pdf). As part of the MentiCor
#' corpus, the same assessment and merging structures have been followed and the script has been adapted 
#' accordingly.
#' 
#' The script expects in its current version the following input parameters:
#' first parameter -- a folder containing containing three subfolders (named "curator1", "curator2", "curator3") 
#'                    and within each folder, an annotation matrix for each publication annotated (file names 
#'                    must match across the three different curators!)
#' second parameter -- an output folder to which the details for all five scores will be written
#' 
#' Example of use:
#' 
#' Rscript kappa_calculations.R /path/to/curation/matrices /path/to/outputFolder
#' 
#'--
#' title: Inter-Annotator Agreement for Multi-label CoreSC annotations
#' author: Shyamasree Saha
#' contributor: Anika Oellrich
#' released: May 2016
#' updated: July 2016
#'--

#'--
#' Read the annotation matrix as assigned by one curator only. Each file contains a matrix consisting of 12 columns,
#' one for the sentence identifier and the remaining eleven covering the CoreSC concepts used for annotation. The 
#' values in each of the columns can range from 0 to 1, depending on how many annotations have been assigned to a 
#' sentence and in which order. The column ordering must be consistent across all the publications annotated and 
#' across all curators.
#' 
#' @param path path to the directory containing annotation matrices for all three curators
#' @param ann specifies the curator ("curator1", "curator2" or "curator3" here)
#' @param filename defines the specific publication for which annotations have been assigned (here PMC identifiers;
#'                 contains the actual matrix)
#' @return Read Annotation matrix for a specific curator and file (here publication)
#'--
readMatrix <- function(path, ann, filename) {
  mat.df=read.csv(file=paste(path,ann,"/",filename,sep=""),header=TRUE);
  mat.df=mat.df[,-1]
  mat=as.matrix(mat.df);
  return(mat)
}

#'--
#' Changes the annotation matrix to a binary representation by replacing any value between 0 and 1 with 1. As the annotations are read with their 
#' weights according to whether it is the first, second or third annotation to a sentence, this causes problems when calculating the kappa score 
#' requiring only a match across all the annotations assigned by two curators. 
#' 
#' @param mat_ann Annotation matrix for curator
#' @return A list of all three converted, binary matrices (one per curator)
#'--
changeForLogical<-function(mat_ann)
{
  mat_ann[which(mat_ann!=0,arr.ind=T)]=1
  return(mat_ann)
}

#'--
#' Calculates the micro-average kappa score based on all annotations assigned by a pair of curators. It does so for all three pairs of curators simultaneously. 
#' 
#' @param mat_all1 Annotations assigned across all files for curator 1
#' @param mat_all2 Annotations assigned across all files for curator 2
#' @param mat_all3 Annotations assigned across all files for curator 3
#' @param all_ann12 Result matrix for comparing curator 1 and 2 with each other, excluding sentences where both did not assign any annotations
#' @param all_ann23 Result matrix for comparing curator 2 and 3 with each other, excluding sentences where both did not assign any annotations
#' @param all_ann31 Result matrix for comparing curator 3 and 1 with each other, excluding sentences where both did not assign any annotations
#' @return Micro-averaged kappa scores for each of the three curator pairings
#'--
kappafunc<-function(mat_all1,mat_all2,mat_all3,all_ann12,all_ann23,all_ann31)
{
  print("kappafunc -- all_ann12 dimension")
  print(dim(all_ann12))
  pa12=(sum(all_ann12))/dim(all_ann12)[1]
  pa23=(sum(all_ann23))/dim(all_ann23)[1]
  pa31=(sum(all_ann31))/dim(all_ann31)[1]
  
  print("kappafunc -- mat_all1 dimension")
  print(dim(mat_all1))
  
  e1=which(rowSums(mat_all1)==0)
  e2=which(rowSums(mat_all2)==0)
  e3=which(rowSums(mat_all3)==0)
  
  freq_ann1 = colSums(mat_all1)/(dim(mat_all1)[1]-length(e1))
  freq_ann2 = colSums(mat_all2)/(dim(mat_all2)[1]-length(e2))
  freq_ann3 = colSums(mat_all3)/(dim(mat_all3)[1]-length(e3))
  
  pe12=sum(freq_ann1*freq_ann2)
  pe23=sum(freq_ann2*freq_ann3)
  pe31=sum(freq_ann3*freq_ann1)
  print("pa12")
  print(pa12)
  kappa12=((pa12-pe12)/(1-pe12))
  kappa23=((pa23-pe23)/(1-pe23))
  kappa31=((pa31-pe31)/(1-pe31))
  
  return(list(k12=kappa12, k23=kappa23, k31=kappa31))
}

#'--
#' Calculates the kappa agreement measure based on whether at least one annotation matches across the assigned annotations for a pair of curators. This means that all
#' non-matching annotations are not taken into consideration as long as there is at least one matching.
#' 
#' @param files Names of all the files (here publications) annotations have been assigned to and a kappa score is needed for
#' @param out Folder in which to store result file
#' @param ann1 String identifying curator 1 in file system
#' @param ann2 String identifying curator 2 in file system
#' @param ann3 String identifying curator 3 in file system
#' @return Matrix containing all-but-one kappa scores for individual files, and macro-average in second to last and micro-average in last row
#'--
logicalLoose<-function(kappa,f,mat_ann1,mat_ann2,mat_ann3,ex12,ex23,ex31,freq_ann1,freq_ann2,freq_ann3,all_ann12,all_ann23,all_ann31)
{
  filesDim = c(files, "macro", "micro")
  kappa = matrix(nrow=length(filesDim),ncol=3,dimnames=list(filesDim,c("c1-c2","c1-c3","c2-c3")));
  sink(file=paste(out,"kappa_micro_loose.log",sep=""))
  
  all_ann12=NULL
  all_ann23=NULL
  all_ann31=NULL
  
  mat_all1=NULL
  mat_all2=NULL
  mat_all3=NULL
  
  for(f in files)
  {
    print(f)
    
    ## read annotations from files and replace to binary representation
    mat_ann1=changeForLogical(readMatrix(path,ann1,f))
    mat_ann2=changeForLogical(readMatrix(path,ann2,f))
    mat_ann3=changeForLogical(readMatrix(path,ann3,f))
    
    ## generate matrix holding file-specific information for micro-average calculation
    mat_all1=rbind(mat_all1,mat_ann1)
    mat_all2=rbind(mat_all2,mat_ann2)
    mat_all3=rbind(mat_all3,mat_ann3)
    
    ## identify sentences without any annotations for each curator
    ex1=which(rowSums(mat_ann1)==0)
    ex2=which(rowSums(mat_ann2)==0)
    ex3=which(rowSums(mat_ann3)==0)
    
    ## identify sentences that are not annotated by pairs of curators as those can be excluded from calculation
    ex12=unique(c(ex1,ex2))
    ex23=unique(c(ex2,ex3))
    ex31=unique(c(ex3,ex1))
    
    ## determine frequencies of CoreSC categories, required for baseline probability
    freq_ann1 = colSums(mat_ann1)/(dim(mat_ann1)[1]-length(ex1))
    freq_ann2 = colSums(mat_ann2)/(dim(mat_ann2)[1]-length(ex2))
    freq_ann3 = colSums(mat_ann3)/(dim(mat_ann3)[1]-length(ex3))
    
    ## remove sentences from kappa score where both curators have not assigned annotations
    if(length(ex12)>0) {
      A12=mat_ann1[-ex12,]*mat_ann2[-ex12,]
    } else {
      A12=mat_ann1*mat_ann2
    }
    
    ## remove sentences from kappa score where both curators have not assigned annotations
    if(length(ex23)>0) {
      A23=mat_ann2[-ex23,]*mat_ann3[-ex23,]
    } else {
      A23=mat_ann2*mat_ann3      
    }
    
    ## remove sentences from kappa score where both curators have not assigned annotations
    if(length(ex31)>0) {
      A31=mat_ann3[-ex31,]*mat_ann1[-ex31,]
    } else {
      A31=mat_ann3*mat_ann1
    }
    
    all_ann12=rbind(all_ann12,A12)
    all_ann23=rbind(all_ann23,A23)
    all_ann31=rbind(all_ann31,A31)
    
    ## multiple matches for one sentence not taken into consideration, therefore probability calculation has to change
    a=length(which(rowSums(A12)>=1))
    b=length(which(rowSums(A12)==2))
    c=length(which(rowSums(A12)==3))
    pa12=(a+b+c)/(dim(A12)[1]+b+c) 
    
    a=length(which(rowSums(A23)>=1))
    b=length(which(rowSums(A23)==2))
    c=length(which(rowSums(A23)==3))
    pa23=(a+b+c)/(dim(A23)[1]+b+c) 
    
    a=length(which(rowSums(A31)>=1))
    b=length(which(rowSums(A31)==2))
    c=length(which(rowSums(A31)==3))
    pa31= (a+b+c)/(dim(A31)[1]+b+c) 
    
    pe12=sum(freq_ann1*freq_ann2)
    pe23=sum(freq_ann2*freq_ann3)
    pe31=sum(freq_ann3*freq_ann1)
    
    k12=((pa12-pe12)/(1-pe12))
    k23=((pa23-pe23)/(1-pe23))
    k31=((pa31-pe31)/(1-pe31))
    
    kappa[f,"c1-c2"] = k12
    kappa[f,"c2-c3"] = k23
    kappa[f,"c1-c3"] = k31
  }
  
  kappa["macro","c1-c2"] = sum(kappa[1:length(files),"c1-c2"])/length(files)
  kappa["macro","c1-c3"] = sum(kappa[1:length(files),"c1-c3"])/length(files)
  kappa["macro","c2-c3"] = sum(kappa[1:length(files),"c2-c3"])/length(files)
  
  microKappa=kappafunc(mat_all1,mat_all2,mat_all3,all_ann12,all_ann23,all_ann31)
  kappa["micro","c1-c2"] = microKappa$k12
  kappa["micro","c1-c3"] = microKappa$k31
  kappa["micro","c2-c3"] = microKappa$k23
  
  sink(NULL);
  return(kappa)
}

#'--
#' Determine from two matrices, which annotator assigned more annotations and stores the number of annotations for 
#' this curator in a list. Each element in this list corresponds to highest number of annotations, which could have
#' been assigned either by curator one or two.
#' While the annotation matrices can contain values in the range [0; 1], this function requires that non-zero values
#' smaller than 1 are replaced with 1 (basically means that annotation was assigned, but the information is last as 
#' to which was first, second or third annotation).
#' 
#' @param mat1 Annotation matrix assigned by one of the curators (binary representation required!)
#' @param mat2 Annotation matrix assigned by another curator (binary representation required!)
#' @return List containing the maximum number of annotations, either assigned by curator one or curator two
#'--
maxAnnotationCount<-function(mat1,mat2) {
  count=NULL;
  if(dim(mat1)[1]==dim(mat2)[1])
  {
    for(i in 1:dim(mat1)[1])
    {
      ## here sum of row is same as number of non zero columns, as these matrices has already been converted to 0,1 values.
      if(sum(mat1[i,]<sum(mat2[i,])))
      {
        count[i]=sum(mat2[i,])
      }
      else
      {
        count[i]=sum(mat1[i,])
      }
    }
  }
  else # in case number of sentences do not match across annotators
  {
    print("Error in maxAnnotationCount, while determining the number of annotations assigned to a sentence.")
    print("f")
  }
  
  return(count)
}

#'--
#' AllButOneMatch calculates pairwise kappa inter-annotator agreement measures with the requirement of all annotations but one have to match
#' for the annotations assigned to a sentence for a pair of annotators. The measure in its current implementation only consider the type of
#' annotation but not further attributes.
#' 
#' @param files Names of all the files (here publications) annotations have been assigned to and a kappa score is needed for
#' @param out Folder in which to store result file
#' @param ann1 String identifying curator 1 in file system
#' @param ann2 String identifying curator 2 in file system
#' @param ann3 String identifying curator 3 in file system
#' @return Matrix containing all-but-one kappa scores for individual files, and macro-average in second to last and micro-average in last row
#'--
AllButOneMatch<-function(files, out, ann1, ann2, ann3) {
  filesDim = c(files, "macro", "micro")
  kappa = matrix(nrow=length(filesDim),ncol=3,dimnames=list(filesDim,c("c1-c2","c1-c3","c2-c3")));
  sink(file=paste(out,"kappa_micro_abo.log",sep=""))
  
  all_ann12=NULL
  all_ann23=NULL
  all_ann31=NULL
  
  mat_all1=NULL
  mat_all2=NULL
  mat_all3=NULL
  
  for(f in files) {
    print(f)
    
    ## read annotations from files and replace to binary representation
    mat_ann1=changeForLogical(readMatrix(path,ann1,f))
    mat_ann2=changeForLogical(readMatrix(path,ann2,f))
    mat_ann3=changeForLogical(readMatrix(path,ann3,f))
    
    ## generate matrix holding file-specific information for micro-average calculation
    mat_all1=rbind(mat_all1,mat_ann1)
    mat_all2=rbind(mat_all2,mat_ann2)
    mat_all3=rbind(mat_all3,mat_ann3)
    
    ## identify sentences without any annotations for each curator
    ex1=which(rowSums(mat_ann1)==0)
    ex2=which(rowSums(mat_ann2)==0)
    ex3=which(rowSums(mat_ann3)==0)
    
    ## identify sentences that are not annotated by pairs of curators as those can be excluded from calculation
    ex12=unique(c(ex1,ex2))
    ex23=unique(c(ex2,ex3))
    ex31=unique(c(ex3,ex1))
    
    ## determine frequencies of CoreSC categories, required for baseline probability
    freq_ann1 = colSums(mat_ann1)/(dim(mat_ann1)[1]-length(ex1))
    freq_ann2 = colSums(mat_ann2)/(dim(mat_ann2)[1]-length(ex2))
    freq_ann3 = colSums(mat_ann3)/(dim(mat_ann3)[1]-length(ex3))
    
    ## remove sentences from kappa score where both curators have not assigned annotations
    if(length(ex12)>0) {
      A12=mat_ann1[-ex12,]*mat_ann2[-ex12,]
      count12=maxAnnotationCount(mat_ann1[-ex12,],mat_ann2[-ex12,])
    } else {
      A12=mat_ann1*mat_ann2
      count12=maxAnnotationCount(mat_ann1,mat_ann2)
    }
    
    ## remove sentences from kappa score where both curators have not assigned annotations
    if(length(ex23)>0) {
      A23=mat_ann2[-ex23,]*mat_ann3[-ex23,]
      count23=maxAnnotationCount(mat_ann2[-ex23,],mat_ann3[-ex23,])
    } else {
      A23=mat_ann2*mat_ann3
      count23=maxAnnotationCount(mat_ann2,mat_ann3)
    }
    
    ## remove sentences from kappa score where both curators have not assigned annotations
    if(length(ex31)>0) {
      A31=mat_ann3[-ex31,]*mat_ann1[-ex31,]
      count31=maxAnnotationCount(mat_ann3[-ex31,],mat_ann1[-ex31,])
    } else {
      A31=mat_ann3*mat_ann1
      count31=maxAnnotationCount(mat_ann3,mat_ann1)
    }
    
    ## result matrices, but without sentences where both curators did not assign annotations
    all_ann12=rbind(all_ann12,A12)
    all_ann23=rbind(all_ann23,A23)
    all_ann31=rbind(all_ann31,A31)
    
    rowSumsA12 = rowSums(A12)
    rowSumsA23 = rowSums(A23)
    rowSumsA31 = rowSums(A31)
    
    allButoneInd12=which(rowSumsA12==count12 | (rowSumsA12==count12-1 & rowSumsA12!=0))
    allButoneInd23=which(rowSumsA23==count23 | (rowSumsA23==count23-1 & rowSumsA23!=0))
    allButoneInd31=which(rowSumsA31==count31 | (rowSumsA31==count31-1 & rowSumsA31!=0))
    
    pa12=length(allButoneInd12)/dim(A12)[1]	
    pa23=length(allButoneInd23)/dim(A23)[1]	
    pa31=length(allButoneInd31)/dim(A31)[1]	
    
    pe12=sum(freq_ann1*freq_ann2)
    pe23=sum(freq_ann2*freq_ann3)
    pe31=sum(freq_ann3*freq_ann1)
    
    k12=((pa12-pe12)/(1-pe12))
    k23=((pa23-pe23)/(1-pe23))
    k31=((pa31-pe31)/(1-pe31))
    
    kappa[f,"c1-c2"] = k12
    kappa[f,"c1-c3"] = k31  
    kappa[f,"c2-c3"] = k23
  }
  
  kappa["macro","c1-c2"] = sum(kappa[1:length(files),"c1-c2"])/length(files)
  kappa["macro","c1-c3"] = sum(kappa[1:length(files),"c1-c3"])/length(files)
  kappa["macro","c2-c3"] = sum(kappa[1:length(files),"c2-c3"])/length(files)
  
  microKappa=kappafunc(mat_all1,mat_all2,mat_all3,all_ann12,all_ann23,all_ann31)
  kappa["micro","c1-c2"] = microKappa$k12
  kappa["micro","c1-c3"] = microKappa$k31
  kappa["micro","c2-c3"] = microKappa$k23
  
  sink(NULL);
  return(kappa)
}

#'--
#' Calculates a kappa agreement measure on annotations that are weighted according to their ranking when assigned to a sentence. Three different
#' ranks are distinguished at most, leading to weights of: 1 in the case of single annotations; 0.6 for the first and 0.4 for the second annotation
#' in the case of two annotations; and 0.6 for the first, 0.3 for the second and 0.1 for the third annotation in the case of three annotations. 
#' More than three annotations cannot be assigned to one sentence and the weights always total to 1 per sentence. 
#' 
#' @param files Names of all the files (here publications) annotations have been assigned to and a kappa score is needed for
#' @param out Folder in which to store result file
#' @param ann1 String identifying curator 1 in file system
#' @param ann2 String identifying curator 2 in file system
#' @param ann3 String identifying curator 3 in file system
#' @return Matrix containing all-but-one kappa scores for individual files, and macro-average in second to last and micro-average in last row
#'--
paperversion <-function(files, out, ann1, ann2, ann3)
{
  filesDim = c(files, "macro", "micro")
  kappa = matrix(nrow=length(filesDim),ncol=3,dimnames=list(filesDim,c("c1-c2","c1-c3","c2-c3")));
  sink(file=paste(out,"kappa_micro_weighted.log",sep=""))
  
  all_ann12=NULL
  all_ann23=NULL
  all_ann31=NULL
  
  mat_all1=NULL
  mat_all2=NULL
  mat_all3=NULL
  
  for(f in files)
  {
    print(f)
    
    ## read annotations from files
    mat_ann1=readMatrix(path,ann1,f)
    mat_ann2=readMatrix(path,ann2,f)
    mat_ann3=readMatrix(path,ann3,f)
    
    ## generate matrix holding file-specific information for micro-average calculation
    mat_all1=rbind(mat_all1,mat_ann1)
    mat_all2=rbind(mat_all2,mat_ann2)
    mat_all3=rbind(mat_all3,mat_ann3)
    
    ## identify sentences without any annotations for each curator
    ex1=which(rowSums(mat_ann1)==0)
    ex2=which(rowSums(mat_ann2)==0)
    ex3=which(rowSums(mat_ann3)==0)
    
    ## identify sentences that are not annotated by pairs of curators as those can be excluded from calculation
    ex12=unique(c(ex1,ex2))
    ex23=unique(c(ex2,ex3))
    ex31=unique(c(ex3,ex1))
    
    ## determine frequencies of CoreSC categories, required for baseline probability
    freq_ann1 = colSums(mat_ann1)/(dim(mat_ann1)[1]-length(ex1))
    freq_ann2 = colSums(mat_ann2)/(dim(mat_ann2)[1]-length(ex2))
    freq_ann3 = colSums(mat_ann3)/(dim(mat_ann3)[1]-length(ex3))
    
    ## remove sentences from kappa score where both curators have not assigned annotations
    if(length(ex12)>0) {
      A12=mat_ann1[-ex12,]*mat_ann2[-ex12,]
    } else {
      A12=mat_ann1*mat_ann2
    }
    
    ## remove sentences from kappa score where both curators have not assigned annotations
    if(length(ex23)>0) {
      A23=mat_ann2[-ex23,]*mat_ann3[-ex23,]
    } else {
      A23=mat_ann2*mat_ann3      
    }
    
    ## remove sentences from kappa score where both curators have not assigned annotations
    if(length(ex31)>0) {
      A31=mat_ann3[-ex31,]*mat_ann1[-ex31,]
    } else {
      A31=mat_ann3*mat_ann1
    }
    
    all_ann12=rbind(all_ann12,A12)
    all_ann23=rbind(all_ann23,A23)
    all_ann31=rbind(all_ann31,A31)
    
    pa12=(sum(A12))/dim(A12)[1]
    pa23=(sum(A23))/dim(A23)[1]
    pa31=(sum(A31))/dim(A31)[1]
    
    pe12=sum(freq_ann1*freq_ann2)
    pe23=sum(freq_ann2*freq_ann3)
    pe31=sum(freq_ann3*freq_ann1)
    
    k12=((pa12-pe12)/(1-pe12))
    k23=((pa23-pe23)/(1-pe23))
    k31=((pa31-pe31)/(1-pe31))
    
    kappa[f,"c1-c2"] = k12
    kappa[f,"c2-c3"] = k23
    kappa[f,"c1-c3"] = k31
  }
  
  kappa["macro","c1-c2"] = sum(kappa[1:length(files),"c1-c2"])/length(files)
  kappa["macro","c1-c3"] = sum(kappa[1:length(files),"c1-c3"])/length(files)
  kappa["macro","c2-c3"] = sum(kappa[1:length(files),"c2-c3"])/length(files)
  
  microKappa=kappafunc(mat_all1,mat_all2,mat_all3,all_ann12,all_ann23,all_ann31)
  kappa["micro","c1-c2"] = microKappa$k12
  kappa["micro","c1-c3"] = microKappa$k31
  kappa["micro","c2-c3"] = microKappa$k23
  
  sink(NULL);
  return(kappa)
}

#'--
#' Changes the annotation matrix to only consider the first annotation of each sentence.
#'
#' @param mat_ann Matrix containing annotations for one curator
#' @return Matrix, where all annotations apart from the first assigned to a sentence, have been removed
#'--
changeForSingle<-function(mat_ann) {
  mat_ann[which(mat_ann==0.6,arr.ind=T)]=1
  mat_ann[which(mat_ann!=1,arr.ind=T)]=0
  return(mat_ann)
}

#'--
#' Calculates kappa agreement measures between all three pairs of curators based on the first CoreSC concept assigned to a sentence only. All other annotations are 
#' neglected in this calculation. 
#'
#' @param files Names of all the files (here publications) annotations have been assigned to and a kappa score is needed for
#' @param out Folder in which to store result file
#' @param ann1 String identifying curator 1 in file system
#' @param ann2 String identifying curator 2 in file system
#' @param ann3 String identifying curator 3 in file system
#' @return Matrix containing all-but-one kappa scores for individual files, and macro-average in second to last and micro-average in last row
#'--
coresc1Match<-function(files, out, ann1, ann2, ann3)
{
  filesDim = c(files, "macro", "micro")
  kappa = matrix(nrow=length(filesDim),ncol=3,dimnames=list(filesDim,c("c1-c2","c1-c3","c2-c3")));
  sink(file=paste(out,"kappa_micro_coresc1.log",sep=""))
  
  all_ann12=NULL
  all_ann23=NULL
  all_ann31=NULL
  
  mat_all1=NULL
  mat_all2=NULL
  mat_all3=NULL
  
  for(f in files)
  {
    print(f)
    
    ## read annotations from files and remove all annotations that have not been assigned as primary annotation
    mat_ann1=changeForSingle(readMatrix(path,ann1,f))
    mat_ann2=changeForSingle(readMatrix(path,ann2,f))
    mat_ann3=changeForSingle(readMatrix(path,ann3,f))
    
    ## generate matrix holding file-specific information for micro-average calculation
    mat_all1=rbind(mat_all1,mat_ann1)
    mat_all2=rbind(mat_all2,mat_ann2)
    mat_all3=rbind(mat_all3,mat_ann3)
    
    ## identify sentences without any annotations for each curator
    ex1=which(rowSums(mat_ann1)==0)
    ex2=which(rowSums(mat_ann2)==0)
    ex3=which(rowSums(mat_ann3)==0)
    
    ## identify sentences that are not annotated by pairs of curators as those can be excluded from calculation
    ex12=unique(c(ex1,ex2))
    ex23=unique(c(ex2,ex3))
    ex31=unique(c(ex3,ex1))
    
    ## determine frequencies of CoreSC categories, required for baseline probability
    freq_ann1 = colSums(mat_ann1)/(dim(mat_ann1)[1]-length(ex1))
    freq_ann2 = colSums(mat_ann2)/(dim(mat_ann2)[1]-length(ex2))
    freq_ann3 = colSums(mat_ann3)/(dim(mat_ann3)[1]-length(ex3))
    
    ## remove sentences from kappa score where both curators have not assigned annotations
    if(length(ex12)>0) {
      A12=mat_ann1[-ex12,]*mat_ann2[-ex12,]
    }else {
      A12=mat_ann1*mat_ann2
    }
    
    ## remove sentences from kappa score where both curators have not assigned annotations
    if(length(ex23)>0) {
      A23=mat_ann2[-ex23,]*mat_ann3[-ex23,]
    } else {
      A23=mat_ann2*mat_ann3      
    }
    
    ## remove sentences from kappa score where both curators have not assigned annotations
    if(length(ex31)>0) {
      A31=mat_ann3[-ex31,]*mat_ann1[-ex31,]
    } else {
      A31=mat_ann3*mat_ann1
    }
    
    all_ann12=rbind(all_ann12,A12)
    all_ann23=rbind(all_ann23,A23)
    all_ann31=rbind(all_ann31,A31)
    
    pa12=(sum(A12))/dim(A12)[1]
    pa23=(sum(A23))/dim(A23)[1]
    pa31=(sum(A31))/dim(A31)[1]
    
    pe12=sum(freq_ann1*freq_ann2)
    pe23=sum(freq_ann2*freq_ann3)
    pe31=sum(freq_ann3*freq_ann1)
    
    k12=((pa12-pe12)/(1-pe12))
    k23=((pa23-pe23)/(1-pe23))
    k31=((pa31-pe31)/(1-pe31))
    
    kappa[f,"c1-c2"] = k12
    kappa[f,"c2-c3"] = k23
    kappa[f,"c1-c3"] = k31
  }
  
  kappa["macro","c1-c2"] = sum(kappa[1:length(files),"c1-c2"])/length(files)
  kappa["macro","c1-c3"] = sum(kappa[1:length(files),"c1-c3"])/length(files)
  kappa["macro","c2-c3"] = sum(kappa[1:length(files),"c2-c3"])/length(files)
  
  microKappa=kappafunc(mat_all1,mat_all2,mat_all3,all_ann12,all_ann23,all_ann31)
  kappa["micro","c1-c2"] = microKappa$k12
  kappa["micro","c1-c3"] = microKappa$k31
  kappa["micro","c2-c3"] = microKappa$k23
  
  sink(NULL);
  return(kappa)
}

#'--
#' Counts the number of annotations where a pair of curators agree on assignment. Note that while annotators can disagree on 
#' first annotation, they can still agree on the second or third annotation of a sentence.
#' 
#' @param tann1 Annotations assigned by one curator
#' @paran tann2 Annotations assigned by second curator
#'--
matchCount<-function(tann1,tann2)
{
  match_ann=0
  for(i in 1:dim(tann1)[1])
  {
    if(length(which((tann1[i,]-tann2[i,])!=0))==0)
    {
      match_ann=match_ann+1;
    }
  }
  return(match_ann)
}

#'--
#' Calculates the micro-averaged kappa agreement for all three pairs of curators based on the annotations assigned. For a strict match,
#' not only the type of the annotation has to match but also the rank with which it is assigned to the sentence. Note that if primary
#' annotations do not match, secondary and teriary still can.
#' 
#' @param mat_all1 Annotations assigned by first curator
#' @param mat_all2 Annotations assigned by second curator
#' @param mat_all1 Annotations assigned by third curator
#' @param count12 Annotations that match between first and second curator across all files
#' @param count23 Annotations that match between second and third curator across all files
#' @param count12 Annotations that match between first and third curator across all files
#' @param n12 Number of sentences that received annotations by first and second curator
#' @param n23 Number of sentences that received annotations by second and third curator
#' @param n13 Number of sentences that received annotations by first and third curator
#' @return Micro-average kappa scores for each of the curator pairings
#'--
kappafuncstrict<-function(mat_all1,mat_all2,mat_all3,count12,count23,count31,n12,n23,n31)
{
  pa12=(count12)/n12
  pa23=(count23)/n23
  pa31=(count31)/n31
  
  e1=which(rowSums(mat_all1)==0)
  e2=which(rowSums(mat_all2)==0)
  e3=which(rowSums(mat_all3)==0)
  
  freq_ann1 = colSums(mat_all1)/(dim(mat_all1)[1]-length(e1))
  freq_ann2 = colSums(mat_all2)/(dim(mat_all2)[1]-length(e2))
  freq_ann3 = colSums(mat_all3)/(dim(mat_all3)[1]-length(e3))
  
  pe12=sum(freq_ann1*freq_ann2)
  pe23=sum(freq_ann2*freq_ann3)
  pe31=sum(freq_ann3*freq_ann1)
  
  kappa12=((pa12-pe12)/(1-pe12))
  kappa23=((pa23-pe23)/(1-pe23))
  kappa31=((pa31-pe31)/(1-pe31))
  
  return(list(k12=kappa12, k23=kappa23, k31=kappa31))
}

#'--
#' @param files Names of all the files (here publications) annotations have been assigned to and a kappa score is needed for
#' @param out Folder in which to store result file
#' @param ann1 String identifying curator 1 in file system
#' @param ann2 String identifying curator 2 in file system
#' @param ann3 String identifying curator 3 in file system
#' @return Matrix containing all-but-one kappa scores for individual files, and macro-average in second to last and micro-average in last row
#'--
strictMatch<-function(files, out, ann1, ann2, ann3)
{
  filesDim = c(files, "macro", "micro")
  kappa = matrix(nrow=length(filesDim),ncol=3,dimnames=list(filesDim,c("c1-c2","c1-c3","c2-c3")));
  sink(file=paste(out,"kappa_micro_strict.log",sep=""))
  
  all_ann12=0
  all_ann23=0
  all_ann31=0
  
  mat_all1=NULL
  mat_all2=NULL
  mat_all3=NULL
  
  n12=0
  n23=0
  n31=0
  
  for(f in files)
  {
    print(f)
    
    ## read annotations from files and replace to binary representation
    mat_ann1=changeForLogical(readMatrix(path,ann1,f))
    mat_ann2=changeForLogical(readMatrix(path,ann2,f))
    mat_ann3=changeForLogical(readMatrix(path,ann3,f))
    
    ## generate matrix holding file-specific information for micro-average calculation
    mat_all1=rbind(mat_all1,mat_ann1)
    mat_all2=rbind(mat_all2,mat_ann2)
    mat_all3=rbind(mat_all3,mat_ann3)
  
    ## identify sentences without any annotations for each curator
    ex1=which(rowSums(mat_ann1)==0)
    ex2=which(rowSums(mat_ann2)==0)
    ex3=which(rowSums(mat_ann3)==0)
    
    ## identify sentences that are not annotated by pairs of curators as those can be excluded from calculation
    ex12=unique(c(ex1,ex2))
    ex23=unique(c(ex2,ex3))
    ex31=unique(c(ex3,ex1))
    
    ## determine frequencies of CoreSC categories, required for baseline probability
    freq_ann1 = colSums(mat_ann1)/(dim(mat_ann1)[1]-length(ex1))
    freq_ann2 = colSums(mat_ann2)/(dim(mat_ann2)[1]-length(ex2))
    freq_ann3 = colSums(mat_ann3)/(dim(mat_ann3)[1]-length(ex3))
    
    ## remove sentences from kappa score where both curators have not assigned annotations
    if(length(ex12)>0) {
      temp_ann1=mat_ann1[-ex12,]
      temp_ann2=mat_ann2[-ex12,]
    } else {
      temp_ann1=mat_ann1
      temp_ann2=mat_ann2
    }
    
    ## determine matching annotations and total sentence number
    matchCount12=matchCount(temp_ann1,temp_ann2)
    temp_n12=dim(temp_ann1)[1] 
    n12=n12+temp_n12
    
    ## remove sentences from kappa score where both curators have not assigned annotations
    if(length(ex23)>0) {
      temp_ann2=mat_ann2[-ex23,]
      temp_ann3=mat_ann3[-ex23,]
    } else {
      temp_ann2=mat_ann2
      temp_ann3=mat_ann3
    }
    
    ## determine matching annotations and total sentence number
    matchCount23=matchCount(temp_ann2,temp_ann3)
    temp_n23=dim(temp_ann2)[1] 
    n23=n23+temp_n23
    
    ## remove sentences from kappa score where both curators have not assigned annotations
    if(length(ex31)>0) {
      temp_ann3=mat_ann3[-ex31,]
      temp_ann1=mat_ann1[-ex31,]
    } else {
      temp_ann3=mat_ann3
      temp_ann1=mat_ann1
    }
    
    ## determine matching annotations and total sentence number
    matchCount31=matchCount(temp_ann3,temp_ann1)
    temp_n31=dim(temp_ann3)[1] 
    n31=n31+temp_n31
    
    all_ann12=all_ann12+matchCount12
    all_ann23=all_ann23+matchCount23
    all_ann31=all_ann31+matchCount31
    
    pa12=(matchCount12)/temp_n12
    pa23=(matchCount23)/temp_n23
    pa31=(matchCount31)/temp_n31
    
    pe12=sum(freq_ann1*freq_ann2)
    pe23=sum(freq_ann2*freq_ann3)
    pe31=sum(freq_ann3*freq_ann1)
    
    k12=((pa12-pe12)/(1-pe12))
    k23=((pa23-pe23)/(1-pe23))
    k31=((pa31-pe31)/(1-pe31))
    
    kappa[f,"c1-c2"] = k12
    kappa[f,"c2-c3"] = k23
    kappa[f,"c1-c3"] = k31
  }
  
  kappa["macro","c1-c2"] = sum(kappa[1:length(files),"c1-c2"])/length(files)
  kappa["macro","c1-c3"] = sum(kappa[1:length(files),"c1-c3"])/length(files)
  kappa["macro","c2-c3"] = sum(kappa[1:length(files),"c2-c3"])/length(files)
  
  microKappa=kappafuncstrict(mat_all1,mat_all2,mat_all3,all_ann12,all_ann23,all_ann31,n12,n23,n31)
  kappa["micro","c1-c2"] = microKappa$k12
  kappa["micro","c1-c3"] = microKappa$k31
  kappa["micro","c2-c3"] = microKappa$k23
  
  sink(NULL);
  return(kappa)
}

##
## Reading input parameters (location matrices and output) and calculating five different kappa scores
##
args<-commandArgs(TRUE)
path = args[1]
out = args[2]

ann1="curator1"
ann2="curator2"
ann3="curator3"

files=list.files(path = paste(path,"curator1/",sep=""))

kappa_strict=strictMatch(files, out, ann1, ann2, ann3)
kappa_coresc1=coresc1Match(files, out, ann1, ann2, ann3)
kappa_weighted=paperversion(files, out, ann1, ann2, ann3)
kappa_abo=AllButOneMatch(files, out, ann1, ann2, ann3)
kappa_loose=logicalLoose(files, out, ann1, ann2, ann3)

write.csv(kappa_strict,file=paste(out, "kappa_strict.csv", sep="")) 
write.csv(kappa_coresc1,file=paste(out, "kappa_coresc1.csv", sep=""))
write.csv(kappa_weighted,file=paste(out, "kappa_weighted.csv", sep=""))
write.csv(kappa_abo,file=paste(out, "kappa_abo.csv", sep=""))
write.csv(kappa_loose,file=paste(out, "kappa_loose.csv", sep=""))
