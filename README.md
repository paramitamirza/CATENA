# CATENA
## CAusal and TEmporal relation extraction from NAtural language texts 


###Requirements
* Java Runtime Environment (JRE) 1.7.x or higher

#####Text processing tools:
* [Stanford CoreNLP 3.7.x](http://stanfordnlp.github.io/CoreNLP/) or higher -- a suite of core NLP tools. The .jar file should be included in the classpath.
* [TextPro](http://textpro.fbk.eu/) -- Text Processing Tools from FBK. 
* [Mate-tools](https://code.google.com/archive/p/mate-tools/) -- Tools for Natural Language Analysis. Our system requires [anna-3.3.jar](https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/mate-tools/anna-3.3.jar) (transition-based and graph-based dependency parser, tagger, lemmatizer and morphologic tagger - version 3.3), and related models including [CoNLL2009-ST-English-ALL.anna-3.3.lemmatizer.model](https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/mate-tools/CoNLL2009-ST-English-ALL.anna-3.3.lemmatizer.model), [CoNLL2009-ST-English-ALL.anna-3.3.postagger.model](https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/mate-tools/CoNLL2009-ST-English-ALL.anna-3.3.postagger.model) and [CoNLL2009-ST-English-ALL.anna-3.3.parser.model](https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/mate-tools/CoNLL2009-ST-English-ALL.anna-3.3.parser.model).
* [JDOM 2.0.x](http://www.jdom.org/index.html) or higher -- JDOM API for accessing, manipulating and  outputting XML data from Java code. The .jar file should be included in the classpath.

#####Other libraries:
* [liblinear-java](http://liblinear.bwaldvogel.de/) -- Java port of the [original liblinear C++ sources](http://www.csie.ntu.edu.tw/~cjlin/liblinear/).
* [WS4J](https://github.com/Sciss/ws4j) -- APIs for several semantic relatedness algorithms for, in theory, any WordNet instance.
* [Jersey](https://jersey.java.net/) -- RESTful Web Service in Java. It is required to access temporal closure module in http://hixwg.univaq.it/TERENCE-reasoner.
 
###Usage
_! The input file(s) must be in the [TimeML annotation format](http://www.timeml.org/site/index.html) !_
```
python python TimexExtraction.py dir_name [options]        or
python python TimexExtraction.py file_name [options]

options: -o output_dir_name/file_name (default: dir_path/dir_name_Timex/ for directory and file_path/file_name_timex.tml for file)
```   
The output file(s) will be a TimeML document annotated with temporal expressions (TIMEX3 tags).

#####To convert TimeML file(s) to HTML for better viewing
```
python python ConvertToHTML.py dir_name [options]        or
python python ConvertToHTML.py file_name [options]

options: -o output_dir_name/file_name (default: dir_path/dir_name_HTML/ for directory and file_path/file_name.html for file)
```   

###Modules
IndoTimex contains two main modules:

1. **Timex recognition**, a finite state transducer (FST) to recognize temporal expressions and their types (based on the TimeML standard, i.e. DATE, DURATION, TIME and SET). The complete FST can be seen in [`lib/fst/timex.pdf`](https://github.com/paramitamirza/IndoTimex/blob/master/lib/fst/timex.pdf) (minimized and drawn with [OpenFST](http://www.openfst.org/)).
2. **Timex normalization**, an extension of [TimeNorm](https://github.com/bethard/timenorm), a library for normalizing the values of temporal expressions (based on the ISO 8601 standard) using synchronous context free grammars, for Indonesian language. To run the timex normalizer: `java -jar ./lib/timenorm-id-0.9.2-jar-with-dependencies.jar ./lib/id.grammar`.
 
#####Publication
Paramita Mirza. 2015. **Recognizing and Normalizing Temporal Expressions in Indonesian Texts**. *(to appear) In Proceedings of the Conference of the Pacific Association for Computational Linguistics (PACLING 2015)*, Bali, Indonesia, May. [pdf]

#####Dataset
The dataset for development and evaluation phases of the system is available in `dataset/`, comprising 75 news articles taken from www.kompas.com.

_! Whenever making reference to this resource please cite the paper in the Publication section. !_

###Demo
The online demo is available at [http://paramitamirza.ml/indotimex/](http://paramitamirza.ml/indotimex/).

###Contact
For more information please contact [Paramita Mirza](http://paramitopia.com/about/) (paramita@fbk.eu).
