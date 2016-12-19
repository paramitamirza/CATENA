# CATENA
## CAusal and TEmporal relation extraction from NAtural language texts 
CATENA is a sieve-based system to perform temporal and causal relation extraction and classification from English texts, exploiting the interaction between the temporal and the causal model. The system requires pre-annotated text with EVENT and TIMEX3 tags according to the TimeML annotation standard, as these annotation are used as features to extract the relations.

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
* [Apache Commons CLI](https://commons.apache.org/proper/commons-cli/) - an API for parsing command line options passed to programs.

#####Other resources:
* Temporal and causal signal lists, available in `resource/`. This folder must be placed within the root folder of the project.
* Classification models, available in `models/`, including: `catena-event-timex.model`, `catena-event-dct.model`, `catena-event-event.model` and `catena-causal-event-event.model`.
 
###Usage
_! The input file(s) must be in the [TimeML annotation format](http://www.timeml.org/site/index.html) !_
```
usage: Catena
 -i,--input <arg>        Input TimeML file/directory path
        
 -x,--textpro <arg>      TextPro directory path
 -l,--matelemma <arg>    Mate tools' lemmatizer model path   
 -g,--matetagger <arg>   Mate tools' PoS tagger model path
 -p,--mateparser <arg>   Mate tools' parser model path      
 
 -t,--ettemporal <arg>   CATENA model path for E-T temporal classifier    
 -d,--edtemporal <arg>   CATENA model path for E-D temporal classifier                       
 -e,--eetemporal <arg>   CATENA model path for E-E temporal classifier
 -c,--eecausal <arg>     CATENA model path for E-E causal classifier
 
 -b,--train              (optional) Train the models
 -m,--tempcorpus <arg>   (optional) TimeML directory path for training temporal
                         classifiers
 -u,--causcorpus <arg>   (optional) TimeML directory path for training causal
                         classifier     
```   
The output will be a list of temporal and/or causal relations, one relation per line, in the format of:
```
<filename>	<entity_1>	<entity_2>	<TLINK_type/CLINK/CLINK-R>
  
  TLINK_type			One of TLINK types according to TimeML, e.g., BEFORE, AFTER, SIMULTANEOUS, etc.
  CLINK					entity_1 CAUSE entity_2
  CLINK-R				entity_1 IS_CAUSED_BY entity_2
```

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
Paramita Mirza and Sara Tonelli. 2016. *CATENA: CAusal and TEmporal relation extraction from NAtural language texts.* In Proceedings of COLING 2016, the 26th International Conference on Computational Linguistics: Technical Papers, Osaka, Japan, December. [[pdf]](https://aclweb.org/anthology/C/C16/C16-1007.pdf)

#####Dataset
* Training data for the Temporal module is taken from the [TempEval-3](https://www.cs.york.ac.uk/semeval-2013/task1/index.php%3Fid=data.html) shared task, particularly the combination of TBAQ-cleaned (English training data) and TE3-platinum (English test data).
* Training data for the Causal module is [Causal-TimeBank](http://hlt-nlp.fbk.eu/technologies/causal-timebank), the TimeBank corpus annotated with causal information.
* [TimeBank-Dense](https://www.usna.edu/Users/cs/nchamber/caevo/#corpus) corpus is used in one of the evaluation schemes for temporal relation extraction. 
* `Causal-TempEval3-eval.txt` (available in `data/`) is used in one of the evaluation schemes for causal relation extraction.

_! Whenever making reference to this resource please cite the paper in the Publication section. !_

###Demo
The online demo is available at [http://paramitamirza.ml/indotimex/](http://paramitamirza.ml/indotimex/).

###Contact
For more information please contact [Paramita Mirza](http://paramitopia.com/about/) (paramita@fbk.eu).
