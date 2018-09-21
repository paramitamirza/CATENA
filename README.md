# CATENA
## CAusal and TEmporal relation extraction from NAtural language texts 
CATENA is a sieve-based system to perform temporal and causal relation extraction and classification from English texts, exploiting the interaction between the temporal and the causal model. The system requires pre-annotated text with EVENT and TIMEX3 tags according to the TimeML annotation standard, as these annotation are used as features to extract the relations.

### Requirements
* Java Runtime Environment (JRE) 1.7.x or higher

#### Maven 
CATENA is now available on [Maven Central](https://search.maven.org/artifact/com.github.paramitamirza/CATENA/1.0.3/jar). Please add the following dependency in your `pom.xml`.
```
<dependency>
  <groupId>com.github.paramitamirza</groupId>
  <artifactId>CATENA</artifactId>
  <version>1.0.3</version>
</dependency>
```
To build the fat (executable) JAR:
* Install the WS4J library in your local Maven repo, e.g., `mvn install:install-file -Dfile=./lib/ws4j-1.0.1.jar -DgroupId=edu.cmu.lti -DartifactId=ws4j -Dversion=1.0.1 -Dpackaging=jar`
* Run `mvn package` to build the executable JAR file (in `target/CATENA-<version>.jar`).

#### Text processing tools:
* [Stanford CoreNLP 3.7.x](http://stanfordnlp.github.io/CoreNLP/) or higher -- a suite of core NLP tools.
* [TextPro](http://textpro.fbk.eu/) -- Text Processing Tools from FBK. 
* [Mate-tools](https://code.google.com/archive/p/mate-tools/) -- Tools for Natural Language Analysis. Our system requires [anna-3.3.jar](https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/mate-tools/anna-3.3.jar) (transition-based and graph-based dependency parser, tagger, lemmatizer and morphologic tagger - version 3.3), and related models including [CoNLL2009-ST-English-ALL.anna-3.3.lemmatizer.model](https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/mate-tools/CoNLL2009-ST-English-ALL.anna-3.3.lemmatizer.model), [CoNLL2009-ST-English-ALL.anna-3.3.postagger.model](https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/mate-tools/CoNLL2009-ST-English-ALL.anna-3.3.postagger.model) and [CoNLL2009-ST-English-ALL.anna-3.3.parser.model](https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/mate-tools/CoNLL2009-ST-English-ALL.anna-3.3.parser.model).
* [JDOM 2.0.x](http://www.jdom.org/index.html) or higher -- JDOM API for accessing, manipulating and  outputting XML data from Java code. The .jar file should be included in the classpath.

#### Other libraries:
* [liblinear-java](http://liblinear.bwaldvogel.de/) -- Java port of the [original liblinear C++ sources](http://www.csie.ntu.edu.tw/~cjlin/liblinear/).
* [WS4J](https://github.com/Sciss/ws4j) -- APIs for several semantic relatedness algorithms for, in theory, any WordNet instance.
* [Jersey](https://jersey.java.net/) -- RESTful Web Service in Java. It is required to access temporal closure module in http://hixwg.univaq.it/TERENCE-reasoner.
* [Apache Commons CLI](https://commons.apache.org/proper/commons-cli/) - an API for parsing command line options passed to programs.

#### Other resources:
* Temporal and causal signal lists, available in `resource/`. This folder must be placed within the root folder of the project.
* Classification models, available in `models/`, including: `catena-event-timex.model`, `catena-event-dct.model`, `catena-event-event.model` and `catena-causal-event-event.model`.
 
### Usage
_! The input file(s) must be in the [__TimeML annotation format__](http://www.timeml.org/site/index.html) or __CoNLL column format__ (one token per line) !_
```
usage: Catena
 -i,--input <arg>        Input TimeML file/directory path
 -f,--col                (optional) Input files are in column format (.col)
 -tl,--tlinks <arg>      (optional) Input file containing list of gold temporal links
 -cl,--clinks <arg>      (optional) Input file containing list of gold causal links
 -gl,--gold              (optional) Gold candidate pairs to be classified are given
 -y,--clinktype          (optional) Output the type of CLINK (ENABLE, PREVENT, etc.) from the rule-based sieve
        
 -x,--textpro <arg>      TextPro directory path
 -l,--matelemma <arg>    Mate tools' lemmatizer model path   
 -g,--matetagger <arg>   Mate tools' PoS tagger model path
 -p,--mateparser <arg>   Mate tools' parser model path      
 
 -t,--ettemporal <arg>   CATENA model path for E-T temporal classifier    
 -d,--edtemporal <arg>   CATENA model path for E-D temporal classifier                       
 -e,--eetemporal <arg>   CATENA model path for E-E temporal classifier
 -c,--eecausal <arg>     CATENA model path for E-E causal classifier
 
 -b,--train              (optional) Train the models
 -m,--tempcorpus <arg>   (optional) Directory path (containing .tml or .col files) for training temporal classifiers
 -u,--causcorpus <arg>   (optional) Directory path (containing .tml or .col files) for training causal classifier     
``` 
For example
```
java -Xmx2G -jar ./target/CATENA-1.0.2.jar -i ./data/example_COL/ --col --tlinks ./data/TempEval3.TLINK.txt --clinks ./data/Causal-TimeBank.CLINK.txt -l ./models/CoNLL2009-ST-English-ALL.anna-3.3.lemmatizer.model -g ./models/CoNLL2009-ST-English-ALL.anna-3.3.postagger.model -p ./models/CoNLL2009-ST-English-ALL.anna-3.3.parser.model -x ./tools/TextPro2.0/ -d ./models/catena-event-dct.model -t ./models/catena-event-timex.model -e ./models/catena-event-event.model -c ./models/catena-causal-event-event.model -b -m ./data/Catena-train_COL/ -u ./data/Causal-TimeBank_COL/
```
  
#### CoNLL column format
The input document must be in tab-separated 'one-token-per-line' format, with each column as:
| `token` | `token-id` | `sentence-id`	|	`lemma` | `event-id` |	`event-class` |	`event-tense+aspect+polarity` | `timex-id` | `timex-type`	| `timex-value` | `signal-id` |	`causal-signal-id` | `pos-tag` | `chunk` | `lemma` | `pos-tag` | `dependencies` | `main-verb` |

* `event-id` and `event-class`: TimeML event ID and attributes
* `timex-id` and `timex-type` and `timex-value`: TimeML timex ID and attributes
* `signal-id` and `causal-signal-id`: temporal and causal signal ID
* `event-tense+aspect+polarity`: optional attributes of an event, if given `O`, CATENA will infer them automatically according to PoS tags and dependency relations
* `pos-tag`: BNC tagset (default tagset uset to build the models) or Penn Treebank tagset
* `chunk`: 
* `dependencies`: in the format of `dep1:deprel1||dep2:deprel2||...`, dependency relations are resulted from [Mate-tools](https://code.google.com/archive/p/mate-tools/)

See for example `data/example_COL/`.

#### Output format
The output will be a list of temporal and/or causal relations, one relation per line, in the format of:
```
filename  entity_1  entity_2  TLINK_type/CLINK/CLINK-R
```
* `TLINK_type`: One of TLINK types according to TimeML, e.g., `BEFORE`, `AFTER`, `SIMULTANEOUS`
* `CLINK`: entity_1 `CAUSE` entity_2
* `CLINK-R`: entity_1 `IS_CAUSED_BY` entity_2

### System architecture

![alt tag](https://github.com/paramitamirza/CATENA/blob/master/CATENA.png)

CATENA contains two main modules:

1. **Temporal module**, a combination of rule-based and supervised classifiers, with a temporal reasoner module in between.
2. **Causal module**, a combination of a rule-based classifier according to causal verbs, and supervised classifier taken into account syntactic and context features, especially causal signals appearing in the text.

The two modules interact, based on the assumption that the notion of causality is tightly connected with the temporal dimension:
(i) TLINK labels for event-event pairs, resulting from the rule-based sieve + temporal reasoner, are used for the CLINK classifier, and
(ii) CLINK labels are used as a post-editing method for correcting the wrongly labelled event pairs by the Temporal module.
 
#### Publication
Paramita Mirza and Sara Tonelli. 2016. *CATENA: CAusal and TEmporal relation extraction from NAtural language texts.* In Proceedings of COLING 2016, the 26th International Conference on Computational Linguistics: Technical Papers, Osaka, Japan, December. [[pdf]](https://aclweb.org/anthology/C/C16/C16-1007.pdf)

#### Dataset
* Training data for the Temporal module is taken from the [TempEval-3](https://www.cs.york.ac.uk/semeval-2013/task1/index.php%3Fid=data.html) shared task, particularly the combination of TBAQ-cleaned (English training data) and TE3-platinum (English test data).
* Training data for the Causal module is [Causal-TimeBank](http://hlt-nlp.fbk.eu/technologies/causal-timebank), the TimeBank corpus annotated with causal information.
* [TimeBank-Dense](https://www.usna.edu/Users/cs/nchamber/caevo/#corpus) corpus is used in one of the evaluation schemes for temporal relation extraction. 
* `Causal-TempEval3-eval.txt` (available in `data/`) is used in one of the evaluation schemes for causal relation extraction.

_! Whenever making reference to this resource please cite the paper in the Publication section. !_

#### Web Service
Soon!

### Contact
For more information please contact [Paramita Mirza](http://paramitamirza.com/) (paramita135@gmail.com).
